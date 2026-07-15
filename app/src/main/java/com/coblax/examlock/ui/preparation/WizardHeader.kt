package com.coblax.examlock.ui.preparation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockBlueSoft
import com.coblax.examlock.ui.theme.LockGoldDark
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
import com.coblax.examlock.ui.theme.LockBlueTint
import com.coblax.examlock.ui.theme.LockOutlineMedium

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Wizard Header
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
internal fun WizardHeader(
    examTitle: String,
    completedCount: Int,
    totalSteps: Int,
    overallProgress: Float,
    canStartExam: Boolean,
    onBackHome: () -> Unit,
    onSwitchToChecklist: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
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
                    brush = Brush.verticalGradient(
                        0f to LockBlueTint,
                        0.6f to LockBlueSoft.copy(alpha = 0.03f),
                        1f to Color.Transparent
                    )
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

                    // Wizard badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(UiTokens.RadiusPill))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(LockBlueDeep, LockBlue)
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "ðŸ§­ " + tr("WIZARD", "WIZARD"),
                            color = LockOnDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Toggle to checklist
                    Box(
                        modifier = Modifier
                            .flatPill(containerColor = LockSurfaceSoft)
                            .border(1.dp, LockOutlineMedium, RoundedCornerShape(UiTokens.RadiusPill))
                            .clickable(onClick = onSwitchToChecklist)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tr("Technical Details", "Detail Teknis"),
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
                        "Step-by-step guided preparation.",
                        "Persiapan terpandu langkah demi langkah."
                    ),
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                val progressColor = if (canStartExam) WizardGreen
                    else if (overallProgress > 0.7f) LockGoldDark
                    else WizardRed
                val reduceMotion = LocalLowRamProfile.current.disableNonEssentialAnimations
                val displayedProgress = if (reduceMotion) {
                    overallProgress
                } else {
                    animateFloatAsState(
                        targetValue = overallProgress,
                        animationSpec = tween(400),
                        label = "wizard_progress"
                    ).value
                }
                LinearProgressIndicator(
                    progress = { displayedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.12f)
                )
                Text(
                    text = tr(
                        "$completedCount/$totalSteps steps completed",
                        "$completedCount/$totalSteps langkah selesai"
                    ),
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
