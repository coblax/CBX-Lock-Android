package com.coblax.examlock.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal that provides the current [WindowSizeClass] throughout the
 * composition tree. Set at the Activity level via
 * `calculateWindowSizeClass(activity)`.
 *
 * Defaults to `null` when not provided (pre-existing code paths that don't
 * inject it yet will still work).
 */
internal val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass?> { null }

/**
 * Central design tokens for the Exam Lock UI.
 *
 * All screens should prefer these tokens over ad-hoc dp values to keep the
 * visual language consistent and to allow a single place to tune spacing or
 * radii for low-RAM devices.
 */
@Immutable
internal object UiTokens {
    // Corner radii — standardized scale
    val RadiusXs: Dp = 8.dp
    val RadiusSm: Dp = 12.dp
    val RadiusMd: Dp = 16.dp
    val RadiusLg: Dp = 20.dp
    val RadiusXl: Dp = 24.dp
    val RadiusCard: Dp = 22.dp
    val RadiusPill: Dp = 999.dp

    // Spacing scale
    val SpaceXxs: Dp = 2.dp
    val SpaceXs: Dp = 4.dp
    val SpaceSm: Dp = 8.dp
    val SpaceMd: Dp = 12.dp
    val SpaceLg: Dp = 16.dp
    val SpaceXl: Dp = 20.dp
    val SpaceXxl: Dp = 28.dp

    // Border widths
    val BorderThin: Dp = 1.dp

    // Border alpha presets — used to give visual separation without shadows
    const val BorderAlphaSubtle: Float = 0.40f
    const val BorderAlphaDefault: Float = 0.60f
    const val BorderAlphaStrong: Float = 0.85f

    // Minimum tap target — Material 3 guideline: 48dp
    val TapTargetMin: Dp = 48.dp
    val TapTargetCompact: Dp = 40.dp

    // Elevation substitute — flat card border intensity
    const val ElevationSubtleBorderAlpha: Float = 0.45f

    // Content padding — standardized across screens
    val ContentPaddingHorizontal: Dp = 16.dp
    val ContentPaddingVertical: Dp = 12.dp
    val ScreenPaddingHorizontal: Dp = 20.dp
    val ScreenPaddingCompact: Dp = 16.dp

    // Dialog padding
    val DialogContentPadding: Dp = 20.dp

    // Icon sizes — standardized scale
    val IconSm: Dp = 16.dp
    val IconMd: Dp = 20.dp
    val IconLg: Dp = 24.dp

    // ── Responsive breakpoints ────────────────────────────────────
    /** Maximum content width for readable text layouts (optimal: 45-75 chars). */
    val ContentMaxWidthCompact: Dp = 480.dp
    val ContentMaxWidthMedium: Dp = 600.dp
    val ContentMaxWidthExpanded: Dp = 720.dp
}

/**
 * Constrains content width so cards and text blocks don't stretch too wide
 * on tablets and landscape devices. Automatically selects the right max-width
 * based on the current [WindowSizeClass].
 *
 * On compact phones this is effectively a no-op since the screen is already
 * narrower than the constraint.
 */
@Composable
internal fun Modifier.responsiveContentWidth(
    compactMaxWidth: Dp = UiTokens.ContentMaxWidthCompact,
    mediumMaxWidth: Dp = UiTokens.ContentMaxWidthMedium,
    expandedMaxWidth: Dp = UiTokens.ContentMaxWidthExpanded
): Modifier {
    val sizeClass = LocalWindowSizeClass.current
    val maxWidth = when (sizeClass?.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> expandedMaxWidth
        WindowWidthSizeClass.Medium -> mediumMaxWidth
        else -> compactMaxWidth
    }
    return this.widthIn(max = maxWidth)
}

/**
 * Returns adaptive horizontal screen padding based on the current window width.
 * Larger screens get proportionally more padding to keep content visually centered.
 */
@Composable
internal fun adaptiveScreenPadding(): Dp {
    val sizeClass = LocalWindowSizeClass.current
    return when (sizeClass?.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 32.dp
        WindowWidthSizeClass.Medium -> 24.dp
        else -> UiTokens.ScreenPaddingHorizontal
    }
}

/**
 * Returns true when the current window is at least [WindowWidthSizeClass.Expanded],
 * useful for conditionally showing 2-column layouts or side panels.
 */
@Composable
internal fun isExpandedLayout(): Boolean {
    val sizeClass = LocalWindowSizeClass.current
    return sizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
}

/**
 * Returns true when the current window is at least [WindowWidthSizeClass.Medium],
 * useful for showing slightly larger content or wider cards.
 */
@Composable
internal fun isMediumOrLarger(): Boolean {
    val sizeClass = LocalWindowSizeClass.current
    return sizeClass?.widthSizeClass != WindowWidthSizeClass.Compact
}

/**
 * Lightweight alternative to Material3 [androidx.compose.material3.Surface] that
 * uses only `background` + `border` — no `RenderNode` for elevation, no shadow
 * rendering pass. Use this as the default card container across the app.
 */
internal fun Modifier.flatCard(
    containerColor: Color = LockCardBg,
    borderColor: Color = LockOutline,
    borderAlpha: Float = UiTokens.BorderAlphaDefault,
    radius: Dp = UiTokens.RadiusCard,
    shape: RoundedCornerShape = RoundedCornerShape(radius)
): Modifier {
    val resolvedBorder = borderColor.copy(alpha = borderAlpha)
    return this
        .clip(shape)
        .background(containerColor)
        .border(UiTokens.BorderThin, resolvedBorder, shape)
}

/**
 * A flat card with a slightly stronger border for emphasized containers like
 * hero sections and primary action cards.
 */
internal fun Modifier.flatCardElevated(
    containerColor: Color = LockCardBg,
    borderColor: Color = LockOutline,
    radius: Dp = UiTokens.RadiusCard,
    shape: RoundedCornerShape = RoundedCornerShape(radius)
): Modifier {
    return this
        .clip(shape)
        .background(containerColor)
        .border(UiTokens.BorderThin, borderColor.copy(alpha = UiTokens.BorderAlphaStrong), shape)
}

/**
 * Flat pill container for badges, chips, and compact pill buttons.
 */
internal fun Modifier.flatPill(
    containerColor: Color,
    borderColor: Color = containerColor,
    borderAlpha: Float = 0f,
    shape: RoundedCornerShape = RoundedCornerShape(UiTokens.RadiusPill)
): Modifier {
    val base = this
        .clip(shape)
        .background(containerColor)
    return if (borderAlpha > 0f) {
        base.border(UiTokens.BorderThin, borderColor.copy(alpha = borderAlpha), shape)
    } else {
        base
    }
}

/**
 * Flat divider line used as a visual separator between groups within the same
 * card. Cheaper than Material3 [androidx.compose.material3.HorizontalDivider]
 * because it avoids elevation math.
 */
@Composable
internal fun Modifier.flatHorizontalDivider(
    color: Color = LockDivider,
    alpha: Float = 0.6f
): Modifier = this.background(color.copy(alpha = alpha))
