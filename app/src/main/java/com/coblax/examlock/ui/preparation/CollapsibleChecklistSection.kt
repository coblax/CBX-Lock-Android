package com.coblax.examlock.ui.preparation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockIssueText
import com.coblax.examlock.ui.theme.LockSafeEmphasis
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockOutlineSubtle

/**
 * Wraps a checklist section with a collapsible header.
 * Sections where [allClear] = true start collapsed and show "âœ… All clear".
 * Sections with issues start expanded and show "âš  N issues".
 * Tapping the header toggles the section content.
 */
@Composable
internal fun CollapsibleChecklistSection(
    sectionKey: String,
    health: SectionHealth?,
    content: @Composable () -> Unit
) {
    if (health == null) {
        // No health data â€” just show content as-is
        content()
        return
    }

    var expanded by rememberSaveable(sectionKey) { mutableStateOf(!health.allClear) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section header (clickable toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(UiTokens.RadiusMd))
                .background(if (health.allClear) Color(0xFFF0F9F4) else LockSurfaceSoft)
                .border(
                    1.dp,
                    if (health.allClear) LockSafeEmphasis.copy(alpha = 0.15f)
                    else LockOutlineSubtle,
                    RoundedCornerShape(UiTokens.RadiusMd)
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = health.title,
                color = LockTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (health.allClear) {
                    Text(
                        text = tr("âœ… All clear", "âœ… Aman semua"),
                        color = LockSafeEmphasis,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "âš  ${health.issueCount} ${tr("issue", "masalah")}${if (health.issueCount > 1) "s" else ""}",
                        color = LockIssueText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (expanded) "â–²" else "â–¼",
                    color = LockTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        // Animated content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp)
            ) {
                content()
            }
        }
    }
}
