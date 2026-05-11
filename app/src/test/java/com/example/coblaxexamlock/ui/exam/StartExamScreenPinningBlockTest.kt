package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.model.UiLanguage
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
            screenPinningActive = false,
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
            screenPinningActive = true,
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
            screenPinningActive = false,
            accessibilityGuardAvailable = false,
            accessibilityGuardEnabled = false
        )

        assertNull(block)
    }
}
