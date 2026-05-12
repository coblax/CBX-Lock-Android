package com.example.coblaxexamlock.ui.preparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockBlueSoft
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextSecondary

@Composable
internal fun PreparationChecklistHeader(
    examTitle: String,
    severeLowRamPreparation: Boolean,
    onBackHome: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (severeLowRamPreparation) 20.dp else 24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.7f)),
        tonalElevation = if (severeLowRamPreparation) 0.dp else 2.dp,
        shadowElevation = if (severeLowRamPreparation) 0.dp else 6.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (severeLowRamPreparation) {
                        Brush.verticalGradient(listOf(Color.White, Color.White))
                    } else {
                        Brush.verticalGradient(
                            0f to LockBlue.copy(alpha = 0.08f),
                            0.6f to LockBlueSoft.copy(alpha = 0.04f),
                            1f to Color.Transparent
                        )
                    }
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home button
                    Surface(
                        shape = CircleShape,
                        color = LockSurfaceSoft,
                        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.7f))
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = onBackHome)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = tr("Back to home", "Kembali ke menu utama"),
                                tint = LockBlueDeep,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Mode badge
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = LockBlueDeep
                    ) {
                        Text(
                            text = tr("PREPARATION", "PERSIAPAN"),
                            color = LockOnDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = examTitle,
                    color = LockBlueDeep,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tr(
                        "Quick device & security check before starting.",
                        "Pemeriksaan perangkat & keamanan sebelum mulai."
                    ),
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
