package com.coblax.examlock.ui.preparation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBadge
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.currentUiMotionPolicy

// ──────────────────────────────────────────────────────────────
// Wizard Header
// ──────────────────────────────────────────────────────────────

@Composable
internal fun WizardHeader(
    examTitle: String,
    currentStepIndex: Int,
    completedCount: Int,
    totalSteps: Int,
    overallProgress: Float,
    canStartExam: Boolean,
    onBackHome: () -> Unit,
    onSwitchToChecklist: () -> Unit
) {
    val tokens = ScopedUiTokens.current
    val motionPolicy = currentUiMotionPolicy()
    val shape = RoundedCornerShape(tokens.radiusLarge)
    val progressColor = if (canStartExam) WizardGreen else AppColors.current.goldDark
    val statusText = if (canStartExam) {
        tr("Ready to start", "Siap memulai")
    } else {
        tr("Needs attention", "Perlu diperiksa")
    }
    val displayedProgress = if (!motionPolicy.enabled) {
        overallProgress
    } else {
        animateFloatAsState(
            targetValue = overallProgress,
            animationSpec = tween(motionPolicy.contentDurationMillis),
            label = "wizard_progress"
        ).value
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PreparationWizardUiTestTags.Header)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.current.outlineMedium, shape)
            .padding(horizontal = tokens.spaceMedium, vertical = tokens.spaceSmall)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackHome,
                    modifier = Modifier.size(tokens.touchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = tr("Back to home", "Kembali ke menu utama"),
                        tint = AppColors.current.brandText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Exam preparation", "Persiapan ujian"),
                        color = AppColors.current.textSecondary,
                        style = AppTextStyles.label
                    )
                    Text(
                        text = examTitle,
                        color = AppColors.current.textPrimary,
                        style = AppTextStyles.cardTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(
                    onClick = onSwitchToChecklist,
                    modifier = Modifier.heightIn(min = tokens.touchTarget)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = tr("Details", "Detail"),
                        style = AppTextStyles.button
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    label = statusText,
                    tone = if (canStartExam) UiStatusTone.Success else UiStatusTone.Warning
                )
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    text = tr(
                        "Step ${currentStepIndex + 1}/$totalSteps · $completedCount complete",
                        "Langkah ${currentStepIndex + 1}/$totalSteps · $completedCount selesai"
                    ),
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.label
                )
            }

            LinearProgressIndicator(
                progress = { displayedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.12f)
            )
        }
    }
}
