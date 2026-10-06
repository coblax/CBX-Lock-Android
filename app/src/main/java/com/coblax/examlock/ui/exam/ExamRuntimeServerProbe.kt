package com.coblax.examlock.ui.exam

import android.os.Build
import android.os.SystemClock
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.model.DiagnosticEventLevel
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val ExamServerProbeIntervalMillis = 30_000L
private const val ExamServerProbeTimeoutMillis = 12_000
private const val ExamServerProbeSlowThresholdMillis = 8_000L
private const val ExamServerProbeUserAgent =
    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

internal fun examServerProbeIntervalMillis(lowRamProfile: LowRamProfile): Long =
    lowRamProfile.examServerProbeIntervalMillis

internal data class ExamServerHttpProbeOutcome(
    val method: String,
    val code: Int?,
    val latencyMs: Long,
    val failure: String?,
    val tlsFailure: Boolean = false,
    val certificateRejected: Boolean = false,
    val timedOut: Boolean = false,
    val reachableWithoutHttp: Boolean = false
)

/**
 * Only Android 7.x lacks the handshake support some current certificates need, and only
 * a handshake that failed outright says so. Elsewhere, or after a timeout, a TLS failure
 * is a real failure: the TCP retry used to turn those into Online too.
 */
internal fun shouldRetryHandshakeOverTcp(outcome: ExamServerHttpProbeOutcome, sdkInt: Int): Boolean =
    outcome.tlsFailure && !outcome.certificateRejected && !outcome.timedOut &&
        sdkInt <= Build.VERSION_CODES.N_MR1

/**
 * The phone itself refuses the server's certificate: expired, issued for another host,
 * or from an issuer it does not trust. The exam WebView checks against the same trust
 * store and refuses it too, so such a server is not one the exam can open.
 */
internal fun isCertificateRejection(throwable: Throwable): Boolean =
    generateSequence(throwable) { it.cause }.take(8).any { cause ->
        cause is SSLPeerUnverifiedException ||
            cause is CertificateException ||
            cause is CertPathValidatorException
    }

internal data class ExamServerProbeResult(
    val status: ExamServerFooterStatus,
    val host: String,
    val method: String,
    val code: Int?,
    val latencyMs: Long?,
    val reason: String
) {
    val eventCode: String
        get() = when (status) {
            ExamServerFooterStatus.Online -> "EXAM_SERVER_PROBE_ONLINE"
            ExamServerFooterStatus.Warning -> "EXAM_SERVER_PROBE_WARNING"
            ExamServerFooterStatus.Offline -> "EXAM_SERVER_PROBE_OFFLINE"
            ExamServerFooterStatus.Checking -> "EXAM_SERVER_PROBE_STARTED"
            ExamServerFooterStatus.Unstable -> "EXAM_SERVER_PROBE_UNSTABLE"
            ExamServerFooterStatus.Insecure -> "EXAM_SERVER_PROBE_INSECURE"
        }

    val eventLevel: DiagnosticEventLevel
        get() = when (status) {
            ExamServerFooterStatus.Online,
            ExamServerFooterStatus.Checking -> DiagnosticEventLevel.INFO
            ExamServerFooterStatus.Warning,
            ExamServerFooterStatus.Unstable,
            ExamServerFooterStatus.Insecure -> DiagnosticEventLevel.WARNING
            ExamServerFooterStatus.Offline -> DiagnosticEventLevel.ERROR
        }
}

internal fun safeExamServerHost(examUrl: String): String {
    return runCatching { URL(examUrl).host.orEmpty().trim() }
        .getOrDefault("")
        .ifBlank { "-" }
}

internal fun buildExamServerProbeDetails(
    trigger: String,
    host: String,
    method: String? = null,
    code: Int? = null,
    latencyMs: Long? = null,
    reason: String? = null
): String {
    return buildString {
        append("trigger=").append(trigger)
        append(" | host=").append(host.ifBlank { "-" })
        method?.let { append(" | method=").append(it.ifBlank { "-" }) }
        append(" | code=").append(code?.toString() ?: "-")
        append(" | latency_ms=").append(latencyMs?.toString() ?: "-")
        reason?.let { append(" | reason=").append(it.ifBlank { "-" }) }
    }
}

private fun executeExamServerHttpProbe(
    url: URL,
    method: String
): ExamServerHttpProbeOutcome {
    var connection: HttpURLConnection? = null
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = ExamServerProbeTimeoutMillis
            readTimeout = ExamServerProbeTimeoutMillis
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", ExamServerProbeUserAgent)
            if (method == "GET") {
                setRequestProperty("Range", "bytes=0-0")
            }
        }
        ExamServerHttpProbeOutcome(
            method = method,
            code = connection.responseCode,
            latencyMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L),
            failure = null
        )
    } catch (throwable: Exception) {
        ExamServerHttpProbeOutcome(
            method = method,
            code = null,
            latencyMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L),
            failure = throwable.javaClass.simpleName.ifBlank { "connection_failed" },
            tlsFailure = throwable is SSLException,
            certificateRejected = isCertificateRejection(throwable),
            timedOut = generateSequence(throwable as Throwable) { it.cause }.take(8)
                .any { it is SocketTimeoutException }
        )
    } finally {
        connection?.disconnect()
    }
}

/**
 * Android 7.0's TLS stack cannot complete a handshake with some current certificates
 * (Let's Encrypt P-384 ECDSA, for one) that the exam WebView, which ships its own TLS,
 * loads fine. The HTTPS probe then failed on every round and the footer kept telling the
 * student the server was unreachable while the exam worked. A server that got as far as
 * the TLS handshake is up, so that case is re-checked with a plain TCP connect.
 *
 * Never for a certificate the phone rejected: that fallback used to report expired,
 * self-signed and wrong-host servers Online while the exam page could not open.
 */
private fun executeExamServerTcpProbe(url: URL): ExamServerHttpProbeOutcome {
    val startedAt = SystemClock.elapsedRealtime()
    val port = if (url.port > 0) url.port else url.defaultPort
    return try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(url.host, port), ExamServerProbeTimeoutMillis)
        }
        ExamServerHttpProbeOutcome(
            method = "TCP",
            code = null,
            latencyMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L),
            failure = null,
            reachableWithoutHttp = true
        )
    } catch (throwable: Exception) {
        ExamServerHttpProbeOutcome(
            method = "TCP",
            code = null,
            latencyMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L),
            failure = throwable.javaClass.simpleName.ifBlank { "connection_failed" }
        )
    }
}

internal fun classifyExamServerProbeOutcome(
    host: String,
    outcome: ExamServerHttpProbeOutcome
): ExamServerProbeResult {
    val code = outcome.code
    val status = when {
        // The exam opens anyway (the WebView proceeds past it), with a warning.
        outcome.certificateRejected -> ExamServerFooterStatus.Insecure
        outcome.reachableWithoutHttp ->
            if (outcome.latencyMs > ExamServerProbeSlowThresholdMillis) {
                ExamServerFooterStatus.Warning
            } else {
                ExamServerFooterStatus.Online
            }
        code == null -> ExamServerFooterStatus.Offline
        code in 200..399 || code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN ->
            if (outcome.latencyMs > ExamServerProbeSlowThresholdMillis) {
                ExamServerFooterStatus.Warning
            } else {
                ExamServerFooterStatus.Online
            }
        code in 400..499 -> ExamServerFooterStatus.Warning
        code >= 500 -> ExamServerFooterStatus.Offline
        else -> ExamServerFooterStatus.Warning
    }
    val reason = when {
        outcome.certificateRejected -> "tls_certificate_rejected"
        outcome.reachableWithoutHttp -> "reachable_tls_unsupported"
        code == null -> outcome.failure ?: "connection_failed"
        status == ExamServerFooterStatus.Online -> "reachable"
        outcome.latencyMs > ExamServerProbeSlowThresholdMillis &&
            (code in 200..399 || code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) ->
            "slow_response"
        code in 400..499 -> "http_client_error"
        code >= 500 -> "http_server_error"
        else -> "unexpected_http_status"
    }
    return ExamServerProbeResult(
        status = status,
        host = host,
        method = outcome.method,
        code = code,
        latencyMs = outcome.latencyMs,
        reason = reason
    )
}

internal suspend fun probeExamServerFooterStatus(examUrl: String): ExamServerProbeResult =
    withContext(Dispatchers.IO) {
        val url = runCatching { URL(examUrl) }.getOrNull()
            ?: return@withContext ExamServerProbeResult(
                status = ExamServerFooterStatus.Warning,
                host = "-",
                method = "-",
                code = null,
                latencyMs = null,
                reason = "invalid_exam_url"
            )
        val host = url.host.orEmpty().ifBlank { "-" }
        val headOutcome = executeExamServerHttpProbe(url, method = "HEAD")
        val httpOutcome =
            if (!headOutcome.tlsFailure &&
                (headOutcome.code == null ||
                    headOutcome.code == HttpURLConnection.HTTP_BAD_METHOD ||
                    headOutcome.code == HttpURLConnection.HTTP_NOT_IMPLEMENTED)
            ) {
                executeExamServerHttpProbe(url, method = "GET")
            } else {
                headOutcome
            }
        // A GET would fail the same handshake, so such a TLS failure goes straight to TCP.
        val finalOutcome = if (shouldRetryHandshakeOverTcp(httpOutcome, Build.VERSION.SDK_INT)) {
            executeExamServerTcpProbe(url)
        } else {
            httpOutcome
        }
        val result = classifyExamServerProbeOutcome(host, finalOutcome)
        // Plain http opens too, on a connection anyone on the network can read.
        if (url.protocol.equals("http", ignoreCase = true) && result.status == ExamServerFooterStatus.Online) {
            result.copy(status = ExamServerFooterStatus.Insecure, reason = "reachable_without_https")
        } else {
            result
        }
    }
