package com.coblax.examlock.ui.exam

import com.coblax.examlock.ExamLockTaskState
import com.coblax.examlock.LockTaskSecurityRequirement
import com.coblax.examlock.ScreenPinningMode
import com.coblax.examlock.model.UiLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartExamScreenPinningBlockTest {
    @Test
    fun enforcedSupportedInactivePinningBlocksStartExam() {
        val block = resolveStartExamScreenPinningBlockMessage(
            uiLanguage = UiLanguage.English,
            screenPinningMode = ScreenPinningMode.Enforced,
            screenPinningAvailable = true,
            lockTaskState = ExamLockTaskState.None,
            lockTaskRequirement = LockTaskSecurityRequirement.AnyActive,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertEquals(ExamRuntimeHardeningDiagnostics.StartExamBlockedScreenPinningInactive, block?.code)
        assertEquals("Start Screen Pinning First", block?.title)
    }

    @Test
    fun enforcedSupportedActivePinningAllowsStartExam() {
        val block = resolveStartExamScreenPinningBlockMessage(
            uiLanguage = UiLanguage.English,
            screenPinningMode = ScreenPinningMode.Enforced,
            screenPinningAvailable = true,
            lockTaskState = ExamLockTaskState.Pinned,
            lockTaskRequirement = LockTaskSecurityRequirement.AnyActive,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertNull(block)
    }

    @Test
    fun bypassModeAllowsStartExamWithoutActivePinning() {
        val block = resolveStartExamScreenPinningBlockMessage(
            uiLanguage = UiLanguage.English,
            screenPinningMode = ScreenPinningMode.Bypassed,
            screenPinningAvailable = true,
            lockTaskState = ExamLockTaskState.None,
            lockTaskRequirement = LockTaskSecurityRequirement.Locked,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertNull(block)
    }

    @Test
    fun managedDeviceRejectsPinnedModeAndRequiresLockedKiosk() {
        val block = resolveStartExamScreenPinningBlockMessage(
            uiLanguage = UiLanguage.English,
            screenPinningMode = ScreenPinningMode.Enforced,
            screenPinningAvailable = true,
            lockTaskState = ExamLockTaskState.Pinned,
            lockTaskRequirement = LockTaskSecurityRequirement.Locked,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertEquals(ExamRuntimeHardeningDiagnostics.StartExamBlockedManagedLockTaskNotLocked, block?.code)
        assertEquals("Managed Kiosk Mode Required", block?.title)
    }

    @Test
    fun managedDeviceAllowsOnlyLockedKioskMode() {
        val block = resolveStartExamScreenPinningBlockMessage(
            uiLanguage = UiLanguage.English,
            screenPinningMode = ScreenPinningMode.Enforced,
            screenPinningAvailable = true,
            lockTaskState = ExamLockTaskState.Locked,
            lockTaskRequirement = LockTaskSecurityRequirement.Locked,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertNull(block)
    }
}
