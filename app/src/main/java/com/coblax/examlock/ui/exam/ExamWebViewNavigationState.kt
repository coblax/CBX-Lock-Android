package com.coblax.examlock.ui.exam

import android.webkit.WebViewClient
import java.net.URI
import java.util.Locale

/** Main-frame navigation state; an error-page finish is never a successful load. */
internal class ExamWebViewNavigationState {
    var currentUrl: String? = null
        private set
    var failedUrl: String? = null
        private set
    var canRecoverOnConnection: Boolean = false
        private set
    /** The HTTP status the server refused the failed page with, if that is how it failed. */
    var failedHttpStatus: Int? = null
        private set
    /**
     * The failed page is worth loading again once its server answers: a connection that
     * failed, or a GET the server could not serve at the time (5xx, a busy server).
     */
    var canReloadWhenServerRecovers: Boolean = false
        private set
    var revision: Long = 0L
        private set
    private var loading = false
    private var failed = false
    private var retryCount = 0
    private var automaticRetryPending = false
    private var pendingHttpError: PendingHttpError? = null
    private var requestedUrl: String? = null
    private var rejectedCertificate: PendingCertificateRejection? = null

    /** Why the page that has started may render blank: failed scripts or styles, script errors. */
    var pageBreakage: ExamPageBreakage = ExamPageBreakage()
        private set

    /**
     * Loads in a row that made no progress for the whole stall window; 0 once a page loads.
     * A second stall in a row is no longer a slow server: something on the phone, a DNS
     * answer or connection left over from a dropped network, is holding requests.
     */
    var consecutiveStalls: Int = 0
        private set

    fun noteStall(): Int = ++consecutiveStalls

    fun prepareAutomaticRetry() {
        automaticRetryPending = true
    }

    /**
     * A main-frame navigation to [url] was asked for. WebView only reports a navigation
     * as started once a response commits, so a certificate it rejects arrives before any
     * start and used to be dropped as if it belonged to some sub-resource.
     */
    fun request(url: String) {
        if (isExamWebUrl(url)) requestedUrl = url
    }

    /** True when [url] is the main frame, whether it has started yet or not. */
    fun isMainFrameNavigation(url: String?): Boolean =
        isCurrentNavigation(url) || sameDocument(url, requestedUrl)

    /**
     * A rejected certificate that matched no known main-frame request (a form post, a
     * script redirect). WebView finishes that main frame right after, which identifies it.
     */
    fun holdCertificateRejection(url: String, message: String) {
        rejectedCertificate = PendingCertificateRejection(url, message)
    }

    fun takeCertificateRejection(url: String?): PendingCertificateRejection? {
        val held = rejectedCertificate ?: return null
        rejectedCertificate = null
        return held.takeIf { sameDocument(it.url, url) }
    }

    private val insecureCertificateHosts = mutableMapOf<String, String>()
    private val reportedInsecureHosts = mutableSetOf<String>()

    /**
     * The exam opens despite a certificate problem on [host] — the school's choice: the
     * exam goes ahead whenever its server is online, and the student is told instead.
     */
    fun acceptInsecureCertificate(host: String, problem: String) {
        insecureCertificateHosts.putIfAbsent(host.lowercase(Locale.US), problem)
    }

    fun isInsecureCertificateAccepted(host: String?): Boolean =
        host != null && host.lowercase(Locale.US) in insecureCertificateHosts

    /** Why the page at [url] is not on a protected connection, or null when it is. */
    fun insecurityOf(url: String?): String? {
        if (url?.startsWith("http://", ignoreCase = true) == true) return "cleartext_http"
        return insecureCertificateHosts[examHostOf(url)]?.let { problem -> "certificate_$problem" }
    }

    /** [insecurityOf], once per host, for the admin's diagnostics. */
    fun takeUnreportedInsecurity(url: String?): String? {
        val insecurity = insecurityOf(url) ?: return null
        val host = examHostOf(url) ?: return null
        return insecurity.takeIf { reportedInsecureHosts.add(host) }
    }

    fun noteBrokenSubresource(description: String) {
        pageBreakage = pageBreakage.copy(
            failedResources = pageBreakage.failedResources + 1,
            firstFailure = pageBreakage.firstFailure ?: description
        )
    }

    fun noteScriptError(message: String) {
        pageBreakage = pageBreakage.copy(
            scriptErrors = pageBreakage.scriptErrors + 1,
            firstScriptError = pageBreakage.firstScriptError ?: message
        )
    }

    /** True while [url] is the main-frame navigation that has started and not finished. */
    fun isLoading(url: String?): Boolean = loading && isCurrentNavigation(url)

    /** A main-frame navigation is under way: asked for and not yet failed, or not finished. */
    val navigationInFlight: Boolean
        get() = requestedUrl != null || loading

    /**
     * WebView reports a main-frame HTTP error as soon as the response headers arrive,
     * which is before onPageStarted for that same navigation. [start] would wipe a
     * failure recorded that early, so the error is held here until the page starts.
     */
    fun holdHttpError(url: String, statusCode: Int?, reloadWhenServerRecovers: Boolean = false) {
        pendingHttpError = PendingHttpError(url, statusCode, reloadWhenServerRecovers)
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
        requestedUrl = null
        failed = false
        failedUrl = null
        failedHttpStatus = null
        canRecoverOnConnection = false
        canReloadWhenServerRecovers = false
        pageBreakage = ExamPageBreakage()
    }

    fun fail(
        url: String?,
        recoverOnConnection: Boolean,
        httpStatus: Int? = null,
        reloadWhenServerRecovers: Boolean = recoverOnConnection
    ) {
        revision++
        loading = false
        failed = true
        failedUrl = url?.takeIf(::isExamWebUrl) ?: requestedUrl ?: currentUrl
        failedHttpStatus = httpStatus
        requestedUrl = null
        canRecoverOnConnection = recoverOnConnection
        canReloadWhenServerRecovers = reloadWhenServerRecovers
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
        consecutiveStalls = 0
        canRecoverOnConnection = false
        canReloadWhenServerRecovers = false
        failedUrl = null
        failedHttpStatus = null
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
            canonicalExamPageUrl(first) == canonicalExamPageUrl(second)
}

/**
 * [url] without its fragment, written the way WebView reports it back: scheme and host in
 * lower case, no default port, "/" for a bare host, spaces escaped. Exam links are loaded
 * as the admin typed them, so "https://cbt.school.id" came back as "https://cbt.school.id/"
 * and its certificate problem was refused as some other page's.
 */
internal fun canonicalExamPageUrl(url: String): String {
    val page = url.substringBefore('#').trim().replace(" ", "%20")
    val uri = runCatching { URI(page).normalize() }.getOrNull() ?: return page
    val scheme = uri.scheme?.lowercase(Locale.US)
    if (scheme != "http" && scheme != "https" || uri.rawAuthority == null) return page
    val defaultPort = if (scheme == "https") 443 else 80
    val authority = uri.host?.let { host ->
        buildString {
            uri.rawUserInfo?.let { append(it).append('@') }
            append(host.lowercase(Locale.US))
            if (uri.port != -1 && uri.port != defaultPort) append(':').append(uri.port)
        }
    } ?: uri.rawAuthority.lowercase(Locale.US)
    return buildString {
        append(scheme).append("://").append(authority)
        append(uri.rawPath?.takeIf { it.isNotEmpty() } ?: "/")
        uri.rawQuery?.let { append('?').append(it) }
    }
}

internal data class PendingHttpError(
    val url: String,
    val statusCode: Int?,
    val reloadWhenServerRecovers: Boolean = false
)

internal data class PendingCertificateRejection(val url: String, val message: String)

internal data class ExamPageBreakage(
    val failedResources: Int = 0,
    val firstFailure: String? = null,
    val scriptErrors: Int = 0,
    val firstScriptError: String? = null
) {
    val any: Boolean
        get() = failedResources > 0 || scriptErrors > 0
}

/** Scripts and styles: without them a modern exam page renders nothing at all. */
internal fun isPageCriticalResource(url: String?): Boolean {
    val path = url.orEmpty().substringBefore('?').substringBefore('#').lowercase(Locale.US)
    return path.endsWith(".js") || path.endsWith(".mjs") || path.endsWith(".css") ||
        path.contains("bundle") || path.contains("chunk")
}

internal fun examHostOf(url: String?): String? =
    runCatching { java.net.URI(url).host }.getOrNull()
        ?.lowercase(Locale.US)
        ?.takeIf { it.isNotBlank() }

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
