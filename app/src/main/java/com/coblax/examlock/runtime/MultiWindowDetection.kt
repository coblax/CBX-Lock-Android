package com.coblax.examlock.runtime

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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

internal data class MultiWindowModeInfo(
    val multiWindowApiSupported: Boolean,
    val pictureInPictureApiSupported: Boolean,
    val inMultiWindowMode: Boolean,
    val inPictureInPictureMode: Boolean
) {
    val inAnySplitMode: Boolean
        get() = inMultiWindowMode || inPictureInPictureMode
}

internal fun readMultiWindowModeInfo(context: Context): MultiWindowModeInfo {
    val multiWindowApiSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    val pictureInPictureApiSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    return MultiWindowModeInfo(
        multiWindowApiSupported = multiWindowApiSupported,
        pictureInPictureApiSupported = pictureInPictureApiSupported,
        inMultiWindowMode = isInMultiWindowMode(context),
        inPictureInPictureMode = isInPictureInPictureMode(context)
    )
}

/**
 * The Activity behind [context]. Compose can hand out a ContextWrapper around it, and a
 * plain `as? Activity` then answered "not split" every time, silently skipping the check.
 */
internal fun Context.findHostActivity(): Activity? {
    var current: Context? = this
    repeat(8) {
        when (current) {
            is Activity -> return current as Activity
            is ContextWrapper -> current = (current as ContextWrapper).baseContext
            else -> return null
        }
    }
    return null
}

internal fun isInMultiWindowMode(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
    val activity = context.findHostActivity() ?: return false
    return runCatching { activity.isInMultiWindowMode }.getOrDefault(false)
}

internal fun isInPictureInPictureMode(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val activity = context.findHostActivity() ?: return false
    return runCatching { activity.isInPictureInPictureMode }.getOrDefault(false)
}

internal fun isInAnySplitMode(context: Context): Boolean {
    return readMultiWindowModeInfo(context).inAnySplitMode
}
