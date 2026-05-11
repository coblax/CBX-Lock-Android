package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumb
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.runtime.ExternalDisplayInfo
import com.example.coblaxexamlock.runtime.MultiWindowModeInfo
import com.example.coblaxexamlock.ui.preparation.PreExamHealthSnapshot
import com.example.coblaxexamlock.ui.preparation.PreparationScreenState

internal fun buildPreparationStateForSession(
    payload: ExamQrPayload,
    adminSettings: AdminSettings,
    flowUiState: ExamRuntimeFlowUiState,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    networkUiState: ExamRuntimeNetworkUiState,
    locationWarmupUiState: ExamRuntimeLocationWarmupUiState,
    keyboardAllowed: Boolean,
    sendingSection: DiagnosticSection?,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    networkTimelinePreview: List<NetworkTimelineEntry>,
    screenPinningAvailable: Boolean,
    screenPinningActive: Boolean,
    screenPinningFixNeeded: Boolean,
    clipboardRuntimeStatus: ClipboardRuntimeStatus,
    clipboardBypassState: ClipboardBypassState,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    deviceTimeBypassState: DeviceTimeBypassState,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    overlayRiskResult: OverlayRiskResult,
    appSwitchStatus: AppSwitchStatus,
    reinstallApkFixNeeded: Boolean,
    bypassScreenPinning: Boolean,
    bypassBluetooth: Boolean,
    bypassAccessibility: Boolean,
    bypassAdb: Boolean,
    adbBypassState: AdbBypassState,
    bypassRoot: Boolean,
    rootBypassState: RootBypassState,
    bypassVirtualEnvironment: Boolean,
    bypassVpn: Boolean,
    vpnBypassState: VpnBypassState,
    bypassKeyboardPolicy: Boolean,
    bypassClipboard: Boolean,
    bypassOverlay: Boolean,
    bypassGeofence: Boolean,
    geofenceBypassState: GeofenceBypassState,
    bypassFakeLocation: Boolean,
    fakeLocationBypassState: FakeLocationBypassState,
    bypassDeviceTime: Boolean,
    bypassAppSwitch: Boolean,
    bypassScreenRecorder: Boolean,
    bypassDisplayMirror: Boolean,
    externalDisplayInfoList: List<ExternalDisplayInfo>,
    bypassMultiWindow: Boolean,
    multiWindowModeInfo: MultiWindowModeInfo,
    preExamHealthCheckSnapshot: PreExamHealthSnapshot,
    deviceSurvivalPolicy: DeviceSurvivalPolicy,
    previousExamSessionBreadcrumb: PreviousExamSessionBreadcrumb
): PreparationScreenState {
    return PreparationScreenState(
        examName = payload.examName,
        keyboardPackage = flowUiState.currentKeyboardPackage.value,
        keyboardAllowed = keyboardAllowed,
        usingBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value || !keyboardAllowed,
        bluetoothPermissionGranted = securityUiState.bluetoothPermissionGranted.value,
        bluetoothEnabled = securityUiState.bluetoothEnabled.value,
        accessibilityServiceEnabled = securityUiState.accessibilityServiceEnabled.value,
        adbInspection = securityUiState.adbInspection.value,
        adbBypassState = adbBypassState,
        rootSecurityStatus = securityUiState.rootSecurityStatus.value,
        rootBypassState = rootBypassState,
        signatureMismatchDetected = securityUiState.signatureMismatchDetected.value,
        virtualEnvironmentDetected = securityUiState.virtualEnvironmentDetected.value,
        tamperDetected = securityUiState.tamperDetected.value || securityUiState.integrityTamperDetected.value,
        sendingSection = sendingSection,
        isStartingExam = flowUiState.lockTaskRequestPending.value ||
            flowUiState.geofenceStartValidationInFlight.value,
        pinningActivationState = flowUiState.pinningActivationState.value,
        screenPinningMessage = flowUiState.screenPinningMessage.value,
        webViewSessionResetInFlight = flowUiState.webViewSessionResetInFlight.value,
        webViewSessionResetError = flowUiState.webViewSessionResetError.value,
        isRefreshingGeofence = flowUiState.geofenceManualRefreshInFlight.value,
        isWarmingLocation = locationWarmupUiState.locationWarmupInFlight.value,
        isRefreshingNetwork = networkUiState.networkManualRefreshInFlight.value,
        lastGeofenceRefreshAt = flowUiState.lastGeofenceRefreshAt.value,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        networkTimelinePreview = networkTimelinePreview,
        lastNetworkChangeAt = networkUiState.lastNetworkChangeAt.value,
        lastNetworkChangeSource = networkUiState.lastNetworkChangeSource.value,
        lastConnectedNetworkLabel = networkUiState.lastConnectedNetworkLabel.value,
        screenPinningAvailable = screenPinningAvailable,
        isScreenPinningActive = screenPinningActive,
        screenPinningFixNeeded = screenPinningFixNeeded,
        clipboardViolationCount = clipboardUiState.clipboardViolationCount.intValue,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        clipboardBypassState = clipboardBypassState,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        deviceTimeBypassState = deviceTimeBypassState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        overlayRiskResult = overlayRiskResult,
        appSwitchStatus = appSwitchStatus,
        reinstallApkFixNeeded = reinstallApkFixNeeded,
        bypassScreenPinning = bypassScreenPinning,
        bypassBluetooth = bypassBluetooth,
        bypassAccessibility = bypassAccessibility,
        bypassAdb = bypassAdb,
        bypassRoot = bypassRoot,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        bypassVpn = bypassVpn,
        vpnBypassState = vpnBypassState,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        bypassClipboard = bypassClipboard,
        bypassOverlay = bypassOverlay,
        bypassGeofence = bypassGeofence,
        geofenceBypassState = geofenceBypassState,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationBypassState = fakeLocationBypassState,
        bypassDeviceTime = bypassDeviceTime,
        bypassAppSwitch = bypassAppSwitch,
        screenRecorderPackages = securityUiState.screenRecorderPackages.value,
        bypassScreenRecorder = bypassScreenRecorder,
        externalDisplayDetected = securityUiState.externalDisplayDetected.value,
        externalDisplayCount = securityUiState.externalDisplayCount.intValue,
        externalDisplayInfoList = externalDisplayInfoList,
        bypassDisplayMirror = bypassDisplayMirror,
        multiWindowDetected = securityUiState.multiWindowDetected.value,
        multiWindowModeInfo = multiWindowModeInfo,
        bypassMultiWindow = bypassMultiWindow,
        preExamHealthCheckSnapshot = preExamHealthCheckSnapshot,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        previousExamSessionBreadcrumb = previousExamSessionBreadcrumb,
        showChecklistDetails = adminSettings.showChecklistDetails
    )
}
