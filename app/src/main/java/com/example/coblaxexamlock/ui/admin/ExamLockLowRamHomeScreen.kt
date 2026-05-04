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
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LowRamHero(
                uiLanguage = uiLanguage,
                onUiLanguageChange = onUiLanguageChange,
                onSecretTap = onSecretTap
            )

            Spacer(modifier = Modifier.height(12.dp))

            LowRamHomeButton(
                text = tr("SCAN EXAM QR", "SCAN QR UJIAN"),
                subtitle = tr(
                    "Scan the exam QR to start. Your settings are already verified.",
                    "Pindai QR ujian untuk mulai. Pengaturan sudah diverifikasi."
                ),
                badgeText = tr("RECOMMENDED", "REKOMENDASI"),
                glyph = "QR",
                containerColor = LockBlue,
                contentColor = LockOnDark,
                borderColor = LockBlue,
                onClick = onScanExam
            )

            Spacer(modifier = Modifier.height(10.dp))

            LowRamHomeButton(
                text = tr("CUSTOM QR (ADMIN)", "CUSTOM QR (ADMIN)"),
                subtitle = tr(
                    "Create a new exam QR for admin tasks like scheduling or trial checks.",
                    "Buat QR ujian baru untuk kebutuhan admin seperti jadwal atau uji coba."
                ),
                badgeText = "ADMIN",
                glyph = "AD",
                containerColor = Color.White,
                contentColor = LockBlue,
                borderColor = LockOutline,
                onClick = onOpenAdmin
            )

            Spacer(modifier = Modifier.height(10.dp))

            LowRamHomeButton(
                text = directLinkLabel,
                subtitle = tr(
                    "Open the exam quickly when you already have the link.",
                    "Buka ujian cepat saat sudah punya link."
                ),
                badgeText = tr("DIRECT LINK", "LINK LANGSUNG"),
                glyph = "GO",
                containerColor = LockGold.copy(alpha = 0.22f),
                contentColor = LockBlueDeep,
                borderColor = LockGold.copy(alpha = 0.55f),
                onClick = onOpenFastExam
            )

            if (showDeferredChrome) {
                Spacer(modifier = Modifier.height(12.dp))
                LowRamText(
                    text = "Developer: COBLAX",
                    color = LockTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                LowRamText(
                    text = DeveloperGithubUrl,
                    color = LockTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                LowRamText(
                    text = tr(
                        "Production build - Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        "Build produksi - Versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    ),
                    color = LockTextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
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
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, LockOutline.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(LockBlueDeep)
                    .border(1.dp, LockBlue.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onSecretTap
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.94f))
                )
                LowRamText(
                    text = tr("PRODUCTION", "PRODUKSI"),
                    color = LockOnDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LowRamLanguageButton(
                    text = "ID",
                    selected = uiLanguage == UiLanguage.Indonesian,
                    onClick = { onUiLanguageChange(UiLanguage.Indonesian) }
                )
                LowRamLanguageButton(
                    text = "EN",
                    selected = uiLanguage == UiLanguage.English,
                    onClick = { onUiLanguageChange(UiLanguage.English) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LowRamText(
            text = "CBX",
            color = LockBlueDeep,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        LowRamText(
            text = "EXAM LOCK",
            color = LockBlue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        LowRamText(
            text = tr(
                "Secure exam entry for low-memory Android devices.",
                "Akses ujian aman untuk perangkat Android low-memory."
            ),
            color = LockTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun LowRamLanguageButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) LockBlueDeep else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        LowRamText(
            text = text,
            color = if (selected) LockOnDark else LockBlueDeep,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.10f))
                .border(1.dp, contentColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                .padding(10.dp)
                .size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            LowRamText(
                text = glyph,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            LowRamText(
                text = badgeText,
                color = contentColor.copy(alpha = 0.72f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
            LowRamText(
                text = text,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            LowRamText(
                text = subtitle,
                color = contentColor.copy(alpha = 0.76f),
                fontSize = 12.sp,
                lineHeight = 16.sp
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
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight
        )
    )
}
