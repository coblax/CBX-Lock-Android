package com.coblax.examlock.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.LowRamProfileOverride
import com.coblax.examlock.LowRamTier
import com.coblax.examlock.R
import com.coblax.examlock.StartupTrace
import com.coblax.examlock.config.DeveloperGithubUrl
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.lowRamProfileBadgeLabel
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.platform.openExternalUrl
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.InScreenSheetHost
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.ui.theme.adaptiveScreenPadding
import com.coblax.examlock.ui.theme.primaryActionColors
import java.util.concurrent.atomic.AtomicBoolean

internal object HomeUiTestTags {
    const val ScanAction = "home_scan_action"
    const val DirectLinkAction = "home_direct_link_action"
    const val AdminAction = "home_admin_action"
    const val SettingsAction = "home_settings_action"
    const val ProfileBadge = "home_profile_badge"
    const val SettingsSheet = "home_settings_sheet"
    const val SettingsSheetScrim = "home_settings_sheet_scrim"
    const val SettingsSheetClose = "home_settings_sheet_close"
}

/** Keeps the dashboard phone-shaped on tablets instead of stretching edge to edge. */
private val HomeContentMaxWidth = 560.dp

/**
 * The single home dashboard for every device profile. It stays light enough for the
 * low-RAM survival shell: no bitmaps on severe profiles, no entrance animations, and
 * everything below the main actions waits for [showDeferredChrome].
 */
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
    val lowRamProfile = LocalLowRamProfile.current
    val firstDrawMarked = remember { AtomicBoolean(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    UpgradeUiScope {
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
            val itemModifier = Modifier
                .widthIn(max = HomeContentMaxWidth)
                .fillMaxWidth()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // While settings are open the sheet is modal for TalkBack too.
                    .then(if (settingsOpen) Modifier.clearAndSetSemantics {} else Modifier)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptiveScreenPadding(), vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HomeTopBar(
                    uiLanguage = uiLanguage,
                    onUiLanguageChange = onUiLanguageChange,
                    onOpenSettings = { settingsOpen = true },
                    modifier = itemModifier
                )
                Spacer(modifier = Modifier.height(24.dp))
                HomeGreeting(modifier = itemModifier)
                Spacer(modifier = Modifier.height(16.dp))
                HomeScanCard(onClick = onScanExam, modifier = itemModifier)
                Spacer(modifier = Modifier.height(12.dp))
                HomeSecondaryActions(
                    directLinkLabel = directLinkLabel,
                    onOpenFastExam = onOpenFastExam,
                    onOpenAdmin = onOpenAdmin,
                    modifier = itemModifier
                )
                if (showDeferredChrome) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HomeTipsCard(modifier = itemModifier)
                }
                Spacer(modifier = Modifier.height(24.dp))
                HomeFooter(
                    onSecretTap = onSecretTap,
                    showDeveloperLink = showDeferredChrome,
                    modifier = itemModifier
                )
            }

            InScreenSheetHost(
                value = if (settingsOpen) Unit else null,
                onDismiss = { settingsOpen = false },
                closeLabel = tr("Close settings", "Tutup pengaturan"),
                maxWidth = HomeContentMaxWidth,
                sheetTag = HomeUiTestTags.SettingsSheet,
                scrimTag = HomeUiTestTags.SettingsSheetScrim
            ) {
                HomeSettingsSheetContent(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    uiLanguage = uiLanguage,
                    onUiLanguageChange = onUiLanguageChange,
                    onOpenPerformanceProfile = onOpenPerformanceProfile,
                    onDismiss = { settingsOpen = false }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Header and greeting
// ──────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    // With an enlarged font the header cannot fit name, language chips, and gear on one
    // line; the language switch stays reachable in Settings.
    val compact = LocalDensity.current.fontScale > 1.3f
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoblaxLogoMark(
            modifier = Modifier.size(44.dp),
            cornerRadius = 12.dp
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() }
        ) {
            Text(
                text = "CBX Lock",
                color = AppColors.current.brandText,
                style = AppTextStyles.cardTitle.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                Text(
                    text = "Coblax Exam Lock",
                    color = AppColors.current.textSecondary,
                    style = AppTextStyles.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!compact) {
            HomeLanguageSwitch(
                currentLanguage = uiLanguage,
                onLanguageChange = onUiLanguageChange
            )
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(tokens.touchTarget)
                .testTag(HomeUiTestTags.SettingsAction)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = tr("Settings", "Pengaturan"),
                tint = AppColors.current.brandText
            )
        }
    }
}

@Composable
private fun HomeLanguageSwitch(
    currentLanguage: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit
) {
    val tokens = ScopedUiTokens.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.radiusPill))
            .background(AppColors.current.surfaceSoft)
            .border(1.dp, AppColors.current.outlineMedium, RoundedCornerShape(tokens.radiusPill))
            .padding(3.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple(UiLanguage.Indonesian, "ID", tr("Indonesian", "Bahasa Indonesia")),
            Triple(UiLanguage.English, "EN", tr("English", "Bahasa Inggris"))
        ).forEach { (language, code, description) ->
            val selected = currentLanguage == language
            val colors = primaryActionColors()
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(tokens.radiusPill))
                    .background(if (selected) colors.container else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onLanguageChange(language) }
                    )
                    .semantics { contentDescription = description }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code,
                    color = if (selected) colors.content else AppColors.current.textSecondary,
                    style = AppTextStyles.label.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeGreeting(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = tr("Ready for your exam?", "Siap ujian?"),
            color = AppColors.current.textPrimary,
            style = AppTextStyles.screenTitle,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = tr(
                "Scan the QR from your proctor, or open the saved exam link.",
                "Pindai QR dari pengawas, atau buka link ujian yang tersimpan."
            ),
            color = AppColors.current.textSecondary,
            style = AppTextStyles.bodyCompact
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Actions
// ──────────────────────────────────────────────────────────────

@Composable
private fun HomeScanCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    // Large fonts need the width for the label, not for decoration.
    val largeFont = LocalDensity.current.fontScale > 1.3f
    val colors = primaryActionColors()
    val shape = RoundedCornerShape(tokens.radiusLarge)
    Row(
        modifier = modifier
            .testTag(HomeUiTestTags.ScanAction)
            .heightIn(min = 96.dp)
            .clip(shape)
            .background(colors.container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (largeFont) 44.dp else 56.dp)
                .clip(RoundedCornerShape(tokens.radiusMedium))
                .background(colors.content.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(if (largeFont) 24.dp else 30.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = tr("Scan exam QR", "Pindai QR ujian"),
                color = colors.content,
                style = AppTextStyles.sectionTitle
            )
            Text(
                text = tr(
                    "Point the camera at the QR from your proctor.",
                    "Arahkan kamera ke QR dari pengawas."
                ),
                color = colors.content,
                style = AppTextStyles.bodyCompact
            )
        }
        if (!largeFont) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = colors.content
            )
        }
    }
}

/**
 * Two tiles side by side; they stack when the phone is narrow or the system font is
 * enlarged so the saved link name never gets cut to a couple of letters.
 */
@Composable
private fun HomeSecondaryActions(
    directLinkLabel: String,
    onOpenFastExam: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier) {
        val spacing = 12.dp
        val minTileWidth = 132.dp * fontScale.coerceIn(1f, 2f)
        val sideBySide = maxWidth >= minTileWidth * 2 + spacing
        val directLink: @Composable (Modifier) -> Unit = { tileModifier ->
            HomeActionTile(
                icon = Icons.Rounded.Link,
                label = tr("Saved exam link", "Link ujian tersimpan"),
                value = directLinkLabel,
                onClick = onOpenFastExam,
                testTag = HomeUiTestTags.DirectLinkAction,
                modifier = tileModifier
            )
        }
        val admin: @Composable (Modifier) -> Unit = { tileModifier ->
            HomeActionTile(
                icon = Icons.Rounded.AdminPanelSettings,
                label = tr("For admins", "Untuk admin"),
                value = "Custom QR",
                onClick = onOpenAdmin,
                testTag = HomeUiTestTags.AdminAction,
                modifier = tileModifier
            )
        }
        if (sideBySide) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                directLink(Modifier.weight(1f).fillMaxHeight())
                admin(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                directLink(Modifier.fillMaxWidth())
                admin(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HomeActionTile(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val tokens = ScopedUiTokens.current
    val shape = RoundedCornerShape(tokens.radiusLarge)
    Column(
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = 104.dp)
            .clip(shape)
            .background(AppColors.current.cardBg)
            .border(1.dp, AppColors.current.outlineMedium, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.current.blueTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.current.brandText,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = AppColors.current.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = label,
                color = AppColors.current.textSecondary,
                style = AppTextStyles.label
            )
            if (value.none { it.isWhitespace() }) {
                // A one-word name such as EXAM_SKANSATP has no break point, so wrapping would
                // split it mid-word. Shrink it to fit one line instead.
                BasicText(
                    text = value,
                    style = AppTextStyles.cardTitle.copy(color = AppColors.current.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 11.sp,
                        maxFontSize = AppTextStyles.cardTitle.fontSize,
                        stepSize = 1.sp
                    )
                )
            } else {
                Text(
                    text = value,
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.cardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Tips and footer
// ──────────────────────────────────────────────────────────────

/**
 * Static reminders for the checks that most often block Start Exam. Nothing here
 * runs a detector, so it costs nothing on low-RAM phones.
 */
@Composable
private fun HomeTipsCard(modifier: Modifier = Modifier) {
    val tokens = ScopedUiTokens.current
    val shape = RoundedCornerShape(tokens.radiusLarge)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppColors.current.surfaceSoft)
            .border(1.dp, AppColors.current.outlineSubtle, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = tr("Before the exam", "Sebelum ujian"),
            color = AppColors.current.textSecondary,
            style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics { heading() }
        )
        HomeTipRow(
            icon = Icons.Rounded.BatteryChargingFull,
            text = tr(
                "Make sure the battery is charged, or plug in a charger.",
                "Pastikan baterai cukup, atau pasang charger."
            )
        )
        HomeTipRow(
            icon = Icons.Rounded.BluetoothDisabled,
            text = tr(
                "Turn off Bluetooth, VPN, and screen recorders.",
                "Matikan Bluetooth, VPN, dan perekam layar."
            )
        )
        HomeTipRow(
            icon = Icons.Rounded.LocationOn,
            text = tr(
                "Allow location if the app asks for it.",
                "Izinkan lokasi jika aplikasi memintanya."
            )
        )
    }
}

@Composable
private fun HomeTipRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.current.brandText,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            color = AppColors.current.textPrimary,
            style = AppTextStyles.bodyCompact
        )
    }
}

@Composable
private fun HomeFooter(
    onSecretTap: () -> Unit,
    showDeveloperLink: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeProfileBadge(onSecretTap = onSecretTap)
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = AppColors.current.textMuted,
                style = AppTextStyles.label
            )
        }
        if (showDeveloperLink) {
            TextButton(onClick = { openExternalUrl(context, DeveloperGithubUrl) }) {
                Text(
                    text = "github.com/coblax",
                    color = AppColors.current.brandText,
                    style = AppTextStyles.label
                )
            }
        }
    }
}

/**
 * Shows the active performance profile. It is also the hidden Secret Admin trigger
 * (four quick taps), so it deliberately gives no visual tap feedback.
 */
@Composable
private fun HomeProfileBadge(onSecretTap: () -> Unit) {
    val colors = AppColors.current
    val lowRamProfile = LocalLowRamProfile.current
    val dotColor = when (lowRamProfile.tier) {
        LowRamTier.Normal -> colors.blue
        LowRamTier.Low -> colors.gold
        LowRamTier.Ultra -> colors.goldAccent
    }
    Row(
        modifier = Modifier
            .testTag(HomeUiTestTags.ProfileBadge)
            .clip(RoundedCornerShape(ScopedUiTokens.current.radiusPill))
            .background(colors.surfaceSoft)
            .border(1.dp, colors.outlineMedium, RoundedCornerShape(ScopedUiTokens.current.radiusPill))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onSecretTap
            )
            .heightIn(min = 32.dp)
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
            text = lowRamProfileBadgeLabel(lowRamProfile),
            color = colors.textSecondary,
            style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Settings sheet
// ──────────────────────────────────────────────────────────────

@Composable
private fun HomeSettingsSheetContent(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onOpenPerformanceProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = ScopedUiTokens.current
    val context = LocalContext.current
    val lowRamProfile = LocalLowRamProfile.current
    val title = tr("Settings", "Pengaturan")
    Column(modifier = Modifier.semantics { paneTitle = title }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = tokens.spaceLarge, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = AppColors.current.textPrimary,
                style = AppTextStyles.sectionTitle,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(tokens.touchTarget)
                    .testTag(HomeUiTestTags.SettingsSheetClose)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = tr("Close settings", "Tutup pengaturan"),
                    tint = AppColors.current.textSecondary
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = tokens.spaceLarge,
                    end = tokens.spaceLarge,
                    top = tokens.spaceSmall,
                    bottom = tokens.spaceLarge
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HomeSettingsGroup(title = tr("Appearance", "Tampilan")) {
                HomeSegmentedControl(
                    options = listOf(
                        HomeSegmentOption(
                            label = tr("System", "Sistem"),
                            icon = Icons.Rounded.BrightnessAuto,
                            description = tr("Theme: system", "Tema: sistem")
                        ),
                        HomeSegmentOption(
                            label = tr("Light", "Terang"),
                            icon = Icons.Rounded.LightMode,
                            description = tr("Theme: light", "Tema: terang")
                        ),
                        HomeSegmentOption(
                            label = tr("Dark", "Gelap"),
                            icon = Icons.Rounded.DarkMode,
                            description = tr("Theme: dark", "Tema: gelap")
                        )
                    ),
                    selectedIndex = when (themeMode) {
                        ThemeMode.System -> 0
                        ThemeMode.Light -> 1
                        ThemeMode.Dark -> 2
                    },
                    onSelect = { index ->
                        onThemeModeChange(
                            when (index) {
                                1 -> ThemeMode.Light
                                2 -> ThemeMode.Dark
                                else -> ThemeMode.System
                            }
                        )
                    }
                )
            }
            HomeSettingsGroup(title = tr("Language", "Bahasa")) {
                HomeSegmentedControl(
                    options = listOf(
                        HomeSegmentOption(label = "Indonesia"),
                        HomeSegmentOption(label = "English")
                    ),
                    selectedIndex = if (uiLanguage == UiLanguage.English) 1 else 0,
                    onSelect = { index ->
                        onUiLanguageChange(if (index == 1) UiLanguage.English else UiLanguage.Indonesian)
                    }
                )
            }
            HomeSettingsGroup(title = tr("Performance", "Performa")) {
                HomeSettingsRow(
                    icon = Icons.Rounded.Speed,
                    title = lowRamProfileBadgeLabel(lowRamProfile),
                    subtitle = if (lowRamProfile.lowRamOverride == LowRamProfileOverride.Auto) {
                        tr("Chosen automatically for this phone", "Dipilih otomatis untuk HP ini")
                    } else {
                        tr("Set manually", "Diatur manual")
                    },
                    actionLabel = tr("Change", "Ubah"),
                    onAction = onOpenPerformanceProfile
                )
            }
            HomeSettingsGroup(title = tr("About", "Tentang")) {
                HomeSettingsRow(
                    icon = null,
                    title = "CBX Lock ${BuildConfig.VERSION_NAME}",
                    subtitle = tr("Developer: github.com/coblax", "Pengembang: github.com/coblax"),
                    actionLabel = tr("Open", "Buka"),
                    onAction = { openExternalUrl(context, DeveloperGithubUrl) }
                )
            }
        }
    }
}

@Composable
private fun HomeSettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = AppColors.current.textSecondary,
            style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

@Composable
private fun HomeSettingsRow(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val tokens = ScopedUiTokens.current
    val shape = RoundedCornerShape(tokens.radiusMedium)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.current.surfaceSoft)
            .border(1.dp, AppColors.current.outlineSubtle, shape)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.current.brandText,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppColors.current.textPrimary,
                style = AppTextStyles.bodyCompact.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                color = AppColors.current.textSecondary,
                style = AppTextStyles.diagnostic
            )
        }
        TextButton(
            onClick = onAction,
            modifier = Modifier.heightIn(min = tokens.touchTarget)
        ) {
            Text(
                text = actionLabel,
                color = AppColors.current.brandText,
                style = AppTextStyles.button
            )
        }
    }
}

private data class HomeSegmentOption(
    val label: String,
    val icon: ImageVector? = null,
    val description: String? = null
)

@Composable
private fun HomeSegmentedControl(
    options: List<HomeSegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val tokens = ScopedUiTokens.current
    val colors = primaryActionColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(tokens.radiusMedium))
            .background(AppColors.current.surfaceSoft)
            .border(1.dp, AppColors.current.outlineSubtle, RoundedCornerShape(tokens.radiusMedium))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val contentColor = if (selected) colors.content else AppColors.current.textSecondary
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(tokens.radiusSmall))
                    .background(if (selected) colors.container else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) }
                    )
                    .then(
                        if (option.description != null) {
                            Modifier.semantics {
                                contentDescription = option.description
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                option.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = option.label,
                    color = contentColor,
                    style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Brand
// ──────────────────────────────────────────────────────────────

@Composable
internal fun CoblaxLogoMark(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (LocalLowRamProfile.current.severe) {
        // Severe profiles skip the launcher bitmap entirely. The glyph is sized from the
        // tile, not the system font, so "CBX" always fits the tile.
        BoxWithConstraints(
            modifier = modifier
                .clip(shape)
                .background(AppColors.current.blueDeep),
            contentAlignment = Alignment.Center
        ) {
            val glyphSize = with(LocalDensity.current) { (maxWidth * 0.3f).toSp() }
            Text(
                text = "CBX",
                color = Color.White,
                fontSize = glyphSize,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
    } else {
        // The launcher foreground bitmap ships with an opaque white plate, which reads
        // as a glaring white square on the dark background. Clipping it to a rounded
        // tile turns that plate into a deliberate app-icon tile.
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = modifier
                .clip(shape)
                .then(
                    if (AppColors.current.isDark) {
                        Modifier.border(
                            width = 1.dp,
                            color = AppColors.current.outlineMedium,
                            shape = shape
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}
