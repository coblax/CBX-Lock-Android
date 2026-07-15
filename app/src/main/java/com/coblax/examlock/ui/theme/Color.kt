package com.coblax.examlock.ui.theme

import androidx.compose.ui.graphics.Color


// ── Primary brand ──────────────────────────────────────────────────
val LockBlue = Color(0xFF3D7AF5)
val LockBlueDark = Color(0xFF2A5EC4)
val LockBlueDeep = Color(0xFF102E6A)
val LockBlueMid = Color(0xFF2B63C7)
val LockBlueSoft = Color(0xFF9CC8F4)

// ── Accent ─────────────────────────────────────────────────────────
val LockGold = Color(0xFFF4BF2E)
val LockGoldDark = Color(0xFFDC9A00)

// ── Surfaces ───────────────────────────────────────────────────────
val LockBackground = Color(0xFFF6F8FC)
val LockSurface = Color(0xFF1A1F2B)
val LockSurfaceSoft = Color(0xFFF0F4FA)
val LockCardBg = Color(0xFFFFFFFF)

// ── Borders & dividers ────────────────────────────────────────────
val LockOutline = Color(0xFFD4DEE9)
val LockDivider = Color(0xFFE4EAF2)

// ── Text ───────────────────────────────────────────────────────────
val LockTextPrimary = Color(0xFF1B2230)
val LockTextSecondary = Color(0xFF505D6F)
val LockTextMuted = Color(0xFF8C99A9)
val LockOnDark = Color(0xFFFFFFFF)

// ── Semantic status ────────────────────────────────────────────────
val LockStatusSafeFill   = Color(0xFFE6F6EB)
val LockStatusWarnFill   = Color(0xFFFFF6DC)
val LockStatusDangerFill = Color(0xFFFEEBEB)
val LockStatusSafe       = Color(0xFF28925A)
val LockStatusWarn       = Color(0xFFD4930A)
val LockStatusDanger     = Color(0xFFD42D26)

// ── Semantic status — extended palette ─────────────────────────────
// Issue / error text used across checklist and dialogs
val LockIssueText        = Color(0xFFB34A4A)   // dark-red issue text (22 occurrences)
// Stronger green for prominent safe labels and celebration text
val LockSafeEmphasis     = Color(0xFF2F8F63)   // bold green (12 occurrences)
val LockSafeStrong       = Color(0xFF1F7A4D)   // darker green for headings (6 occurrences)
// Background fills used in notice cards and warning banners
val LockWarnBgSoft       = Color(0xFFFFF8E6)   // light gold background (9 occurrences)
val LockWarnBgWarm       = Color(0xFFFFF8E8)   // warm gold background variant (6 occurrences)
val LockDangerBgSoft     = Color(0xFFFFEFEF)   // light red background (7 occurrences)
val LockDangerBgSubtle   = Color(0xFFFEF3F2)   // very subtle red tint (4 occurrences)
// Gold accent text for header badges / status indicators
val LockGoldAccent       = Color(0xFFC79317)   // muted gold for text (2 occurrences)

// ── Footer & chrome ────────────────────────────────────────────────
val LockFooterBg = Color(0xFFF4F7FB)

// ── Dialog semantic ────────────────────────────────────────────────
val LockDialogDangerBg   = Color(0xFFFDF1F1)
val LockDialogDangerIcon = Color(0xFFB42318)
val LockDialogSuccessBg  = Color(0xFFE6F6EB)

// ── Telegram ───────────────────────────────────────────────────────
val LockTelegramBlue     = Color(0xFF2AABEE)
val LockTelegramDisabled = Color(0xFFB5DDF3)

// ── Dark mode surfaces ────────────────────────────────────────────
val LockDarkBackground     = Color(0xFF0F1117)
val LockDarkSurface        = Color(0xFF1A1D27)
val LockDarkSurfaceSoft    = Color(0xFF22263A)
val LockDarkCardBg         = Color(0xFF1E2130)
val LockDarkOutline        = Color(0xFF333752)
val LockDarkDivider        = Color(0xFF2A2E42)

// ── Dark mode text ────────────────────────────────────────────────
val LockDarkTextPrimary    = Color(0xFFE8EAF0)
val LockDarkTextSecondary  = Color(0xFF9BA3B5)

// ── Dark mode semantic status ─────────────────────────────────────
val LockDarkStatusSafeFill   = Color(0xFF132B1A)
val LockDarkStatusWarnFill   = Color(0xFF2B2210)
val LockDarkStatusDangerFill = Color(0xFF2B1414)
val LockDarkStatusSafe       = Color(0xFF4DD87C)
val LockDarkStatusWarn       = Color(0xFFFFC547)
val LockDarkStatusDanger     = Color(0xFFFF6B63)

// ── Dark mode primary brand ───────────────────────────────────────
val LockDarkPrimary        = Color(0xFF6FA2FF)
val LockDarkPrimaryDark    = Color(0xFF4A82E0)

// ── Pre-computed alpha variants (performance) ─────────────────────
// Extracted from frequently repeated Color.copy(alpha = ...) calls
// to avoid per-recomposition allocations in hot composable paths.
val LockOutlineSubtle      = LockOutline.copy(alpha = 0.5f)
val LockOutlineMedium      = LockOutline.copy(alpha = 0.60f)
val LockOutlineStrong      = LockOutline.copy(alpha = 0.7f)
val LockBlueTint           = LockBlue.copy(alpha = 0.08f)
val LockBlueFill           = LockBlue.copy(alpha = 0.12f)
val LockDangerTint         = LockDialogDangerIcon.copy(alpha = 0.12f)
