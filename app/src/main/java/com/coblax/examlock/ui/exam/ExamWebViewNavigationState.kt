package com.coblax.examlock.ui.exam

import android.webkit.WebViewClient

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

    fun prepareAutomaticRetry() {
        automaticRetryPending = true
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
        if (failed || !isCurrentNavigation(url) || !isExamWebUrl(url)) return false
        revision++
        loading = false
        retryCount = 0
        canRecoverOnConnection = false
        failedUrl = null
        return true
    }

    // A reachability probe cannot prove that a failed/loading WebView has recovered.
    // A result started before a newer navigation event is stale, even for the same URL.
    fun canApplyServerProbe(startRevision: Long): Boolean =
        revision == startRevision && !loading && !failed

    fun isCurrentNavigation(url: String?): Boolean =
        url != null && currentUrl != null &&
            url.substringBefore('#') == currentUrl?.substringBefore('#')

    fun nextRetryDelayMillis(): Long? {
        if (!canRecoverOnConnection || retryCount >= 3) return null
        return 2_000L * (1L shl retryCount++)
    }
}

internal fun isExamWebUrl(url: String?): Boolean =
    url?.startsWith("https://", ignoreCase = true) == true ||
        url?.startsWith("http://", ignoreCase = true) == true

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
