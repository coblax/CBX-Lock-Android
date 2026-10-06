package com.coblax.examlock.ui.exam

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Exam sites whose connection is not protected, from badssl.com's public test hosts and
 * plain http. The school wants the exam to go ahead whenever its server is online: the
 * page opens on every Android version, the student is warned, and a broken certificate
 * on some third-party resource is still refused.
 */
class ExamWebViewInsecureSiteTest {
    /** Android 9's WebView reports an expired certificate as a date problem. */
    private val ExpiredCertificate = arrayOf("certificate_SSL_EXPIRED", "certificate_SSL_DATE_INVALID")

    @get:Rule
    val composeRule = createComposeRule()

    private var webView: SecureExamWebView? = null
    private val loadErrors = ConcurrentLinkedQueue<String>()
    private val successfulLoads = AtomicInteger()
    private val events = ConcurrentLinkedQueue<String>()
    private var localServer: ServerSocket? = null

    @Before
    fun needsInternet() {
        val reachable = runCatching {
            Socket().use { it.connect(InetSocketAddress("badssl.com", 443), 5_000) }
        }.isSuccess
        assumeTrue("badssl.com is not reachable from this device", reachable)
    }

    @After
    fun cleanup() {
        composeRule.runOnIdle {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
        localServer?.close()
    }

    /** A page on this device answering every request with [response]. */
    private fun serveLocally(response: String): String {
        val socket = ServerSocket(0)
        localServer = socket
        Thread({
            while (!socket.isClosed) {
                try {
                    socket.accept().use { connection ->
                        val input = connection.getInputStream().bufferedReader()
                        while (!input.readLine().isNullOrEmpty()) { }
                        connection.getOutputStream().apply {
                            write(response.toByteArray())
                            flush()
                        }
                    }
                } catch (_: IOException) {
                    if (socket.isClosed) break
                }
            }
        }, "exam-insecure-site-http").apply { isDaemon = true; start() }
        return "http://localhost:${socket.localPort}/ujian"
    }

    private fun redirectingTo(target: String) = serveLocally(
        "HTTP/1.1 302 Found\r\nLocation: $target\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
    )

    private fun open(url: String) {
        composeRule.setContent {
            AndroidView(factory = { context ->
                SecureExamWebView(context).apply {
                    webView = this
                    requestedExamUrl = url
                    // WebView remembers "proceed" per host for the whole process; the app
                    // clears it with every exam WebView it throws away, and so does this.
                    clearSslPreferences()
                    val exam = createExamWebViewClient(
                        onWebViewLoadStart = { _, path -> events.add("start: $path") },
                        onWebViewLoadFinish = { _, path ->
                            events.add("success: $path")
                            successfulLoads.incrementAndGet()
                        },
                        onWebViewLoadError = { _, message ->
                            events.add("error: $message")
                            loadErrors.add(message)
                        },
                        onWebViewHttpError = { _, code -> events.add("http error: $code") },
                        onWebViewRenderProcessGone = { _, _, _ -> true },
                        onLoadingProgressChange = { _, _ -> }
                    )
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) =
                            exam.shouldOverrideUrlLoading(view, request)

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            events.add("raw start: $url")
                            exam.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            events.add("raw finish: $url")
                            exam.onPageFinished(view, url)
                        }

                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            events.add("raw ssl: ${error?.primaryError} ${error?.url}")
                            exam.onReceivedSslError(view, handler, error)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            events.add(
                                "raw error: main=${request?.isForMainFrame} ${error?.errorCode} " +
                                    "${error?.description} ${request?.url}"
                            )
                            exam.onReceivedError(view, request, error)
                        }
                    }
                    loadUrl(url)
                }
            })
        }
    }

    private fun awaitSettled(url: String) {
        try {
            composeRule.waitUntil(25_000) { successfulLoads.get() >= 1 || loadErrors.isNotEmpty() }
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError("$url never settled. Events: ${events.joinToString(" | ")}", timeout)
        }
    }

    /** The page opens; the footer and the admin learn the connection is unprotected. */
    private fun assertOpensWithAWarning(url: String, landsOn: String = url, insecurity: Array<String>) {
        open(url)
        awaitSettled(url)
        composeRule.runOnIdle {
            assertTrue("Events: ${events.joinToString(" | ")}", loadErrors.isEmpty())
            assertEquals(1, successfulLoads.get())
            val state = webView!!.navigationState
            val actual = state.insecurityOf(landsOn)
            assertTrue("$actual not in ${insecurity.toList()}", actual in insecurity)
        }
    }

    @Test
    fun expiredCertificateOpensWithAWarning() =
        assertOpensWithAWarning("https://expired.badssl.com/", insecurity = ExpiredCertificate)

    @Test
    fun certificateForAnotherHostOpensWithAWarning() =
        assertOpensWithAWarning("https://wrong.host.badssl.com/", insecurity = arrayOf("certificate_SSL_ID_MISMATCH"))

    @Test
    fun selfSignedCertificateOpensWithAWarning() =
        assertOpensWithAWarning("https://self-signed.badssl.com/", insecurity = arrayOf("certificate_SSL_UNTRUSTED"))

    @Test
    fun untrustedRootOpensWithAWarning() =
        assertOpensWithAWarning("https://untrusted-root.badssl.com/", insecurity = arrayOf("certificate_SSL_UNTRUSTED"))

    /** The school's link forwards to a server with an expired certificate. */
    @Test
    fun expiredCertificateAfterARedirectOpensWithAWarning() = assertOpensWithAWarning(
        redirectingTo("https://expired.badssl.com/"),
        landsOn = "https://expired.badssl.com/",
        insecurity = ExpiredCertificate
    )

    @Test
    fun plainHttpOpensWithAWarning() =
        assertOpensWithAWarning("http://http.badssl.com/", insecurity = arrayOf("cleartext_http"))

    /** Only the exam's own pages are let through; another site's broken certificate is not. */
    @Test
    fun aThirdPartyResourceWithABrokenCertificateIsStillRefused() {
        val url = serveLocally(
            "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n" +
                "<html><body><h1>Soal 1</h1><img src=\"https://expired.badssl.com/icons/icon-red.png\"></body></html>"
        )
        open(url)
        awaitSettled(url)
        composeRule.waitUntil(15_000) { events.any { it.startsWith("raw ssl:") } }
        composeRule.runOnIdle {
            val state = webView!!.navigationState
            assertTrue(loadErrors.isEmpty())
            assertFalse(state.isInsecureCertificateAccepted("expired.badssl.com"))
            assertEquals(1, state.pageBreakage.failedResources)
        }
    }

    /** The footer must say unprotected, not plainly Online, and never Offline. */
    @Test
    fun serverProbeCallsUnprotectedSitesInsecure() {
        listOf(
            "https://expired.badssl.com/",
            "https://wrong.host.badssl.com/",
            "https://self-signed.badssl.com/",
            "https://untrusted-root.badssl.com/",
            "http://http.badssl.com/"
        ).forEach { url ->
            // One retry: the public test hosts are a long way from a school network.
            val result = runBlocking {
                probeExamServerFooterStatus(url).takeIf { it.status == ExamServerFooterStatus.Insecure }
                    ?: probeExamServerFooterStatus(url)
            }
            assertEquals("$url -> ${result.reason}", ExamServerFooterStatus.Insecure, result.status)
        }
    }

    @Test
    fun serverProbeStillCallsAGoodCertificateOnline() {
        val result = runBlocking { probeExamServerFooterStatus("https://badssl.com/") }
        assertEquals(result.reason, ExamServerFooterStatus.Online, result.status)
    }

    /**
     * A P-384 certificate: Android 7.0's own TLS cannot use it but the WebView can, so the
     * exam opens as a normal secure page and the probe's handshake fallback calls it Online.
     */
    @Test
    fun p384CertificateOpensAsASecurePage() {
        open("https://ecc384.badssl.com/")
        awaitSettled("https://ecc384.badssl.com/")
        composeRule.runOnIdle {
            assertTrue("Events: ${events.joinToString(" | ")}", loadErrors.isEmpty())
            assertEquals(null, webView!!.navigationState.insecurityOf("https://ecc384.badssl.com/"))
        }
        val probe = runBlocking { probeExamServerFooterStatus("https://ecc384.badssl.com/") }
        assertEquals(probe.reason, ExamServerFooterStatus.Online, probe.status)
    }
}
