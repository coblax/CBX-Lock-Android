package com.coblax.examlock.ui.exam

import org.junit.Assert.*
import org.junit.Test

class ExamWebViewNavigationStateTest {
    private val url = "https://exam.example/questions/2"

    @Test
    fun reachableServerCannotHideFailedOrLoadingExamPage() {
        val state = ExamWebViewNavigationState()
        state.start(url)
        assertFalse(state.canApplyServerProbe(state.revision))
        state.fail(url, recoverOnConnection = true)
        assertFalse(state.canApplyServerProbe(state.revision))
        assertFalse(state.finish(url))
        assertFalse(state.canApplyServerProbe(state.revision))
        state.start(url)
        assertTrue(state.finish(url))
        assertTrue(state.canApplyServerProbe(state.revision))
    }

    @Test
    fun probeStartedBeforeNewNavigationCannotOverwriteItsResult() {
        val state = ExamWebViewNavigationState()
        state.start(url)
        assertTrue(state.finish(url))
        val oldProbeRevision = state.revision
        state.start(url)
        assertTrue(state.finish(url))
        assertFalse(state.canApplyServerProbe(oldProbeRevision))
        assertTrue(state.canApplyServerProbe(state.revision))
    }

    @Test
    fun httpAndSslFailuresStayVisibleEvenWhenServerProbeSucceeds() {
        val state = ExamWebViewNavigationState()
        state.start(url)
        state.fail(url, recoverOnConnection = false)
        assertFalse(state.canApplyServerProbe(state.revision))
    }

    @Test
    fun failedPageFinishKeepsErrorAndRetryBudget() {
        val state = ExamWebViewNavigationState()
        state.start(url)
        for (delay in listOf(2_000L, 4_000L, 8_000L)) {
            state.fail(url, recoverOnConnection = true)
            assertFalse(state.finish(url))
            assertEquals(delay, state.nextRetryDelayMillis())
            state.prepareAutomaticRetry()
            state.start(url)
        }
        state.fail(url, recoverOnConnection = true)
        assertFalse(state.finish(url))
        assertNull(state.nextRetryDelayMillis())
        assertEquals(url, state.failedUrl)
    }

    @Test
    fun httpSslAndPostFailuresNeverRetryAutomatically() {
        val state = ExamWebViewNavigationState()
        state.start(url)
        state.fail(url, recoverOnConnection = false)
        assertFalse(state.finish(url))
        assertFalse(state.canRecoverOnConnection)
        assertNull(state.nextRetryDelayMillis())
    }

    @Test
    fun successfulRetryClearsErrorAndNewNavigationGetsFreshBudget() {
        val state = ExamWebViewNavigationState()
        state.start(url)
        state.fail(url, recoverOnConnection = true)
        state.nextRetryDelayMillis()
        state.prepareAutomaticRetry()
        state.start(url)
        assertTrue(state.finish(url))
        assertNull(state.failedUrl)
        assertFalse(state.canRecoverOnConnection)
        state.start("$url/next")
        state.fail("$url/next", recoverOnConnection = true)
        assertEquals(2_000L, state.nextRetryDelayMillis())
    }

    @Test
    fun staleAndSyntheticFinishesCannotMarkExamOnline() {
        val state = ExamWebViewNavigationState()
        state.start(url)
        assertFalse(state.finish("https://exam.example/login"))
        assertFalse(state.finish("about:blank"))
        assertFalse(state.finish("data:text/html,error"))
        assertFalse(state.finish(null))
        assertTrue(state.finish(url))
    }

    @Test
    fun onlyConnectionErrorsAreEligibleForRecovery() {
        assertTrue(isRecoverableExamConnectionError("net::ERR_INTERNET_DISCONNECTED"))
        assertTrue(isRecoverableExamConnectionError("net::ERR_CONNECTION_RESET"))
        for (error in listOf("ERR_CERT_AUTHORITY_INVALID", "ERR_SSL_PROTOCOL_ERROR",
            "ERR_CACHE_MISS", "ERR_ABORTED", "ERR_BLOCKED_BY_RESPONSE", "HTTP 403")) {
            assertFalse(error, isRecoverableExamConnectionError(error))
        }
    }

    @Test
    fun transientNetworkDropsAreEligibleForRecovery() {
        // Reproduced live: a throttled connection made WebView return
        // net::ERR_SOCKET_NOT_CONNECTED while the OS still reported the network as
        // connected. These clear on reload, so they must auto-retry rather than
        // dead-ending on a "check your connection" page.
        for (error in listOf(
            "net::ERR_SOCKET_NOT_CONNECTED", "net::ERR_EMPTY_RESPONSE",
            "net::ERR_CONTENT_LENGTH_MISMATCH", "net::ERR_INCOMPLETE_CHUNKED_ENCODING",
            "net::ERR_RESPONSE_HEADERS_TRUNCATED", "net::ERR_HTTP2_PING_FAILED",
            "net::ERR_QUIC_PROTOCOL_ERROR"
        )) {
            assertTrue(error, isRecoverableExamConnectionError(error))
        }
    }

    @Test
    fun localizedConnectionErrorsUseStableWebViewErrorCodes() {
        for (code in listOf(-2, -6, -7, -8)) {
            assertTrue(isRecoverableExamConnectionError("Koneksi gagal", code))
        }
        for (code in listOf(-1, -4, -5, -9, -10, -11, -12)) {
            assertFalse(isRecoverableExamConnectionError("Halaman gagal", code))
        }
    }
}
