package com.example.coblaxexamlock.ui.exam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextMuted

@Composable
internal fun ExamRuntimeChrome(
    state: ExamRuntimeChromeState,
    actions: ExamRuntimeChromeActions,
    modifier: Modifier = Modifier,
    webViewLayer: @Composable BoxScope.() -> Unit,
    fullscreenLayer: (@Composable BoxScope.() -> Unit)? = null
) {
    val isRefreshing = state.loadingProgress in 0f..0.98f
    // Progress bar color: gold while refreshing, blue while loading fresh content
    val progressBarColor = if (isRefreshing) LockGold else LockBlue

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
                    // Polished 3dp progress bar with rounded cap and color sync
                    LinearProgressIndicator(
                        progress = { state.loadingProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter),
                        color = progressBarColor,
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Round
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
                    isRefreshing = isRefreshing,
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
    // Use Surface(onClick=...) for proper ripple feedback instead of Surface + clickable
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .width(42.dp)
            .height(70.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = if (enabled) 2.dp else 0.dp,
        shadowElevation = if (enabled) 3.dp else 0.dp,
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
