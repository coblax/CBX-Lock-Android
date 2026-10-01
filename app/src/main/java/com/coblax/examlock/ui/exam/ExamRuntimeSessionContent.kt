package com.coblax.examlock.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coblax.examlock.AlarmAcknowledgePayload
import com.coblax.examlock.AlarmAcknowledgeType
import com.coblax.examlock.ExamParticipantCaptureBridge
import com.coblax.examlock.ExamParticipantCaptureResult
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.effectiveExamUserAgent
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.openVpnSettings
import com.coblax.examlock.parseExamParticipantContext
import com.coblax.examlock.PreviousExamSessionBreadcrumbCodes
import com.coblax.examlock.PreviousExamSessionBreadcrumbStore
import com.coblax.examlock.VpnBypassState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Builds what the exam session shows (preparation, runtime chrome, dialogs) and renders
 * it. Split out of the session composable for the same reason as
 * [ExamRuntimeSessionEffects].
 */
@Composable
internal fun ExamRuntimeSessionContent(
    core: ExamRuntimeSessionCore,
    runtimeMonitoringOps: ExamRuntimeMonitoringOps,
    runtimeSecurityOps: ExamRuntimeSecurityOps,
    startExamController: ExamRuntimeStartExamController,
    permissionLaunchers: ExamRuntimePermissionLaunchers,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    modifier: Modifier
) {
    var webViewCompatibilityRefreshKey by core.webViewCompatibilityRefreshKeyState
    val screenPinningMode = core.bypass.screenPinningMode
    val clipboardBypassState = core.bypass.clipboardBypassState
    val adbBypassState = core.bypass.adbBypassState
    val rootBypassState = core.bypass.rootBypassState
    val geofenceBypassState = core.bypass.geofenceBypassState
    val fakeLocationBypassState = core.bypass.fakeLocationBypassState
    val deviceTimeBypassState = core.bypass.deviceTimeBypassState
    val vpnBypassState = core.bypass.vpnBypassState
    val bypassScreenPinning = core.bypass.bypassScreenPinning
    val bypassBluetooth = core.bypass.bypassBluetooth
    val bypassAccessibility = core.bypass.bypassAccessibility
    val bypassAdb = core.bypass.bypassAdb
    val bypassRoot = core.bypass.bypassRoot
    val bypassReverseEngineering = core.bypass.bypassReverseEngineering
    val bypassApkIntegrity = core.bypass.bypassApkIntegrity
    val bypassVirtualEnvironment = core.bypass.bypassVirtualEnvironment
    val bypassKeyboardPolicy = core.bypass.bypassKeyboardPolicy
    val bypassClipboard = core.bypass.bypassClipboard
    val bypassOverlay = core.bypass.bypassOverlay
    val bypassGeofence = core.bypass.bypassGeofence
    val bypassFakeLocation = core.bypass.bypassFakeLocation
    val bypassDeviceTime = core.bypass.bypassDeviceTime
    val bypassVpn = core.bypass.bypassVpn
    val bypassAppSwitch = core.bypass.bypassAppSwitch
    val bypassScreenRecorder = core.bypass.bypassScreenRecorder
    val bypassDisplayMirror = core.bypass.bypassDisplayMirror
    val bypassMultiWindow = core.bypass.bypassMultiWindow
    val adminOverridesSummary = core.bypass.adminOverridesSummary
    val officialApkUrl = core.bypass.officialApkUrl
    var loadingProgress by core.webViewUiState.loadingProgress
    var webViewStopRequested by core.webViewUiState.stopRequested
    var webViewInstance by core.webViewUiState.instance
    var examSessionStarted by core.flowUiState.examSessionStarted
    var lockTaskRequestPending by core.flowUiState.lockTaskRequestPending
    var screenPinningMessage by core.flowUiState.screenPinningMessage
    var showExitExamDialog by core.flowUiState.showExitExamDialog
    var webViewErrorMessage by core.flowUiState.webViewErrorMessage
    var examServerStatus by core.examServerStatusState
    var showNetworkUnstableDialog by core.networkUiState.showNetworkUnstableDialog
    var offlineWarningDurationMs by core.networkUiState.offlineWarningDurationMs
    var showOfflineWarningDialog by core.networkUiState.showOfflineWarningDialog
    var batteryStatus by core.batteryStatusState
    var dpcRuntimeStatus by core.dpcRuntimeStatusState
    var fullScreenCustomView by core.webViewUiState.fullScreenCustomView
    var useBuiltInExamKeyboard by core.flowUiState.useBuiltInExamKeyboard
    var exitSessionClearInFlight by core.flowUiState.exitSessionClearInFlight
    var examRuntimeRecoveryState by core.webViewUiState.recoveryState
    var showBuiltInExamKeyboard by core.flowUiState.showBuiltInExamKeyboard
    var sideArrowControlsVisible by core.flowUiState.sideArrowControlsVisible
    var hasEditableFocus by core.flowUiState.hasEditableFocus
    var builtInKeyboardShiftEnabled by core.flowUiState.builtInKeyboardShiftEnabled
    var geofenceManualRefreshInFlight by core.flowUiState.geofenceManualRefreshInFlight
    var geofenceViolationCount by core.flowUiState.geofenceViolationCount
    var showGeofenceViolationDialog by core.flowUiState.showGeofenceViolationDialog
    var showGeofenceMapViewer by core.flowUiState.showGeofenceMapViewer
    var fakeLocationViolationCount by core.flowUiState.fakeLocationViolationCount
    var showFakeLocationViolationDialog by core.flowUiState.showFakeLocationViolationDialog
    var currentKeyboardLabel by core.flowUiState.currentKeyboardLabel
    var forcedExitViolationCount by core.securityUiState.forcedExitViolationCount
    var showForcedExitAlarm by core.securityUiState.showForcedExitAlarm
    var keyboardViolationCount by core.securityUiState.keyboardViolationCount
    var showKeyboardViolationDialog by core.securityUiState.showKeyboardViolationDialog
    var overlayViolationCount by core.securityUiState.overlayViolationCount
    var showOverlayViolationDialog by core.securityUiState.showOverlayViolationDialog
    var lastExamRefreshDecision by core.securityUiState.lastExamRefreshDecision
    var bluetoothPermissionGranted by core.securityUiState.bluetoothPermissionGranted
    var bluetoothEnabled by core.securityUiState.bluetoothEnabled
    var accessibilityServiceEnabled by core.securityUiState.accessibilityServiceEnabled
    var adbInspection by core.securityUiState.adbInspection
    var rootSecurityStatus by core.securityUiState.rootSecurityStatus
    var signatureMismatchDetected by core.securityUiState.signatureMismatchDetected
    var virtualEnvironmentDetected by core.securityUiState.virtualEnvironmentDetected
    var bluetoothViolationCount by core.securityUiState.bluetoothViolationCount
    var showBluetoothViolationDialog by core.securityUiState.showBluetoothViolationDialog
    var deviceTimeSecurityStatus by core.deviceTimeSecurityStatusState
    var clipboardViolationCount by core.clipboardUiState.clipboardViolationCount
    var lastClipboardConfirmedAt by core.clipboardUiState.lastClipboardConfirmedAt
    var lastClipboardDecision by core.clipboardUiState.lastClipboardDecision
    var showClipboardViolationDialog by core.clipboardUiState.showClipboardViolationDialog
    var securityIssueDialogTitle by core.adminUiState.securityIssueDialogTitle
    var securityIssueDialogMessage by core.adminUiState.securityIssueDialogMessage
    var securityIssueDialogCode by core.adminUiState.securityIssueDialogCode
    var accessibilityGuardEnabled by core.accessibilityGuardEnabledState
    var screenPinningAvailable by core.adminUiState.screenPinningAvailable
    var screenPinningEnabledInSystem by core.adminUiState.screenPinningEnabledInSystem
    var sendingSection by core.adminUiState.sendingSection
    var pendingSection by core.adminUiState.pendingSection
    var bugReportFeedbackTitle by core.adminUiState.bugReportFeedbackTitle
    var bugReportFeedbackMessage by core.adminUiState.bugReportFeedbackMessage
    var lastParticipantCaptureLogKey by core.adminUiState.lastParticipantCaptureLogKey
    var participantContext by core.adminUiState.participantContext
    val currentOfflineDurationMs = core.runtimeDiagnosticsOps.currentOfflineDurationMs
    val networkTimelinePreview = core.runtimeDiagnosticsOps.networkTimelinePreview
    val networkUnstableRuntimeStatus = core.runtimeDiagnosticsOps.networkUnstableRuntimeStatus
    val geofenceRuntimeStatus = core.runtimeDiagnosticsOps.geofenceRuntimeStatus
    val fakeLocationRuntimeStatus = core.runtimeDiagnosticsOps.fakeLocationRuntimeStatus
    val clipboardRuntimeStatus = core.runtimeDiagnosticsOps.clipboardRuntimeStatus
    val appSwitchStatus = core.runtimeDiagnosticsOps.appSwitchStatus
    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = core.runtimeDiagnosticsOps.recordAction(code, details, level)
    fun writePreviousSessionBreadcrumb(
        code: String,
        details: String = "-"
    ) = core.runtimeDiagnosticsOps.writePreviousSessionBreadcrumb(code, details)
    fun markTrustedRuntimeChromeAction(reason: String) = core.markTrustedRuntimeChromeAction(reason)
    fun launchExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) = core.launchExamServerProbe(trigger, markChecking)
    fun currentNetworkEventDetails(
        trigger: String,
        status: NetworkReadinessStatus,
        extraContext: String? = null
    ): String = core.runtimeDiagnosticsOps.currentNetworkEventDetails(trigger, status, extraContext)
    fun launchNetworkManualRefresh(trigger: String) = core.runtimeDiagnosticsOps.launchNetworkManualRefresh(trigger)
    fun acknowledgeRuntimeAlarm(
        type: AlarmAcknowledgeType,
        violationCount: Int,
        buildPayload: (detailRef: String) -> AlarmAcknowledgePayload,
        onUiAcknowledge: () -> Unit
    ) = runtimeMonitoringOps.acknowledgeRuntimeAlarm(type, violationCount, buildPayload, onUiAcknowledge)
    fun hideSystemKeyboard() = runtimeMonitoringOps.hideSystemKeyboard()
    fun hideCustomView() = runtimeMonitoringOps.hideCustomView()
    suspend fun clearExamSessionOnExit(reason: String, waitForResult: Boolean): Result<Unit> =
        runtimeMonitoringOps.clearExamSessionOnExit(reason, waitForResult)
    fun refreshBluetoothSecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshBluetoothSecurity(triggerViolation)
    fun launchTelegramSectionReport(section: DiagnosticSection) = runtimeSecurityOps.launchTelegramSectionReport(section)

    val webViewUiState = core.webViewUiState
    val clipboardUiState = core.clipboardUiState
    val examAlarmController = core.examAlarmController
    val flowUiState = core.flowUiState
    val examExceptionHandler = core.examExceptionHandler
    val examGuardArmed = core.examGuardArmed
    val isKeyboardAllowed = core.isKeyboardAllowed
    val context = core.context
    val appVersionName = core.appVersionName
    val locationWarmupUiState = core.locationWarmupUiState
    val uiLanguage = core.uiLanguage
    val lastTrustedRuntimeChromeActionReasonState = core.lastTrustedRuntimeChromeActionReasonState
    val accessibilityGuardEnabledState = core.accessibilityGuardEnabledState
    val alarmSessionIdentity = core.alarmSessionIdentity
    val lockTaskBridge = core.lockTaskBridge
    val runtimeDiagnosticsOps = core.runtimeDiagnosticsOps
    val coroutineScope = core.coroutineScope
    val activity = core.activity
    val deviceCompatibilityProfile = core.deviceCompatibilityProfile
    val isIndonesian = core.isIndonesian
    val componentActivity = core.componentActivity
    val onExit = core.onExit
    val webViewCompatibilityStatus = core.webViewCompatibilityStatus
    val lastTrustedRuntimeChromeActionElapsedMsState = core.lastTrustedRuntimeChromeActionElapsedMsState
    val nativeExamFullscreenActive = core.nativeExamFullscreenActive
    val networkReadinessStatus = core.networkReadinessStatus
    val lowRamProfile = core.lowRamProfile
    val payload = core.payload
    val adminUiState = core.adminUiState
    val networkUiState = core.networkUiState
    val runtimeCacheState = core.runtimeCacheState
    val deviceQuirkProfile = core.deviceQuirkProfile
    val overlayRiskResult = core.overlayRiskResult
    val examServerStatusState = core.examServerStatusState
    val securityUiState = core.securityUiState
    val securityTamperDetected = core.securityTamperDetected
    val adminSettings = core.adminSettings
    val networkStatus = core.networkReadinessStatus.examStatus
    val examDisplayName = core.payload.examName.ifBlank { tr("Exam Session", "Sesi Ujian") }
    val effectiveExamUserAgent = core.adminSettings.effectiveExamUserAgent()
    val fullScreenContainer = core.webViewUiState.fullScreenContainer

    fun sendBuiltInKeyboardText(rawText: String) {
        sendBuiltInExamKeyboardText(
            webView = webViewInstance,
            rawText = rawText,
            shiftEnabled = builtInKeyboardShiftEnabled,
            updateShiftEnabled = { builtInKeyboardShiftEnabled = it },
            hideSystemKeyboard = { hideSystemKeyboard() }
        )
    }

    fun sendBuiltInKeyboardBackspace() {
        sendBuiltInExamKeyboardBackspace(webViewInstance, { hideSystemKeyboard() })
    }

    fun sendKeyboardArrowLeft() {
        sendExamKeyboardArrowLeft(webViewInstance)
    }

    fun sendKeyboardArrowRight() {
        sendExamKeyboardArrowRight(webViewInstance)
    }

    fun sendBuiltInKeyboardEnter() {
        sendBuiltInExamKeyboardEnter(webViewInstance, { hideSystemKeyboard() })
    }

    fun handleScreenPinningTransitionInterrupted() = core.handleScreenPinningTransitionInterrupted()

    val keyboardBridge: ExamKeyboardBridge = remember {
        ExamKeyboardBridge(
            onEditableFocusChangedCallback = { focused ->
                if (hasEditableFocus != focused) {
                    hasEditableFocus = focused
                }
                val shouldShowBuiltInKeyboard = useBuiltInExamKeyboard && focused
                if (showBuiltInExamKeyboard != shouldShowBuiltInKeyboard) {
                    showBuiltInExamKeyboard = shouldShowBuiltInKeyboard
                }
                if (shouldShowBuiltInKeyboard) {
                    hideSystemKeyboard()
                }
            }
        )
    }
    val participantCaptureBridge = ExamParticipantCaptureBridge { rawPayload, sourceKey ->
        val result = parseExamParticipantContext(rawPayload, sourceKey)
        when (result) {
            is ExamParticipantCaptureResult.Captured -> {
                val capturedContext = result.context
                if (participantContext != capturedContext) {
                    participantContext = capturedContext
                }
                val details = capturedContext.diagnosticSummary()
                if (lastParticipantCaptureLogKey != "captured|$details") {
                    lastParticipantCaptureLogKey = "captured|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_CAPTURED",
                        details = details
                    )
                }
            }

            is ExamParticipantCaptureResult.Ignored -> {
                val details = "source_key=${result.sourceKey} | reason=${result.reason}"
                if (lastParticipantCaptureLogKey != "ignored|$details") {
                    lastParticipantCaptureLogKey = "ignored|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_IGNORED",
                        details = details
                    )
                }
            }

            is ExamParticipantCaptureResult.Failed -> {
                val details = "source_key=${result.sourceKey} | reason=${result.reason}"
                if (lastParticipantCaptureLogKey != "failed|$details") {
                    lastParticipantCaptureLogKey = "failed|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_CAPTURE_FAILED",
                        details = details,
                        level = DiagnosticEventLevel.ERROR
                    )
                }
            }
        }
    }
    val preparationActionOps = ExamRuntimePreparationActionOps(
        context = context,
        activity = activity,
        uiLanguage = uiLanguage,
        isIndonesian = isIndonesian,
        adminSettings = adminSettings,
        officialApkUrl = officialApkUrl,
        lockTaskBridge = lockTaskBridge,
        screenPinningMode = screenPinningMode,
        vpnBypassState = vpnBypassState,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        adminUiState = adminUiState,
        networkUiState = networkUiState,
        webViewUiState = webViewUiState,
        accessibilityGuardEnabledState = accessibilityGuardEnabledState,
        coroutineScope = coroutineScope,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        runtimeSecurityOps = runtimeSecurityOps,
        runtimeMonitoringOps = runtimeMonitoringOps,
        examAlarmController = examAlarmController,
        launchBluetoothPermission = {
            permissionLaunchers.launchBluetooth()
        },
        launchLocationPermission = { permissions ->
            permissionLaunchers.launchLocation(permissions)
        },
        incrementWebViewCompatibilityRefreshKey = { webViewCompatibilityRefreshKey += 1 },
        debugLogExamStart = { message -> debugLogExamStart(message) }
    )

    fun handleChooseKeyboard() = preparationActionOps.handleChooseKeyboard()
    fun handleOpenKeyboardSettings() = preparationActionOps.handleOpenKeyboardSettings()
    fun handleGrantBluetoothPermission() = preparationActionOps.handleGrantBluetoothPermission()
    fun handleOpenBluetoothSettings() = preparationActionOps.handleOpenBluetoothSettings()
    fun handleOpenAccessibilitySettings() = preparationActionOps.handleOpenAccessibilitySettings()
    fun handleOpenOverlayAccessibilitySettings() = preparationActionOps.handleOpenOverlayAccessibilitySettings()
    fun handleOpenDeveloperOptionsSettings() = preparationActionOps.handleOpenDeveloperOptionsSettings()
    fun handleRequestLocationPermission() = preparationActionOps.handleRequestLocationPermission()
    fun handleOpenLocationServicesSettings() = preparationActionOps.handleOpenLocationServicesSettings()
    fun handleRefreshLocationSecurity() = preparationActionOps.handleRefreshLocationSecurity()
    fun handleOpenGeofenceMapViewer() = preparationActionOps.handleOpenGeofenceMapViewer()
    fun handleOpenInternetSettings() = preparationActionOps.handleOpenInternetSettings()
    fun handleOpenVpnSettings() = preparationActionOps.handleOpenVpnSettings()
    fun handleOpenDateTimeSettings() = preparationActionOps.handleOpenDateTimeSettings()
    fun handleOpenWifiSettings() = preparationActionOps.handleOpenWifiSettings()
    fun handleOpenCellularSettings() = preparationActionOps.handleOpenCellularSettings()
    fun handleOpenAirplaneModeSettings() = preparationActionOps.handleOpenAirplaneModeSettings()
    fun handleRefreshNetworkStatus() = preparationActionOps.handleRefreshNetworkStatus()
    fun handleOpenFakeLocationDeveloperOptionsSettings() =
        preparationActionOps.handleOpenFakeLocationDeveloperOptionsSettings()
    fun handleOpenScreenPinningSettings() = preparationActionOps.handleOpenScreenPinningSettings()
    fun handleStartScreenPinning() = preparationActionOps.handleStartScreenPinning()
    fun handleOpenOverlaySettings() = preparationActionOps.handleOpenOverlaySettings()
    fun handleOpenAppSettings() = preparationActionOps.handleOpenAppSettings()
    fun handleOpenAppPermissionSettings() = preparationActionOps.handleOpenAppPermissionSettings()
    fun handleOpenCastSettings() = preparationActionOps.handleOpenCastSettings()
    fun handleOpenWebViewProviderSettings() = preparationActionOps.handleOpenWebViewProviderSettings()
    fun handleReinstallOfficialApk() = preparationActionOps.handleReinstallOfficialApk()
    fun handleAcknowledgeOverlayViolation() = preparationActionOps.handleAcknowledgeOverlayViolation()
    fun handleReleaseScreenPinningThen(then: () -> Unit) =
        preparationActionOps.handleReleaseScreenPinningThen(then)
    fun refreshPreparationStatusChecks() = preparationActionOps.refreshPreparationStatusChecks()
    fun handleRefreshPreparationStatus() = preparationActionOps.handleRefreshPreparationStatus()
    fun handleRefreshAllSecurityChecks() = preparationActionOps.handleRefreshAllSecurityChecks()
    fun handleRefreshPreExamHealthCheck() =
        preparationActionOps.handleRefreshPreExamHealthCheck(deviceCompatibilityProfile)
    fun handleRequestSectionReport(section: DiagnosticSection) =
        preparationActionOps.handleRequestSectionReport(section)
    fun buildCurrentPreExamHealthSnapshot() = buildExamRuntimePreExamHealthSnapshot(
        context = context,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        lockTaskBridge = lockTaskBridge,
        adminSettings = adminSettings,
        vpnBypassState = vpnBypassState,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState,
        deviceTimeBypassState = deviceTimeBypassState,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        overlayRiskResult = overlayRiskResult,
        networkReadinessStatus = networkReadinessStatus,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        examRuntimeRecoveryState = examRuntimeRecoveryState,
        flowUiState = flowUiState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        batteryStatus = batteryStatus,
        dpcRuntimeStatus = dpcRuntimeStatus
    )

    val preExamHealthCheckSnapshot = buildCurrentPreExamHealthSnapshot()
    val deviceSurvivalPolicy = resolveExamRuntimeDeviceSurvivalPolicy(
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        preExamHealthSnapshot = preExamHealthCheckSnapshot
    )
    val diagnosticExportOps = ExamRuntimeDiagnosticExportOps(
        context = context,
        uiLanguage = uiLanguage,
        lowRamProfile = lowRamProfile,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        payload = payload,
        adminSettings = adminSettings,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        webViewUiState = webViewUiState,
        flowUiState = flowUiState,
        adminUiState = adminUiState,
        securityUiState = securityUiState,
        runtimeCacheState = runtimeCacheState,
        preExamHealthSnapshotProvider = { buildCurrentPreExamHealthSnapshot() }
    )

    fun handleExportExamDiagnostics(source: String) = diagnosticExportOps.export(source)

    fun handleStartExam() {
        if (
            flowUiState.startExamPreflight.visible.value ||
            flowUiState.webViewSessionResetInFlight.value ||
            flowUiState.lockTaskRequestPending.value ||
            flowUiState.geofenceStartValidationInFlight.value
        ) {
            return
        }
        startExamController.showStartExamPreflight()
        writePreviousSessionBreadcrumb(
            code = PreviousExamSessionBreadcrumbCodes.StartPressed,
            details = "score=${deviceSurvivalPolicy.score.name} | health_blocking=${deviceSurvivalPolicy.healthBlockingCount}"
        )
        coroutineScope.launch(examExceptionHandler) {
            startExamController.startExamSession()
        }
    }
    val footerShieldStatus = resolveExamFooterShieldStatus(
        examGuardArmed = examGuardArmed,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        isKeyboardAllowed = isKeyboardAllowed,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        bypassBluetooth = bypassBluetooth,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bypassAccessibility = bypassAccessibility,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        bypassAdb = bypassAdb,
        adbInspection = adbInspection,
        bypassRoot = bypassRoot,
        bypassReverseEngineering = bypassReverseEngineering,
        bypassApkIntegrity = bypassApkIntegrity,
        rootSecurityStatus = rootSecurityStatus,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        bypassVpn = bypassVpn,
        networkReadinessStatus = networkReadinessStatus,
        bypassGeofence = bypassGeofence,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bypassDeviceTime = bypassDeviceTime,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        bypassOverlay = bypassOverlay,
        overlayRiskResult = overlayRiskResult,
        bypassAppSwitch = bypassAppSwitch,
        appSwitchStatus = appSwitchStatus,
        bypassClipboard = bypassClipboard,
        signatureMismatchDetected = signatureMismatchDetected && !bypassApkIntegrity,
        securityTamperDetected = securityTamperDetected,
        forcedExitViolationCount = forcedExitViolationCount,
        keyboardViolationCount = keyboardViolationCount,
        overlayViolationCount = overlayViolationCount,
        geofenceViolationCount = geofenceViolationCount,
        fakeLocationViolationCount = fakeLocationViolationCount,
        bluetoothViolationCount = bluetoothViolationCount,
        clipboardViolationCount = clipboardViolationCount,
        showForcedExitAlarm = showForcedExitAlarm,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        showOverlayViolationDialog = showOverlayViolationDialog,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        showClipboardViolationDialog = showClipboardViolationDialog
    )
    val runtimeChromeState = buildExamRuntimeChromeState(
        examSessionStarted = examSessionStarted,
        examDisplayName = examDisplayName,
        loadingProgress = loadingProgress,
        webViewErrorMessage = webViewErrorMessage,
        hasFullscreenCustomView = fullScreenCustomView != null,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        showBuiltInExamKeyboard = showBuiltInExamKeyboard,
        showSideArrowControls = examSessionStarted && sideArrowControlsVisible,
        hasEditableFocus = hasEditableFocus,
        builtInKeyboardShiftEnabled = builtInKeyboardShiftEnabled,
        networkStatus = networkReadinessStatus,
        serverStatus = examServerStatus,
        batteryStatus = batteryStatus,
        shieldStatus = footerShieldStatus
    )
    val runtimeChromeActions = buildExamRuntimeChromeActionsForSession(
        examSessionStarted = examSessionStarted,
        screenPinningMode = screenPinningMode,
        screenPinningAvailable = screenPinningAvailable,
        lockTaskRequestPending = lockTaskRequestPending,
        deviceCompatibilityProfile = deviceCompatibilityProfile,
        isIndonesian = isIndonesian,
        isCurrentlyLoading = { loadingProgress in 0f..0.98f },
        lockTaskAlreadyActive = { lockTaskBridge.active() },
        markTrustedRuntimeChromeAction = { reason -> markTrustedRuntimeChromeAction(reason) },
        clearWebViewError = { webViewErrorMessage = null },
        loadExamUrl = {
            webViewInstance?.let { webView ->
                webView.loadExamUrlSafely(payload.examUrl)
                webView.requestedExamUrl = payload.examUrl
            }
        },
        reloadExamUrlLikeBrowser = {
            webViewInstance?.reloadExamUrlLikeBrowserSafely(payload.examUrl)
        },
        stopWebViewLoading = {
            webViewInstance?.let { webView ->
                webView.cancelPendingConnectionRetries()
                webView.cancelNavigationTimeout()
                webView.navigationState.fail(webView.url, recoverOnConnection = false)
                webView.stopLoading()
            }
        },
        setLoadingProgress = { loadingProgress = it },
        setWebViewStopRequested = { webViewStopRequested = it },
        setLastExamRefreshDecision = { lastExamRefreshDecision = it },
        setScreenPinningMessage = { screenPinningMessage = it },
        setShowExitExamDialog = { showExitExamDialog = it },
        launchExamServerProbe = { trigger, markChecking ->
            launchExamServerProbe(trigger = trigger, markChecking = markChecking)
        },
        recordAction = { code, details, level -> recordAction(code, details, level) },
        sendBuiltInKeyboardText = { rawText -> sendBuiltInKeyboardText(rawText) },
        sendBuiltInKeyboardBackspace = { sendBuiltInKeyboardBackspace() },
        sendKeyboardArrowLeft = { sendKeyboardArrowLeft() },
        sendKeyboardArrowRight = { sendKeyboardArrowRight() },
        toggleSideArrowControls = {
            sideArrowControlsVisible = !sideArrowControlsVisible
            sideArrowControlsVisible
        },
        sendBuiltInKeyboardEnter = { sendBuiltInKeyboardEnter() },
        toggleBuiltInKeyboardShift = {
            builtInKeyboardShiftEnabled = !builtInKeyboardShiftEnabled
        }
    )
    val runtimeDialogsState = buildExamRuntimeDialogsState(
        showForcedExitAlarm = showForcedExitAlarm,
        forcedExitViolationCount = forcedExitViolationCount,
        appSwitchStatus = appSwitchStatus,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        keyboardViolationCount = keyboardViolationCount,
        currentKeyboardLabel = currentKeyboardLabel,
        showOverlayViolationDialog = showOverlayViolationDialog,
        overlayViolationCount = overlayViolationCount,
        overlayTrigger = overlayRiskResult.lastTrigger,
        showOfflineWarningDialog = showOfflineWarningDialog,
        offlineDurationMs = offlineWarningDurationMs,
        currentOfflineDurationMs = currentOfflineDurationMs,
        uiLanguage = uiLanguage,
        showVpnDetectedDialog = examSessionStarted && networkReadinessStatus.diagnostics.isVpnActive && !bypassVpn,
        vpnBypassActive = bypassVpn,
        vpnBypassTampered = vpnBypassState == VpnBypassState.Tampered,
        showNetworkUnstableDialog = showNetworkUnstableDialog,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothViolationCount = bluetoothViolationCount,
        showClipboardViolationDialog = showClipboardViolationDialog,
        clipboardViolationCount = clipboardViolationCount,
        clipboardLastConfirmedAt = lastClipboardConfirmedAt,
        clipboardLastDecision = lastClipboardDecision,
        showExitExamDialog = showExitExamDialog,
        exitSessionClearInFlight = exitSessionClearInFlight
    )
    val runtimeDialogsActions = buildRuntimeDialogsActionsForSession(
        context = context,
        componentActivity = componentActivity,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        networkUiState = networkUiState,
        appSwitchStatus = appSwitchStatus,
        overlayRiskResult = overlayRiskResult,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        currentOfflineDurationMs = currentOfflineDurationMs,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        alarmSessionIdentity = alarmSessionIdentity,
        appVersionName = appVersionName,
        adminOverridesSummary = adminOverridesSummary,
        examSessionStarted = examSessionStarted,
        examGuardArmed = examGuardArmed,
        acknowledgeRuntimeAlarm = { type, violationCount, buildPayload, onUiAcknowledge ->
            acknowledgeRuntimeAlarm(type, violationCount, buildPayload, onUiAcknowledge)
        },
        recordAction = { code, details, level -> recordAction(code, details, level) },
        currentNetworkEventDetails = { trigger, status, extraContext -> currentNetworkEventDetails(trigger, status, extraContext) },
        openVpnSettings = { handleOpenVpnSettings() },
        refreshVpnStatus = { trigger -> launchNetworkManualRefresh(trigger) },
        requestSectionReport = { section -> handleRequestSectionReport(section) },
        refreshBluetoothSecurity = { triggerViolation -> refreshBluetoothSecurity(triggerViolation) },
        clearExamSessionOnExit = { reason, waitForResult ->
            clearExamSessionOnExit(reason = reason, waitForResult = waitForResult)
        },
        writePreviousSessionBreadcrumb = { code, details ->
            writePreviousSessionBreadcrumb(code = code, details = details)
        },
        onExit = onExit,
        examAlarmController = examAlarmController
    )

    val screenPinningFixNeeded = !bypassScreenPinning &&
        screenPinningAvailable &&
        screenPinningEnabledInSystem.equals("Nonaktif", ignoreCase = true)
    val reinstallApkFixNeeded = signatureMismatchDetected && officialApkUrl.isNotBlank()
    val previousExamSessionBreadcrumb = remember {
        PreviousExamSessionBreadcrumbStore.read(context)
    }
    ExamRuntimeResolvedDiagnosticsEffects(
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        examName = payload.examName,
        recordAction = { code, details, level -> recordAction(code, details, level) },
        writePreviousSessionBreadcrumb = { code, details ->
            writePreviousSessionBreadcrumb(code = code, details = details)
        }
    )
    // The pin can end without any event reaching the app (swipe-and-hold to unpin), so the
    // checklist kept saying "ready" until something else redrew it. Poll while preparing.
    var preparationPinActive by remember { mutableStateOf(lockTaskBridge.active()) }
    LaunchedEffect(examSessionStarted, lockTaskBridge, lowRamProfile) {
        while (!examSessionStarted) {
            preparationPinActive = lockTaskBridge.active()
            delay(if (lowRamProfile.ultra) 2_000L else 1_000L)
        }
    }
    val preparationState = buildPreparationStateForSession(
        payload = payload,
        adminSettings = adminSettings,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        networkUiState = networkUiState,
        locationWarmupUiState = locationWarmupUiState,
        keyboardAllowed = isKeyboardAllowed,
        sendingSection = sendingSection,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        networkTimelinePreview = networkTimelinePreview,
        screenPinningAvailable = screenPinningAvailable,
        screenPinningActive = if (examSessionStarted) lockTaskBridge.active() else preparationPinActive,
        screenPinningFixNeeded = screenPinningFixNeeded,
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
        adbBypassState = adbBypassState,
        bypassRoot = bypassRoot,
        rootBypassState = rootBypassState,
        bypassReverseEngineering = bypassReverseEngineering,
        bypassApkIntegrity = bypassApkIntegrity,
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
        externalDisplayInfoList = securityUiState.externalDisplayInfoList.value,
        bypassMultiWindow = bypassMultiWindow,
        multiWindowModeInfo = securityUiState.multiWindowModeInfo.value,
        preExamHealthCheckSnapshot = preExamHealthCheckSnapshot,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        previousExamSessionBreadcrumb = previousExamSessionBreadcrumb,
        dpcRuntimeStatus = dpcRuntimeStatus
    )
    val preparationActions = buildPreparationScreenActions(
        onChooseKeyboard = { handleChooseKeyboard() },
        onOpenKeyboardSettings = { handleOpenKeyboardSettings() },
        onGrantBluetoothPermission = { handleGrantBluetoothPermission() },
        onOpenBluetoothSettings = { handleOpenBluetoothSettings() },
        onOpenAccessibilitySettings = { handleOpenAccessibilitySettings() },
        onOpenOverlayAccessibilitySettings = { handleOpenOverlayAccessibilitySettings() },
        onOpenDeveloperOptionsSettings = { handleOpenDeveloperOptionsSettings() },
        onRequestLocationPermission = { handleRequestLocationPermission() },
        onOpenLocationServicesSettings = { handleOpenLocationServicesSettings() },
        onRefreshGeofenceLocation = { handleRefreshLocationSecurity() },
        onOpenGeofenceMapViewer = { handleOpenGeofenceMapViewer() },
        onOpenInternetSettings = { handleOpenInternetSettings() },
        onOpenVpnSettings = { handleOpenVpnSettings() },
        onOpenWifiSettings = { handleOpenWifiSettings() },
        onOpenCellularSettings = { handleOpenCellularSettings() },
        onOpenAirplaneModeSettings = { handleOpenAirplaneModeSettings() },
        onRefreshNetworkStatus = { handleRefreshNetworkStatus() },
        onOpenDateTimeSettings = { handleOpenDateTimeSettings() },
        onOpenFakeLocationDeveloperOptionsSettings = { handleOpenFakeLocationDeveloperOptionsSettings() },
        onOpenScreenPinningSettings = { handleOpenScreenPinningSettings() },
        onStartScreenPinning = { handleStartScreenPinning() },
        onOpenOverlaySettings = { handleOpenOverlaySettings() },
        onOpenAppSettings = { handleOpenAppSettings() },
        onOpenCastSettings = { handleOpenCastSettings() },
        onOpenWebViewProviderSettings = { handleOpenWebViewProviderSettings() },
        onReinstallOfficialApk = { handleReinstallOfficialApk() },
        onAcknowledgeOverlayViolation = { handleAcknowledgeOverlayViolation() },
        onReleaseScreenPinningThen = { then -> handleReleaseScreenPinningThen(then) },
        onRefreshStatus = { handleRefreshPreparationStatus() },
        onRefreshAllSecurityChecks = { handleRefreshAllSecurityChecks() },
        onRefreshHealthCheck = { handleRefreshPreExamHealthCheck() },
        onRequestSectionReport = { section -> handleRequestSectionReport(section) },
        onExportDiagnostics = { handleExportExamDiagnostics("preparation_recovery") },
        onAutoFixShown = { details ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PreparationAutoFixShown,
                details = details
            )
        },
        onPreviousSessionRecoveryHintShown = { details ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PreviousSessionRecoveryHintShown,
                details = details
            )
        },
        onAutoFixActionOpened = { actionCode ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.PreparationAutoFixActionOpened,
                details = "action=$actionCode"
            )
        },
        onScreenPinningDeferred = { details ->
            recordAction(
                code = ExamRuntimeHardeningDiagnostics.ScreenPinningDeferredUntilBlockersClear,
                details = details
            )
        },
        onStartExam = { handleStartExam() },
        onBackHome = onExit
    )
    val renderedUiCallbacks = ExamRuntimeRenderedUiCallbacks(
        componentActivity = componentActivity,
        lockTaskBridge = lockTaskBridge,
        deviceQuirkProfile = deviceQuirkProfile,
        deviceSurvivalPolicy = deviceSurvivalPolicy,
        webViewCompatibilityStatus = webViewCompatibilityStatus,
        runtimeDiagnosticsOps = runtimeDiagnosticsOps,
        runtimeMonitoringOps = runtimeMonitoringOps,
        webViewUiState = webViewUiState,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        adminUiState = adminUiState,
        examServerStatusState = examServerStatusState,
        lastTrustedRuntimeChromeActionElapsedMsState = lastTrustedRuntimeChromeActionElapsedMsState,
        lastTrustedRuntimeChromeActionReasonState = lastTrustedRuntimeChromeActionReasonState,
        examAlarmController = examAlarmController,
        hideSystemKeyboard = { hideSystemKeyboard() },
        launchTelegramSectionReport = { section -> launchTelegramSectionReport(section) },
        onExit = onExit
    )

    ExamRuntimeSessionRenderedUiSection(
        examSessionStarted = examSessionStarted,
        showGeofenceMapViewer = showGeofenceMapViewer,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
        preparationState = preparationState,
        preparationActions = preparationActions,
        runtimeChromeState = runtimeChromeState,
        runtimeChromeActions = runtimeChromeActions,
        payload = payload,
        bypassOverlay = bypassOverlay,
        examAlarmController = examAlarmController,
        participantCaptureBridge = participantCaptureBridge,
        nativeFullscreenBridge = nativeFullscreenBridge,
        keyboardBridge = keyboardBridge,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        effectiveExamUserAgent = effectiveExamUserAgent,
        fullScreenContainer = fullScreenContainer,
        fullScreenCustomView = fullScreenCustomView,
        nativeExamFullscreenActive = nativeExamFullscreenActive,
        runtimeDialogsState = runtimeDialogsState,
        runtimeDialogsActions = runtimeDialogsActions,
        pendingSection = pendingSection,
        uiLanguage = uiLanguage,
        screenPinningMessage = if (examSessionStarted) screenPinningMessage else null,
        securityIssueDialogTitle = securityIssueDialogTitle,
        securityIssueDialogMessage = securityIssueDialogMessage,
        securityIssueDialogCode = securityIssueDialogCode,
        startExamPreflightState = flowUiState.startExamPreflight,
        lockTaskRequestPending = lockTaskRequestPending,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        securityUiState = securityUiState,
        renderedUiCallbacks = renderedUiCallbacks,
        onHideSystemKeyboard = { hideSystemKeyboard() },
        onHideCustomView = { hideCustomView() },
        onOpenStaticSecurityAppSettings = { handleOpenAppSettings() },
        onOpenStaticSecurityCastSettings = { handleOpenCastSettings() },
        onRefreshStaticSecurityStatus = { handleRefreshPreparationStatus() },
        onSendStaticSecurityReport = { section -> launchTelegramSectionReport(section) },
        onRefreshNetworkStatus = preparationActions.onRefreshNetworkStatus,
        onOpenAppPermissionSettings = { handleOpenAppPermissionSettings() },
        onCancelPinningActivation = { flowUiState.pinningActivationCancelRequested.value = true },
        modifier = modifier
    )
}
