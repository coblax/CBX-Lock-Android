package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.CompatibilityScore
import com.coblax.examlock.DeviceSurvivalPolicy
import com.coblax.examlock.PreviousExamSessionBreadcrumb
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockGoldDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockIssueText
import com.coblax.examlock.ui.theme.LockSafeEmphasis
import com.coblax.examlock.ui.theme.LockSafeStrong
import com.coblax.examlock.ui.theme.LockWarnBgWarm
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
import com.coblax.examlock.ui.theme.LockOutlineMedium

@Composable
internal fun PreExamHealthCheckCard(
    snapshot: PreExamHealthSnapshot,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onFixWebViewProvider: () -> Unit
) {
    val accentColor = when {
        snapshot.blockingCount > 0 -> LockIssueText
        snapshot.warningCount > 0 -> LockGoldDark
        else -> LockSafeStrong
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LockOutlineMedium, RoundedCornerShape(UiTokens.RadiusLg))
            .padding(horizontal = 14.dp, vertical = 14.dp),
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
                shape = RoundedCornerShape(UiTokens.RadiusSm),
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
                color = LockIssueText,
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
                color = LockSafeStrong,
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

@Composable
internal fun DeviceSurvivalPolicyCard(
    policy: DeviceSurvivalPolicy,
    previousSessionBreadcrumb: PreviousExamSessionBreadcrumb,
    onExportDiagnostics: () -> Unit
) {
    val accentColor = when (policy.score) {
        CompatibilityScore.Excellent -> LockSafeStrong
        CompatibilityScore.Good -> LockSafeEmphasis
        CompatibilityScore.NeedsSetup -> LockGoldDark
        CompatibilityScore.NotRecommended -> LockIssueText
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            Box(
                modifier = Modifier
                    .flatPill(containerColor = accentColor.copy(alpha = 0.11f))
                    .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(UiTokens.RadiusPill))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = policy.score.name,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
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
                color = LockIssueText,
                modifier = Modifier.weight(1f)
            )
            PreExamHealthCountChip(
                label = tr("Warn", "Warn"),
                count = policy.healthWarningCount,
                color = LockGoldDark,
                modifier = Modifier.weight(1f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(UiTokens.RadiusSm))
                    .background(accentColor.copy(alpha = 0.08f))
                    .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(UiTokens.RadiusSm))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
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
        previousSessionBreadcrumb.latestRecoveryHint?.let { hint ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(UiTokens.RadiusSm))
                    .background(LockWarnBgWarm)
                    .border(1.dp, LockGoldDark.copy(alpha = 0.15f), RoundedCornerShape(UiTokens.RadiusSm))
                    .padding(10.dp),
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

@Composable
private fun PreExamHealthCountChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(UiTokens.RadiusSm))
            .padding(horizontal = 8.dp, vertical = 7.dp),
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

@Composable
private fun PreExamHealthRow(
    item: PreExamHealthItem,
    onFix: (() -> Unit)? = null
) {
    val color = when (item.verdict) {
        PreExamHealthVerdict.Blocking -> LockIssueText
        PreExamHealthVerdict.Warning -> LockGoldDark
        PreExamHealthVerdict.Stable -> LockSafeStrong
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
                Box(
                    modifier = Modifier
                        .flatPill(containerColor = color.copy(alpha = 0.10f))
                        .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(UiTokens.RadiusPill))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.verdict.name,
                        color = color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
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
