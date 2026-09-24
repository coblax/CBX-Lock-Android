package com.coblax.examlock.ui.exam

import android.net.http.SslError
import android.os.Build
import android.os.SystemClock
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

internal fun SecureExamWebView.createExamWebViewClient(
    onWebViewLoadStart: (WebView?, String?) -> Unit,
    onWebViewLoadFinish: (WebView?, String?) -> Unit,
    onWebViewLoadError: (WebView?, String) -> Unit,
    onWebViewHttpError: (WebView?, Int?) -> Unit,
    onWebViewRenderProcessGone: (SecureExamWebView?, Boolean, Int?) -> Boolean,
    onLoadingProgressChange: (WebView?, Float) -> Unit
): WebViewClient {
    val loadingTimeoutMs = 60_000L
    var pageLoadStartedAtElapsedMs = 0L

    return object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            // Sub-resources and frames keep the previous pass-through behaviour; only a
            // main-frame navigation can take the student out of the exam.
            if (request?.isForMainFrame != true) {
                return false
            }
            val scheme = request.url?.scheme
            if (isNavigableExamScheme(scheme)) {
                return false
            }
            // A deep-link scheme is a request to leave the exam for another app. Letting
            // it through produced ERR_UNKNOWN_URL_SCHEME at best, and on WebView builds
            // that resolve these it would hand the session to Play Store, the dialer or
            // a chat app while the exam is supposed to be locked down.
            android.util.Log.w(
                "ExamWebView",
                "Blocked non-web main-frame navigation: ${scheme.orEmpty().take(24)}"
            )
            return true
        }

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
            val heldHttpError = navigationState.takeHeldHttpError(url)
            navigationState.start(url)
            cancelPendingConnectionRetries()
            pageLoadStartedAtElapsedMs = SystemClock.elapsedRealtime()
            onWebViewLoadStart(view, url)
            cancelNavigationTimeout()
            val watchdog = Runnable {
                // Reaching here means the load made no progress at all for the whole
                // window (SecureExamWebView.onNavigationProgress restarts the timer
                // while the page is still downloading), so this really is a stall.
                // A stalled navigation may be a form submission. Never replay it automatically.
                navigationState.fail(url, recoverOnConnection = false)
                view?.stopLoading()
                onLoadingProgressChange(view, 1f)
                onWebViewLoadError(
                    view,
                    "Halaman ujian berhenti memuat selama ${loadingTimeoutMs / 1000} detik. " +
                        "Periksa koneksi internet, lalu tekan Refresh."
                )
            }
            scheduleNavigationTimeout(watchdog, loadingTimeoutMs)
            // Applied after the load-start callback, which clears the error message.
            if (heldHttpError != null) {
                failMainFrameWithHttpError(view, url, heldHttpError.statusCode)
            }
        }

        private fun failMainFrameWithHttpError(view: WebView?, url: String, statusCode: Int?) {
            navigationState.fail(url, recoverOnConnection = false)
            cancelNavigationTimeout()
            cancelPendingConnectionRetries()
            onLoadingProgressChange(view, 1f)
            onWebViewHttpError(view, statusCode)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            // WebView also calls onPageFinished after a failed main-frame load.
            // Keep its error and pending retry until a real navigation succeeds.
            if (!navigationState.isCurrentNavigation(url)) return
            cancelNavigationTimeout()
            onLoadingProgressChange(view, 1f)
            if (!navigationState.finish(url)) return
            onWebViewLoadFinish(view, url)
            cancelPendingConnectionRetries()
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
            handler?.cancel()
            // SSL errors can belong to third-party subresources too.
            // They must be rejected without replacing a working exam page.
            val sslUrl = error?.url
            if (!navigationState.isCurrentNavigation(sslUrl)) return
            navigationState.fail(sslUrl, recoverOnConnection = false)
            cancelNavigationTimeout()
            cancelPendingConnectionRetries()
            onLoadingProgressChange(view, 1f)
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
            onWebViewLoadError(
                view,
                "$userFriendlyMessage (SSL: $errorType | ${sslUrl.orEmpty().take(60)})"
            )
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (request?.isForMainFrame == true) {
                val errorDesc = error?.description?.toString()
                    ?: "Halaman ujian gagal dimuat."
                val failedUrl = request.url.toString()
                if (!navigationState.isCurrentNavigation(failedUrl)) return
                cancelNavigationTimeout()
                val recoverable = request.method.equals("GET", ignoreCase = true) &&
                    isRecoverableExamConnectionError(errorDesc, error?.errorCode)
                navigationState.fail(failedUrl, recoverOnConnection = recoverable)
                onLoadingProgressChange(view, 1f)
                onWebViewLoadError(view, errorDesc)
                val retryDelay = navigationState.nextRetryDelayMillis()
                if (retryDelay != null) {
                    postConnectionRetry(retryDelay, failedUrl)
                } else {
                    cancelPendingConnectionRetries()
                }
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
                val failedUrl = request.url.toString()
                val statusCode = errorResponse?.statusCode
                if (navigationState.isLoading(failedUrl)) {
                    // The page already started (older WebView ordering): fail it now.
                    failMainFrameWithHttpError(view, failedUrl, statusCode)
                } else {
                    // Usual ordering: headers arrive before onPageStarted, which would
                    // reset a failure recorded now. Held until that page starts.
                    navigationState.holdHttpError(failedUrl, statusCode)
                }
            }
        }

        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?
        ): Boolean {
            cancelNavigationTimeout()
            cancelPendingConnectionRetries()
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
}
