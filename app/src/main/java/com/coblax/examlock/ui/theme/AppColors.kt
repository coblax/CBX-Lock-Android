package com.coblax.examlock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.coblax.examlock.model.ThemeMode

/**
 * Resolved color palette that adapts to the current theme mode (light/dark).
 *
 * All UI screens should use [AppColors] composable accessors instead of
 * referencing `Lock*` color constants directly, so that dark/light mode
 * works consistently across the entire app.
 */
@Immutable
data class ExamLockColorPalette(
    // ── Primary brand ──────────────────────────────────────────────
    val blue: Color,
    val blueDark: Color,
    val blueDeep: Color,
    val blueMid: Color,
    val blueSoft: Color,

    // ── Accent ─────────────────────────────────────────────────────
    val gold: Color,
    val goldDark: Color,

    // ── Surfaces ───────────────────────────────────────────────────
    val background: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val cardBg: Color,

    // ── Borders & dividers ────────────────────────────────────────
    val outline: Color,
    val divider: Color,

    // ── Text ───────────────────────────────────────────────────────
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val onDark: Color,

    // ── Semantic status ────────────────────────────────────────────
    val statusSafeFill: Color,
    val statusWarnFill: Color,
    val statusDangerFill: Color,
    val statusSafe: Color,
    val statusWarn: Color,
    val statusDanger: Color,

    // ── Semantic status — extended palette ─────────────────────────
    val issueText: Color,
    val safeEmphasis: Color,
    val safeStrong: Color,
    val warnBgSoft: Color,
    val warnBgWarm: Color,
    val dangerBgSoft: Color,
    val dangerBgSubtle: Color,
    val goldAccent: Color,

    // ── Footer & chrome ────────────────────────────────────────────
    val footerBg: Color,

    // ── Dialog semantic ────────────────────────────────────────────
    val dialogDangerBg: Color,
    val dialogDangerIcon: Color,
    val dialogSuccessBg: Color,

    // ── Telegram ───────────────────────────────────────────────────
    val telegramBlue: Color,
    val telegramDisabled: Color,

    // ── Pre-computed alpha variants ────────────────────────────────
    val outlineSubtle: Color,
    val outlineMedium: Color,
    val outlineStrong: Color,
    val blueTint: Color,
    val blueFill: Color,
    val dangerTint: Color,

    // ── isDark flag for conditional rendering ──────────────────────
    val isDark: Boolean
) {
    // Deep blue remains a container color; text needs a brighter blue in dark mode.
    val brandText: Color get() = if (isDark) blue else blueDeep
}

internal fun lightExamLockColors(): ExamLockColorPalette = ExamLockColorPalette(
    blue = LockBlue,
    blueDark = LockBlueDark,
    blueDeep = LockBlueDeep,
    blueMid = LockBlueMid,
    blueSoft = LockBlueSoft,
    gold = LockGold,
    goldDark = LockGoldDark,
    background = LockBackground,
    surface = LockSurface,
    surfaceSoft = LockSurfaceSoft,
    cardBg = LockCardBg,
    outline = LockOutline,
    divider = LockDivider,
    textPrimary = LockTextPrimary,
    textSecondary = LockTextSecondary,
    textMuted = LockTextMuted,
    onDark = LockOnDark,
    statusSafeFill = LockStatusSafeFill,
    statusWarnFill = LockStatusWarnFill,
    statusDangerFill = LockStatusDangerFill,
    statusSafe = LockStatusSafe,
    statusWarn = LockStatusWarn,
    statusDanger = LockStatusDanger,
    issueText = LockIssueText,
    safeEmphasis = LockSafeEmphasis,
    safeStrong = LockSafeStrong,
    warnBgSoft = LockWarnBgSoft,
    warnBgWarm = LockWarnBgWarm,
    dangerBgSoft = LockDangerBgSoft,
    dangerBgSubtle = LockDangerBgSubtle,
    goldAccent = LockGoldAccent,
    footerBg = LockFooterBg,
    dialogDangerBg = LockDialogDangerBg,
    dialogDangerIcon = LockDialogDangerIcon,
    dialogSuccessBg = LockDialogSuccessBg,
    telegramBlue = LockTelegramBlue,
    telegramDisabled = LockTelegramDisabled,
    outlineSubtle = LockOutlineSubtle,
    outlineMedium = LockOutlineMedium,
    outlineStrong = LockOutlineStrong,
    blueTint = LockBlueTint,
    blueFill = LockBlueFill,
    dangerTint = LockDangerTint,
    isDark = false
)

internal fun darkExamLockColors(): ExamLockColorPalette = ExamLockColorPalette(
    blue = LockDarkPrimary,
    blueDark = LockDarkPrimaryDark,
    blueDeep = Color(0xFF1A3B7A),
    blueMid = LockDarkPrimaryDark,
    blueSoft = Color(0xFF4A6FA5),
    gold = LockGold,
    goldDark = LockGoldDark,
    background = LockDarkBackground,
    surface = LockDarkSurface,
    surfaceSoft = LockDarkSurfaceSoft,
    cardBg = LockDarkCardBg,
    outline = LockDarkOutline,
    divider = LockDarkDivider,
    textPrimary = LockDarkTextPrimary,
    textSecondary = LockDarkTextSecondary,
    textMuted = Color(0xFF6E7A8E),
    onDark = LockOnDark,
    statusSafeFill = LockDarkStatusSafeFill,
    statusWarnFill = LockDarkStatusWarnFill,
    statusDangerFill = LockDarkStatusDangerFill,
    statusSafe = LockDarkStatusSafe,
    statusWarn = LockDarkStatusWarn,
    statusDanger = LockDarkStatusDanger,
    issueText = Color(0xFFFF8A80),
    safeEmphasis = Color(0xFF66D49A),
    safeStrong = Color(0xFF4DD87C),
    warnBgSoft = Color(0xFF2B2210),
    warnBgWarm = Color(0xFF332912),
    dangerBgSoft = Color(0xFF2B1414),
    dangerBgSubtle = Color(0xFF251515),
    goldAccent = Color(0xFFE8B84A),
    footerBg = Color(0xFF151820),
    dialogDangerBg = Color(0xFF2B1414),
    dialogDangerIcon = Color(0xFFFF6B63),
    dialogSuccessBg = LockDarkStatusSafeFill,
    telegramBlue = LockTelegramBlue,
    telegramDisabled = Color(0xFF3A5A7A),
    outlineSubtle = LockDarkOutline.copy(alpha = 0.5f),
    outlineMedium = LockDarkOutline.copy(alpha = 0.60f),
    outlineStrong = LockDarkOutline.copy(alpha = 0.7f),
    blueTint = LockDarkPrimary.copy(alpha = 0.10f),
    blueFill = LockDarkPrimary.copy(alpha = 0.15f),
    dangerTint = LockDarkStatusDanger.copy(alpha = 0.15f),
    isDark = true
)

/**
 * CompositionLocal providing the resolved [ExamLockColorPalette] throughout the
 * composition tree. Set by [COBLAXEXAMLOCKTheme] based on the active [ThemeMode].
 */
internal val LocalExamLockColors = staticCompositionLocalOf { lightExamLockColors() }

/**
 * Convenience accessor for the theme-aware color palette.
 *
 * Usage in composable code:
 * ```
 * val colors = AppColors.current
 * Box(modifier = Modifier.background(colors.background))
 * ```
 */
internal object AppColors {
    /** The resolved color palette for the current composition. */
    val current: ExamLockColorPalette
        @Composable get() = LocalExamLockColors.current
}

/**
 * Resolves whether dark mode should be active based on the given [ThemeMode].
 */
@Composable
internal fun resolveIsDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
