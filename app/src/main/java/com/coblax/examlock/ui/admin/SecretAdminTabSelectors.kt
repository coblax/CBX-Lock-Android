package com.coblax.examlock.ui.admin

import android.location.Location
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.CustomQrAdminTab
import com.coblax.examlock.model.SecretAdminTab
import com.coblax.examlock.R
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockOutlineStrong

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt


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
        shape = RoundedCornerShape(18.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, label) ->
                val selected = tab == selectedTab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(UiTokens.RadiusSm))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onTabSelected(tab) }
                        ),
                    shape = RoundedCornerShape(UiTokens.RadiusSm),
                    color = if (selected) LockBlue else Color.Transparent
                ) {
                    Text(
                        text = label,
                        color = if (selected) LockOnDark else LockTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SecretAdminTabSelector(
    selectedTab: SecretAdminTab,
    onTabSelected: (SecretAdminTab) -> Unit
) {
    val tabs = listOf(
        SecretAdminTab.Setup to tr("Setup", "Setup"),
        SecretAdminTab.Security to tr("Security", "Security")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutlineStrong, RoundedCornerShape(UiTokens.RadiusSm))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (tab, label) ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) LockBlue else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = { onTabSelected(tab) }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) LockOnDark else LockTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
