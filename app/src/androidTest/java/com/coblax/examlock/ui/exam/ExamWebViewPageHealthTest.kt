package com.coblax.examlock.ui.exam

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.applyExamWebViewSettings
import com.coblax.examlock.config.DefaultExamUserAgent
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pages that arrive from a healthy server yet show the student nothing: a cached page
 * from an earlier launch, a script the server no longer has, a script the WebView cannot
 * run. Each must end in a message, while a page that is merely still drawing must not.
 */
class ExamWebViewPageHealthTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var server: ServerSocket? = null
    private var webView: SecureExamWebView? = null
    private val loadErrors = ConcurrentLinkedQueue<String>()
    private val successfulLoads = AtomicInteger()
    private val requests = ConcurrentLinkedQueue<String>()

    @Volatile
    private var examPageVersion = 1
    private val generation = mutableIntStateOf(0)
    private var contentSet = false
    private var nextUrl = ""
    private var nextExamSettings = false
    private var nextClearCache = false

    @After
    fun cleanup() {
        composeRule.runOnIdle {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
        server?.close()
    }

    private fun startServer(): String {
        val socket = ServerSocket(0)
        server = socket
        Thread({
            while (!socket.isClosed) {
                try {
                    socket.accept().use { connection ->
                        connection.soTimeout = 3_000
                        val input = connection.getInputStream().bufferedReader()
                        val path = input.readLine().orEmpty().split(' ').getOrNull(1).orEmpty()
                        requests.add(path)
                        while (!input.readLine().isNullOrEmpty()) { }
                        val (status, type, body, extraHeaders) = respond(path)
                        val bytes = body.toByteArray(Charsets.UTF_8)
                        connection.getOutputStream().apply {
                            write(
                                ("HTTP/1.1 $status\r\nContent-Type: $type; charset=utf-8\r\n$extraHeaders" +
                                    "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray()
                            )
                            write(bytes)
                            flush()
                        }
                    }
                } catch (_: IOException) {
                    if (socket.isClosed) break
                }
            }
        }, "exam-page-health-http").apply { isDaemon = true; start() }
        return "http://localhost:${socket.localPort}"
    }

    private data class Response(val status: String, val type: String, val body: String, val headers: String = "")

    private fun respond(path: String): Response = when {
        // Revalidated like any page a static exam server hands out.
        path.startsWith("/exam") -> Response(
            "200 OK", "text/html",
            "<html><body><h1>Soal versi $examPageVersion</h1></body></html>",
            "Cache-Control: no-cache\r\nETag: \"v$examPageVersion\"\r\n"
        )
        path.startsWith("/spa-missing-script") -> Response(
            "200 OK", "text/html",
            "<html><body><div id=\"app\"></div><script src=\"/assets/app.4f2a.js\"></script></body></html>"
        )
        path.startsWith("/spa-broken-script") -> Response(
            "200 OK", "text/html",
            "<html><body><div id=\"app\"></div><script src=\"/assets/broken.js\"></script></body></html>"
        )
        path.startsWith("/assets/broken.js") -> Response("200 OK", "application/javascript", "var exam = ;")
        path.startsWith("/still-drawing") -> Response(
            "200 OK", "text/html",
            "<html><body><div id=\"app\" style=\"width:40px;height:40px\"></div></body></html>"
        )
        path.startsWith("/working-page") -> Response(
            "200 OK", "text/html",
            "<html><body><h1>Soal nomor 1</h1><script src=\"/assets/analytics.js\"></script></body></html>"
        )
        else -> Response("404 Not Found", "text/plain", "missing")
    }

    /** Each call opens a new exam WebView, as a fresh app launch would. */
    private fun open(url: String, examSettings: Boolean = false, clearCacheFirst: Boolean = false) {
        nextUrl = url
        nextExamSettings = examSettings
        nextClearCache = clearCacheFirst
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                key(generation.intValue) {
                    AndroidView(factory = { context -> newExamWebView(context) })
                }
            }
        } else {
            composeRule.runOnIdle {
                webView?.stopLoading()
                generation.intValue++
            }
        }
    }

    private fun newExamWebView(context: Context): SecureExamWebView =
        SecureExamWebView(context).apply {
            webView?.destroy()
            webView = this
            if (nextClearCache) clearCache(true)
            if (nextExamSettings) applyExamWebViewSettings(DefaultExamUserAgent, LowRamProfile())
            settings.javaScriptEnabled = true
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    noteConsoleMessage(consoleMessage?.messageLevel(), consoleMessage?.message())
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            webViewClient = createExamWebViewClient(
                onWebViewLoadStart = { _, _ -> },
                onWebViewLoadFinish = { _, _ -> successfulLoads.incrementAndGet() },
                onWebViewLoadError = { _, message -> loadErrors.add(message) },
                onWebViewHttpError = { _, code -> loadErrors.add("http $code") },
                onWebViewRenderProcessGone = { _, _, _ -> true },
                onLoadingProgressChange = { _, _ -> },
                blankPageFirstCheckMillis = 1_500L,
                blankPageSecondCheckMillis = 1_500L
            )
            loadUrl(nextUrl)
        }

    private fun pageText(): String {
        val text = AtomicReference<String?>(null)
        composeRule.runOnUiThread {
            webView!!.evaluateJavascript("document.body.innerText") { text.set(it) }
        }
        composeRule.waitUntil(5_000) { text.get() != null }
        return text.get()!!
    }

    private fun awaitLoadError(): String {
        try {
            composeRule.waitUntil(15_000) { loadErrors.isNotEmpty() }
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError("No error reached the student. Requests: ${requests.joinToString()}", timeout)
        }
        return loadErrors.first()
    }

    /** Opening the app again must show the exam as the server has it now. */
    @Test
    fun aFreshLaunchShowsTheCurrentPageNotACachedOne() {
        val base = startServer()
        open("$base/exam", examSettings = true, clearCacheFirst = true)
        composeRule.waitUntil(15_000) { successfulLoads.get() >= 1 }
        assertTrue(pageText(), pageText().contains("versi 1"))

        examPageVersion = 2
        successfulLoads.set(0)
        open("$base/exam", examSettings = true)
        composeRule.waitUntil(15_000) { successfulLoads.get() >= 1 }
        assertTrue("Got: ${pageText()}", pageText().contains("versi 2"))
    }

    @Test
    fun aBlankPageWhoseScriptIsMissingTellsTheStudent() {
        val base = startServer()
        open("$base/spa-missing-script")
        val message = awaitLoadError()
        assertTrue(message, message.contains("(BLANK:") && message.contains("file pentingnya gagal dimuat"))
        composeRule.runOnIdle {
            val state = webView!!.navigationState
            assertEquals("$base/spa-missing-script", state.failedUrl)
            assertTrue(!state.canApplyServerProbe(state.revision))
        }
    }

    @Test
    fun aBlankPageWhoseScriptCannotRunTellsTheStudent() {
        val base = startServer()
        open("$base/spa-broken-script")
        val message = awaitLoadError()
        assertTrue(message, message.contains("(BLANK:") && message.contains("script_errors=1"))
    }

    /** A page still drawing its first screen gives no reason to be blank: no error over it. */
    @Test
    fun aPageStillDrawingIsLeftAlone() {
        val base = startServer()
        open("$base/still-drawing")
        composeRule.waitUntil(15_000) { successfulLoads.get() >= 1 }
        Thread.sleep(5_000)
        composeRule.runOnIdle { assertTrue(loadErrors.joinToString(), loadErrors.isEmpty()) }
    }

    /** A failed analytics script on a page the student can read changes nothing. */
    @Test
    fun aWorkingPageWithAFailedScriptIsLeftAlone() {
        val base = startServer()
        open("$base/working-page")
        composeRule.waitUntil(15_000) { successfulLoads.get() >= 1 }
        Thread.sleep(5_000)
        composeRule.runOnIdle {
            assertTrue(loadErrors.joinToString(), loadErrors.isEmpty())
            assertEquals(1, webView!!.navigationState.pageBreakage.failedResources)
        }
    }
}
