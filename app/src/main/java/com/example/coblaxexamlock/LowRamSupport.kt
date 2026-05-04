package com.example.coblaxexamlock

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import java.util.concurrent.CopyOnWriteArraySet

internal data class LowRamProfile(val enabled: Boolean)

internal fun resolveLowRamProfile(context: Context): LowRamProfile {
    val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)
    return LowRamProfile(enabled = activityManager?.isLowRamDevice == true)
}

internal object MemoryPressureCoordinator {
    private val listeners = CopyOnWriteArraySet<(Int) -> Unit>()

    @Volatile
    private var lastTrimLevel: Int? = null

    fun addListener(listener: (Int) -> Unit) {
        listeners.add(listener)
        lastTrimLevel?.let { trimLevel ->
            runCatching { listener(trimLevel) }
        }
    }

    fun removeListener(listener: (Int) -> Unit) {
        listeners.remove(listener)
    }

    fun dispatchTrimMemory(level: Int) {
        lastTrimLevel = level
        listeners.forEach { listener ->
            runCatching { listener(level) }
        }
    }

    @Suppress("DEPRECATION")
    fun dispatchLowMemory() {
        dispatchTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    @Suppress("DEPRECATION")
    fun shouldRespondToPressure(level: Int): Boolean {
        return when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> true
            else -> false
        }
    }

    @Suppress("DEPRECATION")
    fun shouldReleaseUiBitmaps(level: Int): Boolean {
        return level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
            level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
            level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
    }
}
