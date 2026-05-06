package com.example.coblaxexamlock

import android.content.Context
import android.os.SystemClock
import java.util.Locale

internal const val PreviousExamSessionBreadcrumbMaxBytes = 16 * 1024

internal object PreviousExamSessionBreadcrumbCodes {
    const val PreparationOpened = "preparation_opened"
    const val StartPressed = "start_pressed"
    const val ScreenPinningActive = "screen_pinning_active"
    const val ScreenPinningSkipped = "screen_pinning_skipped"
    const val WebViewCreated = "webview_created"
    const val RendererGone = "renderer_gone"
    const val CleanupStarted = "cleanup_started"
    const val CleanupSucceeded = "cleanup_succeeded"
    const val CleanupTimeout = "cleanup_timeout"
    const val ExitCompleted = "exit_completed"
}

internal data class PreviousExamSessionBreadcrumbEntry(
    val code: String,
    val details: String,
    val elapsedRealtimeMs: Long,
    val wallClockMs: Long
)

internal data class PreviousExamSessionBreadcrumb(
    val entries: List<PreviousExamSessionBreadcrumbEntry>
) {
    val latestRecoveryHint: String?
        get() {
            val lastRecovery = entries.lastOrNull {
                it.code == PreviousExamSessionBreadcrumbCodes.RendererGone ||
                    it.code == PreviousExamSessionBreadcrumbCodes.CleanupTimeout
            } ?: return null
            val lastCleanExit = entries.lastOrNull {
                it.code == PreviousExamSessionBreadcrumbCodes.CleanupSucceeded ||
                    it.code == PreviousExamSessionBreadcrumbCodes.ExitCompleted
            }
            if (lastCleanExit != null && lastCleanExit.elapsedRealtimeMs >= lastRecovery.elapsedRealtimeMs) {
                return null
            }
            return when (lastRecovery.code) {
                PreviousExamSessionBreadcrumbCodes.RendererGone ->
                    "Previous exam browser renderer stopped; CBX cleaned the session and is ready for manual retry."
                PreviousExamSessionBreadcrumbCodes.CleanupTimeout ->
                    "Previous cleanup timed out; CBX continued safe exit and will prepare a clean session before retry."
                else -> null
            }
        }

    fun diagnosticSummary(): String {
        return entries
            .takeLast(8)
            .joinToString(",") { entry -> entry.code }
            .ifBlank { "-" }
    }

    fun toRedactedText(): String {
        return entries.joinToString("\n") { entry ->
            "${entry.wallClockMs} ${entry.code}: ${entry.details}"
        }.ifBlank { "-" }
    }
}

internal fun appendPreviousExamSessionBreadcrumb(
    existingEntries: List<PreviousExamSessionBreadcrumbEntry>,
    newEntry: PreviousExamSessionBreadcrumbEntry,
    maxBytes: Int = PreviousExamSessionBreadcrumbMaxBytes
): List<PreviousExamSessionBreadcrumbEntry> {
    val sanitizedEntry = newEntry.copy(
        code = sanitizeBreadcrumbCode(newEntry.code),
        details = sanitizePreviousExamSessionBreadcrumbDetails(newEntry.details)
    )
    val result = existingEntries.toMutableList()
    result += sanitizedEntry
    while (encodePreviousExamSessionBreadcrumb(result).toByteArray(Charsets.UTF_8).size > maxBytes && result.isNotEmpty()) {
        result.removeAt(0)
    }
    return result
}

internal fun sanitizePreviousExamSessionBreadcrumbDetails(
    details: String,
    maxLength: Int = 240
): String {
    val noControl = details
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace('\t', ' ')
    return redactBreadcrumbSensitiveText(noControl)
        .trim()
        .ifBlank { "-" }
        .take(maxLength)
}

internal object PreviousExamSessionBreadcrumbStore {
    private const val PreferencesName = "cbx_previous_exam_session_breadcrumb"
    private const val EntriesKey = "entries"

    fun read(context: Context): PreviousExamSessionBreadcrumb {
        val raw = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getString(EntriesKey, "")
            .orEmpty()
        return PreviousExamSessionBreadcrumb(decodePreviousExamSessionBreadcrumb(raw))
    }

    fun append(
        context: Context,
        code: String,
        details: String = "-"
    ): PreviousExamSessionBreadcrumb {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val currentEntries = decodePreviousExamSessionBreadcrumb(
            preferences.getString(EntriesKey, "").orEmpty()
        )
        val updatedEntries = appendPreviousExamSessionBreadcrumb(
            existingEntries = currentEntries,
            newEntry = PreviousExamSessionBreadcrumbEntry(
                code = code,
                details = details,
                elapsedRealtimeMs = SystemClock.elapsedRealtime(),
                wallClockMs = System.currentTimeMillis()
            )
        )
        preferences.edit()
            .putString(EntriesKey, encodePreviousExamSessionBreadcrumb(updatedEntries))
            .apply()
        return PreviousExamSessionBreadcrumb(updatedEntries)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .remove(EntriesKey)
            .apply()
    }
}

private fun sanitizeBreadcrumbCode(code: String): String {
    return code
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9_]+"), "_")
        .trim('_')
        .ifBlank { "event" }
        .take(64)
}

private fun redactBreadcrumbSensitiveText(text: String): String {
    var redacted = text.replace(Regex("https?://([^\\s/?#]+)[^\\s]*", RegexOption.IGNORE_CASE)) {
        "${it.value.substringBefore("://")}://${it.groupValues[1]}/..."
    }
    redacted = redacted.replace(
        Regex("(?i)\\b(token|password|passwd|pass|payload|qr_payload|secret)=([^\\s|,;]+)")
    ) { matchResult ->
        "${matchResult.groupValues[1]}=<redacted>"
    }
    redacted = redacted.replace(
        Regex("(?i)\\b(ssid|bssid)=([^\\s|,;]+)")
    ) { matchResult ->
        "${matchResult.groupValues[1]}=<redacted>"
    }
    redacted = redacted.replace(
        Regex("\\b[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}\\b"),
        "<bssid-redacted>"
    )
    redacted = redacted.replace(
        Regex("(?i)\\b(lat|latitude|lng|lon|longitude)=\\s*[-+]?\\d{1,3}\\.\\d+"),
    ) { matchResult ->
        "${matchResult.groupValues[1]}=<coord-redacted>"
    }
    return redacted.replace(
        Regex("(?<![A-Za-z0-9])[-+]?\\d{1,3}\\.\\d{4,}(?![A-Za-z0-9])"),
        "<coord-redacted>"
    )
}

private fun encodePreviousExamSessionBreadcrumb(
    entries: List<PreviousExamSessionBreadcrumbEntry>
): String {
    return entries.joinToString("\n") { entry ->
        listOf(
            entry.elapsedRealtimeMs.toString(),
            entry.wallClockMs.toString(),
            sanitizeBreadcrumbCode(entry.code),
            sanitizePreviousExamSessionBreadcrumbDetails(entry.details)
        ).joinToString("\t")
    }
}

private fun decodePreviousExamSessionBreadcrumb(raw: String): List<PreviousExamSessionBreadcrumbEntry> {
    return raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.split('\t', limit = 4)
            if (parts.size != 4) {
                return@mapNotNull null
            }
            PreviousExamSessionBreadcrumbEntry(
                elapsedRealtimeMs = parts[0].toLongOrNull() ?: 0L,
                wallClockMs = parts[1].toLongOrNull() ?: 0L,
                code = sanitizeBreadcrumbCode(parts[2]),
                details = sanitizePreviousExamSessionBreadcrumbDetails(parts[3])
            )
        }
        .toList()
}
