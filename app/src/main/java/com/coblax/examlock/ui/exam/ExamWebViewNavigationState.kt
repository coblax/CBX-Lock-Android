package com.coblax.examlock.ui.exam

import android.webkit.WebViewClient
import java.util.Locale

/** Main-frame navigation state; an error-page finish is never a successful load. */
internal class ExamWebViewNavigationState {
    var currentUrl: String? = null
        private set
    var failedUrl: String? = null
        private set
    var canRecoverOnConnection: Boolean = false
        private set
    var revision: Long = 0L
        private set
    private var loading = false
    private var failed = false
    private var retryCount = 0
    private var automaticRetryPending = false
    private var pendingHttpError: PendingHttpError? = null

    fun prepareAutomaticRetry() {
        automaticRetryPending = true
    }

    /** True while [url] is the main-frame navigation that has started and not finished. */
    fun isLoading(url: String?): Boolean = loading && isCurrentNavigation(url)

    /**
     * WebView reports a main-frame HTTP error as soon as the response headers arrive,
     * which is before onPageStarted for that same navigation. [start] would wipe a
     * failure recorded that early, so the error is held here until the page starts.
     */
    fun holdHttpError(url: String, statusCode: Int?) {
        pendingHttpError = PendingHttpError(url, statusCode)
    }

    /**
     * Hands back the held HTTP error when it belongs to [url]. Anything held is dropped
     * either way, so an error from a navigation that never committed cannot leak into a
     * later one.
     */
    fun takeHeldHttpError(url: String?): PendingHttpError? {
        val held = pendingHttpError ?: return null
        pendingHttpError = null
        return held.takeIf { sameDocument(it.url, url) }
    }

    /** A stopped or replaced navigation never commits its held error. */
    fun dropHeldHttpError() {
        pendingHttpError = null
    }

    fun start(url: String) {
        revision++
        loading = true
        if (!automaticRetryPending) retryCount = 0
        automaticRetryPending = false
        currentUrl = url
        failed = false
        failedUrl = null
        canRecoverOnConnection = false
    }

    fun fail(url: String?, recoverOnConnection: Boolean) {
        revision++
        loading = false
        failed = true
        failedUrl = url?.takeIf(::isExamWebUrl) ?: currentUrl
        canRecoverOnConnection = recoverOnConnection
    }

    fun finish(url: String?): Boolean {
        if (!isCurrentNavigation(url)) return false
        // The navigation is over even when it does not count as an exam page load —
        // a blob:/file: attachment preview is a real main-frame load. Leaving it
        // marked as loading froze the footer on "Checking" and permanently blocked
        // the reachability probe from publishing another result.
        if (loading) revision++
        loading = false
        if (failed || !isExamWebUrl(url)) return false
        retryCount = 0
        canRecoverOnConnection = false
        failedUrl = null
        return true
    }

    // A reachability probe cannot prove that a failed/loading WebView has recovered.
    // A result started before a newer navigation event is stale, even for the same URL.
    fun canApplyServerProbe(startRevision: Long): Boolean =
        revision == startRevision && !loading && !failed

    fun isCurrentNavigation(url: String?): Boolean = sameDocument(url, currentUrl)

    fun nextRetryDelayMillis(): Long? {
        if (!canRecoverOnConnection || retryCount >= 3) return null
        return 2_000L * (1L shl retryCount++)
    }

    private fun sameDocument(first: String?, second: String?): Boolean =
        first != null && second != null &&
            first.substringBefore('#') == second.substringBefore('#')
}

internal data class PendingHttpError(val url: String, val statusCode: Int?)

internal fun isExamWebUrl(url: String?): Boolean =
    url?.startsWith("https://", ignoreCase = true) == true ||
        url?.startsWith("http://", ignoreCase = true) == true

/**
 * Schemes a main-frame navigation may use inside the exam browser. Everything else —
 * `intent:`, `android-app:`, `market:`, `tel:`, `sms:`, `mailto:`, `whatsapp:`, `tg:` —
 * exists to hand control to another app, which is the one thing a locked exam session
 * must never do. Every scheme listed here stays inside the WebView.
 */
private val NavigableExamSchemes = setOf("http", "https", "blob", "about", "data")

internal fun isNavigableExamScheme(scheme: String?): Boolean =
    scheme?.lowercase(Locale.US) in NavigableExamSchemes

internal fun isRecoverableExamConnectionError(description: String, errorCode: Int? = null): Boolean =
    // Descriptions can be localized by the WebView provider; use the stable API code too.
    errorCode in setOf(
        WebViewClient.ERROR_HOST_LOOKUP,
        WebViewClient.ERROR_CONNECT,
        WebViewClient.ERROR_IO,
        WebViewClient.ERROR_TIMEOUT
    ) || listOf(
        "ERR_CONNECTION_", "ERR_TIMED_OUT", "ERR_NAME_NOT_RESOLVED",
        "ERR_INTERNET_DISCONNECTED", "ERR_ADDRESS_UNREACHABLE",
        "ERR_NETWORK_CHANGED", "ERR_NETWORK_IO_SUSPENDED",
        // Transient errors seen on flaky/congested school Wi-Fi where the OS still
        // reports the network as connected. Without auto-retry the student hits a
        // dead-end "check your connection" page while the status footer says Online.
        // A socket dropping mid-load, a truncated response, or an HTTP/2/QUIC hiccup
        // all clear on a reload, so they are eligible for the same bounded retry.
        "ERR_SOCKET_", "ERR_EMPTY_RESPONSE", "ERR_CONTENT_LENGTH_MISMATCH",
        "ERR_INCOMPLETE_CHUNKED_ENCODING", "ERR_RESPONSE_HEADERS_TRUNCATED",
        "ERR_HTTP2_", "ERR_QUIC_"
    ).any { description.contains(it, ignoreCase = true) }
