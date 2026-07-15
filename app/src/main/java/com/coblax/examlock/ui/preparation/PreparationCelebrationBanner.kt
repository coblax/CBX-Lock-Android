package com.coblax.examlock.ui.preparation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockSafeEmphasis
import com.coblax.examlock.ui.theme.UiTokens

@Composable
internal fun PreparationCelebrationBanner() {
    val accentColor = LockSafeEmphasis
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(Color(0xFFEDF9F2))
            .border(1.dp, accentColor.copy(alpha = 0.20f), RoundedCornerShape(UiTokens.RadiusLg))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = tr(
                "âœ… All checks passed!",
                "âœ… Semua pemeriksaan lulus!"
            ),
            color = accentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = tr(
                "Your device is ready for the exam. Tap START EXAM below.",
                "Perangkat Anda siap untuk ujian. Ketuk MULAI UJIAN di bawah."
            ),
            color = LockTextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
