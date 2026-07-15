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
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockGoldDark
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockDialogDangerIcon
import com.coblax.examlock.ui.theme.LockSafeEmphasis
import com.coblax.examlock.WebViewCompatibilityStatus
import com.coblax.examlock.WebViewHealthSeverity
import com.coblax.examlock.ui.theme.UiTokens
import com.google.android.gms.tasks.Task

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
    val statusColor = adminReadinessVerdictColor(summary.verdict)
    val securityLabel = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> tr("Not checked", "Belum dicek")
        AdminReadinessVerdict.Ready -> tr("Ready", "Siap")
        AdminReadinessVerdict.NeedsSetup -> tr("Need Check", "Perlu Dicek")
        AdminReadinessVerdict.Blocked -> tr("Blocked", "Terblokir")
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.24f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Device Readiness", "Kesiapan Perangkat"),
                        color = LockTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary.detail,
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(UiTokens.RadiusPill),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = summary.title,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminHealthLine(
                    label = tr("WebView", "WebView"),
                    value = summary.webViewLabel
                )
                AdminHealthLine(
                    label = tr("Security", "Keamanan"),
                    value = securityLabel
                )
                AdminHealthLine(
                    label = tr("Vendor", "Vendor"),
                    value = summary.vendorLabel
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = primaryClick,
                    enabled = !fieldReadinessRunning,
                    shape = RoundedCornerShape(UiTokens.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = LockOnDark,
                        disabledContainerColor = statusColor.copy(alpha = 0.42f),
                        disabledContentColor = LockOnDark.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (fieldReadinessRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LockOnDark
                        )
                    } else {
                        Text(
                            text = summary.nextActionLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (summary.verdict != AdminReadinessVerdict.NotRun) {
                    TextButton(
                        onClick = onRunCheck,
                        enabled = !fieldReadinessRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = tr("Run Check", "Cek Ulang"),
                            color = LockBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(
                    onClick = onOpenAdvanced,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tr("Details", "Detail"),
                        color = LockBlue,
                        fontWeight = FontWeight.Bold
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Advanced Diagnostics", "Diagnostik Lanjutan"),
                        color = LockTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Technical details are hidden until needed.",
                            "Detail teknis disembunyikan sampai dibutuhkan."
                        ),
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                TextButton(onClick = onToggleExpanded) {
                    Text(
                        text = if (expanded) tr("Hide", "Tutup") else tr("Open", "Buka"),
                        color = LockBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!expanded) {
                Text(
                    text = tr(
                        "Open only for troubleshooting.",
                        "Buka hanya saat troubleshooting."
                    ),
                    color = LockTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                return@Column
            }

            AdminDiagnosticDivider()
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
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onRefreshWebView,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Refresh", "Refresh"), color = LockBlue, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onOpenWebViewSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Open Settings", "Buka Setelan"), color = LockBlue, fontWeight = FontWeight.Bold)
                }
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
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.title,
                        color = LockTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.detail,
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Battery", "Baterai"), color = LockBlue)
                }
                TextButton(
                    onClick = onOpenLocationSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Location", "Lokasi"), color = LockBlue)
                }
                TextButton(
                    onClick = onOpenOverlaySettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Floating Apps", "Floating App"), color = LockBlue)
                }
            }
            TextButton(
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr("Open App Settings", "Buka Setelan Aplikasi"),
                    color = LockBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AdminDiagnosticSectionTitle(text: String) {
    Text(
        text = text,
        color = LockTextPrimary,
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
            .background(LockOutline.copy(alpha = 0.8f))
    )
}

private fun adminReadinessVerdictColor(verdict: AdminReadinessVerdict): Color {
    return when (verdict) {
        AdminReadinessVerdict.NotRun -> LockBlue
        AdminReadinessVerdict.Ready -> LockSafeEmphasis
        AdminReadinessVerdict.NeedsSetup -> LockGoldDark
        AdminReadinessVerdict.Blocked -> LockDialogDangerIcon
    }
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
            color = LockTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value.ifBlank { "-" },
            color = LockTextPrimary,
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
            color = LockTextSecondary,
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
                color = LockTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "ready=${report.readyCount} warning=${report.warningCount} blocked=${report.blockedCount}",
                color = LockTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text = "score=${survivalPolicy.score.name} runtime=${survivalPolicy.runtimeTier.name}",
                color = LockTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Text(
                text = survivalPolicy.webViewRiskLabel,
                color = LockTextMuted,
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
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.detail,
                        color = LockTextSecondary,
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

private fun fieldReadinessVerdictColor(verdict: FieldReadinessVerdict): Color {
    return when (verdict) {
        FieldReadinessVerdict.Ready -> LockSafeEmphasis
        FieldReadinessVerdict.Warning -> LockGoldDark
        FieldReadinessVerdict.Blocked -> LockDialogDangerIcon
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

