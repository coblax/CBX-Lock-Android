package com.example.coblaxexamlock.ui.exam
import android.util.Log
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.coblaxexamlock.LocalLowRamProfile
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
    val batteryIndicatorColor = when {
        batteryStatus.isCharging -> Color(0xFF56C271)
        batteryStatus.levelPercent <= 20 -> Color(0xFFF26A6A)
        batteryStatus.levelPercent <= 40 -> LockGoldDark
        else -> LockBlue
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
    val shieldLabel = when (shieldStatus) {
        ExamFooterShieldStatus.Safe -> "Aman"
        ExamFooterShieldStatus.Warning -> "Cek"
        ExamFooterShieldStatus.Danger -> "Blok"
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
    val lowRamProfile = LocalLowRamProfile.current
    val connectivityIndicatorColor = when {
        networkStatus.verdict == NetworkReadinessVerdict.Offline ||
            networkStatus.verdict == NetworkReadinessVerdict.AirplaneMode -> Color(0xFFF26A6A)
        networkStatus.verdict == NetworkReadinessVerdict.Unvalidated ||
            networkStatus.verdict == NetworkReadinessVerdict.CaptivePortal ||
            networkStatus.verdict == NetworkReadinessVerdict.Unstable -> LockGoldDark
        else -> Color(0xFF56C271)
    }
    val connectivityVisual = resolveExamFooterConnectivityVisual(networkStatus, serverStatus)
    val connectivityDescription = "$networkContentDescription. $serverContentDescription"
    val refreshContainerColor = if (isRefreshing) LockGold else LockBlue
    BoxWithConstraints(modifier = modifier) {
        val layoutSpec = calculateExamFooterLayoutSpec(
            maxWidthDp = maxWidth.value.toInt(),
            lowRamEnabled = lowRamProfile.enabled,
            lowRamSevere = lowRamProfile.severe
        )
        val footerHorizontalPadding = layoutSpec.horizontalPaddingDp.dp
        val footerVerticalPadding = layoutSpec.verticalPaddingDp.dp
        val itemSpacing = layoutSpec.itemSpacingDp.dp
        val actionSpacing = layoutSpec.actionSpacingDp.dp
        val actionButtonSize = layoutSpec.buttonSizeDp.dp
        val arrowPillWidth = layoutSpec.arrowPillWidthDp.dp
        val connectivityPillWidth = layoutSpec.connectivityPillWidthDp.dp
        val shieldPillWidth = layoutSpec.shieldPillWidthDp.dp
        val actionTouchTargetSize = layoutSpec.touchTargetDp.dp
        val iconSize = layoutSpec.iconSizeDp.dp
        val rowSpacing = layoutSpec.rowSpacingDp.dp
        val batteryPillWidth = when {
            !layoutSpec.showBatteryPercent -> actionButtonSize
            layoutSpec.compact -> 52.dp
            batteryStatus.levelPercent.coerceIn(0, 100) >= 100 -> 62.dp
            else -> 58.dp
        }
        LaunchedEffect(
            layoutSpec.layoutMode,
            maxWidth.value.toInt(),
            lowRamProfile.enabled,
            lowRamProfile.severe
        ) {
            Log.i(
                "ExamRuntimeHardening",
                "${ExamRuntimeHardeningDiagnostics.FooterLayoutMode} " +
                    "mode=${layoutSpec.layoutMode.name} " +
                    "width_dp=${maxWidth.value.toInt()} " +
                    "low_ram=${lowRamProfile.enabled} " +
                    "severe=${lowRamProfile.severe}"
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(layoutSpec.cornerRadiusDp.dp),
            tonalElevation = layoutSpec.tonalElevationDp.dp,
            shadowElevation = layoutSpec.shadowElevationDp.dp,
            border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.80f))
        ) {
            if (layoutSpec.layoutMode == ExamFooterLayoutMode.TwoRowCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = layoutSpec.minHeightDp.dp,
                            max = layoutSpec.maxHeightDp.dp
                        )
                        .padding(
                            horizontal = footerHorizontalPadding,
                            vertical = footerVerticalPadding
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(rowSpacing, Alignment.CenterVertically)
                ) {
                    ArrowVisibilityTogglePill(
                        visible = showArrowControls,
                        buttonWidth = arrowPillWidth,
                        buttonSize = actionButtonSize,
                        touchTargetSize = actionTouchTargetSize,
                        iconSize = layoutSpec.arrowIconSizeDp.dp,
                        onClick = onToggleArrowControls
                    )
                    ExamFooterStatusCluster(
                        connectivityVisual = connectivityVisual,
                        connectivityIndicatorColor = connectivityIndicatorColor,
                        connectivityDescription = connectivityDescription,
                        connectivityPillWidth = connectivityPillWidth,
                        batteryStatus = batteryStatus,
                        batteryIndicatorColor = batteryIndicatorColor,
                        batteryPillWidth = batteryPillWidth,
                        shieldIndicatorColor = shieldIndicatorColor,
                        shieldLabel = shieldLabel,
                        shieldContentDescription = shieldContentDescription,
                        shieldPillWidth = shieldPillWidth,
                        actionButtonSize = actionButtonSize,
                        iconSize = iconSize,
                        itemSpacing = itemSpacing,
                        showBatteryPercent = layoutSpec.showBatteryPercent
                    )
                    ExamFooterActionCluster(
                        refreshContainerColor = refreshContainerColor,
                        actionButtonSize = actionButtonSize,
                        actionTouchTargetSize = actionTouchTargetSize,
                        iconSize = iconSize,
                        actionSpacing = actionSpacing,
                        onRefresh = onRefresh,
                        onGoHome = onGoHome
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = layoutSpec.minHeightDp.dp,
                            max = layoutSpec.maxHeightDp.dp
                        )
                        .padding(horizontal = footerHorizontalPadding, vertical = footerVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArrowVisibilityTogglePill(
                        visible = showArrowControls,
                        buttonWidth = arrowPillWidth,
                        buttonSize = actionButtonSize,
                        touchTargetSize = actionTouchTargetSize,
                        iconSize = layoutSpec.arrowIconSizeDp.dp,
                        onClick = onToggleArrowControls
                    )
                    Spacer(modifier = Modifier.width(itemSpacing))
                    ExamFooterStatusCluster(
                        connectivityVisual = connectivityVisual,
                        connectivityIndicatorColor = connectivityIndicatorColor,
                        connectivityDescription = connectivityDescription,
                        connectivityPillWidth = connectivityPillWidth,
                        batteryStatus = batteryStatus,
                        batteryIndicatorColor = batteryIndicatorColor,
                        batteryPillWidth = batteryPillWidth,
                        shieldIndicatorColor = shieldIndicatorColor,
                        shieldLabel = shieldLabel,
                        shieldContentDescription = shieldContentDescription,
                        shieldPillWidth = shieldPillWidth,
                        actionButtonSize = actionButtonSize,
                        iconSize = iconSize,
                        itemSpacing = itemSpacing,
                        showBatteryPercent = layoutSpec.showBatteryPercent
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    ExamFooterActionCluster(
                        refreshContainerColor = refreshContainerColor,
                        actionButtonSize = actionButtonSize,
                        actionTouchTargetSize = actionTouchTargetSize,
                        iconSize = iconSize,
                        actionSpacing = actionSpacing,
                        onRefresh = onRefresh,
                        onGoHome = onGoHome
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamFooterStatusCluster(
    connectivityVisual: ExamFooterConnectivityVisual,
    connectivityIndicatorColor: Color,
    connectivityDescription: String,
    connectivityPillWidth: Dp,
    batteryStatus: ExamBatteryStatus,
    batteryIndicatorColor: Color,
    batteryPillWidth: Dp,
    shieldIndicatorColor: Color,
    shieldLabel: String,
    shieldContentDescription: String,
    shieldPillWidth: Dp,
    actionButtonSize: Dp,
    iconSize: Dp,
    itemSpacing: Dp,
    showBatteryPercent: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectivitySignalPill(
            visual = connectivityVisual,
            statusColor = connectivityIndicatorColor,
            contentDescription = connectivityDescription,
            width = connectivityPillWidth,
            height = actionButtonSize
        )
        BatteryStatusIconPill(
            batteryStatus = batteryStatus,
            statusColor = batteryIndicatorColor,
            height = actionButtonSize,
            width = batteryPillWidth,
            iconSize = iconSize,
            showPercent = showBatteryPercent
        )
        SecurityShieldStatusIconPill(
            statusColor = shieldIndicatorColor,
            label = shieldLabel,
            contentDescription = shieldContentDescription,
            width = shieldPillWidth,
            height = actionButtonSize,
            iconSize = iconSize
        )
    }
}

@Composable
private fun ExamFooterActionCluster(
    refreshContainerColor: Color,
    actionButtonSize: Dp,
    actionTouchTargetSize: Dp,
    iconSize: Dp,
    actionSpacing: Dp,
    onRefresh: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(actionSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExamFooterIconButton(
            onClick = onRefresh,
            icon = Icons.Rounded.Refresh,
            contentDescription = tr("Refresh exam page", "Refresh halaman ujian"),
            containerColor = refreshContainerColor,
            contentColor = LockOnDark,
            size = actionButtonSize,
            touchTargetSize = actionTouchTargetSize,
            iconSize = iconSize
        )

        ExamFooterIconButton(
            onClick = onGoHome,
            icon = Icons.Rounded.Home,
            contentDescription = tr("Back to the main menu", "Kembali ke menu utama"),
            containerColor = LockSurfaceSoft,
            contentColor = LockBlueDeep,
            size = actionButtonSize,
            touchTargetSize = actionTouchTargetSize,
            iconSize = iconSize
        )
    }
}

@Composable
private fun ArrowVisibilityTogglePill(
    visible: Boolean,
    buttonWidth: Dp,
    buttonSize: Dp,
    touchTargetSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    val containerColor = if (visible) LockBlue else LockSurfaceSoft
    val contentColor = if (visible) LockOnDark else LockBlueDeep
    Box(
        modifier = Modifier
            .width(buttonWidth)
            .height(touchTargetSize)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonSize)
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
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = if (visible) {
                        tr("Hide side arrows", "Sembunyikan tombol panah")
                    } else {
                        tr("Show side arrows", "Tampilkan tombol panah")
                    },
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
                Text(
                    text = "Panah",
                    color = contentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
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
    touchTargetSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(touchTargetSize)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
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
}

@Composable
private fun ConnectivitySignalPill(
    visual: ExamFooterConnectivityVisual,
    statusColor: Color,
    contentDescription: String,
    width: Dp,
    height: Dp
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
            ) {
                SignalBars(
                    level = visual.signalLevel,
                    statusColor = statusColor,
                    danger = visual.severity == ExamFooterConnectivitySeverity.Danger
                )
                visual.cellularLabel?.let { label ->
                    Text(
                        text = label,
                        color = statusColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }
            visual.badgeText?.let { badge ->
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SignalBars(
    level: Int,
    statusColor: Color,
    danger: Boolean
) {
    val activeBars = if (danger) 1 else level.coerceIn(0, 4)
    val inactiveColor = LockOutline.copy(alpha = 0.42f)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(7.dp, 10.dp, 13.dp, 16.dp).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index < activeBars) statusColor else inactiveColor)
            )
        }
    }
}

@Composable
private fun SecurityShieldStatusIconPill(
    statusColor: Color,
    label: String,
    contentDescription: String,
    width: Dp,
    height: Dp,
    iconSize: Dp
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(12.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = contentDescription,
                tint = statusColor,
                modifier = Modifier.size((iconSize.value - 3).coerceAtLeast(12f).dp)
            )
            Text(
                text = label,
                color = statusColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip
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
    iconSize: androidx.compose.ui.unit.Dp,
    showPercent: Boolean
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
            if (showPercent) {
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

