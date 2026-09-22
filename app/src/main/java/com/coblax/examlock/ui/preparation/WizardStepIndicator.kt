package com.coblax.examlock.ui.preparation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.currentUiMotionPolicy
import com.coblax.examlock.ui.theme.isExpandedLayout

// ──────────────────────────────────────────────────────────────
// Step Indicator (numbered circles)
// ──────────────────────────────────────────────────────────────

/**
 * Horizontal row of numbered step circles. On expanded (tablet) layouts the
 * circles are slightly larger and include abbreviated step labels beneath
 * each circle for improved scanability.
 */
@Composable
internal fun WizardStepIndicator(
    steps: List<WizardStep>,
    stepStates: List<WizardStepState>,
    currentStepIndex: Int,
    onStepClick: (Int) -> Unit
) {
    val motionPolicy = currentUiMotionPolicy()
    val tokens = ScopedUiTokens.current
    val expanded = isExpandedLayout()
    val circleSize = if (expanded) 36.dp else 30.dp
    val iconSize = if (expanded) 18.dp else 16.dp
    val fontSize = if (expanded) 12.sp else 11.sp
    val uiLanguage = LocalUiLanguage.current
    val listState = rememberLazyListState()

    LaunchedEffect(currentStepIndex, motionPolicy.enabled) {
        if (!motionPolicy.enabled) {
            listState.scrollToItem(currentStepIndex.coerceAtLeast(0))
        } else {
            listState.animateScrollToItem(currentStepIndex.coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PreparationWizardUiTestTags.StepIndicator)
            .clip(RoundedCornerShape(tokens.radiusMedium))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.current.outlineSubtle, RoundedCornerShape(tokens.radiusMedium)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            count = steps.size,
            key = { index -> steps[index].sectionKey }
        ) { index ->
            val step = steps[index]
            val stepState = stepStates.getOrNull(index)
            val isCompleted = stepState?.isCompleted ?: false
            val hasIssue = (stepState?.issueCount ?: 0) > 0
            val isCurrent = index == currentStepIndex

            val targetBgColor = when {
                isCurrent && isCompleted -> WizardGreen
                isCurrent && hasIssue -> WizardRed
                isCurrent -> AppColors.current.blue
                isCompleted -> WizardGreen.copy(alpha = 0.15f)
                hasIssue -> AppColors.current.statusDangerFill
                else -> AppColors.current.surfaceSoft
            }
            val bgColor = if (!motionPolicy.enabled) {
                targetBgColor
            } else {
                animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(motionPolicy.contentDurationMillis),
                    label = "step_bg_$index"
                ).value
            }
            val targetTextColor = when {
                isCurrent -> Color.White
                isCompleted -> WizardGreen
                hasIssue -> WizardRed
                else -> AppColors.current.textMuted
            }
            val textColor = if (!motionPolicy.enabled) {
                targetTextColor
            } else {
                animateColorAsState(
                    targetValue = targetTextColor,
                    animationSpec = tween(motionPolicy.contentDurationMillis),
                    label = "step_text_$index"
                ).value
            }
            val borderColor = when {
                isCurrent -> Color.Transparent
                isCompleted -> WizardGreen.copy(alpha = 0.25f)
                hasIssue -> WizardRed.copy(alpha = 0.35f)
                else -> AppColors.current.outline.copy(alpha = 0.40f)
            }
            val statusDescription = when {
                isCompleted -> tr("Complete", "Selesai")
                hasIssue -> tr(
                    "${stepState?.issueCount ?: 0} issues",
                    "${stepState?.issueCount ?: 0} masalah"
                )
                else -> tr("Checking", "Memeriksa")
            }
            val stepDescription = tr(
                "Step ${index + 1} of ${steps.size}: ${step.title(uiLanguage)}, $statusDescription",
                "Langkah ${index + 1} dari ${steps.size}: ${step.title(uiLanguage)}, $statusDescription"
            )
            val interactiveModifier = Modifier
                .sizeIn(minWidth = tokens.touchTarget, minHeight = tokens.touchTarget)
                .semantics {
                    role = Role.Tab
                    selected = isCurrent
                    stateDescription = statusDescription
                    contentDescription = stepDescription
                }
                .clickable(role = Role.Tab) { onStepClick(index) }

            if (expanded) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = interactiveModifier.padding(horizontal = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted && !isCurrent) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(iconSize)
                            )
                        } else if (hasIssue && !isCurrent) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(iconSize)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        text = step.shortLabel(uiLanguage),
                        color = if (isCurrent) AppColors.current.blue else AppColors.current.textMuted,
                        fontSize = 8.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Box(
                    modifier = interactiveModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted && !isCurrent -> Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(iconSize)
                            )
                            hasIssue && !isCurrent -> Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(iconSize)
                            )
                            else -> Text(
                                text = "${index + 1}",
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
