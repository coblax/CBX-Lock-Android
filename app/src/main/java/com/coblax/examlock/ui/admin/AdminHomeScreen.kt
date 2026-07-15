package com.coblax.examlock.ui.admin

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.coblax.examlock.BuildConfig
import com.coblax.examlock.config.DeveloperGithubUrl
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.lowRamProfileBadgeLabel
import com.coblax.examlock.lowRamProfileBadgePalette
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.platform.openExternalUrl
import com.coblax.examlock.R
import com.coblax.examlock.StartupTrace
import com.coblax.examlock.ui.theme.adaptiveScreenPadding
import com.coblax.examlock.ui.theme.flatCard
import com.coblax.examlock.ui.theme.flatCardElevated
import com.coblax.examlock.ui.theme.flatPill
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockBlueMid
import com.coblax.examlock.ui.theme.LockBlueSoft
import com.coblax.examlock.ui.theme.LockCardBg
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.responsiveContentWidth
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.LockBlueFill
import com.coblax.examlock.ui.theme.LockOutlineMedium
import com.coblax.examlock.ui.theme.LockBlueTint
import com.google.android.gms.tasks.Task

import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun ExamLockHomeScreen(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onScanExam: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenFastExam: () -> Unit,
    directLinkLabel: String,
    onSecretTap: () -> Unit,
    onOpenPerformanceProfile: () -> Unit,
    showDeferredChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    val versionLabel = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi
    val firstDrawMarked = remember { AtomicBoolean(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (firstDrawMarked.compareAndSet(false, true)) {
                    StartupTrace.mark("home_first_frame", "severe=${lowRamProfile.severe}")
                }
            }
            .background(LockBackground)
    ) {
        if (!compactHome) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            0f to LockBlueFill,
                            0.5f to LockBlueSoft.copy(alpha = 0.05f),
                            1f to Color.Transparent
                        )
                    )
            )
        }

        val screenPadding = adaptiveScreenPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .responsiveContentWidth()
                .padding(
                    start = screenPadding,
                    end = screenPadding,
                    top = if (compactHome) 12.dp else 18.dp,
                    bottom = if (compactHome) 12.dp else 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeHeroCard(
                uiLanguage = uiLanguage,
                onUiLanguageChange = onUiLanguageChange,
                onSecretTap = onSecretTap,
                onOpenPerformanceProfile = onOpenPerformanceProfile
            )

            Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 18.dp))

            HomeActionButton(
                text = tr("SCAN EXAM QR", "SCAN QR UJIAN"),
                subtitle = tr(
                    "Scan the exam QR to start. Your settings are already verified.",
                    "Pindai QR ujian untuk mulai. Pengaturan sudah diverifikasi."
                ),
                badgeText = tr("RECOMMENDED", "REKOMENDASI"),
                icon = { Icons.Rounded.QrCodeScanner },
                severeGlyph = "QR",
                containerColor = LockBlue,
                contentColor = LockOnDark,
                borderColor = LockBlue,
                iconContainerColor = Color.White.copy(alpha = 0.16f),
                onClick = onScanExam
            )

            Spacer(modifier = Modifier.height(if (compactHome) 10.dp else 14.dp))

            HomeActionButton(
                text = tr("CUSTOM QR (ADMIN)", "CUSTOM QR (ADMIN)"),
                subtitle = tr(
                    "Create a new exam QR for admin tasks like scheduling or trial checks.",
                    "Buat QR ujian baru untuk kebutuhan admin seperti jadwal atau uji coba."
                ),
                badgeText = "ADMIN",
                icon = { Icons.Rounded.AdminPanelSettings },
                severeGlyph = "AD",
                containerColor = Color.White,
                contentColor = LockBlue,
                borderColor = LockOutline,
                iconContainerColor = LockBlueFill,
                onClick = onOpenAdmin
            )

            Spacer(modifier = Modifier.height(if (compactHome) 10.dp else 14.dp))

            HomeActionButton(
                text = directLinkLabel,
                subtitle = tr(
                    "Open the exam quickly when you already have the link.",
                    "Buka ujian cepat saat sudah punya link."
                ),
                badgeText = tr("DIRECT LINK", "LINK LANGSUNG"),
                icon = { Icons.Rounded.Language },
                severeGlyph = "GO",
                containerColor = LockGold.copy(alpha = 0.22f),
                contentColor = LockBlueDeep,
                borderColor = LockGold.copy(alpha = 0.55f),
                iconContainerColor = LockBlueDeep.copy(alpha = 0.08f),
                onClick = onOpenFastExam
            )

            if (showDeferredChrome) {
                Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 18.dp))

                DeveloperInfo()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr(
                        "Production build - Version $versionLabel",
                        "Build produksi - Versi $versionLabel"
                    ),
                    color = LockTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HomeActionButton(
    text: String,
    subtitle: String,
    badgeText: String,
    icon: () -> androidx.compose.ui.graphics.vector.ImageVector,
    severeGlyph: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    iconContainerColor: Color,
    onClick: () -> Unit
) {
    if (LocalLowRamProfile.current.severe) {
        ActionButton(
            text = text,
            subtitle = subtitle,
            badgeText = badgeText,
            iconContent = {
                LightweightHomeGlyph(
                    text = severeGlyph,
                    color = contentColor
                )
            },
            containerColor = containerColor,
            contentColor = contentColor,
            borderColor = borderColor,
            iconContainerColor = iconContainerColor,
            onClick = onClick
        )
    } else {
        ActionButton(
            text = text,
            subtitle = subtitle,
            badgeText = badgeText,
            icon = icon(),
            containerColor = containerColor,
            contentColor = contentColor,
            borderColor = borderColor,
            iconContainerColor = iconContainerColor,
            onClick = onClick
        )
    }
}

@Composable
private fun LightweightHomeGlyph(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .padding(12.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun HomeHeroCard(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onSecretTap: () -> Unit,
    onOpenPerformanceProfile: () -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (compactHome) Modifier.flatCard(radius = 22.dp)
                else Modifier.flatCardElevated(radius = 26.dp)
            )
            .padding(
                horizontal = if (compactHome) 14.dp else 18.dp,
                vertical = if (compactHome) 12.dp else 16.dp
            ),
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
                ProductionBuildBadge(
                    uiLanguage = uiLanguage,
                    onSecretTap = onSecretTap
                )
                PerformanceProfileGearButton(onClick = onOpenPerformanceProfile)
            }

            LanguageTogglePill(
                currentLanguage = uiLanguage,
                onLanguageChange = onUiLanguageChange
            )
        }

        Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 18.dp))

        CoblaxFrontBrand(uiLanguage = uiLanguage)
    }
}

@Composable
internal fun ProductionBuildBadge(
    uiLanguage: UiLanguage,
    onSecretTap: () -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val badgePalette = lowRamProfileBadgePalette(lowRamProfile)
    val containerColor = Color(badgePalette.containerColorArgb)
    val contentColor = Color(badgePalette.contentColorArgb)
    val borderColor = Color(badgePalette.borderColorArgb)
    val dotColor = Color(badgePalette.dotColorArgb)
    val label = lowRamProfileBadgeLabel(lowRamProfile)

    Row(
        modifier = Modifier
            .flatPill(
                containerColor = containerColor,
                borderColor = borderColor,
                borderAlpha = 1f
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onSecretTap
            )
            .heightIn(min = 30.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = localized(uiLanguage, label, label),
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
internal fun LanguageTogglePill(
    currentLanguage: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi

    Row(
        modifier = Modifier
            .flatPill(
                containerColor = LockCardBg.copy(alpha = 0.98f),
                borderColor = LockOutline,
                borderAlpha = 0.70f
            )
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(LockBlueTint)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (!compactHome) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = tr("Change language", "Ubah bahasa"),
                    tint = LockBlueDeep,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = "LANG",
                color = LockBlueDeep,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageOptionChip(
                label = "EN",
                selected = currentLanguage == UiLanguage.English,
                onClick = { onLanguageChange(UiLanguage.English) }
            )
            LanguageOptionChip(
                label = "ID",
                selected = currentLanguage == UiLanguage.Indonesian,
                onClick = { onLanguageChange(UiLanguage.Indonesian) }
            )
        }
    }
}

@Composable
internal fun LanguageOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .flatPill(containerColor = if (selected) LockBlue else LockSurfaceSoft)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) LockOnDark else LockTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun CoblaxFrontBrand(uiLanguage: UiLanguage) {
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi
    val logoSize = if (compactHome) 96.dp else 140.dp
    val titleSize = if (compactHome) 26.sp else 30.sp
    val subtitleSize = if (compactHome) 12.sp else 14.sp
    val bodySize = if (compactHome) 12.sp else 13.sp
    val bodyLineHeight = if (compactHome) 16.sp else 18.sp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoblaxLogoMark(
            modifier = Modifier.size(logoSize)
        )

        Spacer(modifier = Modifier.height(if (compactHome) 4.dp else 8.dp))

        Text(
            text = "CBX Lock",
            color = LockBlueDeep,
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "COBLAX EXAM LOCK",
            color = LockBlueMid,
            fontSize = subtitleSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )

        Spacer(modifier = Modifier.height(if (compactHome) 8.dp else 12.dp))

        Text(
            text = localized(
                uiLanguage,
                "Keeps online exams focused and safer from cheating by locking the device and guiding students to the official exam page.",
                "Menjaga ujian online tetap fokus dan lebih aman dari kecurangan dengan mengunci perangkat serta mengarahkan siswa ke halaman ujian resmi."
            ),
            color = LockTextSecondary,
            fontSize = bodySize,
            lineHeight = bodyLineHeight,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun CoblaxLogoMark(modifier: Modifier = Modifier) {
    if (LocalLowRamProfile.current.severe) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(26.dp))
                .background(LockBlueDeep),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CBX",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
    } else {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = modifier
        )
    }
}

@Composable
internal fun DeveloperInfo() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(UiTokens.RadiusLg))
            .background(LockCardBg)
            .border(1.dp, LockOutlineMedium, RoundedCornerShape(UiTokens.RadiusLg))
            .clickable { openExternalUrl(context, DeveloperGithubUrl) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = tr("Developer", "Pengembang"),
                color = LockTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "github.com/coblax",
                color = LockBlueDeep,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .flatPill(containerColor = LockBlueTint)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = tr("OPEN", "BUKA"),
                color = LockBlueDeep,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
