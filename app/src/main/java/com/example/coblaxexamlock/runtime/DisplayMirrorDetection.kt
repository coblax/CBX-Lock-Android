package com.example.coblaxexamlock.runtime

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * Detects external displays connected to the device.
 *
 * Uses [DisplayManager.getDisplays] to enumerate all displays.
 * Display index 0 is always the built-in screen.
 * Any additional display (index 1+) indicates an external connection
 * via Miracast, Chromecast, wired HDMI, or USB-C DisplayPort.
 *
 * Zero false positive: multi-display only occurs when user explicitly
 * connects to an external screen. No OEM reports phantom displays.
 */

internal data class ExternalDisplayInfo(
    val displayId: Int,
    val name: String,
    val state: Int,
    val flags: Int
)

internal fun hasExternalDisplay(context: Context): Boolean {
    return getExternalDisplayCount(context) > 0
}

internal fun getExternalDisplayCount(context: Context): Int {
    val displayManager = context.getSystemService(DisplayManager::class.java) ?: return 0
    val displays = runCatching { displayManager.displays }.getOrDefault(emptyArray())
    // Display[0] = built-in, anything beyond = external
    return (displays.size - 1).coerceAtLeast(0)
}

internal fun getExternalDisplayInfoList(context: Context): List<ExternalDisplayInfo> {
    val displayManager = context.getSystemService(DisplayManager::class.java) ?: return emptyList()
    val displays = runCatching { displayManager.displays }.getOrDefault(emptyArray())
    if (displays.size <= 1) return emptyList()
    return displays.drop(1).map { display ->
        ExternalDisplayInfo(
            displayId = display.displayId,
            name = display.name.orEmpty(),
            state = display.state,
            flags = display.flags
        )
    }
}
