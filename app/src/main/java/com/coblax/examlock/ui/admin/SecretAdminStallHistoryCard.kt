package com.coblax.examlock.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.persistence.ExamStallLogEntry
import com.coblax.examlock.persistence.ExamStallLogKind
import com.coblax.examlock.persistence.ExamStallLogStore
import com.coblax.examlock.ui.exam.buildExamStallSummaryLines
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.UiStatusTone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val SecretAdminStallHistoryTag = "secret_admin_stall_history"

/**
 * Exam pages that stalled on this phone, the restarts that followed and whether a page
 * loaded after them. Admin only, so the full line with the exam host and its addresses
 * is shown; Copy puts the whole history on the clipboard for a chat to the developer.
 */
@Composable
internal fun SecretAdminStallHistoryCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val language = LocalUiLanguage.current
    var entries by remember { mutableStateOf(ExamStallLogStore.read(context)) }
    var copied by remember { mutableStateOf(false) }

    SecretAdminSection(
        title = tr("Stalled exam pages", "Riwayat halaman macet"),
        modifier = modifier.testTag(SecretAdminStallHistoryTag)
    ) {
        if (entries.isEmpty()) {
            SecretAdminNote(
                text = tr(
                    "No exam page has stalled on this phone.",
                    "Belum ada halaman ujian yang macet di HP ini."
                ),
                tone = UiStatusTone.Success
            )
            return@SecretAdminSection
        }
        SecretAdminNote(
            text = tr(
                "Newest first. Stalls, app restarts after them, and the first page that loaded after a restart.",
                "Terbaru di atas. Macet, mulai ulang aplikasi sesudahnya, dan halaman pertama yang terbuka setelah mulai ulang."
            )
        )
        entries.forEach { entry -> StallHistoryEntry(entry, language) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                copyToClipboard(context, formatExamStallHistoryForCopy(entries, language))
                copied = true
            }) {
                Text(if (copied) tr("Copied", "Tersalin") else tr("Copy all", "Salin semua"), style = AppTextStyles.button)
            }
            TextButton(onClick = {
                ExamStallLogStore.clear(context)
                entries = emptyList()
            }) {
                Text(tr("Clear", "Hapus"), style = AppTextStyles.button)
            }
        }
    }
}

@Composable
private fun StallHistoryEntry(entry: ExamStallLogEntry, language: UiLanguage) {
    val colors = AppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSoft)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = stallHistoryHeading(entry, language),
            color = colors.textPrimary,
            style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold)
        )
        stallHistoryBody(entry, language).forEach { line ->
            Text(text = line, color = colors.textSecondary, style = AppTextStyles.diagnostic)
        }
    }
}

private fun stallHistoryTime(wallClockMs: Long): String =
    SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ROOT).format(Date(wallClockMs))

internal fun stallHistoryHeading(entry: ExamStallLogEntry, language: UiLanguage): String {
    val kind = when (entry.kind) {
        ExamStallLogKind.Stall -> localized(language, "Stall", "Macet") +
            (entry.report?.attempt?.let { " #$it" } ?: "")
        ExamStallLogKind.Restart -> localized(language, "App restarted", "Aplikasi dimulai ulang")
        ExamStallLogKind.Recovered -> localized(language, "Page loaded after restart", "Halaman terbuka setelah mulai ulang")
    }
    return "${stallHistoryTime(entry.wallClockMs)} · $kind"
}

/** The readable summary, then the full admin line (host and addresses included). */
internal fun stallHistoryBody(entry: ExamStallLogEntry, language: UiLanguage): List<String> {
    val report = entry.report
    return if (report != null) {
        val summary = buildExamStallSummaryLines(
            report = report,
            language = language,
            clockText = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date(report.wallClockMs))
        ).drop(1)
        summary + "EXAM_PAGE_STALLED: ${report.adminDetail}"
    } else {
        listOfNotNull(entry.note.takeIf { it.isNotBlank() })
    }
}

internal fun formatExamStallHistoryForCopy(entries: List<ExamStallLogEntry>, language: UiLanguage): String =
    entries.joinToString("\n\n") { entry ->
        (listOf(stallHistoryHeading(entry, language)) + stallHistoryBody(entry, language)).joinToString("\n")
    }

private fun copyToClipboard(context: Context, text: String) {
    runCatching {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("CBX stall history", text))
    }
}
