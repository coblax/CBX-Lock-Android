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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.StatusBadge
import com.coblax.examlock.ui.theme.UiStatusTone

// ──────────────────────────────────────────────────────────────
// Step Title Card
// ──────────────────────────────────────────────────────────────

@Composable
internal fun WizardStepTitleCard(
    stepIndex: Int,
    step: WizardStep,
    stepState: WizardStepState?
) {
    val tokens = ScopedUiTokens.current
    val uiLanguage = LocalUiLanguage.current
    val isCompleted = stepState?.isCompleted ?: false
    val issueCount = stepState?.issueCount ?: 0
    val accentColor = when {
        isCompleted -> WizardGreen
        issueCount > 0 -> WizardRed
        else -> AppColors.current.blue
    }
    val statusText = when {
        isCompleted -> tr("Ready", "Siap")
        issueCount == 1 -> tr("1 issue", "1 masalah")
        issueCount > 1 -> tr("$issueCount issues", "$issueCount masalah")
        else -> tr("Checking", "Memeriksa")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PreparationWizardUiTestTags.StepSummary)
            .clip(RoundedCornerShape(tokens.radiusLarge))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.08f),
                        accentColor.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.20f), RoundedCornerShape(tokens.radiusLarge))
            .padding(horizontal = tokens.spaceMedium, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Step number badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.materialIcon(),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Step ${stepIndex + 1}", "Langkah ${stepIndex + 1}"),
                    color = accentColor,
                    style = AppTextStyles.label
                )
                Text(
                    text = step.title(uiLanguage),
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.cardTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = step.description(uiLanguage),
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.diagnostic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StatusBadge(
                label = statusText,
                tone = when {
                    isCompleted -> UiStatusTone.Success
                    issueCount > 0 -> UiStatusTone.Danger
                    else -> UiStatusTone.Neutral
                }
            )
        }
    }
}

private fun WizardStep.materialIcon(): ImageVector {
    return when (this) {
        WizardStep.DeviceSetup -> Icons.Rounded.Settings
        WizardStep.Connectivity -> Icons.Rounded.Wifi
        WizardStep.DeviceHealth -> Icons.Rounded.HealthAndSafety
        WizardStep.RuntimeInteraction -> Icons.Rounded.AccessibilityNew
        WizardStep.DeviceIntegrity -> Icons.Rounded.VerifiedUser
        WizardStep.Clipboard -> Icons.Rounded.ContentPaste
        WizardStep.Location -> Icons.Rounded.LocationOn
        WizardStep.DeviceLock -> Icons.Rounded.PhonelinkLock
        WizardStep.RuntimeSecurity -> Icons.Rounded.Security
    }
}
