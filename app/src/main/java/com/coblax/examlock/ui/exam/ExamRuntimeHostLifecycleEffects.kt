package com.coblax.examlock.ui.exam

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import com.coblax.examlock.ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED
import com.coblax.examlock.AccessibilityExamGuardStore
import com.coblax.examlock.alarmSeverityForAppSwitchViolationCount
import com.coblax.examlock.AppSwitchSignal
import com.coblax.examlock.AppSwitchSuppressionReason
import com.coblax.examlock.ClipboardBypassState
import com.coblax.examlock.ClipboardChangeDecision
import com.coblax.examlock.ClipboardSnapshot
import com.coblax.examlock.config.ClipboardResumeConfirmWindowMillis
import com.coblax.examlock.config.ClipboardResumeSettleWindowMillis
import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.format.diagnosticTimestamp
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.readClipboardSnapshotLite
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun RuntimeHostActivityLifecycleEffect(
    context: Context,
    componentActivity: ComponentActivity,
    coroutineScope: CoroutineScope,
    examAlarmController: ExamAlarmController,
    examGuardArmed: Boolean,
    geofenceEnabled: Boolean,
    clipboardBypassState: ClipboardBypassState,
    bypassClipboard: Boolean,
    appSwitchRuntimeMonitoringActive: Boolean,
    appSwitchSuppressionReason: AppSwitchSuppressionReason?,
    appSwitchSuppressedUntilElapsedMs: Long?,
    accessibilityGuardEnabledState: MutableState<Boolean>,
    accessibilityGuardFallbackActiveState: MutableState<Boolean>,
    accessibilityGuardLastReasonState: MutableState<String?>,
    accessibilityGuardLastForeignPackageState: MutableState<String?>,
    accessibilityGuardLastEventTypeState: MutableState<String?>,
    accessibilityGuardLastDetectedAtState: MutableState<String?>,
    accessibilityGuardAlarmSeverityState: MutableState<String>,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    adminUiState: ExamRuntimeAdminUiState,
    currentAppSwitchSuppressionReason: () -> AppSwitchSuppressionReason?,
    currentAppSwitchEventDetails: (AppSwitchSignal) -> String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    recordAppSwitchEvent: (String, AppSwitchSignal, DiagnosticEventLevel) -> Unit,
    armClipboardResumeCheck: (String) -> Unit,
    refreshReverseEngineeringStatus: () -> Unit,
    refreshKeyboardSecurity: (Boolean) -> Unit,
    refreshBluetoothSecurity: (Boolean) -> Unit,
    refreshDeviceIntegritySecurity: (Boolean) -> Unit,
    refreshDeviceTimeSecurity: (String) -> Unit,
    refreshGeofenceStatus: suspend (Boolean, String, Boolean) -> Unit,
    confirmClipboardViolation: (
        ClipboardSnapshot,
        ClipboardChangeDecision,
        String,
        Boolean,
        String?
    ) -> Unit,
    diagnosticTimestamp: () -> String
) {
    val clipboardMainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(
        componentActivity,
        securityUiState.pendingForcedExitViolation.value,
        examGuardArmed,
        geofenceEnabled,
        clipboardBypassState,
        bypassClipboard,
        appSwitchRuntimeMonitoringActive,
        appSwitchSuppressionReason,
        appSwitchSuppressedUntilElapsedMs,
        accessibilityGuardFallbackActiveState.value,
        accessibilityGuardEnabledState.value
    ) {
        val hostActivity = componentActivity
        val lifecycleCallbacks = object : EmptyActivityLifecycleCallbacks() {
            override fun onActivityStopped(activity: Activity) {
                if (
                    activity === hostActivity &&
                    examGuardArmed &&
                    !appSwitchRuntimeMonitoringActive &&
                    clipboardBypassState != ClipboardBypassState.Active &&
                    !bypassClipboard
                ) {
                    armClipboardResumeCheck("activity_stopped")
                }
                if (
                    activity === hostActivity &&
                    appSwitchRuntimeMonitoringActive &&
                    currentAppSwitchSuppressionReason() == null &&
                    !accessibilityGuardFallbackActiveState.value
                ) {
                    adminUiState.appSwitchLifecycleResumePending.value = true
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity !== hostActivity) {
                    return
                }

                refreshReverseEngineeringStatus()
                refreshKeyboardSecurity(true)
                refreshBluetoothSecurity(true)
                refreshDeviceIntegritySecurity(true)
                refreshDeviceTimeSecurity("activity_resumed")
                val lifecycleExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                    android.util.Log.e(
                        ExamRuntimeHardeningLogTag,
                        "HostLifecycle uncaught coroutine exception: ${throwable.javaClass.simpleName}",
                        throwable
                    )
                }
                coroutineScope.launch(lifecycleExceptionHandler) {
                    refreshGeofenceStatus(false, "activity_resumed", true)
                }

                if (accessibilityGuardFallbackActiveState.value) {
                    val guardSnapshot = AccessibilityExamGuardStore.snapshot(context)
                    accessibilityGuardEnabledState.value = guardSnapshot.enabled
                    accessibilityGuardFallbackActiveState.value =
                        guardSnapshot.fallbackActive && guardSnapshot.armed
                    accessibilityGuardLastReasonState.value = guardSnapshot.lastReason
                    accessibilityGuardLastForeignPackageState.value = guardSnapshot.lastForeignPackage
                    accessibilityGuardLastEventTypeState.value = guardSnapshot.lastEventType
                    accessibilityGuardLastDetectedAtState.value = guardSnapshot.lastDetectedAt
                    accessibilityGuardAlarmSeverityState.value = guardSnapshot.alarmSeverity.name

                    val currentViolationCount = securityUiState.forcedExitViolationCount.intValue
                    val guardViolation = guardSnapshot.toRuntimeViolationIfNewer(
                        currentViolationCount = currentViolationCount,
                        source = "activity_resumed"
                    )
                    if (guardViolation != null || !guardSnapshot.enabled) {
                        val violation = guardViolation ?: AccessibilityGuardRuntimeViolation(
                            foreignPackage = "accessibility_service_disabled",
                            eventType = "service_state_changed",
                            detectedAt = diagnosticTimestamp(),
                            violationCount = currentViolationCount + 1,
                            severity = alarmSeverityForAppSwitchViolationCount(currentViolationCount + 1),
                            reason = ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED,
                            source = "service_disabled"
                        )
                        securityUiState.forcedExitViolationCount.intValue = maxOf(
                            currentViolationCount,
                            violation.violationCount.coerceAtLeast(1)
                        )
                        securityUiState.pendingForcedExitViolation.value = true
                        securityUiState.showForcedExitAlarm.value = true
                        accessibilityGuardLastReasonState.value = violation.reason
                        accessibilityGuardLastForeignPackageState.value = violation.foreignPackage
                        accessibilityGuardLastEventTypeState.value = violation.eventType
                        accessibilityGuardLastDetectedAtState.value = violation.detectedAt
                        accessibilityGuardAlarmSeverityState.value = violation.severity.name
                        adminUiState.lastAppSwitchTrigger.value =
                            AppSwitchSignal.AccessibilityGuard.diagnosticLabel()
                        adminUiState.lastAppSwitchAt.value =
                            violation.detectedAt ?: diagnosticTimestamp()
                        val details = buildAccessibilityGuardViolationDetails(
                            currentAppSwitchEventDetails(AppSwitchSignal.AccessibilityGuard),
                            violation
                        )
                        adminUiState.lastAppSwitchContext.value = details
                        recordAction(
                            accessibilityGuardEventCodeForReason(violation.reason),
                            details,
                            DiagnosticEventLevel.SECURITY
                        )
                        recordAction(
                            "ACCESSIBILITY_GUARD_RETURN_TO_EXAM_REQUESTED",
                            "foreign_package=${violation.foreignPackage?.ifBlank { "-" } ?: "-"}",
                            DiagnosticEventLevel.INFO
                        )
                        examAlarmController.start(violation.severity)
                    }
                }

                if (
                    examGuardArmed &&
                    appSwitchRuntimeMonitoringActive &&
                    adminUiState.appSwitchLifecycleResumePending.value &&
                    !securityUiState.pendingForcedExitViolation.value &&
                    !accessibilityGuardFallbackActiveState.value
                ) {
                    recordAppSwitchEvent(
                        "APP_SWITCH_DETECTED",
                        AppSwitchSignal.LifecycleResumeFallback,
                        DiagnosticEventLevel.SECURITY
                    )
                    securityUiState.forcedExitViolationCount.intValue += 1
                    securityUiState.pendingForcedExitViolation.value = true
                }
                adminUiState.appSwitchLifecycleResumePending.value = false

                if (
                    examGuardArmed &&
                    clipboardUiState.clipboardResumeCheckPending.value &&
                    !appSwitchRuntimeMonitoringActive &&
                    clipboardBypassState != ClipboardBypassState.Active &&
                    !bypassClipboard
                ) {
                    clipboardUiState.clipboardConfirmRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.clipboardConfirmRunnable.value = null
                    clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.lastClipboardDecision.value = "resume_check_pending"
                    val preBackgroundFingerprint =
                        clipboardUiState.clipboardPreBackgroundFingerprint.value
                            ?: clipboardUiState.clipboardDecisionFingerprint.value
                    val preBackgroundSignature =
                        clipboardUiState.clipboardPreBackgroundSignature.value
                    val preBackgroundSemanticSignature =
                        clipboardUiState.clipboardPreBackgroundSemanticSignature.value
                            ?: clipboardUiState.clipboardDecisionSemanticSignature.value
                    val resumeCheckRunnable = Runnable {
                        if (
                            !examGuardArmed ||
                            clipboardBypassState == ClipboardBypassState.Active ||
                            bypassClipboard
                        ) {
                            clipboardUiState.clipboardResumeCheckPending.value = false
                            clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                            clipboardUiState.clipboardPreBackgroundSignature.value = null
                            clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                            clipboardUiState.clipboardResumeCheckRunnable.value = null
                            return@Runnable
                        }

                        val resumedSnapshot = readClipboardSnapshotLite(context)
                        if (
                            resumedSnapshot.semanticSignature == preBackgroundSemanticSignature ||
                            resumedSnapshot.decisionFingerprint == preBackgroundFingerprint
                        ) {
                            clipboardUiState.lastClipboardDecision.value =
                                if (resumedSnapshot.semanticSignature == preBackgroundSemanticSignature) {
                                    ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                                } else {
                                    ClipboardChangeDecision.IgnoredNoSubstantiveChange.diagnosticLabel()
                                }
                            clipboardUiState.clipboardDecisionFingerprint.value =
                                resumedSnapshot.decisionFingerprint
                            clipboardUiState.clipboardDecisionSemanticSignature.value =
                                resumedSnapshot.semanticSignature
                            clipboardUiState.clipboardResumeCheckPending.value = false
                            clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                            clipboardUiState.clipboardPreBackgroundSignature.value = null
                            clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                            clipboardUiState.clipboardResumeCheckRunnable.value = null
                        } else {
                            clipboardUiState.lastClipboardDecision.value = "resume_check_reconfirm_pending"
                            val firstChangedSnapshot = resumedSnapshot
                            val confirmResumedRunnable = Runnable {
                                if (
                                    !examGuardArmed ||
                                    clipboardBypassState == ClipboardBypassState.Active ||
                                    bypassClipboard
                                ) {
                                    clipboardUiState.clipboardResumeCheckPending.value = false
                                    clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                                    clipboardUiState.clipboardPreBackgroundSignature.value = null
                                    clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                                    clipboardUiState.clipboardResumeCheckRunnable.value = null
                                    return@Runnable
                                }

                                val confirmedSnapshot = readClipboardSnapshotLite(context)
                                val returnedToBaseline =
                                    confirmedSnapshot.semanticSignature == preBackgroundSemanticSignature ||
                                        confirmedSnapshot.decisionFingerprint == preBackgroundFingerprint
                                if (returnedToBaseline) {
                                    clipboardUiState.lastClipboardDecision.value =
                                        if (confirmedSnapshot.semanticSignature == preBackgroundSemanticSignature) {
                                            ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                                        } else {
                                            ClipboardChangeDecision.IgnoredReturnedToBaseline.diagnosticLabel()
                                        }
                                    clipboardUiState.clipboardDecisionFingerprint.value =
                                        confirmedSnapshot.decisionFingerprint
                                    clipboardUiState.clipboardDecisionSemanticSignature.value =
                                        confirmedSnapshot.semanticSignature
                                } else if (
                                    confirmedSnapshot.semanticSignature !=
                                        firstChangedSnapshot.semanticSignature ||
                                    confirmedSnapshot.decisionFingerprint !=
                                        firstChangedSnapshot.decisionFingerprint
                                ) {
                                    clipboardUiState.lastClipboardDecision.value =
                                        ClipboardChangeDecision.IgnoredResumeNotStable.diagnosticLabel()
                                } else {
                                    clipboardUiState.lastClipboardObservedAt.value =
                                        diagnosticTimestamp()
                                    clipboardUiState.lastClipboardObservedSignature.value =
                                        preBackgroundSignature
                                            ?: clipboardUiState.lastClipboardObservedSignature.value
                                    confirmClipboardViolation(
                                        confirmedSnapshot,
                                        ClipboardChangeDecision.ConfirmedResumeCheck,
                                        "resume_after_background",
                                        false,
                                        preBackgroundSemanticSignature
                                    )
                                }
                                clipboardUiState.clipboardResumeCheckPending.value = false
                                clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                                clipboardUiState.clipboardPreBackgroundSignature.value = null
                                clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                                clipboardUiState.clipboardResumeCheckRunnable.value = null
                            }
                            clipboardUiState.clipboardResumeCheckRunnable.value =
                                confirmResumedRunnable
                            clipboardMainHandler.postDelayed(
                                confirmResumedRunnable,
                                ClipboardResumeConfirmWindowMillis
                            )
                        }
                    }
                    clipboardUiState.clipboardResumeCheckRunnable.value = resumeCheckRunnable
                    clipboardMainHandler.postDelayed(
                        resumeCheckRunnable,
                        ClipboardResumeSettleWindowMillis
                    )
                } else if (
                    appSwitchRuntimeMonitoringActive &&
                    clipboardUiState.clipboardResumeCheckPending.value
                ) {
                    clipboardUiState.lastClipboardDecision.value =
                        ClipboardChangeDecision.IgnoredCoveredByAppSwitch.diagnosticLabel()
                    clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.clipboardResumeCheckRunnable.value = null
                    clipboardUiState.clipboardResumeCheckPending.value = false
                    clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                    clipboardUiState.clipboardPreBackgroundSignature.value = null
                    clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                } else if (
                    !examGuardArmed ||
                    clipboardBypassState == ClipboardBypassState.Active ||
                    bypassClipboard
                ) {
                    clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.clipboardResumeCheckRunnable.value = null
                    clipboardUiState.clipboardResumeCheckPending.value = false
                    clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                    clipboardUiState.clipboardPreBackgroundSignature.value = null
                    clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                }

                if (securityUiState.pendingForcedExitViolation.value) {
                    recordAppSwitchEvent(
                        "APP_SWITCH_RESUME_AFTER_LEAVE",
                        AppSwitchSignal.ResumeAfterLeave,
                        DiagnosticEventLevel.INFO
                    )
                    securityUiState.showForcedExitAlarm.value = true
                    examAlarmController.start(
                        alarmSeverityForAppSwitchViolationCount(
                            securityUiState.forcedExitViolationCount.intValue
                        )
                    )
                }
            }
        }
        hostActivity.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        onDispose {
            hostActivity.application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }
}
