package com.example.coblaxexamlock.ui.exam

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
}
