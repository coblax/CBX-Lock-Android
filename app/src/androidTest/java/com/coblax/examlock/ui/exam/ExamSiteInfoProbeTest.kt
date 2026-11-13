package com.coblax.examlock.ui.exam

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs [ExamSiteInfoProbeScript] in a real WebView against markup shaped like CBX Exam
 * System's pages (exam top bar from exam-client.tsx, student home header from
 * siswa/page.tsx), so a markup change there shows up here rather than as an empty Info.
 */
class ExamSiteInfoProbeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var webView: WebView? = null

    @After
    fun tearDown() {
        instrumentation.runOnMainSync { webView?.destroy() }
    }

    @Test
    fun examPageTopBarIsRead() {
        val site = probe("https://exam.test/ujian/42", ExamPageHtml)!!
        assertEquals("ujian", site.page)
        assertEquals("Siti Aminah", site.nama)
        assertEquals("12345", site.nis)
        assertEquals("R07", site.ruang)
        assertEquals("Matematika Wajib", site.paket)
        assertEquals("Offline", site.simpan)
        assertEquals(3, site.tertunda)
        assertEquals(12, site.dijawab)
        assertEquals(40, site.total)
        assertEquals("1:05:09", site.sisaWaktu)
        assertEquals("XII TKJ 1", site.kelas)
        assertEquals("Ruang 7", site.ruangNama)
    }

    @Test
    fun studentHomeIsReadAndItsClassCarriesIntoTheExamPage() {
        val home = probe("https://exam.test/siswa", HomePageHtml)!!
        assertEquals("siswa", home.page)
        assertEquals("Siti Aminah", home.nama)
        assertEquals("12345", home.nis)
        assertEquals("XII TKJ 1", home.kelas)

        // Same origin and tab, as after the site's in-page link: no embedded class data.
        val exam = probe("https://exam.test/ujian/42", ExamPageHtml.replace(EmbeddedData, ""), reuse = true)!!
        assertEquals("XII TKJ 1", exam.kelas)
        assertNull(exam.ruangNama)
    }

    @Test
    fun otherPagesReportOnlyThatTheyAreOther() {
        val site = probe("https://exam.test/login", "<html><body><h1>Masuk</h1></body></html>")!!
        assertEquals("lain", site.page)
        assertNull(site.nama)
    }

    private fun probe(url: String, html: String, reuse: Boolean = false): ExamSiteInfo? {
        val loaded = CountDownLatch(1)
        val result = AtomicReference<String?>()
        val answered = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val view = webView.takeIf { reuse } ?: WebView(instrumentation.targetContext).also { created ->
                webView?.destroy()
                webView = created
                created.settings.javaScriptEnabled = true
                created.settings.domStorageEnabled = true
            }
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    loaded.countDown()
                }
            }
            view.loadDataWithBaseURL(url, html, "text/html", "utf-8", null)
        }
        assertTrue("page loaded", loaded.await(15, TimeUnit.SECONDS))
        instrumentation.runOnMainSync {
            webView!!.evaluateExamJavascriptSafely(ExamSiteInfoProbeScript) {
                result.set(it)
                answered.countDown()
            }
        }
        assertTrue("probe answered", answered.await(10, TimeUnit.SECONDS))
        return parseExamSiteInfo(result.get())
    }

    private companion object {
        // React writes the RSC payload into script tags with escaped quotes.
        const val EmbeddedData =
            "<script>self.__next_f.push([1,\"{\\\"peserta\\\":{\\\"namaSiswa\\\":\\\"Siti Aminah\\\"," +
                "\\\"kelasNama\\\":\\\"XII TKJ 1\\\",\\\"ruangKode\\\":\\\"R07\\\",\\\"ruangNama\\\":\\\"Ruang 7\\\"}}\"])</script>"

        val ExamPageHtml = """
            <html><body><div data-area-siswa>
            <header>
              <div>
                <div>
                  <p title="Matematika Wajib">Matematika Wajib</p>
                  <p class="hidden sm:flex">
                    <span><svg></svg>Offline · 3</span>
                    <span>·</span>
                    <span>12/40 dijawab</span>
                    <span class="hidden md:inline">· Siti Aminah · NIS 12345 · R07</span>
                  </p>
                </div>
                <p class="sm:hidden">
                  <span><svg></svg>Offline · 3</span>
                  <span>·</span>
                  <span>12/40</span>
                </p>
                <button aria-label="Ukuran huruf &amp; info peserta"><span>Aa</span></button>
                <div role="timer" aria-label="Sisa waktu"><svg></svg>1:05:09</div>
              </div>
            </header>
            <main><p>Soal 1</p></main>
            </div>
            $EmbeddedData
            </body></html>
        """.trimIndent()

        val HomePageHtml = """
            <html><body>
            <header><p>Selamat datang</p></header>
            <div>
              <p>Siswa</p>
              <h1>Siti Aminah</h1>
              <div class="mt-1 flex">
                <span>NIS 12345</span>
                <span>XII TKJ 1</span>
                <span>Teknik Komputer dan Jaringan</span>
              </div>
            </div>
            </body></html>
        """.trimIndent()
    }
}
