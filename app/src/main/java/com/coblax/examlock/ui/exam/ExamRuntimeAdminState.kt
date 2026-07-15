package com.coblax.examlock.ui.exam

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.coblax.examlock.AppSwitchSuppressionReason
import com.coblax.examlock.ExamParticipantContext
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.format.diagnosticTimestamp
import com.coblax.examlock.model.DiagnosticEvent
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.save.DiagnosticEventLogSaver
import com.coblax.examlock.ScreenPinningPlatformBridge

internal class ExamRuntimeAdminUiState(
    val securityIssueDialogTitle: MutableState<String?>,
    val securityIssueDialogMessage: MutableState<String?>,
    val securityIssueDialogCode: MutableState<String?>,
    val exitOnSecurityIssueDialogDismiss: MutableState<Boolean>,
    val screenPinningBypassTamperLogged: MutableState<Boolean>,
    val accessibilityBypassTamperLogged: MutableState<Boolean>,
    val adbBypassTamperLogged: MutableState<Boolean>,
    val clipboardBypassTamperLogged: MutableState<Boolean>,
    val overlayBypassTamperLogged: MutableState<Boolean>,
    val geofenceBypassTamperLogged: MutableState<Boolean>,
    val fakeLocationBypassTamperLogged: MutableState<Boolean>,
    val deviceTimeBypassTamperLogged: MutableState<Boolean>,
    val vpnBypassTamperLogged: MutableState<Boolean>,
    val appSwitchBypassTamperLogged: MutableState<Boolean>,
    val rootBypassTamperLogged: MutableState<Boolean>,
    val lastAppSwitchTrigger: MutableState<String?>,
    val lastAppSwitchAt: MutableState<String?>,
    val lastAppSwitchContext: MutableState<String?>,
    val appSwitchSuppressionReason: MutableState<AppSwitchSuppressionReason?>,
    val appSwitchSuppressedUntilElapsedMs: MutableState<Long?>,
    val appSwitchLifecycleResumePending: MutableState<Boolean>,
    val appSwitchFallbackArmedLogged: MutableState<Boolean>,
    val screenPinningAvailable: MutableState<Boolean>,
    val screenPinningEnabledInSystem: MutableState<String>,
    val lockTaskStateBeforePinningRequest: MutableState<String>,
    val lockTaskStateAfterPinningRequest: MutableState<String>,
    val screenPinningRequestOutcome: MutableState<String>,
    val screenPinningDialogLikelyShown: MutableState<Boolean>,
    val screenPinningUserActionInference: MutableState<String>,
    val screenPinningActivationDurationMs: MutableState<Long?>,
    val examSessionCancelledByPinningFailure: MutableState<Boolean>,
    val sendingSection: MutableState<DiagnosticSection?>,
    val pendingSection: MutableState<DiagnosticSection?>,
    val bugReportFeedbackTitle: MutableState<String?>,
    val bugReportFeedbackMessage: MutableState<String?>,
    val appStartedAtElapsedMs: Long,
    val examRuntimeMonitoringArmed: MutableState<Boolean>,
    val examSessionStartedAtElapsedMs: MutableState<Long?>,
    val lastParticipantCaptureLogKey: MutableState<String?>,
    val participantContext: MutableState<ExamParticipantContext?>,
    val diagnosticEvents: MutableState<List<DiagnosticEvent>>,
    val lastAlarmAcknowledgeDedupKey: MutableState<String?>,
    val lastAlarmAcknowledgeAtElapsedMs: MutableLongState
)

@Composable
internal fun rememberExamRuntimeAdminUiState(
    context: Context,
    payload: ExamQrPayload
): ExamRuntimeAdminUiState {
    val securityIssueDialogTitle = rememberSaveable { mutableStateOf<String?>(null) }
    val securityIssueDialogMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val securityIssueDialogCode = rememberSaveable { mutableStateOf<String?>(null) }
    val exitOnSecurityIssueDialogDismiss = rememberSaveable { mutableStateOf(false) }
    val screenPinningBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val accessibilityBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val adbBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val clipboardBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val overlayBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val geofenceBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val fakeLocationBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val deviceTimeBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val vpnBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val appSwitchBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val rootBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val lastAppSwitchTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastAppSwitchAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastAppSwitchContext = rememberSaveable { mutableStateOf<String?>(null) }
    val appSwitchSuppressionReason = rememberSaveable {
        mutableStateOf<AppSwitchSuppressionReason?>(null)
    }
    val appSwitchSuppressedUntilElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val appSwitchLifecycleResumePending = rememberSaveable { mutableStateOf(false) }
    val appSwitchFallbackArmedLogged = rememberSaveable { mutableStateOf(false) }
    val screenPinningAvailable = rememberSaveable {
        mutableStateOf(ScreenPinningPlatformBridge.isAvailable())
    }
    val screenPinningEnabledInSystem = rememberSaveable {
        mutableStateOf(ScreenPinningPlatformBridge.readSystemSetting(context))
    }
    val lockTaskStateBeforePinningRequest = rememberSaveable { mutableStateOf("Unknown") }
    val lockTaskStateAfterPinningRequest = rememberSaveable { mutableStateOf("Unknown") }
    val screenPinningRequestOutcome = rememberSaveable { mutableStateOf("Belum diminta") }
    val screenPinningDialogLikelyShown = rememberSaveable { mutableStateOf(false) }
    val screenPinningUserActionInference = rememberSaveable { mutableStateOf("Belum ada") }
    val screenPinningActivationDurationMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val examSessionCancelledByPinningFailure = rememberSaveable { mutableStateOf(false) }
    val sendingSection = rememberSaveable { mutableStateOf<DiagnosticSection?>(null) }
    val pendingSection = rememberSaveable { mutableStateOf<DiagnosticSection?>(null) }
    val bugReportFeedbackTitle = rememberSaveable { mutableStateOf<String?>(null) }
    val bugReportFeedbackMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val appStartedAtElapsedMs = rememberSaveable { SystemClock.elapsedRealtime() }
    val examRuntimeMonitoringArmed = rememberSaveable { mutableStateOf(false) }
    val examSessionStartedAtElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val lastParticipantCaptureLogKey = remember(payload.examUrl, payload.examName) {
        mutableStateOf<String?>(null)
    }
    val participantContext = remember(payload.examUrl, payload.examName) {
        mutableStateOf<ExamParticipantContext?>(null)
    }
    val diagnosticEvents = rememberSaveable(stateSaver = DiagnosticEventLogSaver) {
        mutableStateOf(
            listOf(
                DiagnosticEvent(
                    timestamp = diagnosticTimestamp(),
                    level = DiagnosticEventLevel.INFO.name,
                    code = "APP_OPENED",
                    screen = "preparation",
                    appElapsedMs = 0L,
                    sessionElapsedMs = null,
                    details = "Aplikasi dibuka"
                )
            )
        )
    }
    val lastAlarmAcknowledgeDedupKey = rememberSaveable { mutableStateOf<String?>(null) }
    val lastAlarmAcknowledgeAtElapsedMs = rememberSaveable { mutableLongStateOf(0L) }
    return ExamRuntimeAdminUiState(
        securityIssueDialogTitle = securityIssueDialogTitle,
        securityIssueDialogMessage = securityIssueDialogMessage,
        securityIssueDialogCode = securityIssueDialogCode,
        exitOnSecurityIssueDialogDismiss = exitOnSecurityIssueDialogDismiss,
        screenPinningBypassTamperLogged = screenPinningBypassTamperLogged,
        accessibilityBypassTamperLogged = accessibilityBypassTamperLogged,
        adbBypassTamperLogged = adbBypassTamperLogged,
        clipboardBypassTamperLogged = clipboardBypassTamperLogged,
        overlayBypassTamperLogged = overlayBypassTamperLogged,
        geofenceBypassTamperLogged = geofenceBypassTamperLogged,
        fakeLocationBypassTamperLogged = fakeLocationBypassTamperLogged,
        deviceTimeBypassTamperLogged = deviceTimeBypassTamperLogged,
        vpnBypassTamperLogged = vpnBypassTamperLogged,
        appSwitchBypassTamperLogged = appSwitchBypassTamperLogged,
        rootBypassTamperLogged = rootBypassTamperLogged,
        lastAppSwitchTrigger = lastAppSwitchTrigger,
        lastAppSwitchAt = lastAppSwitchAt,
        lastAppSwitchContext = lastAppSwitchContext,
        appSwitchSuppressionReason = appSwitchSuppressionReason,
        appSwitchSuppressedUntilElapsedMs = appSwitchSuppressedUntilElapsedMs,
        appSwitchLifecycleResumePending = appSwitchLifecycleResumePending,
        appSwitchFallbackArmedLogged = appSwitchFallbackArmedLogged,
        screenPinningAvailable = screenPinningAvailable,
        screenPinningEnabledInSystem = screenPinningEnabledInSystem,
        lockTaskStateBeforePinningRequest = lockTaskStateBeforePinningRequest,
        lockTaskStateAfterPinningRequest = lockTaskStateAfterPinningRequest,
        screenPinningRequestOutcome = screenPinningRequestOutcome,
        screenPinningDialogLikelyShown = screenPinningDialogLikelyShown,
        screenPinningUserActionInference = screenPinningUserActionInference,
        screenPinningActivationDurationMs = screenPinningActivationDurationMs,
        examSessionCancelledByPinningFailure = examSessionCancelledByPinningFailure,
        sendingSection = sendingSection,
        pendingSection = pendingSection,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        appStartedAtElapsedMs = appStartedAtElapsedMs,
        examRuntimeMonitoringArmed = examRuntimeMonitoringArmed,
        examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
        lastParticipantCaptureLogKey = lastParticipantCaptureLogKey,
        participantContext = participantContext,
        diagnosticEvents = diagnosticEvents,
        lastAlarmAcknowledgeDedupKey = lastAlarmAcknowledgeDedupKey,
        lastAlarmAcknowledgeAtElapsedMs = lastAlarmAcknowledgeAtElapsedMs
    )
}
