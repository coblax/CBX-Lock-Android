package com.coblax.examlock.ui.exam

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import com.coblax.examlock.LowRamProfile
import com.coblax.examlock.applyExamWebViewSettings
import com.coblax.examlock.config.DefaultExamUserAgent
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * What the exam server actually receives: the CBX User-Agent marker and the
 * X-CBX-App-Key header, on page loads, JavaScript navigations, redirects, fetch and XHR,
 * and never the key on a request to another host.
 */
class ExamRequestIdentityTest {
    @get:Rule val composeRule = createComposeRule()
    private var server: ServerSocket? = null
    private var webView: SecureExamWebView? = null
    private val requests = ConcurrentLinkedQueue<RecordedRequest>()

    private data class RecordedRequest(
        val method: String,
        val path: String,
        val host: String,
        val headers: Map<String, String>
    ) {
        fun header(name: String): String? =
            headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    @After
    fun cleanup() {
        composeRule.runOnIdle {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
        server?.close()
    }

    private fun startServer(): Int {
        val socket = ServerSocket(0)
        server = socket
        val port = socket.localPort
        Thread({
            while (!socket.isClosed) {
                try {
                    socket.accept().use { connection ->
                        connection.soTimeout = 3_000
                        val input = connection.getInputStream().bufferedReader()
                        val requestLine = input.readLine().orEmpty().split(' ')
                        val headers = linkedMapOf<String, String>()
                        while (true) {
                            val line = input.readLine()
                            if (line.isNullOrEmpty()) break
                            val colon = line.indexOf(':')
                            if (colon > 0) headers[line.substring(0, colon).trim()] = line.substring(colon + 1).trim()
                        }
                        val path = requestLine.getOrNull(1).orEmpty()
                        val host = headers.entries.firstOrNull { it.key.equals("Host", true) }?.value.orEmpty()
                        requests.add(RecordedRequest(requestLine.firstOrNull().orEmpty(), path, host, headers))
                        val (status, extraHeaders, body) = respond(path, port)
                        connection.getOutputStream().apply {
                            val bytes = body.toByteArray(Charsets.UTF_8)
                            write(
                                ("HTTP/1.1 $status\r\n$extraHeaders" +
                                    "Content-Type: text/html; charset=utf-8\r\n" +
                                    "Cache-Control: no-store\r\n" +
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
        }, "exam-identity-http").apply { isDaemon = true; start() }
        return port
    }

    private fun respond(path: String, port: Int): Triple<String, String, String> = when {
        path == "/" || path.startsWith("/login") -> Triple(
            "200 OK",
            "",
            """
            <html><body><h1>Login</h1><script>
            setTimeout(function() {
              var xhr = new XMLHttpRequest();
              xhr.open('GET', '/xhr');
              xhr.onloadend = function() {
                fetch('/fetch').then(function() {
                  return fetch('http://127.0.0.1:$port/cross').catch(function() {});
                }).then(function() {
                  location.href = '/next';
                });
              };
              xhr.send();
            }, 600);
            </script></body></html>
            """.trimIndent()
        )
        path.startsWith("/next") -> Triple("302 Found", "Location: /final\r\n", "")
        else -> Triple("200 OK", "Access-Control-Allow-Origin: *\r\n", "<html><body><p>$path</p></body></html>")
    }

    private fun open(url: String, appKey: String) {
        composeRule.setContent {
            AndroidView(factory = { context ->
                SecureExamWebView(context).apply {
                    webView = this
                    requestedExamUrl = url
                    installExamHostHeaders(buildExamHostHeaders(url, appKey))
                    applyExamWebViewSettings(DefaultExamUserAgent, LowRamProfile())
                    webViewClient = createExamWebViewClient(
                        onWebViewLoadStart = { _, _ -> },
                        onWebViewLoadFinish = { _, _ -> },
                        onWebViewLoadError = { _, _ -> },
                        onWebViewHttpError = { _, _ -> },
                        onWebViewRenderProcessGone = { _, _, _ -> true },
                        onLoadingProgressChange = { _, _ -> }
                    )
                    loadUrl(url)
                }
            })
        }
    }

    private fun awaitPath(path: String) {
        try {
            composeRule.waitUntil(30_000) { requests.any { it.path == path } }
        } catch (error: ComposeTimeoutException) {
            throw AssertionError(
                "No request for $path. Seen: ${requests.joinToString { "${it.method} ${it.host}${it.path}" }}",
                error
            )
        }
    }

    private fun examRequests(port: Int) = requests.filter {
        it.host == "localhost:$port" && it.path != "/favicon.ico"
    }

    @Test
    fun everyExamRequestCarriesTheMarkerAndTheKey() {
        val port = startServer()
        open("http://localhost:$port/login", appKey = "223611")
        awaitPath("/final")
        awaitPath("/cross")

        val exam = examRequests(port)
        assertEquals(
            listOf("/login", "/xhr", "/fetch", "/next", "/final"),
            exam.map { it.path }
        )
        exam.forEach { request ->
            val ua = request.header("User-Agent").orEmpty()
            assertTrue("UA for ${request.path}: $ua", ua.startsWith("Mozilla/") && ua.endsWith(" CBXExamLockAndroid"))
            assertEquals("key for ${request.path}", "223611", request.header("X-CBX-App-Key"))
        }

        val cross = requests.first { it.path == "/cross" }
        assertEquals("GET", cross.method)
        assertNull("the key must not leave the exam host", cross.header("X-CBX-App-Key"))
    }

    @Test
    fun blankKeySendsNoHeaderButStillTheMarker() {
        val port = startServer()
        open("http://localhost:$port/login", appKey = "")
        awaitPath("/final")

        examRequests(port).forEach { request ->
            assertNull("key for ${request.path}", request.header("X-CBX-App-Key"))
            assertTrue(request.header("User-Agent").orEmpty().contains("CBXExamLockAndroid"))
        }
    }
}
