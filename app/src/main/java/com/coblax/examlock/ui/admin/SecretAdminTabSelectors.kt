package com.coblax.examlock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.CustomQrAdminTab
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.UiTokens

@Composable
internal fun CustomQrAdminTabSelector(
    selectedTab: CustomQrAdminTab,
    onTabSelected: (CustomQrAdminTab) -> Unit
) {
    val tabs = listOf(
        CustomQrAdminTab.Exam to tr("Exam", "Ujian"),
        CustomQrAdminTab.Location to tr("Location", "Lokasi"),
        CustomQrAdminTab.Generate to tr("Generate", "Generate")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.RadiusMd),
        color = AppColors.current.surfaceSoft,
        border = BorderStroke(1.dp, AppColors.current.outline)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (tab, label) ->
                val selected = tab == selectedTab
                val completed = tab.ordinal < selectedTab.ordinal
                val stepStateDescription = when {
                    selected -> tr("Current step", "Langkah aktif")
                    completed -> tr("Completed step", "Langkah selesai")
                    else -> tr("Upcoming step", "Langkah berikutnya")
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(UiTokens.RadiusSm))
                        .semantics {
                            this.selected = selected
                            stateDescription = stepStateDescription
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) }
                        ),
                    shape = RoundedCornerShape(UiTokens.RadiusSm),
                    color = if (selected) AppColors.current.blue else Color.Transparent
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        selected -> AppColors.current.onDark.copy(alpha = 0.18f)
                                        completed -> AppColors.current.statusSafeFill
                                        else -> AppColors.current.background
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = when {
                                    selected -> AppColors.current.onDark
                                    completed -> AppColors.current.statusSafe
                                    else -> AppColors.current.textMuted
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = label,
                            color = if (selected) {
                                AppColors.current.onDark
                            } else {
                                AppColors.current.textSecondary
                            },
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
