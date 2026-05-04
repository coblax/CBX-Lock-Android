package com.example.coblaxexamlock.runtime

import android.content.Context
import android.os.Build
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.IntegrityCheckResult
import com.example.coblaxexamlock.IntegrityGuard
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ReverseEngineeringResult
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.SignatureIntegrity
import com.example.coblaxexamlock.SignatureIntegrityResult
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.format.formatElapsedDuration
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.model.ClipboardDiagnostics
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.model.VirtualEnvironmentDiagnostics
import com.example.coblaxexamlock.resolveExpectedSigningFingerprints
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class TelegramSectionDetailsContext(
    val context: Context,
    val section: DiagnosticSection,
    val examUserAgent: String,
    val examUserAgentSource: String,
    val keyboardPackage: String,
    val keyboardAllowed: Boolean,
    val usingBuiltInExamKeyboard: Boolean,
    val keyboardRawInputMethod: String,
    val keyboardVersion: String,
    val enabledKeyboardPackages: List<String>,
    val keyboardSystemApp: Boolean,
    val bluetoothPermissionGranted: Boolean,
    val bluetoothEnabled: Boolean,
    val bluetoothAdapterState: String,
    val bluetoothConnectedDevicesCount: Int?,
    val bluetoothHeadsetConnected: Boolean?,
    val accessibilityServiceEnabled: Boolean,
    val bypassAccessibility: Boolean,
    val accessibilityBypassTampered: Boolean,
    val accessibilityInspection: AccessibilityInspectionResult,
    val accessibilityManagerEnabled: Boolean,
    val touchExplorationEnabled: Boolean,
    val accessibilityPackages: List<String>,
    val accessibilityRawValue: String,
    val allowedAccessibilityServices: List<String>,
    val allowedAccessibilityPackages: List<String>,
    val effectiveAccessibilityPackages: List<String>,
    val riskyAccessibilityPackages: List<String>,
    val bypassOverlay: Boolean,
    val overlayBypassTampered: Boolean,
    val overlayRiskResult: OverlayRiskResult,
    val overlayViolationCount: Int,
    val geofenceRuntimeStatus: GeofenceRuntimeStatus,
    val fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    val bypassAppSwitch: Boolean,
    val appSwitchBypassTampered: Boolean,
    val appSwitchStatus: AppSwitchStatus,
    val adbBypassState: AdbBypassState,
    val adbInspection: AdbInspection,
    val usbConnected: Boolean,
    val installSource: String,
    val appDebuggable: Boolean,
    val rootBypassState: RootBypassState,
    val rootSecurityStatus: RootSecurityStatus,
    val signatureIntegrityResult: SignatureIntegrityResult?,
    val virtualEnvironmentDiagnostics: VirtualEnvironmentDiagnostics?,
    val clipboardDiagnostics: ClipboardDiagnostics,
    val clipboardSignature: String,
    val clipboardViolationCount: Int,
    val lastClipboardChangeEvent: String,
    val clipboardRuntimeStatus: ClipboardRuntimeStatus,
    val screenPinningAvailable: Boolean,
    val screenPinningEnabledInSystem: String,
    val lockTaskStateBeforePinningRequest: String,
    val lockTaskStateAfterPinningRequest: String,
    val screenPinningRequestOutcome: String,
    val screenPinningDialogLikelyShown: Boolean,
    val screenPinningUserActionInference: String,
    val screenPinningActivationDurationMs: Long?,
    val examSessionCancelledByPinningFailure: Boolean,
    val isScreenPinningActive: Boolean,
    val bypassScreenPinning: Boolean,
    val integritySummary: String,
    val networkStatus: ExamNetworkStatus,
    val offlineRuntimeStatus: ExamOfflineRuntimeStatus,
    val deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    val bypassDeviceTime: Boolean,
    val uiLanguage: UiLanguage,
    val healthIntegrityResult: IntegrityCheckResult?,
    val healthReverseResult: ReverseEngineeringResult?,
    val healthLastCheckedAt: String?,
    val networkReadinessStatus: NetworkReadinessStatus? = null,
    val networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus? = null,
    val networkTimelinePreview: List<NetworkTimelineEntry> = emptyList(),
    val lastNetworkChangeAt: String? = null,
    val lastNetworkChangeSource: String? = null,
    val lastConnectedNetworkLabel: String? = null
)

private fun DeviceTimeSecurityVerdict.telegramLabel(): String = when (this) {
    DeviceTimeSecurityVerdict.Safe -> "Safe"
    DeviceTimeSecurityVerdict.AutoTimeDisabled -> "Automatic date & time off"
    DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> "Automatic time zone off"
    DeviceTimeSecurityVerdict.ClockDriftDetected -> "Clock change detected"
}

private fun DeviceTimeBypassState.telegramLabel(): String = when (this) {
    DeviceTimeBypassState.Active -> "Active"
    DeviceTimeBypassState.Inactive -> "Inactive"
    DeviceTimeBypassState.Tampered -> "Tampered"
}

private fun StringBuilder.appendDeviceTimeDetails(
    status: DeviceTimeSecurityStatus,
    bypassDeviceTime: Boolean
) {
    appendLine(
        "Device time verdict: ${status.finalVerdict.telegramLabel()}"
    )
    appendLine("Device time bypass active: ${if (bypassDeviceTime) "Ya" else "Tidak"}")
    appendLine("Device time bypass state: ${status.bypassState.telegramLabel()}")
    appendLine("Auto date & time: ${if (status.autoTimeEnabled) "On" else "Off"}")
    appendLine("Automatic time zone: ${if (status.autoTimeZoneEnabled) "On" else "Off"}")
    appendLine("Timezone: ${status.timezoneSummary.ifBlank { "-" }}")
    appendLine("Clock change detected: ${if (status.clockDriftDetected) "Ya" else "Tidak"}")
    appendLine("Clock drift: ${status.clockDriftMillis} ms")
    appendLine("Baseline wall clock: ${status.baselineWallClockMillis}")
    appendLine("Baseline elapsed: ${status.baselineElapsedRealtimeMillis}")
    appendLine("Current wall clock: ${status.wallClockNowMillis}")
    appendLine("Current elapsed: ${status.elapsedNowMillis}")
    appendLine("Blocking now: ${if (status.blocking) "Ya" else "Tidak"}")
}

private fun yesNo(value: Boolean): String = if (value) "Ya" else "Tidak"

private fun yesNoUnknown(value: Boolean?): String =
    value?.let(::yesNo) ?: "-"

private fun StringBuilder.appendNetworkDetails(
    networkStatus: ExamNetworkStatus,
    offlineRuntimeStatus: ExamOfflineRuntimeStatus,
    readinessStatus: NetworkReadinessStatus?,
    unstableRuntimeStatus: NetworkUnstableRuntimeStatus?,
    timelinePreview: List<NetworkTimelineEntry>,
    lastNetworkChangeAt: String?,
    lastNetworkChangeSource: String?,
    lastConnectedNetworkLabel: String?,
    uiLanguage: UiLanguage
) {
    appendLine("[NETWORK / CONNECTIVITY]")
    appendLine("Network label: ${networkStatus.label.ifBlank { "-" }}")
    appendLine("Network detail: ${networkStatus.detail.ifBlank { "-" }}")
    appendLine("Connected: ${yesNo(networkStatus.isConnected)}")
    appendLine("Cellular provider: ${networkStatus.cellularProvider?.ifBlank { "-" } ?: "-"}")
    readinessStatus?.let { status ->
        val diagnostics = status.diagnostics
        appendLine("Readiness verdict: ${status.verdict.name}")
        appendLine("Readiness transport: ${status.transportLabel.ifBlank { "-" }}")
        appendLine("Quick fix reason: ${status.quickFixReason?.ifBlank { "-" } ?: "-"}")
        appendLine("Active network available: ${yesNo(diagnostics.activeNetworkAvailable)}")
        appendLine("Transports: ${diagnostics.transports.joinToString().ifBlank { "-" }}")
        appendLine("Internet capability: ${yesNo(diagnostics.hasInternetCapability)}")
        appendLine("Validated: ${yesNo(diagnostics.isValidated)}")
        appendLine("Captive portal: ${yesNo(diagnostics.isCaptivePortal)}")
        appendLine("Metered: ${yesNo(diagnostics.isMetered)}")
        appendLine("VPN active: ${yesNo(diagnostics.isVpnActive)}")
        appendLine("Airplane mode: ${yesNo(diagnostics.isAirplaneModeEnabled)}")
        appendLine("Not roaming: ${yesNoUnknown(diagnostics.notRoaming)}")
        appendLine("Interface: ${diagnostics.interfaceName.ifBlank { "-" }}")
        diagnostics.wifi?.let { wifi ->
            appendLine("WiFi SSID: ${wifi.ssid.ifBlank { "-" }}")
            appendLine("WiFi BSSID: ${wifi.bssid.ifBlank { "-" }}")
            appendLine("WiFi RSSI: ${wifi.rssiDbm?.let { "$it dBm" } ?: "-"}")
            appendLine("WiFi signal level: ${wifi.signalLevel?.toString() ?: "-"}")
            appendLine("WiFi link speed: ${wifi.linkSpeedMbps?.let { "$it Mbps" } ?: "-"}")
            appendLine("WiFi band: ${wifi.bandLabel.ifBlank { "-" }}")
            appendLine("WiFi hidden SSID: ${yesNoUnknown(wifi.hiddenSsid)}")
            appendLine("WiFi location permission: ${yesNo(wifi.locationPermissionGranted)}")
            appendLine("Location services: ${yesNo(wifi.locationServicesEnabled)}")
        }
        diagnostics.cellular?.let { cellular ->
            appendLine("Cellular provider: ${cellular.providerName.ifBlank { "-" }}")
            appendLine("Cellular operator code: ${cellular.operatorCode.ifBlank { "-" }}")
            appendLine("Cellular network type: ${cellular.networkType.ifBlank { "-" }}")
            appendLine("Cellular roaming: ${yesNoUnknown(cellular.roaming)}")
            appendLine("Cellular signal level: ${cellular.signalLevel?.toString() ?: "-"}")
            appendLine("SIM state: ${cellular.simState.ifBlank { "-" }}")
        }
    } ?: appendLine("Readiness diagnostics: -")
    appendLine("Offline active: ${yesNo(offlineRuntimeStatus.offlineActive)}")
    appendLine("Offline started at: ${offlineRuntimeStatus.offlineStartedAt?.ifBlank { "-" } ?: "-"}")
    appendLine(
        "Offline duration now: ${
            formatElapsedDuration(offlineRuntimeStatus.currentOfflineDurationMs, uiLanguage)
        }"
    )
    appendLine("Offline warning shown: ${yesNo(offlineRuntimeStatus.offlineWarningShown)}")
    appendLine("Last offline warning at: ${offlineRuntimeStatus.lastOfflineWarningAt?.ifBlank { "-" } ?: "-"}")
    appendLine(
        "Last offline duration: ${
            formatElapsedDuration(offlineRuntimeStatus.lastOfflineDurationMs, uiLanguage)
        }"
    )
    unstableRuntimeStatus?.let { unstable ->
        appendLine("Unstable active: ${yesNo(unstable.unstableActive)}")
        appendLine("Unstable episode started at: ${unstable.episodeStartedAt?.ifBlank { "-" } ?: "-"}")
        appendLine("Network changes/flaps: ${unstable.flapCount}")
        appendLine("Last flap at: ${unstable.lastFlapAt?.ifBlank { "-" } ?: "-"}")
        appendLine("Unstable warning shown: ${yesNo(unstable.warningShown)}")
        appendLine("Last unstable warning at: ${unstable.lastWarningAt?.ifBlank { "-" } ?: "-"}")
        appendLine("Last unstable transport: ${unstable.lastTransportLabel?.ifBlank { "-" } ?: "-"}")
    }
    appendLine("Last network change at: ${lastNetworkChangeAt?.ifBlank { "-" } ?: "-"}")
    appendLine("Last network change source: ${lastNetworkChangeSource?.ifBlank { "-" } ?: "-"}")
    appendLine("Last connected network: ${lastConnectedNetworkLabel?.ifBlank { "-" } ?: "-"}")
    if (timelinePreview.isNotEmpty()) {
        appendLine("[RECENT NETWORK CHANGES]")
        timelinePreview.forEach { entry ->
            appendLine(
                "- ${entry.timestamp} | source=${entry.source.ifBlank { "-" }} | " +
                    "transport=${entry.transportLabel.ifBlank { "-" }} | " +
                    "connected=${yesNo(entry.connected)} | validated=${yesNo(entry.validated)} | " +
                    "captive=${yesNo(entry.captivePortal)} | ${entry.summary.ifBlank { "-" }}"
            )
        }
    }
}

internal fun StringBuilder.appendTelegramSectionDetails(details: TelegramSectionDetailsContext) {
    with(details) {
            when (section) {
                DiagnosticSection.Keyboard -> {
                    appendLine("[KEYBOARD]")
                    appendLine("Keyboard package: ${keyboardPackage.ifBlank { "-" }}")
                    appendLine("Keyboard allowed: ${if (keyboardAllowed) "Ya" else "Tidak"}")
                    appendLine("Fallback internal: ${if (usingBuiltInExamKeyboard) "Ya" else "Tidak"}")
                    appendLine("Keyboard raw input method: $keyboardRawInputMethod")
                    appendLine("Keyboard app version: $keyboardVersion")
                    appendLine(
                        "Enabled keyboards: ${
                            enabledKeyboardPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine("Keyboard system app: ${if (keyboardSystemApp) "Ya" else "Tidak"}")
                }
                DiagnosticSection.Bluetooth -> {
                    appendLine("[BLUETOOTH]")
                    appendLine("Bluetooth permission: ${if (bluetoothPermissionGranted) "Sudah" else "Belum"}")
                    appendLine("Bluetooth aktif: ${if (bluetoothEnabled) "Ya" else "Tidak"}")
                    appendLine("Bluetooth adapter state: $bluetoothAdapterState")
                    appendLine(
                        "Bluetooth connected devices count: ${
                            bluetoothConnectedDevicesCount?.toString() ?: "Tidak diketahui"
                        }"
                    )
                    appendLine(
                        "Bluetooth headset/A2DP connected: ${
                            bluetoothHeadsetConnected?.let { if (it) "Ya" else "Tidak" } ?: "Tidak diketahui"
                        }"
                    )
                }
                DiagnosticSection.Network -> appendNetworkDetails(
                    networkStatus = networkStatus,
                    offlineRuntimeStatus = offlineRuntimeStatus,
                    readinessStatus = networkReadinessStatus,
                    unstableRuntimeStatus = networkUnstableRuntimeStatus,
                    timelinePreview = networkTimelinePreview,
                    lastNetworkChangeAt = lastNetworkChangeAt,
                    lastNetworkChangeSource = lastNetworkChangeSource,
                    lastConnectedNetworkLabel = lastConnectedNetworkLabel,
                    uiLanguage = uiLanguage
                )
                DiagnosticSection.Accessibility -> {
                    appendLine("[ACCESSIBILITY]")
                    appendLine("Accessibility bypass: ${if (bypassAccessibility) "Ya" else "Tidak"}")
                    appendLine(
                        "Accessibility bypass tampered: ${
                            if (accessibilityBypassTampered) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Accessibility blocking service active: ${
                            if (accessibilityServiceEnabled) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Accessibility allowed-only active: ${
                            if (accessibilityInspection.allowedOnlyActive) "Ya" else "Tidak"
                        }"
                    )
                    appendLine("Accessibility manager enabled: ${if (accessibilityManagerEnabled) "Ya" else "Tidak"}")
                    appendLine("Touch exploration enabled: ${if (touchExplorationEnabled) "Ya" else "Tidak"}")
                    appendLine("Accessibility services count: ${accessibilityPackages.size}")
                    appendLine("Accessibility services raw: $accessibilityRawValue")
                    appendLine(
                        "Accessibility packages: ${
                            accessibilityPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Allowed accessibility services: ${
                            allowedAccessibilityServices.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Allowed accessibility packages: ${
                            allowedAccessibilityPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Effective accessibility packages: ${
                            effectiveAccessibilityPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Risky accessibility packages: ${
                            riskyAccessibilityPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                }
                DiagnosticSection.Overlay -> {
                    appendLine("[OVERLAY / FLOATING APP]")
                    appendLine("Overlay bypass: ${if (bypassOverlay) "Ya" else "Tidak"}")
                    appendLine(
                        "Overlay bypass tampered: ${
                            if (overlayBypassTampered) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Overlay heuristic risk: ${
                            if (overlayRiskResult.heuristicRisk) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Overlay confirmed runtime interaction: ${
                            if (overlayRiskResult.confirmedInteractionDetected) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Overlay shield supported: ${
                            if (overlayRiskResult.shieldStatus.supported) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Overlay shield requested: ${
                            if (overlayRiskResult.shieldStatus.requested) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Overlay shield active: ${
                            if (overlayRiskResult.shieldStatus.active) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Overlay shield last apply succeeded: ${
                            overlayRiskResult.shieldStatus.lastApplySucceeded?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Overlay shield last apply at: ${
                            overlayRiskResult.shieldStatus.lastApplyAt?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine("Overlay violation count: $overlayViolationCount")
                    appendLine(
                        "Overlay signals: ${
                            overlayRiskResult.signals
                                .map { it.diagnosticLabel() }
                                .joinToString()
                                .ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Overlay risky accessibility packages: ${
                            overlayRiskResult.riskyAccessibilityPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Overlay quick fixes: ${
                            overlayRiskResult.quickFixTargets
                                .map { it.diagnosticLabel() }
                                .joinToString()
                                .ifBlank { "-" }
                        }"
                    )
                    appendLine("Last overlay trigger: ${overlayRiskResult.lastTrigger?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last overlay timestamp: ${overlayRiskResult.lastDetectedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last overlay context: ${overlayRiskResult.lastContext?.ifBlank { "-" } ?: "-"}")
                    appendLine("Accessibility service active: ${if (accessibilityServiceEnabled) "Ya" else "Tidak"}")
                }
                DiagnosticSection.Geofence -> {
                    appendLine("[GEOFENCE]")
                    appendLine("Location policy source: ${geofenceRuntimeStatus.policySource.diagnosticLabel()}")
                    appendLine(
                        "Geofence bypass state: ${
                            geofenceRuntimeStatus.securityStatus.bypassState.name.lowercase(Locale.US)
                        }"
                    )
                    appendLine("Geofence enabled: ${if (geofenceRuntimeStatus.evaluation.enabled) "Ya" else "Tidak"}")
                    appendLine(
                        "Location permission granted: ${
                            if (geofenceRuntimeStatus.evaluation.permissionGranted) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Precise location granted: ${
                            if (geofenceRuntimeStatus.securityStatus.preciseLocationGranted) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Location services enabled: ${
                            if (geofenceRuntimeStatus.evaluation.locationServicesEnabled) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Geofence shape: ${
                            geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"
                        }"
                    )
                    appendLine(
                        "Polygon vertex count: ${
                            geofenceRuntimeStatus.evaluation.config?.vertices?.size?.toString() ?: "-"
                        }"
                    )
                    appendLine(
                        "Polygon vertices: ${
                            summarizePolygonVertices(geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty())
                        }"
                    )
                    appendLine(
                        "Circle center count: ${
                            effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config).size
                        }"
                    )
                    appendLine(
                        "Circle centers: ${
                            summarizeCircleCenters(effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config))
                        }"
                    )
                    appendLine(
                        "Closest / primary center: ${
                            geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                                formatCoordinates(it.latitude, it.longitude)
                            } ?: geofenceRuntimeStatus.evaluation.config?.let {
                                formatCoordinates(it.centerLat, it.centerLng)
                            } ?: "-"
                        }"
                    )
                    appendLine(
                        "Shared radius meters: ${
                            geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                                String.format(Locale.US, "%.1f", it)
                            } ?: "-"
                        }"
                    )
                    appendLine(
                        "Current coordinates: ${
                            geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                                formatCoordinates(it.latitude, it.longitude)
                            } ?: "-"
                        }"
                    )
                    appendLine(
                        "Location provider: ${
                            geofenceRuntimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Location accuracy meters: ${
                            geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                                String.format(Locale.US, "%.1f", it)
                            } ?: "-"
                        }"
                    )
                    appendLine("Location fix quality: ${geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}")
                    appendLine("Location fix age: ${formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)}")
                    appendLine(
                        "Location snapshot used for geofence: ${
                            if (geofenceRuntimeStatus.securityStatus.fixQualityStatus.usableForGeofence) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Last fix timestamp: ${
                            geofenceRuntimeStatus.evaluation.locationSnapshot?.fixTimestampMs?.let {
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))
                            } ?: "-"
                        }"
                    )
                    appendLine("Distance from closest center: ${formatGeofenceDistance(geofenceRuntimeStatus.evaluation.distanceMeters)}")
                    appendLine("Final geofence verdict: ${geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}")
                    appendLine("Geofence verdict: ${geofenceRuntimeStatus.evaluation.verdict.diagnosticLabel()}")
                    appendLine("Geofence violations: ${geofenceRuntimeStatus.violationCount}")
                    appendLine("Last geofence trigger: ${geofenceRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last geofence timestamp: ${geofenceRuntimeStatus.lastDetectedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last geofence context: ${geofenceRuntimeStatus.lastContext?.ifBlank { "-" } ?: "-"}")
                }
                DiagnosticSection.FakeLocation -> {
                    appendLine("[ANTI-FAKE-LOCATION]")
                    appendLine(
                        "Fake-location bypass state: ${
                            fakeLocationRuntimeStatus.securityStatus.bypassState.name.lowercase(Locale.US)
                        }"
                    )
                    appendLine(
                        "Monitoring enabled: ${
                            if (fakeLocationRuntimeStatus.securityStatus.monitoringEnabled) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Location permission granted: ${
                            if (fakeLocationRuntimeStatus.securityStatus.permissionGranted) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Location services enabled: ${
                            if (fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Location snapshot available: ${
                            if (fakeLocationRuntimeStatus.securityStatus.snapshotAvailable) "Ya" else "Tidak"
                        }"
                    )
                    appendLine("Final verdict: ${fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}")
                    appendLine("Confidence tier: ${fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel()}")
                    appendLine(
                        "Mock location flag: ${
                            if (fakeLocationRuntimeStatus.securityStatus.mockLocationDetected) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Developer options enabled: ${
                            if (fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Suspicious fake-location packages: ${
                            fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Fix quality eligible: ${
                            if (fakeLocationRuntimeStatus.securityStatus.fixQualityEligible) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Fix quality verdict: ${
                            fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()
                        }"
                    )
                    appendLine(
                        "Supporting signals: ${
                            fakeLocationRuntimeStatus.securityStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine("Fake-location violations: ${fakeLocationRuntimeStatus.violationCount}")
                    appendLine("Last fake-location trigger: ${fakeLocationRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last fake-location timestamp: ${fakeLocationRuntimeStatus.lastDetectedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last fake-location context: ${fakeLocationRuntimeStatus.lastContext?.ifBlank { "-" } ?: "-"}")
                }
                DiagnosticSection.AppSwitch -> {
                    appendLine("[APP SWITCH]")
                    appendLine("App Switch bypass: ${if (bypassAppSwitch) "Ya" else "Tidak"}")
                    appendLine(
                        "App Switch bypass tampered: ${
                            if (appSwitchBypassTampered) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "App Switch monitoring enabled: ${
                            if (appSwitchStatus.monitoringEnabled) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "App Switch runtime monitoring active: ${
                            if (appSwitchStatus.runtimeMonitoringActive) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "App Switch protection mode: ${
                            appSwitchStatus.protectionMode.diagnosticLabel()
                        }"
                    )
                    appendLine(
                        "App Switch lock task active: ${
                            if (appSwitchStatus.lockTaskActive) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "App Switch fallback guard active: ${
                            if (appSwitchStatus.fallbackGuardActive) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Accessibility Guard enabled: ${
                            if (appSwitchStatus.accessibilityGuardEnabled) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Accessibility fallback active: ${
                            if (appSwitchStatus.accessibilityFallbackActive) "Ya" else "Tidak"
                        }"
                    )
                    appendLine("Accessibility Guard violations: ${appSwitchStatus.accessibilityViolationCount}")
                    appendLine(
                        "Last Accessibility reason: ${
                            appSwitchStatus.accessibilityLastReason?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Last Accessibility foreign package: ${
                            appSwitchStatus.accessibilityLastForeignPackage?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Last Accessibility event type: ${
                            appSwitchStatus.accessibilityLastEventType?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Last Accessibility timestamp: ${
                            appSwitchStatus.accessibilityLastDetectedAt?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Current alarm severity: ${
                            appSwitchStatus.accessibilityAlarmSeverity?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine("App Switch violations: ${appSwitchStatus.violationCount}")
                    appendLine(
                        "App Switch pending violation: ${
                            if (appSwitchStatus.pendingViolation) "Ya" else "Tidak"
                        }"
                    )
                    appendLine("Last App Switch trigger: ${appSwitchStatus.lastTrigger?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last App Switch timestamp: ${appSwitchStatus.lastDetectedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last App Switch context: ${appSwitchStatus.lastContext?.ifBlank { "-" } ?: "-"}")
                }
                DiagnosticSection.DeveloperAdb -> {
                    appendLine("[DEVELOPER MODE / ADB]")
                    appendLine("ADB bypass: ${if (adbBypassState == AdbBypassState.Active) "Ya" else "Tidak"}")
                    appendLine("ADB bypass tampered: ${if (adbBypassState == AdbBypassState.Tampered) "Ya" else "Tidak"}")
                    appendLine("Developer mode: ${if (adbInspection.developerOptionsEnabled) "Aktif" else "Tidak aktif"}")
                    appendLine("Developer settings raw: ${adbInspection.developerOptionsRawValue}")
                    appendLine("ADB: ${if (adbInspection.adbEnabled) "Aktif" else "Tidak aktif"}")
                    appendLine("ADB raw: ${adbInspection.adbRawValue}")
                    appendLine("ro.adb.secure: ${adbInspection.adbSecureProperty}")
                    appendLine("ADB integrity hint: ${adbInspection.integrityHintSummary}")
                    appendLine("USB connected: ${if (usbConnected) "Ya" else "Tidak"}")
                    appendLine("Install source: $installSource")
                    appendLine("BuildConfig.DEBUG: ${if (BuildConfig.DEBUG) "true" else "false"}")
                    appendLine("App debuggable: ${if (appDebuggable) "Ya" else "Tidak"}")
                }
                DiagnosticSection.Root -> {
                    appendLine("[ROOT DEVICE]")
                    appendLine("Root bypass: ${if (rootBypassState == RootBypassState.Active) "Ya" else "Tidak"}")
                    appendLine("Root bypass tampered: ${if (rootBypassState == RootBypassState.Tampered) "Ya" else "Tidak"}")
                    appendLine("Root detected: ${if (rootSecurityStatus.detected) "Ya" else "Tidak"}")
                    appendLine("Root severity: ${rootSecurityStatus.severityLabel}")
                    appendLine("Primary root indicator: ${rootSecurityStatus.primaryIndicatorLabel.ifBlank { "-" }}")
                    appendLine("Root evidence summary: ${rootSecurityStatus.evidenceSummary}")
                    appendLine("Root test-keys: ${if (rootSecurityStatus.details.hasTestKeys) "Ya" else "Tidak"}")
                    appendLine("su binary detected: ${if (rootSecurityStatus.details.hasSuBinary) "Ya" else "Tidak"}")
                    appendLine("Root binaries found: ${rootSecurityStatus.details.rootBinaryPaths.joinToString().ifBlank { "-" }}")
                    appendLine(
                        "Root app packages found: ${
                             rootSecurityStatus.details.foundRootPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine("Magisk paths found: ${rootSecurityStatus.details.magiskPaths.joinToString().ifBlank { "-" }}")
                    appendLine("Zygisk detected: ${if (rootSecurityStatus.details.zygiskDetected) "Ya" else "Tidak"}")
                    appendLine("Bootloader unlocked: ${if (rootSecurityStatus.details.bootloaderUnlocked) "Ya" else "Tidak"}")
                    appendLine("Verified boot state: ${rootSecurityStatus.details.verifiedBootState}")
                    appendLine("VBMeta device state: ${rootSecurityStatus.details.vbmetaDeviceState}")
                    appendLine("Flash locked: ${rootSecurityStatus.details.flashLocked}")
                    appendLine("SELinux enabled: ${formatYesNo(rootSecurityStatus.details.selinuxEnabled)}")
                    appendLine("SELinux enforced: ${formatYesNo(rootSecurityStatus.details.selinuxEnforced)}")
                    appendLine(
                        "Dangerous system props: ${
                            rootSecurityStatus.details.dangerousSystemProperties.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine("ro.debuggable: ${rootSecurityStatus.details.roDebuggable}")
                    appendLine("ro.secure: ${rootSecurityStatus.details.roSecure}")
                    appendLine("ro.adb.secure: ${rootSecurityStatus.details.roAdbSecure}")
                    appendLine("ro.build.type: ${rootSecurityStatus.details.roBuildType}")
                }
                DiagnosticSection.Signature -> {
                    val signatureResult = signatureIntegrityResult ?: run {
                        val expectedFingerprints = resolveExpectedSigningFingerprints(
                            isDebugBuild = BuildConfig.DEBUG,
                            releaseFingerprint = SecureStrings.signingFingerprintRelease,
                            debugFingerprint = SecureStrings.signingFingerprintDebug
                        )
                        SignatureIntegrity.check(context, expectedFingerprints)
                    }
                    appendLine("[OFFICIAL APK SIGNATURE]")
                    appendLine("Signature match: ${if (signatureResult.isMatch) "Ya" else "Tidak"}")
                    appendLine("Reason: ${signatureResult.reason}")
                    appendLine("Actual fingerprint: ${signatureResult.actualFingerprint.ifBlank { "-" }}")
                    appendLine(
                        "Expected fingerprints: ${
                            signatureResult.expectedFingerprints.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine("Debug build: ${if (BuildConfig.DEBUG) "true" else "false"}")
                }
                DiagnosticSection.VirtualEnvironment -> {
                    val diagnostics = virtualEnvironmentDiagnostics ?: getVirtualEnvironmentDiagnostics(context)
                    val fingerprint = Build.FINGERPRINT.orEmpty()
                    val model = Build.MODEL.orEmpty()
                    val manufacturer = Build.MANUFACTURER.orEmpty()
                    val brand = Build.BRAND.orEmpty()
                    val device = Build.DEVICE.orEmpty()
                    val product = Build.PRODUCT.orEmpty()
                    val hardware = Build.HARDWARE.orEmpty()
                    appendLine("[VIRTUAL ENVIRONMENT]")
                    appendLine("Detected: ${if (diagnostics.detected) "Ya" else "Tidak"}")
                    appendLine("Indicators: ${diagnostics.indicators.joinToString().ifBlank { "-" }}")
                    appendLine("ro.kernel.qemu: ${diagnostics.qemuProperty.ifBlank { "-" }}")
                    appendLine(
                        "Emulator packages: ${
                            diagnostics.emulatorPackages.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine("QEMU files: ${diagnostics.qemuFiles.joinToString().ifBlank { "-" }}")
                    appendLine("ABIs: ${diagnostics.abis.joinToString().ifBlank { "-" }}")
                    appendLine("Build fingerprint: ${fingerprint.ifBlank { "-" }}")
                    appendLine("Build model: ${model.ifBlank { "-" }}")
                    appendLine("Build manufacturer: ${manufacturer.ifBlank { "-" }}")
                    appendLine("Build brand: ${brand.ifBlank { "-" }}")
                    appendLine("Build device: ${device.ifBlank { "-" }}")
                    appendLine("Build product: ${product.ifBlank { "-" }}")
                    appendLine("Build hardware: ${hardware.ifBlank { "-" }}")
                }
                DiagnosticSection.Clipboard -> {
                    appendLine("[CLIPBOARD]")
                    appendLine("Clipboard currently has data: ${if (clipboardDiagnostics.hasData) "Ya" else "Tidak"}")
                    appendLine("Primary clip item count: ${clipboardDiagnostics.itemCount}")
                    appendLine("Last clipboard signature: ${clipboardSignature.ifBlank { "-" }}")
                    appendLine(
                        "Current clipboard semantic signature: ${
                            clipboardDiagnostics.currentSemanticSignature.ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Last clipboard observed signature: ${
                            clipboardRuntimeStatus.lastObservedSignature?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Baseline semantic signature: ${
                            clipboardRuntimeStatus.baselineSemanticSignature?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Detected semantic signature: ${
                            clipboardRuntimeStatus.detectedSemanticSignature?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine("Clipboard violation count: $clipboardViolationCount")
                    appendLine("Last clipboard change detected: $lastClipboardChangeEvent")
                    appendLine(
                        "Last clipboard observed at: ${
                            clipboardRuntimeStatus.lastObservedAt?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Last clipboard confirmed at: ${
                            clipboardRuntimeStatus.lastConfirmedAt?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine("Last clipboard decision: ${clipboardRuntimeStatus.lastDecision}")
                }
                DiagnosticSection.ScreenPinning -> {
                    appendLine("[SCREEN PINNING]")
                    appendLine("Screen pinning bypass: ${if (bypassScreenPinning) "Ya" else "Tidak"}")
                    appendLine("Screen pinning: ${if (isScreenPinningActive) "Aktif" else "Belum aktif"}")
                    appendLine("Screen pinning available: ${if (screenPinningAvailable) "Ya" else "Tidak"}")
                    appendLine("Screen pinning system setting: $screenPinningEnabledInSystem")
                    appendLine("Lock task state before request: $lockTaskStateBeforePinningRequest")
                    appendLine("Lock task state after request: $lockTaskStateAfterPinningRequest")
                    appendLine("Pinning request outcome: $screenPinningRequestOutcome")
                    appendLine(
                        "Dialog pinning appeared: ${
                            if (screenPinningDialogLikelyShown) "Kemungkinan ya" else "Belum terindikasi"
                        }"
                    )
                    appendLine("User action on dialog: $screenPinningUserActionInference")
                    appendLine(
                        "Time to activate: ${
                            screenPinningActivationDurationMs?.let { "$it ms" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Exam session cancelled by pinning failure: ${
                            if (examSessionCancelledByPinningFailure) "Ya" else "Tidak"
                        }"
                    )
                }
                DiagnosticSection.DeviceTime -> {
                    appendLine("[DEVICE TIME]")
                    appendDeviceTimeDetails(
                        status = deviceTimeSecurityStatus,
                        bypassDeviceTime = bypassDeviceTime
                    )
                }
                DiagnosticSection.SecurityHealth -> {
                    val latestIntegrityResult =
                        healthIntegrityResult ?: IntegrityGuard.check(context, baselineFingerprint = null)
                    val latestReverseResult =
                        healthReverseResult ?: ReverseEngineeringGuard.inspect(context)
                    appendLine("[SECURITY HEALTH]")
                    appendLine("Exam user-agent: ${examUserAgent.ifBlank { "-" }}")
                    appendLine("Exam user-agent source: ${examUserAgentSource.ifBlank { "-" }}")
                    appendLine("Network status now: ${networkStatus.label.ifBlank { "-" }}")
                    appendLine("Offline active: ${if (offlineRuntimeStatus.offlineActive) "Ya" else "Tidak"}")
                    appendLine("Offline started at: ${offlineRuntimeStatus.offlineStartedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine(
                        "Offline duration now: ${
                            formatElapsedDuration(offlineRuntimeStatus.currentOfflineDurationMs, uiLanguage)
                        }"
                    )
                    appendLine(
                        "Offline warning shown: ${
                            if (offlineRuntimeStatus.offlineWarningShown) "Ya" else "Tidak"
                        }"
                    )
                    appendLine(
                        "Last offline warning at: ${
                            offlineRuntimeStatus.lastOfflineWarningAt?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Last offline duration: ${
                            formatElapsedDuration(offlineRuntimeStatus.lastOfflineDurationMs, uiLanguage)
                        }"
                    )
                    appendLine("Last checked: ${healthLastCheckedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine("Integrity public summary: ${integritySummary.ifBlank { "-" }}")
                    appendLine("Integrity ok: ${if (latestIntegrityResult.ok) "Ya" else "Tidak"}")
                    appendLine(
                        "Integrity issues: ${
                            latestIntegrityResult.issues.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Integrity details: ${latestIntegrityResult.details.ifBlank { "-" }}"
                    )
                    appendLine(
                        "Current fingerprint: ${
                            latestIntegrityResult.currentFingerprint.ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Expected dex hash: ${
                            latestIntegrityResult.expectedDexHash.ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Actual dex hash: ${
                            latestIntegrityResult.actualDexHash.ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Reverse tamper detected: ${
                            if (latestReverseResult.tamperDetected) "Ya" else "Tidak"
                        }"
                    )
                    appendLine("Reverse score: ${latestReverseResult.score}")
                    appendLine(
                        "Reverse strong signals: ${
                            latestReverseResult.strongSignals.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendLine(
                        "Reverse weak signals: ${
                            latestReverseResult.weakSignals.joinToString().ifBlank { "-" }
                        }"
                    )
                    appendDeviceTimeDetails(
                        status = deviceTimeSecurityStatus,
                        bypassDeviceTime = bypassDeviceTime
                    )
                }
            }
    }
}
