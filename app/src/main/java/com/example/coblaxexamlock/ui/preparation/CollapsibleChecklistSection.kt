package com.example.coblaxexamlock.ui.preparation

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
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary

/**
 * Wraps a checklist section with a collapsible header.
 * Sections where [allClear] = true start collapsed and show "✅ All clear".
 * Sections with issues start expanded and show "⚠ N issues".
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
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section header (clickable toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (health.allClear) Color(0xFFF0F9F4) else LockSurfaceSoft)
                .border(
                    1.dp,
                    if (health.allClear) Color(0xFF2F8F63).copy(alpha = 0.15f)
                    else LockOutline.copy(alpha = 0.55f),
                    RoundedCornerShape(16.dp)
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
                        text = tr("✅ All clear", "✅ Aman semua"),
                        color = Color(0xFF2F8F63),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "⚠ ${health.issueCount} ${tr("issue", "masalah")}${if (health.issueCount > 1) "s" else ""}",
                        color = Color(0xFFB34A4A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (expanded) "▲" else "▼",
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
