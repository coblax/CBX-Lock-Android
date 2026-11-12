package com.coblax.examlock.ui.exam

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamStallRecoveryTest {
    @Test
    fun firstStallKeepsTheReloadAdviceAndMentionsTheRestart() {
        val message = examStallMessage(consecutiveStalls = 1, stallSeconds = 60)
        assertTrue(message.contains("60 detik"))
        assertTrue(message.contains("Muat ulang"))
        assertTrue(message.contains("Mulai ulang aplikasi"))
        assertFalse(message.contains("Wi-Fi"))
    }

    @Test
    fun secondStallSaysHowToClearItWithoutRestartingThePhone() {
        val message = examStallMessage(consecutiveStalls = 2, stallSeconds = 60)
        assertTrue(message.contains("Mulai ulang aplikasi"))
        // Students were on mobile data or tethering: airplane mode, not Wi-Fi.
        assertTrue(message.contains("mode pesawat"))
        assertFalse(message.contains("Wi-Fi"))
        assertTrue(message.contains("Tidak perlu restart HP"))
        assertEquals(message, examStallMessage(consecutiveStalls = 5, stallSeconds = 60))
    }

    @Test
    fun summaryLeavesTheExamServerOutButNamesOtherHosts() {
        val report = ExamStallReport(
            attempt = 2,
            wallClockMs = 0L,
            examHost = "cbt.sekolah.sch.id",
            page = ExamStallPageEvidence(
                readyState = "loading",
                resources = 3,
                finished = 1,
                pending = listOf("cbt.sekolah.sch.id js", "cdn.jsdelivr.net css"),
                pendingCount = 2
            ),
            network = ExamStallNetworkEvidence(
                transport = "cellular",
                validated = true,
                dnsServers = listOf("10.0.0.1"),
                privateDns = "auto",
                ipv6 = true,
                lookups = listOf("cbt.sekolah.sch.id" to "103.10.20.30,2001:db8::1", "cdn.jsdelivr.net" to "timeout")
            )
        )

        val lines = buildExamStallSummaryLines(report, com.coblax.examlock.model.UiLanguage.Indonesian, "10:42:13")

        assertEquals(
            listOf(
                "Macet ke-2 · 10:42:13",
                "Menggantung: 1 JS dari server ujian, 1 CSS dari cdn.jsdelivr.net",
                "Jaringan: Data seluler, internet terverifikasi",
                "DNS server ujian: terjawab (IPv4+IPv6)",
                "Private DNS: otomatis · IPv6: ya"
            ),
            lines
        )
        lines.forEach { line ->
            assertFalse(line, line.contains("sekolah.sch.id"))
            assertFalse(line, line.contains("103.10.20.30"))
        }
        // The admin line keeps everything.
        assertTrue(report.adminDetail.contains("cbt.sekolah.sch.id=103.10.20.30"))
    }

    @Test
    fun summaryForAMainPageThatNeverArrived() {
        val report = ExamStallReport(
            attempt = 1,
            wallClockMs = 0L,
            examHost = "cbt.sekolah.sch.id",
            page = ExamStallPageEvidence(readyState = "complete", resources = 0, finished = 0),
            network = ExamStallNetworkEvidence(
                transport = "wifi",
                validated = true,
                lookups = listOf("cbt.sekolah.sch.id" to "UnknownHostException")
            )
        )

        val lines = buildExamStallSummaryLines(report, com.coblax.examlock.model.UiLanguage.English, "08:00:00")

        assertEquals("Hung: the main page from the exam server never arrived", lines[1])
        assertEquals("Exam server DNS: failed: UnknownHostException", lines[3])
    }

    @Test
    fun studentReadsTheStallWordingNotTheGenericFallback() {
        // The 60-second message used to be replaced by "Halaman ujian belum dapat dimuat".
        val description = examStallErrorDescription(consecutiveStalls = 2, stallSeconds = 60)
        assertEquals(examStallMessage(2, 60), resolveWebViewLoadErrorMessage(description))
        assertTrue(description.endsWith("(STALL: 2)"))
    }

    @Test
    fun stallCountGrowsUntilAPageLoads() {
        val state = ExamWebViewNavigationState()
        state.start("https://cbt.test/soal/1")
        assertEquals(1, state.noteStall())
        state.fail("https://cbt.test/soal/1", recoverOnConnection = false)
        state.start("https://cbt.test/soal/1")
        assertEquals(2, state.noteStall())
        state.fail("https://cbt.test/soal/1", recoverOnConnection = false)

        state.start("https://cbt.test/soal/1")
        assertEquals(2, state.consecutiveStalls)
        assertTrue(state.finish("https://cbt.test/soal/1"))
        assertEquals(0, state.consecutiveStalls)
    }

    @Test
    fun probeResultIsReadFromEvaluateJavascriptQuoting() {
        val inner = JSONObject()
            .put("ready", "loading")
            .put("resources", 3)
            .put("finished", 1)
            .put("pending", org.json.JSONArray(listOf("cdn.example.com js", "cbt.test:8080 css")))
            .put("pendingCount", 2)
            .toString()
        val raw = JSONObject.quote(inner)

        val evidence = parseExamStallProbe(raw)

        assertEquals("loading", evidence.readyState)
        assertEquals(3, evidence.resources)
        assertEquals(1, evidence.finished)
        assertEquals(2, evidence.pendingCount)
        assertEquals(listOf("cdn.example.com", "cbt.test"), evidence.pendingHosts)
    }

    @Test
    fun probeFailuresAreNamedNotThrown() {
        assertEquals("no_result", parseExamStallProbe(null).probeError)
        assertEquals("no_result", parseExamStallProbe("null").probeError)
        assertEquals("unparseable", parseExamStallProbe("{not json").probeError)
        assertEquals(
            "TypeError: x",
            parseExamStallProbe(JSONObject.quote(JSONObject().put("error", "TypeError: x").toString())).probeError
        )
    }

    @Test
    fun evidenceLineCarriesWhatHungAndHowTheHostsResolve() {
        val line = formatExamStallEvidence(
            attempt = 2,
            page = ExamStallPageEvidence(
                readyState = "loading",
                resources = 4,
                finished = 2,
                pending = listOf("cdn.example.com js"),
                pendingCount = 1
            ),
            network = ExamStallNetworkEvidence(
                transport = "wifi",
                validated = true,
                dnsServers = listOf("192.168.1.1"),
                privateDns = "off",
                ipv6 = false,
                lookups = listOf("cbt.test" to "10.0.0.5", "cdn.example.com" to "timeout")
            )
        )

        assertEquals(
            "attempt=2 | ready=loading | resources=2/4 finished | pending=1 [cdn.example.com js] | " +
                "transport=wifi | validated=yes | dns=[192.168.1.1] | private_dns=off | ipv6=no | " +
                "resolve: cbt.test=10.0.0.5 cdn.example.com=timeout",
            line
        )
    }

    @Test
    fun evidenceLineSaysWhenThePageCouldNotBeProbed() {
        val line = formatExamStallEvidence(1, ExamStallPageEvidence(probeError = "evaluate_failed"), ExamStallNetworkEvidence())
        assertTrue(line.startsWith("attempt=1 | page_probe=evaluate_failed | transport=-"))
    }
}
