package com.coblax.examlock.ui.exam

import android.net.Uri
import android.webkit.WebSettings
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.applyExamWebViewSettings
import com.coblax.examlock.config.DefaultExamUserAgent
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The field failure: the exam page's HTML arrives, then a script it needs never does.
 * White screen, progress bar frozen part-way, network up. The second stall in a row must
 * tell the student to cycle Wi-Fi, the reload after a stall must bypass every cache, and
 * the probe must name the host that hung.
 */
class ExamWebViewStallTest {
    @get:Rule val composeRule = createComposeRule()
    private var server: ServerSocket? = null
    private var webView: SecureExamWebView? = null
    private val heldSockets = ConcurrentLinkedQueue<Socket>()
    private val scriptHangs = AtomicBoolean(true)
    private val errors = ConcurrentLinkedQueue<String>()
    private val loads = ConcurrentLinkedQueue<String>()
    private val requests = ConcurrentLinkedQueue<String>()

    @After
    fun cleanup() {
        composeRule.runOnIdle {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
        heldSockets.forEach { runCatching { it.close() } }
        server?.close()
    }

    private fun startServer(): Int {
        val socket = ServerSocket(0)
        server = socket
        Thread({
            while (!socket.isClosed) {
                val connection = try { socket.accept() } catch (_: IOException) { break }
                Thread({ serve(connection) }, "stall-test-conn").apply { isDaemon = true; start() }
            }
        }, "stall-test-http").apply { isDaemon = true; start() }
        return socket.localPort
    }

    private fun serve(connection: Socket) {
        try {
            val input = connection.getInputStream().bufferedReader()
            val path = input.readLine().orEmpty().split(' ').getOrNull(1).orEmpty()
            while (!input.readLine().isNullOrEmpty()) { }
            requests.add(path)
            if (path.startsWith("/app.js") && scriptHangs.get()) {
                // Accept the request and never answer, like a connection left half-open.
                heldSockets.add(connection)
                return
            }
            val (type, body) = if (path.startsWith("/app.js")) {
                "application/javascript" to "document.body.setAttribute('data-ready','1');"
            } else {
                "text/html" to "<html><head><script src=\"/app.js\"></script></head>" +
                    "<body><h1>Soal 1</h1></body></html>"
            }
            val bytes = body.toByteArray(Charsets.UTF_8)
            connection.getOutputStream().apply {
                write(
                    ("HTTP/1.1 200 OK\r\nContent-Type: $type; charset=utf-8\r\n" +
                        "Cache-Control: max-age=3600\r\nContent-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n\r\n").toByteArray()
                )
                write(bytes)
                flush()
            }
            connection.close()
        } catch (_: IOException) {
            runCatching { connection.close() }
        }
    }

    private fun open(url: String) {
        composeRule.setContent {
            AndroidView(factory = { context ->
                SecureExamWebView(context).apply {
                    webView = this
                    requestedExamUrl = url
                    applyExamWebViewSettings(DefaultExamUserAgent, LowRamProfile())
                    webViewClient = createExamWebViewClient(
                        onWebViewLoadStart = { _, _ -> },
                        onWebViewLoadFinish = { _, finished -> loads.add(finished.orEmpty()) },
                        onWebViewLoadError = { _, message -> errors.add(message) },
                        onWebViewHttpError = { _, _ -> },
                        onWebViewRenderProcessGone = { _, _, _ -> true },
                        onLoadingProgressChange = { _, _ -> },
                        stallTimeoutMillis = 4_000L
                    )
                    loadUrl(url)
                }
            })
        }
    }

    private fun awaitErrors(count: Int) {
        try {
            composeRule.waitUntil(30_000) { errors.size >= count }
        } catch (error: ComposeTimeoutException) {
            throw AssertionError("Expected $count stall(s). Errors: $errors | requests: $requests", error)
        }
    }

    @Test
    fun secondStallAsksForAWifiCycleAndTheReloadBypassesTheCache() {
        val port = startServer()
        val url = "http://localhost:$port/soal/1"
        open(url)

        awaitErrors(1)
        assertTrue(errors.last(), errors.last().endsWith("(STALL: 1)"))
        assertTrue(errors.last().contains("Muat ulang"))
        composeRule.runOnIdle { assertEquals(1, webView!!.navigationState.consecutiveStalls) }

        // The probe, read before the page was stopped, names what never finished: the
        // page's own script, on the exam host.
        val probe = AtomicReference<String>()
        composeRule.runOnIdle { probe.set(webView!!.lastStallProbe) }
        val evidence = parseExamStallProbe(probe.get())
        assertEquals("probe: ${probe.get()}", listOf("localhost"), evidence.pendingHosts)
        assertTrue(evidence.pending.single().endsWith(" js"))
        assertEquals("loading", evidence.readyState)

        composeRule.runOnIdle {
            assertTrue(webView!!.reloadExamUrlLikeBrowserSafely(url))
            assertEquals(WebSettings.LOAD_NO_CACHE, webView!!.settings.cacheMode)
        }
        awaitErrors(2)
        assertTrue(errors.last(), errors.last().endsWith("(STALL: 2)"))
        assertTrue(errors.last().contains("Mulai ulang aplikasi"))
        assertTrue(errors.last().contains("mode pesawat"))
        assertTrue(errors.last().contains("Tidak perlu restart HP"))

        // The network is cleared up: the next reload loads and everything resets.
        scriptHangs.set(false)
        composeRule.runOnIdle { webView!!.reloadExamUrlLikeBrowserSafely(url) }
        try {
            composeRule.waitUntil(30_000) { loads.any { Uri.parse(it).path == "/soal/1" } }
        } catch (error: ComposeTimeoutException) {
            throw AssertionError("Page never loaded. Errors: $errors | requests: $requests", error)
        }
        composeRule.runOnIdle {
            assertEquals(0, webView!!.navigationState.consecutiveStalls)
            assertEquals(WebSettings.LOAD_DEFAULT, webView!!.settings.cacheMode)
        }
        assertEquals("only two stalls were reported", 2, errors.size)
    }
}
