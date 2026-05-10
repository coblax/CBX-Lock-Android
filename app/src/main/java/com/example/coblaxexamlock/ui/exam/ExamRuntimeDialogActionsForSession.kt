package com.example.coblaxexamlock.ui.exam

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.AlarmSessionIdentity
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbCodes
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.openBluetoothSettings
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsActions
import kotlinx.coroutines.launch

internal fun buildRuntimeDialogsActionsForSession(
    context: Context,
    componentActivity: ComponentActivity,
    flowUiState: ExamRuntimeFlowUiState,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    networkUiState: ExamRuntimeNetworkUiState,
    appSwitchStatus: AppSwitchStatus,
    overlayRiskResult: OverlayRiskResult,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    currentOfflineDurationMs: Long?,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    clipboardRuntimeStatus: ClipboardRuntimeStatus,
    alarmSessionIdentity: AlarmSessionIdentity,
    appVersionName: String,
    adminOverridesSummary: String,
    examSessionStarted: Boolean,
    examGuardArmed: Boolean,
    acknowledgeRuntimeAlarm: (
        AlarmAcknowledgeType,
        Int,
        (String) -> AlarmAcknowledgePayload,
        () -> Unit
    ) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    currentNetworkEventDetails: (String, NetworkReadinessStatus, String?) -> String,
    openVpnSettings: () -> Unit,
    refreshVpnStatus: (String) -> Unit,
    requestSectionReport: (DiagnosticSection) -> Unit,
    refreshBluetoothSecurity: (Boolean) -> Unit,
    clearExamSessionOnExit: suspend (String, Boolean) -> Result<Unit>,
    writePreviousSessionBreadcrumb: (String, String) -> Unit,
    onExit: () -> Unit,
    examAlarmController: ExamAlarmController
): ExamRuntimeDialogsActions {
    return buildExamRuntimeDialogsActions(
        forcedExitViolationCount = securityUiState.forcedExitViolationCount.intValue,
        appSwitchStatus = appSwitchStatus,
        keyboardViolationCount = securityUiState.keyboardViolationCount.intValue,
        currentKeyboardLabel = flowUiState.currentKeyboardLabel.value,
        overlayViolationCount = securityUiState.overlayViolationCount.intValue,
        overlayRiskResult = overlayRiskResult,
        lastConnectedNetworkLabel = networkUiState.lastConnectedNetworkLabel.value,
        offlineWarningDurationMs = networkUiState.offlineWarningDurationMs.value,
        currentOfflineDurationMs = currentOfflineDurationMs,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        geofenceViolationCount = flowUiState.geofenceViolationCount.intValue,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationViolationCount = flowUiState.fakeLocationViolationCount.intValue,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bluetoothViolationCount = securityUiState.bluetoothViolationCount.intValue,
        bluetoothEnabled = securityUiState.bluetoothEnabled.value,
        clipboardViolationCount = clipboardUiState.clipboardViolationCount.intValue,
        lastClipboardConfirmedAt = clipboardUiState.lastClipboardConfirmedAt.value,
        lastClipboardDecision = clipboardUiState.lastClipboardDecision.value,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        alarmSessionIdentity = alarmSessionIdentity,
        appVersionName = appVersionName,
        adminOverridesSummary = adminOverridesSummary,
        examSessionStarted = examSessionStarted,
        examGuardArmed = examGuardArmed,
        acknowledgeRuntimeAlarm = acknowledgeRuntimeAlarm,
        recordAction = recordAction,
        currentNetworkEventDetails = currentNetworkEventDetails,
        dismissForcedExitAlarm = {
            securityUiState.showForcedExitAlarm.value = false
            securityUiState.pendingForcedExitViolation.value = false
            examAlarmController.stop()
        },
        dismissKeyboardViolationDialog = {
            securityUiState.showKeyboardViolationDialog.value = false
            examAlarmController.stop()
        },
        dismissOverlayViolationDialog = {
            securityUiState.showOverlayViolationDialog.value = false
            examAlarmController.stop()
        },
        dismissOfflineWarningDialog = { networkUiState.showOfflineWarningDialog.value = false },
        openVpnSettings = openVpnSettings,
        refreshVpnStatus = { refreshVpnStatus("vpn_runtime_dialog") },
        sendVpnReport = { requestSectionReport(DiagnosticSection.Network) },
        dismissNetworkUnstableDialog = {
            networkUiState.showNetworkUnstableDialog.value = false
        },
        dismissGeofenceViolationDialog = {
            flowUiState.showGeofenceViolationDialog.value = false
            examAlarmController.stop()
        },
        dismissFakeLocationViolationDialog = {
            flowUiState.showFakeLocationViolationDialog.value = false
            examAlarmController.stop()
        },
        openBluetoothSettings = { openBluetoothSettings(context) },
        dismissBluetoothViolationDialog = {
            securityUiState.showBluetoothViolationDialog.value = false
            examAlarmController.stop()
        },
        refreshBluetoothSecurity = { refreshBluetoothSecurity(false) },
        dismissClipboardViolationDialog = {
            clipboardUiState.showClipboardViolationDialog.value = false
            examAlarmController.stop()
        },
        dismissExitExamDialog = {
            if (!flowUiState.exitSessionClearInFlight.value) {
                flowUiState.showExitExamDialog.value = false
            }
        },
        confirmExitExam = {
            if (!flowUiState.exitSessionClearInFlight.value) {
                componentActivity.lifecycleScope.launch {
                    clearExamSessionOnExit("footer_home_confirm", true)
                    writePreviousSessionBreadcrumb(
                        PreviousExamSessionBreadcrumbCodes.ExitCompleted,
                        "reason=footer_home_confirm"
                    )
                    flowUiState.showExitExamDialog.value = false
                    onExit()
                }
            }
        }
    )
}
