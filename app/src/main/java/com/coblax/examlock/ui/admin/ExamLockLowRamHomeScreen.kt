package com.coblax.examlock.ui.admin

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
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.StartupTrace
import com.coblax.examlock.config.DeveloperGithubUrl
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.lowRamProfileBadgeLabel
import com.coblax.examlock.lowRamProfileBadgePalette
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.adaptiveScreenPadding
import com.coblax.examlock.ui.theme.responsiveContentWidth
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
import com.coblax.examlock.ui.theme.LockBlueTint
import com.coblax.examlock.ui.theme.LockOutlineSubtle
import com.coblax.examlock.ui.theme.LockOutlineMedium
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
    onOpenPerformanceProfile: () -> Unit,
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
        // Subtle top gradient accent â€” lightweight, no bitmap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        0f to LockBlue.copy(alpha = 0.05f),
                        1f to Color.Transparent
                    )
                )
        )

        val screenPadding = adaptiveScreenPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .responsiveContentWidth()
                .padding(start = screenPadding, end = screenPadding, top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LowRamHero(
                uiLanguage = uiLanguage,
                onUiLanguageChange = onUiLanguageChange,
                onSecretTap = onSecretTap,
                onOpenPerformanceProfile = onOpenPerformanceProfile
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
                glyphContainerColor = LockBlueTint,
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
                        .clip(RoundedCornerShape(UiTokens.RadiusMd))
                        .background(LockSurfaceSoft)
                        .border(1.dp, LockOutline.copy(alpha = 0.50f), RoundedCornerShape(UiTokens.RadiusMd))
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
    onSecretTap: () -> Unit,
    onOpenPerformanceProfile: () -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val badgePalette = lowRamProfileBadgePalette(lowRamProfile)
    val containerColor = Color(badgePalette.containerColorArgb)
    val contentColor = Color(badgePalette.contentColorArgb)
    val borderColor = Color(badgePalette.borderColorArgb)
    val dotColor = Color(badgePalette.dotColorArgb)
    val label = lowRamProfileBadgeLabel(lowRamProfile)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(Color.White)
            .border(1.dp, LockOutlineSubtle, RoundedCornerShape(UiTokens.RadiusLg))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .flatPill(containerColor = containerColor)
                        .border(1.dp, borderColor, RoundedCornerShape(UiTokens.RadiusPill))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onSecretTap
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                .background(dotColor)
                        )
                        LowRamText(
                            text = label,
                            color = contentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        )
                    }
                }
                PerformanceProfileGearButton(onClick = onOpenPerformanceProfile)
            }

            Row(
                modifier = Modifier
                    .flatPill(containerColor = Color(0xFFF4F7FB))
                    .border(1.dp, LockOutlineMedium, RoundedCornerShape(UiTokens.RadiusPill))
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
            .flatPill(containerColor = if (selected) LockBlue else Color.Transparent)
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
            .border(1.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon glyph container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(UiTokens.RadiusSm))
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
