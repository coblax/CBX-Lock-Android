package com.example.coblaxexamlock

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import java.util.concurrent.CopyOnWriteArraySet

private const val OneMegabyteBytes = 1024L * 1024L
private const val LowRamTotalMemoryMb = 1024L
private const val SevereLowRamTotalMemoryMb = 768L
private const val LowRamMemoryClassMb = 128
private const val SevereLowRamMemoryClassMb = 96

internal data class LowRamProfile(
    val enabled: Boolean = false,
    val severe: Boolean = false,
    val qrMaxEdgePx: Int = 2560,
    val deferHeavyUi: Boolean = false,
    val slowPollingMultiplier: Int = 1
)

internal fun calculateLowRamProfile(
    isLowRamDevice: Boolean,
    totalMemoryBytes: Long?,
    memoryClassMb: Int?
): LowRamProfile {
    val totalMemoryMb = totalMemoryBytes
        ?.takeIf { it > 0L }
        ?.let { it / OneMegabyteBytes }
    val normalizedMemoryClassMb = memoryClassMb?.takeIf { it > 0 }

    val totalMemoryLow = totalMemoryMb?.let { it <= LowRamTotalMemoryMb } == true
    val memoryClassLow = normalizedMemoryClassMb?.let { it <= LowRamMemoryClassMb } == true
    val totalMemorySevere = totalMemoryMb?.let { it <= SevereLowRamTotalMemoryMb } == true
    val memoryClassSevere =
        normalizedMemoryClassMb?.let { it <= SevereLowRamMemoryClassMb } == true

    val severe = totalMemorySevere || memoryClassSevere
    val enabled = isLowRamDevice || totalMemoryLow || memoryClassLow || severe

    return LowRamProfile(
        enabled = enabled,
        severe = severe,
        qrMaxEdgePx = when {
            severe -> 960
            enabled -> 1280
            else -> 2560
        },
        deferHeavyUi = enabled,
        slowPollingMultiplier = if (enabled) 2 else 1
    )
}

internal fun resolveLowRamProfile(context: Context): LowRamProfile {
    val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)
    val memoryInfo = activityManager?.let {
        ActivityManager.MemoryInfo().also { info ->
            runCatching { activityManager.getMemoryInfo(info) }
        }
    }
    return calculateLowRamProfile(
        isLowRamDevice = activityManager?.isLowRamDevice == true,
        totalMemoryBytes = memoryInfo?.totalMem,
        memoryClassMb = activityManager?.memoryClass
    )
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
