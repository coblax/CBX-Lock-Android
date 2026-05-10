package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.ScreenPinningEnforcer
import com.example.coblaxexamlock.ScreenPinningMode
import java.util.Locale

internal fun buildExamRuntimeChromeActionsForSession(
    examSessionStarted: Boolean,
    screenPinningMode: ScreenPinningMode,
    screenPinningAvailable: Boolean,
    lockTaskRequestPending: Boolean,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    isIndonesian: Boolean,
    lockTaskAlreadyActive: () -> Boolean,
    markTrustedRuntimeChromeAction: (String) -> Unit,
    clearWebViewError: () -> Unit,
    reloadWebView: () -> Unit,
    setLoadingProgress: (Float) -> Unit,
    setLastExamRefreshDecision: (String) -> Unit,
    setScreenPinningMessage: (String?) -> Unit,
    setShowExitExamDialog: (Boolean) -> Unit,
    launchExamServerProbe: (String, Boolean) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    sendBuiltInKeyboardText: (String) -> Unit,
    sendBuiltInKeyboardBackspace: () -> Unit,
    sendKeyboardArrowLeft: () -> Unit,
    sendKeyboardArrowRight: () -> Unit,
    toggleSideArrowControls: () -> Boolean,
    sendBuiltInKeyboardEnter: () -> Unit,
    toggleBuiltInKeyboardShift: () -> Unit
): ExamRuntimeChromeActions {
    return ExamRuntimeChromeActions(
        onRetryLoading = {
            markTrustedRuntimeChromeAction("webview_retry")
            clearWebViewError()
            reloadWebView()
        },
        onRefreshPage = {
            markTrustedRuntimeChromeAction("webview_refresh")
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
                clearWebViewError()
                setLoadingProgress(0.05f)
                launchExamServerProbe("manual_refresh", true)
                reloadWebView()
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
        onArrowLeft = {
            markTrustedRuntimeChromeAction("exam_arrow_left")
            sendKeyboardArrowLeft()
        },
        onArrowRight = {
            markTrustedRuntimeChromeAction("exam_arrow_right")
            sendKeyboardArrowRight()
        },
        onToggleSideArrowControls = {
            markTrustedRuntimeChromeAction("exam_arrow_toggle")
            val sideArrowControlsVisible = toggleSideArrowControls()
            recordAction(
                "EXAM_ARROW_CONTROLS_TOGGLED",
                if (sideArrowControlsVisible) "visible" else "hidden",
                DiagnosticEventLevel.INFO
            )
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
        }
    )
}
