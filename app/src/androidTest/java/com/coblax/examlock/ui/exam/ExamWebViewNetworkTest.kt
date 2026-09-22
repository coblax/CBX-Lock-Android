package com.coblax.examlock.ui.exam

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createComposeRule
import java.net.ServerSocket
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExamWebViewNetworkTest {
    @get:Rule val composeRule = createComposeRule()
    private var server: ServerSocket? = null
    private var webView: SecureExamWebView? = null
    private var client: android.webkit.WebViewClient? = null
    private val successfulLoads = AtomicInteger()
    private val errors = AtomicInteger()
    private val httpErrors = AtomicInteger()
    private val lastResourceError = AtomicReference<android.webkit.WebResourceError>()
    private val events = ConcurrentLinkedQueue<String>()

    @After
    fun cleanup() {
        composeRule.runOnIdle {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
        server?.close()
    }

    private fun startServer(port: Int = 0): String {
        val socket = ServerSocket(port)
        server = socket
        Thread({
            while (!socket.isClosed) {
                try {
                    socket.accept().use { connection ->
                        connection.soTimeout = 3_000
                        val input = connection.getInputStream().bufferedReader()
                        val path = input.readLine().orEmpty().split(' ').getOrNull(1).orEmpty()
                        events.add("server request: $path")
                        while (!input.readLine().isNullOrEmpty()) { }
                        val code = when {
                            path.startsWith("/redirect") -> "302 Found"
                            path.startsWith("/forbidden") -> "403 Forbidden"
                            path.startsWith("/unavailable") -> "503 Service Unavailable"
                            else -> "200 OK"
                        }
                        val body = "<html><body><h1>Exam page</h1><p>$code</p></body></html>"
                            .toByteArray(Charsets.UTF_8)
                        connection.getOutputStream().apply {
                            val redirectHeader = if (path.startsWith("/redirect")) {
                                "Location: /forbidden\r\n"
                            } else ""
                            write(("HTTP/1.1 $code\r\n${redirectHeader}Content-Type: text/html; charset=utf-8\r\n" +
                                "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
                            write(body)
                            flush()
                        }
                    }
                } catch (_: IOException) {
                    if (socket.isClosed) break
                }
            }
        }, "exam-test-http").apply { isDaemon = true; start() }
        return "http://localhost:${socket.localPort}"
    }

    private fun open(url: String) {
        composeRule.setContent {
            AndroidView(factory = { context ->
                SecureExamWebView(context).apply {
                    webView = this
                    requestedExamUrl = url
                    client = createExamWebViewClient(
                        onWebViewLoadStart = { _, path -> events.add("start: $path") },
                        onWebViewLoadFinish = { _, path ->
                            events.add("success: $path")
                            successfulLoads.incrementAndGet()
                        },
                        onWebViewLoadError = { _, message ->
                            events.add("error: $message")
                            errors.incrementAndGet()
                        },
                        onWebViewHttpError = { _, code ->
                            events.add("http error: $code")
                            httpErrors.incrementAndGet()
                        },
                        onWebViewRenderProcessGone = { _, _, _ -> true },
                        onLoadingProgressChange = { _, _ -> }
                    )
                    val delegate = client!!
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean =
                            delegate.shouldOverrideUrlLoading(view, request)
                        override fun onPageStarted(view: android.webkit.WebView?, url: String?, icon: android.graphics.Bitmap?) {
                            delegate.onPageStarted(view, url, icon)
                        }
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            events.add("raw finish: $url")
                            delegate.onPageFinished(view, url)
                        }
                        override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            events.add("raw error: ${request?.url} ${error?.description}")
                            if (error != null) lastResourceError.set(error)
                            delegate.onReceivedError(view, request, error)
                        }
                        override fun onReceivedHttpError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, response: android.webkit.WebResourceResponse?) {
                            events.add("raw http: ${request?.url} main=${request?.isForMainFrame} code=${response?.statusCode} current=${navigationState.currentUrl}")
                            delegate.onReceivedHttpError(view, request, response)
                        }
                    }
                    loadUrl(url)
                }
            })
        }
    }

    private fun awaitHttpError() {
        try {
            composeRule.waitUntil(15_000) { httpErrors.get() >= 1 }
        } catch (error: AssertionError) {
            throw AssertionError("HTTP error not delivered. Events: ${events.joinToString(" | ")}", error)
        }
    }

    @Test
    fun onlineProbeCannotClearWebViewHttpFailureAfterRedirect() {
        val base = startServer()
        open("$base/redirect")
        awaitHttpError()
        val probe = kotlinx.coroutines.runBlocking { probeExamServerFooterStatus("$base/redirect") }
        assertEquals(ExamServerFooterStatus.Online, probe.status)
        composeRule.runOnIdle {
            val state = webView!!.navigationState
            assertEquals("$base/forbidden", state.failedUrl)
            assertFalse(state.canApplyServerProbe(state.revision))
            assertEquals(0, successfulLoads.get())
        }
    }

    @Test
    fun connectionRetryRecoversWhenExamServerReturns() {
        val port = ServerSocket(0).use { it.localPort }
        val url = "http://localhost:$port/questions/2"
        open(url)
        composeRule.waitUntil(15_000) { errors.get() >= 1 }
        startServer(port)
        composeRule.waitUntil(20_000) { successfulLoads.get() >= 1 }
        composeRule.runOnIdle {
            val state = webView!!.navigationState
            assertEquals(url, webView!!.url)
            assertNull(state.failedUrl)
            assertFalse(state.canRecoverOnConnection)
            assertTrue(state.canApplyServerProbe(state.revision))
        }
    }

    private fun request(url: String, mainFrame: Boolean, method: String) =
        object : android.webkit.WebResourceRequest {
            override fun getUrl() = android.net.Uri.parse(url)
            override fun isForMainFrame() = mainFrame
            override fun isRedirect() = false
            override fun hasGesture() = false
            override fun getMethod() = method
            override fun getRequestHeaders() = emptyMap<String, String>()
        }

    @Test
    fun subresourceFailureKeepsExamUsableAndPostFailureNeverAutoReplays() {
        val base = startServer()
        val url = "$base/questions/2"
        val unusedPort = ServerSocket(0).use { it.localPort }
        open("http://localhost:$unusedPort/questions/2")
        composeRule.waitUntil(15_000) { lastResourceError.get() != null }
        composeRule.runOnIdle {
            webView!!.cancelPendingConnectionRetries()
            webView!!.loadUrl(url)
        }
        composeRule.waitUntil(15_000) { successfulLoads.get() >= 1 }
        composeRule.runOnIdle {
            val view = webView!!
            val originalErrors = errors.get()
            val connectionError = lastResourceError.get()!!
            client!!.onReceivedError(view, request("$base/analytics.js", false, "GET"), connectionError)
            assertEquals(originalErrors, errors.get())
            assertNull(view.navigationState.failedUrl)
            client!!.onPageStarted(view, url, null)
            client!!.onReceivedError(view, request(url, true, "POST"), connectionError)
            client!!.onPageFinished(view, url)
            assertEquals(originalErrors + 1, errors.get())
            assertEquals(1, successfulLoads.get())
            assertFalse(view.navigationState.canRecoverOnConnection)
            assertNull(view.navigationState.nextRetryDelayMillis())
        }
    }

    @Test
    fun httpErrorFinishCannotClearErrorAndManualRecoveryKeepsCurrentUrl() {
        val base = startServer()
        open("$base/forbidden")
        awaitHttpError()
        composeRule.runOnIdle {
            val view = webView!!
            client!!.onPageFinished(view, "$base/forbidden")
            assertEquals(0, successfulLoads.get())
            assertFalse(view.navigationState.canRecoverOnConnection)
            assertNull(view.navigationState.nextRetryDelayMillis())
            view.loadUrl("$base/questions/2")
        }
        composeRule.waitUntil(15_000) { successfulLoads.get() >= 1 }
        composeRule.runOnIdle {
            val view = webView!!
            assertEquals("$base/questions/2", view.url)
            view.reloadExamUrlLikeBrowserSafely("$base/forbidden")
        }
        composeRule.waitUntil(15_000) { successfulLoads.get() >= 2 }
        composeRule.runOnIdle {
            assertEquals("$base/questions/2", webView!!.url)
            assertEquals("$base/forbidden", webView!!.requestedExamUrl)
        }
    }

    @Test
    fun serverErrorDoesNotStartAutomaticReloadLoop() {
        val base = startServer()
        open("$base/unavailable")
        awaitHttpError()
        composeRule.runOnIdle {
            val view = webView!!
            client!!.onPageFinished(view, "$base/unavailable")
            assertEquals(0, successfulLoads.get())
            assertFalse(view.navigationState.canRecoverOnConnection)
            assertNull(view.navigationState.nextRetryDelayMillis())
        }
    }

    /**
     * The navigation watchdog must measure "no progress", not "not finished yet".
     * A slow but healthy load on congested school Wi-Fi used to be aborted with a
     * "check your internet connection" error while the page was still downloading.
     */
    @Test
    fun navigationWatchdogSurvivesSlowButProgressingLoad() {
        val fired = AtomicInteger()
        val watchdogWindowMs = 700L
        composeRule.setContent {
            AndroidView(factory = { context ->
                SecureExamWebView(context).apply { webView = this }
            })
        }
        composeRule.waitUntil(5_000) { webView != null }
        composeRule.runOnUiThread {
            webView!!.scheduleNavigationTimeout({ fired.incrementAndGet() }, watchdogWindowMs)
        }

        // Keep reporting forward progress for noticeably longer than one window.
        repeat(5) { step ->
            Thread.sleep(250)
            composeRule.runOnUiThread { webView!!.onNavigationProgress(10 + step * 15) }
        }
        assertEquals(
            "Watchdog fired while the page was still progressing. Events: ${events.joinToString(" | ")}",
            0,
            fired.get()
        )

        // Once progress stops the watchdog must still catch the real stall.
        composeRule.waitUntil(5_000) { fired.get() == 1 }
    }

    @Test
    fun refusedConnectionStopsAfterThreeRetries() {
        val unusedPort = ServerSocket(0).use { it.localPort }
        open("http://localhost:$unusedPort/questions/2")
        composeRule.waitUntil(35_000) { errors.get() >= 4 }
        composeRule.runOnIdle {
            assertEquals(4, errors.get())
            assertEquals(0, successfulLoads.get())
            assertNull(webView!!.navigationState.nextRetryDelayMillis())
            assertEquals("http://localhost:$unusedPort/questions/2", webView!!.navigationState.failedUrl)
        }
    }
}
