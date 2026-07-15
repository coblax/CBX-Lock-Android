package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockBlueSoft
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockGoldAccent
import com.coblax.examlock.ui.theme.LockIssueText
import com.coblax.examlock.ui.theme.LockSafeEmphasis
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
import com.coblax.examlock.ui.theme.LockBlueTint
import com.coblax.examlock.ui.theme.LockOutlineMedium

@Composable
internal fun PreparationChecklistHeader(
    examTitle: String,
    severeLowRamPreparation: Boolean,
    blockingCount: Int,
    warningCount: Int,
    safeCount: Int,
    canStartExam: Boolean,
    firstBlockingReason: String?,
    onBackHome: () -> Unit,
    onSwitchToWizard: () -> Unit = {}
) {
    val shape = RoundedCornerShape(if (severeLowRamPreparation) 20.dp else 24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LockOutlineMedium, shape)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (severeLowRamPreparation) {
                        Brush.verticalGradient(listOf(Color.White, Color.White))
                    } else {
                        Brush.verticalGradient(
                            0f to LockBlueTint,
                            0.6f to LockBlueSoft.copy(alpha = 0.03f),
                            1f to Color.Transparent
                        )
                    }
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LockSurfaceSoft)
                            .border(1.dp, LockOutlineMedium, CircleShape)
                            .clickable(onClick = onBackHome)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = tr("Back to home", "Kembali ke menu utama"),
                            tint = LockBlueDeep,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Mode badge
                    Box(
                        modifier = Modifier
                            .flatPill(containerColor = LockBlueDeep)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tr("PREPARATION", "PERSIAPAN"),
                            color = LockOnDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Wizard mode toggle
                    Box(
                        modifier = Modifier
                            .flatPill(containerColor = LockSurfaceSoft)
                            .border(1.dp, LockOutlineMedium, RoundedCornerShape(UiTokens.RadiusPill))
                            .clickable(onClick = onSwitchToWizard)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "\uD83E\uDDED " + tr("Wizard", "Wizard"),
                            color = LockBlueDeep,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = examTitle,
                    color = LockBlueDeep,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tr(
                        "Quick device & security check before starting.",
                        "Pemeriksaan perangkat & keamanan sebelum mulai."
                    ),
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                val totalChecks = safeCount + blockingCount
                val progress = if (totalChecks > 0) safeCount.toFloat() / totalChecks else 1f
                val progressColor = if (canStartExam) LockSafeEmphasis else if (progress > 0.7f) LockGoldAccent else LockIssueText
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.12f)
                )
                Text(
                    text = tr(
                        "$safeCount/$totalChecks passed",
                        "$safeCount/$totalChecks lulus"
                    ),
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Readiness summary chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadinessChip(
                        count = safeCount,
                        label = tr("Safe", "Aman"),
                        color = LockSafeEmphasis,
                        modifier = Modifier.weight(1f)
                    )
                    ReadinessChip(
                        count = warningCount,
                        label = tr("Warn", "Warn"),
                        color = LockGoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ReadinessChip(
                        count = blockingCount,
                        label = tr("Block", "Blok"),
                        color = LockIssueText,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status sentence
                val statusText = if (canStartExam) {
                    tr(
                        "âœ… All checks passed â€” ready to start.",
                        "âœ… Semua pemeriksaan lulus â€” siap mulai."
                    )
                } else if (blockingCount == 1) {
                    firstBlockingReason?.let {
                        tr(
                            "ðŸ”´ 1 issue must be fixed: $it",
                            "ðŸ”´ 1 masalah harus diperbaiki: $it"
                        )
                    } ?: tr(
                        "ðŸ”´ 1 issue must be fixed before starting.",
                        "ðŸ”´ 1 masalah harus diperbaiki sebelum mulai."
                    )
                } else {
                    tr(
                        "ðŸ”´ $blockingCount issues must be fixed before starting.",
                        "ðŸ”´ $blockingCount masalah harus diperbaiki sebelum mulai."
                    )
                }
                val statusColor = if (canStartExam) LockSafeEmphasis else LockIssueText
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ReadinessChip(
    count: Int,
    label: String,
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
