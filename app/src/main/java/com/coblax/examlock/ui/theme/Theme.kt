package com.coblax.examlock.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.model.ThemeMode

/** Duration of the smooth cross-fade between light ↔ dark palettes. */
private const val ThemeTransitionMs = 350

private val ThemeTransitionSpec = tween<Color>(
    durationMillis = ThemeTransitionMs,
    easing = FastOutSlowInEasing
)

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

/**
 * CompositionLocal providing the current [ThemeMode] for UI components
 * that need to read or display the active theme preference (e.g. toggle buttons).
 */
internal val LocalThemeMode = staticCompositionLocalOf { ThemeMode.System }

/**
 * Animates a single color using the shared theme transition spec.
 */
@Composable
private fun animateThemeColor(target: Color): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = ThemeTransitionSpec,
        label = "themeColor"
    )
    return animated
}

/**
 * Returns an [ExamLockColorPalette] where every color is wrapped in
 * [animateColorAsState] so that light ↔ dark transitions are smooth.
 */
@Composable
private fun animateExamLockColors(target: ExamLockColorPalette): ExamLockColorPalette {
    return ExamLockColorPalette(
        blue = animateThemeColor(target.blue),
        blueDark = animateThemeColor(target.blueDark),
        blueDeep = animateThemeColor(target.blueDeep),
        blueMid = animateThemeColor(target.blueMid),
        blueSoft = animateThemeColor(target.blueSoft),
        gold = animateThemeColor(target.gold),
        goldDark = animateThemeColor(target.goldDark),
        background = animateThemeColor(target.background),
        surface = animateThemeColor(target.surface),
        surfaceSoft = animateThemeColor(target.surfaceSoft),
        cardBg = animateThemeColor(target.cardBg),
        outline = animateThemeColor(target.outline),
        divider = animateThemeColor(target.divider),
        textPrimary = animateThemeColor(target.textPrimary),
        textSecondary = animateThemeColor(target.textSecondary),
        textMuted = animateThemeColor(target.textMuted),
        onDark = animateThemeColor(target.onDark),
        statusSafeFill = animateThemeColor(target.statusSafeFill),
        statusWarnFill = animateThemeColor(target.statusWarnFill),
        statusDangerFill = animateThemeColor(target.statusDangerFill),
        statusSafe = animateThemeColor(target.statusSafe),
        statusWarn = animateThemeColor(target.statusWarn),
        statusDanger = animateThemeColor(target.statusDanger),
        issueText = animateThemeColor(target.issueText),
        safeEmphasis = animateThemeColor(target.safeEmphasis),
        safeStrong = animateThemeColor(target.safeStrong),
        warnBgSoft = animateThemeColor(target.warnBgSoft),
        warnBgWarm = animateThemeColor(target.warnBgWarm),
        dangerBgSoft = animateThemeColor(target.dangerBgSoft),
        dangerBgSubtle = animateThemeColor(target.dangerBgSubtle),
        goldAccent = animateThemeColor(target.goldAccent),
        footerBg = animateThemeColor(target.footerBg),
        dialogDangerBg = animateThemeColor(target.dialogDangerBg),
        dialogDangerIcon = animateThemeColor(target.dialogDangerIcon),
        dialogSuccessBg = animateThemeColor(target.dialogSuccessBg),
        telegramBlue = animateThemeColor(target.telegramBlue),
        telegramDisabled = animateThemeColor(target.telegramDisabled),
        outlineSubtle = animateThemeColor(target.outlineSubtle),
        outlineMedium = animateThemeColor(target.outlineMedium),
        outlineStrong = animateThemeColor(target.outlineStrong),
        blueTint = animateThemeColor(target.blueTint),
        blueFill = animateThemeColor(target.blueFill),
        dangerTint = animateThemeColor(target.dangerTint),
        isDark = target.isDark
    )
}

/**
 * Returns a [ColorScheme] where key surface/text colors are animated
 * for a smooth Material3 theme transition.
 */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animateThemeColor(target.primary),
        secondary = animateThemeColor(target.secondary),
        background = animateThemeColor(target.background),
        surface = animateThemeColor(target.surface),
        surfaceVariant = animateThemeColor(target.surfaceVariant),
        outline = animateThemeColor(target.outline),
        outlineVariant = animateThemeColor(target.outlineVariant),
        onPrimary = animateThemeColor(target.onPrimary),
        onBackground = animateThemeColor(target.onBackground),
        onSurface = animateThemeColor(target.onSurface),
        onSurfaceVariant = animateThemeColor(target.onSurfaceVariant),
        error = animateThemeColor(target.error),
        onError = animateThemeColor(target.onError)
    )
}

@Composable
internal fun COBLAXEXAMLOCKTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveIsDarkTheme(themeMode)
    val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val targetExamLockColors = if (darkTheme) darkExamLockColors() else lightExamLockColors()

    // Both palettes are published through staticCompositionLocalOf, so every animation
    // frame invalidates the whole UI tree. That is affordable on a normal phone but not
    // on the low-RAM devices this app targets, where it turns a theme switch into a
    // multi-second stutter. Those devices get the new palette immediately instead.
    val animateTransition = !LocalLowRamProfile.current.deferHeavyUi
    val animatedColorScheme =
        if (animateTransition) animateColorScheme(targetColorScheme) else targetColorScheme
    val animatedExamLockColors =
        if (animateTransition) animateExamLockColors(targetExamLockColors) else targetExamLockColors

    // enableEdgeToEdge() derives system-bar icon tint from the *system* night setting,
    // so picking Dark on a light phone (or Light on a dark one) leaves dark icons on a
    // dark bar. Follow the resolved app theme instead.
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(darkTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalExamLockColors provides animatedExamLockColors
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = Typography,
            content = content
        )
    }
}
