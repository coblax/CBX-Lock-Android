package com.coblax.examlock.ui.admin

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.webkit.WebViewCompat


import com.coblax.examlock.DeviceCompatibilityProfile
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.R
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.WebViewHealthSeverity
import com.coblax.examlock.WebViewHealthVerdict
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBadge
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.primaryActionColors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt


@Composable
internal fun AdminReadinessSummaryCard(
    summary: AdminReadinessSummary,
    fieldReadinessRunning: Boolean,
    webViewStatus: WebViewCompatibilityStatus,
    onRunCheck: () -> Unit,
    onOpenWebViewSettings: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    val tokens = ScopedUiTokens.current
    val tone = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> UiStatusTone.Info
        AdminReadinessVerdict.Ready -> UiStatusTone.Success
        AdminReadinessVerdict.NeedsSetup -> UiStatusTone.Warning
        AdminReadinessVerdict.Blocked -> UiStatusTone.Danger
    }
    val primaryClick = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> onRunCheck
        AdminReadinessVerdict.Ready -> onOpenAdvanced
        AdminReadinessVerdict.NeedsSetup,
        AdminReadinessVerdict.Blocked -> {
            if (webViewStatus.severity != WebViewHealthSeverity.Stable) {
                onOpenWebViewSettings
            } else {
                onOpenAdvanced
            }
        }
    }
    // The summary builder's labels are fixed strings shared with diagnostics; the card
    // shows them in the UI language instead.
    val badgeLabel = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> tr("Not checked", "Belum dicek")
        AdminReadinessVerdict.Ready -> tr("Ready", "Siap")
        AdminReadinessVerdict.NeedsSetup -> tr("Needs setup", "Perlu setup")
        AdminReadinessVerdict.Blocked -> tr("Blocked", "Terblokir")
    }
    val detail = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> tr(
            "Run the check on the real device before exam day.",
            "Jalankan pemeriksaan di perangkat asli sebelum hari ujian."
        )
        AdminReadinessVerdict.Ready -> tr(
            "The device is ready for a field test.",
            "Perangkat siap untuk uji lapangan."
        )
        AdminReadinessVerdict.NeedsSetup -> tr(
            "Some items need a look; only security blockers stop Start Exam.",
            "Ada yang perlu dicek; hanya blocker keamanan yang menghentikan Mulai Ujian."
        )
        AdminReadinessVerdict.Blocked -> tr(
            "A security blocker must be fixed before the exam.",
            "Ada blocker keamanan yang harus dibereskan sebelum ujian."
        )
    }
    val webViewLabel = when (webViewStatus.verdict) {
        WebViewHealthVerdict.Ready -> tr("Ready", "Siap")
        WebViewHealthVerdict.NeedsUpdate -> tr("Needs update", "Perlu update")
        WebViewHealthVerdict.Unavailable -> tr("Unavailable", "Tidak tersedia")
        WebViewHealthVerdict.Unknown -> tr("Unknown", "Tidak diketahui")
    }
    val nextActionLabel = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> tr("Run check", "Jalankan cek")
        AdminReadinessVerdict.Ready -> tr("View details", "Lihat detail")
        AdminReadinessVerdict.NeedsSetup,
        AdminReadinessVerdict.Blocked -> tr("Fix first", "Perbaiki dulu")
    }
    val primary = primaryActionColors()
    SecretAdminSection(
        title = tr("Device readiness", "Kesiapan perangkat"),
        trailing = { StatusBadge(label = badgeLabel, tone = tone) }
    ) {
        Text(
            text = detail,
            color = AppColors.current.textSecondary,
            style = AppTextStyles.diagnostic
        )
        Column {
            SecretAdminInfoRow(label = "WebView", value = webViewLabel)
            SecretAdminInfoRow(label = "Vendor", value = summary.vendorLabel)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = primaryClick,
                enabled = !fieldReadinessRunning,
                shape = RoundedCornerShape(tokens.radiusMedium),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary.container,
                    contentColor = primary.content,
                    disabledContainerColor = AppColors.current.surfaceSoft,
                    disabledContentColor = AppColors.current.textSecondary
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = tokens.touchTarget)
            ) {
                if (fieldReadinessRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.current.textSecondary
                    )
                } else {
                    Text(text = nextActionLabel, style = AppTextStyles.button)
                }
            }
            if (summary.verdict != AdminReadinessVerdict.NotRun) {
                TextButton(
                    onClick = onRunCheck,
                    enabled = !fieldReadinessRunning,
                    modifier = Modifier.heightIn(min = tokens.touchTarget)
                ) {
                    Text(
                        text = tr("Run again", "Cek ulang"),
                        color = AppColors.current.brandText,
                        style = AppTextStyles.button
                    )
                }
            }
        }
    }
}

@Composable
internal fun AdminAdvancedDiagnosticsCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    report: FieldReadinessReport?,
    survivalPolicy: DeviceSurvivalPolicy,
    webViewStatus: WebViewCompatibilityStatus,
    vendorChecklist: DeviceVendorChecklist,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    onRefreshWebView: () -> Unit,
    onOpenWebViewSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    SecretAdminSection(
        title = tr("Advanced diagnostics", "Diagnostik lanjutan"),
        trailing = {
            TextButton(
                onClick = onToggleExpanded,
                modifier = Modifier.heightIn(min = ScopedUiTokens.current.touchTarget)
            ) {
                Text(
                    text = if (expanded) tr("Hide", "Tutup") else tr("Open", "Buka"),
                    color = AppColors.current.brandText,
                    style = AppTextStyles.button
                )
            }
        }
    ) {
        if (!expanded) {
            Text(
                text = tr(
                    "WebView provider, field test results, and the vendor setup checklist. For troubleshooting.",
                    "Provider WebView, hasil field test, dan checklist setup vendor. Untuk troubleshooting."
                ),
                color = AppColors.current.textSecondary,
                style = AppTextStyles.diagnostic
            )
            return@SecretAdminSection
        }

        AdminDiagnosticSectionTitle(tr("WebView Provider", "Provider WebView"))
        AdminHealthLine(tr("Status", "Status"), "${webViewStatus.verdict.name} / ${webViewStatus.severity.name}")
        AdminHealthLine(tr("Provider", "Provider"), webViewStatus.providerLabel)
        AdminHealthLine(tr("Package", "Package"), webViewStatus.packageName)
        AdminHealthLine(tr("Version", "Versi"), webViewStatus.versionLabel)
        AdminHealthLine(tr("Source", "Sumber"), webViewStatus.providerSource)
        AdminHealthLine(
            tr("Survival score", "Skor survival"),
            "${survivalPolicy.score.name} / ${survivalPolicy.runtimeTier.name}"
        )
        webViewStatus.quickFix?.takeIf { it.isNotBlank() }?.let { quickFix ->
            Text(
                text = quickFix,
                color = AppColors.current.textSecondary,
                style = AppTextStyles.diagnostic
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminLinkButton(tr("Refresh", "Refresh"), onRefreshWebView, Modifier.weight(1f))
            AdminLinkButton(tr("Open Settings", "Buka Setelan"), onOpenWebViewSettings, Modifier.weight(1f))
        }

        AdminDiagnosticDivider()
        AdminDiagnosticSectionTitle(tr("Field Readiness Details", "Detail Field Readiness"))
        FieldReadinessReportCard(
            report = report,
            survivalPolicy = survivalPolicy
        )

        AdminDiagnosticDivider()
        AdminDiagnosticSectionTitle(tr("Device Setup Checklist", "Checklist Setup Perangkat"))
        AdminHealthLine(
            label = tr("Vendor", "Vendor"),
            value = vendorChecklist.displayName
        )
        AdminHealthLine(
            label = tr("Compatibility", "Kompatibilitas"),
            value = "${deviceCompatibilityProfile.family.name} | ${deviceCompatibilityProfile.model}"
        )
        vendorChecklist.items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.bodyCompact.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = item.detail,
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminLinkButton(tr("Battery", "Baterai"), onOpenBatterySettings, Modifier.weight(1f))
            AdminLinkButton(tr("Location", "Lokasi"), onOpenLocationSettings, Modifier.weight(1f))
            AdminLinkButton(tr("Floating Apps", "Floating App"), onOpenOverlaySettings, Modifier.weight(1f))
        }
        AdminLinkButton(
            tr("Open App Settings", "Buka Setelan Aplikasi"),
            onOpenAppSettings,
            Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdminLinkButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ScopedUiTokens.current.touchTarget)
    ) {
        Text(
            text = label,
            color = AppColors.current.brandText,
            style = AppTextStyles.button,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun AdminDiagnosticSectionTitle(text: String) {
    Text(
        text = text,
        color = AppColors.current.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun AdminDiagnosticDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColors.current.outline.copy(alpha = 0.8f))
    )
}

@Composable
private fun AdminHealthLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = AppColors.current.textMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value.ifBlank { "-" },
            color = AppColors.current.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun FieldReadinessReportCard(
    report: FieldReadinessReport?,
    survivalPolicy: DeviceSurvivalPolicy
) {
    if (report == null) {
        Text(
            text = tr(
                "No field test yet. Run it on the actual device before exam day.",
                "Belum ada field test. Jalankan di perangkat asli sebelum hari ujian."
            ),
            color = AppColors.current.textSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        return
    }

    val statusColor = fieldReadinessVerdictColor(report.finalVerdict)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr("Last result", "Hasil terakhir"),
                color = AppColors.current.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "ready=${report.readyCount} warning=${report.warningCount} blocked=${report.blockedCount}",
                color = AppColors.current.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text = "score=${survivalPolicy.score.name} runtime=${survivalPolicy.runtimeTier.name}",
                color = AppColors.current.textMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Text(
                text = survivalPolicy.webViewRiskLabel,
                color = AppColors.current.textMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        Surface(
            shape = RoundedCornerShape(UiTokens.RadiusPill),
            color = statusColor.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.36f))
        ) {
            Text(
                text = report.finalVerdict.name.uppercase(),
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        report.items.forEach { item ->
            val itemColor = fieldReadinessVerdictColor(item.verdict)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.verdict.name.take(1),
                    color = itemColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = AppColors.current.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.detail,
                        color = AppColors.current.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    if (!item.quickFix.isNullOrBlank()) {
                        Text(
                            text = item.quickFix,
                            color = itemColor,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldReadinessVerdictColor(verdict: FieldReadinessVerdict): Color {
    return when (verdict) {
        FieldReadinessVerdict.Ready -> AppColors.current.safeEmphasis
        FieldReadinessVerdict.Warning -> AppColors.current.goldDark
        FieldReadinessVerdict.Blocked -> AppColors.current.dialogDangerIcon
    }
}

internal fun readSecretAdminLockTaskStateLabel(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return "Unsupported"
    }
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val state = runCatching { activityManager?.lockTaskModeState }.getOrNull()
    return when (state) {
        ActivityManager.LOCK_TASK_MODE_LOCKED -> "LOCKED"
        ActivityManager.LOCK_TASK_MODE_PINNED -> "PINNED"
        ActivityManager.LOCK_TASK_MODE_NONE -> "NONE"
        null -> "Unknown"
        else -> "Unknown($state)"
    }
}

