package com.coblax.examlock.ui.preparation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.StatusSummaryCard
import com.coblax.examlock.ui.theme.UiStatusTone

@Composable
internal fun PreparationCelebrationBanner(
    modifier: Modifier = Modifier
) {
    StatusSummaryCard(
        title = tr("Final readiness summary", "Ringkasan kesiapan akhir"),
        message = tr(
            "All required checks passed. Your device is ready for the exam.",
            "Semua pemeriksaan wajib lulus. Perangkat siap digunakan untuk ujian."
        ),
        tone = UiStatusTone.Success,
        modifier = modifier.testTag(PreparationWizardUiTestTags.FinalSummary),
        statusLabel = tr("Ready", "Siap"),
        supportingText = tr(
            "Review the status, then select Start exam below.",
            "Tinjau status, lalu pilih Mulai ujian di bawah."
        )
    )
}
