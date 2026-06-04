package com.example.coblaxexamlock.ui.preparation

import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.DpcProtectionTier
import com.example.coblaxexamlock.DpcRuntimeStatus
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceEvaluation
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.GeofenceVerdict
import com.example.coblaxexamlock.LocationBypassState
import com.example.coblaxexamlock.LocationFixQualityStatus
import com.example.coblaxexamlock.LocationFixQualityVerdict
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlayQuickFixTarget
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.defaultDpcRuntimeStatus
import com.example.coblaxexamlock.resolveWebViewCompatibilityStatus
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.NetworkDiagnostics
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreExamHealthCheckTest {
    @Test
    fun stableInputsProduceStableSnapshot() {
        val snapshot = buildPreExamHealthSnapshot(defaultInput())

        assertEquals(0, snapshot.blockingCount)
        assertEquals(0, snapshot.warningCount)
        assertTrue(snapshot.items.any { it.category == PreExamHealthCategory.ScreenPinning })
    }

    @Test
    fun activeScreenPinningIsStableAndDocumentsSkippedRepeatRequest() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(screenPinningActive = true)
        )

        val pinning = snapshot.items.first { it.category == PreExamHealthCategory.ScreenPinning }
        assertEquals(PreExamHealthVerdict.Stable, pinning.verdict)
        assertTrue(pinning.detail.contains("repeated", ignoreCase = true))
    }

    @Test
    fun supportedInactiveScreenPinningBlocksStartGate() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(screenPinningActive = false)
        )

        val pinning = snapshot.items.first { it.category == PreExamHealthCategory.ScreenPinning }
        assertEquals(PreExamHealthVerdict.Blocking, pinning.verdict)
        assertTrue(pinning.quickFix.orEmpty().contains("Start Screen Pinning"))
        assertEquals(PreExamHealthCategory.ScreenPinning, preExamHealthStartBlocker(snapshot)?.category)
    }

    @Test
    fun missingScreenPinningAndConfirmedOverlayBecomeBlocking() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                screenPinningAvailable = false,
                overlayRiskResult = defaultOverlayRisk(confirmedInteractionDetected = true)
            )
        )

        assertEquals(2, snapshot.blockingCount)
        assertEquals(
            PreExamHealthVerdict.Blocking,
            snapshot.items.first { it.category == PreExamHealthCategory.FloatingAppOverlay }.verdict
        )
    }

    @Test
    fun nonStableNetworkIsWarningNotNewStartBlocker() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                networkReadinessStatus = defaultNetwork(
                    verdict = NetworkReadinessVerdict.CaptivePortal,
                    userVerdict = NetworkReadinessUserVerdict.CaptivePortal
                )
            )
        )

        assertEquals(
            PreExamHealthVerdict.Warning,
            snapshot.items.first { it.category == PreExamHealthCategory.Network }.verdict
        )
    }

    @Test
    fun vpnActiveWithoutBypassBlocksStartGate() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                networkReadinessStatus = defaultNetwork(
                    verdict = NetworkReadinessVerdict.VpnActive,
                    userVerdict = NetworkReadinessUserVerdict.VpnActive,
                    vpnActive = true
                )
            )
        )

        val network = snapshot.items.first { it.category == PreExamHealthCategory.Network }
        assertEquals(PreExamHealthVerdict.Blocking, network.verdict)
        assertEquals(PreExamHealthCategory.Network, preExamHealthStartBlocker(snapshot)?.category)
    }

    @Test
    fun vpnActiveWithBypassWarnsWithoutBlockingStartGate() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                vpnBypassed = true,
                networkReadinessStatus = defaultNetwork(
                    verdict = NetworkReadinessVerdict.VpnActive,
                    userVerdict = NetworkReadinessUserVerdict.VpnActive,
                    vpnActive = true
                )
            )
        )

        val network = snapshot.items.first { it.category == PreExamHealthCategory.Network }
        assertEquals(PreExamHealthVerdict.Warning, network.verdict)
        assertEquals(null, preExamHealthStartBlocker(snapshot))
    }

    @Test
    fun unavailableWebViewProviderBlocksStartGate() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                    packageName = null,
                    versionName = null
                )
            )
        )

        val blocker = preExamHealthStartBlocker(snapshot)
        assertEquals(PreExamHealthCategory.WebView, blocker?.category)
        assertEquals(PreExamHealthVerdict.Blocking, blocker?.verdict)
    }

    @Test
    fun oldWebViewProviderWarnsWithoutBlockingStartGate() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                    packageName = "com.android.webview",
                    versionName = "74.0.3729.186"
                )
            )
        )

        val webView = snapshot.items.first { it.category == PreExamHealthCategory.WebView }
        assertEquals(PreExamHealthVerdict.Warning, webView.verdict)
        assertEquals(null, preExamHealthStartBlocker(snapshot))
        assertTrue(webView.detail.contains("update", ignoreCase = true))
    }

    @Test
    fun unknownWebViewProviderVersionWarnsWithoutCrash() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                webViewCompatibilityStatus = resolveWebViewCompatibilityStatus(
                    packageName = "com.android.chrome",
                    versionName = "vendor-build"
                )
            )
        )

        val webView = snapshot.items.first { it.category == PreExamHealthCategory.WebView }
        assertEquals(PreExamHealthVerdict.Warning, webView.verdict)
        assertEquals(null, preExamHealthStartBlocker(snapshot))
        assertTrue(webView.detail.contains("cannot be verified", ignoreCase = true))
    }

    @Test
    fun accessibilityGuardFallbackTurnsMissingPinningIntoWarning() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                screenPinningAvailable = false,
                accessibilityGuardAvailable = true,
                accessibilityGuardEnabled = true
            )
        )

        assertEquals(0, snapshot.blockingCount)
        assertEquals(
            PreExamHealthVerdict.Warning,
            snapshot.items.first { it.category == PreExamHealthCategory.ScreenPinning }.verdict
        )
    }

    @Test
    fun android7NormalApkWarnsThatFloatingAppsCannotBeFullyBlocked() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                overlayRiskResult = defaultOverlayRisk(shieldSupported = false),
                dpcRuntimeStatus = defaultDpcRuntimeStatus(
                    sdkInt = 24,
                    overlayShieldSupported = false
                )
            )
        )

        val overlay = snapshot.items.first { it.category == PreExamHealthCategory.FloatingAppOverlay }
        assertEquals(PreExamHealthVerdict.Warning, overlay.verdict)
        assertTrue(overlay.detail.contains("Legacy Android"))
        assertTrue(overlay.detail.contains("not recommended", ignoreCase = true))
    }

    @Test
    fun android11NormalApkWarnsThatFloatingAppsCannotBeFullyBlocked() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                overlayRiskResult = defaultOverlayRisk(shieldSupported = false),
                dpcRuntimeStatus = defaultDpcRuntimeStatus(
                    sdkInt = 30,
                    overlayShieldSupported = false
                )
            )
        )

        val overlay = snapshot.items.first { it.category == PreExamHealthCategory.FloatingAppOverlay }
        assertEquals(PreExamHealthVerdict.Warning, overlay.verdict)
        assertTrue(overlay.detail.contains("Legacy Android"))
        assertTrue(overlay.detail.contains("not recommended", ignoreCase = true))
    }

    @Test
    fun android7DeviceOwnerWarnsWithLegacyLimitationInsteadOfClaimingFullOverlayBlock() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                overlayRiskResult = defaultOverlayRisk(shieldSupported = false),
                dpcRuntimeStatus = DpcRuntimeStatus(
                    deviceOwner = true,
                    adminActive = true,
                    lockTaskPermitted = true,
                    createWindowsRestrictionSupported = false,
                    createWindowsRestrictionActive = false,
                    protectionTier = DpcProtectionTier.LegacyDpcAndroid7
                )
            )
        )

        val overlay = snapshot.items.first { it.category == PreExamHealthCategory.FloatingAppOverlay }
        assertEquals(PreExamHealthVerdict.Warning, overlay.verdict)
        assertTrue(overlay.detail.contains("legacy limitation", ignoreCase = true))
        assertTrue(overlay.detail.contains("createWindowsSupported=no"))
    }

    @Test
    fun android12NormalApkKeepsOverlayReadinessStableWithShieldTier() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                dpcRuntimeStatus = defaultDpcRuntimeStatus(
                    sdkInt = 31,
                    overlayShieldSupported = true
                )
            )
        )

        val overlay = snapshot.items.first { it.category == PreExamHealthCategory.FloatingAppOverlay }
        assertEquals(PreExamHealthVerdict.Stable, overlay.verdict)
        assertTrue(overlay.detail.contains("tier=normal_apk"))
    }

    @Test
    fun supportedShieldApplyFailureWarnsInsteadOfReportingGood() {
        val snapshot = buildPreExamHealthSnapshot(
            defaultInput(
                overlayRiskResult = defaultOverlayRisk(
                    shieldSupported = true,
                    shieldLastApplySucceeded = false
                ),
                dpcRuntimeStatus = defaultDpcRuntimeStatus(
                    sdkInt = 31,
                    overlayShieldSupported = true
                )
            )
        )

        val overlay = snapshot.items.first { it.category == PreExamHealthCategory.FloatingAppOverlay }
        assertEquals(PreExamHealthVerdict.Warning, overlay.verdict)
        assertTrue(overlay.detail.contains("failed to apply", ignoreCase = true))
    }

    private fun defaultInput(
        screenPinningAvailable: Boolean = true,
        screenPinningActive: Boolean = screenPinningAvailable,
        accessibilityGuardAvailable: Boolean = false,
        accessibilityGuardEnabled: Boolean = false,
        overlayRiskResult: OverlayRiskResult = defaultOverlayRisk(),
        networkReadinessStatus: NetworkReadinessStatus = defaultNetwork(),
        vpnBypassed: Boolean = false,
        webViewCompatibilityStatus: com.example.coblaxexamlock.WebViewCompatibilityStatus =
            resolveWebViewCompatibilityStatus(
                packageName = "com.android.webview",
                versionName = "120.0.0.0"
            ),
        dpcRuntimeStatus: DpcRuntimeStatus = defaultDpcRuntimeStatus()
    ): PreExamHealthCheckInput {
        return PreExamHealthCheckInput(
            compatibilityProfile = DeviceCompatibilityProfile(),
            screenPinningAvailable = screenPinningAvailable,
            screenPinningActive = screenPinningActive,
            screenPinningBypassed = false,
            accessibilityGuardAvailable = accessibilityGuardAvailable,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            overlayRiskResult = overlayRiskResult,
            overlayBypassed = false,
            networkReadinessStatus = networkReadinessStatus,
            vpnBypassed = vpnBypassed,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            webViewRecoveryState = "Idle",
            webViewSessionResetInFlight = false,
            webViewSessionResetError = null,
            geofenceRuntimeStatus = defaultGeofenceRuntimeStatus(),
            geofenceBypassed = false,
            fakeLocationRuntimeStatus = defaultFakeLocationRuntimeStatus(),
            fakeLocationBypassed = false,
            deviceTimeSecurityStatus = defaultDeviceTimeStatus(),
            deviceTimeBypassed = false,
            batteryStatus = ExamBatteryStatus(levelPercent = 80, isCharging = true),
            dpcRuntimeStatus = dpcRuntimeStatus
        )
    }

    private fun defaultOverlayRisk(
        confirmedInteractionDetected: Boolean = false,
        shieldSupported: Boolean = true,
        shieldLastApplySucceeded: Boolean? = if (shieldSupported) true else null
    ): OverlayRiskResult {
        return OverlayRiskResult(
            bypassed = false,
            confirmedInteractionDetected = confirmedInteractionDetected,
            heuristicRisk = false,
            accessibilityEnabled = false,
            riskyAccessibilityPackages = emptyList(),
            violationCount = if (confirmedInteractionDetected) 1 else 0,
            signals = if (confirmedInteractionDetected) setOf(OverlaySignal.ObscuredTouch) else emptySet(),
            quickFixTargets = if (confirmedInteractionDetected) {
                setOf(OverlayQuickFixTarget.OverlaySettings)
            } else {
                emptySet()
            },
            shieldStatus = OverlayShieldStatus(
                supported = shieldSupported,
                requested = shieldSupported,
                lastApplySucceeded = shieldLastApplySucceeded,
                lastApplyAt = null
            ),
            lastTrigger = null,
            lastDetectedAt = null,
            lastContext = null
        )
    }

    private fun defaultNetwork(
        verdict: NetworkReadinessVerdict = NetworkReadinessVerdict.ConnectedStable,
        userVerdict: NetworkReadinessUserVerdict = NetworkReadinessUserVerdict.Stable,
        vpnActive: Boolean = false
    ): NetworkReadinessStatus {
        return NetworkReadinessStatus(
            examStatus = ExamNetworkStatus(
                label = "Online",
                detail = "Connected",
                isConnected = true
            ),
            diagnostics = NetworkDiagnostics(
                activeNetworkAvailable = true,
                transports = if (vpnActive) listOf("wifi", "vpn") else listOf("wifi"),
                hasInternetCapability = true,
                isValidated = true,
                isCaptivePortal = false,
                isMetered = false,
                isVpnActive = vpnActive,
                isAirplaneModeEnabled = false,
                notRoaming = true,
                interfaceName = "wlan0",
                wifi = null,
                cellular = null
            ),
            verdict = verdict,
            transportLabel = "Wi-Fi",
            quickFixReason = null,
            userFacingVerdict = userVerdict,
            userFacingQuickFixText = null
        )
    }

    private fun defaultGeofenceRuntimeStatus(): GeofenceRuntimeStatus {
        val evaluation = GeofenceEvaluation(
            enabled = false,
            config = null,
            configError = null,
            permissionGranted = true,
            locationServicesEnabled = true,
            locationSnapshot = null,
            closestCircleCenter = null,
            distanceMeters = null,
            verdict = GeofenceVerdict.Disabled
        )
        val fixQuality = LocationFixQualityStatus(
            snapshot = null,
            ageMs = null,
            accuracyMeters = null,
            accuracyThresholdMeters = 100f,
            verdict = LocationFixQualityVerdict.Fresh
        )
        return GeofenceRuntimeStatus(
            evaluation = evaluation,
            securityStatus = GeofenceSecurityStatus(
                geofenceEvaluation = evaluation,
                bypassState = LocationBypassState.Inactive,
                preciseLocationGranted = true,
                fixQualityStatus = fixQuality,
                finalVerdict = GeofenceSecurityVerdict.Disabled
            ),
            policySource = LocationPolicySource.DisabledNoPolicy,
            violationCount = 0,
            lastTrigger = null,
            lastDetectedAt = null,
            lastContext = null
        )
    }

    private fun defaultFakeLocationRuntimeStatus(): FakeLocationRuntimeStatus {
        return FakeLocationRuntimeStatus(
            securityStatus = LocationSpoofSecurityStatus(
                monitoringEnabled = false,
                bypassState = LocationBypassState.Inactive,
                permissionGranted = true,
                locationServicesEnabled = true,
                snapshotAvailable = false,
                suspiciousFakeLocationPackages = emptyList(),
                developerOptionsEnabled = false,
                mockLocationDetected = false,
                supportingSignals = emptySet(),
                confidenceTier = LocationSpoofConfidenceTier.Safe,
                fixQualityStatus = LocationFixQualityStatus(
                    snapshot = null,
                    ageMs = null,
                    accuracyMeters = null,
                    accuracyThresholdMeters = 100f,
                    verdict = LocationFixQualityVerdict.Fresh
                ),
                fixQualityEligible = false,
                finalVerdict = LocationSpoofSecurityVerdict.Disabled
            ),
            violationCount = 0,
            lastTrigger = null,
            lastDetectedAt = null,
            lastContext = null
        )
    }

    private fun defaultDeviceTimeStatus(): DeviceTimeSecurityStatus {
        return DeviceTimeSecurityStatus(
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
}
