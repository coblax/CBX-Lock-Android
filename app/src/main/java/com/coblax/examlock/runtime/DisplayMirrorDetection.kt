package com.coblax.examlock.runtime

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

internal fun displayStateLabel(state: Int): String {
    return when (state) {
        0 -> "UNKNOWN"
        1 -> "OFF"
        2 -> "ON"
        3 -> "DOZE"
        4 -> "DOZE_SUSPEND"
        5 -> "VR"
        6 -> "ON_SUSPEND"
        else -> "state_$state"
    }
}

internal fun displayFlagsLabel(flags: Int): String {
    val labels = mutableListOf<String>()
    if (flags and Display.FLAG_SUPPORTS_PROTECTED_BUFFERS != 0) {
        labels.add("SUPPORTS_PROTECTED_BUFFERS")
    }
    if (flags and Display.FLAG_SECURE != 0) {
        labels.add("SECURE")
    }
    if (flags and Display.FLAG_PRIVATE != 0) {
        labels.add("PRIVATE")
    }
    if (flags and Display.FLAG_PRESENTATION != 0) {
        labels.add("PRESENTATION")
    }
    return labels.joinToString("|").ifBlank { "none" }
}

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
