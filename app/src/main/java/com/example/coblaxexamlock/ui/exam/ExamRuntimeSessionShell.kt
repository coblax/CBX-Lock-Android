package com.example.coblaxexamlock.ui.exam

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.example.coblaxexamlock.applyExamWebViewSettings
import com.example.coblaxexamlock.attachExamKeyboardBridge
import com.example.coblaxexamlock.attachExamNativeFullscreenBridge
import com.example.coblaxexamlock.attachExamParticipantCaptureBridge
import com.example.coblaxexamlock.config.ExamNativeFullscreenBridgeInstallScript
import com.example.coblaxexamlock.config.InstallExamKeyboardScript
import com.example.coblaxexamlock.config.InstallExamSideArrowControlsScript
import com.example.coblaxexamlock.config.RemoveExamSideArrowControlsScript
import com.example.coblaxexamlock.ExamParticipantCaptureBridge
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.format.buildExamNativeFullscreenStateSyncScript
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.installExamNativeFullscreenDocumentStartScriptIfSupported
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.effectiveExamUserAgent
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsActions
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsState
import com.example.coblaxexamlock.ui.preparation.ExamPreparationScene
import com.example.coblaxexamlock.ui.preparation.PreparationScreenActions
import com.example.coblaxexamlock.ui.preparation.PreparationScreenState
import com.example.coblaxexamlock.ui.theme.LockBackground

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
                        setBackgroundColor(LockBackground.toArgb())
                        isLongClickable = false
                        isHapticFeedbackEnabled = false
                        setOnLongClickListener { true }
                        attachExamParticipantCaptureBridge(participantCaptureBridge)
                        attachExamNativeFullscreenBridge(nativeFullscreenBridge)
                        installExamNativeFullscreenDocumentStartScriptIfSupported()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            runCatching {
                                setRendererPriorityPolicy(
                                    WebView.RENDERER_PRIORITY_IMPORTANT,
                                    true
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
                        applyExamWebViewSettings(effectiveExamUserAgent)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
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
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                onWebViewLoadStart(view, url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                onWebViewLoadFinish(view, url)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    onWebViewLoadError(
                                        view,
                                        error?.description?.toString() ?: "Halaman ujian gagal dimuat."
                                    )
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    onWebViewHttpError(view, errorResponse?.statusCode)
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                val didCrash =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && detail != null) {
                                        detail.didCrash()
                                    } else {
                                        false
                                    }
                                val rendererPriorityAtExit =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && detail != null) {
                                        detail.rendererPriorityAtExit()
                                    } else {
                                        null
                                    }
                                return onWebViewRenderProcessGone(
                                    view as? SecureExamWebView,
                                    didCrash,
                                    rendererPriorityAtExit
                                )
                            }
                        }
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
                    onWebViewErrorMessageChange(null)
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
    onDismissBugReportFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LockBackground)
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
                bugReportFeedbackTitle = bugReportFeedbackTitle,
                bugReportFeedbackMessage = bugReportFeedbackMessage,
                onDismissPendingSection = onDismissPendingSection,
                onConfirmPendingSection = onConfirmPendingSection,
                onDismissScreenPinningMessage = onDismissScreenPinningMessage,
                onDismissSecurityIssueDialog = onDismissSecurityIssueDialog,
                onDismissBugReportFeedback = onDismissBugReportFeedback
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
