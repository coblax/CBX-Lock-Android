package com.coblax.examlock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayHardeningTest {
    @Test
    fun heuristicOnlyOverlayRiskDoesNotBlockExamStart() {
        val result = OverlayRiskAnalyzer.inspect(
            bypassed = false,
            accessibilityEnabled = true,
            riskyAccessibilityPackages = listOf("com.example.autoclicker"),
            violationCount = 0,
            shieldStatus = OverlayShieldStatus(
                supported = true,
                requested = true,
                lastApplySucceeded = true,
                lastApplyAt = null
            ),
            lastTrigger = null,
            lastDetectedAt = null,
            lastContext = null
        )

        assertTrue(result.hasAnyRisk)
        assertTrue(result.heuristicRisk)
        assertFalse(result.confirmedInteractionDetected)
        assertFalse(result.hasBlockingRisk)
    }

    @Test
    fun confirmedOverlayInteractionStillBlocksExamStart() {
        val result = OverlayRiskAnalyzer.inspect(
            bypassed = false,
            accessibilityEnabled = false,
            riskyAccessibilityPackages = emptyList(),
            violationCount = 1,
            shieldStatus = OverlayShieldStatus(
                supported = true,
                requested = true,
                lastApplySucceeded = true,
                lastApplyAt = null
            ),
            lastTrigger = OverlaySignal.ObscuredTouch.diagnosticLabel(),
            lastDetectedAt = "now",
            lastContext = "fully_obscured=yes"
        )

        assertTrue(result.confirmedInteractionDetected)
        assertTrue(result.hasBlockingRisk)
    }

    @Test
    fun legacyWindowFocusLossHistoryDoesNotBlockExamStart() {
        val result = OverlayRiskAnalyzer.inspect(
            bypassed = false,
            accessibilityEnabled = false,
            riskyAccessibilityPackages = emptyList(),
            violationCount = 3,
            shieldStatus = OverlayShieldStatus(
                supported = true,
                requested = true,
                lastApplySucceeded = false,
                lastApplyAt = "now"
            ),
            lastTrigger = OverlaySignal.WindowFocusLoss.diagnosticLabel(),
            lastDetectedAt = "now",
            lastContext = "trigger=window_focus_loss | app_switch_pending=no"
        )

        assertFalse(result.confirmedInteractionDetected)
        assertFalse(result.hasBlockingRisk)
    }
}
