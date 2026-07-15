package com.coblax.examlock.ui.admin

import com.coblax.examlock.DeviceCompatibilityProfile
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.DeviceTimeSecurityVerdict
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.OverlayQuickFixTarget
import com.coblax.examlock.OverlayRiskResult
import com.coblax.examlock.OverlayShieldStatus
import com.coblax.examlock.OverlaySignal
import com.coblax.examlock.resolveDeviceCompatibilityProfile
import com.coblax.examlock.resolveWebViewCompatibilityStatus
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.ExamNetworkStatus
import com.coblax.examlock.model.NetworkDiagnostics
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkReadinessUserVerdict
import com.coblax.examlock.model.NetworkReadinessVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldReadinessTest {
    @Test
    fun healthyDeviceIsReady() {
        val report = buildFieldReadinessReport(defaultInput())

        assertEquals(FieldReadinessVerdict.Ready, report.finalVerdict)
        assertEquals(0, report.blockedCount)
        assertEquals(0, report.warningCount)
    }

    @Test
    fun missingWebViewProviderBlocksFieldTrial() {
        val report = buildFieldReadinessReport(
            defaultInput(
                webViewAvailable = false
            )
        )

        assertEquals(FieldReadinessVerdict.Blocked, report.finalVerdict)
        assertTrue(report.items.any {
            it.category == FieldReadinessCategory.WebView &&
                it.verdict == FieldReadinessVerdict.Blocked
        })
    }

    @Test
    fun oldWebViewProviderProducesWarningOnly() {
        val report = buildFieldReadinessReport(
            defaultInput(
                webViewVersion = "74.0.3729.186"
            )
        )

        assertEquals(FieldReadinessVerdict.Warning, report.finalVerdict)
        assertTrue(report.items.any {
            it.category == FieldReadinessCategory.WebView &&
                it.verdict == FieldReadinessVerdict.Warning
        })
    }

    @Test
    fun adminReadinessWithoutReportIsNeutralWhenWebViewReady() {
        val summary = buildAdminReadinessSummary(
            report = null,
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "120.0.0.0"
            ),
            vendorChecklist = resolveDeviceVendorChecklist(
                manufacturer = "Samsung",
                brand = "samsung"
            )
        )

        assertEquals(AdminReadinessVerdict.NotRun, summary.verdict)
        assertEquals(0, summary.blockedCount)
        assertEquals("Ready", summary.webViewLabel)
        assertEquals("Samsung", summary.vendorLabel)
    }

    @Test
    fun adminReadinessUnavailableWebViewBlocksEvenBeforeFieldReport() {
        val summary = buildAdminReadinessSummary(
            report = null,
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = null,
                versionName = null
            ),
            vendorChecklist = resolveDeviceVendorChecklist(
                manufacturer = "Android",
                brand = "generic"
            )
        )

        assertEquals(AdminReadinessVerdict.Blocked, summary.verdict)
        assertEquals(1, summary.blockedCount)
        assertEquals("Unavailable", summary.webViewLabel)
    }

    @Test
    fun adminReadinessOldWebViewNeedsSetupButDoesNotBlock() {
        val summary = buildAdminReadinessSummary(
            report = null,
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "74.0.3729.186"
            ),
            vendorChecklist = resolveDeviceVendorChecklist(
                manufacturer = "Xiaomi",
                brand = "redmi"
            )
        )

        assertEquals(AdminReadinessVerdict.NeedsSetup, summary.verdict)
        assertEquals(0, summary.blockedCount)
        assertEquals(1, summary.warningCount)
        assertEquals("Need Update", summary.webViewLabel)
    }

    @Test
    fun adminReadinessHealthyFieldReportIsReady() {
        val report = buildFieldReadinessReport(defaultInput())
        val summary = buildAdminReadinessSummary(
            report = report,
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "120.0.0.0"
            ),
            vendorChecklist = resolveDeviceVendorChecklist(
                manufacturer = "Android",
                brand = "generic"
            )
        )

        assertEquals(AdminReadinessVerdict.Ready, summary.verdict)
        assertEquals(0, summary.blockedCount)
        assertEquals(0, summary.warningCount)
    }

    @Test
    fun missingScreenPinningAndDisabledAccessibilityGuardBlocks() {
        val report = buildFieldReadinessReport(
            defaultInput(
                screenPinningAvailable = false,
                accessibilityGuardAvailable = true,
                accessibilityGuardEnabled = false
            )
        )

        assertEquals(FieldReadinessVerdict.Blocked, report.finalVerdict)
        assertTrue(report.items.any {
            it.category == FieldReadinessCategory.ScreenPinning &&
                it.verdict == FieldReadinessVerdict.Blocked
        })
    }

    @Test
    fun samsungLegacyAddsWarningAndDocumentsPartialOverlayPolicy() {
        val report = buildFieldReadinessReport(
            defaultInput(
                compatibilityProfile = resolveDeviceCompatibilityProfile(
                    manufacturer = "Samsung",
                    brand = "samsung",
                    model = "SM-T295",
                    sdkInt = 29,
                    lowRamProfile = LowRamProfile(enabled = true, severe = true)
                )
            )
        )

        assertEquals(FieldReadinessVerdict.Warning, report.finalVerdict)
        assertTrue(report.items.any {
            it.category == FieldReadinessCategory.SamsungLegacy &&
                it.detail.contains("partial overlays", ignoreCase = true)
        })
    }

    private fun defaultInput(
        compatibilityProfile: DeviceCompatibilityProfile = DeviceCompatibilityProfile(),
        screenPinningAvailable: Boolean = true,
        accessibilityGuardAvailable: Boolean = false,
        accessibilityGuardEnabled: Boolean = false,
        webViewAvailable: Boolean = true,
        webViewVersion: String = "120.0.0.0"
    ): FieldReadinessInput {
        return FieldReadinessInput(
            generatedAt = "2026-05-05T00:00:00Z",
            compatibilityProfile = compatibilityProfile,
            screenPinningAvailable = screenPinningAvailable,
            screenPinningSystemSetting = "Aktif",
            lockTaskState = "NONE",
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            overlayRiskResult = defaultOverlayRisk(),
            webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                packageName = if (webViewAvailable) "com.android.webview" else null,
                versionName = if (webViewAvailable) webViewVersion else null
            ),
            networkReadinessStatus = defaultNetwork(),
            batteryStatus = ExamBatteryStatus(levelPercent = 80, isCharging = true),
            locationPermissionGranted = true,
            preciseLocationGranted = true,
            locationServicesEnabled = true,
            geofencePolicyEnabled = false,
            fakeLocationMonitoringEnabled = false,
            deviceTimeSecurityStatus = defaultDeviceTimeStatus()
        )
    }

    private fun defaultOverlayRisk(): OverlayRiskResult =
        OverlayRiskResult(
            bypassed = false,
            confirmedInteractionDetected = false,
            heuristicRisk = false,
            accessibilityEnabled = false,
            riskyAccessibilityPackages = emptyList(),
            violationCount = 0,
            signals = emptySet(),
            quickFixTargets = emptySet<OverlayQuickFixTarget>(),
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

    private fun defaultNetwork(): NetworkReadinessStatus =
        NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = "Online",
                detail = "Connected",
                isConnected = true
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = true,
                transports = listOf("wifi"),
                hasInternetCapability = true,
                isValidated = true,
                isCaptivePortal = false,
                isMetered = false,
                isVpnActive = false,
                isAirplaneModeEnabled = false,
                notRoaming = true,
                interfaceName = "wlan0",
                wifi = null,
                cellular = null
            ),
            verdict = NetworkReadinessVerdict.ConnectedStable,
            transportLabel = "Wi-Fi",
            quickFixReason = null,
            userFacingVerdict = NetworkReadinessUserVerdict.Stable,
            userFacingQuickFixText = null
        )

    private fun defaultDeviceTimeStatus(): DeviceTimeSecurityStatus =
        DeviceTimeSecurityStatus(
            autoTimeEnabled = true,
            autoTimeZoneEnabled = true,
            clockDriftDetected = false,
            clockDriftMillis = 0,
            timezoneSummary = "UTC",
            wallClockNowMillis = 1_000L,
            elapsedNowMillis = 1_000L,
            baselineWallClockMillis = 1_000L,
            baselineElapsedRealtimeMillis = 1_000L,
            bypassState = DeviceTimeBypassState.Inactive,
            finalVerdict = DeviceTimeSecurityVerdict.Safe
        )
}
