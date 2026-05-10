package com.example.coblaxexamlock.runtime

import android.app.Activity
import android.content.Context
import android.os.Build

/**
 * Detects whether the current activity is in multi-window or picture-in-picture mode.
 *
 * - [isInMultiWindowMode]: Available from API 24 (Android 7.0 Nougat).
 * - [isInPictureInPictureMode]: Available from API 26 (Android 8.0 Oreo).
 *
 * Zero false positive: these flags are only true when the user explicitly
 * enters split-screen or PiP mode. In normal single-app mode, always false.
 */

internal fun isInMultiWindowMode(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
    val activity = context as? Activity ?: return false
    return runCatching { activity.isInMultiWindowMode }.getOrDefault(false)
}

internal fun isInPictureInPictureMode(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val activity = context as? Activity ?: return false
    return runCatching { activity.isInPictureInPictureMode }.getOrDefault(false)
}

internal fun isInAnySplitMode(context: Context): Boolean {
    return isInMultiWindowMode(context) || isInPictureInPictureMode(context)
}
