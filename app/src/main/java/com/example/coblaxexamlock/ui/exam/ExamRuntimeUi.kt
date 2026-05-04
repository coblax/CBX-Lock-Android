package com.example.coblaxexamlock.ui.exam
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary
import java.util.Locale

internal enum class ExamFooterShieldStatus {
    Safe,
    Warning,
    Danger
}

internal enum class ExamServerFooterStatus {
    Checking,
    Online,
    Warning,
    Offline
}

internal data class ExamRuntimeChromeState(
    val examSessionStarted: Boolean,
    val examDisplayName: String,
    val loadingProgress: Float,
    val webViewErrorMessage: String?,
    val hasFullscreenCustomView: Boolean,
    val useBuiltInExamKeyboard: Boolean,
    val showBuiltInExamKeyboard: Boolean,
    val showSideArrowControls: Boolean,
    @Suppress("unused")
    val hasEditableFocus: Boolean,
    val builtInKeyboardShiftEnabled: Boolean,
    val networkStatus: NetworkReadinessStatus,
    val serverStatus: ExamServerFooterStatus,
    val batteryStatus: ExamBatteryStatus,
    val shieldStatus: ExamFooterShieldStatus
)

internal data class ExamRuntimeChromeActions(
    val onRetryLoading: () -> Unit,
    val onRefreshPage: () -> Unit,
    val onGoHome: () -> Unit,
    val onTextKey: (String) -> Unit,
    val onBackspace: () -> Unit,
    val onArrowLeft: () -> Unit,
    val onArrowRight: () -> Unit,
    val onToggleSideArrowControls: () -> Unit,
    val onEnter: () -> Unit,
    val onSpace: () -> Unit,
    val onShiftToggle: () -> Unit
)

@Composable
internal fun ExamRuntimeChrome(
    state: ExamRuntimeChromeState,
    actions: ExamRuntimeChromeActions,
    modifier: Modifier = Modifier,
    webViewLayer: @Composable BoxScope.() -> Unit,
    fullscreenLayer: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.examSessionStarted) {
                    webViewLayer()
                }

                if (state.examSessionStarted && state.loadingProgress < 1f) {
                    LinearProgressIndicator(
                        progress = { state.loadingProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = LockBlue,
                        trackColor = Color.Transparent
                    )
                }

                if (state.examSessionStarted && !state.webViewErrorMessage.isNullOrBlank()) {
                    ExamWebErrorOverlay(
                        examDisplayName = state.examDisplayName,
                        errorMessage = state.webViewErrorMessage,
                        onRetry = actions.onRetryLoading,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (state.examSessionStarted && state.hasFullscreenCustomView) {
                    fullscreenLayer?.invoke(this)
                }
            }

            if (state.examSessionStarted && state.useBuiltInExamKeyboard && state.showBuiltInExamKeyboard) {
                ExamBuiltInKeyboardPanel(
                    isShiftEnabled = state.builtInKeyboardShiftEnabled,
                    onTextKey = actions.onTextKey,
                    onBackspace = actions.onBackspace,
                    onEnter = actions.onEnter,
                    onSpace = actions.onSpace,
                    onShiftToggle = actions.onShiftToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (state.examSessionStarted) {
                ExamWebViewBottomBar(
                    networkStatus = state.networkStatus,
                    serverStatus = state.serverStatus,
                    batteryStatus = state.batteryStatus,
                    shieldStatus = state.shieldStatus,
                    showArrowControls = state.showSideArrowControls,
                    isRefreshing = state.loadingProgress in 0f..0.98f,
                    onToggleArrowControls = actions.onToggleSideArrowControls,
                    onRefresh = actions.onRefreshPage,
                    onGoHome = actions.onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Arrow controls are injected into the WebView DOM so fullscreen web content
        // and focus-trapping exam pages cannot steal the caret target from Compose.
    }
}

@Composable
private fun ExamSideArrowPopups(
    enabled: Boolean,
    onArrowLeft: () -> Unit,
    onArrowRight: () -> Unit
) {
    val properties = PopupProperties(
        focusable = false,
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        clippingEnabled = false
    )
    Popup(
        alignment = Alignment.CenterStart,
        offset = IntOffset(x = 6, y = 0),
        properties = properties
    ) {
        ExamSideArrowButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = tr("Move cursor left", "Geser kursor ke kiri"),
            enabled = enabled,
            onClick = onArrowLeft
        )
    }
    Popup(
        alignment = Alignment.CenterEnd,
        offset = IntOffset(x = -6, y = 0),
        properties = properties
    ) {
        ExamSideArrowButton(
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = tr("Move cursor right", "Geser kursor ke kanan"),
            enabled = enabled,
            onClick = onArrowRight
        )
    }
}

@Composable
private fun ExamSideArrowControls(
    enabled: Boolean,
    onArrowLeft: () -> Unit,
    onArrowRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ExamSideArrowButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = tr("Move cursor left", "Geser kursor ke kiri"),
            enabled = enabled,
            onClick = onArrowLeft,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
        )
        ExamSideArrowButton(
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = tr("Move cursor right", "Geser kursor ke kanan"),
            enabled = enabled,
            onClick = onArrowRight,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
        )
    }
}

@Composable
private fun ExamSideArrowButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled) {
        Color.White.copy(alpha = 0.94f)
    } else {
        LockSurfaceSoft.copy(alpha = 0.52f)
    }
    val contentColor = if (enabled) {
        LockBlueDeep
    } else {
        LockTextMuted.copy(alpha = 0.78f)
    }
    Surface(
        modifier = modifier
            .width(42.dp)
            .height(70.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        tonalElevation = if (enabled) 6.dp else 0.dp,
        shadowElevation = if (enabled) 8.dp else 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) LockBlue.copy(alpha = 0.34f) else LockOutline.copy(alpha = 0.42f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun ExamWebErrorOverlay(
    examDisplayName: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, LockOutline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = tr("Exam page is not available yet", "Halaman ujian belum berhasil dimuat"),
                color = LockTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 25.sp
            )
            Text(
                text = examDisplayName,
                color = LockBlueDeep,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = errorMessage.orEmpty(),
                color = LockTextSecondary,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LockBlue,
                    contentColor = LockOnDark
                )
            ) {
                Text(tr("Retry Loading", "Muat Ulang"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun ExamBuiltInKeyboardPanel(
    isShiftEnabled: Boolean,
    onTextKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    )

    Surface(
        modifier = modifier,
        color = Color(0xFFF2F5FA),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.70f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(LockOutline)
            )

            Text(
                text = tr("Internal Keyboard", "Keyboard Internal"),
                color = LockTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            keyboardRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { key ->
                        ExamBuiltInKeyboardKey(
                            label = if (isShiftEnabled) key.uppercase(Locale.US) else key,
                            onClick = { onTextKey(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ExamBuiltInKeyboardKey(
                    icon = Icons.Rounded.ArrowUpward,
                    label = "",
                    onClick = onShiftToggle,
                    modifier = Modifier.weight(1.05f),
                    isAccent = isShiftEnabled
                )
                listOf("z", "x", "c", "v", "b", "n", "m").forEach { key ->
                    ExamBuiltInKeyboardKey(
                        label = if (isShiftEnabled) key.uppercase(Locale.US) else key,
                        onClick = { onTextKey(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                ExamBuiltInKeyboardKey(
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    label = "",
                    onClick = onBackspace,
                    modifier = Modifier.weight(1.05f),
                    isSecondary = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("@", ".", "-", "/").forEach { key ->
                    ExamBuiltInKeyboardKey(
                        label = key,
                        onClick = { onTextKey(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                ExamBuiltInKeyboardKey(
                    label = "",
                    onClick = onSpace,
                    modifier = Modifier.weight(3.1f),
                    isSecondary = true
                )
                ExamBuiltInKeyboardKey(
                    icon = Icons.AutoMirrored.Rounded.KeyboardReturn,
                    label = "",
                    onClick = onEnter,
                    modifier = Modifier.weight(1.05f),
                    isAccent = true
                )
            }
        }
    }
}

@Composable
internal fun ExamBuiltInKeyboardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isAccent: Boolean = false,
    isSecondary: Boolean = false
) {
    val backgroundColor = when {
        isAccent -> LockBlue
        isSecondary -> Color(0xFFE6EBF3)
        else -> Color.White
    }
    val contentColor = if (isAccent) LockOnDark else LockTextPrimary

    Surface(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null && label.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun ExamWebViewBottomBar(
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus,
    batteryStatus: ExamBatteryStatus,
    shieldStatus: ExamFooterShieldStatus,
    showArrowControls: Boolean,
    isRefreshing: Boolean,
    onToggleArrowControls: () -> Unit,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when (networkStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable -> Color(0xFF56C271)
        NetworkReadinessVerdict.Offline,
        NetworkReadinessVerdict.AirplaneMode -> Color(0xFFF26A6A)
        NetworkReadinessVerdict.Unvalidated,
        NetworkReadinessVerdict.CaptivePortal,
        NetworkReadinessVerdict.Unstable -> LockGoldDark
    }
    val batteryIndicatorColor = when {
        batteryStatus.isCharging -> Color(0xFF56C271)
        batteryStatus.levelPercent <= 20 -> Color(0xFFF26A6A)
        batteryStatus.levelPercent <= 40 -> LockGoldDark
        else -> LockBlue
    }
    val serverIndicatorColor = when (serverStatus) {
        ExamServerFooterStatus.Online -> Color(0xFF56C271)
        ExamServerFooterStatus.Warning -> LockGoldDark
        ExamServerFooterStatus.Offline -> Color(0xFFF26A6A)
        ExamServerFooterStatus.Checking -> Color(0xFF8A96A3)
    }
    val serverContentDescription = when (serverStatus) {
        ExamServerFooterStatus.Online -> tr("Exam server reachable", "Server ujian bisa diakses")
        ExamServerFooterStatus.Warning -> tr("Exam server warning", "Peringatan server ujian")
        ExamServerFooterStatus.Offline -> tr("Exam server unreachable", "Server ujian tidak bisa diakses")
        ExamServerFooterStatus.Checking -> tr("Checking exam server", "Mengecek server ujian")
    }
    val shieldIndicatorColor = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> Color(0xFF56C271)
        ExamFooterShieldStatus.Warning -> LockGoldDark
        ExamFooterShieldStatus.Danger -> Color(0xFFF26A6A)
    }
    val shieldContentDescription = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> tr("Security protected", "Keamanan terlindungi")
        ExamFooterShieldStatus.Warning -> tr("Security warning", "Peringatan keamanan")
        ExamFooterShieldStatus.Danger -> tr("Security issue detected", "Masalah keamanan terdeteksi")
    }
    val transportLabel = networkStatus.transportLabel
    val networkContentDescription = when (networkStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable ->
            tr(
                "Network connected: ${transportLabel.ifBlank { "unknown transport" }}",
                "Jaringan terhubung: ${transportLabel.ifBlank { "transport tidak diketahui" }}"
            )
        NetworkReadinessVerdict.Offline ->
            tr("Network offline", "Jaringan offline")
        NetworkReadinessVerdict.AirplaneMode ->
            tr("Airplane mode is active", "Mode pesawat aktif")
        NetworkReadinessVerdict.Unvalidated ->
            tr("Network is limited", "Jaringan terbatas")
        NetworkReadinessVerdict.CaptivePortal ->
            tr("Network requires captive portal login", "Jaringan membutuhkan login captive portal")
        NetworkReadinessVerdict.Unstable ->
            tr("Network is unstable", "Jaringan tidak stabil")
    }
    val refreshContainerColor = if (isRefreshing) LockGold else LockBlue
    BoxWithConstraints(modifier = modifier) {
        val compactFooter = maxWidth <= 390.dp
        val footerHorizontalPadding = if (compactFooter) 5.dp else 8.dp
        val footerVerticalPadding = if (compactFooter) 5.dp else 6.dp
        val itemSpacing = if (compactFooter) 4.dp else 6.dp
        val actionSpacing = if (compactFooter) 4.dp else 6.dp
        val controlStatusGap = if (compactFooter) 8.dp else 10.dp
        val actionButtonSize = if (compactFooter) 32.dp else 36.dp
        val iconSize = if (compactFooter) 17.dp else 19.dp
        val batteryPillWidth = when {
            compactFooter -> 54.dp
            batteryStatus.levelPercent.coerceIn(0, 100) >= 100 -> 62.dp
            else -> 58.dp
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(if (compactFooter) 16.dp else 18.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.80f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = footerHorizontalPadding, vertical = footerVerticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArrowVisibilityTogglePill(
                    visible = showArrowControls,
                    buttonSize = actionButtonSize,
                    iconSize = if (compactFooter) 12.dp else 13.dp,
                    onClick = onToggleArrowControls
                )
                Spacer(modifier = Modifier.width(controlStatusGap))
                ExamFooterDivider(height = if (compactFooter) 20.dp else 22.dp)
                Spacer(modifier = Modifier.width(controlStatusGap))
                NetworkStatusIconPill(
                    icon = networkStatusIcon(networkStatus),
                    statusColor = indicatorColor,
                    contentDescription = networkContentDescription,
                    size = actionButtonSize,
                    iconSize = iconSize
                )
                Spacer(modifier = Modifier.width(itemSpacing))
                ServerStatusIconPill(
                    serverStatus = serverStatus,
                    statusColor = serverIndicatorColor,
                    contentDescription = serverContentDescription,
                    size = actionButtonSize,
                    iconSize = iconSize
                )
                Spacer(modifier = Modifier.width(itemSpacing))
                BatteryStatusIconPill(
                    batteryStatus = batteryStatus,
                    statusColor = batteryIndicatorColor,
                    height = actionButtonSize,
                    width = batteryPillWidth,
                    iconSize = iconSize
                )
                Spacer(modifier = Modifier.width(itemSpacing))
                SecurityShieldStatusIconPill(
                    statusColor = shieldIndicatorColor,
                    contentDescription = shieldContentDescription,
                    size = actionButtonSize,
                    iconSize = iconSize
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExamFooterIconButton(
                        onClick = onRefresh,
                        icon = Icons.Rounded.Refresh,
                        contentDescription = tr("Refresh exam page", "Refresh halaman ujian"),
                        containerColor = refreshContainerColor,
                        contentColor = LockOnDark,
                        size = actionButtonSize,
                        iconSize = iconSize
                    )

                    ExamFooterIconButton(
                        onClick = onGoHome,
                        icon = Icons.Rounded.Home,
                        contentDescription = tr("Back to the main menu", "Kembali ke menu utama"),
                        containerColor = LockSurfaceSoft,
                        contentColor = LockBlueDeep,
                        size = actionButtonSize,
                        iconSize = iconSize
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamFooterDivider(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(LockOutline.copy(alpha = 0.75f))
    )
}

@Composable
private fun ArrowVisibilityTogglePill(
    visible: Boolean,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val containerColor = if (visible) LockBlue else LockSurfaceSoft
    val contentColor = if (visible) LockOnDark else LockBlueDeep
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(buttonSize)
            .border(
                width = 1.dp,
                color = if (visible) LockBlueDeep.copy(alpha = 0.35f) else LockOutline.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = if (visible) {
                    tr("Hide side arrows", "Sembunyikan tombol panah")
                } else {
                    tr("Show side arrows", "Tampilkan tombol panah")
                },
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun ExamFooterIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.32f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun NetworkStatusIconPill(
    icon: ImageVector,
    statusColor: Color,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = statusColor,
                modifier = Modifier.size(iconSize)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }
    }
}

@Composable
private fun ServerStatusIconPill(
    serverStatus: ExamServerFooterStatus,
    statusColor: Color,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (serverStatus == ExamServerFooterStatus.Offline) {
                    Icons.Rounded.CloudOff
                } else {
                    Icons.Rounded.Cloud
                },
                contentDescription = contentDescription,
                tint = statusColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun SecurityShieldStatusIconPill(
    statusColor: Color,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = contentDescription,
                tint = statusColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun BatteryStatusIconPill(
    batteryStatus: ExamBatteryStatus,
    statusColor: Color,
    height: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val percent = batteryStatus.levelPercent.coerceIn(0, 100)
    val contentDescription = if (batteryStatus.isCharging) {
        tr("Battery $percent percent, charging", "Baterai $percent persen, sedang diisi")
    } else {
        tr("Battery $percent percent", "Baterai $percent persen")
    }

    Surface(
        modifier = Modifier
            .height(height)
            .width(width),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = batteryStatusIcon(batteryStatus),
                contentDescription = contentDescription,
                tint = statusColor,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = "$percent%",
                color = LockTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private fun networkStatusIcon(status: NetworkReadinessStatus): ImageVector {
    val transportSummary =
        (status.diagnostics.transports + status.transportLabel)
            .joinToString(" ")
            .lowercase(Locale.US)
    return when (status.verdict) {
        NetworkReadinessVerdict.AirplaneMode -> Icons.Rounded.AirplanemodeActive
        NetworkReadinessVerdict.Offline -> Icons.Rounded.WifiOff
        NetworkReadinessVerdict.Unvalidated,
        NetworkReadinessVerdict.CaptivePortal,
        NetworkReadinessVerdict.Unstable -> Icons.Rounded.WarningAmber
        NetworkReadinessVerdict.ConnectedStable ->
            if (transportSummary.contains("cellular")) {
                Icons.Rounded.SignalCellularAlt
            } else {
                Icons.Rounded.Wifi
            }
    }
}

private fun batteryStatusIcon(status: ExamBatteryStatus): ImageVector {
    return when {
        status.isCharging -> Icons.Rounded.BatteryChargingFull
        status.levelPercent <= 20 -> Icons.Rounded.BatteryAlert
        else -> Icons.Rounded.BatteryFull
    }
}

@Composable
internal fun ExamStatusPill(
    dotColor: Color,
    text: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 6.dp else 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 7.dp else 8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                color = LockTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

