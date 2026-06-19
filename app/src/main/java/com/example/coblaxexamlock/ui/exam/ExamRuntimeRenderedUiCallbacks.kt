package com.example.coblaxexamlock.ui.exam

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.lifecycle.lifecycleScope
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumbCodes
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

internal class ExamRuntimeRenderedUiCallbacks(
    private val componentActivity: ComponentActivity,
    private val lockTaskBridge: ActivityLockTaskBridge,
    private val deviceQuirkProfile: ExamRuntimeDeviceQuirkProfile,
    private val deviceSurvivalPolicy: DeviceSurvivalPolicy,
    private val webViewCompatibilityStatus: WebViewCompatibilityStatus,
    private val runtimeDiagnosticsOps: ExamRuntimeDiagnosticsOps,
    private val runtimeMonitoringOps: ExamRuntimeMonitoringOps,
    private val webViewUiState: ExamRuntimeWebViewUiState,
    private val flowUiState: ExamRuntimeFlowUiState,
    private val securityUiState: ExamRuntimeSecurityUiState,
    private val adminUiState: ExamRuntimeAdminUiState,
    private val examServerStatusState: MutableState<ExamServerFooterStatus>,
    private val lastTrustedRuntimeChromeActionElapsedMsState: MutableState<Long?>,
    private val lastTrustedRuntimeChromeActionReasonState: MutableState<String?>,
    private val examAlarmController: ExamAlarmController,
    private val hideSystemKeyboard: () -> Unit,
    private val launchTelegramSectionReport: (DiagnosticSection) -> Unit,
    private val onExit: () -> Unit
) {
    private val examGuardArmed: Boolean
        get() = adminUiState.examRuntimeMonitoringArmed.value ||
            flowUiState.lockTaskRequestPending.value ||
            flowUiState.examSessionStarted.value

    fun onDismissGeofenceMapViewer() {
        flowUiState.showGeofenceMapViewer.value = false
    }

    fun onRefreshGeofenceMapViewer() {
        runtimeDiagnosticsOps.launchLocationSecurityManualRefresh(trigger = "geofence_map_viewer_refresh")
    }

    fun onRefreshMapViewerActionLogged() {
        runtimeDiagnosticsOps.recordAction(
            code = "GEOFENCE_QUICK_FIX_REFRESH_REQUESTED",
            details = "trigger=map_viewer",
            level = DiagnosticEventLevel.INFO
        )
    }

    fun onOverlayObscuredTouch(touchSignal: ExamOverlayTouchSignal): Boolean =
        handleExamRuntimeOverlayObscuredTouch(
            touchSignal = touchSignal,
            lockTaskRequestPending = flowUiState.lockTaskRequestPending.value,
            examSessionStarted = flowUiState.examSessionStarted.value,
            lockTaskStateLabel = lockTaskBridge.stateLabel(),
            deviceQuirkProfile = deviceQuirkProfile,
            lastTrustedRuntimeChromeActionElapsedMs = lastTrustedRuntimeChromeActionElapsedMsState.value,
            lastTrustedRuntimeChromeActionReason = lastTrustedRuntimeChromeActionReasonState.value,
            currentOverlayEventDetails = runtimeDiagnosticsOps::currentOverlayEventDetails,
            recordAction = runtimeDiagnosticsOps::recordAction,
            recordOverlayEvent = runtimeDiagnosticsOps::recordOverlayEvent,
            onBlockedOverlayTouch = {
                securityUiState.overlayViolationCount.intValue += 1
                securityUiState.showOverlayViolationDialog.value = true
                examAlarmController.start()
            }
        )

    fun onShowBuiltInExamKeyboardChange(show: Boolean) {
        flowUiState.showBuiltInExamKeyboard.value = show
    }

    fun onWebViewInstanceChange(nextWebView: SecureExamWebView?) {
        val currentWebView = webViewUiState.instance.value
        val wasMissing = currentWebView == null
        if (nextWebView != null && nextWebView !== currentWebView) {
            webViewUiState.generation.value = nextExamWebViewGeneration(webViewUiState.generation.value)
            webViewUiState.destroyedGeneration.value = null
        }
        webViewUiState.instance.value = nextWebView
        if (wasMissing && nextWebView != null) {
            runtimeDiagnosticsOps.writePreviousSessionBreadcrumb(
                code = PreviousExamSessionBreadcrumbCodes.WebViewCreated,
                details = "provider=${webViewCompatibilityStatus.packageName} | " +
                    "score=${deviceSurvivalPolicy.score.name} | generation=${webViewUiState.generation.value}"
            )
        }
    }

    fun onWebViewLoadStart(view: WebView?, url: String?) {
        if (runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback("load_start", view)) {
            return
        }
        webViewUiState.stopRequested.value = false
        handleExamRuntimeWebViewLoadStart(
            url = url,
            useBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value,
            recordAction = runtimeDiagnosticsOps::recordAction,
            setHasEditableFocus = { flowUiState.hasEditableFocus.value = it },
            setWebViewErrorMessage = { flowUiState.webViewErrorMessage.value = it },
            setLoadingProgress = { webViewUiState.loadingProgress.floatValue = it },
            setExamServerStatus = { examServerStatusState.value = it },
            setShowBuiltInExamKeyboard = { flowUiState.showBuiltInExamKeyboard.value = it }
        )
    }

    fun onWebViewLoadFinish(view: WebView?, url: String?) {
        if (runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback("load_finish", view)) {
            return
        }
        handleExamRuntimeWebViewLoadFinish(
            view = view,
            url = url,
            sideArrowControlsVisible = flowUiState.sideArrowControlsVisible.value,
            useBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value,
            nativeExamFullscreenActive = examGuardArmed || webViewUiState.fullScreenCustomView.value != null,
            recordAction = runtimeDiagnosticsOps::recordAction,
            setWebViewErrorMessage = { flowUiState.webViewErrorMessage.value = it },
            setExamServerStatus = { examServerStatusState.value = it },
            hideSystemKeyboard = hideSystemKeyboard
        )
    }

    fun onWebViewLoadError(view: WebView?, description: String) {
        if (runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback("load_error", view)) {
            return
        }
        handleExamRuntimeWebViewLoadError(
            description = description,
            recordAction = runtimeDiagnosticsOps::recordAction,
            setWebViewErrorMessage = { flowUiState.webViewErrorMessage.value = it },
            setExamServerStatus = { examServerStatusState.value = it }
        )
    }

    fun onWebViewHttpError(view: WebView?, statusCode: Int?) {
        if (runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback("http_error", view)) {
            return
        }
        handleExamRuntimeWebViewHttpError(
            statusCode = statusCode,
            recordAction = runtimeDiagnosticsOps::recordAction,
            setWebViewErrorMessage = { flowUiState.webViewErrorMessage.value = it },
            setExamServerStatus = { examServerStatusState.value = it }
        )
    }

    fun onWebViewRenderProcessGone(
        view: SecureExamWebView?,
        didCrash: Boolean,
        rendererPriorityAtExit: Int?
    ): Boolean =
        runtimeMonitoringOps.handleWebViewRendererGone(
            view = view,
            didCrash = didCrash,
            rendererPriorityAtExit = rendererPriorityAtExit
        )

    fun onLoadingProgressChange(view: WebView?, progress: Float) {
        if (runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback("progress", view)) {
            return
        }
        if (!webViewUiState.stopRequested.value) {
            webViewUiState.loadingProgress.floatValue = progress
        }
    }

    fun onWebViewErrorMessageChange(message: String?) {
        flowUiState.webViewErrorMessage.value = message
    }

    fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        if (view != null) {
            runtimeMonitoringOps.showCustomView(view, callback)
        }
    }

    fun onDismissPendingSection() {
        adminUiState.pendingSection.value = null
    }

    fun onConfirmPendingSection(section: DiagnosticSection) {
        adminUiState.pendingSection.value = null
        launchTelegramSectionReport(section)
    }

    fun onDismissScreenPinningMessage() {
        flowUiState.screenPinningMessage.value = null
    }

    fun onDismissSecurityIssueDialog() {
        val shouldExit = adminUiState.exitOnSecurityIssueDialogDismiss.value
        adminUiState.securityIssueDialogTitle.value = null
        adminUiState.securityIssueDialogMessage.value = null
        adminUiState.securityIssueDialogCode.value = null
        adminUiState.exitOnSecurityIssueDialogDismiss.value = false
        examAlarmController.stop()
        if (shouldExit) {
            val launchExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                android.util.Log.e(
                    ExamRuntimeHardeningLogTag,
                    "RenderedUiCallbacks exit on fatal dialog dismiss uncaught coroutine exception: ${throwable.javaClass.simpleName}",
                    throwable
                )
            }
            componentActivity.lifecycleScope.launch(launchExceptionHandler) {
                runtimeMonitoringOps.clearExamSessionOnExit(
                    reason = "fatal_security_dialog_dismiss",
                    waitForResult = true
                )
                runtimeDiagnosticsOps.writePreviousSessionBreadcrumb(
                    code = PreviousExamSessionBreadcrumbCodes.ExitCompleted,
                    details = "reason=fatal_security_dialog_dismiss"
                )
                onExit()
            }
        }
    }

    fun onDismissBugReportFeedback() {
        adminUiState.bugReportFeedbackTitle.value = null
        adminUiState.bugReportFeedbackMessage.value = null
    }
}
