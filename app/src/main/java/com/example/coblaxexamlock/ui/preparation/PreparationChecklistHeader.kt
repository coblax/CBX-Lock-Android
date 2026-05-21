package com.example.coblaxexamlock.ui.preparation

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockBlueSoft
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

@Composable
internal fun PreparationChecklistHeader(
    examTitle: String,
    severeLowRamPreparation: Boolean,
    blockingCount: Int,
    warningCount: Int,
    safeCount: Int,
    canStartExam: Boolean,
    firstBlockingReason: String?,
    onBackHome: () -> Unit
) {
    val shape = RoundedCornerShape(if (severeLowRamPreparation) 20.dp else 24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, LockOutline.copy(alpha = 0.60f), shape)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (severeLowRamPreparation) {
                        Brush.verticalGradient(listOf(Color.White, Color.White))
                    } else {
                        Brush.verticalGradient(
                            0f to LockBlue.copy(alpha = 0.07f),
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
                            .border(1.dp, LockOutline.copy(alpha = 0.60f), CircleShape)
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
                            .clip(RoundedCornerShape(999.dp))
                            .background(LockBlueDeep)
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
                val progressColor = if (canStartExam) Color(0xFF2F8F63) else if (progress > 0.7f) Color(0xFFC79317) else Color(0xFFB34A4A)
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
                        color = Color(0xFF2F8F63),
                        modifier = Modifier.weight(1f)
                    )
                    ReadinessChip(
                        count = warningCount,
                        label = tr("Warn", "Warn"),
                        color = Color(0xFFC79317),
                        modifier = Modifier.weight(1f)
                    )
                    ReadinessChip(
                        count = blockingCount,
                        label = tr("Block", "Blok"),
                        color = Color(0xFFB34A4A),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status sentence
                val statusText = if (canStartExam) {
                    tr(
                        "✅ All checks passed — ready to start.",
                        "✅ Semua pemeriksaan lulus — siap mulai."
                    )
                } else if (blockingCount == 1) {
                    firstBlockingReason?.let {
                        tr(
                            "🔴 1 issue must be fixed: $it",
                            "🔴 1 masalah harus diperbaiki: $it"
                        )
                    } ?: tr(
                        "🔴 1 issue must be fixed before starting.",
                        "🔴 1 masalah harus diperbaiki sebelum mulai."
                    )
                } else {
                    tr(
                        "🔴 $blockingCount issues must be fixed before starting.",
                        "🔴 $blockingCount masalah harus diperbaiki sebelum mulai."
                    )
                }
                val statusColor = if (canStartExam) Color(0xFF2F8F63) else Color(0xFFB34A4A)
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
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
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
