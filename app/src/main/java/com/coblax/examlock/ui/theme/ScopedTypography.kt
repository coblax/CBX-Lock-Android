package com.coblax.examlock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography used by upgraded screens. The existing Material [Typography]
 * remains unchanged for Home compatibility.
 */
@Immutable
internal data class AppTextStyleSet(
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyCompact: TextStyle,
    val diagnostic: TextStyle,
    val label: TextStyle,
    val button: TextStyle
)

private val ScopedFontFamily = FontFamily.SansSerif

internal val DefaultAppTextStyles = AppTextStyleSet(
    screenTitle = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    sectionTitle = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    ),
    cardTitle = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    body = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyCompact = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    diagnostic = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.15.sp
    ),
    label = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    button = TextStyle(
        fontFamily = ScopedFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    )
)

internal val LocalAppTextStyles = staticCompositionLocalOf { DefaultAppTextStyles }

internal object AppTextStyles {
    val screenTitle: TextStyle
        @Composable get() = LocalAppTextStyles.current.screenTitle
    val sectionTitle: TextStyle
        @Composable get() = LocalAppTextStyles.current.sectionTitle
    val cardTitle: TextStyle
        @Composable get() = LocalAppTextStyles.current.cardTitle
    val body: TextStyle
        @Composable get() = LocalAppTextStyles.current.body
    val bodyCompact: TextStyle
        @Composable get() = LocalAppTextStyles.current.bodyCompact
    val diagnostic: TextStyle
        @Composable get() = LocalAppTextStyles.current.diagnostic
    val label: TextStyle
        @Composable get() = LocalAppTextStyles.current.label
    val button: TextStyle
        @Composable get() = LocalAppTextStyles.current.button
}
