package com.coblax.examlock.ui.exam

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
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
import com.coblax.examlock.ui.theme.LockBackground

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
                        var connectionRetryCount = 0
                        val maxConnectionRetries = 3
                        var loadingTimeoutWatchdog: Runnable? = null
                        var loadingTimeoutAutoRetried = false
                        val loadingTimeoutMs = 60_000L
                        var pageLoadStartedAtElapsedMs = 0L

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
                                // Skip synthetic pages: loadDataWithBaseURL (error HTML)
                                // and about:blank trigger onPageStarted, which would flash
                                // the status to "Checking" and restart the timeout watchdog
                                // even though we know these are not real navigations.
                                if (url == null || url == "about:blank" || url.startsWith("data:")) {
                                    return
                                }
                                // Reset per-navigation state so each new URL gets:
                                // - full 3 connection retries (not leftover from previous URL)
                                // - a fresh silent timeout retry opportunity
                                connectionRetryCount = 0
                                loadingTimeoutAutoRetried = false
                                pageLoadStartedAtElapsedMs = SystemClock.elapsedRealtime()
                                onWebViewLoadStart(view, url)
                                // Start the loading timeout watchdog — if onPageFinished
                                // is not called within loadingTimeoutMs, show an error.
                                loadingTimeoutWatchdog?.let { view?.removeCallbacks(it) }
                                val watchdog = Runnable {
                                    if (!loadingTimeoutAutoRetried) {
                                        // First timeout: silently retry once
                                        loadingTimeoutAutoRetried = true
                                        (view as? SecureExamWebView)?.let { secureView ->
                                            secureView.postConnectionRetry(
                                                delayMillis = 500L,
                                                retryUrl = resolveExamWebViewRetryUrl(
                                                    requestedExamUrl = secureView.requestedExamUrl,
                                                    fallbackExamUrl = payload.examUrl
                                                )
                                            )
                                        }
                                    } else {
                                        // Second timeout: show error to user
                                        onWebViewLoadError(
                                            view,
                                            "Halaman ujian tidak merespons setelah ${loadingTimeoutMs / 1000} detik. Periksa koneksi internet."
                                        )
                                    }
                                }
                                loadingTimeoutWatchdog = watchdog
                                view?.postDelayed(watchdog, loadingTimeoutMs)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                // Cancel the loading timeout watchdog — page finished loading.
                                loadingTimeoutWatchdog?.let { view?.removeCallbacks(it) }
                                loadingTimeoutWatchdog = null
                                loadingTimeoutAutoRetried = false
                                // IMPORTANT: call onWebViewLoadFinish FIRST so error state is
                                // cleared before any slow-load diagnostics. The previous order
                                // (error → finish) caused a race where the error overlay
                                // flashed briefly then disappeared on pages >15s.
                                onWebViewLoadFinish(view, url)
                                (view as? SecureExamWebView)?.cancelPendingConnectionRetries()
                                connectionRetryCount = 0
                                // Slow-load is informational only — log for admin diagnostics
                                // but do NOT trigger error overlay to students. Loading >15s
                                // is common on congested school Wi-Fi (30+ students).
                                val loadDurationMs = SystemClock.elapsedRealtime() - pageLoadStartedAtElapsedMs
                                if (loadDurationMs > 15_000L) {
                                    android.util.Log.w(
                                        "ExamWebView",
                                        "Slow page load: ${loadDurationMs / 1000}s for ${view?.url?.take(80)}"
                                    )
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                // Security: ALWAYS cancel invalid SSL — never bypass.
                                // But record the diagnostic so admins can trace the issue.
                                (view as? SecureExamWebView)?.cancelPendingConnectionRetries()
                                val errorType = when (error?.primaryError) {
                                    SslError.SSL_EXPIRED -> "SSL_EXPIRED"
                                    SslError.SSL_IDMISMATCH -> "SSL_ID_MISMATCH"
                                    SslError.SSL_NOTYETVALID -> "SSL_NOT_YET_VALID"
                                    SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
                                    SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
                                    SslError.SSL_INVALID -> "SSL_INVALID"
                                    else -> "SSL_UNKNOWN"
                                }
                                val userFriendlyMessage = when (error?.primaryError) {
                                    SslError.SSL_EXPIRED ->
                                        "Sertifikat keamanan server ujian sudah expired. Hubungi admin sekolah."
                                    SslError.SSL_IDMISMATCH ->
                                        "Nama domain tidak cocok dengan sertifikat keamanan. Pastikan URL ujian benar."
                                    SslError.SSL_NOTYETVALID ->
                                        "Sertifikat keamanan belum berlaku. Periksa tanggal/waktu perangkat."
                                    SslError.SSL_UNTRUSTED ->
                                        "Sertifikat keamanan tidak dipercaya. Jaringan mungkin memblokir koneksi aman."
                                    SslError.SSL_DATE_INVALID ->
                                        "Tanggal sertifikat tidak valid. Pastikan waktu perangkat sudah benar."
                                    else ->
                                        "Masalah keamanan koneksi ($errorType). Coba gunakan jaringan lain."
                                }
                                val sslUrl = error?.url ?: "unknown"
                                onWebViewLoadError(
                                    view,
                                    "$userFriendlyMessage (SSL: $errorType | ${sslUrl.take(60)})"
                                )
                                handler?.cancel()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    val errorDesc = error?.description?.toString()
                                        ?: "Halaman ujian gagal dimuat."
                                    // Match specific Chromium network error codes.
                                    // Previously included "FAILED" which was too broad and
                                    // matched non-network errors like ERR_BLOCKED_BY_RESPONSE,
                                    // ERR_BLOCKED_BY_CLIENT, ERR_ABORTED, ERR_CACHE_MISS, etc.
                                    // causing false "Koneksi Terputus" when internet was fine.
                                    val isConnectionError = errorDesc.contains("ERR_CONNECTION_", ignoreCase = true) ||
                                        errorDesc.contains("ERR_TIMED_OUT", ignoreCase = true) ||
                                        errorDesc.contains("ERR_NAME_NOT_RESOLVED", ignoreCase = true) ||
                                        errorDesc.contains("ERR_INTERNET_DISCONNECTED", ignoreCase = true) ||
                                        errorDesc.contains("ERR_ADDRESS_UNREACHABLE", ignoreCase = true) ||
                                        errorDesc.contains("ERR_NETWORK_CHANGED", ignoreCase = true) ||
                                        errorDesc.contains("ERR_NETWORK_IO_SUSPENDED", ignoreCase = true)

                                    if (isConnectionError && connectionRetryCount < maxConnectionRetries) {
                                        connectionRetryCount++
                                        val retryDelayMs = (2000L * (1L shl (connectionRetryCount - 1)))
                                            .coerceAtMost(8000L)
                                        onWebViewLoadError(
                                            view,
                                            "$errorDesc (retry $connectionRetryCount/$maxConnectionRetries in ${retryDelayMs / 1000}s)"
                                        )
                                        (view as? SecureExamWebView)?.let { secureView ->
                                            secureView.postConnectionRetry(
                                                delayMillis = retryDelayMs,
                                                retryUrl = resolveExamWebViewRetryUrl(
                                                    requestedExamUrl = secureView.requestedExamUrl,
                                                    fallbackExamUrl = payload.examUrl
                                                )
                                            )
                                        }
                                        return
                                    }

                                    connectionRetryCount = 0
                                    (view as? SecureExamWebView)?.cancelPendingConnectionRetries()
                                    onWebViewLoadError(view, errorDesc)
                                    val errorHtml = """
                                        <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                                        <style>
                                        *{margin:0;padding:0;box-sizing:border-box}
                                        body{background:#F6F8FC;display:flex;align-items:center;justify-content:center;
                                        min-height:100vh;font-family:sans-serif;color:#3A4A5C;text-align:center;padding:24px}
                                        .c{max-width:340px}
                                        .icon{font-size:48px;margin-bottom:16px}
                                        h1{font-size:18px;font-weight:700;margin-bottom:8px;color:#1A2332}
                                        p{font-size:14px;line-height:1.5;color:#6B7B8D;margin-bottom:6px}
                                        .hint{font-size:12px;color:#94A3B8;margin-top:12px}
                                        </style></head><body><div class="c">
                                        <div class="icon">&#9888;&#65039;</div>
                                        <h1>Koneksi Terputus</h1>
                                        <p>Tidak bisa terhubung ke server ujian setelah beberapa percobaan otomatis.</p>
                                        <p>Periksa koneksi WiFi atau data seluler Anda, lalu tekan tombol <b>Muat Ulang</b> di toolbar.</p>
                                        <p class="hint">Error: ${errorDesc.take(120)}</p>
                                        </div></body></html>
                                    """.trimIndent()
                                    view?.loadDataWithBaseURL(
                                        null,
                                        errorHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                } else {
                                    // Sub-resource failed — log for diagnostics only.
                                    // Do NOT trigger error overlay because many "critical-looking"
                                    // sub-resources fail without affecting the exam page:
                                    // - Analytics scripts (Google Analytics, Hotjar)
                                    // - Third-party fonts (Google Fonts CDN)
                                    // - Lazy-loaded chunks not yet needed
                                    // - Resources blocked by school network firewall/proxy
                                    // Showing error overlay for these confuses students into
                                    // thinking the exam is broken when it's working fine.
                                    val failedUrl = request?.url?.toString().orEmpty()
                                    val isCriticalAsset = failedUrl.endsWith(".js") ||
                                        failedUrl.endsWith(".css") ||
                                        failedUrl.contains("bundle", ignoreCase = true) ||
                                        failedUrl.contains("chunk", ignoreCase = true)
                                    if (isCriticalAsset) {
                                        val assetDesc = error?.description?.toString() ?: "unknown"
                                        android.util.Log.w(
                                            "ExamWebView",
                                            "Sub-resource failed: ${failedUrl.substringAfterLast('/').take(80)} ($assetDesc)"
                                        )
                                    }
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    val statusCode = errorResponse?.statusCode ?: 0
                                    onWebViewHttpError(view, statusCode)
                                    if (statusCode >= 500) {
                                        if (connectionRetryCount < maxConnectionRetries) {
                                            connectionRetryCount++
                                            val retryDelayMs = (3000L * (1L shl (connectionRetryCount - 1)))
                                                .coerceAtMost(12000L)
                                            (view as? SecureExamWebView)?.let { secureView ->
                                                secureView.postConnectionRetry(
                                                    delayMillis = retryDelayMs,
                                                    retryUrl = resolveExamWebViewRetryUrl(
                                                        requestedExamUrl = secureView.requestedExamUrl,
                                                        fallbackExamUrl = payload.examUrl
                                                    )
                                                )
                                            }
                                            return
                                        }
                                        connectionRetryCount = 0
                                        (view as? SecureExamWebView)?.cancelPendingConnectionRetries()
                                        val serverErrorHtml = """
                                            <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                                            <style>
                                            *{margin:0;padding:0;box-sizing:border-box}
                                            body{background:#F6F8FC;display:flex;align-items:center;justify-content:center;
                                            min-height:100vh;font-family:sans-serif;color:#3A4A5C;text-align:center;padding:24px}
                                            .c{max-width:340px}
                                            .icon{font-size:48px;margin-bottom:16px}
                                            h1{font-size:18px;font-weight:700;margin-bottom:8px;color:#1A2332}
                                            p{font-size:14px;line-height:1.5;color:#6B7B8D;margin-bottom:6px}
                                            .hint{font-size:12px;color:#94A3B8;margin-top:12px}
                                            </style></head><body><div class="c">
                                            <div class="icon">&#9881;&#65039;</div>
                                            <h1>Server Sedang Bermasalah</h1>
                                            <p>Server ujian mengalami gangguan setelah beberapa percobaan otomatis ($statusCode).</p>
                                            <p>Tekan tombol <b>Muat Ulang</b> di toolbar untuk mencoba lagi.</p>
                                            </div></body></html>
                                        """.trimIndent()
                                        view?.loadDataWithBaseURL(
                                            null,
                                            serverErrorHtml,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    }
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                (view as? SecureExamWebView)?.cancelPendingConnectionRetries()
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
