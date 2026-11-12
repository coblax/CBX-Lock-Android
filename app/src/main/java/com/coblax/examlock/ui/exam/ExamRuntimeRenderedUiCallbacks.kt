package com.coblax.examlock.ui.exam

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.lifecycle.lifecycleScope
import com.coblax.examlock.ActivityLockTaskBridge
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.PreviousExamSessionBreadcrumbCodes
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.model.DiagnosticEventLevel
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.persistence.ExamStallLogEntry
import com.coblax.examlock.persistence.ExamStallLogKind
import com.coblax.examlock.persistence.ExamStallLogStore
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
            currentOverlayEventDetails = { signal, extraContext -> runtimeDiagnosticsOps.currentOverlayEventDetails(signal, extraContext) },
            recordAction = { code, details, level -> runtimeDiagnosticsOps.recordAction(code, details, level) },
            recordOverlayEvent = { code, signal, level, extraContext -> runtimeDiagnosticsOps.recordOverlayEvent(code, signal, level, extraContext) },
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
        // A new attempt: the card that showed the last stall's log is gone.
        flowUiState.lastStallReport.value = null
        handleExamRuntimeWebViewLoadStart(
            url = url,
            useBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value,
            recordAction = { code, details, level -> runtimeDiagnosticsOps.recordAction(code, details, level) },
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
            useBuiltInExamKeyboard = flowUiState.useBuiltInExamKeyboard.value,
            nativeExamFullscreenActive = examGuardArmed || webViewUiState.fullScreenCustomView.value != null,
            recordAction = { code, details, level -> runtimeDiagnosticsOps.recordAction(code, details, level) },
            setWebViewErrorMessage = { flowUiState.webViewErrorMessage.value = it },
            setExamServerStatus = { examServerStatusState.value = it },
            hideSystemKeyboard = hideSystemKeyboard
        )
        flowUiState.unresolvedStall.value = false
        recordRecoveryAfterRestart()
    }

    /** The first exam page that loads after a stall restart: proof the restart helped. */
    private fun recordRecoveryAfterRestart() {
        val context = componentActivity.applicationContext
        val restartedAt = ExamStallLogStore.takeRestartPending(context) ?: return
        val now = System.currentTimeMillis()
        val minutes = ((now - restartedAt) / 60_000L).coerceAtLeast(0L)
        val note = "page_loaded_after_restart | minutes_since_restart=$minutes"
        ExamStallLogStore.append(context, ExamStallLogEntry(ExamStallLogKind.Recovered, now, note = note))
        runtimeDiagnosticsOps.recordAction("EXAM_PAGE_LOADED_AFTER_RESTART", note, DiagnosticEventLevel.INFO)
    }

    fun onWebViewLoadError(view: WebView?, description: String) {
        if (runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback("load_error", view)) {
            return
        }
        handleExamRuntimeWebViewLoadError(
            description = description,
            recordAction = { code, details, level -> runtimeDiagnosticsOps.recordAction(code, details, level) },
            setWebViewErrorMessage = { flowUiState.webViewErrorMessage.value = it },
            setExamServerStatus = { examServerStatusState.value = it }
        )
        if (description.contains(ExamStallDetailMarker)) {
            flowUiState.unresolvedStall.value = true
            recordExamStallEvidence(view as? SecureExamWebView)
        }
    }

    /**
     * What hung: the page's unfinished scripts and styles by host, and how the phone
     * resolves those hosts right now. The full line goes to the diagnostic log and the
     * stall history (admin only); the stall card shows a summary without the exam host.
     */
    private fun recordExamStallEvidence(view: SecureExamWebView?) {
        val webView = view ?: return
        val attempt = webView.navigationState.consecutiveStalls
        val pageHost = stallLookupHost(webView.navigationState.failedUrl ?: webView.url)
        // Read by the client just before it stopped the page; null if the page never answered.
        val page = parseExamStallProbe(webView.lastStallProbe)
        val stalledAt = System.currentTimeMillis()
        componentActivity.lifecycleScope.launch {
            val hosts = (listOfNotNull(pageHost) + page.pendingHosts).distinct()
            val network = runCatching {
                collectExamStallNetworkEvidence(componentActivity.applicationContext, hosts)
            }.getOrDefault(ExamStallNetworkEvidence())
            val report = ExamStallReport(
                attempt = attempt,
                wallClockMs = stalledAt,
                examHost = pageHost,
                page = page,
                network = network
            )
            // Only while this stall's card is still up: a retry may have started meanwhile.
            if (webView.navigationState.consecutiveStalls == attempt && flowUiState.webViewErrorMessage.value != null) {
                flowUiState.lastStallReport.value = report
            }
            runtimeDiagnosticsOps.recordAction("EXAM_PAGE_STALLED", report.adminDetail, DiagnosticEventLevel.WARNING)
            ExamStallLogStore.append(
                componentActivity.applicationContext,
                ExamStallLogEntry(ExamStallLogKind.Stall, stalledAt, report = report)
            )
        }
    }

    fun onWebViewHttpError(view: WebView?, statusCode: Int?) {
        if (runtimeMonitoringOps.shouldIgnoreStaleWebViewCallback("http_error", view)) {
            return
        }
        handleExamRuntimeWebViewHttpError(
            statusCode = statusCode,
            recordAction = { code, details, level -> runtimeDiagnosticsOps.recordAction(code, details, level) },
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
