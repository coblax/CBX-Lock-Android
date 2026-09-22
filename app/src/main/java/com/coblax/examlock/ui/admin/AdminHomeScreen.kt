package com.coblax.examlock.ui.admin

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.coblax.examlock.LowRamTier
import com.coblax.examlock.lowRamProfileBadgeLabel
import com.coblax.examlock.lowRamProfileBadgePalette
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.platform.openExternalUrl
import com.coblax.examlock.R
import com.coblax.examlock.StartupTrace
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.adaptiveScreenPadding
import com.coblax.examlock.ui.theme.flatCard
import com.coblax.examlock.ui.theme.flatCardElevated
import com.coblax.examlock.ui.theme.flatPill

import com.coblax.examlock.ui.theme.responsiveContentWidth
import com.coblax.examlock.ui.theme.UiTokens
import com.google.android.gms.tasks.Task

import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ExamLockHomeScreen(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeModeChange: (ThemeMode) -> Unit = {},
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
    val enableEntranceAnimations = !lowRamProfile.severe

    // ── Staggered entrance animation state ──
    var heroVisible by remember { mutableStateOf(!enableEntranceAnimations) }
    var button1Visible by remember { mutableStateOf(!enableEntranceAnimations) }
    var button2Visible by remember { mutableStateOf(!enableEntranceAnimations) }
    var button3Visible by remember { mutableStateOf(!enableEntranceAnimations) }
    var footerVisible by remember { mutableStateOf(!enableEntranceAnimations) }

    if (enableEntranceAnimations) {
        LaunchedEffect(Unit) {
            heroVisible = true
            delay(60)
            button1Visible = true
            delay(60)
            button2Visible = true
            delay(60)
            button3Visible = true
            delay(80)
            footerVisible = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (firstDrawMarked.compareAndSet(false, true)) {
                    StartupTrace.mark("home_first_frame", "severe=${lowRamProfile.severe}")
                }
            }
            .background(AppColors.current.background)
    ) {
        if (!compactHome) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            0f to AppColors.current.blueFill,
                            0.5f to AppColors.current.blueSoft.copy(alpha = 0.05f),
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
            // ── Hero card with entrance animation ──
            val heroAlpha by animateFloatAsState(
                targetValue = if (heroVisible) 1f else 0f,
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                label = "heroAlpha"
            )
            val heroTranslation by animateFloatAsState(
                targetValue = if (heroVisible) 0f else 24f,
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                label = "heroTranslation"
            )
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = heroAlpha
                    translationY = heroTranslation
                }
            ) {
                HomeHeroCard(
                    uiLanguage = uiLanguage,
                    onUiLanguageChange = onUiLanguageChange,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onSecretTap = onSecretTap,
                    onOpenPerformanceProfile = onOpenPerformanceProfile
                )
            }

            Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 12.dp))

            // ── Action buttons with staggered entrance ──
            StaggeredEntranceItem(visible = button1Visible) {
                HomeActionButton(
                    text = tr("SCAN EXAM QR", "SCAN QR UJIAN"),
                    subtitle = tr(
                        "Scan the exam QR to start. Your settings are already verified.",
                        "Pindai QR ujian untuk mulai. Pengaturan sudah diverifikasi."
                    ),
                    badgeText = tr("RECOMMENDED", "REKOMENDASI"),
                    icon = { Icons.Rounded.QrCodeScanner },
                    severeGlyph = "QR",
                    containerColor = AppColors.current.blue,
                    contentColor = AppColors.current.onDark,
                    borderColor = AppColors.current.blue,
                    iconContainerColor = Color.White.copy(alpha = 0.16f),
                    onClick = onScanExam
                )
            }

            Spacer(modifier = Modifier.height(if (compactHome) 10.dp else 14.dp))

            StaggeredEntranceItem(visible = button2Visible) {
                HomeActionButton(
                    text = tr("CUSTOM QR (ADMIN)", "CUSTOM QR (ADMIN)"),
                    subtitle = tr(
                        "Create a new exam QR for admin tasks like scheduling or trial checks.",
                        "Buat QR ujian baru untuk kebutuhan admin seperti jadwal atau uji coba."
                    ),
                    badgeText = "ADMIN",
                    icon = { Icons.Rounded.AdminPanelSettings },
                    severeGlyph = "AD",
                    containerColor = AppColors.current.cardBg,
                    contentColor = AppColors.current.blue,
                    borderColor = AppColors.current.outline,
                    iconContainerColor = AppColors.current.blueFill,
                    onClick = onOpenAdmin
                )
            }

            Spacer(modifier = Modifier.height(if (compactHome) 10.dp else 14.dp))

            StaggeredEntranceItem(visible = button3Visible) {
                HomeActionButton(
                    text = directLinkLabel,
                    subtitle = tr(
                        "Open the exam quickly when you already have the link.",
                        "Buka ujian cepat saat sudah punya link."
                    ),
                    badgeText = tr("DIRECT LINK", "LINK LANGSUNG"),
                    icon = { Icons.Rounded.Language },
                    severeGlyph = "GO",
                    containerColor = AppColors.current.gold.copy(alpha = 0.22f),
                    contentColor = AppColors.current.brandText,
                    borderColor = AppColors.current.gold.copy(alpha = 0.55f),
                    iconContainerColor = AppColors.current.blueDeep.copy(alpha = 0.08f),
                    onClick = onOpenFastExam
                )
            }

            if (showDeferredChrome) {
                Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 18.dp))

                StaggeredEntranceItem(visible = footerVisible) {
                    DeveloperInfo()
                }

                Spacer(modifier = Modifier.height(12.dp))

                val footerAlpha by animateFloatAsState(
                    targetValue = if (footerVisible) 1f else 0f,
                    animationSpec = tween(250),
                    label = "footerAlpha"
                )
                Text(
                    text = tr(
                        "Production build - Version $versionLabel",
                        "Build produksi - Versi $versionLabel"
                    ),
                    color = AppColors.current.textMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = footerAlpha }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Wrapper that applies a staggered slide-up + fade-in entrance to its [content].
 */
@Composable
private fun StaggeredEntranceItem(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "staggerTranslation"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeHeroCard(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeModeChange: (ThemeMode) -> Unit = {},
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
                horizontal = if (compactHome) 14.dp else 15.dp,
                vertical = if (compactHome) 12.dp else 13.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // FlowRow (not Row) so the control strip wraps onto a second line instead of
        // overflowing the card edge on narrow screens or at large system font scales.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeTogglePill(
                    currentThemeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
                LanguageTogglePill(
                    currentLanguage = uiLanguage,
                    onLanguageChange = onUiLanguageChange
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 13.dp))

        CoblaxFrontBrand(uiLanguage = uiLanguage)
    }
}

@Composable
internal fun ProductionBuildBadge(
    uiLanguage: UiLanguage,
    onSecretTap: () -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val colors = AppColors.current
    // lowRamProfileBadgePalette() is shared with the pre-Compose launcher view, which is
    // always light. In Compose the badge has to follow the active theme instead.
    val badgePalette = lowRamProfileBadgePalette(lowRamProfile)
    val containerColor: Color
    val contentColor: Color
    val borderColor: Color
    val dotColor: Color
    if (colors.isDark) {
        containerColor = colors.surfaceSoft
        contentColor = colors.textPrimary
        borderColor = colors.outlineMedium
        dotColor = when (lowRamProfile.tier) {
            LowRamTier.Normal -> colors.blue
            LowRamTier.Low -> colors.gold
            LowRamTier.Ultra -> colors.goldAccent
        }
    } else {
        containerColor = Color(badgePalette.containerColorArgb)
        contentColor = Color(badgePalette.contentColorArgb)
        borderColor = Color(badgePalette.borderColorArgb)
        dotColor = Color(badgePalette.dotColorArgb)
    }
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
            letterSpacing = 0.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
internal fun LanguageTogglePill(
    currentLanguage: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit
) {
    // Kept deliberately narrow: the header strip also carries the profile badge,
    // the performance gear and the theme toggle. A decorative "LANG" caption here
    // pushed the EN/ID chips past the card edge, where they were clipped and the
    // label wrapped to one letter per line.
    Row(
        modifier = Modifier
            .flatPill(
                containerColor = AppColors.current.cardBg.copy(alpha = 0.98f),
                borderColor = AppColors.current.outline,
                borderAlpha = 0.70f
            )
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LanguageOptionChip(
            label = "EN",
            clickLabel = tr("Switch to English", "Ganti ke Bahasa Inggris"),
            selected = currentLanguage == UiLanguage.English,
            onClick = { onLanguageChange(UiLanguage.English) }
        )
        LanguageOptionChip(
            label = "ID",
            clickLabel = tr("Switch to Indonesian", "Ganti ke Bahasa Indonesia"),
            selected = currentLanguage == UiLanguage.Indonesian,
            onClick = { onLanguageChange(UiLanguage.Indonesian) }
        )
    }
}

@Composable
internal fun LanguageOptionChip(
    label: String,
    clickLabel: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .flatPill(containerColor = if (selected) AppColors.current.blue else AppColors.current.surfaceSoft)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = clickLabel,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AppColors.current.onDark else AppColors.current.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Compact pill button that cycles through [ThemeMode] values:
 * System → Light → Dark → System.
 *
 * Features:
 * - Animated icon crossfade with scale transition
 * - 180° rotation animation on each tap
 * - Mode-adaptive accent glow border
 */
@Composable
internal fun ThemeTogglePill(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val nextMode = when (currentThemeMode) {
        ThemeMode.System -> ThemeMode.Light
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.System
    }

    // Rotation: accumulates 180° per tap for smooth continuous spin
    var rotationTarget by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "themeRotation"
    )

    // Accent glow based on active mode
    val accentBorderColor = when (currentThemeMode) {
        ThemeMode.Light -> AppColors.current.gold.copy(alpha = 0.55f)
        ThemeMode.Dark -> AppColors.current.blue.copy(alpha = 0.50f)
        ThemeMode.System -> AppColors.current.outline.copy(alpha = 0.70f)
    }

    Box(
        modifier = Modifier
            .flatPill(
                containerColor = AppColors.current.cardBg.copy(alpha = 0.98f),
                borderColor = accentBorderColor,
                borderAlpha = 1f
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = {
                    rotationTarget += 180f
                    onThemeModeChange(nextMode)
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentThemeMode,
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.7f,
                    animationSpec = tween(200)
                )) togetherWith (fadeOut(tween(150)) + scaleOut(
                    targetScale = 0.7f,
                    animationSpec = tween(150)
                ))
            },
            label = "themeIcon"
        ) { mode ->
            val icon = when (mode) {
                ThemeMode.System -> Icons.Rounded.BrightnessAuto
                ThemeMode.Light -> Icons.Rounded.LightMode
                ThemeMode.Dark -> Icons.Rounded.DarkMode
            }
            Icon(
                imageVector = icon,
                contentDescription = when (mode) {
                    ThemeMode.System -> tr("Theme: system", "Tema: sistem")
                    ThemeMode.Light -> tr("Theme: light", "Tema: terang")
                    ThemeMode.Dark -> tr("Theme: dark", "Tema: gelap")
                },
                tint = AppColors.current.textPrimary,
                modifier = Modifier.size(18.dp).graphicsLayer {
                    rotationZ = rotation
                }
            )
        }
    }
}

@Composable
internal fun CoblaxFrontBrand(uiLanguage: UiLanguage) {
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi
    val logoSize = if (compactHome) 96.dp else 112.dp
    val titleSize = if (compactHome) 26.sp else 27.sp
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

        Spacer(modifier = Modifier.height(if (compactHome) 4.dp else 6.dp))

        Text(
            text = "CBX Lock",
            color = AppColors.current.brandText,
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "COBLAX EXAM LOCK",
            color = AppColors.current.blueMid,
            fontSize = subtitleSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )

        Spacer(modifier = Modifier.height(if (compactHome) 8.dp else 10.dp))

        Text(
            text = localized(
                uiLanguage,
                "Keeps online exams focused and safer from cheating by locking the device and guiding students to the official exam page.",
                "Menjaga ujian online tetap fokus dan lebih aman dari kecurangan dengan mengunci perangkat serta mengarahkan siswa ke halaman ujian resmi."
            ),
            color = AppColors.current.textSecondary,
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
                .background(AppColors.current.blueDeep),
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
        // The launcher foreground bitmap ships with an opaque white plate, which reads
        // as a glaring white square on the dark card. Clipping it to the same rounded
        // tile the severe branch uses turns that plate into a deliberate app-icon tile.
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = modifier
                .clip(RoundedCornerShape(26.dp))
                .then(
                    if (AppColors.current.isDark) {
                        Modifier.border(
                            width = 1.dp,
                            color = AppColors.current.outlineMedium,
                            shape = RoundedCornerShape(26.dp)
                        )
                    } else {
                        Modifier
                    }
                )
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
            .background(AppColors.current.cardBg)
            .border(1.dp, AppColors.current.outlineMedium, RoundedCornerShape(UiTokens.RadiusLg))
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
                color = AppColors.current.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "github.com/coblax",
                color = AppColors.current.brandText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .flatPill(containerColor = AppColors.current.blueTint)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = tr("OPEN", "BUKA"),
                color = AppColors.current.brandText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
