package com.example.coblaxexamlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.CompatibilityScore
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.PreviousExamSessionBreadcrumb
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

@Composable
internal fun PreExamHealthCheckCard(
    snapshot: PreExamHealthSnapshot,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onFixWebViewProvider: () -> Unit
) {
    val accentColor = when {
        snapshot.blockingCount > 0 -> Color(0xFFB34A4A)
        snapshot.warningCount > 0 -> LockGoldDark
        else -> Color(0xFF1F7A4D)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Pre-Exam Health Check", "Health Check Sebelum Ujian"),
                        color = LockTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Device profile: ${snapshot.compatibilityLabel}",
                            "Profil perangkat: ${snapshot.compatibilityLabel}"
                        ),
                        color = LockTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                Button(
                    onClick = onRefresh,
                    enabled = !refreshing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(
                        text = if (refreshing) {
                            tr("Checking", "Cek")
                        } else {
                            tr("Refresh", "Refresh")
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreExamHealthCountChip(
                    label = tr("Block", "Blok"),
                    count = snapshot.blockingCount,
                    color = Color(0xFFB34A4A),
                    modifier = Modifier.weight(1f)
                )
                PreExamHealthCountChip(
                    label = tr("Warn", "Warn"),
                    count = snapshot.warningCount,
                    color = LockGoldDark,
                    modifier = Modifier.weight(1f)
                )
                PreExamHealthCountChip(
                    label = tr("Ready", "Siap"),
                    count = snapshot.stableCount,
                    color = Color(0xFF1F7A4D),
                    modifier = Modifier.weight(1f)
                )
            }

            snapshot.items.forEach { item ->
                PreExamHealthRow(
                    item = item,
                    onFix = if (
                        item.category == PreExamHealthCategory.WebView &&
                        item.verdict != PreExamHealthVerdict.Stable
                    ) {
                        onFixWebViewProvider
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
internal fun DeviceSurvivalPolicyCard(
    policy: DeviceSurvivalPolicy,
    previousSessionBreadcrumb: PreviousExamSessionBreadcrumb,
    onExportDiagnostics: () -> Unit
) {
    val accentColor = when (policy.score) {
        CompatibilityScore.Excellent -> Color(0xFF1F7A4D)
        CompatibilityScore.Good -> Color(0xFF2F8F63)
        CompatibilityScore.NeedsSetup -> LockGoldDark
        CompatibilityScore.NotRecommended -> Color(0xFFB34A4A)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Device Readiness", "Kesiapan Perangkat"),
                        color = LockTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${policy.vendorRiskLabel} â€¢ ${policy.webViewRiskLabel}",
                        color = LockTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentColor.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = policy.score.name,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreExamHealthCountChip(
                    label = tr("Block", "Blok"),
                    count = policy.healthBlockingCount,
                    color = Color(0xFFB34A4A),
                    modifier = Modifier.weight(1f)
                )
                PreExamHealthCountChip(
                    label = tr("Warn", "Warn"),
                    count = policy.healthWarningCount,
                    color = LockGoldDark,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = policy.runtimeTier.name,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = tr("Runtime", "Runtime"),
                            color = LockTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            previousSessionBreadcrumb.latestRecoveryHint?.let { hint ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF8E8),
                    border = BorderStroke(1.dp, LockGoldDark.copy(alpha = 0.18f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = tr("Previous Session Recovery", "Recovery Sesi Sebelumnya"),
                            color = LockGoldDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = hint,
                            color = LockTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        TextButton(onClick = onExportDiagnostics) {
                            Text(
                                text = tr("Export Diagnostics", "Export Diagnostik"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreExamHealthCountChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = LockTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PreExamHealthRow(
    item: PreExamHealthItem,
    onFix: (() -> Unit)? = null
) {
    val color = when (item.verdict) {
        PreExamHealthVerdict.Blocking -> Color(0xFFB34A4A)
        PreExamHealthVerdict.Warning -> LockGoldDark
        PreExamHealthVerdict.Stable -> Color(0xFF1F7A4D)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = LockTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = item.verdict.name,
                        color = color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = item.detail,
                color = LockTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            item.quickFix?.takeIf { it.isNotBlank() }?.let { quickFix ->
                Text(
                    text = quickFix,
                    color = LockTextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
            if (onFix != null) {
                TextButton(
                    onClick = onFix,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 0.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text(
                        text = tr("Fix WebView Provider", "Perbaiki WebView Provider"),
                        color = LockBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
