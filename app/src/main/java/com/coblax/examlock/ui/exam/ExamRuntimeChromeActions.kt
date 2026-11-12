package com.coblax.examlock.ui.exam

import com.coblax.examlock.DeviceCompatibilityProfile
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.ScreenPinningEnforcer
import com.coblax.examlock.ScreenPinningMode
import java.util.Locale

internal fun buildExamRuntimeChromeActionsForSession(
    examSessionStarted: Boolean,
    screenPinningMode: ScreenPinningMode,
    screenPinningAvailable: Boolean,
    lockTaskRequestPending: Boolean,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    isIndonesian: Boolean,
    isCurrentlyLoading: () -> Boolean,
    lockTaskAlreadyActive: () -> Boolean,
    markTrustedRuntimeChromeAction: (String) -> Unit,
    clearWebViewError: () -> Unit,
    retryExamPageAfterError: () -> Unit,
    reloadExamUrlLikeBrowser: () -> Unit,
    stopWebViewLoading: () -> Unit,
    setLoadingProgress: (Float) -> Unit,
    setWebViewStopRequested: (Boolean) -> Unit,
    setLastExamRefreshDecision: (String) -> Unit,
    setScreenPinningMessage: (String?) -> Unit,
    setShowExitExamDialog: (Boolean) -> Unit,
    launchExamServerProbe: (String, Boolean) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    sendBuiltInKeyboardText: (String) -> Unit,
    sendBuiltInKeyboardBackspace: () -> Unit,
    sendBuiltInKeyboardEnter: () -> Unit,
    toggleBuiltInKeyboardShift: () -> Unit,
    requestAppRestart: () -> Unit = {}
): ExamRuntimeChromeActions {
    return ExamRuntimeChromeActions(
        onRetryLoading = {
            markTrustedRuntimeChromeAction("webview_retry")
            setWebViewStopRequested(false)
            clearWebViewError()
            setLoadingProgress(0.05f)
            launchExamServerProbe("manual_retry", true)
            retryExamPageAfterError()
        },
        onRefreshPage = {
            markTrustedRuntimeChromeAction("webview_refresh")

            // If the page is currently loading, stop it instead of refreshing again
            if (isCurrentlyLoading()) {
                setWebViewStopRequested(true)
                stopWebViewLoading()
                setLoadingProgress(1f)
                recordAction(
                    ExamRuntimeHardeningDiagnostics.ExamRefreshStoppedByUser,
                    "user_cancelled_loading=true",
                    DiagnosticEventLevel.INFO
                )
                return@ExamRuntimeChromeActions
            }

            val refreshSafetyDecision = resolveExamRefreshSafetyDecision(
                examSessionStarted = examSessionStarted,
                screenPinningEnforced = screenPinningMode == ScreenPinningMode.Enforced && screenPinningAvailable,
                lockTaskAlreadyActive = lockTaskAlreadyActive(),
                lockTaskRequestPending = lockTaskRequestPending
            )
            setLastExamRefreshDecision(
                "${refreshSafetyDecision.outcome.name.lowercase(Locale.US)} | ${refreshSafetyDecision.diagnosticDetails}"
            )
            recordAction(
                ExamRuntimeHardeningDiagnostics.ExamRefreshRequested,
                refreshSafetyDecision.diagnosticDetails,
                DiagnosticEventLevel.INFO
            )
            if (refreshSafetyDecision.eventCode != ExamRuntimeHardeningDiagnostics.ExamRefreshCompleted) {
                recordAction(
                    refreshSafetyDecision.eventCode,
                    refreshSafetyDecision.diagnosticDetails,
                    when (refreshSafetyDecision.outcome) {
                        ExamRefreshSafetyOutcome.BlockedPinningInactive,
                        ExamRefreshSafetyOutcome.BlockedPinningPending -> DiagnosticEventLevel.WARNING
                        else -> DiagnosticEventLevel.INFO
                    }
                )
            }
            if (deviceCompatibilityProfile.samsungLegacyTablet) {
                recordAction(
                    ExamRuntimeHardeningDiagnostics.PinningRefreshSafeSuppressed,
                    "trusted_chrome=webview_refresh | " +
                        "family=${deviceCompatibilityProfile.family.name} | " +
                        "overlay_suppression_ms=${deviceCompatibilityProfile.overlayChromeActionSuppressionMillis} | " +
                        "refresh_outcome=${refreshSafetyDecision.outcome.name.lowercase(Locale.US)}",
                    DiagnosticEventLevel.INFO
                )
            }
            if (refreshSafetyDecision.allowWebViewReload) {
                setWebViewStopRequested(false)
                clearWebViewError()
                setLoadingProgress(0.05f)
                launchExamServerProbe("manual_refresh", true)
                reloadExamUrlLikeBrowser()
                recordAction(
                    ExamRuntimeHardeningDiagnostics.ExamRefreshCompleted,
                    refreshSafetyDecision.diagnosticDetails,
                    DiagnosticEventLevel.INFO
                )
            } else if (refreshSafetyDecision.outcome == ExamRefreshSafetyOutcome.BlockedPinningInactive) {
                setScreenPinningMessage(ScreenPinningEnforcer.pendingMessage(isIndonesian))
            }
        },
        onGoHome = {
            markTrustedRuntimeChromeAction("exit_to_menu")
            recordAction("EXIT_TO_MENU_REQUESTED", "-", DiagnosticEventLevel.INFO)
            setShowExitExamDialog(true)
        },
        onTextKey = { value ->
            markTrustedRuntimeChromeAction("built_in_keyboard_text")
            sendBuiltInKeyboardText(value)
        },
        onBackspace = {
            markTrustedRuntimeChromeAction("built_in_keyboard_backspace")
            sendBuiltInKeyboardBackspace()
        },
        onShowProctorInfo = {
            markTrustedRuntimeChromeAction("proctor_info")
            recordAction("EXAM_PROCTOR_INFO_OPENED", "-", DiagnosticEventLevel.INFO)
        },
        onEnter = {
            markTrustedRuntimeChromeAction("built_in_keyboard_enter")
            sendBuiltInKeyboardEnter()
        },
        onSpace = {
            markTrustedRuntimeChromeAction("built_in_keyboard_space")
            sendBuiltInKeyboardText(" ")
        },
        onShiftToggle = {
            markTrustedRuntimeChromeAction("built_in_keyboard_shift")
            toggleBuiltInKeyboardShift()
        },
        onRestartApp = {
            markTrustedRuntimeChromeAction("app_restart")
            recordAction("APP_RESTART_REQUESTED", "source=exam_chrome", DiagnosticEventLevel.WARNING)
            requestAppRestart()
        }
    )
}
