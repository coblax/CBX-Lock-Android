package com.coblax.examlock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Canonical token set for upgraded screens.
 *
 * [UiTokens] remains the compatibility layer for older admin screens. New and
 * redesigned screens (Home, Preparation, Runtime) read these scoped tokens.
 */
@Immutable
internal data class UiDesignTokenSet(
    val radiusSmall: Dp = 12.dp,
    val radiusMedium: Dp = 16.dp,
    val radiusLarge: Dp = 20.dp,
    val radiusPill: Dp = 999.dp,
    val spaceXSmall: Dp = 4.dp,
    val spaceSmall: Dp = 8.dp,
    val spaceMedium: Dp = 12.dp,
    val spaceLarge: Dp = 16.dp,
    val spaceXLarge: Dp = 24.dp,
    val spaceXXLarge: Dp = 32.dp,
    val touchTarget: Dp = 48.dp,
    val compactControlHeight: Dp = 48.dp,
    val primaryActionHeight: Dp = 54.dp,
    val statusStripHeight: Dp = 40.dp,
    val contentMaxWidthCompact: Dp = 480.dp,
    val contentMaxWidthMedium: Dp = 600.dp,
    val contentMaxWidthExpanded: Dp = 720.dp
)

internal val DefaultUiDesignTokens = UiDesignTokenSet()

private val LocalUiDesignTokens = staticCompositionLocalOf { DefaultUiDesignTokens }

internal object ScopedUiTokens {
    val current: UiDesignTokenSet
        @Composable get() = LocalUiDesignTokens.current
}

internal enum class UiSurfaceRole {
    Background,
    Card,
    Elevated
}

@Composable
internal fun uiSurfaceColor(role: UiSurfaceRole): Color {
    val colors = AppColors.current
    return when (role) {
        UiSurfaceRole.Background -> colors.background
        UiSurfaceRole.Card -> colors.cardBg
        UiSurfaceRole.Elevated -> colors.surfaceSoft
    }
}

/**
 * Applies only the new visual system. Call this at the root of Home, Preparation,
 * Runtime, and Admin screens.
 */
@Composable
internal fun UpgradeUiScope(
    tokens: UiDesignTokenSet = DefaultUiDesignTokens,
    textStyles: AppTextStyleSet = DefaultAppTextStyles,
    motionPolicy: UiMotionPolicy? = null,
    content: @Composable () -> Unit
) {
    ProvideUiMotionPolicy(policy = motionPolicy) {
        CompositionLocalProvider(
            LocalUiDesignTokens provides tokens,
            LocalAppTextStyles provides textStyles,
            content = content
        )
    }
}
