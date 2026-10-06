package com.coblax.examlock.ui.exam

import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateExpiredException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamServerProbeClassificationTest {

    private fun classify(outcome: ExamServerHttpProbeOutcome) =
        classifyExamServerProbeOutcome(host = "exam.example", outcome = outcome)

    @Test
    fun serverReachedOverTcpAfterAnUnsupportedHandshakeIsOnline() {
        // Android 7.0 cannot handshake with some certificates the exam WebView loads fine.
        val result = classify(
            ExamServerHttpProbeOutcome(
                method = "TCP",
                code = null,
                latencyMs = 300L,
                failure = null,
                reachableWithoutHttp = true
            )
        )
        assertEquals(ExamServerFooterStatus.Online, result.status)
        assertEquals("reachable_tls_unsupported", result.reason)
    }

    @Test
    fun slowTcpFallbackIsAWarning() {
        val result = classify(
            ExamServerHttpProbeOutcome(
                method = "TCP",
                code = null,
                latencyMs = 9_000L,
                failure = null,
                reachableWithoutHttp = true
            )
        )
        assertEquals(ExamServerFooterStatus.Warning, result.status)
    }

    @Test
    fun tcpFallbackThatAlsoFailsIsOffline() {
        val result = classify(
            ExamServerHttpProbeOutcome(
                method = "TCP",
                code = null,
                latencyMs = 12_000L,
                failure = "SocketTimeoutException"
            )
        )
        assertEquals(ExamServerFooterStatus.Offline, result.status)
        assertEquals("SocketTimeoutException", result.reason)
    }

    @Test
    fun httpAnswersKeepTheirMeaning() {
        assertEquals(
            ExamServerFooterStatus.Online,
            classify(ExamServerHttpProbeOutcome("HEAD", 200, 200L, null)).status
        )
        assertEquals(
            ExamServerFooterStatus.Offline,
            classify(ExamServerHttpProbeOutcome("GET", 503, 200L, null)).status
        )
        assertEquals(
            ExamServerFooterStatus.Offline,
            classify(ExamServerHttpProbeOutcome("GET", null, 200L, "UnknownHostException")).status
        )
    }

    /** The exam opens past the certificate problem, flagged as unprotected: never plain Online. */
    @Test
    fun aCertificateThePhoneRejectsIsInsecureNotOnline() {
        val result = classify(
            ExamServerHttpProbeOutcome(
                method = "HEAD",
                code = null,
                latencyMs = 400L,
                failure = "SSLHandshakeException",
                tlsFailure = true,
                certificateRejected = true
            )
        )
        assertEquals(ExamServerFooterStatus.Insecure, result.status)
        assertEquals("tls_certificate_rejected", result.reason)
    }

    @Test
    fun certificateRejectionsAreToldApartFromUnsupportedHandshakes() {
        fun handshake(cause: Throwable?) = SSLHandshakeException("handshake failed").apply { initCause(cause) }
        // Expired, self-signed or unknown issuer, and issued for another host.
        assertTrue(isCertificateRejection(handshake(CertificateExpiredException("expired"))))
        assertTrue(
            isCertificateRejection(
                handshake(java.security.cert.CertificateException(CertPathValidatorException("no anchor")))
            )
        )
        assertTrue(isCertificateRejection(SSLPeerUnverifiedException("Hostname not verified")))
        // Android 7.0 failing to negotiate a P-384 certificate: the TCP fallback still applies.
        assertFalse(isCertificateRejection(handshake(javax.net.ssl.SSLProtocolException("SSLV3_ALERT_HANDSHAKE_FAILURE"))))
        assertFalse(isCertificateRejection(java.net.SocketTimeoutException("timeout")))
    }

    @Test
    fun onlyAnAndroid7HandshakeFailureIsRetriedOverTcp() {
        val unsupportedHandshake = ExamServerHttpProbeOutcome(
            method = "HEAD",
            code = null,
            latencyMs = 300L,
            failure = "SSLHandshakeException",
            tlsFailure = true
        )
        assertTrue(shouldRetryHandshakeOverTcp(unsupportedHandshake, sdkInt = 24))
        assertTrue(shouldRetryHandshakeOverTcp(unsupportedHandshake, sdkInt = 25))
        assertFalse(shouldRetryHandshakeOverTcp(unsupportedHandshake, sdkInt = 28))
        assertFalse(shouldRetryHandshakeOverTcp(unsupportedHandshake.copy(timedOut = true), sdkInt = 24))
        assertFalse(shouldRetryHandshakeOverTcp(unsupportedHandshake.copy(certificateRejected = true), sdkInt = 24))
        assertFalse(shouldRetryHandshakeOverTcp(unsupportedHandshake.copy(tlsFailure = false), sdkInt = 24))
    }
}
