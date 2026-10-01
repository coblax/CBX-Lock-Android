package com.coblax.examlock.ui.exam

import org.junit.Assert.assertEquals
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
}
