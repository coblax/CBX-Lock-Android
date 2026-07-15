package com.coblax.examlock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousExamSessionBreadcrumbTest {
    @Test
    fun redactsUrlsTokensWifiAndCoordinates() {
        val redacted = sanitizePreviousExamSessionBreadcrumbDetails(
            "url=https://exam.example.sch.id/path/start?token=abc token=secret " +
                "ssid=SchoolWifi bssid=12:34:56:78:90:ab lat=-6.123456 lng=106.123456"
        )

        assertTrue(redacted.contains("exam.example.sch.id"))
        assertFalse(redacted.contains("/path/start"))
        assertFalse(redacted.contains("secret"))
        assertFalse(redacted.contains("SchoolWifi"))
        assertFalse(redacted.contains("12:34:56:78:90:ab"))
        assertFalse(redacted.contains("-6.123456"))
        assertFalse(redacted.contains("106.123456"))
    }

    @Test
    fun rendererGoneWithoutCleanExitProducesRecoveryHint() {
        val breadcrumb = PreviousExamSessionBreadcrumb(
            appendPreviousExamSessionBreadcrumb(
                existingEntries = emptyList(),
                newEntry = PreviousExamSessionBreadcrumbEntry(
                    code = PreviousExamSessionBreadcrumbCodes.RendererGone,
                    details = "didCrash=true",
                    elapsedRealtimeMs = 100L,
                    wallClockMs = 1_000L
                )
            )
        )

        assertNotNull(breadcrumb.latestRecoveryHint)
    }

    @Test
    fun cleanExitAfterRendererGoneClearsRecoveryHint() {
        val entries = appendPreviousExamSessionBreadcrumb(
            existingEntries = listOf(
                PreviousExamSessionBreadcrumbEntry(
                    code = PreviousExamSessionBreadcrumbCodes.RendererGone,
                    details = "didCrash=true",
                    elapsedRealtimeMs = 100L,
                    wallClockMs = 1_000L
                )
            ),
            newEntry = PreviousExamSessionBreadcrumbEntry(
                code = PreviousExamSessionBreadcrumbCodes.CleanupSucceeded,
                details = "reason=exit",
                elapsedRealtimeMs = 200L,
                wallClockMs = 2_000L
            )
        )

        assertTrue(PreviousExamSessionBreadcrumb(entries).latestRecoveryHint == null)
    }

    @Test
    fun breadcrumbIsBoundedByMaxBytes() {
        var entries = emptyList<PreviousExamSessionBreadcrumbEntry>()
        repeat(80) { index ->
            entries = appendPreviousExamSessionBreadcrumb(
                existingEntries = entries,
                newEntry = PreviousExamSessionBreadcrumbEntry(
                    code = "event_$index",
                    details = "x".repeat(600),
                    elapsedRealtimeMs = index.toLong(),
                    wallClockMs = index.toLong()
                ),
                maxBytes = 1_500
            )
        }

        assertTrue(entries.size < 80)
    }
}
