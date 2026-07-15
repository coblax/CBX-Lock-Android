package com.coblax.examlock.ui.exam

import android.os.SystemClock
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.model.DiagnosticEventLevel
import java.net.HttpURLConnection
import java.net.URL
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

private data class ExamServerHttpProbeOutcome(
    val method: String,
    val code: Int?,
    val latencyMs: Long,
    val failure: String?
)

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
        }

    val eventLevel: DiagnosticEventLevel
        get() = when (status) {
            ExamServerFooterStatus.Online,
            ExamServerFooterStatus.Checking -> DiagnosticEventLevel.INFO
            ExamServerFooterStatus.Warning,
            ExamServerFooterStatus.Unstable -> DiagnosticEventLevel.WARNING
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
            failure = throwable.javaClass.simpleName.ifBlank { "connection_failed" }
        )
    } finally {
        connection?.disconnect()
    }
}

private fun classifyExamServerProbeOutcome(
    host: String,
    outcome: ExamServerHttpProbeOutcome
): ExamServerProbeResult {
    val code = outcome.code
    val status = when {
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
        val finalOutcome =
            if (headOutcome.code == null ||
                headOutcome.code == HttpURLConnection.HTTP_BAD_METHOD ||
                headOutcome.code == HttpURLConnection.HTTP_NOT_IMPLEMENTED
            ) {
                executeExamServerHttpProbe(url, method = "GET")
            } else {
                headOutcome
            }
        classifyExamServerProbeOutcome(host, finalOutcome)
    }
