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
import com.coblax.examlock.readWebViewCompatibilityStatus

/** First look for a blank page, then a later one for pages whose failures arrive slowly. */
private const val BlankPageFirstCheckMillis = 6_000L
private const val BlankPageSecondCheckMillis = 14_000L

/**
 * "blank" when the page shows nothing a student could read or touch. ES5 on purpose: it
 * must still run on the outdated WebViews whose script errors leave pages blank.
 */
private const val BlankPageProbeScript = """
(function() {
  var body = document.body;
  if (!body) return 'blank';
  if ((body.innerText || '').replace(/\s+/g, '').length > 0) return 'content';
  if (body.querySelector('img,svg,canvas,video,iframe,frame,embed,object,input,textarea,select,button')) {
    return 'content';
  }
  var all = body.getElementsByTagName('*');
  for (var i = 0; i < all.length; i++) {
    if (all[i].shadowRoot) return 'content';
  }
  return 'blank';
})()
"""

internal fun SecureExamWebView.createExamWebViewClient(
    onWebViewLoadStart: (WebView?, String?) -> Unit,
    onWebViewLoadFinish: (WebView?, String?) -> Unit,
    onWebViewLoadError: (WebView?, String) -> Unit,
    onWebViewHttpError: (WebView?, Int?) -> Unit,
    onWebViewRenderProcessGone: (SecureExamWebView?, Boolean, Int?) -> Boolean,
    onLoadingProgressChange: (WebView?, Float) -> Unit,
    blankPageFirstCheckMillis: Long = BlankPageFirstCheckMillis,
    blankPageSecondCheckMillis: Long = BlankPageSecondCheckMillis
): WebViewClient {
    val loadingTimeoutMs = 60_000L
    var pageLoadStartedAtElapsedMs = 0L
    val examWebView = this

    fun armStallWatchdog(view: WebView?, url: String) {
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
    }
    onMainFrameRequested = { url -> armStallWatchdog(examWebView, url) }

    fun failMainFrameWithCertificate(view: WebView?, url: String?, message: String) {
        navigationState.fail(url, recoverOnConnection = false)
        cancelNavigationTimeout()
        cancelPendingConnectionRetries()
        onLoadingProgressChange(view, 1f)
        onWebViewLoadError(view, message)
    }

    fun failBlankPage(view: WebView?, url: String, breakage: ExamPageBreakage) {
        navigationState.fail(url, recoverOnConnection = false)
        onWebViewLoadError(view, blankPageMessage(view, breakage))
    }

    fun checkForBlankPage(view: WebView?, url: String, revision: Long, secondLook: Boolean) {
        val delayMillis = if (secondLook) blankPageSecondCheckMillis else blankPageFirstCheckMillis
        examWebView.scheduleBlankPageCheck(Runnable {
            if (navigationState.revision != revision) return@Runnable
            view?.evaluateExamJavascriptSafely(BlankPageProbeScript) { result ->
                if (navigationState.revision != revision || result != "\"blank\"") {
                    return@evaluateExamJavascriptSafely
                }
                // Only a blank page with a reason to be blank is reported: a page
                // still drawing its first screen must never get an error over it.
                val breakage = navigationState.pageBreakage
                when {
                    breakage.any -> failBlankPage(view, url, breakage)
                    !secondLook -> checkForBlankPage(view, url, revision, secondLook = true)
                    else -> android.util.Log.w("ExamWebView", "Blank exam page without a visible cause")
                }
            }
        }, delayMillis)
    }

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
                // Links and redirects: a certificate rejected on the way there has to be
                // recognised as this main frame, before WebView ever reports it started.
                // No early watchdog here: a link may be a download or a 204, which never
                // starts a page and would raise a stall error over a working exam.
                navigationState.request(request.url.toString())
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
            cancelBlankPageCheck()
            pageLoadStartedAtElapsedMs = SystemClock.elapsedRealtime()
            onWebViewLoadStart(view, url)
            armStallWatchdog(view, url)
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
            if (!navigationState.isCurrentNavigation(url)) {
                // WebView finishes a main frame whose certificate it rejected without
                // ever starting it. That is the only sign left of a rejection that
                // matched no main-frame request we knew about.
                navigationState.takeCertificateRejection(url)?.let { rejection ->
                    failMainFrameWithCertificate(view, rejection.url, rejection.message)
                }
                return
            }
            // WebView also calls onPageFinished after a failed main-frame load.
            // Keep its error and pending retry until a real navigation succeeds.
            cancelNavigationTimeout()
            onLoadingProgressChange(view, 1f)
            if (!navigationState.finish(url)) return
            onWebViewLoadFinish(view, url)
            cancelPendingConnectionRetries()
            checkForBlankPage(view, url!!, navigationState.revision, secondLook = false)
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
            val sslUrl = error?.url
            val host = examHostOf(sslUrl)
            if (
                host != null &&
                (navigationState.isMainFrameNavigation(sslUrl) || navigationState.isInsecureCertificateAccepted(host))
            ) {
                // The school's choice: an exam whose server is online goes ahead even with
                // an expired, self-signed or mismatched certificate. Anyone on the same
                // network could read or alter that traffic, so the student sees a warning
                // under the header and the admin's diagnostics record why.
                navigationState.acceptInsecureCertificate(host, sslErrorType(error))
                handler?.proceed()
                return
            }
            handler?.cancel()
            // A third-party sub-resource is rejected without replacing a working exam
            // page; it is still evidence if the page then renders blank. It may also be
            // a main frame nobody announced (a form post), which its finish reveals.
            val message = certificateRejectionMessage(error)
            navigationState.noteBrokenSubresource("ssl ${sslUrl.orEmpty().take(60)}")
            if (sslUrl != null) navigationState.holdCertificateRejection(sslUrl, message)
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
                if (!navigationState.isMainFrameNavigation(failedUrl)) return
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
                // Sub-resource failed — never an error overlay on its own: analytics,
                // third-party fonts, lazy chunks not yet needed and resources blocked by
                // a school firewall all fail without affecting the exam page. A failed
                // script or style is kept as evidence in case the page renders blank.
                val failedUrl = request?.url?.toString().orEmpty()
                if (isPageCriticalResource(failedUrl)) {
                    val assetDesc = error?.description?.toString() ?: "unknown"
                    navigationState.noteBrokenSubresource(
                        "${failedUrl.substringAfterLast('/').take(60)} ($assetDesc)"
                    )
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
            val failedUrl = request?.url?.toString() ?: return
            val statusCode = errorResponse?.statusCode
            if (request.isForMainFrame) {
                if (navigationState.isLoading(failedUrl)) {
                    // The page already started (older WebView ordering): fail it now.
                    failMainFrameWithHttpError(view, failedUrl, statusCode)
                } else {
                    // Usual ordering: headers arrive before onPageStarted, which would
                    // reset a failure recorded now. Held until that page starts.
                    navigationState.holdHttpError(failedUrl, statusCode)
                }
            } else if (isPageCriticalResource(failedUrl)) {
                // A script the cached page still names but the server no longer has.
                navigationState.noteBrokenSubresource(
                    "${failedUrl.substringAfterLast('/').take(60)} (HTTP ${statusCode ?: "-"})"
                )
            }
        }

        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?
        ): Boolean {
            cancelNavigationTimeout()
            cancelPendingConnectionRetries()
            cancelBlankPageCheck()
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

/** What the student reads, followed by the technical detail kept for diagnostics. */
internal fun sslErrorType(error: SslError?): String = when (error?.primaryError) {
    SslError.SSL_EXPIRED -> "SSL_EXPIRED"
    SslError.SSL_IDMISMATCH -> "SSL_ID_MISMATCH"
    SslError.SSL_NOTYETVALID -> "SSL_NOT_YET_VALID"
    SslError.SSL_UNTRUSTED -> "SSL_UNTRUSTED"
    SslError.SSL_DATE_INVALID -> "SSL_DATE_INVALID"
    SslError.SSL_INVALID -> "SSL_INVALID"
    else -> "SSL_UNKNOWN"
}

internal fun certificateRejectionMessage(error: SslError?): String {
    val errorType = sslErrorType(error)
    val userFriendlyMessage = when (error?.primaryError) {
        SslError.SSL_EXPIRED ->
            "Sertifikat keamanan server ujian sudah kedaluwarsa. Hubungi admin/pengawas ujian."
        SslError.SSL_IDMISMATCH ->
            "Sertifikat keamanan server ujian tidak cocok dengan alamatnya. Pastikan URL ujian benar, lalu hubungi admin."
        SslError.SSL_NOTYETVALID,
        SslError.SSL_DATE_INVALID ->
            "Sertifikat keamanan server ujian tidak berlaku menurut jam HP. Pastikan tanggal & waktu HP otomatis dan benar, lalu tekan Muat ulang."
        SslError.SSL_UNTRUSTED ->
            "Sertifikat keamanan server ujian tidak dipercaya HP ini. Jaringan mungkin menyadap koneksi aman, atau sertifikat server tidak lengkap. Hubungi admin/pengawas."
        else ->
            "Koneksi aman ke server ujian gagal ($errorType). Coba jaringan lain atau hubungi admin."
    }
    return "$userFriendlyMessage (SSL: $errorType | ${error?.url.orEmpty().take(60)})"
}

private fun blankPageMessage(view: WebView?, breakage: ExamPageBreakage): String {
    val webView = view?.context?.let { context ->
        runCatching { readWebViewCompatibilityStatus(context) }.getOrNull()
    }
    val studentMessage = when {
        breakage.scriptErrors > 0 && webView?.outdatedLikely == true ->
            "Halaman ujian tidak bisa tampil karena ${webView.displayLabel} di HP ini terlalu lama. " +
                "Perbarui Android System WebView dan Chrome di Play Store, lalu buka ujian lagi."
        breakage.failedResources > 0 ->
            "Halaman ujian terbuka, tetapi file pentingnya gagal dimuat sehingga layar kosong. " +
                "Tekan Muat ulang. Jika tetap kosong, jaringan mungkin memblokir sebagian situs ujian; hubungi pengawas."
        else ->
            "Halaman ujian terbuka tetapi gagal dijalankan sehingga layar kosong. " +
                "Tekan Muat ulang. Jika tetap kosong, hubungi admin/pengawas."
    }
    return "$studentMessage (BLANK: resources=${breakage.failedResources}" +
        " first=${breakage.firstFailure ?: "-"}" +
        " | script_errors=${breakage.scriptErrors}" +
        " first=${breakage.firstScriptError?.take(80) ?: "-"}" +
        " | webview=${webView?.displayLabel ?: "-"})"
}
