package com.coblax.examlock.ui.exam

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.coblax.examlock.applyExamWebViewSettings
import com.coblax.examlock.attachExamKeyboardBridge
import com.coblax.examlock.attachExamNativeFullscreenBridge
import com.coblax.examlock.attachExamParticipantCaptureBridge
import com.coblax.examlock.config.ExamNativeFullscreenBridgeInstallScript
import com.coblax.examlock.config.InstallExamKeyboardScript
import com.coblax.examlock.config.InstallExamSideArrowControlsScript
import com.coblax.examlock.config.RemoveExamSideArrowControlsScript
import com.coblax.examlock.ExamParticipantCaptureBridge
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.format.buildExamNativeFullscreenStateSyncScript
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.installExamNativeFullscreenDocumentStartScriptIfSupported
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.effectiveExamUserAgent
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.dialog.ExamRuntimeDialogsActions
import com.coblax.examlock.ui.dialog.ExamRuntimeDialogsState
import com.coblax.examlock.ui.preparation.ExamPreparationScene
import com.coblax.examlock.ui.preparation.PreparationScreenActions
import com.coblax.examlock.ui.preparation.PreparationScreenState
import com.coblax.examlock.ui.theme.AppColors

@Composable
private fun ExamRuntimeSessionMainContent(
    examSessionStarted: Boolean,
    showGeofenceMapViewer: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    geofenceManualRefreshInFlight: Boolean,
    onDismissGeofenceMapViewer: () -> Unit,
    onRefreshGeofenceMapViewer: () -> Unit,
    preparationState: PreparationScreenState,
    preparationActions: PreparationScreenActions,
    runtimeChromeState: ExamRuntimeChromeState,
    runtimeChromeActions: ExamRuntimeChromeActions,
    payload: ExamQrPayload,
    bypassOverlay: Boolean,
    examAlarmController: ExamAlarmController,
    participantCaptureBridge: ExamParticipantCaptureBridge,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    keyboardBridge: ExamKeyboardBridge,
    useBuiltInExamKeyboard: Boolean,
    effectiveExamUserAgent: String,
    fullScreenContainer: FrameLayout,
    fullScreenCustomView: View?,
    nativeExamFullscreenActive: Boolean,
    onRefreshMapViewerActionLogged: () -> Unit,
    onOverlayObscuredTouch: (ExamOverlayTouchSignal) -> Boolean,
    onShowBuiltInExamKeyboardChange: (Boolean) -> Unit,
    onWebViewInstanceChange: (SecureExamWebView?) -> Unit,
    onHideSystemKeyboard: () -> Unit,
    onWebViewLoadStart: (WebView?, String?) -> Unit,
    onWebViewLoadFinish: (WebView?, String?) -> Unit,
    onWebViewLoadError: (WebView?, String) -> Unit,
    onWebViewHttpError: (WebView?, Int?) -> Unit,
    onWebViewRenderProcessGone: (SecureExamWebView?, Boolean, Int?) -> Boolean,
    onLoadingProgressChange: (WebView?, Float) -> Unit,
    onWebViewErrorMessageChange: (String?) -> Unit,
    onShowCustomView: (View?, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lowRamProfile = LocalLowRamProfile.current
    val latestBypassOverlay by rememberUpdatedState(bypassOverlay)
    val latestOnOverlayObscuredTouch by rememberUpdatedState(onOverlayObscuredTouch)

    if (!examSessionStarted) {
        ExamPreparationScene(
            showGeofenceMapViewer = showGeofenceMapViewer,
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            isRefreshingGeofence = geofenceManualRefreshInFlight,
            onDismissGeofenceMapViewer = onDismissGeofenceMapViewer,
            onRefreshGeofenceLocation = {
                onRefreshGeofenceMapViewer()
                onRefreshMapViewerActionLogged()
            },
            preparationState = preparationState,
            preparationActions = preparationActions,
            modifier = modifier
        )
        return
    }

    ExamRuntimeChrome(
        state = runtimeChromeState,
        actions = runtimeChromeActions,
        modifier = modifier,
        webViewLayer = {
            val webViewBgColor = AppColors.current.background.toArgb()
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    SecureExamWebView(
                        context = context,
                        onObscuredTouchDetected = { touchSignal ->
                            if (!latestBypassOverlay) {
                                latestOnOverlayObscuredTouch(touchSignal)
                            } else {
                                false
                            }
                        }
                    ).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = true
                        isFocusableInTouchMode = true
                        onWebViewInstanceChange(this)
                        setBackgroundColor(webViewBgColor)
                        isLongClickable = false
                        isHapticFeedbackEnabled = false
                        setOnLongClickListener { true }
                        attachExamParticipantCaptureBridge(participantCaptureBridge)
                        attachExamNativeFullscreenBridge(nativeFullscreenBridge)
                        installExamNativeFullscreenDocumentStartScriptIfSupported()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            runCatching {
                                val policy = resolveExamWebViewRendererPriorityPolicy()
                                setRendererPriorityPolicy(
                                    policy.rendererPriority,
                                    policy.waivedWhenNotVisible
                                )
                            }
                        }
                        attachExamKeyboardBridge(
                            bridge = keyboardBridge,
                            onHideSystemKeyboard = if (useBuiltInExamKeyboard) onHideSystemKeyboard else null
                        )
                        if (!useBuiltInExamKeyboard) {
                            onShowBuiltInExamKeyboardChange(false)
                            post {
                                requestFocus(View.FOCUS_DOWN)
                                requestFocus()
                            }
                        }
                        applyExamWebViewSettings(effectiveExamUserAgent, lowRamProfile)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                // Keep the navigation watchdog alive while the page is
                                // still making progress, so a slow-but-working load is
                                // not aborted as a connection failure.
                                (view as? SecureExamWebView)?.onNavigationProgress(newProgress)
                                onLoadingProgressChange(view, newProgress / 100f)
                            }
                            override fun onShowCustomView(
                                view: View?,
                                callback: CustomViewCallback?
                            ) {
                                if (view == null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                onShowCustomView(view, callback)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onShowCustomView(
                                view: View?,
                                requestedOrientation: Int,
                                callback: CustomViewCallback?
                            ) {
                                onShowCustomView(view, callback)
                            }

                            override fun onHideCustomView() {
                                onHideCustomView()
                            }

                            // Capture JavaScript errors from the exam page for diagnostics.
                            // Important: Do NOT call onWebViewLoadError or modify webViewErrorMessage
                            // here — many websites produce non-fatal console.error() calls (React dev
                            // warnings, analytics failures, CORS errors for tracking pixels, etc.)
                            // that would incorrectly trigger/clear the error overlay and confuse
                            // students during active exams. This is truly log-only.
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                if (consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                    android.util.Log.w(
                                        "ExamWebView",
                                        "JS console error: ${consoleMessage.message()?.take(200)} (line ${consoleMessage.lineNumber()})"
                                    )
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }
                        webViewClient = createExamWebViewClient(
                            onWebViewLoadStart = onWebViewLoadStart,
                            onWebViewLoadFinish = onWebViewLoadFinish,
                            onWebViewLoadError = onWebViewLoadError,
                            onWebViewHttpError = onWebViewHttpError,
                            onWebViewRenderProcessGone = onWebViewRenderProcessGone,
                            onLoadingProgressChange = onLoadingProgressChange
                        )
                        loadExamUrlSafely(payload.examUrl)
                        requestedExamUrl = payload.examUrl
                    }
                },
                update = { webView ->
                    webView.attachExamParticipantCaptureBridge(participantCaptureBridge)
                    webView.attachExamNativeFullscreenBridge(nativeFullscreenBridge)
                    webView.attachExamKeyboardBridge(
                        bridge = keyboardBridge,
                        onHideSystemKeyboard = if (useBuiltInExamKeyboard) onHideSystemKeyboard else null
                    )
                    webView.evaluateExamJavascriptSafely(InstallExamKeyboardScript)
                    webView.evaluateExamJavascriptSafely(
                        if (runtimeChromeState.showSideArrowControls) {
                            InstallExamSideArrowControlsScript
                        } else {
                            RemoveExamSideArrowControlsScript
                        }
                    )
                    if (!useBuiltInExamKeyboard) {
                        onShowBuiltInExamKeyboardChange(false)
                        webView.post {
                            webView.requestFocus(View.FOCUS_DOWN)
                            webView.requestFocus()
                        }
                    }
                    if (webView.requestedExamUrl != payload.examUrl) {
                        webView.loadExamUrlSafely(payload.examUrl)
                        webView.requestedExamUrl = payload.examUrl
                    }
                    webView.updateExamUserAgentSafely(effectiveExamUserAgent)
                    webView.evaluateExamJavascriptSafely(ExamNativeFullscreenBridgeInstallScript)
                    webView.evaluateExamJavascriptSafely(
                        buildExamNativeFullscreenStateSyncScript(nativeExamFullscreenActive)
                    )
                    // Note: do NOT clear webViewErrorMessage here — the WebViewClient
                    // callbacks (onReceivedError, onReceivedHttpError) manage the error
                    // state. Clearing it on every recomposition was racing with those
                    // callbacks and hiding errors from the user.
                }
            )
        },
        fullscreenLayer = {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { fullScreenContainer },
                update = { container ->
                    val view = fullScreenCustomView
                    if (view != null && view.parent != container) {
                        (view.parent as? ViewGroup)?.removeView(view)
                        container.removeAllViews()
                        container.addView(
                            view,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
        }
    )
}

@Composable
internal fun ExamRuntimeSessionRenderedUi(
    examSessionStarted: Boolean,
    showGeofenceMapViewer: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    geofenceManualRefreshInFlight: Boolean,
    preparationState: PreparationScreenState,
    preparationActions: PreparationScreenActions,
    runtimeChromeState: ExamRuntimeChromeState,
    runtimeChromeActions: ExamRuntimeChromeActions,
    payload: ExamQrPayload,
    bypassOverlay: Boolean,
    examAlarmController: ExamAlarmController,
    participantCaptureBridge: ExamParticipantCaptureBridge,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    keyboardBridge: ExamKeyboardBridge,
    useBuiltInExamKeyboard: Boolean,
    effectiveExamUserAgent: String,
    fullScreenContainer: FrameLayout,
    fullScreenCustomView: View?,
    nativeExamFullscreenActive: Boolean,
    runtimeDialogsState: ExamRuntimeDialogsState,
    runtimeDialogsActions: ExamRuntimeDialogsActions,
    pendingSection: DiagnosticSection?,
    uiLanguage: UiLanguage,
    screenPinningMessage: String?,
    securityIssueDialogTitle: String?,
    securityIssueDialogMessage: String?,
    securityIssueDialogCode: String?,
    startExamPreflightState: StartExamPreflightUiState,
    lockTaskRequestPending: Boolean,
    bugReportFeedbackTitle: String?,
    bugReportFeedbackMessage: String?,
    securityUiState: ExamRuntimeSecurityUiState,
    onDismissGeofenceMapViewer: () -> Unit,
    onRefreshGeofenceMapViewer: () -> Unit,
    onRefreshMapViewerActionLogged: () -> Unit,
    onOverlayObscuredTouch: (ExamOverlayTouchSignal) -> Boolean,
    onShowBuiltInExamKeyboardChange: (Boolean) -> Unit,
    onWebViewInstanceChange: (SecureExamWebView?) -> Unit,
    onHideSystemKeyboard: () -> Unit,
    onWebViewLoadStart: (WebView?, String?) -> Unit,
    onWebViewLoadFinish: (WebView?, String?) -> Unit,
    onWebViewLoadError: (WebView?, String) -> Unit,
    onWebViewHttpError: (WebView?, Int?) -> Unit,
    onWebViewRenderProcessGone: (SecureExamWebView?, Boolean, Int?) -> Boolean,
    onLoadingProgressChange: (WebView?, Float) -> Unit,
    onWebViewErrorMessageChange: (String?) -> Unit,
    onShowCustomView: (View?, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onDismissPendingSection: () -> Unit,
    onConfirmPendingSection: (DiagnosticSection) -> Unit,
    onOpenStaticSecurityAppSettings: () -> Unit,
    onOpenStaticSecurityCastSettings: () -> Unit,
    onRefreshStaticSecurityStatus: () -> Unit,
    onSendStaticSecurityReport: (DiagnosticSection) -> Unit,
    onDismissScreenPinningMessage: () -> Unit,
    onDismissSecurityIssueDialog: () -> Unit,
    onRefreshNetworkStatus: () -> Unit,
    onDismissBugReportFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.current.background)
        ) {
            ExamRuntimeSessionMainContent(
                examSessionStarted = examSessionStarted,
                showGeofenceMapViewer = showGeofenceMapViewer,
                geofenceRuntimeStatus = geofenceRuntimeStatus,
                geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
                onDismissGeofenceMapViewer = onDismissGeofenceMapViewer,
                onRefreshGeofenceMapViewer = onRefreshGeofenceMapViewer,
                preparationState = preparationState,
                preparationActions = preparationActions,
                runtimeChromeState = runtimeChromeState,
                runtimeChromeActions = runtimeChromeActions,
                payload = payload,
                bypassOverlay = bypassOverlay,
                examAlarmController = examAlarmController,
                participantCaptureBridge = participantCaptureBridge,
                nativeFullscreenBridge = nativeFullscreenBridge,
                keyboardBridge = keyboardBridge,
                useBuiltInExamKeyboard = useBuiltInExamKeyboard,
                effectiveExamUserAgent = effectiveExamUserAgent,
                fullScreenContainer = fullScreenContainer,
                fullScreenCustomView = fullScreenCustomView,
                nativeExamFullscreenActive = nativeExamFullscreenActive,
                onRefreshMapViewerActionLogged = onRefreshMapViewerActionLogged,
                onOverlayObscuredTouch = onOverlayObscuredTouch,
                onShowBuiltInExamKeyboardChange = onShowBuiltInExamKeyboardChange,
                onWebViewInstanceChange = onWebViewInstanceChange,
                onHideSystemKeyboard = onHideSystemKeyboard,
                onWebViewLoadStart = onWebViewLoadStart,
                onWebViewLoadFinish = onWebViewLoadFinish,
                onWebViewLoadError = onWebViewLoadError,
                onWebViewHttpError = onWebViewHttpError,
                onWebViewRenderProcessGone = onWebViewRenderProcessGone,
                onLoadingProgressChange = onLoadingProgressChange,
                onWebViewErrorMessageChange = onWebViewErrorMessageChange,
                onShowCustomView = onShowCustomView,
                onHideCustomView = onHideCustomView,
                modifier = Modifier.weight(1f)
            )
            ExamRuntimeDialogsCoordinator(
                pendingSection = pendingSection,
                uiLanguage = uiLanguage,
                runtimeDialogsState = runtimeDialogsState,
                runtimeDialogsActions = runtimeDialogsActions,
                screenPinningMessage = screenPinningMessage,
                securityIssueDialogTitle = securityIssueDialogTitle,
                securityIssueDialogMessage = securityIssueDialogMessage,
                securityIssueDialogCode = securityIssueDialogCode,
                startExamPreflightState = startExamPreflightState,
                isRefreshingNetwork = preparationState.isRefreshingNetwork,
                bugReportFeedbackTitle = bugReportFeedbackTitle,
                bugReportFeedbackMessage = bugReportFeedbackMessage,
                onDismissPendingSection = onDismissPendingSection,
                onConfirmPendingSection = onConfirmPendingSection,
                onDismissScreenPinningMessage = onDismissScreenPinningMessage,
                onDismissSecurityIssueDialog = onDismissSecurityIssueDialog,
                onRefreshNetworkStatus = onRefreshNetworkStatus,
                onDismissBugReportFeedback = onDismissBugReportFeedback,
                onCancelPreflight = {
                    hideStartExamPreflight(startExamPreflightState)
                },
                lockTaskRequestPending = lockTaskRequestPending
            )
            RuntimeStaticSecurityDialogsHost(
                securityUiState = securityUiState,
                onOpenAppSettings = onOpenStaticSecurityAppSettings,
                onOpenCastSettings = onOpenStaticSecurityCastSettings,
                onRefreshStatus = onRefreshStaticSecurityStatus,
                onSendReport = onSendStaticSecurityReport
            )
        }
        // Full-screen pinning overlay — shown when screen pinning is being activated.
        // zIndex ensures it covers everything including dialogs.
        if (preparationState.pinningActivationState.isPending()) {
            PinningActivationOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
            )
        }
    }
}

internal data class ExamRuntimeSessionInputs(
    val payload: ExamQrPayload,
    val adminSettings: AdminSettings,
    val pendingDirectLinkSaveLog: String?,
    val pendingRecoveryEventDetails: String?,
    val examSessionRecoveryNonce: Long,
    val deviceTimeBaselineWallClockMillis: Long,
    val deviceTimeBaselineElapsedRealtimeMillis: Long
)

internal data class ExamRuntimeSessionCallbacks(
    val onDirectLinkSaveLogConsumed: () -> Unit,
    val onRecoveryEventConsumed: () -> Unit,
    val onExamSessionStartedStateChange: (Boolean) -> Unit,
    val onExit: () -> Unit
)
