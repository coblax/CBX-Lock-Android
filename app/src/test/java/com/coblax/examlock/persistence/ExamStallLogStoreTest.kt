package com.coblax.examlock.persistence

import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.admin.formatExamStallHistoryForCopy
import com.coblax.examlock.ui.admin.stallHistoryBody
import com.coblax.examlock.ui.exam.ExamStallNetworkEvidence
import com.coblax.examlock.ui.exam.ExamStallPageEvidence
import com.coblax.examlock.ui.exam.ExamStallReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamStallLogStoreTest {
    private val report = ExamStallReport(
        attempt = 2,
        wallClockMs = 1_700_000_000_000L,
        examHost = "cbt.sekolah.sch.id",
        page = ExamStallPageEvidence(
            readyState = "loading",
            resources = 3,
            finished = 1,
            pending = listOf("cbt.sekolah.sch.id js"),
            pendingCount = 1
        ),
        network = ExamStallNetworkEvidence(
            transport = "cellular",
            validated = null,
            dnsServers = listOf("10.0.0.1", "10.0.0.2"),
            privateDns = "strict:dns.google",
            ipv6 = false,
            lookups = listOf("cbt.sekolah.sch.id" to "103.10.20.30")
        )
    )

    @Test
    fun entriesSurviveTheRoundTripUnchanged() {
        val entries = listOf(
            ExamStallLogEntry(ExamStallLogKind.Stall, report.wallClockMs, report = report),
            ExamStallLogEntry(ExamStallLogKind.Restart, report.wallClockMs + 1_000, note = "reason=button"),
            ExamStallLogEntry(ExamStallLogKind.Recovered, report.wallClockMs + 60_000, note = "minutes_since_restart=1")
        )

        assertEquals(entries, decodeExamStallLog(encodeExamStallLog(entries)))
    }

    @Test
    fun aProbeThatFailedIsKeptAsSuch() {
        val failed = report.copy(page = ExamStallPageEvidence(probeError = "no_result"))
        val entry = ExamStallLogEntry(ExamStallLogKind.Stall, 1L, report = failed)

        assertEquals(listOf(entry), decodeExamStallLog(encodeExamStallLog(listOf(entry))))
    }

    @Test
    fun brokenStorageReadsAsEmpty() {
        assertEquals(emptyList<ExamStallLogEntry>(), decodeExamStallLog(null))
        assertEquals(emptyList<ExamStallLogEntry>(), decodeExamStallLog("not json"))
        assertEquals(emptyList<ExamStallLogEntry>(), decodeExamStallLog("""[{"kind":"unknown","at":1}]"""))
    }

    @Test
    fun adminHistoryShowsTheSummaryAndTheFullLine() {
        val body = stallHistoryBody(ExamStallLogEntry(ExamStallLogKind.Stall, 1L, report = report), UiLanguage.Indonesian)

        assertEquals("Menggantung: 1 JS dari server ujian", body.first())
        assertTrue(body.last(), body.last().startsWith("EXAM_PAGE_STALLED: attempt=2"))
        assertTrue(body.last().contains("cbt.sekolah.sch.id=103.10.20.30"))
        assertTrue(body.last().contains("private_dns=strict:dns.google"))
    }

    @Test
    fun copiedHistoryHasOneBlockPerEntry() {
        val text = formatExamStallHistoryForCopy(
            listOf(
                ExamStallLogEntry(ExamStallLogKind.Restart, 1L, note = "reason=exit_after_stall"),
                ExamStallLogEntry(ExamStallLogKind.Stall, 1L, report = report)
            ),
            UiLanguage.Indonesian
        )
        val blocks = text.split("\n\n")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0].contains("Aplikasi dimulai ulang"))
        assertTrue(blocks[0].contains("reason=exit_after_stall"))
        assertTrue(blocks[1].contains("Macet #2"))
    }
}
