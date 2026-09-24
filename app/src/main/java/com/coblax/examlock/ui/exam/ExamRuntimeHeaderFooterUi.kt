package com.coblax.examlock.ui.exam

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.ExamBatteryStatus
import com.coblax.examlock.model.NetworkReadinessStatus
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UiStatusTone
import com.coblax.examlock.ui.theme.resolveUiStatusColors

/** Actions never stretch wider than this on tablets; the bar stays phone-shaped. */
private val ExamFooterContentMaxWidth = 560.dp

/**
 * Bar text follows the system font up to [ExamChromeMaxFontScale]. Past that, extra
 * size goes to the exam page instead of taller bars; TalkBack still reads full labels.
 */
@Composable
private fun CappedChromeFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    if (density.fontScale <= ExamChromeMaxFontScale) {
        content()
    } else {
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, ExamChromeMaxFontScale),
            content = content
        )
    }
}

/**
 * Slim information header (about 32dp): exam name plus connection, battery, and
 * security. Nothing here is tappable, so it does not need 48dp touch targets and can
 * stay as thin as a line of text.
 */
@Composable
internal fun ExamRuntimeHeader(
    examDisplayName: String,
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus,
    batteryStatus: ExamBatteryStatus,
    shieldStatus: ExamFooterShieldStatus,
    modifier: Modifier = Modifier
) = CappedChromeFontScale {
    val lowRamProfile = LocalLowRamProfile.current
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ExamRuntimeUiTestTags.Header)
            .background(AppColors.current.cardBg)
    ) {
        val spec = calculateExamChromeLayoutSpec(
            maxWidthDp = maxWidth.value.toInt(),
            fontScale = fontScale,
            lowRamSevere = lowRamProfile.severe
        )
        LaunchedEffect(spec.layoutMode, maxWidth.value.toInt(), lowRamProfile.severe) {
            Log.i(
                "ExamRuntimeHardening",
                "${ExamRuntimeHardeningDiagnostics.FooterLayoutMode} " +
                    "mode=${spec.layoutMode.name} " +
                    "width_dp=${maxWidth.value.toInt()} " +
                    "low_ram=${lowRamProfile.enabled} " +
                    "severe=${lowRamProfile.severe} " +
                    "placement=compact_header_footer"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spec.headerHeightDp.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (spec.showExamName) {
                Text(
                    text = examDisplayName.ifBlank { tr("Exam", "Ujian") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    color = AppColors.current.textPrimary,
                    style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            ExamStatusItems(
                networkStatus = networkStatus,
                serverStatus = serverStatus,
                batteryStatus = batteryStatus,
                shieldStatus = shieldStatus,
                showLabels = spec.showStatusLabels
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.current.outlineSubtle)
        )
    }
}

/**
 * Connection, battery, and security as small icon + word pairs. Colors carry the state;
 * the merged semantics carry the full wording for TalkBack.
 */
@Composable
private fun ExamStatusItems(
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus,
    batteryStatus: ExamBatteryStatus,
    shieldStatus: ExamFooterShieldStatus,
    showLabels: Boolean
) {
    val connectivity = resolveExamFooterConnectivityVisual(networkStatus, serverStatus)
    val connectionTone = when (connectivity.severity) {
        ExamFooterConnectivitySeverity.Stable -> UiStatusTone.Success
        ExamFooterConnectivitySeverity.Info -> UiStatusTone.Info
        ExamFooterConnectivitySeverity.Warning -> UiStatusTone.Warning
    }
    val connectionIcon = when (connectivity.state) {
        ExamRuntimeConnectivityState.Offline -> Icons.Rounded.CloudOff
        ExamRuntimeConnectivityState.Checking -> Icons.Rounded.NetworkCheck
        ExamRuntimeConnectivityState.Online,
        ExamRuntimeConnectivityState.Limited -> when (connectivity.transport) {
            ExamFooterConnectivityTransport.Cellular -> Icons.Rounded.SignalCellularAlt
            else -> Icons.Rounded.Wifi
        }
    }
    val connectionLabel = when (connectivity.state) {
        ExamRuntimeConnectivityState.Online -> tr("Online", "Online")
        ExamRuntimeConnectivityState.Checking -> tr("Checking", "Memeriksa")
        ExamRuntimeConnectivityState.Limited -> tr("Limited", "Terbatas")
        ExamRuntimeConnectivityState.Offline -> tr("Offline", "Offline")
    }
    val percent = batteryStatus.levelPercent.coerceIn(0, 100)
    val batteryTone = when {
        batteryStatus.isCharging -> UiStatusTone.Success
        percent <= 20 -> UiStatusTone.Danger
        percent <= 40 -> UiStatusTone.Warning
        else -> UiStatusTone.Neutral
    }
    val batteryIcon = when {
        batteryStatus.isCharging -> Icons.Rounded.BatteryChargingFull
        percent <= 20 -> Icons.Rounded.BatteryAlert
        else -> Icons.Rounded.BatteryFull
    }
    val shieldTone = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> UiStatusTone.Success
        ExamFooterShieldStatus.Warning -> UiStatusTone.Warning
        ExamFooterShieldStatus.Danger -> UiStatusTone.Danger
    }
    val shieldIcon = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> Icons.Rounded.GppGood
        ExamFooterShieldStatus.Warning -> Icons.Rounded.GppMaybe
        ExamFooterShieldStatus.Danger -> Icons.Rounded.GppBad
    }
    val shieldLabel = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> tr("Protected", "Aman")
        ExamFooterShieldStatus.Warning -> tr("Check", "Periksa")
        ExamFooterShieldStatus.Danger -> tr("Blocked", "Diblokir")
    }
    val batteryText = if (batteryStatus.isCharging) {
        tr("Battery $percent percent, charging", "Baterai $percent persen, sedang diisi")
    } else {
        tr("Battery $percent percent", "Baterai $percent persen")
    }
    val shieldText = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> tr("Exam security is protected", "Keamanan ujian terlindungi")
        ExamFooterShieldStatus.Warning -> tr("Exam security needs attention", "Keamanan ujian perlu diperiksa")
        ExamFooterShieldStatus.Danger -> tr("A blocking security issue was detected", "Masalah keamanan yang memblokir terdeteksi")
    }
    val semanticText = "${connectionSemanticLabel(networkStatus, serverStatus)}. $batteryText. $shieldText."

    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = semanticText },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusItem(icon = connectionIcon, tone = connectionTone, label = connectionLabel.takeIf { showLabels })
        // The percentage is the point of the battery item, so it stays even when words go.
        StatusItem(icon = batteryIcon, tone = batteryTone, label = "$percent%")
        StatusItem(icon = shieldIcon, tone = shieldTone, label = shieldLabel.takeIf { showLabels })
    }
}

@Composable
private fun statusColor(tone: UiStatusTone): Color {
    val colors = AppColors.current
    return when (tone) {
        UiStatusTone.Neutral -> colors.textSecondary
        else -> resolveUiStatusColors(colors, tone).contentColor
    }
}

@Composable
private fun StatusItem(icon: ImageVector, tone: UiStatusTone, label: String?) {
    val color = statusColor(tone)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        if (label != null) {
            Text(
                text = label,
                color = color,
                style = AppTextStyles.label.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}

/**
 * Slim action footer: 48dp buttons (the touch-target minimum), flat, no card or
 * padding around them. With a narrow phone or an enlarged system font the words move
 * under the icons and shrink to fit, so the buttons never become unlabeled icons on
 * ordinary phones.
 */
@Composable
internal fun ExamRuntimeFooter(
    showArrowControls: Boolean,
    isRefreshing: Boolean,
    onToggleArrowControls: () -> Unit,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) = CappedChromeFontScale {
    val lowRamProfile = LocalLowRamProfile.current
    val fontScale = LocalDensity.current.fontScale
    val colors = AppColors.current
    // Background first, inset inside: the footer surface also fills a visible nav bar.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ExamRuntimeUiTestTags.Footer)
            .background(colors.cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outlineSubtle)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val spec = calculateExamChromeLayoutSpec(
                maxWidthDp = maxWidth.value.toInt(),
                fontScale = fontScale,
                lowRamSevere = lowRamProfile.severe
            )
            val labelMode = when {
                !spec.showActionLabels -> FooterLabelMode.Hidden
                spec.stackActionLabels -> FooterLabelMode.Stacked
                else -> FooterLabelMode.Inline
            }
            Row(
                modifier = Modifier
                    .widthIn(max = ExamFooterContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExamFooterAction(
                    icon = Icons.Rounded.Code,
                    label = tr("Arrows", "Panah"),
                    labelMode = labelMode,
                    minHeight = spec.footerHeightDp.dp,
                    description = if (showArrowControls) {
                        tr("Hide side arrow controls", "Sembunyikan kontrol panah samping")
                    } else {
                        tr("Show side arrow controls", "Tampilkan kontrol panah samping")
                    },
                    state = if (showArrowControls) {
                        tr("Arrow controls shown", "Kontrol panah ditampilkan")
                    } else {
                        tr("Arrow controls hidden", "Kontrol panah disembunyikan")
                    },
                    selected = showArrowControls,
                    contentColor = colors.brandText,
                    testTag = ExamRuntimeUiTestTags.ArrowToggle,
                    onClick = onToggleArrowControls,
                    modifier = Modifier.weight(1f)
                )
                ExamFooterAction(
                    icon = if (isRefreshing) Icons.Rounded.Close else Icons.Rounded.Refresh,
                    label = if (isRefreshing) tr("Stop", "Stop") else tr("Reload", "Muat ulang"),
                    labelMode = labelMode,
                    minHeight = spec.footerHeightDp.dp,
                    description = if (isRefreshing) {
                        tr("Stop loading the exam page", "Hentikan pemuatan halaman ujian")
                    } else {
                        tr("Reload the exam page", "Muat ulang halaman ujian")
                    },
                    state = null,
                    selected = false,
                    contentColor = colors.brandText,
                    testTag = ExamRuntimeUiTestTags.RefreshAction,
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                )
                ExamFooterAction(
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    label = tr("Exit", "Keluar"),
                    labelMode = labelMode,
                    minHeight = spec.footerHeightDp.dp,
                    description = tr(
                        "Open the exit exam confirmation",
                        "Buka konfirmasi keluar dari ujian"
                    ),
                    state = null,
                    selected = false,
                    contentColor = if (colors.isDark) colors.statusDanger else colors.dialogDangerIcon,
                    testTag = ExamRuntimeUiTestTags.ExitAction,
                    onClick = onGoHome,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class FooterLabelMode {
    Inline,
    Stacked,
    Hidden
}

@Composable
private fun ExamFooterAction(
    icon: ImageVector,
    label: String,
    labelMode: FooterLabelMode,
    minHeight: Dp,
    description: String,
    state: String?,
    selected: Boolean,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppColors.current
    val tokens = ScopedUiTokens.current
    val selectedBackground = if (selected) colors.blueTint else Color.Transparent
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                if (state != null) {
                    stateDescription = state
                }
            },
        shape = RoundedCornerShape(tokens.radiusSmall),
        color = Color.Transparent,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (labelMode == FooterLabelMode.Stacked) {
                // Bottom-navigation shape: the selection pill sits behind the icon only.
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(tokens.radiusPill))
                            .background(selectedBackground)
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        FooterIcon(icon = icon, tint = contentColor)
                    }
                    FooterLabel(text = label, color = contentColor)
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(tokens.radiusPill))
                        .background(selectedBackground)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FooterIcon(icon = icon, tint = contentColor)
                    if (labelMode == FooterLabelMode.Inline) {
                        FooterLabel(text = label, color = contentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
}

/** Shrinks a step or two before it would ever cut a word off. */
@Composable
private fun FooterLabel(text: String, color: Color) {
    val style = AppTextStyles.label
    BasicText(
        text = text,
        style = style.copy(
            color = color,
            fontWeight = FontWeight.SemiBold,
            lineHeight = TextUnit.Unspecified
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 9.sp,
            maxFontSize = style.fontSize,
            stepSize = 0.5.sp
        )
    )
}

/**
 * Non-blocking connection problems as one slim line under the header. Blocking states
 * still use the popups; this only keeps the student informed without eating the page.
 */
@Composable
internal fun RuntimeConnectionWarningBanner(
    noticeKind: ExamRuntimeConnectionNoticeKind,
    modifier: Modifier = Modifier
) {
    val colors = AppColors.current
    val toneColors = resolveUiStatusColors(colors, UiStatusTone.Warning)
    val message = when (noticeKind) {
        ExamRuntimeConnectionNoticeKind.Offline -> tr(
            "Offline. The exam stays open while reconnecting.",
            "Koneksi terputus. Ujian tetap terbuka sambil menyambung ulang."
        )
        ExamRuntimeConnectionNoticeKind.AirplaneMode -> tr(
            "Airplane mode is on. Turn it off to reconnect.",
            "Mode pesawat aktif. Matikan agar tersambung lagi."
        )
        ExamRuntimeConnectionNoticeKind.VpnActive -> tr(
            "VPN detected. Check the required exam network.",
            "VPN terdeteksi. Periksa jaringan yang diwajibkan ujian."
        )
        ExamRuntimeConnectionNoticeKind.CaptivePortal -> tr(
            "This network needs a sign-in before it reaches the exam server.",
            "Jaringan ini butuh login sebelum bisa ke server ujian."
        )
        ExamRuntimeConnectionNoticeKind.LimitedNetwork -> tr(
            "Internet access is limited. Still checking.",
            "Akses internet terbatas. Terus dicek otomatis."
        )
        ExamRuntimeConnectionNoticeKind.UnstableNetwork -> tr(
            "Connection is unstable. Stay in the exam while it recovers.",
            "Koneksi tidak stabil. Tetap di ujian selama dipulihkan."
        )
        ExamRuntimeConnectionNoticeKind.ServerOffline -> tr(
            "Exam server not reachable yet. Retrying automatically.",
            "Server ujian belum terjangkau. Dicoba ulang otomatis."
        )
        ExamRuntimeConnectionNoticeKind.ServerUnstable -> tr(
            "Exam server is unstable. This page stays open.",
            "Server ujian tidak stabil. Halaman ini tetap terbuka."
        )
        ExamRuntimeConnectionNoticeKind.ServerWarning -> tr(
            "Exam server needs attention. Continue on this page.",
            "Server ujian perlu diperiksa. Lanjutkan di halaman ini."
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ExamRuntimeUiTestTags.WarningBanner)
            .semantics(mergeDescendants = true) {}
            .background(toneColors.containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = tr("Connection warning", "Peringatan koneksi"),
            tint = toneColors.contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            color = colors.textPrimary,
            style = AppTextStyles.diagnostic,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
