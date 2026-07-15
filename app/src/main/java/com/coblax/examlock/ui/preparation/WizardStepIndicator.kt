package com.coblax.examlock.ui.preparation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.isExpandedLayout
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockOutlineSubtle

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Step Indicator (numbered circles)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    val reduceMotion = LocalLowRamProfile.current.disableNonEssentialAnimations
    val expanded = isExpandedLayout()
    val circleSize = if (expanded) 36.dp else 30.dp
    val iconSize = if (expanded) 18.dp else 16.dp
    val fontSize = if (expanded) 12.sp else 11.sp
    val uiLanguage = LocalUiLanguage.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusMd))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LockOutlineSubtle, RoundedCornerShape(UiTokens.RadiusMd))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val stepState = stepStates.getOrNull(index)
            val isCompleted = stepState?.isCompleted ?: false
            val isCurrent = index == currentStepIndex

            val targetBgColor = when {
                isCurrent && isCompleted -> WizardGreen
                isCurrent -> LockBlue
                isCompleted -> WizardGreen.copy(alpha = 0.15f)
                else -> LockSurfaceSoft
            }
            val bgColor = if (reduceMotion) {
                targetBgColor
            } else {
                animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(300),
                    label = "step_bg_$index"
                ).value
            }
            val targetTextColor = when {
                isCurrent -> Color.White
                isCompleted -> WizardGreen
                else -> LockTextMuted
            }
            val textColor = if (reduceMotion) {
                targetTextColor
            } else {
                animateColorAsState(
                    targetValue = targetTextColor,
                    animationSpec = tween(300),
                    label = "step_text_$index"
                ).value
            }
            val borderColor = when {
                isCurrent -> Color.Transparent
                isCompleted -> WizardGreen.copy(alpha = 0.25f)
                else -> LockOutline.copy(alpha = 0.40f)
            }

            if (expanded) {
                // Tablet: circle + short label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.clickable { onStepClick(index) }
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
                        color = if (isCurrent) LockBlue else LockTextMuted,
                        fontSize = 8.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // Phone: circle only
                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable { onStepClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted && !isCurrent) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
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
            }
        }
    }
}
