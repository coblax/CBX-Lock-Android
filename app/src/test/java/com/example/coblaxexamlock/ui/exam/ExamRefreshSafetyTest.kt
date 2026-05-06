package com.example.coblaxexamlock.ui.exam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRefreshSafetyTest {
    @Test
    fun activePinnedExamReloadsWithoutRequestingLockTask() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = true,
            lockTaskAlreadyActive = true,
            lockTaskRequestPending = false
        )

        assertEquals(ExamRefreshSafetyOutcome.SafeReloadOnly, decision.outcome)
        assertTrue(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshSafeLockTaskSkipped, decision.eventCode)
    }

    @Test
    fun refreshWaitsWhenPinningRequestAlreadyPending() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = true,
            lockTaskAlreadyActive = false,
            lockTaskRequestPending = true
        )

        assertEquals(ExamRefreshSafetyOutcome.BlockedPinningPending, decision.outcome)
        assertFalse(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshPinningPendingBlocked, decision.eventCode)
    }

    @Test
    fun refreshDoesNotTryToRecoverLostPinningByRequestingAgain() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = true,
            lockTaskAlreadyActive = false,
            lockTaskRequestPending = false
        )

        assertEquals(ExamRefreshSafetyOutcome.BlockedPinningInactive, decision.outcome)
        assertFalse(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshPinningInactiveBlocked, decision.eventCode)
    }

    @Test
    fun bypassedPinningExamCanReloadWithoutLockTask() {
        val decision = resolveExamRefreshSafetyDecision(
            examSessionStarted = true,
            screenPinningEnforced = false,
            lockTaskAlreadyActive = false,
            lockTaskRequestPending = false
        )

        assertEquals(ExamRefreshSafetyOutcome.SafeReloadOnly, decision.outcome)
        assertTrue(decision.allowWebViewReload)
        assertFalse(decision.shouldRequestLockTask)
        assertEquals(ExamRuntimeHardeningDiagnostics.ExamRefreshCompleted, decision.eventCode)
    }
}
