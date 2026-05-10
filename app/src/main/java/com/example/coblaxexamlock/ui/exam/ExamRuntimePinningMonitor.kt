package com.example.coblaxexamlock.ui.exam

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.FatalSecuritySignal
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.PinningActivationState
import com.example.coblaxexamlock.ScreenPinningEnforcer
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.ScreenPinningMonitor
import com.example.coblaxexamlock.ScreenPinningSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val ScreenPinningMonitorWarmupIntervalMillis = 300L
internal const val ScreenPinningMonitorSteadyIntervalMillis = 1_000L
internal const val ScreenPinningMonitorWarmupWindowMillis = 5_000L
private const val ScreenPinningMonitorStartupGraceMillis = 12_000L

@Composable
internal fun RuntimeScreenPinningMonitorEffect(
    mainActivity: MainActivity?,
    screenPinningMode: ScreenPinningMode,
    examSessionStarted: Boolean,
    examSessionStartedAtElapsedMs: Long?,
    lockTaskRequestPending: Boolean,
    accessibilityGuardFallbackActive: Boolean,
    exitOnSecurityIssueDialogDismiss: Boolean,
    lockTaskBridge: ActivityLockTaskBridge,
    isIndonesian: Boolean,
    deviceQuirkProfile: ExamRuntimeDeviceQuirkProfile,
    currentScreenPinningMonitorIntervalMillis: () -> Long,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    applyFatalSecuritySignal: (FatalSecuritySignal) -> Unit
) {
    val latestExamSessionStarted by rememberUpdatedState(examSessionStarted)
    val latestExamSessionStartedAtElapsedMs by rememberUpdatedState(examSessionStartedAtElapsedMs)
    val latestLockTaskRequestPending by rememberUpdatedState(lockTaskRequestPending)
    val latestIsIndonesian by rememberUpdatedState(isIndonesian)
    val latestScreenPinningMode by rememberUpdatedState(screenPinningMode)
    val latestFatalSecurityExitPending by rememberUpdatedState(exitOnSecurityIssueDialogDismiss)
    val latestDeviceQuirkProfile by rememberUpdatedState(deviceQuirkProfile)
    val latestRecordAction by rememberUpdatedState(recordAction)

    LaunchedEffect(
        mainActivity,
        screenPinningMode,
        examSessionStarted,
        examSessionStartedAtElapsedMs,
        lockTaskRequestPending,
        accessibilityGuardFallbackActive,
        exitOnSecurityIssueDialogDismiss
    ) {
        if (
            mainActivity == null ||
            screenPinningMode != ScreenPinningMode.Enforced ||
            (!examSessionStarted && !lockTaskRequestPending) ||
            accessibilityGuardFallbackActive ||
            exitOnSecurityIssueDialogDismiss
        ) {
            return@LaunchedEffect
        }

        var firstInactiveDetectedAtElapsedMs: Long? = null

        while (true) {
            delay(currentScreenPinningMonitorIntervalMillis())
            if (latestFatalSecurityExitPending) {
                continue
            }
            val fatalSignal = ScreenPinningMonitor.detectViolation(
                mode = latestScreenPinningMode,
                sessionStarted = latestExamSessionStarted,
                requestPending = latestLockTaskRequestPending,
                bridge = lockTaskBridge,
                isIndonesian = latestIsIndonesian
            )
            if (fatalSignal != null) {
                val nowElapsedMs = SystemClock.elapsedRealtime()
                val sessionAgeMs = latestExamSessionStartedAtElapsedMs?.let { startedAt ->
                    (nowElapsedMs - startedAt).coerceAtLeast(0L)
                }
                val withinStartupGrace =
                    sessionAgeMs != null && sessionAgeMs <= ScreenPinningMonitorStartupGraceMillis

                if (withinStartupGrace) {
                    firstInactiveDetectedAtElapsedMs = null
                    if (latestExamSessionStarted) {
                        if (latestDeviceQuirkProfile.samsungLegacyTablet) {
                            latestRecordAction(
                                ExamRuntimeHardeningDiagnostics.ScreenPinningTransientLossRecheck,
                                "state=${lockTaskBridge.stateLabel()}" +
                                    " | model=${latestDeviceQuirkProfile.model}" +
                                    " | action=startup_grace_observe_only" +
                                    " | session_age_ms=$sessionAgeMs",
                                DiagnosticEventLevel.WARNING
                            )
                        }
                    }
                    continue
                }

                val firstInactiveAt = firstInactiveDetectedAtElapsedMs
                if (firstInactiveAt == null) {
                    firstInactiveDetectedAtElapsedMs = nowElapsedMs
                    if (latestDeviceQuirkProfile.samsungLegacyTablet) {
                        latestRecordAction(
                            ExamRuntimeHardeningDiagnostics.ScreenPinningTransientLossRecheck,
                            "state=${lockTaskBridge.stateLabel()}" +
                                " | model=${latestDeviceQuirkProfile.model}" +
                                " | action=observe_only" +
                                " | confirm_ms=" +
                                latestDeviceQuirkProfile.screenPinningLostConfirmWindowMillis,
                            DiagnosticEventLevel.WARNING
                        )
                    }
                    continue
                }
                if (
                    nowElapsedMs - firstInactiveAt <
                    latestDeviceQuirkProfile.screenPinningLostConfirmWindowMillis
                ) {
                    continue
                }
                applyFatalSecuritySignal(fatalSignal)
                break
            } else {
                firstInactiveDetectedAtElapsedMs = null
            }
        }
    }
}

@Composable
internal fun RuntimeScreenPinningActivationEffect(
    mainActivity: MainActivity?,
    lockTaskBridge: ActivityLockTaskBridge,
    isIndonesian: Boolean,
    flowUiState: ExamRuntimeFlowUiState,
    adminUiState: ExamRuntimeAdminUiState,
    coroutineScope: CoroutineScope,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    clearAppSwitchSuppression: () -> Unit,
    disarmExamRuntimeMonitoring: () -> Unit,
    resetPreparationSecurityEpisodes: () -> Unit,
    prepareCleanExamWebViewSessionForStart: suspend () -> Boolean,
    finalizeExamSessionStart: (Boolean) -> Unit
) {
    val lockTaskRequestPending = flowUiState.lockTaskRequestPending.value
    val examSessionStarted = flowUiState.examSessionStarted.value

    LaunchedEffect(lockTaskRequestPending, mainActivity) {
        if (lockTaskRequestPending) {
            if (mainActivity == null) {
                recordAction(
                    ScreenPinningSignals.eventRequestFailed(),
                    ScreenPinningSignals.unavailableActivityDetail(),
                    DiagnosticEventLevel.ERROR
                )
                flowUiState.lockTaskRequestPending.value = false
                flowUiState.pinningActivationState.value = PinningActivationState.TimeoutRetryReady
                flowUiState.pinningActivationStartedAtElapsedMs.value = null
                adminUiState.lockTaskStateAfterPinningRequest.value = "Unknown"
                adminUiState.screenPinningRequestOutcome.value = ScreenPinningSignals.failureOutcome()
                adminUiState.screenPinningUserActionInference.value = "Tidak dapat diproses"
                adminUiState.examSessionCancelledByPinningFailure.value = true
                clearAppSwitchSuppression()
                disarmExamRuntimeMonitoring()
                flowUiState.screenPinningMessage.value =
                    ScreenPinningEnforcer.unavailableActivityMessage(isIndonesian)
                return@LaunchedEffect
            }

            flowUiState.pinningActivationState.value = PinningActivationState.WaitingForLockTaskActive
            if (flowUiState.pinningActivationStartedAtElapsedMs.value == null) {
                flowUiState.pinningActivationStartedAtElapsedMs.value = SystemClock.elapsedRealtime()
            }
            recordAction(
                ExamRuntimeHardeningDiagnostics.PinningWaitStarted,
                "state_before=${lockTaskBridge.stateLabel()} | poll_ms=250 | timeout_ms=20000",
                DiagnosticEventLevel.INFO
            )
            val screenPinningReport = ScreenPinningEnforcer.requestAndAwaitActivation(
                bridge = lockTaskBridge,
                isIndonesian = isIndonesian
            )
            if (screenPinningReport.dialogLikelyShown) {
                flowUiState.pinningActivationState.value = PinningActivationState.WaitingForSystemDialog
                recordAction(
                    ExamRuntimeHardeningDiagnostics.PinningDialogExpected,
                    "not_active_after_feedback=true | attempts=${screenPinningReport.engageAttemptCount}",
                    DiagnosticEventLevel.WARNING
                )
                recordAction(
                    ScreenPinningSignals.eventPending(),
                    "Belum aktif setelah 1000ms | attempts=${screenPinningReport.engageAttemptCount} | single_request_no_retry=true",
                    DiagnosticEventLevel.WARNING
                )
                flowUiState.screenPinningMessage.value =
                    ScreenPinningEnforcer.pendingMessage(isIndonesian)
            }
            flowUiState.lockTaskRequestPending.value = false
            adminUiState.lockTaskStateAfterPinningRequest.value = screenPinningReport.afterState
            adminUiState.screenPinningDialogLikelyShown.value = screenPinningReport.dialogLikelyShown
            adminUiState.screenPinningRequestOutcome.value = screenPinningReport.outcome
            adminUiState.screenPinningUserActionInference.value = screenPinningReport.userActionInference
            adminUiState.screenPinningActivationDurationMs.value = screenPinningReport.activationDurationMs

            if (screenPinningReport.active) {
                // Guard against immediate unpin on devices where the system dialog
                // briefly drops lock task mode after confirmation (Samsung, etc.)
                delay(500)
                if (!lockTaskBridge.active()) {
                    recordAction(
                        ExamRuntimeHardeningDiagnostics.ScreenPinningTransientLossRecheck,
                        "state=${lockTaskBridge.stateLabel()} | action=post_confirm_reengage",
                        DiagnosticEventLevel.WARNING
                    )
                    lockTaskBridge.engage(allowLockTask = true)
                    delay(1_000)
                    if (!lockTaskBridge.active()) {
                        recordAction(
                            ExamRuntimeHardeningDiagnostics.ScreenPinningTransientLossRecheck,
                            "state=${lockTaskBridge.stateLabel()} | action=post_confirm_reengage_failed",
                            DiagnosticEventLevel.WARNING
                        )
                    }
                }
                flowUiState.pinningActivationState.value = PinningActivationState.ActiveConfirmed
                recordAction(
                    ScreenPinningSignals.eventActive(),
                    "Lock task state ${screenPinningReport.afterState}",
                    DiagnosticEventLevel.INFO
                )
                recordAction(
                    ExamRuntimeHardeningDiagnostics.PinningActiveConfirmed,
                    "state=${screenPinningReport.afterState} | duration_ms=${screenPinningReport.activationDurationMs} | suppressed=${flowUiState.pinningSuppressedTransitionCount.value}",
                    DiagnosticEventLevel.INFO
                )
                adminUiState.examSessionCancelledByPinningFailure.value = false
                flowUiState.pinningActivationStartedAtElapsedMs.value = null
                flowUiState.screenPinningMessage.value = null
                flowUiState.webViewErrorMessage.value = null
                adminUiState.exitOnSecurityIssueDialogDismiss.value = false
                resetPreparationSecurityEpisodes()
                coroutineScope.launch {
                    if (!prepareCleanExamWebViewSessionForStart()) {
                        adminUiState.examSessionCancelledByPinningFailure.value = true
                        lockTaskBridge.disengage()
                        disarmExamRuntimeMonitoring()
                        flowUiState.examSessionStarted.value = false
                        adminUiState.examSessionStartedAtElapsedMs.value = null
                        flowUiState.showBuiltInExamKeyboard.value = false
                        flowUiState.hasEditableFocus.value = false
                        clearAppSwitchSuppression()
                        return@launch
                    }
                    finalizeExamSessionStart(true)
                    delay(500)
                    clearAppSwitchSuppression()
                }
            } else {
                flowUiState.pinningActivationState.value = PinningActivationState.TimeoutRetryReady
                recordAction(
                    ScreenPinningSignals.eventFailed(),
                    "Timeout atau ditolak pengguna | attempts=${screenPinningReport.engageAttemptCount} | state=${screenPinningReport.afterState}",
                    DiagnosticEventLevel.WARNING
                )
                recordAction(
                    ExamRuntimeHardeningDiagnostics.PinningWaitTimeout,
                    "state=${screenPinningReport.afterState} | duration_ms=${screenPinningReport.activationDurationMs} | suppressed=${flowUiState.pinningSuppressedTransitionCount.value}",
                    DiagnosticEventLevel.WARNING
                )
                recordAction(
                    ExamRuntimeHardeningDiagnostics.PinningRetryReady,
                    "retry_ready=true | exam_started=false",
                    DiagnosticEventLevel.WARNING
                )
                adminUiState.examSessionCancelledByPinningFailure.value = true
                lockTaskBridge.disengage()
                disarmExamRuntimeMonitoring()
                flowUiState.examSessionStarted.value = false
                adminUiState.examSessionStartedAtElapsedMs.value = null
                flowUiState.showBuiltInExamKeyboard.value = false
                flowUiState.hasEditableFocus.value = false
                clearAppSwitchSuppression()
                flowUiState.pinningActivationStartedAtElapsedMs.value = null
                flowUiState.screenPinningMessage.value = ScreenPinningEnforcer.retryMessage(isIndonesian)
            }
        } else if (!examSessionStarted) {
            flowUiState.lockTaskRequestPending.value = false
            if (flowUiState.pinningActivationState.value != PinningActivationState.TimeoutRetryReady) {
                flowUiState.pinningActivationState.value = PinningActivationState.Idle
            }
            flowUiState.pinningActivationStartedAtElapsedMs.value = null
            clearAppSwitchSuppression()
            disarmExamRuntimeMonitoring()
            adminUiState.examSessionStartedAtElapsedMs.value = null
        }
    }
}
