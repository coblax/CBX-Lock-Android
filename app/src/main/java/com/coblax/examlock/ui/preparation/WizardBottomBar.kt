package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.isExpandedLayout

// ──────────────────────────────────────────────────────────────
// Bottom Navigation Bar
// ──────────────────────────────────────────────────────────────

/**
 * Wizard bottom bar with adaptive max-width. On expanded (tablet) screens
 * the bar is constrained so it doesn't stretch edge-to-edge, and side
 * buttons are slightly wider with visible text labels.
 */
@Composable
internal fun WizardBottomBar(
    currentStepIndex: Int,
    totalSteps: Int,
    currentStepCompleted: Boolean,
    canStartExam: Boolean,
    isStartingExam: Boolean,
    webViewSessionResetInFlight: Boolean,
    startButtonColor: Color,
    startButtonContentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRecheck: () -> Unit,
    onStartExam: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val isFirstStep = currentStepIndex == 0
    val expanded = isExpandedLayout()
    // The side buttons have fixed widths, so at large accessibility font scales their
    // labels used to wrap mid-word ("Rechec" / "k"). Past that point drop to icon-only,
    // which also keeps the bar compact.
    val showSideLabels = LocalDensity.current.fontScale <= 1.3f
    val backButtonWidth = when {
        !showSideLabels -> 56.dp
        expanded -> 112.dp
        else -> 76.dp
    }
    val recheckButtonWidth = when {
        !showSideLabels -> 56.dp
        expanded -> 132.dp
        else -> 96.dp
    }
    val primaryAction = resolveWizardPrimaryAction(
        currentStepIndex = currentStepIndex,
        totalSteps = totalSteps,
        isStartingExam = isStartingExam,
        webViewSessionResetInFlight = webViewSessionResetInFlight
    )
    val isStartAction = primaryAction != WizardPrimaryAction.Next
    val primaryEnabled = when (primaryAction) {
        WizardPrimaryAction.Start -> canStartExam
        WizardPrimaryAction.Next -> true
        WizardPrimaryAction.Starting,
        WizardPrimaryAction.Preparing -> false
    }
    val primaryText = when (primaryAction) {
        WizardPrimaryAction.Next -> tr("Continue", "Lanjut")
        WizardPrimaryAction.Start -> tr("Start exam", "Mulai ujian")
        WizardPrimaryAction.Starting -> tr("Starting…", "Memulai…")
        WizardPrimaryAction.Preparing -> tr("Preparing…", "Menyiapkan…")
    }
    val primaryContainerColor = when {
        isStartAction -> startButtonColor
        currentStepCompleted -> WizardGreen
        else -> AppColors.current.blue
    }
    val primaryContentColor = if (isStartAction) startButtonContentColor else Color.White
    val showStepHint = !currentStepCompleted || (isStartAction && !canStartExam)

    Column(
        modifier = modifier
            .then(
                if (expanded) Modifier.widthIn(max = tokens.contentMaxWidthExpanded)
                else Modifier
            )
            .testTag(PreparationWizardUiTestTags.BottomBar)
            .clip(RoundedCornerShape(tokens.radiusLarge))
            // cardBg, not surface: ExamLockColorPalette.surface is the dark picker
            // surface (#1A1F2B) even in light mode, which made the outlined Back /
            // Recheck labels dark-on-dark here.
            .background(AppColors.current.cardBg)
            .border(
                1.dp,
                AppColors.current.outlineMedium,
                RoundedCornerShape(tokens.radiusLarge)
            )
            .fillMaxWidth()
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = if (isFirstStep) onBackHome else onPrevious,
                modifier = Modifier
                    .width(backButtonWidth)
                    .testTag(PreparationWizardUiTestTags.BackAction)
                    .heightIn(min = tokens.compactControlHeight),
                shape = RoundedCornerShape(tokens.radiusMedium),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.current.brandText
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val backLabel = if (isFirstStep) tr("Menu", "Menu") else tr("Back", "Kembali")
                Icon(
                    imageVector = if (isFirstStep) Icons.Rounded.Home
                    else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = if (showSideLabels) null else backLabel,
                    modifier = Modifier.size(17.dp)
                )
                if (showSideLabels) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = backLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            OutlinedButton(
                onClick = onRecheck,
                modifier = Modifier
                    .width(recheckButtonWidth)
                    .testTag(PreparationWizardUiTestTags.RecheckAction)
                    .heightIn(min = tokens.compactControlHeight),
                shape = RoundedCornerShape(tokens.radiusMedium),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.current.brandText
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                val recheckLabel = tr("Recheck", "Cek ulang")
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = if (showSideLabels) null else recheckLabel,
                    modifier = Modifier.size(17.dp)
                )
                if (showSideLabels) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = recheckLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Button(
                onClick = if (primaryAction == WizardPrimaryAction.Next) onNext else onStartExam,
                modifier = Modifier
                    .weight(1f)
                    .testTag(PreparationWizardUiTestTags.PrimaryAction)
                    .heightIn(min = tokens.primaryActionHeight),
                shape = RoundedCornerShape(tokens.radiusMedium),
                enabled = primaryEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryContainerColor,
                    contentColor = primaryContentColor,
                    disabledContainerColor = primaryContainerColor.copy(alpha = 0.45f),
                    disabledContentColor = primaryContentColor.copy(alpha = 0.82f)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                if (primaryAction == WizardPrimaryAction.Next) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showStepHint) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PreparationWizardUiTestTags.StepHint)
                    .clip(RoundedCornerShape(tokens.radiusSmall))
                    .background(AppColors.current.statusWarnFill)
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = AppColors.current.statusWarn,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isStartAction) {
                        tr(
                            "Resolve all blocking issues before starting the exam.",
                            "Selesaikan semua masalah penghambat sebelum memulai ujian."
                        )
                    } else {
                        tr(
                            "This step still needs attention; you can continue reviewing other steps.",
                            "Langkah ini masih perlu diperiksa; Anda tetap dapat meninjau langkah lain."
                        )
                    },
                    color = AppColors.current.statusWarn,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
