package com.coblax.examlock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


private val LightColorScheme = lightColorScheme(
    primary = LockBlue,
    secondary = LockBlueDark,
    background = LockBackground,
    surface = LockCardBg,
    surfaceVariant = LockSurfaceSoft,
    outline = LockOutline,
    outlineVariant = LockDivider,
    onPrimary = LockOnDark,
    onBackground = LockTextPrimary,
    onSurface = LockTextPrimary,
    onSurfaceVariant = LockTextSecondary,
    error = LockStatusDanger,
    onError = LockOnDark
)

private val DarkColorScheme = darkColorScheme(
    primary = LockDarkPrimary,
    secondary = LockDarkPrimaryDark,
    background = LockDarkBackground,
    surface = LockDarkCardBg,
    surfaceVariant = LockDarkSurfaceSoft,
    outline = LockDarkOutline,
    outlineVariant = LockDarkDivider,
    onPrimary = LockOnDark,
    onBackground = LockDarkTextPrimary,
    onSurface = LockDarkTextPrimary,
    onSurfaceVariant = LockDarkTextSecondary,
    error = LockDarkStatusDanger,
    onError = LockOnDark
)

@Composable
fun COBLAXEXAMLOCKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
