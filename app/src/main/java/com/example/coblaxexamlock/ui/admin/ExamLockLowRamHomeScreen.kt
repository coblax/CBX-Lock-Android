package com.example.coblaxexamlock.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.StartupTrace
import com.example.coblaxexamlock.config.DeveloperGithubUrl
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.ui.theme.LockBackground
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextSecondary
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun ExamLockLowRamHomeScreen(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onScanExam: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenFastExam: () -> Unit,
    directLinkLabel: String,
    onSecretTap: () -> Unit,
    showDeferredChrome: Boolean,
    modifier: Modifier = Modifier
) {
    val firstDrawMarked = remember { AtomicBoolean(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (firstDrawMarked.compareAndSet(false, true)) {
                    StartupTrace.mark("home_first_frame", "severe=true shell=foundation")
                }
            }
            .background(LockBackground)
    ) {
        // Subtle top gradient accent — lightweight, no bitmap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        0f to LockBlue.copy(alpha = 0.06f),
                        1f to Color.Transparent
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LowRamHero(
                uiLanguage = uiLanguage,
                onUiLanguageChange = onUiLanguageChange,
                onSecretTap = onSecretTap
            )

            Spacer(modifier = Modifier.height(16.dp))

            LowRamHomeButton(
                text = tr("SCAN EXAM QR", "SCAN QR UJIAN"),
                subtitle = tr(
                    "Scan the exam QR to start",
                    "Pindai QR ujian untuk mulai"
                ),
                badgeText = tr("RECOMMENDED", "REKOMENDASI"),
                glyph = "QR",
                containerColor = LockBlue,
                contentColor = LockOnDark,
                borderColor = LockBlue,
                glyphContainerColor = Color.White.copy(alpha = 0.18f),
                onClick = onScanExam
            )

            Spacer(modifier = Modifier.height(10.dp))

            LowRamHomeButton(
                text = tr("CUSTOM QR (ADMIN)", "CUSTOM QR (ADMIN)"),
                subtitle = tr(
                    "Create exam QR for admin tasks",
                    "Buat QR ujian untuk kebutuhan admin"
                ),
                badgeText = "ADMIN",
                glyph = "AD",
                containerColor = Color.White,
                contentColor = LockBlueDeep,
                borderColor = LockOutline,
                glyphContainerColor = LockBlue.copy(alpha = 0.08f),
                onClick = onOpenAdmin
            )

            Spacer(modifier = Modifier.height(10.dp))

            LowRamHomeButton(
                text = directLinkLabel,
                subtitle = tr(
                    "Open exam quickly with saved link",
                    "Buka ujian cepat dengan link tersimpan"
                ),
                badgeText = tr("DIRECT LINK", "LINK LANGSUNG"),
                glyph = "GO",
                containerColor = LockGold.copy(alpha = 0.12f),
                contentColor = LockBlueDeep,
                borderColor = LockGold.copy(alpha = 0.40f),
                glyphContainerColor = LockBlueDeep.copy(alpha = 0.08f),
                onClick = onOpenFastExam
            )

            if (showDeferredChrome) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFD))
                        .border(1.dp, LockOutline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LowRamText(
                            text = "COBLAX",
                            color = LockBlueDeep,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        LowRamText(
                            text = DeveloperGithubUrl,
                            color = LockTextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LowRamText(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = LockTextMuted.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LowRamHero(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onSecretTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, LockOutline.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Production badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            0f to LockBlueDeep,
                            1f to LockBlue.copy(alpha = 0.85f)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onSecretTap
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                    LowRamText(
                        text = "PROD",
                        color = LockOnDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Language toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFF4F7FB))
                    .border(1.dp, LockOutline.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                LowRamLanguageChip(
                    text = "EN",
                    selected = uiLanguage == UiLanguage.English,
                    onClick = { onUiLanguageChange(UiLanguage.English) }
                )
                LowRamLanguageChip(
                    text = "ID",
                    selected = uiLanguage == UiLanguage.Indonesian,
                    onClick = { onUiLanguageChange(UiLanguage.Indonesian) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Brand mark
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = Brush.verticalGradient(
                        0f to LockBlueDeep,
                        1f to LockBlue
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            LowRamText(
                text = "CBX",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LowRamText(
            text = "EXAM LOCK",
            color = LockBlueDeep,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        LowRamText(
            text = tr(
                "Secure exam browser for Android",
                "Browser ujian aman untuk Android"
            ),
            color = LockTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LowRamLanguageChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) LockBlue else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        LowRamText(
            text = text,
            color = if (selected) LockOnDark else LockTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LowRamHomeButton(
    text: String,
    subtitle: String,
    badgeText: String,
    glyph: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    glyphContainerColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon glyph container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glyphContainerColor),
            contentAlignment = Alignment.Center
        ) {
            LowRamText(
                text = glyph,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            LowRamText(
                text = badgeText,
                color = contentColor.copy(alpha = 0.65f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            LowRamText(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            LowRamText(
                text = subtitle,
                color = contentColor.copy(alpha = 0.70f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun LowRamText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing
        )
    )
}
