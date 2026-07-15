package com.coblax.examlock.ui.exam

import com.coblax.examlock.AdbBypassState
import com.coblax.examlock.AppSwitchStatus
import com.coblax.examlock.ClipboardBypassState
import com.coblax.examlock.ClipboardRuntimeStatus
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.DeviceTimeBypassState
import com.coblax.examlock.DeviceTimeSecurityStatus
import com.coblax.examlock.DpcRuntimeStatus
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.FakeLocationBypassState
import com.coblax.examlock.FakeLocationRuntimeStatus
import com.coblax.examlock.GeofenceBypassState
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.OverlayRiskResult
import com.coblax.examlock.PreviousExamSessionBreadcrumb
import com.coblax.examlock.RootBypassState
import com.coblax.examlock.RootSecurityStatus
import com.coblax.examlock.VpnBypassState
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.model.NetworkTimelineEntry
import com.coblax.examlock.model.NetworkUnstableRuntimeStatus
import com.coblax.examlock.runtime.ExternalDisplayInfo
import com.coblax.examlock.runtime.MultiWindowModeInfo
import com.coblax.examlock.ui.preparation.PreExamHealthSnapshot
import com.coblax.examlock.ui.preparation.PreparationBypassState
import com.coblax.examlock.ui.preparation.PreparationDeviceState
import com.coblax.examlock.ui.preparation.PreparationDiagnosticsState
import com.coblax.examlock.ui.preparation.PreparationLocationState
import com.coblax.examlock.ui.preparation.PreparationNetworkState
import com.coblax.examlock.ui.preparation.PreparationRuntimeSecurityState
import com.coblax.examlock.ui.preparation.PreparationScreenState
import com.coblax.examlock.ui.preparation.PreparationSessionState

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
    bypassReverseEngineering: Boolean,
    bypassApkIntegrity: Boolean,
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
    previousExamSessionBreadcrumb: PreviousExamSessionBreadcrumb,
    dpcRuntimeStatus: DpcRuntimeStatus
): PreparationScreenState {
    return PreparationScreenState(
        session = PreparationSessionState(
            examName = payload.examName,
            sendingSection = sendingSection,
            isStartingExam = flowUiState.lockTaskRequestPending.value ||
                flowUiState.geofenceStartValidationInFlight.value ||
                flowUiState.startExamPreflight.visible.value,
            pinningActivationState = flowUiState.pinningActivationState.value,
            screenPinningMessage = flowUiState.screenPinningMessage.value,
            webViewSessionResetInFlight = flowUiState.webViewSessionResetInFlight.value,
            webViewSessionResetError = flowUiState.webViewSessionResetError.value,
            showChecklistDetails = adminSettings.showChecklistDetails
        ),
        network = PreparationNetworkState(
            networkReadinessStatus = networkReadinessStatus,
            networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
            networkTimelinePreview = networkTimelinePreview,
            lastNetworkChangeAt = networkUiState.lastNetworkChangeAt.value,
            lastNetworkChangeSource = networkUiState.lastNetworkChangeSource.value,
            lastConnectedNetworkLabel = networkUiState.lastConnectedNetworkLabel.value,
            isRefreshingNetwork = networkUiState.networkManualRefreshInFlight.value,
            bypassVpn = bypassVpn,
            vpnBypassState = vpnBypassState
        ),
        device = PreparationDeviceState(
            keyboardPackage = flowUiState.currentKeyboardPackage.value,
            keyboardAllowed = keyboardAllowed,
            usingBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value || !keyboardAllowed,
            bluetoothPermissionGranted = securityUiState.bluetoothPermissionGranted.value,
            bluetoothEnabled = securityUiState.bluetoothEnabled.value,
            adbInspection = securityUiState.adbInspection.value,
            adbBypassState = adbBypassState,
            rootSecurityStatus = securityUiState.rootSecurityStatus.value,
            rootBypassState = rootBypassState,
            signatureMismatchDetected = securityUiState.signatureMismatchDetected.value,
            virtualEnvironmentDetected = securityUiState.virtualEnvironmentDetected.value,
            screenPinningAvailable = screenPinningAvailable,
            isScreenPinningActive = screenPinningActive,
            screenPinningFixNeeded = screenPinningFixNeeded,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            deviceTimeSecurityStatus = deviceTimeSecurityStatus,
            deviceTimeBypassState = deviceTimeBypassState,
            reinstallApkFixNeeded = reinstallApkFixNeeded
        ),
        location = PreparationLocationState(
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
            isRefreshingGeofence = flowUiState.geofenceManualRefreshInFlight.value,
            isWarmingLocation = locationWarmupUiState.locationWarmupInFlight.value,
            lastGeofenceRefreshAt = flowUiState.lastGeofenceRefreshAt.value,
            geofenceBypassState = geofenceBypassState,
            fakeLocationBypassState = fakeLocationBypassState
        ),
        runtimeSecurity = PreparationRuntimeSecurityState(
            accessibilityServiceEnabled = securityUiState.accessibilityServiceEnabled.value,
            overlayRiskResult = overlayRiskResult,
            appSwitchStatus = appSwitchStatus,
            clipboardViolationCount = clipboardUiState.clipboardViolationCount.intValue,
            clipboardRuntimeStatus = clipboardRuntimeStatus,
            clipboardBypassState = clipboardBypassState,
            screenRecorderPackages = securityUiState.screenRecorderPackages.value,
            externalDisplayDetected = securityUiState.externalDisplayDetected.value,
            externalDisplayCount = securityUiState.externalDisplayCount.intValue,
            externalDisplayInfoList = externalDisplayInfoList,
            multiWindowDetected = securityUiState.multiWindowDetected.value,
            multiWindowModeInfo = multiWindowModeInfo,
            staticSecurityInitialScanComplete = securityUiState.staticSecurityInitialScanComplete.value,
            tamperDetected = securityUiState.tamperDetected.value || securityUiState.integrityTamperDetected.value,
            reverseEngineeringDetected = securityUiState.tamperDetected.value,
            reverseEngineeringSummary = securityUiState.tamperSummary.value,
            reverseEngineeringBypassActive = bypassReverseEngineering,
            integrityDetected = securityUiState.integrityTamperDetected.value,
            integritySummary = securityUiState.integritySummary.value,
            integrityBypassActive = bypassApkIntegrity,
            dpcRuntimeStatus = dpcRuntimeStatus,
            overlayAppsDetected = securityUiState.overlayAppsDetected.value,
            overlayGuardPermissionGranted = securityUiState.overlayGuardPermissionGranted.value
        ),
        bypass = PreparationBypassState(
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
            bypassScreenRecorder = bypassScreenRecorder,
            bypassDisplayMirror = bypassDisplayMirror,
            bypassMultiWindow = bypassMultiWindow,
            bypassReverseEngineering = bypassReverseEngineering,
            bypassApkIntegrity = bypassApkIntegrity
        ),
        diagnostics = PreparationDiagnosticsState(
            preExamHealthCheckSnapshot = preExamHealthCheckSnapshot,
            deviceSurvivalPolicy = deviceSurvivalPolicy,
            previousExamSessionBreadcrumb = previousExamSessionBreadcrumb
        )
    )
}
