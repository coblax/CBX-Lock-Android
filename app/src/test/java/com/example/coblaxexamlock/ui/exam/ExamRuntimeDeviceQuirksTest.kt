package com.example.coblaxexamlock.ui.exam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRuntimeDeviceQuirksTest {
    @Test
    fun samsungSmT295UsesLegacyTabletPolicy() {
        val profile = resolveExamRuntimeDeviceQuirkProfile(
            manufacturer = "samsung",
            brand = "samsung",
            model = "SM-T295",
            sdkInt = 29
        )

        assertTrue(profile.samsungLegacyTablet)
        assertTrue(profile.allowPartialObscuredWebViewTouch)
        assertEquals(7_000L, profile.screenPinningLostConfirmWindowMillis)
        assertEquals(3_000L, profile.overlayChromeActionSuppressionMillis)
    }

    @Test
    fun normalDeviceKeepsStrictPinningAndWarnOnlyPartialOverlayPolicy() {
        val profile = resolveExamRuntimeDeviceQuirkProfile(
            manufacturer = "Google",
            brand = "google",
            model = "Pixel 8",
            sdkInt = 35
        )

        assertFalse(profile.samsungLegacyTablet)
        assertTrue(profile.allowPartialObscuredWebViewTouch)
        assertEquals(1_500L, profile.overlayFocusLossConfirmWindowMillis)
        assertEquals(2_000L, profile.screenPinningLostConfirmWindowMillis)
    }

    @Test
    fun samsungPartialObscuredWebViewTouchIsWarningOnly() {
        val decision = decideExamOverlayTouch(
            signal = ExamOverlayTouchSignal(
                fullyObscured = false,
                partiallyObscured = true,
                actionMasked = 0
            ),
            profile = resolveExamRuntimeDeviceQuirkProfile(
                manufacturer = "samsung",
                brand = "samsung",
                model = "SM-T295",
                sdkInt = 29
            ),
            source = ExamOverlayTouchSource.WebViewContent,
            elapsedSinceTrustedChromeActionMs = null
        )

        assertEquals(ExamOverlayTouchDecision.WarnAndAllow, decision)
    }

    @Test
    fun genericPartialObscuredWebViewTouchIsWarningOnly() {
        val decision = decideExamOverlayTouch(
            signal = ExamOverlayTouchSignal(
                fullyObscured = false,
                partiallyObscured = true,
                actionMasked = 0
            ),
            profile = resolveExamRuntimeDeviceQuirkProfile(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 8",
                sdkInt = 35
            ),
            source = ExamOverlayTouchSource.WebViewContent,
            elapsedSinceTrustedChromeActionMs = null
        )

        assertEquals(ExamOverlayTouchDecision.WarnAndAllow, decision)
    }

    @Test
    fun fullObscuredTouchStillBlocksOnSamsungLegacyTablet() {
        val decision = decideExamOverlayTouch(
            signal = ExamOverlayTouchSignal(
                fullyObscured = true,
                partiallyObscured = true,
                actionMasked = 0
            ),
            profile = resolveExamRuntimeDeviceQuirkProfile(
                manufacturer = "samsung",
                brand = "samsung",
                model = "SM-T295",
                sdkInt = 29
            ),
            source = ExamOverlayTouchSource.WebViewContent,
            elapsedSinceTrustedChromeActionMs = 250L
        )

        assertEquals(ExamOverlayTouchDecision.BlockAndReport, decision)
    }

    @Test
    fun fullObscuredTouchStillBlocksOnGenericDevice() {
        val decision = decideExamOverlayTouch(
            signal = ExamOverlayTouchSignal(
                fullyObscured = true,
                partiallyObscured = true,
                actionMasked = 0
            ),
            profile = resolveExamRuntimeDeviceQuirkProfile(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 8",
                sdkInt = 35
            ),
            source = ExamOverlayTouchSource.WebViewContent,
            elapsedSinceTrustedChromeActionMs = 250L
        )

        assertEquals(ExamOverlayTouchDecision.BlockAndReport, decision)
    }

    @Test
    fun partialObscuredAfterTrustedChromeActionIsSuppressed() {
        val decision = decideExamOverlayTouch(
            signal = ExamOverlayTouchSignal(
                fullyObscured = false,
                partiallyObscured = true,
                actionMasked = 0
            ),
            profile = resolveExamRuntimeDeviceQuirkProfile(
                manufacturer = "samsung",
                brand = "samsung",
                model = "SM-T295",
                sdkInt = 29
            ),
            source = ExamOverlayTouchSource.WebViewContent,
            elapsedSinceTrustedChromeActionMs = 450L
        )

        assertEquals(ExamOverlayTouchDecision.SuppressAndAllow, decision)
    }

    @Test
    fun windowFocusLossCoveredByAppSwitchIsSuppressed() {
        val decision = decideExamOverlayWindowFocusLoss(
            appSwitchRuntimeMonitoringActive = true,
            pendingForcedExitViolation = true,
            appSwitchLifecycleResumePending = false,
            hasOverlayAppsDetected = false
        )

        assertEquals(ExamOverlayFocusLossDecision.SuppressCoveredByAppSwitch, decision)
    }

    @Test
    fun windowFocusLossWithoutCorroboratingEvidenceIsWarningOnly() {
        val decision = decideExamOverlayWindowFocusLoss(
            appSwitchRuntimeMonitoringActive = true,
            pendingForcedExitViolation = false,
            appSwitchLifecycleResumePending = false,
            hasOverlayAppsDetected = false
        )

        assertEquals(ExamOverlayFocusLossDecision.WarnAndAllow, decision)
    }

    @Test
    fun windowFocusLossWithOverlayAppsDetectedTriggersAlarm() {
        val decision = decideExamOverlayWindowFocusLoss(
            appSwitchRuntimeMonitoringActive = true,
            pendingForcedExitViolation = false,
            appSwitchLifecycleResumePending = false,
            hasOverlayAppsDetected = true
        )

        assertEquals(ExamOverlayFocusLossDecision.TriggerViolationAlarm, decision)
    }

    @Test
    fun trustedChromeSuppressionExpires() {
        val profile = resolveExamRuntimeDeviceQuirkProfile(
            manufacturer = "samsung",
            brand = "samsung",
            model = "SM-T295",
            sdkInt = 29
        )

        val active = resolveExamTrustedChromeActionSuppression(
            profile = profile,
            nowElapsedMs = 10_500L,
            lastTrustedActionElapsedMs = 10_000L,
            lastTrustedActionReason = "webview_refresh"
        )
        val expired = resolveExamTrustedChromeActionSuppression(
            profile = profile,
            nowElapsedMs = 14_000L,
            lastTrustedActionElapsedMs = 10_000L,
            lastTrustedActionReason = "webview_refresh"
        )

        assertEquals("webview_refresh", active?.reason)
        assertEquals(500L, active?.ageMs)
        assertNull(expired)
    }
}
