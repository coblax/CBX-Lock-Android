package com.example.coblaxexamlock.ui.exam

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.MutableState
import com.example.coblaxexamlock.AppSwitchSuppressionReason
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.PinningActivationGraceWindowMillis
import com.example.coblaxexamlock.PinningActivationPurpose
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbStore
import com.example.coblaxexamlock.ScreenPinningEnforcer
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.ScreenPinningSignals
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.VpnBypassState
import com.example.coblaxexamlock.openAccessibilitySettings
import com.example.coblaxexamlock.openAirplaneModeSettings
import com.example.coblaxexamlock.openBluetoothSettings
import com.example.coblaxexamlock.openCellularSettings
import com.example.coblaxexamlock.openDateTimeSettings
import com.example.coblaxexamlock.openDeveloperOptionsSettings
import com.example.coblaxexamlock.openInternetConnectivitySettings
import com.example.coblaxexamlock.openKeyboardSettings
import com.example.coblaxexamlock.openLocationServicesSettings
import com.example.coblaxexamlock.openOverlaySettings
import com.example.coblaxexamlock.openScreenPinningSettings
import com.example.coblaxexamlock.openVpnSettings
import com.example.coblaxexamlock.openWebViewProviderSettings
import com.example.coblaxexamlock.openWifiSettings
import com.example.coblaxexamlock.platform.openExternalUrl
import com.example.coblaxexamlock.runtime.getBluetoothConnectPermission
import com.example.coblaxexamlock.showKeyboardPicker
import com.example.coblaxexamlock.ui.preparation.PreExamHealthSnapshot

internal class ExamRuntimePreparationRefreshCallbacks(
    val launchNetworkManualRefresh: (String) -> Unit,
    val updateNetworkReadiness: (String) -> Unit,
    val launchLocationSecurityManualRefresh: (String) -> Unit,
    val refreshReverseEngineeringStatus: () -> Unit,
    val refreshIntegrityGuard: () -> Unit,
    val refreshScreenPinningDiagnostics: () -> Unit,
    val incrementWebViewCompatibilityRefreshKey: () -> Unit,
    val updateAccessibilityGuardEnabled: (Boolean) -> Unit,
    val refreshKeyboardSecurity: (Boolean) -> Unit,
    val refreshBluetoothSecurity: (Boolean) -> Unit,
    val refreshDeviceIntegritySecurity: (Boolean) -> Unit,
    val refreshDeviceTimeSecurity: (String) -> DeviceTimeSecurityStatus,
    val refreshRuntimeStaticSecurity: () -> Unit,
    val debugLogExamStart: (String) -> Unit
)

internal fun refreshExamRuntimePreparationStatusChecks(
    context: Context,
    networkManualRefreshInFlight: Boolean,
    geofenceManualRefreshInFlight: Boolean,
    isExamGuardAccessibilityEnabled: (Context) -> Boolean,
    callbacks: ExamRuntimePreparationRefreshCallbacks
) {
    val startedAt = SystemClock.elapsedRealtime()
    if (!networkManualRefreshInFlight) {
        callbacks.launchNetworkManualRefresh("checklist_refresh")
    } else {
        callbacks.updateNetworkReadiness("checklist_refresh")
    }
    if (!geofenceManualRefreshInFlight) {
        callbacks.launchLocationSecurityManualRefresh("checklist_refresh")
    }
    callbacks.refreshReverseEngineeringStatus()
    callbacks.refreshIntegrityGuard()
    callbacks.refreshScreenPinningDiagnostics()
    callbacks.incrementWebViewCompatibilityRefreshKey()
    callbacks.updateAccessibilityGuardEnabled(isExamGuardAccessibilityEnabled(context))
    callbacks.refreshKeyboardSecurity(false)
    callbacks.refreshBluetoothSecurity(false)
    callbacks.refreshDeviceIntegritySecurity(false)
    callbacks.refreshDeviceTimeSecurity("checklist_refresh")
    callbacks.refreshRuntimeStaticSecurity()
    callbacks.debugLogExamStart(
        "refreshPreparationStatusChecks scheduled in ${SystemClock.elapsedRealtime() - startedAt} ms"
    )
}

internal fun buildExamDiagnosticSnapshotForSession(
    context: Context,
    source: String,
    lowRamProfile: com.example.coblaxexamlock.LowRamProfile,
    deviceCompatibilityProfile: com.example.coblaxexamlock.DeviceCompatibilityProfile,
    deviceSurvivalPolicy: DeviceSurvivalPolicy,
    payload: com.example.coblaxexamlock.ExamQrPayload,
    examSessionStarted: Boolean,
    examGuardArmed: Boolean,
    webViewPresent: Boolean,
    webViewCompatibilityStatus: WebViewCompatibilityStatus,
    webViewError: String?,
    recoveryState: ExamRuntimeRecoveryState,
    lastRuntimeMemoryActionSummary: String?,
    networkReadinessStatus: NetworkReadinessStatus,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    preExamHealthSnapshot: com.example.coblaxexamlock.ui.preparation.PreExamHealthSnapshot,
    lastPinningDecision: String,
    lastOverlayDecision: String?,
    lastRefreshDecision: String?,
    reverseEngineeringBypass: Boolean,
    apkIntegrityBypass: Boolean,
    reverseEngineeringSignals: String,
    integrityIssues: String,
    diagnosticEvents: List<DiagnosticEvent>
): ExamDiagnosticSnapshot {
    return buildExamDiagnosticSnapshot(
        ExamDiagnosticSnapshotInput(
            source = source,
            lowRamProfile = lowRamProfile,
            deviceCompatibilityProfile = deviceCompatibilityProfile,
            deviceSurvivalPolicy = deviceSurvivalPolicy,
            previousExamSessionBreadcrumb = PreviousExamSessionBreadcrumbStore.read(context),
            payload = payload,
            examSessionStarted = examSessionStarted,
            examGuardArmed = examGuardArmed,
            webViewPresent = webViewPresent,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            webViewError = webViewError,
            rendererGone = recoveryState == ExamRuntimeRecoveryState.RendererGone ||
                recoveryState == ExamRuntimeRecoveryState.ReadyToRetry,
            recoveryState = recoveryState,
            lastTrimMemoryAction = lastRuntimeMemoryActionSummary,
            networkReadinessStatus = networkReadinessStatus,
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
            deviceTimeSecurityStatus = deviceTimeSecurityStatus,
            preExamHealthSnapshot = preExamHealthSnapshot,
            lastPinningDecision = lastPinningDecision,
            lastOverlayDecision = lastOverlayDecision,
            lastRefreshDecision = lastRefreshDecision,
            reverseEngineeringBypass = reverseEngineeringBypass,
            apkIntegrityBypass = apkIntegrityBypass,
            reverseEngineeringSignals = reverseEngineeringSignals,
            integrityIssues = integrityIssues,
            diagnosticEvents = diagnosticEvents
        )
    )
}

internal fun exportExamDiagnosticsForSession(
    context: Context,
    uiLanguage: UiLanguage,
    source: String,
    snapshot: ExamDiagnosticSnapshot,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    showExportFailure: (String, String) -> Unit
) {
    runCatching {
        ExamDiagnosticExportHelper.share(context, snapshot)
    }.onSuccess {
        recordAction(
            ExamRuntimeHardeningDiagnostics.DiagnosticExportSucceeded,
            "source=$source | events=${snapshot.events.size}",
            DiagnosticEventLevel.INFO
        )
    }.onFailure { throwable ->
        val errorSummary = throwable.message?.take(160)
            ?: throwable.javaClass.simpleName.take(160)
        recordAction(
            ExamRuntimeHardeningDiagnostics.DiagnosticExportFailed,
            "source=$source | error=$errorSummary",
            DiagnosticEventLevel.ERROR
        )
        showExportFailure(
            localized(uiLanguage, "Diagnostics export failed", "Export diagnostik gagal"),
            errorSummary
        )
    }
}

internal class ExamRuntimeDiagnosticExportOps(
    private val context: Context,
    private val uiLanguage: UiLanguage,
    private val lowRamProfile: LowRamProfile,
    private val deviceCompatibilityProfile: DeviceCompatibilityProfile,
    private val deviceSurvivalPolicy: DeviceSurvivalPolicy,
    private val payload: ExamQrPayload,
    private val adminSettings: AdminSettings,
    private val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    private val runtimeDiagnosticsOps: ExamRuntimeDiagnosticsOps,
    private val webViewUiState: ExamRuntimeWebViewUiState,
    private val flowUiState: ExamRuntimeFlowUiState,
    private val adminUiState: ExamRuntimeAdminUiState,
    private val securityUiState: ExamRuntimeSecurityUiState,
    private val runtimeCacheState: ExamRuntimeRuntimeCacheState,
    private val preExamHealthSnapshotProvider: () -> PreExamHealthSnapshot
) {
    private val examGuardArmed: Boolean
        get() = adminUiState.examRuntimeMonitoringArmed.value ||
            flowUiState.lockTaskRequestPending.value ||
            flowUiState.examSessionStarted.value

    fun buildSnapshot(source: String): ExamDiagnosticSnapshot =
        buildExamDiagnosticSnapshotForSession(
            context = context,
            source = source,
            lowRamProfile = lowRamProfile,
            deviceCompatibilityProfile = deviceCompatibilityProfile,
            deviceSurvivalPolicy = deviceSurvivalPolicy,
            payload = payload,
            examSessionStarted = flowUiState.examSessionStarted.value,
            examGuardArmed = examGuardArmed,
            webViewPresent = webViewUiState.instance.value != null,
            webViewCompatibilityStatus = webViewCompatibilityStatus,
            webViewError = flowUiState.webViewErrorMessage.value ?: flowUiState.webViewSessionResetError.value,
            recoveryState = webViewUiState.recoveryState.value,
            lastRuntimeMemoryActionSummary = runtimeCacheState.lastRuntimeMemoryActionSummary.value,
            networkReadinessStatus = runtimeDiagnosticsOps.networkReadinessStatus,
            geofenceRuntimeStatus = runtimeDiagnosticsOps.geofenceRuntimeStatus,
            fakeLocationRuntimeStatus = runtimeDiagnosticsOps.fakeLocationRuntimeStatus,
            deviceTimeSecurityStatus = runtimeDiagnosticsOps.deviceTimeSecurityStatus,
            preExamHealthSnapshot = preExamHealthSnapshotProvider(),
            lastPinningDecision = adminUiState.screenPinningUserActionInference.value,
            lastOverlayDecision = securityUiState.lastOverlayContext.value,
            lastRefreshDecision = securityUiState.lastExamRefreshDecision.value,
            reverseEngineeringBypass = adminSettings.bypassReverseEngineering,
            apkIntegrityBypass = adminSettings.bypassApkIntegrity,
            reverseEngineeringSignals = securityUiState.tamperSummary.value,
            integrityIssues = securityUiState.integrityPublicSummary.value
                .ifBlank { securityUiState.integritySummary.value },
            diagnosticEvents = adminUiState.diagnosticEvents.value
        )

    fun export(source: String) {
        runtimeDiagnosticsOps.recordAction(
            code = ExamRuntimeHardeningDiagnostics.DiagnosticExportRequested,
            details = "source=$source"
        )
        exportExamDiagnosticsForSession(
            context = context,
            uiLanguage = uiLanguage,
            source = source,
            snapshot = buildSnapshot(source),
            recordAction = runtimeDiagnosticsOps::recordAction,
            showExportFailure = { title, message ->
                adminUiState.bugReportFeedbackTitle.value = title
                adminUiState.bugReportFeedbackMessage.value = message
            }
        )
    }
}

internal class ExamRuntimePreparationActionOps(
    private val context: Context,
    private val activity: Activity?,
    private val uiLanguage: UiLanguage,
    private val isIndonesian: Boolean,
    private val adminSettings: AdminSettings,
    private val officialApkUrl: String,
    private val lockTaskBridge: ActivityLockTaskBridge,
    private val screenPinningMode: ScreenPinningMode,
    private val vpnBypassState: VpnBypassState,
    private val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    private val flowUiState: ExamRuntimeFlowUiState,
    private val securityUiState: ExamRuntimeSecurityUiState,
    private val adminUiState: ExamRuntimeAdminUiState,
    private val networkUiState: ExamRuntimeNetworkUiState,
    private val webViewUiState: ExamRuntimeWebViewUiState,
    private val accessibilityGuardEnabledState: MutableState<Boolean>,
    private val runtimeDiagnosticsOps: ExamRuntimeDiagnosticsOps,
    private val runtimeSecurityOps: ExamRuntimeSecurityOps,
    private val runtimeMonitoringOps: ExamRuntimeMonitoringOps,
    private val examAlarmController: ExamAlarmController,
    private val launchBluetoothPermission: () -> Unit,
    private val launchLocationPermission: (Array<String>) -> Unit,
    private val incrementWebViewCompatibilityRefreshKey: () -> Unit,
    private val debugLogExamStart: (String) -> Unit
) {
    fun handleChooseKeyboard() {
        runtimeDiagnosticsOps.recordAction(code = "KEYBOARD_PICKER_OPENED")
        if (!showKeyboardPicker(activity)) {
            openKeyboardSettings(context)
        }
    }

    fun handleOpenKeyboardSettings() {
        runtimeDiagnosticsOps.recordAction(code = "KEYBOARD_SETTINGS_OPENED")
        openKeyboardSettings(context)
    }

    fun handleGrantBluetoothPermission() {
        runtimeDiagnosticsOps.recordAction(code = "BLUETOOTH_PERMISSION_REQUESTED")
        launchBluetoothPermission()
    }

    fun handleOpenBluetoothSettings() {
        runtimeDiagnosticsOps.recordAction(code = "BLUETOOTH_SETTINGS_OPENED")
        openBluetoothSettings(context)
    }

    fun handleOpenAccessibilitySettings() {
        runtimeDiagnosticsOps.recordAction(code = "ACCESSIBILITY_SETTINGS_OPENED")
        openAccessibilitySettings(context)
    }

    fun handleOpenOverlayAccessibilitySettings() {
        runtimeDiagnosticsOps.recordAction(code = "OVERLAY_ACCESSIBILITY_SETTINGS_OPENED")
        openAccessibilitySettings(context)
    }

    fun handleOpenDeveloperOptionsSettings() {
        runtimeDiagnosticsOps.recordAction(code = "DEVELOPER_OPTIONS_OPENED")
        openDeveloperOptionsSettings(context)
    }

    fun handleRequestLocationPermission() {
        runtimeDiagnosticsOps.invalidateWarmLocationValidationCache()
        runtimeDiagnosticsOps.recordAction(
            code = "LOCATION_PERMISSION_REQUESTED",
            details = "trigger=location_quick_fix",
            level = DiagnosticEventLevel.INFO
        )
        flowUiState.geofencePermissionRequestInFlight.value = true
        launchLocationPermission(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun handleOpenLocationServicesSettings() {
        runtimeDiagnosticsOps.invalidateWarmLocationValidationCache()
        runtimeDiagnosticsOps.recordAction(
            code = "LOCATION_SERVICES_SETTINGS_OPENED",
            details = "trigger=location_quick_fix",
            level = DiagnosticEventLevel.INFO
        )
        openLocationServicesSettings(context)
    }

    fun handleRefreshLocationSecurity() {
        runtimeDiagnosticsOps.launchLocationSecurityManualRefresh(trigger = "location_quick_fix")
        runtimeDiagnosticsOps.recordAction(
            code = "LOCATION_QUICK_FIX_REFRESH_REQUESTED",
            details = "trigger=checklist",
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenGeofenceMapViewer() {
        flowUiState.showGeofenceMapViewer.value = true
        runtimeDiagnosticsOps.recordAction(
            code = "GEOFENCE_MAP_VIEW_OPENED",
            details = runtimeDiagnosticsOps.currentGeofenceEventDetails(
                trigger = "checklist_map_view",
                geofenceStatus = securityUiState.geofenceSecurityStatus.value
            ),
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenInternetSettings() {
        runtimeDiagnosticsOps.recordAction(
            code = "INTERNET_SETTINGS_OPENED",
            details = runtimeDiagnosticsOps.currentNetworkEventDetails(
                trigger = "network_quick_fix",
                status = runtimeDiagnosticsOps.networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openInternetConnectivitySettings(context)
    }

    fun handleOpenVpnSettings() {
        runtimeDiagnosticsOps.recordAction(
            code = ExamRuntimeHardeningDiagnostics.VpnSettingsOpened,
            details = runtimeDiagnosticsOps.currentNetworkEventDetails(
                trigger = "network_vpn_quick_fix",
                status = runtimeDiagnosticsOps.networkReadinessStatus,
                extraContext = "bypass=${if (vpnBypassState == VpnBypassState.Active) "yes" else "no"}"
            ),
            level = DiagnosticEventLevel.INFO
        )
        openVpnSettings(context)
    }

    fun handleOpenDateTimeSettings() {
        runtimeDiagnosticsOps.recordAction(
            code = "DEVICE_TIME_SETTINGS_OPENED",
            details = buildDeviceTimeEventDetails(
                trigger = "device_time_quick_fix",
                status = runtimeDiagnosticsOps.refreshDeviceTimeSecurity(
                    trigger = "device_time_quick_fix",
                    emitDiagnosticEvent = false
                )
            ),
            level = DiagnosticEventLevel.INFO
        )
        openDateTimeSettings(context)
    }

    fun handleOpenWifiSettings() {
        runtimeDiagnosticsOps.recordAction(
            code = "WIFI_SETTINGS_OPENED",
            details = runtimeDiagnosticsOps.currentNetworkEventDetails(
                trigger = "network_wifi_quick_fix",
                status = runtimeDiagnosticsOps.networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openWifiSettings(context)
    }

    fun handleOpenCellularSettings() {
        runtimeDiagnosticsOps.recordAction(
            code = "CELLULAR_SETTINGS_OPENED",
            details = runtimeDiagnosticsOps.currentNetworkEventDetails(
                trigger = "network_cellular_quick_fix",
                status = runtimeDiagnosticsOps.networkReadinessStatus,
                extraContext = "last_connected=" +
                    (networkUiState.lastConnectedNetworkLabel.value?.ifBlank { "-" } ?: "-")
            ),
            level = DiagnosticEventLevel.INFO
        )
        openCellularSettings(context)
    }

    fun handleOpenAirplaneModeSettings() {
        runtimeDiagnosticsOps.recordAction(
            code = "AIRPLANE_MODE_SETTINGS_OPENED",
            details = runtimeDiagnosticsOps.currentNetworkEventDetails(
                trigger = "network_airplane_mode_quick_fix",
                status = runtimeDiagnosticsOps.networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openAirplaneModeSettings(context)
    }

    fun handleRefreshNetworkStatus() {
        runtimeDiagnosticsOps.launchNetworkManualRefresh(trigger = "network_quick_fix")
        runtimeDiagnosticsOps.recordAction(
            code = "NETWORK_QUICK_FIX_REFRESH_REQUESTED",
            details = runtimeDiagnosticsOps.currentNetworkEventDetails(
                trigger = "network_quick_fix",
                status = runtimeDiagnosticsOps.networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenFakeLocationDeveloperOptionsSettings() {
        runtimeDiagnosticsOps.invalidateWarmLocationValidationCache()
        runtimeDiagnosticsOps.recordAction(
            code = "FAKE_LOCATION_DEVELOPER_OPTIONS_OPENED",
            details = runtimeDiagnosticsOps.currentFakeLocationEventDetails(
                trigger = "fake_location_developer_options_quick_fix",
                fakeLocationStatus = runtimeDiagnosticsOps.fakeLocationRuntimeStatus.securityStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openDeveloperOptionsSettings(context)
    }

    fun handleOpenScreenPinningSettings() {
        runtimeDiagnosticsOps.recordAction(code = "SCREEN_PINNING_SETTINGS_OPENED")
        openScreenPinningSettings(context)
    }

    fun handleStartScreenPinning() {
        runtimeSecurityOps.refreshScreenPinningDiagnostics()
        if (flowUiState.lockTaskRequestPending.value || flowUiState.examSessionStarted.value) {
            return
        }
        if (adminSettings.bypassScreenPinning || screenPinningMode == ScreenPinningMode.Bypassed) {
            runtimeDiagnosticsOps.recordAction(
                code = ScreenPinningSignals.eventBypassUsed(),
                details = "source=preparation_start_screen_pinning | bypass=true",
                level = DiagnosticEventLevel.INFO
            )
            flowUiState.pinningActivationPurpose.value = PinningActivationPurpose.ExamStart
            return
        }
        if (!adminUiState.screenPinningAvailable.value) {
            runtimeDiagnosticsOps.recordAction(
                code = ExamRuntimeHardeningDiagnostics.StartExamBlockedScreenPinningInactive,
                details = "source=preparation_start_screen_pinning | screen_pinning_available=false",
                level = DiagnosticEventLevel.WARNING
            )
            adminUiState.securityIssueDialogTitle.value = localized(
                uiLanguage,
                "Screen Pinning Unavailable",
                "Screen Pinning Tidak Tersedia"
            )
            adminUiState.securityIssueDialogMessage.value = localized(
                uiLanguage,
                "This device does not support Screen Pinning. Use the Accessibility Exam Guard fallback or Secret Admin bypass.",
                "Perangkat ini tidak mendukung Screen Pinning. Gunakan fallback Accessibility Exam Guard atau bypass Secret Admin."
            )
            return
        }
        if (adminUiState.screenPinningEnabledInSystem.value.equals("Nonaktif", ignoreCase = true)) {
            runtimeDiagnosticsOps.recordAction(
                code = "SCREEN_PINNING_START_ATTEMPTED_WITH_SETTING_OFF",
                details = "source=preparation_start_screen_pinning | system_setting=" +
                    adminUiState.screenPinningEnabledInSystem.value,
                level = DiagnosticEventLevel.INFO
            )
        }
        if (lockTaskBridge.active()) {
            val activeState = lockTaskBridge.stateLabel()
            adminUiState.lockTaskStateBeforePinningRequest.value = activeState
            adminUiState.lockTaskStateAfterPinningRequest.value = activeState
            adminUiState.screenPinningRequestOutcome.value = ScreenPinningSignals.successOutcome()
            adminUiState.screenPinningDialogLikelyShown.value = false
            adminUiState.screenPinningUserActionInference.value = "Sudah aktif; setup preparation dilewati"
            adminUiState.screenPinningActivationDurationMs.value = 0L
            adminUiState.examSessionCancelledByPinningFailure.value = false
            flowUiState.pinningActivationPurpose.value = PinningActivationPurpose.ExamStart
            flowUiState.lockTaskRequestPending.value = false
            flowUiState.pinningActivationState.value = PinningActivationState.ActiveConfirmed
            flowUiState.pinningActivationStartedAtElapsedMs.value = null
            flowUiState.pinningSuppressedTransitionCount.intValue = 0
            runtimeDiagnosticsOps.clearAppSwitchSuppression()
            flowUiState.screenPinningMessage.value = null
            flowUiState.webViewErrorMessage.value = null
            adminUiState.exitOnSecurityIssueDialogDismiss.value = false
            runtimeDiagnosticsOps.recordAction(
                code = ScreenPinningSignals.eventActive(),
                details = "preparation_setup_already_active | state=$activeState",
                level = DiagnosticEventLevel.INFO
            )
            return
        }

        val requestState = ScreenPinningEnforcer.launchState(screenPinningMode, lockTaskBridge)
        adminUiState.lockTaskStateBeforePinningRequest.value = requestState.beforeState
        adminUiState.lockTaskStateAfterPinningRequest.value = requestState.afterState
        adminUiState.screenPinningRequestOutcome.value = requestState.outcome
        adminUiState.screenPinningDialogLikelyShown.value = requestState.dialogLikelyShown
        adminUiState.screenPinningUserActionInference.value = requestState.userActionInference
        adminUiState.screenPinningActivationDurationMs.value = requestState.activationDurationMs
        adminUiState.examSessionCancelledByPinningFailure.value = false
        flowUiState.pinningActivationPurpose.value = PinningActivationPurpose.PreparationSetup
        flowUiState.lockTaskRequestPending.value = true
        flowUiState.pinningActivationState.value = PinningActivationState.Requested
        flowUiState.pinningActivationStartedAtElapsedMs.value = SystemClock.elapsedRealtime()
        flowUiState.pinningSuppressedTransitionCount.intValue = 0
        runtimeDiagnosticsOps.recordAction(
            code = requestState.eventCode,
            details = "purpose=preparation_setup | ${requestState.eventDetails}",
            level = DiagnosticEventLevel.INFO
        )
        runtimeDiagnosticsOps.recordAction(
            code = ExamRuntimeHardeningDiagnostics.PinningStartRequested,
            details = "purpose=preparation_setup | before=${requestState.beforeState} | " +
                "state=${requestState.afterState} | grace_ms=$PinningActivationGraceWindowMillis",
            level = DiagnosticEventLevel.INFO
        )
        runtimeDiagnosticsOps.recordAction(
            code = ExamRuntimeHardeningDiagnostics.PinningDialogExpected,
            details = "purpose=preparation_setup | screen_pinning_dialog_expected=true | keep_app_foreground=true",
            level = DiagnosticEventLevel.INFO
        )
        runtimeDiagnosticsOps.setAppSwitchSuppression(AppSwitchSuppressionReason.ScreenPinningRequest)
        flowUiState.screenPinningMessage.value = ScreenPinningEnforcer.activatingMessage(
            isIndonesian = isIndonesian,
            purpose = PinningActivationPurpose.PreparationSetup
        )
        flowUiState.webViewErrorMessage.value = null
        adminUiState.exitOnSecurityIssueDialogDismiss.value = false
    }

    fun handleOpenOverlaySettings() {
        runtimeDiagnosticsOps.recordAction(code = "OVERLAY_SETTINGS_OPENED")
        openOverlaySettings(context)
    }

    fun handleOpenAppSettings() {
        runtimeDiagnosticsOps.recordAction(code = "APP_SETTINGS_OPENED", details = "quick_fix=screen_recorder")
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun handleOpenCastSettings() {
        runtimeDiagnosticsOps.recordAction(code = "CAST_SETTINGS_OPENED", details = "quick_fix=display_mirror")
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_CAST_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun handleOpenWebViewProviderSettings() {
        runtimeDiagnosticsOps.recordAction(
            code = "WEBVIEW_PROVIDER_SETTINGS_OPENED",
            details = webViewCompatibilityStatus.adminDetail
        )
        runtimeDiagnosticsOps.recordAction(
            code = ExamRuntimeHardeningDiagnostics.WebViewProviderHealthFixOpened,
            details = webViewCompatibilityStatus.adminDetail
        )
        openWebViewProviderSettings(
            context = context,
            providerPackageName = webViewCompatibilityStatus.packageName
        )
    }

    fun handleReinstallOfficialApk() {
        runtimeDiagnosticsOps.recordAction(code = "OFFICIAL_APK_REINSTALL_OPENED")
        openExternalUrl(context, officialApkUrl)
    }

    fun refreshPreparationStatusChecks() {
        refreshExamRuntimePreparationStatusChecks(
            context = context,
            networkManualRefreshInFlight = networkUiState.networkManualRefreshInFlight.value,
            geofenceManualRefreshInFlight = flowUiState.geofenceManualRefreshInFlight.value,
            isExamGuardAccessibilityEnabled = ::isExamGuardAccessibilityEnabled,
            callbacks = ExamRuntimePreparationRefreshCallbacks(
                launchNetworkManualRefresh = runtimeDiagnosticsOps::launchNetworkManualRefresh,
                updateNetworkReadiness = runtimeDiagnosticsOps::updateNetworkReadiness,
                launchLocationSecurityManualRefresh =
                    runtimeDiagnosticsOps::launchLocationSecurityManualRefresh,
                refreshReverseEngineeringStatus = runtimeMonitoringOps::refreshReverseEngineeringStatus,
                refreshIntegrityGuard = runtimeMonitoringOps::refreshIntegrityGuard,
                refreshScreenPinningDiagnostics = runtimeSecurityOps::refreshScreenPinningDiagnostics,
                incrementWebViewCompatibilityRefreshKey = incrementWebViewCompatibilityRefreshKey,
                updateAccessibilityGuardEnabled = { accessibilityGuardEnabledState.value = it },
                refreshKeyboardSecurity = runtimeSecurityOps::refreshKeyboardSecurity,
                refreshBluetoothSecurity = runtimeSecurityOps::refreshBluetoothSecurity,
                refreshDeviceIntegritySecurity = runtimeSecurityOps::refreshDeviceIntegritySecurity,
                refreshDeviceTimeSecurity = { trigger ->
                    runtimeDiagnosticsOps.refreshDeviceTimeSecurity(trigger = trigger)
                },
                refreshRuntimeStaticSecurity = {
                    refreshRuntimeStaticSecurityForSession(
                        context = context,
                        examSessionStarted = flowUiState.examSessionStarted.value,
                        bypassScreenRecorder = adminSettings.bypassScreenRecorder,
                        bypassDisplayMirror = adminSettings.bypassDisplayMirror,
                        bypassMultiWindow = adminSettings.bypassMultiWindow,
                        securityUiState = securityUiState,
                        trigger = "checklist_refresh",
                        recordAction = runtimeDiagnosticsOps::recordAction,
                        startAlarm = examAlarmController::start,
                        forceRefresh = true
                    )
                },
                debugLogExamStart = debugLogExamStart
            )
        )
    }

    fun handleRefreshPreparationStatus() {
        runtimeDiagnosticsOps.recordAction(code = "SECURITY_STATUS_REFRESHED")
        refreshPreparationStatusChecks()
    }

    fun handleRefreshAllSecurityChecks() {
        runtimeDiagnosticsOps.recordAction(
            code = "ALL_SECURITY_CHECKS_REFRESH_REQUESTED",
            details = "source=quick_fixes",
            level = DiagnosticEventLevel.INFO
        )
        refreshPreparationStatusChecks()
    }

    fun handleRefreshPreExamHealthCheck(deviceCompatibilityProfile: DeviceCompatibilityProfile) {
        handleExamRuntimePreExamHealthRefresh(
            deviceCompatibilityProfile = deviceCompatibilityProfile,
            recordAction = runtimeDiagnosticsOps::recordAction,
            refreshPreparationStatusChecks = ::refreshPreparationStatusChecks
        )
    }

    fun handleRequestSectionReport(section: DiagnosticSection) {
        adminUiState.pendingSection.value = section
    }
}
