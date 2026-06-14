package com.example.coblaxexamlock.ui.preparation

import android.view.Display
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.evaluateDeviceTimeSecurityStatus
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.NetworkDiagnostics
import com.example.coblaxexamlock.model.NetworkDnsProbeStatus
import com.example.coblaxexamlock.model.NetworkDnsProbeVerdict
import com.example.coblaxexamlock.model.NetworkLatencyBucket
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.resolveWebViewCompatibilityStatus
import com.example.coblaxexamlock.runtime.ExternalDisplayInfo
import com.example.coblaxexamlock.runtime.MultiWindowModeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationChecklistAuditDetailsTest {
    @Test
    fun appendAuditDetailPreservesQuickFixWhenAuditHidden() {
        val detail = appendPreparationAuditDetail(
            actionDetail = "Turn off VPN, then tap Refresh.",
            auditDetail = null
        )

        assertEquals("Turn off VPN, then tap Refresh.", detail)
        assertFalse(detail.orEmpty().contains("Technical details"))
    }

    @Test
    fun networkAuditIncludesVpnAndDnsDiagnostics() {
        val detail = buildPreparationNetworkAuditDetail(
            status = networkStatus(),
            unstableStatus = NetworkUnstableRuntimeStatus(
                unstableActive = true,
                episodeStartedAt = "2026-05-11 12:00:00",
                flapCount = 2,
                lastFlapAt = "2026-05-11 12:01:00",
                warningShown = true,
                lastWarningAt = "2026-05-11 12:01:30",
                lastTransportLabel = "Wi-Fi"
            ),
            lastNetworkChangeAt = "2026-05-11 12:01:00",
            lastNetworkChangeSource = "callback",
            lastConnectedNetworkLabel = "Wi-Fi",
            bypassVpn = false,
            vpnBypassState = VpnBypassState.Tampered,
            isRefreshingNetwork = true,
            uiLanguage = UiLanguage.English
        )

        assertTrue(detail.contains("Readiness verdict: VpnActive"))
        assertTrue(detail.contains("User verdict: VpnActive"))
        assertTrue(detail.contains("VPN active: Yes"))
        assertTrue(detail.contains("VPN bypass tampered: Yes"))
        assertTrue(detail.contains("Global DNS probe: Resolved"))
        assertTrue(detail.contains("Exam DNS probe: Failed"))
        assertTrue(detail.contains("Network changes/flaps: 2"))
    }

    @Test
    fun webViewAuditIncludesProviderAndSessionState() {
        val detail = buildPreparationWebViewAuditDetail(
            status = resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "89.0.4389.105",
                providerSource = "current_provider"
            ),
            webViewSessionResetInFlight = true,
            webViewSessionResetError = "renderer_gone",
            uiLanguage = UiLanguage.English
        )

        assertTrue(detail.contains("Verdict: NeedsUpdate"))
        assertTrue(detail.contains("Severity: Warning"))
        assertTrue(detail.contains("Package: com.android.webview"))
        assertTrue(detail.contains("Version: 89.0.4389.105"))
        assertTrue(detail.contains("Session reset running: Yes"))
        assertTrue(detail.contains("Session reset error: renderer_gone"))
    }

    @Test
    fun deviceTimeAuditIncludesAutomaticSettingsAndClockBaseline() {
        val detail = buildPreparationDeviceTimeAuditDetail(
            status = evaluateDeviceTimeSecurityStatus(
                autoTimeEnabled = false,
                autoTimeZoneEnabled = true,
                baseline = DeviceTimeBaseline(
                    wallClockMillis = 1_000L,
                    elapsedRealtimeMillis = 500L
                ),
                bypassState = DeviceTimeBypassState.Inactive,
                timezoneSummary = "Asia/Jakarta",
                nowWallClockMillis = 10_000L,
                nowElapsedRealtimeMillis = 2_000L
            ),
            uiLanguage = UiLanguage.English
        )

        assertTrue(detail.contains("Final verdict: AutoTimeDisabled"))
        assertTrue(detail.contains("Automatic date/time: No"))
        assertTrue(detail.contains("Automatic time zone: Yes"))
        assertTrue(detail.contains("Timezone: Asia/Jakarta"))
        assertTrue(detail.contains("Baseline wall clock: 1000"))
        assertTrue(detail.contains("Blocking now: Yes"))
    }

    @Test
    fun staticSecurityAuditDetailsIncludeEmptyAndDetectedStates() {
        val screenRecorder = buildPreparationScreenRecorderAuditDetail(
            screenRecorderPackages = emptyList(),
            bypassScreenRecorder = false,
            uiLanguage = UiLanguage.English
        )
        val displayMirror = buildPreparationDisplayMirrorAuditDetail(
            externalDisplayDetected = true,
            externalDisplayCount = 1,
            externalDisplayInfoList = listOf(
                ExternalDisplayInfo(
                    displayId = 7,
                    name = "HDMI Display",
                    state = 2,
                    flags = Display.FLAG_SECURE or Display.FLAG_PRESENTATION
                )
            ),
            bypassDisplayMirror = false,
            uiLanguage = UiLanguage.English
        )
        val multiWindow = buildPreparationMultiWindowAuditDetail(
            modeInfo = MultiWindowModeInfo(
                multiWindowApiSupported = true,
                pictureInPictureApiSupported = true,
                inMultiWindowMode = false,
                inPictureInPictureMode = true
            ),
            runtimeDetected = true,
            bypassMultiWindow = false,
            uiLanguage = UiLanguage.English
        )

        assertTrue(screenRecorder.contains("No visible recorder package detected: Yes"))
        assertTrue(screenRecorder.contains("Visible recorder package count: 0"))
        assertTrue(displayMirror.contains("External display [0]: id=7"))
        assertTrue(displayMirror.contains("state=ON"))
        assertTrue(displayMirror.contains("flags=SECURE|PRESENTATION"))
        assertTrue(multiWindow.contains("isInMultiWindowMode: No"))
        assertTrue(multiWindow.contains("isInPictureInPictureMode: Yes"))
        assertTrue(multiWindow.contains("isInAnySplitMode: Yes"))
    }

    private fun networkStatus(): NetworkReadinessStatus {
        return NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = "VPN",
                detail = "VPN active",
                isConnected = true,
                cellularProvider = null
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = true,
                transports = listOf("VPN", "WIFI"),
                hasInternetCapability = true,
                isValidated = true,
                isCaptivePortal = false,
                isMetered = false,
                isVpnActive = true,
                isAirplaneModeEnabled = false,
                notRoaming = true,
                interfaceName = "tun0",
                wifi = null,
                cellular = null
            ),
            verdict = NetworkReadinessVerdict.VpnActive,
            transportLabel = "VPN, Wi-Fi",
            quickFixReason = "vpn_active",
            dnsProbeStatus = NetworkDnsProbeStatus(
                verdict = NetworkDnsProbeVerdict.Failed,
                host = "example.com",
                latencyMillis = 1_250L,
                latencyBucket = NetworkLatencyBucket.Slow,
                error = "timeout"
            ),
            globalDnsProbeStatus = NetworkDnsProbeStatus(
                verdict = NetworkDnsProbeVerdict.Resolved,
                host = "one.one.one.one",
                latencyMillis = 120L,
                latencyBucket = NetworkLatencyBucket.Fast
            ),
            userFacingVerdict = NetworkReadinessUserVerdict.VpnActive,
            userFacingQuickFixText = "Turn off VPN, then tap Refresh."
        )
    }
}
