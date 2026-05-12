package com.example.coblaxexamlock.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    val RadiusMd: Dp = 14.dp
    val RadiusLg: Dp = 18.dp
    val RadiusXl: Dp = 22.dp
    val RadiusPill: Dp = 999.dp

    // Spacing scale
    val SpaceXs: Dp = 4.dp
    val SpaceSm: Dp = 8.dp
    val SpaceMd: Dp = 12.dp
    val SpaceLg: Dp = 16.dp
    val SpaceXl: Dp = 20.dp

    // Border widths
    val BorderThin: Dp = 1.dp

    // Border alpha presets — used to give visual separation without shadows
    const val BorderAlphaSubtle: Float = 0.55f
    const val BorderAlphaDefault: Float = 0.70f
    const val BorderAlphaStrong: Float = 0.90f
}

/**
 * Lightweight alternative to Material3 [androidx.compose.material3.Surface] that
 * uses only `background` + `border` — no `RenderNode` for elevation, no shadow
 * rendering pass. Use this as the default card container across the app.
 */
internal fun Modifier.flatCard(
    containerColor: Color = Color.White,
    borderColor: Color = LockOutline,
    borderAlpha: Float = UiTokens.BorderAlphaDefault,
    shape: RoundedCornerShape = RoundedCornerShape(UiTokens.RadiusLg)
): Modifier {
    val resolvedBorder = borderColor.copy(alpha = borderAlpha)
    return this
        .clip(shape)
        .background(containerColor)
        .border(UiTokens.BorderThin, resolvedBorder, shape)
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
    color: Color = LockOutline,
    alpha: Float = 0.5f
): Modifier = this.background(color.copy(alpha = alpha))
