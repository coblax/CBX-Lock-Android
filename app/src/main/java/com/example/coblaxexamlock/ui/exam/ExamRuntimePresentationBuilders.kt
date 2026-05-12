package com.example.coblaxexamlock.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.config.OfflineTooLongWarningThresholdMillis
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.format.formatElapsedDuration
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbCodes
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsState
import com.example.coblaxexamlock.ui.preparation.PreparationDeviceActions
import com.example.coblaxexamlock.ui.preparation.PreparationLocationActions
import com.example.coblaxexamlock.ui.preparation.PreparationNetworkActions
import com.example.coblaxexamlock.ui.preparation.PreparationRuntimeSecurityActions
import com.example.coblaxexamlock.ui.preparation.PreparationScreenActions
import com.example.coblaxexamlock.ui.preparation.PreparationSessionActions
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.WebViewHealthSeverity

internal fun resolveExamFooterShieldStatus(
    examGuardArmed: Boolean,
    bypassKeyboardPolicy: Boolean,
    isKeyboardAllowed: Boolean,
    useBuiltInExamKeyboard: Boolean,
    bypassBluetooth: Boolean,
    bluetoothEnabled: Boolean,
    bluetoothPermissionGranted: Boolean,
    bypassAccessibility: Boolean,
    accessibilityServiceEnabled: Boolean,
    bypassAdb: Boolean,
    adbInspection: AdbInspection,
    bypassRoot: Boolean,
    rootSecurityStatus: RootSecurityStatus,
    bypassVirtualEnvironment: Boolean,
    virtualEnvironmentDetected: Boolean,
    bypassVpn: Boolean,
    networkReadinessStatus: NetworkReadinessStatus,
    bypassGeofence: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    bypassFakeLocation: Boolean,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    bypassDeviceTime: Boolean,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    bypassOverlay: Boolean,
    overlayRiskResult: OverlayRiskResult,
    bypassAppSwitch: Boolean,
    appSwitchStatus: AppSwitchStatus,
    bypassClipboard: Boolean,
    signatureMismatchDetected: Boolean,
    securityTamperDetected: Boolean,
    forcedExitViolationCount: Int,
    keyboardViolationCount: Int,
    overlayViolationCount: Int,
    geofenceViolationCount: Int,
    fakeLocationViolationCount: Int,
    bluetoothViolationCount: Int,
    clipboardViolationCount: Int,
    showForcedExitAlarm: Boolean,
    showKeyboardViolationDialog: Boolean,
    showOverlayViolationDialog: Boolean,
    showGeofenceViolationDialog: Boolean,
    showFakeLocationViolationDialog: Boolean,
    showBluetoothViolationDialog: Boolean,
    showClipboardViolationDialog: Boolean
): ExamFooterShieldStatus {
    val activeViolationDialog =
        showForcedExitAlarm ||
            showKeyboardViolationDialog ||
            showOverlayViolationDialog ||
            showGeofenceViolationDialog ||
            showFakeLocationViolationDialog ||
            showBluetoothViolationDialog ||
            showClipboardViolationDialog
    val keyboardBlocked = !bypassKeyboardPolicy && !isKeyboardAllowed && !useBuiltInExamKeyboard
    val bluetoothBlocked =
        !bypassBluetooth &&
            (bluetoothEnabled || (requiresBluetoothExamPermission() && !bluetoothPermissionGranted))
    val blockingSecurityIssue =
        keyboardBlocked ||
            bluetoothBlocked ||
            (!bypassAccessibility && accessibilityServiceEnabled) ||
            (!bypassAdb && adbInspection.blocking) ||
            (!bypassRoot && rootSecurityStatus.blocking) ||
            (!bypassVirtualEnvironment && virtualEnvironmentDetected) ||
            (!bypassVpn && networkReadinessStatus.diagnostics.isVpnActive) ||
            (!bypassGeofence && geofenceRuntimeStatus.securityStatus.blocking) ||
            (!bypassFakeLocation && fakeLocationRuntimeStatus.securityStatus.blocking) ||
            (!bypassDeviceTime && deviceTimeSecurityStatus.blocking) ||
            (!bypassOverlay && overlayRiskResult.hasAnyRisk) ||
            (!bypassAppSwitch && appSwitchStatus.pendingViolation) ||
            signatureMismatchDetected

    if (securityTamperDetected || activeViolationDialog || blockingSecurityIssue) {
        return ExamFooterShieldStatus.Danger
    }

    val bypassActive =
        bypassKeyboardPolicy ||
            bypassBluetooth ||
            bypassAccessibility ||
            bypassAdb ||
            bypassRoot ||
            bypassVirtualEnvironment ||
            bypassVpn ||
            bypassGeofence ||
            bypassFakeLocation ||
            bypassDeviceTime ||
            bypassOverlay ||
            bypassAppSwitch ||
            bypassClipboard
    val historicalViolation =
        forcedExitViolationCount > 0 ||
            keyboardViolationCount > 0 ||
            overlayViolationCount > 0 ||
            geofenceViolationCount > 0 ||
            fakeLocationViolationCount > 0 ||
            bluetoothViolationCount > 0 ||
            clipboardViolationCount > 0 ||
            appSwitchStatus.hasViolations
    val nonBlockingSecurityWarning =
        bypassActive ||
            historicalViolation ||
            (!bypassFakeLocation && fakeLocationRuntimeStatus.securityStatus.warningOnly) ||
            (!bypassRoot && rootSecurityStatus.selinuxPermissive) ||
            !examGuardArmed

    return if (nonBlockingSecurityWarning) {
        ExamFooterShieldStatus.Warning
    } else {
        ExamFooterShieldStatus.Safe
    }
}

internal fun buildExamRuntimeChromeState(
    examSessionStarted: Boolean,
    examDisplayName: String,
    loadingProgress: Float,
    webViewErrorMessage: String?,
    hasFullscreenCustomView: Boolean,
    useBuiltInExamKeyboard: Boolean,
    showBuiltInExamKeyboard: Boolean,
    showSideArrowControls: Boolean,
    hasEditableFocus: Boolean,
    builtInKeyboardShiftEnabled: Boolean,
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus,
    batteryStatus: com.example.coblaxexamlock.model.ExamBatteryStatus,
    shieldStatus: ExamFooterShieldStatus
): ExamRuntimeChromeState {
    return ExamRuntimeChromeState(
        examSessionStarted = examSessionStarted,
        examDisplayName = examDisplayName,
        loadingProgress = loadingProgress,
        webViewErrorMessage = webViewErrorMessage,
        hasFullscreenCustomView = hasFullscreenCustomView,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        showBuiltInExamKeyboard = showBuiltInExamKeyboard,
        showSideArrowControls = showSideArrowControls,
        hasEditableFocus = hasEditableFocus,
        builtInKeyboardShiftEnabled = builtInKeyboardShiftEnabled,
        networkStatus = networkStatus,
        serverStatus = serverStatus,
        batteryStatus = batteryStatus,
        shieldStatus = shieldStatus
    )
}

internal fun buildExamRuntimeChromeActions(
    onRetryLoading: () -> Unit,
    onRefreshPage: () -> Unit,
    onGoHome: () -> Unit,
    onTextKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onArrowLeft: () -> Unit,
    onArrowRight: () -> Unit,
    onToggleSideArrowControls: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftToggle: () -> Unit
): ExamRuntimeChromeActions {
    return ExamRuntimeChromeActions(
        onRetryLoading = onRetryLoading,
        onRefreshPage = onRefreshPage,
        onGoHome = onGoHome,
        onTextKey = onTextKey,
        onBackspace = onBackspace,
        onArrowLeft = onArrowLeft,
        onArrowRight = onArrowRight,
        onToggleSideArrowControls = onToggleSideArrowControls,
        onEnter = onEnter,
        onSpace = onSpace,
        onShiftToggle = onShiftToggle
    )
}

internal fun buildExamRuntimeDialogsState(
    showForcedExitAlarm: Boolean,
    forcedExitViolationCount: Int,
    appSwitchStatus: AppSwitchStatus,
    showKeyboardViolationDialog: Boolean,
    keyboardViolationCount: Int,
    currentKeyboardLabel: String,
    showOverlayViolationDialog: Boolean,
    overlayViolationCount: Int,
    overlayTrigger: String?,
    showOfflineWarningDialog: Boolean,
    offlineDurationMs: Long?,
    currentOfflineDurationMs: Long?,
    uiLanguage: UiLanguage,
    showVpnDetectedDialog: Boolean,
    vpnBypassActive: Boolean,
    vpnBypassTampered: Boolean,
    showNetworkUnstableDialog: Boolean,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    showGeofenceViolationDialog: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    showFakeLocationViolationDialog: Boolean,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    showBluetoothViolationDialog: Boolean,
    bluetoothEnabled: Boolean,
    bluetoothViolationCount: Int,
    showClipboardViolationDialog: Boolean,
    clipboardViolationCount: Int,
    clipboardLastConfirmedAt: String?,
    clipboardLastDecision: String,
    showExitExamDialog: Boolean,
    exitSessionClearInFlight: Boolean
): ExamRuntimeDialogsState {
    return ExamRuntimeDialogsState(
        showForcedExitAlarm = showForcedExitAlarm,
        forcedExitViolationCount = forcedExitViolationCount,
        appSwitchStatus = appSwitchStatus,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        keyboardViolationCount = keyboardViolationCount,
        currentKeyboardLabel = currentKeyboardLabel,
        showOverlayViolationDialog = showOverlayViolationDialog,
        overlayViolationCount = overlayViolationCount,
        overlayTrigger = overlayTrigger,
        showOfflineWarningDialog = showOfflineWarningDialog,
        offlineDurationText = formatElapsedDuration(
            offlineDurationMs ?: currentOfflineDurationMs ?: OfflineTooLongWarningThresholdMillis,
            uiLanguage
        ),
        showVpnDetectedDialog = showVpnDetectedDialog,
        vpnTransportLabel = networkReadinessStatus.transportLabel,
        vpnInterfaceName = networkReadinessStatus.diagnostics.interfaceName,
        vpnBypassActive = vpnBypassActive,
        vpnBypassTampered = vpnBypassTampered,
        showNetworkUnstableDialog = showNetworkUnstableDialog,
        networkTransportLabel = networkReadinessStatus.transportLabel,
        networkUnstableFlapCount = networkUnstableRuntimeStatus.flapCount,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        geofenceStatus = geofenceRuntimeStatus.securityStatus,
        geofenceViolationCount = geofenceRuntimeStatus.violationCount,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        fakeLocationStatus = fakeLocationRuntimeStatus.securityStatus,
        fakeLocationViolationCount = fakeLocationRuntimeStatus.violationCount,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothViolationCount = bluetoothViolationCount,
        showClipboardViolationDialog = showClipboardViolationDialog,
        clipboardViolationCount = clipboardViolationCount,
        clipboardLastConfirmedAt = clipboardLastConfirmedAt,
        clipboardLastDecision = clipboardLastDecision,
        showExitExamDialog = showExitExamDialog,
        exitSessionClearInFlight = exitSessionClearInFlight
    )
}

@Composable
internal fun ExamRuntimeResolvedDiagnosticsEffects(
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    deviceSurvivalPolicy: DeviceSurvivalPolicy,
    examName: String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    writePreviousSessionBreadcrumb: (String, String) -> Unit
) {
    LaunchedEffect(webViewCompatibilityStatus.diagnosticSummary()) {
        recordAction(
            ExamRuntimeHardeningDiagnostics.WebViewProviderHealthResolved,
            webViewCompatibilityStatus.diagnosticSummary(),
            DiagnosticEventLevel.INFO
        )
        if (webViewCompatibilityStatus.severity != WebViewHealthSeverity.Stable) {
            recordAction(
                ExamRuntimeHardeningDiagnostics.WebViewProviderHealthWarning,
                webViewCompatibilityStatus.adminDetail,
                DiagnosticEventLevel.WARNING
            )
        }
    }
    LaunchedEffect(deviceSurvivalPolicy.diagnosticSummary()) {
        recordAction(
            ExamRuntimeHardeningDiagnostics.DeviceSurvivalPolicyResolved,
            deviceSurvivalPolicy.diagnosticSummary(),
            DiagnosticEventLevel.INFO
        )
        recordAction(
            ExamRuntimeHardeningDiagnostics.CompatibilityScoreUpdated,
            "score=${deviceSurvivalPolicy.score.name} | runtime=${deviceSurvivalPolicy.runtimeTier.name}",
            DiagnosticEventLevel.INFO
        )
    }
    LaunchedEffect(examName) {
        writePreviousSessionBreadcrumb(
            PreviousExamSessionBreadcrumbCodes.PreparationOpened,
            "exam=${examName.take(80)} | score=${deviceSurvivalPolicy.score.name}"
        )
    }
}

internal fun buildPreparationScreenActions(
    onChooseKeyboard: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onGrantBluetoothPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlayAccessibilitySettings: () -> Unit,
    onOpenDeveloperOptionsSettings: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationServicesSettings: () -> Unit,
    onRefreshGeofenceLocation: () -> Unit,
    onOpenGeofenceMapViewer: () -> Unit,
    onOpenInternetSettings: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenCellularSettings: () -> Unit,
    onOpenAirplaneModeSettings: () -> Unit,
    onRefreshNetworkStatus: () -> Unit,
    onOpenDateTimeSettings: () -> Unit,
    onOpenFakeLocationDeveloperOptionsSettings: () -> Unit,
    onOpenScreenPinningSettings: () -> Unit,
    onStartScreenPinning: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenCastSettings: () -> Unit,
    onOpenWebViewProviderSettings: () -> Unit,
    onReinstallOfficialApk: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRefreshAllSecurityChecks: () -> Unit,
    onRefreshHealthCheck: () -> Unit,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    onExportDiagnostics: () -> Unit,
    onAutoFixShown: (String) -> Unit,
    onPreviousSessionRecoveryHintShown: (String) -> Unit,
    onAutoFixActionOpened: (String) -> Unit,
    onStartExam: () -> Unit,
    onBackHome: () -> Unit
): PreparationScreenActions {
    return PreparationScreenActions(
        session = PreparationSessionActions(
            onRefreshStatus = onRefreshStatus,
            onRefreshAllSecurityChecks = onRefreshAllSecurityChecks,
            onRefreshHealthCheck = onRefreshHealthCheck,
            onRequestSectionReport = onRequestSectionReport,
            onExportDiagnostics = onExportDiagnostics,
            onAutoFixShown = onAutoFixShown,
            onPreviousSessionRecoveryHintShown = onPreviousSessionRecoveryHintShown,
            onAutoFixActionOpened = onAutoFixActionOpened,
            onStartExam = onStartExam,
            onBackHome = onBackHome
        ),
        network = PreparationNetworkActions(
            onOpenInternetSettings = onOpenInternetSettings,
            onOpenVpnSettings = onOpenVpnSettings,
            onOpenWifiSettings = onOpenWifiSettings,
            onOpenCellularSettings = onOpenCellularSettings,
            onOpenAirplaneModeSettings = onOpenAirplaneModeSettings,
            onRefreshNetworkStatus = onRefreshNetworkStatus
        ),
        device = PreparationDeviceActions(
            onChooseKeyboard = onChooseKeyboard,
            onOpenKeyboardSettings = onOpenKeyboardSettings,
            onGrantBluetoothPermission = onGrantBluetoothPermission,
            onOpenBluetoothSettings = onOpenBluetoothSettings,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenOverlayAccessibilitySettings = onOpenOverlayAccessibilitySettings,
            onOpenDeveloperOptionsSettings = onOpenDeveloperOptionsSettings,
            onOpenDateTimeSettings = onOpenDateTimeSettings,
            onOpenScreenPinningSettings = onOpenScreenPinningSettings,
            onStartScreenPinning = onStartScreenPinning,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onOpenAppSettings = onOpenAppSettings,
            onOpenCastSettings = onOpenCastSettings,
            onOpenWebViewProviderSettings = onOpenWebViewProviderSettings,
            onReinstallOfficialApk = onReinstallOfficialApk
        ),
        location = PreparationLocationActions(
            onRequestLocationPermission = onRequestLocationPermission,
            onOpenLocationServicesSettings = onOpenLocationServicesSettings,
            onRefreshGeofenceLocation = onRefreshGeofenceLocation,
            onOpenGeofenceMapViewer = onOpenGeofenceMapViewer,
            onOpenFakeLocationDeveloperOptionsSettings = onOpenFakeLocationDeveloperOptionsSettings
        ),
        runtimeSecurity = PreparationRuntimeSecurityActions(
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenOverlayAccessibilitySettings = onOpenOverlayAccessibilitySettings,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onOpenAppSettings = onOpenAppSettings,
            onOpenCastSettings = onOpenCastSettings
        )
    )
}
