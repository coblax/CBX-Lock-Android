package com.example.coblaxexamlock.ui.theme

import androidx.compose.material3.MaterialTheme
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

@Composable
fun COBLAXEXAMLOCKTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
