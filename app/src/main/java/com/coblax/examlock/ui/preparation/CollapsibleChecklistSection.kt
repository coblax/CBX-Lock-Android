package com.coblax.examlock.ui.preparation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiTokens

/**
 * Wraps a checklist section with a collapsible header.
 * Clear sections start collapsed; sections with issues start expanded.
 * Tapping the header toggles the section content.
 */
@Composable
internal fun CollapsibleChecklistSection(
    sectionKey: String,
    health: SectionHealth?,
    content: @Composable () -> Unit
) {
    if (health == null) {
        // No health data — just show content as-is
        content()
        return
    }

    var expanded by rememberSaveable(sectionKey) { mutableStateOf(!health.allClear) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            )
    ) {
        // Section header (clickable toggle)
        val allClearBg = if (AppColors.current.isDark) AppColors.current.statusSafeFill
        else Color(0xFFF0F9F4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(UiTokens.RadiusMd))
                .background(if (health.allClear) allClearBg else AppColors.current.surfaceSoft)
                .border(
                    1.dp,
                    if (health.allClear) AppColors.current.safeEmphasis.copy(alpha = 0.15f)
                    else AppColors.current.outlineSubtle,
                    RoundedCornerShape(UiTokens.RadiusMd)
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = health.title,
                color = AppColors.current.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (health.allClear) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (health.allClear) AppColors.current.safeEmphasis else AppColors.current.issueText,
                    modifier = Modifier.size(16.dp)
                )
                if (health.allClear) {
                    Text(
                        text = tr("All clear", "Aman semua"),
                        color = AppColors.current.safeEmphasis,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = tr(
                            "${health.issueCount} ${if (health.issueCount == 1) "issue" else "issues"}",
                            "${health.issueCount} masalah"
                        ),
                        color = AppColors.current.issueText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = tr(
                        if (expanded) "Collapse section" else "Expand section",
                        if (expanded) "Tutup bagian" else "Buka bagian"
                    ),
                    tint = AppColors.current.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Animated content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ),
            exit = shrinkVertically(
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp)
            ) {
                content()
            }
        }
    }
}
