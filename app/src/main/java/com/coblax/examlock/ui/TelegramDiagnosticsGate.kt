package com.coblax.examlock.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether Telegram diagnostic sending is available in the current composition.
 *
 * This is the single gate every "Send to Telegram" control reads. It combines two
 * conditions, resolved once at the app root:
 *  - the build actually ships remote diagnostics ([com.coblax.examlock.BuildConfig.REMOTE_DIAGNOSTICS_ENABLED]),
 *    which controls whether the bot token is even present, and
 *  - the admin has enabled it (AdminSettings.telegramDiagnosticsEnabled).
 *
 * When false, every send button is hidden — there is nothing to send and no way to send it.
 * Default is false so a control that renders outside the provider never offers a dead action.
 */
internal val LocalTelegramDiagnosticsEnabled = staticCompositionLocalOf { false }
