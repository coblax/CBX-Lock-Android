package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.UiTokens

@Composable
internal fun PreparationNoticeCard(
    title: String,
    message: String,
    accentColor: Color,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusMd))
            .background(backgroundColor)
            .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(UiTokens.RadiusMd))
    ) {
        // Left accent stripe â€” no animation, safe for API 24 / low-RAM
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(accentColor)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = LockTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp
            )
            Text(
                text = message,
                color = LockTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
