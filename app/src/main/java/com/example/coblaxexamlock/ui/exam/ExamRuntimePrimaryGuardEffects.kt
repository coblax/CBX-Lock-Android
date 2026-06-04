package com.example.coblaxexamlock.ui.exam

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.coblaxexamlock.AppSwitchProtectionMode
import com.example.coblaxexamlock.AppSwitchSignal
import com.example.coblaxexamlock.AppSwitchSuppressionReason
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.OverlayBypassState
import com.example.coblaxexamlock.OverlaySignal

@Composable
internal fun RuntimePrimaryGuardEffects(
    mainActivity: MainActivity?,
    examGuardArmed: Boolean,
    overlayBypassState: OverlayBypassState,
    clipboardBypassState: ClipboardBypassState,
    bypassClipboard: Boolean,
    appSwitchRuntimeMonitoringActive: Boolean,
    appSwitchProtectionMode: AppSwitchProtectionMode,
    appSwitchLockTaskActive: Boolean,
    accessibilityGuardFallbackActive: Boolean,
    accessibilityGuardEnabled: Boolean,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    adminUiState: ExamRuntimeAdminUiState,
    fullScreenCustomView: View?,
    showOfflineWarningDialog: Boolean,
    showExitExamDialog: Boolean,
    pendingSection: DiagnosticSection?,
    securityIssueDialogMessage: String?,
    bugReportFeedbackMessage: String?,
    deviceQuirkProfile: ExamRuntimeDeviceQuirkProfile,
    currentLastTrustedRuntimeChromeActionElapsedMs: () -> Long?,
    currentLastTrustedRuntimeChromeActionReason: () -> String?,
    currentAppSwitchSuppressionReason: () -> AppSwitchSuppressionReason?,
    currentAppSwitchEventDetails: (AppSwitchSignal, AppSwitchSuppressionReason?) -> String,
    currentOverlayEventDetails: (OverlaySignal, String?) -> String,
    currentInternalDialogReason: () -> String?,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    recordAppSwitchEvent: (String, AppSwitchSignal, DiagnosticEventLevel) -> Unit,
    onScreenPinningTransitionInterrupted: () -> Unit,
    armClipboardResumeCheck: (String) -> Unit,
    startAlarm: () -> Unit
) {
    val overlayMainHandler = remember { Handler(Looper.getMainLooper()) }
    val latestDeviceQuirkProfile by rememberUpdatedState(deviceQuirkProfile)
    val latestTrustedRuntimeChromeActionElapsedMs by rememberUpdatedState(
        currentLastTrustedRuntimeChromeActionElapsedMs
    )
    val latestTrustedRuntimeChromeActionReason by rememberUpdatedState(
        currentLastTrustedRuntimeChromeActionReason
    )

    DisposableEffect(mainActivity, examGuardArmed, overlayBypassState) {
        val hostActivity = mainActivity
        val shieldShouldBeRequested =
            hostActivity != null &&
                overlayBypassState != OverlayBypassState.Active

        securityUiState.overlayShieldRequested.value = shieldShouldBeRequested
        if (hostActivity != null) {
            val applyResult = hostActivity.setOverlayShieldMode(shieldShouldBeRequested)
            securityUiState.overlayShieldLastApplySucceeded.value = applyResult
            securityUiState.overlayShieldLastAppliedAt.value = diagnosticTimestamp()
            val eventCode = when {
                shieldShouldBeRequested && applyResult == null -> "OVERLAY_SHIELD_UNSUPPORTED"
                shieldShouldBeRequested && applyResult == false -> "OVERLAY_SHIELD_APPLY_FAILED"
                shieldShouldBeRequested -> "OVERLAY_SHIELD_APPLIED"
                else -> "OVERLAY_SHIELD_DISABLED"
            }
            recordAction(
                eventCode,
                buildString {
                    append("requested=")
                    append(if (shieldShouldBeRequested) "yes" else "no")
                    append(" | supported=")
                    append(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "yes" else "no")
                    append(" | result=")
                    append(
                        when (applyResult) {
                            null -> "unsupported"
                            true -> "success"
                            false -> "failed"
                        }
                    )
                },
                if (shieldShouldBeRequested && applyResult == false) {
                    DiagnosticEventLevel.WARNING
                } else {
                    DiagnosticEventLevel.INFO
                }
            )
        } else {
            securityUiState.overlayShieldLastApplySucceeded.value = null
            securityUiState.overlayShieldLastAppliedAt.value = null
        }

        onDispose {
            if (shieldShouldBeRequested) {
                hostActivity.setOverlayShieldMode(false)
            }
        }
    }

    DisposableEffect(
        mainActivity,
        adminUiState.examRuntimeMonitoringArmed.value,
        adminUiState.appSwitchSuppressionReason.value,
        adminUiState.appSwitchSuppressedUntilElapsedMs.value,
        appSwitchRuntimeMonitoringActive,
        appSwitchProtectionMode,
        appSwitchLockTaskActive,
        accessibilityGuardFallbackActive,
        accessibilityGuardEnabled
    ) {
        if (mainActivity == null || !appSwitchRuntimeMonitoringActive) {
            mainActivity?.setOnUserLeaveExamHandler(null)
            onDispose { mainActivity?.setOnUserLeaveExamHandler(null) }
        } else {
            mainActivity.setOnUserLeaveExamHandler {
                val suppressionReason = currentAppSwitchSuppressionReason()
                if (suppressionReason != null) {
                    recordAction(
                        "APP_SWITCH_MONITOR_SUPPRESSED",
                        currentAppSwitchEventDetails(
                            AppSwitchSignal.SuppressedInternalFlow,
                            suppressionReason
                        ),
                        DiagnosticEventLevel.INFO
                    )
                    if (suppressionReason == AppSwitchSuppressionReason.ScreenPinningRequest) {
                        onScreenPinningTransitionInterrupted()
                    }
                } else {
                    if (accessibilityGuardFallbackActive && accessibilityGuardEnabled) {
                        recordAction(
                            "APP_SWITCH_MONITOR_SUPPRESSED",
                            currentAppSwitchEventDetails(
                                AppSwitchSignal.SuppressedInternalFlow,
                                null
                            ) + " | reason=accessibility_guard_primary",
                            DiagnosticEventLevel.INFO
                        )
                        adminUiState.appSwitchLifecycleResumePending.value = false
                        return@setOnUserLeaveExamHandler
                    }
                    if (
                        examGuardArmed &&
                        !appSwitchRuntimeMonitoringActive &&
                        clipboardBypassState != ClipboardBypassState.Active &&
                        !bypassClipboard
                    ) {
                        armClipboardResumeCheck("user_leave_hint")
                    }
                    adminUiState.appSwitchLifecycleResumePending.value = false
                    recordAppSwitchEvent(
                        "APP_SWITCH_DETECTED",
                        AppSwitchSignal.UserLeaveHint,
                        DiagnosticEventLevel.SECURITY
                    )
                    securityUiState.forcedExitViolationCount.intValue += 1
                    securityUiState.pendingForcedExitViolation.value = true
                }
            }
            onDispose {
                mainActivity.setOnUserLeaveExamHandler(null)
            }
        }
    }

    DisposableEffect(
        mainActivity,
        examGuardArmed,
        overlayBypassState,
        appSwitchRuntimeMonitoringActive,
        showOfflineWarningDialog,
        securityUiState.showForcedExitAlarm.value,
        securityUiState.showKeyboardViolationDialog.value,
        securityUiState.showOverlayViolationDialog.value,
        securityUiState.showBluetoothViolationDialog.value,
        clipboardUiState.showClipboardViolationDialog.value,
        showExitExamDialog,
        pendingSection,
        securityIssueDialogMessage,
        bugReportFeedbackMessage,
        fullScreenCustomView
    ) {
        val hostActivity = mainActivity
        if (hostActivity == null || !examGuardArmed || overlayBypassState == OverlayBypassState.Active) {
            securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
            securityUiState.overlayFocusLossConfirmRunnable.value = null
            securityUiState.overlayWindowFocusLossPending.value = false
            securityUiState.overlayWindowHasFocus.value = true
            hostActivity?.setOnExamWindowFocusChangedHandler(null)
            onDispose {
                hostActivity?.setOnExamWindowFocusChangedHandler(null)
            }
        } else {
            fun currentTrustedChromeSuppression(): ExamTrustedChromeActionSuppression? {
                return resolveExamTrustedChromeActionSuppression(
                    profile = latestDeviceQuirkProfile,
                    nowElapsedMs = SystemClock.elapsedRealtime(),
                    lastTrustedActionElapsedMs = latestTrustedRuntimeChromeActionElapsedMs(),
                    lastTrustedActionReason = latestTrustedRuntimeChromeActionReason()
                )
            }

            hostActivity.setOnExamWindowFocusChangedHandler { hasFocus ->
                securityUiState.overlayWindowHasFocus.value = hasFocus
                if (hasFocus) {
                    securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
                    securityUiState.overlayFocusLossConfirmRunnable.value = null
                    securityUiState.overlayWindowFocusLossPending.value = false
                    return@setOnExamWindowFocusChangedHandler
                }

                if (!examGuardArmed || securityUiState.showOverlayViolationDialog.value) {
                    return@setOnExamWindowFocusChangedHandler
                }

                val internalDialogReason = currentInternalDialogReason()
                val suppressionReason = currentAppSwitchSuppressionReason()
                val trustedChromeSuppression = currentTrustedChromeSuppression()
                when {
                    internalDialogReason != null -> {
                        recordAction(
                            "OVERLAY_MONITOR_SUPPRESSED",
                            currentOverlayEventDetails(
                                OverlaySignal.WindowFocusLoss,
                                "reason=internal_dialog:$internalDialogReason"
                            ),
                            DiagnosticEventLevel.INFO
                        )
                    }
                    suppressionReason != null -> {
                        recordAction(
                            "OVERLAY_MONITOR_SUPPRESSED",
                            currentOverlayEventDetails(
                                OverlaySignal.WindowFocusLoss,
                                "reason=app_switch_suppression:${suppressionReason.diagnosticLabel()}"
                            ),
                            DiagnosticEventLevel.INFO
                        )
                    }
                    trustedChromeSuppression != null -> {
                        recordAction(
                            "OVERLAY_MONITOR_SUPPRESSED",
                            currentOverlayEventDetails(
                                OverlaySignal.WindowFocusLoss,
                                "reason=trusted_runtime_chrome:${trustedChromeSuppression.reason}" +
                                    " | age_ms=${trustedChromeSuppression.ageMs}" +
                                    " | samsung_legacy_tablet=${latestDeviceQuirkProfile.samsungLegacyTablet}"
                            ),
                            DiagnosticEventLevel.INFO
                        )
                    }
                    fullScreenCustomView != null -> {
                        recordAction(
                            "OVERLAY_MONITOR_SUPPRESSED",
                            currentOverlayEventDetails(
                                OverlaySignal.WindowFocusLoss,
                                "reason=fullscreen_custom_view"
                            ),
                            DiagnosticEventLevel.INFO
                        )
                    }
                    else -> {
                        securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
                        securityUiState.overlayWindowFocusLossPending.value = true
                        val confirmRunnable = Runnable {
                            securityUiState.overlayFocusLossConfirmRunnable.value = null
                            if (
                                !examGuardArmed ||
                                overlayBypassState == OverlayBypassState.Active ||
                                securityUiState.overlayWindowHasFocus.value ||
                                securityUiState.showOverlayViolationDialog.value
                            ) {
                                securityUiState.overlayWindowFocusLossPending.value = false
                                return@Runnable
                            }

                            val confirmInternalDialogReason = currentInternalDialogReason()
                            if (confirmInternalDialogReason != null) {
                                securityUiState.overlayWindowFocusLossPending.value = false
                                recordAction(
                                    "OVERLAY_MONITOR_SUPPRESSED",
                                    currentOverlayEventDetails(
                                        OverlaySignal.WindowFocusLoss,
                                        "reason=internal_dialog:$confirmInternalDialogReason"
                                    ),
                                    DiagnosticEventLevel.INFO
                                )
                                return@Runnable
                            }

                            val confirmTrustedChromeSuppression = currentTrustedChromeSuppression()
                            if (confirmTrustedChromeSuppression != null) {
                                securityUiState.overlayWindowFocusLossPending.value = false
                                recordAction(
                                    "OVERLAY_MONITOR_SUPPRESSED",
                                    currentOverlayEventDetails(
                                        OverlaySignal.WindowFocusLoss,
                                        "reason=trusted_runtime_chrome_confirm:" +
                                            confirmTrustedChromeSuppression.reason +
                                            " | age_ms=${confirmTrustedChromeSuppression.ageMs}" +
                                            " | samsung_legacy_tablet=" +
                                            latestDeviceQuirkProfile.samsungLegacyTablet
                                    ),
                                    DiagnosticEventLevel.INFO
                                )
                                return@Runnable
                            }

                            securityUiState.overlayWindowFocusLossPending.value = false
                            val hasOverlayApps = securityUiState.overlayAppsDetected.value.isNotEmpty()
                            when (
                                decideExamOverlayWindowFocusLoss(
                                    appSwitchRuntimeMonitoringActive = appSwitchRuntimeMonitoringActive,
                                    pendingForcedExitViolation =
                                        securityUiState.pendingForcedExitViolation.value,
                                    appSwitchLifecycleResumePending =
                                        adminUiState.appSwitchLifecycleResumePending.value,
                                    hasOverlayAppsDetected = hasOverlayApps
                                )
                            ) {
                                ExamOverlayFocusLossDecision.SuppressCoveredByAppSwitch -> {
                                    recordAction(
                                        "OVERLAY_MONITOR_SUPPRESSED",
                                        currentOverlayEventDetails(
                                            OverlaySignal.WindowFocusLoss,
                                            "reason=covered_by_app_switch"
                                        ),
                                        DiagnosticEventLevel.INFO
                                    )
                                }
                                ExamOverlayFocusLossDecision.WarnAndAllow -> {
                                    recordAction(
                                        "OVERLAY_MONITOR_SUPPRESSED",
                                        currentOverlayEventDetails(
                                            OverlaySignal.WindowFocusLoss,
                                            "reason=focus_loss_uncorroborated | policy=warning_only"
                                        ),
                                        DiagnosticEventLevel.WARNING
                                    )
                                }
                                ExamOverlayFocusLossDecision.TriggerViolationAlarm -> {
                                    recordAction(
                                        "OVERLAY_WINDOW_FOCUS_LOSS",
                                        currentOverlayEventDetails(
                                            OverlaySignal.WindowFocusLoss,
                                            "reason=focus_loss_corroborated_by_overlay_apps"
                                        ),
                                        DiagnosticEventLevel.SECURITY
                                    )
                                    securityUiState.overlayViolationCount.intValue += 1
                                    securityUiState.showOverlayViolationDialog.value = true
                                    startAlarm()
                                }
                            }
                        }
                        securityUiState.overlayFocusLossConfirmRunnable.value = confirmRunnable
                        overlayMainHandler.postDelayed(
                            confirmRunnable,
                            latestDeviceQuirkProfile.overlayFocusLossConfirmWindowMillis
                        )
                    }
                }
            }
            onDispose {
                securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
                securityUiState.overlayFocusLossConfirmRunnable.value = null
                securityUiState.overlayWindowFocusLossPending.value = false
                hostActivity.setOnExamWindowFocusChangedHandler(null)
            }
        }
    }
}
