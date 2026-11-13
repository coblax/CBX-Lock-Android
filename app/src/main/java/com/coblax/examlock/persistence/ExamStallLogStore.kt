package com.coblax.examlock.persistence

import android.content.Context
import androidx.core.content.edit
import com.coblax.examlock.ui.exam.ExamStallNetworkEvidence
import com.coblax.examlock.ui.exam.ExamStallPageEvidence
import com.coblax.examlock.ui.exam.ExamStallReport
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stalls, app restarts after them, and the first page that loaded after a restart, kept
 * across restarts so an admin can read them on the phone afterwards (Secret Admin →
 * Diagnostics) without Telegram. The in-memory diagnostic log died with the session.
 */
internal enum class ExamStallLogKind(val code: String) {
    Stall("stall"),
    Restart("restart"),
    Recovered("recovered");

    companion object {
        fun fromCode(code: String?): ExamStallLogKind? = entries.firstOrNull { it.code == code }
    }
}

internal data class ExamStallLogEntry(
    val kind: ExamStallLogKind,
    val wallClockMs: Long,
    val report: ExamStallReport? = null,
    /** Restart reason, or how long after the restart the page loaded. */
    val note: String = ""
)

internal const val ExamStallLogMaxEntries = 10

internal object ExamStallLogStore {
    private const val PreferencesName = "cbx_exam_stall_log"
    private const val KeyEntries = "entries"
    private const val KeyRestartPendingAt = "restart_pending_at"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    /** Newest first. */
    fun read(context: Context): List<ExamStallLogEntry> =
        runCatching { decodeExamStallLog(prefs(context).getString(KeyEntries, null)) }
            .getOrDefault(emptyList())
            .asReversed()

    @Synchronized
    fun append(context: Context, entry: ExamStallLogEntry) {
        runCatching {
            val prefs = prefs(context)
            val existing = decodeExamStallLog(prefs.getString(KeyEntries, null))
            val updated = (existing + entry).takeLast(ExamStallLogMaxEntries)
            // commit: a restart follows right after this write and must not lose it.
            prefs.edit(commit = true) { putString(KeyEntries, encodeExamStallLog(updated)) }
        }
    }

    fun clear(context: Context) {
        prefs(context).edit(commit = true) { remove(KeyEntries) }
    }

    fun markRestartPending(context: Context, wallClockMs: Long) {
        prefs(context).edit(commit = true) { putLong(KeyRestartPendingAt, wallClockMs) }
    }

    /** When the app restarted itself and no page has loaded since; cleared by reading. */
    fun takeRestartPending(context: Context): Long? {
        val prefs = prefs(context)
        val at = prefs.getLong(KeyRestartPendingAt, 0L).takeIf { it > 0L } ?: return null
        prefs.edit { remove(KeyRestartPendingAt) }
        return at
    }

    /**
     * The next exam start after a stall restart keeps the WebView session (login cookie,
     * localStorage, IndexedDB) instead of wiping it, once, within [keepForMillis]. The
     * same student continues on the same phone: CBX Exam System ties a running exam to
     * the device id it keeps in localStorage ("cbx_exam_device_id") and refuses a new one
     * ("Akun ini sedang dipakai di perangkat lain"), and answers not yet sent wait in
     * IndexedDB. Wiping them turned "restart the app" into "call the proctor".
     */
    fun markKeepWebViewSession(context: Context, nowMillis: Long, keepForMillis: Long = KeepSessionMillis) {
        prefs(context).edit(commit = true) { putLong(KeyKeepSessionUntil, nowMillis + keepForMillis) }
    }

    /** True once if a stall restart asked to keep the session and it has not expired. */
    fun takeKeepWebViewSession(context: Context, nowMillis: Long): Boolean {
        val prefs = prefs(context)
        val until = prefs.getLong(KeyKeepSessionUntil, 0L)
        if (until <= 0L) return false
        prefs.edit(commit = true) { remove(KeyKeepSessionUntil) }
        return shouldKeepWebViewSession(until, nowMillis)
    }

    private const val KeyKeepSessionUntil = "keep_session_until"
    private const val KeepSessionMillis = 30 * 60_000L
}

internal fun shouldKeepWebViewSession(keepUntilMillis: Long, nowMillis: Long): Boolean =
    keepUntilMillis > 0L && nowMillis < keepUntilMillis

internal fun encodeExamStallLog(entries: List<ExamStallLogEntry>): String =
    JSONArray().apply {
        entries.forEach { entry ->
            put(
                JSONObject()
                    .put("kind", entry.kind.code)
                    .put("at", entry.wallClockMs)
                    .put("note", entry.note)
                    .apply { entry.report?.let { put("report", encodeExamStallReport(it)) } }
            )
        }
    }.toString()

internal fun decodeExamStallLog(raw: String?): List<ExamStallLogEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        val json = array.optJSONObject(index) ?: return@mapNotNull null
        val kind = ExamStallLogKind.fromCode(json.optString("kind")) ?: return@mapNotNull null
        ExamStallLogEntry(
            kind = kind,
            wallClockMs = json.optLong("at"),
            report = json.optJSONObject("report")?.let(::decodeExamStallReport),
            note = json.optString("note")
        )
    }
}

private fun encodeExamStallReport(report: ExamStallReport): JSONObject =
    JSONObject()
        .put("attempt", report.attempt)
        .put("at", report.wallClockMs)
        .put("examHost", report.examHost ?: JSONObject.NULL)
        .put(
            "page",
            JSONObject()
                .put("ready", report.page.readyState)
                .put("resources", report.page.resources)
                .put("finished", report.page.finished)
                .put("pending", JSONArray(report.page.pending))
                .put("pendingCount", report.page.pendingCount)
                .put("probeError", report.page.probeError ?: JSONObject.NULL)
        )
        .put(
            "network",
            JSONObject()
                .put("transport", report.network.transport)
                .put("validated", report.network.validated ?: JSONObject.NULL)
                .put("dns", JSONArray(report.network.dnsServers))
                .put("privateDns", report.network.privateDns)
                .put("ipv6", report.network.ipv6 ?: JSONObject.NULL)
                .put(
                    "lookups",
                    JSONArray().apply {
                        report.network.lookups.forEach { (host, outcome) -> put(JSONArray(listOf(host, outcome))) }
                    }
                )
        )

private fun decodeExamStallReport(json: JSONObject): ExamStallReport {
    val page = json.optJSONObject("page") ?: JSONObject()
    val network = json.optJSONObject("network") ?: JSONObject()
    fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key)
    fun JSONObject.optNullableBoolean(key: String): Boolean? =
        if (isNull(key) || !has(key)) null else optBoolean(key)
    fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }
    val lookups = network.optJSONArray("lookups")
    return ExamStallReport(
        attempt = json.optInt("attempt"),
        wallClockMs = json.optLong("at"),
        examHost = json.optNullableString("examHost"),
        page = ExamStallPageEvidence(
            readyState = page.optString("ready", "-"),
            resources = page.optInt("resources"),
            finished = page.optInt("finished"),
            pending = page.optJSONArray("pending").strings(),
            pendingCount = page.optInt("pendingCount"),
            probeError = page.optNullableString("probeError")
        ),
        network = ExamStallNetworkEvidence(
            transport = network.optString("transport", "-"),
            validated = network.optNullableBoolean("validated"),
            dnsServers = network.optJSONArray("dns").strings(),
            privateDns = network.optString("privateDns", "-"),
            ipv6 = network.optNullableBoolean("ipv6"),
            lookups = if (lookups == null) {
                emptyList()
            } else {
                (0 until lookups.length()).mapNotNull { index ->
                    val pair = lookups.optJSONArray(index) ?: return@mapNotNull null
                    pair.optString(0) to pair.optString(1)
                }
            }
        )
    )
}
