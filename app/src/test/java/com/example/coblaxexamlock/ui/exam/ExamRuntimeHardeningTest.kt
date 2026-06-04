package com.example.coblaxexamlock.ui.exam

import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRuntimeHardeningTest {
    @Test
    fun activeExamTrimKeepsWebView() {
        val action = resolveExamRuntimeMemoryAction(
            shouldRespondToPressure = true,
            examSessionStarted = true,
            hasFullscreenCustomView = false
        )

        assertTrue(action.respond)
        assertTrue(action.keepActiveWebView)
        assertFalse(action.cleanupInactiveWebView)
        assertTrue(action.clearUnusedFullscreenContainer)
        assertEquals(
            listOf(
                "clear_warm_location",
                "clear_reverse_engineering_cache",
                "clear_integrity_cache",
                "clear_unused_fullscreen_container",
                "keep_active_webview"
            ),
            action.diagnosticActions()
        )
    }

    @Test
    fun activeExamCriticalTrimKeepsActiveWebViewCacheUntouched() {
        val action = resolveExamRuntimeMemoryAction(
            shouldRespondToPressure = true,
            examSessionStarted = true,
            hasFullscreenCustomView = false
        )

        assertTrue(action.keepActiveWebView)
        assertFalse(action.clearActiveWebViewCache)
        assertFalse(action.diagnosticActions().contains("clear_active_webview_cache"))
    }

    @Test
    fun activeExamTrimNeverDestroysActiveWebView() {
        val action = resolveExamRuntimeMemoryAction(
            shouldRespondToPressure = true,
            examSessionStarted = true,
            hasFullscreenCustomView = false
        )

        assertTrue(action.keepActiveWebView)
        assertFalse(action.cleanupInactiveWebView)
    }

    @Test
    fun webViewGenerationRejectsStaleCallbacks() {
        val activeGeneration = nextExamWebViewGeneration(0L)
        val staleGeneration = activeGeneration - 1L

        assertTrue(isCurrentExamWebViewGeneration(activeGeneration, activeGeneration))
        assertFalse(isCurrentExamWebViewGeneration(staleGeneration, activeGeneration))
    }

    @Test
    fun webViewCleanupRunsOncePerGeneration() {
        val generation = nextExamWebViewGeneration(3L)

        assertTrue(shouldRunExamWebViewCleanup(generation, destroyedGeneration = null))
        assertTrue(shouldRunExamWebViewCleanup(generation, destroyedGeneration = generation - 1L))
        assertFalse(shouldRunExamWebViewCleanup(generation, destroyedGeneration = generation))
    }

    @Test
    fun preparationTrimCanCleanupInactiveWebView() {
        val action = resolveExamRuntimeMemoryAction(
            shouldRespondToPressure = true,
            examSessionStarted = false,
            hasFullscreenCustomView = false
        )

        assertTrue(action.respond)
        assertTrue(action.cleanupInactiveWebView)
        assertFalse(action.keepActiveWebView)
    }

    @Test
    fun fullscreenTrimDoesNotClearActiveFullscreenContainer() {
        val action = resolveExamRuntimeMemoryAction(
            shouldRespondToPressure = true,
            examSessionStarted = true,
            hasFullscreenCustomView = true
        )

        assertFalse(action.clearUnusedFullscreenContainer)
        assertFalse(action.diagnosticActions().contains("clear_unused_fullscreen_container"))
    }

    @Test
    fun ignoredTrimDoesNotCleanupRuntimeState() {
        val action = resolveExamRuntimeMemoryAction(
            shouldRespondToPressure = false,
            examSessionStarted = true,
            hasFullscreenCustomView = false
        )

        assertFalse(action.respond)
        assertFalse(action.keepActiveWebView)
        assertFalse(action.cleanupInactiveWebView)
        assertEquals(listOf("ignore_trim_level"), action.diagnosticActions())
    }

    @Test
    fun exitCleanupDecisionStartsOnlyOnce() {
        assertEquals(
            ExamRuntimeExitCleanupDecision.StartCleanup,
            resolveExamRuntimeExitCleanupDecision(
                ExamRuntimeExitCleanupSnapshot(requested = false, inFlight = false)
            )
        )
        assertEquals(
            ExamRuntimeExitCleanupDecision.JoinInFlight,
            resolveExamRuntimeExitCleanupDecision(
                ExamRuntimeExitCleanupSnapshot(requested = true, inFlight = true)
            )
        )
        assertEquals(
            ExamRuntimeExitCleanupDecision.AlreadyCompleted,
            resolveExamRuntimeExitCleanupDecision(
                ExamRuntimeExitCleanupSnapshot(requested = true, inFlight = false)
            )
        )
    }

    @Test
    fun recoveryStateDocumentsManualSafePath() {
        val states = listOf(
            ExamRuntimeRecoveryState.Idle,
            ExamRuntimeRecoveryState.RendererGone,
            ExamRuntimeRecoveryState.CleanupInFlight,
            ExamRuntimeRecoveryState.ReadyToRetry
        )

        assertEquals(4, states.distinct().size)
    }

    @Test
    fun webViewRendererPriorityStaysImportantWhenInsetsOrImeCoverWindow() {
        val policy = resolveExamWebViewRendererPriorityPolicy()

        assertEquals(WebView.RENDERER_PRIORITY_IMPORTANT, policy.rendererPriority)
        assertFalse(policy.waivedWhenNotVisible)
    }

    @Test
    fun retryUrlUsesRequestedExamUrlBeforeFallback() {
        assertEquals(
            "https://exam.example/start",
            resolveExamWebViewRetryUrl(
                requestedExamUrl = "https://exam.example/start",
                fallbackExamUrl = "https://fallback.example"
            )
        )
    }

    @Test
    fun retryUrlFallsBackWhenRequestedUrlMissing() {
        assertEquals(
            "https://fallback.example",
            resolveExamWebViewRetryUrl(
                requestedExamUrl = " ",
                fallbackExamUrl = "https://fallback.example"
            )
        )
    }
}
