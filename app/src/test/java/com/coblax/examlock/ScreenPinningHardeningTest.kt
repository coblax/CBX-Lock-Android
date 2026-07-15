package com.coblax.examlock

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenPinningHardeningTest {
    @Test
    fun lockTaskStartIsSkippedWhenAlreadyActive() {
        assertFalse(
            shouldStartExamLockTask(
                enabled = true,
                allowLockTask = true,
                lockTaskAlreadyActive = true
            )
        )
    }

    @Test
    fun lockTaskStartRequiresEnabledAllowedAndInactive() {
        assertTrue(
            shouldStartExamLockTask(
                enabled = true,
                allowLockTask = true,
                lockTaskAlreadyActive = false
            )
        )
        assertFalse(
            shouldStartExamLockTask(
                enabled = true,
                allowLockTask = false,
                lockTaskAlreadyActive = false
            )
        )
        assertFalse(
            shouldStartExamLockTask(
                enabled = false,
                allowLockTask = true,
                lockTaskAlreadyActive = false
            )
        )
    }

    @Test
    fun lockTaskStopOnlyRunsWhenDisablingActiveLockTask() {
        assertTrue(shouldStopExamLockTask(enabled = false, lockTaskAlreadyActive = true))
        assertFalse(shouldStopExamLockTask(enabled = false, lockTaskAlreadyActive = false))
        assertFalse(shouldStopExamLockTask(enabled = true, lockTaskAlreadyActive = true))
    }

    @Test
    fun pinningEngageAttemptIsSingleShotAndSkippedWhenActive() {
        assertTrue(
            shouldIssueScreenPinningEngageAttempt(
                lockTaskAlreadyActive = false,
                engageAttemptCount = 0
            )
        )
        assertFalse(
            shouldIssueScreenPinningEngageAttempt(
                lockTaskAlreadyActive = false,
                engageAttemptCount = 1
            )
        )
        assertFalse(
            shouldIssueScreenPinningEngageAttempt(
                lockTaskAlreadyActive = true,
                engageAttemptCount = 0
            )
        )
    }

    @Test
    fun pinningActivationStateKnowsPendingStates() {
        assertTrue(PinningActivationState.Requested.isPending())
        assertTrue(PinningActivationState.WaitingForSystemDialog.isPending())
        assertTrue(PinningActivationState.WaitingForLockTaskActive.isPending())
        assertFalse(PinningActivationState.Idle.isPending())
        assertFalse(PinningActivationState.ActiveConfirmed.isPending())
        assertFalse(PinningActivationState.TimeoutRetryReady.isPending())
    }

    @Test
    fun pinningTransitionViolationIsSuppressedInsideGraceWindow() {
        assertTrue(
            shouldSuppressPinningTransitionViolation(
                lockTaskRequestPending = true,
                examSessionStarted = false,
                startedAtElapsedMs = 1_000L,
                nowElapsedMs = 12_999L
            )
        )
        assertFalse(
            shouldSuppressPinningTransitionViolation(
                lockTaskRequestPending = true,
                examSessionStarted = false,
                startedAtElapsedMs = 1_000L,
                nowElapsedMs = 13_001L
            )
        )
        assertFalse(
            shouldSuppressPinningTransitionViolation(
                lockTaskRequestPending = true,
                examSessionStarted = true,
                startedAtElapsedMs = 1_000L,
                nowElapsedMs = 2_000L
            )
        )
    }

    @Test
    fun screenPinningDialogRejectionRequiresFocusLossReturnAndInactiveLockTask() {
        assertTrue(
            shouldTreatScreenPinningDialogAsRejected(
                dialogLikelyShown = true,
                dialogFocusLossObserved = true,
                windowHasFocus = true,
                lockTaskActive = false
            )
        )
        assertFalse(
            shouldTreatScreenPinningDialogAsRejected(
                dialogLikelyShown = true,
                dialogFocusLossObserved = false,
                windowHasFocus = true,
                lockTaskActive = false
            )
        )
        assertFalse(
            shouldTreatScreenPinningDialogAsRejected(
                dialogLikelyShown = true,
                dialogFocusLossObserved = true,
                windowHasFocus = false,
                lockTaskActive = false
            )
        )
        assertFalse(
            shouldTreatScreenPinningDialogAsRejected(
                dialogLikelyShown = true,
                dialogFocusLossObserved = true,
                windowHasFocus = true,
                lockTaskActive = true
            )
        )
    }

    @Test
    fun activationDoesNotEngageWhenLockTaskAlreadyActive() = runBlocking {
        val bridge = FakeLockTaskBridge(active = true, stateLabel = "PINNED")

        val report = ScreenPinningEnforcer.requestAndAwaitActivation(
            bridge = bridge,
            isIndonesian = true
        )

        assertTrue(report.active)
        assertEquals("PINNED", report.afterState)
        assertFalse(report.dialogLikelyShown)
        assertEquals(0L, report.activationDurationMs)
        assertEquals(0, bridge.engageCount)
    }

    private class FakeLockTaskBridge(
        private var active: Boolean,
        private val stateLabel: String
    ) : LockTaskBridge {
        var engageCount: Int = 0

        override fun engage(allowLockTask: Boolean) {
            engageCount += 1
            active = true
        }

        override fun disengage() {
            active = false
        }

        override fun active(): Boolean = active

        override fun stateLabel(): String = stateLabel
    }
}
