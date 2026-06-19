package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.MutableState
import com.example.coblaxexamlock.AccessibilityExamGuardStore
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AppSwitchSuppressionReason
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DpcRuntimeStatus
import com.example.coblaxexamlock.ExamAlarmSeverity
import com.example.coblaxexamlock.ExamPolicyEngine
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.ExamScheduleValidator
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceConfigParseResult
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.PinningActivationGraceWindowMillis
import com.example.coblaxexamlock.PinningActivationPurpose
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.ScreenPinningEnforcer
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.ScreenPinningSignals
import com.example.coblaxexamlock.SignatureIntegrityResult
import com.example.coblaxexamlock.SplitLocationSecurityStatus
import com.example.coblaxexamlock.TrustedNetworkTimeCoordinator
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.evaluateLocationFixQuality
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessUserVerdict
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.getExternalDisplayCount
import com.example.coblaxexamlock.runtime.getVirtualEnvironmentDiagnosticsOnIo
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isInAnySplitMode
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatusWithExamHostProbe
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.ui.preparation.PreExamHealthCheckInput
import com.example.coblaxexamlock.ui.preparation.buildPreExamHealthSnapshot
import com.example.coblaxexamlock.ui.preparation.preExamHealthStartBlocker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val StartNetworkRecoveryAttempts = 3
private const val StartNetworkRecoveryRetryDelayMillis = 1_000L

internal class ExamRuntimeStartBlockCallbacks(
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    val setSecurityIssueDialogTitle: (String) -> Unit,
    val setSecurityIssueDialogMessage: (String) -> Unit,
    val setSecurityIssueDialogCode: (String?) -> Unit
)

internal fun applyExamRuntimeStartBlockMessage(
    message: StartExamBlockMessage,
    level: DiagnosticEventLevel = DiagnosticEventLevel.WARNING,
    callbacks: ExamRuntimeStartBlockCallbacks
) {
    callbacks.recordAction(message.code, message.details, level)
    callbacks.setSecurityIssueDialogTitle(message.title)
    callbacks.setSecurityIssueDialogMessage(message.message)
    callbacks.setSecurityIssueDialogCode(message.code)
}

internal class ExamRuntimeStartPrecheckCallbacks(
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    val applyVirtualEnvironmentDiagnostics: (com.example.coblaxexamlock.model.VirtualEnvironmentDiagnostics, Boolean) -> Unit,
    val refreshReverseEngineeringStatus: suspend () -> Unit,
    val refreshIntegrityGuard: suspend () -> Unit,
    val refreshScreenPinningDiagnostics: () -> Unit,
    val refreshKeyboardSecurity: (Boolean) -> Unit,
    val refreshBluetoothSecurity: (Boolean) -> Unit,
    val refreshDeviceIntegritySecurity: (Boolean) -> Unit,
    val updateStartExamPreflight: (StartExamPreflightStep, String?) -> Unit,
    val hideStartExamPreflight: () -> Unit,
    val applyStartExamBlockMessage: (StartExamBlockMessage) -> Unit,
    val refreshDeviceTimeSecurity: (String, Boolean) -> DeviceTimeSecurityStatus,
    val applyNetworkReadinessStatus: (String, NetworkReadinessStatus) -> Unit,
    val checkSignatureIntegrity: (Boolean) -> SignatureIntegrityResult,
    val currentGeofenceEventDetails: (String, GeofenceSecurityStatus) -> String,
    val currentFakeLocationEventDetails: (String, LocationSpoofSecurityStatus) -> String,
    val ensureDeviceOwnerLockTaskActive: () -> Boolean,
    val refreshDpcRuntimeStatus: () -> DpcRuntimeStatus,
    val requestBluetoothPermission: () -> Unit,
    val requestLocationPermission: () -> Unit,
    val launchFinalLocationValidation: (Long) -> Unit,
    val debugLogExamStart: (String) -> Unit
)

internal fun shouldRetryStartExamNetworkStatus(status: NetworkReadinessStatus): Boolean {
    return when (status.userFacingVerdict) {
        NetworkReadinessUserVerdict.Offline,
        NetworkReadinessUserVerdict.CaptivePortal,
        NetworkReadinessUserVerdict.AirplaneMode -> true
        NetworkReadinessUserVerdict.Stable,
        NetworkReadinessUserVerdict.Unvalidated,
        NetworkReadinessUserVerdict.DnsFailed,
        NetworkReadinessUserVerdict.Slow,
        NetworkReadinessUserVerdict.VpnActive,
        NetworkReadinessUserVerdict.Unstable -> false
    }
}

internal suspend fun readStartExamNetworkStatusWithRecovery(
    attempts: Int = StartNetworkRecoveryAttempts,
    retryDelayMillis: Long = StartNetworkRecoveryRetryDelayMillis,
    readStatus: suspend () -> NetworkReadinessStatus
): NetworkReadinessStatus {
    var latestStatus = readStatus()
    repeat(attempts.coerceAtLeast(1) - 1) {
        if (!shouldRetryStartExamNetworkStatus(latestStatus)) {
            return latestStatus
        }
        if (retryDelayMillis > 0L) {
            delay(retryDelayMillis)
        }
        latestStatus = readStatus()
    }
    return latestStatus
}

internal suspend fun runExamRuntimeStartPrechecks(
    context: Context,
    uiLanguage: com.example.coblaxexamlock.model.UiLanguage,
    payload: ExamQrPayload,
    lockTaskBridge: ActivityLockTaskBridge,
    screenPinningMode: ScreenPinningMode,
    screenPinningAvailable: Boolean,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    overlayRiskResult: OverlayRiskResult,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    webViewRecoveryStateName: String,
    batteryStatus: ExamBatteryStatus,
    geofenceConfigParseResult: GeofenceConfigParseResult,
    effectiveLocationPolicySource: LocationPolicySource,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState,
    flowUiState: ExamRuntimeFlowUiState,
    securityUiState: ExamRuntimeSecurityUiState,
    adminUiState: ExamRuntimeAdminUiState,
    accessibilityGuardEnabledState: MutableState<Boolean>,
    bypassScreenPinning: Boolean,
    bypassOverlay: Boolean,
    bypassVpn: Boolean,
    bypassDeviceTime: Boolean,
    bypassKeyboardPolicy: Boolean,
    bypassBluetooth: Boolean,
    bypassAccessibility: Boolean,
    bypassAdb: Boolean,
    bypassVirtualEnvironment: Boolean,
    bypassRoot: Boolean,
    bypassReverseEngineering: Boolean,
    bypassApkIntegrity: Boolean,
    bypassScreenRecorder: Boolean,
    bypassDisplayMirror: Boolean,
    bypassMultiWindow: Boolean,
    bypassGeofence: Boolean,
    bypassFakeLocation: Boolean,
    callbacks: ExamRuntimeStartPrecheckCallbacks
) {
    if (flowUiState.webViewSessionResetInFlight.value) {
        return
    }
    fun updatePreflight(step: StartExamPreflightStep, detail: String? = null) {
        callbacks.updateStartExamPreflight(step, detail)
    }

    fun applyBlock(message: StartExamBlockMessage) {
        callbacks.hideStartExamPreflight()
        callbacks.applyStartExamBlockMessage(message)
    }

    val startExamPressedAt = SystemClock.elapsedRealtime()
    flowUiState.webViewSessionResetError.value = null
    callbacks.recordAction("START_EXAM_PRESSED", "-", DiagnosticEventLevel.INFO)
    updatePreflight(StartExamPreflightStep.TamperAndIntegrity)
    val startVirtualEnvironmentDiagnostics = getVirtualEnvironmentDiagnosticsOnIo(
        context = context,
        forceRefresh = true
    )
    callbacks.applyVirtualEnvironmentDiagnostics(startVirtualEnvironmentDiagnostics, false)
    debugMeasureExamStartSuspendWork("startExamSession:tampers") {
        callbacks.refreshReverseEngineeringStatus()
        callbacks.refreshIntegrityGuard()
    }
    val reverseEngineeringDetected = securityUiState.tamperDetected.value
    val apkIntegrityDetected = securityUiState.integrityTamperDetected.value
    val tamperBlock = resolveStartExamTamperBlockMessage(
        uiLanguage = uiLanguage,
        reverseEngineeringDetected = reverseEngineeringDetected,
        reverseEngineeringSummary = securityUiState.tamperSummary.value,
        reverseEngineeringBypassActive = bypassReverseEngineering,
        apkIntegrityDetected = apkIntegrityDetected,
        apkIntegritySummary = securityUiState.integritySummary.value,
        apkIntegrityBypassActive = bypassApkIntegrity
    )
    if (tamperBlock != null) {
        applyBlock(tamperBlock)
        return
    }
    if (reverseEngineeringDetected && bypassReverseEngineering) {
        callbacks.recordAction(
            "REVERSE_ENGINEERING_BYPASS_ACTIVE",
            securityUiState.tamperSummary.value.ifBlank { "-" },
            DiagnosticEventLevel.SECURITY
        )
    }
    if (apkIntegrityDetected && bypassApkIntegrity) {
        callbacks.recordAction(
            "APK_INTEGRITY_BYPASS_ACTIVE",
            securityUiState.integritySummary.value.ifBlank { "-" },
            DiagnosticEventLevel.SECURITY
        )
    }
    if ((reverseEngineeringDetected && bypassReverseEngineering) ||
        (apkIntegrityDetected && bypassApkIntegrity)
    ) {
        callbacks.recordAction(
            "START_EXAM_TAMPER_BYPASSED",
            "reverse_detected=$reverseEngineeringDetected reverse_bypass=$bypassReverseEngineering | " +
                "apk_integrity_detected=$apkIntegrityDetected apk_integrity_bypass=$bypassApkIntegrity",
            DiagnosticEventLevel.SECURITY
        )
    }

    updatePreflight(StartExamPreflightStep.DeviceSecurity)
    callbacks.refreshScreenPinningDiagnostics()
    if (screenPinningMode == ScreenPinningMode.Enforced && !lockTaskBridge.active()) {
        callbacks.ensureDeviceOwnerLockTaskActive()
    }
    val latestAccessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context)
    val latestAccessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
    accessibilityGuardEnabledState.value = latestAccessibilityGuardEnabled
    val screenPinningBlock = resolveStartExamScreenPinningBlockMessage(
        uiLanguage = uiLanguage,
        screenPinningMode = screenPinningMode,
        screenPinningAvailable = screenPinningAvailable,
        screenPinningActive = lockTaskBridge.active(),
        accessibilityGuardAvailable = latestAccessibilityGuardAvailable,
        accessibilityGuardEnabled = latestAccessibilityGuardEnabled
    )
    if (screenPinningBlock != null) {
        applyBlock(screenPinningBlock)
        return
    } else if (
        screenPinningMode == ScreenPinningMode.Enforced &&
        !screenPinningAvailable &&
        latestAccessibilityGuardEnabled
    ) {
        callbacks.recordAction(
            "ACCESSIBILITY_GUARD_ENABLED_REQUIRED",
            "screen_pinning_available=false | accessibility_guard_enabled=true",
            DiagnosticEventLevel.INFO
        )
    }

    debugMeasureExamStartWork("startExamSession:device_prechecks") {
        callbacks.refreshScreenPinningDiagnostics()
        if (screenPinningMode == ScreenPinningMode.Enforced && !lockTaskBridge.active()) {
            callbacks.ensureDeviceOwnerLockTaskActive()
        }
        accessibilityGuardEnabledState.value = isExamGuardAccessibilityEnabled(context)
        callbacks.refreshKeyboardSecurity(false)
        callbacks.refreshBluetoothSecurity(false)
        callbacks.refreshDeviceIntegritySecurity(false)
    }
    val devicePrecheckScreenPinningBlock = resolveStartExamScreenPinningBlockMessage(
        uiLanguage = uiLanguage,
        screenPinningMode = screenPinningMode,
        screenPinningAvailable = screenPinningAvailable,
        screenPinningActive = lockTaskBridge.active(),
        accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context),
        accessibilityGuardEnabled = accessibilityGuardEnabledState.value,
        phaseSuffix = "phase=device_prechecks"
    )
    if (devicePrecheckScreenPinningBlock != null) {
        applyBlock(devicePrecheckScreenPinningBlock)
        return
    }

    updatePreflight(StartExamPreflightStep.DeviceTime)
    val startDeviceTimeStatus = callbacks.refreshDeviceTimeSecurity("start_exam_precheck", true)
    val startDeviceTimeBlock = resolveStartExamDeviceTimeBlockMessage(
        uiLanguage = uiLanguage,
        trigger = "start_exam_precheck",
        status = startDeviceTimeStatus
    )
    if (startDeviceTimeBlock != null) {
        applyBlock(startDeviceTimeBlock)
        return
    }

    updatePreflight(
        StartExamPreflightStep.NetworkDns,
        localized(uiLanguage, "Checking global DNS and the exam host DNS.", "Cek DNS global dan DNS host ujian.")
    )
    val startNetworkStatus = readStartExamNetworkStatusWithRecovery {
        readNetworkReadinessStatusWithExamHostProbe(context, payload.examUrl)
    }
    callbacks.applyNetworkReadinessStatus("start_exam_precheck", startNetworkStatus)
    if (!bypassVpn) {
        val startVpnBlock = resolveStartExamVpnBlockMessage(
            uiLanguage = uiLanguage,
            status = startNetworkStatus
        )
        if (startVpnBlock != null) {
            applyBlock(startVpnBlock)
            return
        }
    }
    resolveStartExamNetworkReachabilityBlockMessage(
        uiLanguage = uiLanguage,
        status = startNetworkStatus
    )?.let { networkBlock ->
        applyBlock(networkBlock)
        return
    }
    updatePreflight(StartExamPreflightStep.ServerProbe)
    val startServerProbe = probeExamServerFooterStatus(payload.examUrl)
    callbacks.recordAction(
        startServerProbe.eventCode,
        buildExamServerProbeDetails(
            trigger = "start_exam_precheck",
            host = startServerProbe.host,
            method = startServerProbe.method,
            code = startServerProbe.code,
            latencyMs = startServerProbe.latencyMs,
            reason = startServerProbe.reason
        ),
        startServerProbe.eventLevel
    )
    resolveStartExamServerProbeBlockMessage(
        uiLanguage = uiLanguage,
        result = startServerProbe
    )?.let { serverBlock ->
        applyBlock(serverBlock)
        return
    }
    val startDpcRuntimeStatus = callbacks.refreshDpcRuntimeStatus()

    updatePreflight(StartExamPreflightStep.HealthSnapshot)
    val startHealthSnapshot = buildPreExamHealthSnapshot(
        PreExamHealthCheckInput(
            compatibilityProfile = deviceCompatibilityProfile,
            screenPinningAvailable = screenPinningAvailable,
            screenPinningActive = lockTaskBridge.active(),
            screenPinningBypassed = bypassScreenPinning,
            accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context),
            accessibilityGuardEnabled = accessibilityGuardEnabledState.value,
            overlayRiskResult = overlayRiskResult,
            overlayBypassed = bypassOverlay,
            networkReadinessStatus = startNetworkStatus,
            vpnBypassed = bypassVpn,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            webViewRecoveryState = webViewRecoveryStateName,
            webViewSessionResetInFlight = flowUiState.webViewSessionResetInFlight.value,
            webViewSessionResetError = flowUiState.webViewSessionResetError.value,
            geofenceRuntimeStatus = buildExamRuntimeGeofenceStatus(
                geofenceStatus = securityUiState.geofenceSecurityStatus.value,
                policySource = effectiveLocationPolicySource,
                violationCount = flowUiState.geofenceViolationCount.intValue,
                lastTrigger = flowUiState.lastGeofenceTrigger.value,
                lastDetectedAt = flowUiState.lastGeofenceAt.value,
                lastContext = flowUiState.lastGeofenceContext.value
            ),
            geofenceBypassed = bypassGeofence,
            fakeLocationRuntimeStatus = buildExamRuntimeFakeLocationStatus(
                fakeLocationStatus = securityUiState.fakeLocationSecurityStatus.value,
                violationCount = flowUiState.fakeLocationViolationCount.intValue,
                lastTrigger = flowUiState.lastFakeLocationTrigger.value,
                lastDetectedAt = flowUiState.lastFakeLocationAt.value,
                lastContext = flowUiState.lastFakeLocationContext.value
            ),
            fakeLocationBypassed = bypassFakeLocation,
            deviceTimeSecurityStatus = startDeviceTimeStatus,
            deviceTimeBypassed = bypassDeviceTime,
            batteryStatus = batteryStatus,
            dpcRuntimeStatus = startDpcRuntimeStatus,
            generatedAtElapsedMs = SystemClock.elapsedRealtime()
        )
    )
    val healthBlocker = preExamHealthStartBlocker(startHealthSnapshot)
    if (healthBlocker != null) {
        callbacks.recordAction(
            ExamRuntimeHardeningDiagnostics.StartExamBlockedHealthCheck,
            "category=${healthBlocker.category.name} | verdict=${healthBlocker.verdict.name} | detail=${healthBlocker.detail}",
            DiagnosticEventLevel.WARNING
        )
        adminUiState.securityIssueDialogTitle.value = localized(
            uiLanguage,
            "Pre-Exam Health Check",
            "Health Check Sebelum Ujian"
        )
        adminUiState.securityIssueDialogMessage.value = buildString {
            append(healthBlocker.title)
            append("\n\n")
            append(healthBlocker.detail)
            if (!healthBlocker.quickFix.isNullOrBlank()) {
                append("\n\n")
                append(healthBlocker.quickFix)
            }
        }
        callbacks.hideStartExamPreflight()
        return
    }

    updatePreflight(StartExamPreflightStep.StaticSecurity)
    val signatureResult = debugMeasureExamStartWork("startExamSession:signature_check") {
        callbacks.checkSignatureIntegrity(!bypassApkIntegrity)
    }
    if (ExamPolicyEngine.shouldBlock(signatureResult) && !bypassApkIntegrity) {
        callbacks.recordAction(
            "START_EXAM_BLOCKED_SIGNATURE",
            signatureResult.reason,
            DiagnosticEventLevel.WARNING
        )
        callbacks.hideStartExamPreflight()
        return
    }
    if (ExamPolicyEngine.shouldBlock(signatureResult) && bypassApkIntegrity) {
        callbacks.recordAction(
            "APK_INTEGRITY_BYPASS_ACTIVE",
            "signature=${signatureResult.reason}",
            DiagnosticEventLevel.SECURITY
        )
    }

    val builtInKeyboardNeeded = !bypassKeyboardPolicy && !flowUiState.lastKeyboardAllowed.value
    val bluetoothPermissionReady =
        securityUiState.bluetoothPermissionGranted.value || !requiresBluetoothExamPermission()
    flowUiState.useBuiltInExamKeyboard.value = builtInKeyboardNeeded
    flowUiState.showBuiltInExamKeyboard.value = builtInKeyboardNeeded
    flowUiState.builtInKeyboardShiftEnabled.value = false

    if (!bypassBluetooth) {
        if (!bluetoothPermissionReady) {
            updatePreflight(StartExamPreflightStep.DeviceSecurity)
            callbacks.hideStartExamPreflight()
            callbacks.requestBluetoothPermission()
            return
        }
        if (securityUiState.bluetoothEnabled.value) {
            securityUiState.showBluetoothViolationDialog.value = true
            callbacks.hideStartExamPreflight()
            return
        }
    }

    val staticSecurityBlock = resolveStartExamStaticSecurityBlockMessage(
        bypassAccessibility = bypassAccessibility,
        accessibilityServiceEnabled = securityUiState.accessibilityServiceEnabled.value,
        bypassAdb = bypassAdb,
        developerOptionsEnabled = securityUiState.developerOptionsEnabled.value,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        virtualEnvironmentDetected = securityUiState.virtualEnvironmentDetected.value,
        adbEnabled = securityUiState.adbEnabled.value,
        adbInsecureSystemProperty = securityUiState.adbInspection.value.insecureSystemProperty,
        bypassRoot = bypassRoot,
        rootSecurityStatus = securityUiState.rootSecurityStatus.value,
        bypassScreenRecorder = bypassScreenRecorder,
        screenRecorderPackages = SecurityDetectorCache.readScreenRecorderPackages(
            context = context,
            forceRefresh = true
        ),
        bypassDisplayMirror = bypassDisplayMirror,
        externalDisplayDetected = getExternalDisplayCount(context) > 0,
        bypassMultiWindow = bypassMultiWindow,
        multiWindowDetected = isInAnySplitMode(context)
    )
    if (staticSecurityBlock != null) {
        applyBlock(staticSecurityBlock)
        return
    }

    securityUiState.overlayGuardPermissionGranted.value =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    securityUiState.overlayAppsDetected.value = emptyList()
    securityUiState.showOverlayAppViolationDialog.value = false

    val geofenceEnabled = geofenceConfigParseResult.enabled
    if (geofenceEnabled && !bypassGeofence && geofenceConfigParseResult.config == null) {
        val status = evaluateGeofenceSecurity(
            configResult = geofenceConfigParseResult,
            permissionGranted = hasLocationPermissionForWifi(context),
            preciseLocationGranted = hasFineLocationPermission(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            locationSnapshot = null,
            bypassState = geofenceBypassState
        )
        securityUiState.geofenceSecurityStatus.value = status
        securityUiState.geofenceEvaluation.value = status.geofenceEvaluation
        applyBlock(
            resolveStartExamGeofenceConfigBlockMessage(
                uiLanguage = uiLanguage,
                details = callbacks.currentGeofenceEventDetails("start_exam", status)
            )
        )
        return
    }

    val coarseOrFineGranted = hasLocationPermissionForWifi(context)
    val preciseLocationGranted = hasFineLocationPermission(context)
    val preciseLocationRequiredForStart = geofenceEnabled && !bypassGeofence
    if (
        (preciseLocationRequiredForStart && !preciseLocationGranted) ||
        (!preciseLocationRequiredForStart && !bypassFakeLocation && !coarseOrFineGranted)
    ) {
        updatePreflight(StartExamPreflightStep.LocationPermission)
        flowUiState.pendingStartExamAfterLocationPermission.value = true
        flowUiState.geofencePermissionRequestInFlight.value = true
        callbacks.recordAction(
            "LOCATION_PERMISSION_REQUESTED",
            "trigger=start_exam",
            DiagnosticEventLevel.WARNING
        )
        callbacks.hideStartExamPreflight()
        callbacks.requestLocationPermission()
        callbacks.debugLogExamStart(
            "startExamSession waiting for location permission after ${SystemClock.elapsedRealtime() - startExamPressedAt} ms"
        )
        return
    }

    if (!bypassGeofence && geofenceEnabled && !isLocationServicesEnabled(context)) {
        val status = evaluateGeofenceSecurity(
            configResult = geofenceConfigParseResult,
            permissionGranted = true,
            preciseLocationGranted = true,
            locationServicesEnabled = false,
            locationSnapshot = null,
            bypassState = geofenceBypassState
        )
        securityUiState.geofenceSecurityStatus.value = status
        securityUiState.geofenceEvaluation.value = status.geofenceEvaluation
        applyBlock(
            resolveStartExamGeofenceLocationDisabledBlockMessage(
                uiLanguage = uiLanguage,
                details = callbacks.currentGeofenceEventDetails("start_exam", status)
            )
        )
        return
    }

    if (!bypassFakeLocation && !isLocationServicesEnabled(context)) {
        val status = evaluateFakeLocationSecurity(
            monitoringEnabled = true,
            permissionGranted = hasLocationPermissionForWifi(context),
            locationServicesEnabled = false,
            locationSnapshot = null,
            fixQualityStatus = evaluateLocationFixQuality(null),
            developerOptionsEnabled = securityUiState.developerOptionsEnabled.value,
            suspiciousFakeLocationPackages =
                SecurityDetectorCache.readSuspiciousFakeLocationPackages(
                    context = context,
                    forceRefresh = true
                ),
            bypassState = fakeLocationBypassState
        )
        securityUiState.fakeLocationSecurityStatus.value = status
        applyBlock(
            resolveStartExamFakeLocationServicesDisabledBlockMessage(
                uiLanguage = uiLanguage,
                details = callbacks.currentFakeLocationEventDetails("start_exam", status)
            )
        )
        return
    }

    updatePreflight(StartExamPreflightStep.LocationValidation)
    callbacks.launchFinalLocationValidation(startExamPressedAt)
}

internal class ExamRuntimeCompleteStartCallbacks(
    val setAccessibilityGuardFallbackActive: (Boolean) -> Unit,
    val setAccessibilityGuardLastReason: (String?) -> Unit,
    val setAccessibilityGuardLastForeignPackage: (String?) -> Unit,
    val setAccessibilityGuardLastEventType: (String?) -> Unit,
    val setAccessibilityGuardLastDetectedAt: (String?) -> Unit,
    val setAccessibilityGuardAlarmSeverity: (String) -> Unit,
    val setForcedExitViolationCount: (Int) -> Unit,
    val setPendingForcedExitViolation: (Boolean) -> Unit,
    val setShowForcedExitAlarm: (Boolean) -> Unit,
    val setLockTaskStateBeforePinningRequest: (String) -> Unit,
    val setLockTaskStateAfterPinningRequest: (String) -> Unit,
    val setScreenPinningRequestOutcome: (String) -> Unit,
    val setScreenPinningDialogLikelyShown: (Boolean) -> Unit,
    val setScreenPinningUserActionInference: (String) -> Unit,
    val setScreenPinningActivationDurationMs: (Long?) -> Unit,
    val setExamSessionCancelledByPinningFailure: (Boolean) -> Unit,
    val setLockTaskRequestPending: (Boolean) -> Unit,
    val setPinningActivationState: (PinningActivationState) -> Unit,
    val setPinningActivationStartedAtElapsedMs: (Long?) -> Unit,
    val setPinningSuppressedTransitionCount: (Int) -> Unit,
    val setScreenPinningMessage: (String?) -> Unit,
    val setWebViewErrorMessage: (String?) -> Unit,
    val setExitOnSecurityIssueDialogDismiss: (Boolean) -> Unit,
    val resetPreparationSecurityEpisodes: () -> Unit,
    val prepareCleanExamWebViewSessionForStart: suspend () -> Boolean,
    val armExamRuntimeMonitoring: (String) -> Unit,
    val finalizeExamSessionStart: (Boolean) -> Unit,
    val ensureDeviceOwnerLockTaskActive: () -> Boolean,
    val clearAppSwitchSuppression: () -> Unit,
    val setAppSwitchSuppression: (AppSwitchSuppressionReason) -> Unit,
    val hideStartExamPreflight: () -> Unit,
    val applyStartExamBlockMessage: (StartExamBlockMessage) -> Unit,
    val recordAction: (String, String, DiagnosticEventLevel) -> Unit
)

internal fun completeExamRuntimeStartAfterPrechecks(
    context: Context,
    lockTaskBridge: ActivityLockTaskBridge,
    coroutineScope: CoroutineScope,
    uiLanguage: com.example.coblaxexamlock.model.UiLanguage,
    isIndonesian: Boolean,
    screenPinningMode: ScreenPinningMode,
    screenPinningAvailable: Boolean,
    accessibilityGuardEnabled: Boolean,
    lockTaskRequestPending: Boolean,
    geofenceStartValidationInFlight: Boolean,
    webViewSessionResetInFlight: Boolean,
    examGuardArmed: Boolean,
    deviceCompatibilityProfile: com.example.coblaxexamlock.DeviceCompatibilityProfile,
    callbacks: ExamRuntimeCompleteStartCallbacks
) {
    val launchExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e(
            ExamRuntimeHardeningLogTag,
            "StartSessionCoordinator uncaught coroutine exception: ${throwable.javaClass.simpleName}",
            throwable
        )
    }
    if (lockTaskRequestPending || geofenceStartValidationInFlight || webViewSessionResetInFlight) {
        return
    }

    if (
        screenPinningMode == ScreenPinningMode.Enforced &&
        !screenPinningAvailable &&
        accessibilityGuardEnabled
    ) {
        launchAccessibilityGuardFallbackExamStart(
            context = context,
            lockTaskBridge = lockTaskBridge,
            coroutineScope = coroutineScope,
            examGuardArmed = examGuardArmed,
            updateFallbackUiState = { beforeState ->
                callbacks.setAccessibilityGuardFallbackActive(true)
                callbacks.setAccessibilityGuardLastReason(null)
                callbacks.setAccessibilityGuardLastForeignPackage(null)
                callbacks.setAccessibilityGuardLastEventType(null)
                callbacks.setAccessibilityGuardLastDetectedAt(null)
                callbacks.setAccessibilityGuardAlarmSeverity(ExamAlarmSeverity.Warning.name)
                callbacks.setForcedExitViolationCount(0)
                callbacks.setPendingForcedExitViolation(false)
                callbacks.setShowForcedExitAlarm(false)
                callbacks.setLockTaskStateBeforePinningRequest(beforeState)
                callbacks.setLockTaskStateAfterPinningRequest(beforeState)
                callbacks.setScreenPinningRequestOutcome("Accessibility guard fallback")
                callbacks.setScreenPinningDialogLikelyShown(false)
                callbacks.setScreenPinningUserActionInference("Tidak diminta; Accessibility Exam Guard aktif")
                callbacks.setScreenPinningActivationDurationMs(0L)
                callbacks.setExamSessionCancelledByPinningFailure(false)
                callbacks.setLockTaskRequestPending(false)
                callbacks.setPinningActivationState(PinningActivationState.ActiveConfirmed)
                callbacks.setPinningActivationStartedAtElapsedMs(null)
                callbacks.setPinningSuppressedTransitionCount(0)
                callbacks.setScreenPinningMessage(null)
                callbacks.setWebViewErrorMessage(null)
                callbacks.setExitOnSecurityIssueDialogDismiss(false)
            },
            recordAction = { code, details, level -> callbacks.recordAction(code, details, level) },
            clearAppSwitchSuppression = callbacks.clearAppSwitchSuppression,
            resetPreparationSecurityEpisodes = callbacks.resetPreparationSecurityEpisodes,
            prepareCleanExamWebViewSessionForStart = callbacks.prepareCleanExamWebViewSessionForStart,
            armExamRuntimeMonitoring = callbacks.armExamRuntimeMonitoring,
            finalizeExamSessionStart = callbacks.finalizeExamSessionStart,
            onCleanSessionFailed = {
                callbacks.setAccessibilityGuardFallbackActive(false)
                callbacks.hideStartExamPreflight()
            },
            onStartFailed = { throwable ->
                callbacks.setAccessibilityGuardFallbackActive(false)
                callbacks.hideStartExamPreflight()
                callbacks.applyStartExamBlockMessage(
                    resolveStartExamUnexpectedFailureBlockMessage(
                        uiLanguage = uiLanguage,
                        phase = "accessibility_guard_fallback",
                        throwable = throwable
                    )
                )
            },
            exceptionHandler = launchExceptionHandler
        )
        return
    }

    AccessibilityExamGuardStore.disarm(context)
    callbacks.setAccessibilityGuardFallbackActive(false)

    if (screenPinningMode == ScreenPinningMode.Bypassed) {
        val bypassState = ScreenPinningEnforcer.launchState(screenPinningMode, lockTaskBridge)
        callbacks.recordAction(bypassState.eventCode, bypassState.eventDetails, DiagnosticEventLevel.INFO)
        callbacks.setLockTaskStateBeforePinningRequest(bypassState.beforeState)
        callbacks.setLockTaskStateAfterPinningRequest(bypassState.afterState)
        callbacks.setScreenPinningRequestOutcome(bypassState.outcome)
        callbacks.setScreenPinningDialogLikelyShown(bypassState.dialogLikelyShown)
        callbacks.setScreenPinningUserActionInference(bypassState.userActionInference)
        callbacks.setScreenPinningActivationDurationMs(bypassState.activationDurationMs)
        callbacks.setExamSessionCancelledByPinningFailure(false)
        callbacks.setLockTaskRequestPending(false)
        callbacks.setPinningActivationState(PinningActivationState.Idle)
        callbacks.setPinningActivationStartedAtElapsedMs(null)
        callbacks.setPinningSuppressedTransitionCount(0)
        callbacks.clearAppSwitchSuppression()
        callbacks.setScreenPinningMessage(null)
        callbacks.setWebViewErrorMessage(null)
        callbacks.setExitOnSecurityIssueDialogDismiss(false)
        callbacks.resetPreparationSecurityEpisodes()
        coroutineScope.launch(launchExceptionHandler) {
            try {
                if (!callbacks.prepareCleanExamWebViewSessionForStart()) {
                    return@launch
                }
                if (!examGuardArmed) {
                    callbacks.armExamRuntimeMonitoring("start_exam_pressed")
                }
                callbacks.finalizeExamSessionStart(false)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                callbacks.hideStartExamPreflight()
                callbacks.applyStartExamBlockMessage(
                    resolveStartExamUnexpectedFailureBlockMessage(
                        uiLanguage = uiLanguage,
                        phase = "screen_pinning_bypassed_start",
                        throwable = throwable
                    )
                )
            }
        }
        return
    }

    if (screenPinningMode == ScreenPinningMode.Enforced && !lockTaskBridge.active()) {
        callbacks.ensureDeviceOwnerLockTaskActive()
    }

    if (lockTaskBridge.active()) {
        val activeState = lockTaskBridge.stateLabel()
        callbacks.setLockTaskStateBeforePinningRequest(activeState)
        callbacks.setLockTaskStateAfterPinningRequest(activeState)
        callbacks.setScreenPinningRequestOutcome(ScreenPinningSignals.successOutcome())
        callbacks.setScreenPinningDialogLikelyShown(false)
        callbacks.setScreenPinningUserActionInference("Sudah aktif; request pinning dilewati")
        callbacks.setScreenPinningActivationDurationMs(0L)
        callbacks.setExamSessionCancelledByPinningFailure(false)
        callbacks.setLockTaskRequestPending(false)
        callbacks.setPinningActivationState(PinningActivationState.ActiveConfirmed)
        callbacks.setPinningActivationStartedAtElapsedMs(null)
        callbacks.setPinningSuppressedTransitionCount(0)
        callbacks.clearAppSwitchSuppression()
        callbacks.setScreenPinningMessage(null)
        callbacks.setWebViewErrorMessage(null)
        callbacks.setExitOnSecurityIssueDialogDismiss(false)
        callbacks.recordAction(
            ScreenPinningSignals.eventActive(),
            "already_active_before_request | state=$activeState",
            DiagnosticEventLevel.INFO
        )
        callbacks.recordAction(
            ExamRuntimeHardeningDiagnostics.ScreenPinningAlreadyActive,
            "state=$activeState | request_pending=false",
            DiagnosticEventLevel.INFO
        )
        callbacks.recordAction(
            ExamRuntimeHardeningDiagnostics.ScreenPinningRequestSkippedAlreadyActive,
            "state=$activeState | policy_skip_if_active=${deviceCompatibilityProfile.skipScreenPinningRequestWhenAlreadyActive}",
            DiagnosticEventLevel.INFO
        )
        callbacks.recordAction(
            ExamRuntimeHardeningDiagnostics.PinningActiveConfirmed,
            "already_active_before_request=true | state=$activeState | duration_ms=0",
            DiagnosticEventLevel.INFO
        )
        callbacks.resetPreparationSecurityEpisodes()
        if (!examGuardArmed) {
            callbacks.armExamRuntimeMonitoring("start_exam_pressed_pinning_already_active")
        }
        lockTaskBridge.engage(allowLockTask = false)
        coroutineScope.launch(launchExceptionHandler) {
            try {
                if (!callbacks.prepareCleanExamWebViewSessionForStart()) {
                    return@launch
                }
                callbacks.finalizeExamSessionStart(true)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                callbacks.hideStartExamPreflight()
                callbacks.applyStartExamBlockMessage(
                    resolveStartExamUnexpectedFailureBlockMessage(
                        uiLanguage = uiLanguage,
                        phase = "screen_pinning_active_start",
                        throwable = throwable
                    )
                )
            }
        }
        return
    }

    if (screenPinningMode == ScreenPinningMode.Enforced && screenPinningAvailable) {
        callbacks.hideStartExamPreflight()
        callbacks.applyStartExamBlockMessage(
            resolveStartExamScreenPinningBlockMessage(
                uiLanguage = uiLanguage,
                screenPinningMode = screenPinningMode,
                screenPinningAvailable = screenPinningAvailable,
                screenPinningActive = false,
                accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context),
                accessibilityGuardEnabled = accessibilityGuardEnabled,
                phaseSuffix = "phase=final_precheck"
            ) ?: StartExamBlockMessage(
                code = ExamRuntimeHardeningDiagnostics.StartExamBlockedScreenPinningInactive,
                details = "screen_pinning_available=true | lock_task_active=false | bypass=false | phase=final_precheck",
                title = localized(uiLanguage, "Start Screen Pinning First", "Start Screen Pinning Dulu"),
                message = localized(
                    uiLanguage,
                    "Start Screen Pinning first from Preparation, confirm the Android dialog, then press Start Exam.",
                    "Jalankan Start Screen Pinning dulu dari Preparation, konfirmasi dialog Android, lalu tekan Mulai Ujian."
                )
            )
        )
        return
    }

    if (!examGuardArmed) {
        callbacks.armExamRuntimeMonitoring("start_exam_pressed")
    }

    val requestState = ScreenPinningEnforcer.launchState(screenPinningMode, lockTaskBridge)
    callbacks.setLockTaskStateBeforePinningRequest(requestState.beforeState)
    callbacks.setLockTaskStateAfterPinningRequest(requestState.afterState)
    callbacks.setScreenPinningRequestOutcome(requestState.outcome)
    callbacks.setScreenPinningDialogLikelyShown(requestState.dialogLikelyShown)
    callbacks.setScreenPinningUserActionInference(requestState.userActionInference)
    callbacks.setScreenPinningActivationDurationMs(requestState.activationDurationMs)
    callbacks.setExamSessionCancelledByPinningFailure(false)
    callbacks.setLockTaskRequestPending(true)
    callbacks.setPinningActivationState(PinningActivationState.Requested)
    callbacks.setPinningActivationStartedAtElapsedMs(SystemClock.elapsedRealtime())
    callbacks.setPinningSuppressedTransitionCount(0)
    callbacks.recordAction(requestState.eventCode, requestState.eventDetails, DiagnosticEventLevel.INFO)
    callbacks.recordAction(
        ExamRuntimeHardeningDiagnostics.PinningStartRequested,
        "before=${requestState.beforeState} | state=${requestState.afterState} | grace_ms=$PinningActivationGraceWindowMillis",
        DiagnosticEventLevel.INFO
    )
    callbacks.recordAction(
        ExamRuntimeHardeningDiagnostics.PinningDialogExpected,
        "screen_pinning_dialog_expected=true | keep_app_foreground=true",
        DiagnosticEventLevel.INFO
    )
    callbacks.setAppSwitchSuppression(AppSwitchSuppressionReason.ScreenPinningRequest)
    callbacks.setScreenPinningMessage(
        ScreenPinningEnforcer.activatingMessage(
            isIndonesian = isIndonesian,
            purpose = PinningActivationPurpose.ExamStart
        )
    )
    callbacks.setWebViewErrorMessage(null)
    callbacks.setExitOnSecurityIssueDialogDismiss(false)
    callbacks.hideStartExamPreflight()
}

internal class ExamRuntimeStartLocationValidationCallbacks(
    val isGeofenceStartValidationInFlight: () -> Boolean,
    val setGeofenceStartValidationInFlight: (Boolean) -> Unit,
    val resolveStartExamLocationValidation: suspend () -> SplitLocationSecurityStatus,
    val currentGeofenceEventDetails: (String, GeofenceSecurityStatus) -> String,
    val currentFakeLocationEventDetails: (String, LocationSpoofSecurityStatus) -> String,
    val updateStartExamPreflight: (StartExamPreflightStep, String?) -> Unit,
    val hideStartExamPreflight: () -> Unit,
    val applyStartExamBlockMessage: (StartExamBlockMessage) -> Unit,
    val refreshDeviceTimeSecurity: (String, Boolean) -> DeviceTimeSecurityStatus,
    val completeStartExamSessionAfterPrechecks: () -> Unit,
    val debugLogExamStart: (String) -> Unit
)

internal fun launchExamRuntimeStartLocationValidation(
    context: Context,
    coroutineScope: CoroutineScope,
    uiLanguage: com.example.coblaxexamlock.model.UiLanguage,
    payload: ExamQrPayload,
    bypassGeofence: Boolean,
    bypassFakeLocation: Boolean,
    startExamPressedAt: Long,
    callbacks: ExamRuntimeStartLocationValidationCallbacks
) {
    if (callbacks.isGeofenceStartValidationInFlight()) {
        return
    }

    val launchExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e(
            ExamRuntimeHardeningLogTag,
            "StartSessionCoordinator location validation uncaught coroutine exception: ${throwable.javaClass.simpleName}",
            throwable
        )
    }

    fun updatePreflight(step: StartExamPreflightStep, detail: String? = null) {
        callbacks.updateStartExamPreflight(step, detail)
    }

    fun applyBlock(message: StartExamBlockMessage) {
        callbacks.hideStartExamPreflight()
        callbacks.applyStartExamBlockMessage(message)
    }

    updatePreflight(StartExamPreflightStep.LocationValidation)
    callbacks.setGeofenceStartValidationInFlight(true)
    coroutineScope.launch(launchExceptionHandler) {
        try {
            val latestLocationStatus = debugMeasureExamStartSuspendWork("startExamSession:location_validation") {
                callbacks.resolveStartExamLocationValidation()
            }
            callbacks.setGeofenceStartValidationInFlight(false)
            val locationBlockMessage = resolveStartExamLocationBlockMessage(
                uiLanguage = uiLanguage,
                latestLocationStatus = latestLocationStatus,
                bypassGeofence = bypassGeofence,
                bypassFakeLocation = bypassFakeLocation,
                geofenceDetails = { geofenceStatus ->
                    callbacks.currentGeofenceEventDetails("start_exam", geofenceStatus)
                },
                fakeLocationDetails = { fakeLocationStatus ->
                    callbacks.currentFakeLocationEventDetails("start_exam", fakeLocationStatus)
                }
            )
            if (locationBlockMessage != null) {
                applyBlock(locationBlockMessage)
                return@launch
            }

            updatePreflight(StartExamPreflightStep.DeviceTime)
            val finalDeviceTimeStatus = callbacks.refreshDeviceTimeSecurity(
                "start_exam_final",
                false
            )
            val finalDeviceTimeBlock = resolveStartExamDeviceTimeBlockMessage(
                uiLanguage = uiLanguage,
                trigger = "start_exam_final",
                status = finalDeviceTimeStatus
            )
            if (finalDeviceTimeBlock != null) {
                applyBlock(finalDeviceTimeBlock)
                return@launch
            }
            val networkNowMillis = TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context)
            val scheduleValidationResult = ExamScheduleValidator.validateAfterDeviceTimeCheck(
                payload = payload,
                deviceTimeStatus = finalDeviceTimeStatus,
                networkNowMillis = networkNowMillis
            )
            val scheduleBlock = resolveStartExamScheduleBlockMessage(
                uiLanguage = uiLanguage,
                payload = payload,
                validationResult = scheduleValidationResult,
                networkNowMillis = networkNowMillis,
                deviceTimeStatus = finalDeviceTimeStatus
            )
            if (scheduleBlock != null) {
                applyBlock(scheduleBlock)
                return@launch
            }
            callbacks.debugLogExamStart(
                "startExamSession passed all prechecks in ${SystemClock.elapsedRealtime() - startExamPressedAt} ms"
            )
            updatePreflight(StartExamPreflightStep.PreparingWebView)
            callbacks.completeStartExamSessionAfterPrechecks()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            callbacks.setGeofenceStartValidationInFlight(false)
            applyBlock(
                resolveStartExamUnexpectedFailureBlockMessage(
                    uiLanguage = uiLanguage,
                    phase = "location_validation",
                    throwable = throwable
                )
            )
        }
    }
}
