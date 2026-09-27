package com.coblax.examlock.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coblax.examlock.ExamQrSecurityBypass
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.AdminSettings
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UiStatusTone

internal object CustomQrBypassUiTestTags {
    const val Section = "custom_qr_bypass_section"
    const val EditAction = "custom_qr_bypass_edit"
    const val Picker = "custom_qr_bypass_picker"
    const val DoneAction = "custom_qr_bypass_done"
    const val ClearAction = "custom_qr_bypass_clear"
    const val RowPrefix = "custom_qr_bypass_"
}

/**
 * The checks this QR turns off, as one row on the Generate step. The list itself opens in
 * a picker so the step stays short and the generated QR stays in view.
 */
@Composable
internal fun CustomQrBypassSection(
    selected: Set<ExamQrSecurityBypass>,
    onSelectedChange: (Set<ExamQrSecurityBypass>) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val uiLanguage = LocalUiLanguage.current
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val selectedTitles = remember(selected, uiLanguage) { examQrBypassTitles(uiLanguage, selected) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CustomQrBypassUiTestTags.Section),
        shape = RoundedCornerShape(tokens.radiusMedium),
        color = colors.cardBg,
        border = BorderStroke(
            1.dp,
            if (selected.isEmpty()) colors.outlineStrong else colors.gold.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = tr("Relax security checks", "Longgarkan pengamanan"),
                        color = colors.textPrimary,
                        style = AppTextStyles.cardTitle
                    )
                    Text(
                        text = if (selectedTitles.isEmpty()) {
                            tr("None. Every check applies.", "Tidak ada. Semua pemeriksaan berlaku.")
                        } else {
                            selectedTitles.joinToString()
                        },
                        color = if (selectedTitles.isEmpty()) colors.textSecondary else colors.goldDark,
                        style = AppTextStyles.bodyCompact
                    )
                }
                TextButton(
                    onClick = { pickerOpen = true },
                    modifier = Modifier
                        .heightIn(min = tokens.touchTarget)
                        .testTag(CustomQrBypassUiTestTags.EditAction)
                ) {
                    Text(
                        text = if (selected.isEmpty()) tr("Choose", "Pilih") else tr("Change", "Ubah"),
                        color = colors.brandText,
                        style = AppTextStyles.button
                    )
                }
            }
            if (selected.isNotEmpty()) {
                SecretAdminNote(
                    text = tr(
                        "Everyone who scans this QR runs this exam without these checks. Detections are still logged. Students need CBX Lock 3.2.51 or newer.",
                        "Semua siswa yang memindai QR ini menjalankan ujian tanpa pemeriksaan ini. Deteksi tetap dicatat. Siswa perlu CBX Lock 3.2.51 atau lebih baru."
                    ),
                    tone = UiStatusTone.Warning
                )
            }
        }
    }

    if (pickerOpen) {
        CustomQrBypassPicker(
            selected = selected,
            onSelectedChange = onSelectedChange,
            onDismiss = { pickerOpen = false }
        )
    }
}

/** The same grouped switches as Secret Admin, choosing what this QR relaxes. */
@Composable
private fun CustomQrBypassPicker(
    selected: Set<ExamQrSecurityBypass>,
    onSelectedChange: (Set<ExamQrSecurityBypass>) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = ScopedUiTokens.current
    val colors = AppColors.current
    val uiLanguage = LocalUiLanguage.current
    val groups = remember(uiLanguage) { securityOverrideGroups(uiLanguage, AdminSettings()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .testTag(CustomQrBypassUiTestTags.Picker)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Relax security checks", "Longgarkan pengamanan"),
                        color = colors.textPrimary,
                        style = AppTextStyles.sectionTitle
                    )
                    Text(
                        text = if (selected.isEmpty()) {
                            tr("Nothing selected", "Belum ada yang dipilih")
                        } else {
                            tr("${selected.size} selected", "${selected.size} dipilih")
                        },
                        color = if (selected.isEmpty()) colors.textSecondary else colors.goldDark,
                        style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .heightIn(min = tokens.touchTarget)
                        .testTag(CustomQrBypassUiTestTags.DoneAction)
                ) {
                    Text(
                        text = tr("Done", "Selesai"),
                        color = colors.brandText,
                        style = AppTextStyles.button
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = SecretAdminContentMaxWidth)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SecretAdminNote(
                        text = tr(
                            "Chosen checks are off only for the exam opened from this QR. The device's own Secret Admin settings do not change.",
                            "Pemeriksaan yang dipilih hanya mati untuk ujian dari QR ini. Pengaturan Secret Admin di HP siswa tidak berubah."
                        )
                    )
                    groups.forEach { group ->
                        SecretAdminSection(title = group.title, padded = false) {
                            group.items.forEachIndexed { index, item ->
                                val bypass = ExamQrSecurityBypass.fromKey(item.key)
                                    ?: return@forEachIndexed
                                if (index > 0) SecretAdminRowDivider()
                                val on = bypass in selected
                                SecretAdminSwitchRow(
                                    title = item.title,
                                    description = item.description,
                                    checked = on,
                                    highlighted = on,
                                    testTag = CustomQrBypassUiTestTags.RowPrefix + item.key,
                                    onCheckedChange = { enable ->
                                        onSelectedChange(if (enable) selected + bypass else selected - bypass)
                                    }
                                )
                            }
                        }
                    }
                    if (selected.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onSelectedChange(emptySet()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = tokens.touchTarget)
                                .testTag(CustomQrBypassUiTestTags.ClearAction),
                            shape = RoundedCornerShape(tokens.radiusMedium),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brandText),
                            border = BorderStroke(1.dp, colors.outline)
                        ) {
                            Text(text = tr("Clear all", "Hapus semua pilihan"), style = AppTextStyles.button)
                        }
                    }
                }
            }
        }
    }
}
