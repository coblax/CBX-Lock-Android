package com.example.coblaxexamlock

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import com.example.coblaxexamlock.config.AdminKeyLowRamProfileOverride
import com.example.coblaxexamlock.config.AdminPreferencesName
import java.util.concurrent.CopyOnWriteArraySet

private const val OneMegabyteBytes = 1024L * 1024L
private const val LowRamTotalMemoryMb = 2048L
private const val SevereLowRamTotalMemoryMb = 1024L
private const val UltraLowRamAvailableMemoryMb = 512L
private const val LowRamMemoryClassMb = 128
private const val SevereLowRamMemoryClassMb = 96
private const val NormalQrMaxEdgePx = 2560
private const val LowQrMaxEdgePx = 1024
private const val UltraQrMaxEdgePx = 720
private const val NormalDiagnosticLogMaxEntries = 20
private const val LowDiagnosticLogMaxEntries = 16
private const val UltraDiagnosticLogMaxEntries = 8
private const val NormalManualRefreshCooldownMillis = 0L
private const val LowManualRefreshCooldownMillis = 800L
private const val UltraManualRefreshCooldownMillis = 1_200L
private const val NormalScreenPinningSteadyPollMillis = 1_000L
private const val LowScreenPinningSteadyPollMillis = 2_000L
private const val UltraScreenPinningSteadyPollMillis = 4_000L
private const val NormalAccessibilityLivenessPollMillis = 1_000L
private const val LowAccessibilityLivenessPollMillis = 2_500L
private const val UltraAccessibilityLivenessPollMillis = 5_000L
private const val NormalExamServerProbeIntervalMillis = 30_000L
private const val LowExamServerProbeIntervalMillis = 60_000L
private const val UltraExamServerProbeIntervalMillis = 120_000L
private const val NormalDetectorMetadataCacheMaxEntries = 64
private const val LowDetectorMetadataCacheMaxEntries = 24
private const val UltraDetectorMetadataCacheMaxEntries = 8

internal enum class LowRamTier {
    Normal,
    Low,
    Ultra
}

internal enum class LowRamProfileOverride {
    Auto,
    Normal,
    Low,
    Ultra
}

internal data class LowRamProfileBadgePalette(
    val containerColorArgb: Int,
    val contentColorArgb: Int,
    val borderColorArgb: Int,
    val dotColorArgb: Int
)

internal data class LowRamProfile(
    val enabled: Boolean = false,
    val severe: Boolean = false,
    val ultra: Boolean = false,
    val totalMemoryMb: Long? = null,
    val availableMemoryMb: Long? = null,
    val memoryLow: Boolean = false,
    val lowRamOverride: LowRamProfileOverride = LowRamProfileOverride.Auto,
    val detectedTier: LowRamTier? = null,
    val qrMaxEdgePx: Int = NormalQrMaxEdgePx,
    val deferHeavyUi: Boolean = false,
    val slowPollingMultiplier: Int = 1,
    val diagnosticLogMaxEntries: Int = NormalDiagnosticLogMaxEntries,
    val manualRefreshCooldownMillis: Long = NormalManualRefreshCooldownMillis,
    val screenPinningSteadyPollMillis: Long = NormalScreenPinningSteadyPollMillis,
    val accessibilityLivenessPollMillis: Long = NormalAccessibilityLivenessPollMillis,
    val examServerProbeIntervalMillis: Long = NormalExamServerProbeIntervalMillis,
    val detectorMetadataCacheMaxEntries: Int = NormalDetectorMetadataCacheMaxEntries,
    val disableNonEssentialAnimations: Boolean = false
) {
    val tier: LowRamTier
        get() = when {
            ultra || severe -> LowRamTier.Ultra
            enabled -> LowRamTier.Low
            else -> LowRamTier.Normal
        }

    fun diagnosticSummary(): String {
        return "enabled=$enabled" +
            " severe=$severe" +
            " ultra=$ultra" +
            " total=${totalMemoryMb ?: "-"}MB" +
            " avail=${availableMemoryMb ?: "-"}MB" +
            " memoryLow=$memoryLow" +
            " override=${lowRamOverride.name}" +
            " detected=${detectedTier?.name ?: tier.name}" +
            " effective=${tier.name}" +
            " qrMaxEdgePx=$qrMaxEdgePx" +
            " polling=${slowPollingMultiplier}x" +
            " logMax=$diagnosticLogMaxEntries" +
            " refreshCooldownMs=$manualRefreshCooldownMillis" +
            " screenPinningPollMs=$screenPinningSteadyPollMillis" +
            " accessibilityPollMs=$accessibilityLivenessPollMillis" +
            " serverProbeMs=$examServerProbeIntervalMillis" +
            " detectorCacheMax=$detectorMetadataCacheMaxEntries" +
            " reduceMotion=$disableNonEssentialAnimations"
    }
}

internal fun parseLowRamProfileOverride(rawValue: String?): LowRamProfileOverride {
    val normalizedValue = rawValue.orEmpty().trim()
    return LowRamProfileOverride.entries.firstOrNull { override ->
        override.name.equals(normalizedValue, ignoreCase = true)
    } ?: LowRamProfileOverride.Auto
}

internal fun lowRamProfileOverrideToRaw(override: LowRamProfileOverride): String = override.name

internal fun lowRamProfileOverrideOptions(): List<LowRamProfileOverride> =
    listOf(
        LowRamProfileOverride.Auto,
        LowRamProfileOverride.Normal,
        LowRamProfileOverride.Low,
        LowRamProfileOverride.Ultra
    )

private fun tierRank(tier: LowRamTier): Int =
    when (tier) {
        LowRamTier.Normal -> 0
        LowRamTier.Low -> 1
        LowRamTier.Ultra -> 2
    }

private fun forcedTierForOverride(override: LowRamProfileOverride): LowRamTier? =
    when (override) {
        LowRamProfileOverride.Auto -> null
        LowRamProfileOverride.Normal -> LowRamTier.Normal
        LowRamProfileOverride.Low -> LowRamTier.Low
        LowRamProfileOverride.Ultra -> LowRamTier.Ultra
    }

internal fun isLowRamProfileOverrideRisky(
    detectedProfile: LowRamProfile,
    override: LowRamProfileOverride
): Boolean {
    val forcedTier = forcedTierForOverride(override) ?: return false
    return tierRank(forcedTier) < tierRank(detectedProfile.tier)
}

internal fun applyLowRamProfileOverride(
    detectedProfile: LowRamProfile,
    override: LowRamProfileOverride
): LowRamProfile {
    val detectedTier = detectedProfile.detectedTier ?: detectedProfile.tier
    return when (override) {
        LowRamProfileOverride.Auto -> detectedProfile.copy(
            lowRamOverride = LowRamProfileOverride.Auto,
            detectedTier = detectedTier
        )
        LowRamProfileOverride.Normal -> detectedProfile.copy(
            enabled = false,
            severe = false,
            ultra = false,
            lowRamOverride = override,
            detectedTier = detectedTier,
            qrMaxEdgePx = NormalQrMaxEdgePx,
            deferHeavyUi = false,
            slowPollingMultiplier = 1,
            diagnosticLogMaxEntries = NormalDiagnosticLogMaxEntries,
            manualRefreshCooldownMillis = NormalManualRefreshCooldownMillis,
            screenPinningSteadyPollMillis = NormalScreenPinningSteadyPollMillis,
            accessibilityLivenessPollMillis = NormalAccessibilityLivenessPollMillis,
            examServerProbeIntervalMillis = NormalExamServerProbeIntervalMillis,
            detectorMetadataCacheMaxEntries = NormalDetectorMetadataCacheMaxEntries,
            disableNonEssentialAnimations = false
        )
        LowRamProfileOverride.Low -> detectedProfile.copy(
            enabled = true,
            severe = false,
            ultra = false,
            lowRamOverride = override,
            detectedTier = detectedTier,
            qrMaxEdgePx = LowQrMaxEdgePx,
            deferHeavyUi = true,
            slowPollingMultiplier = 2,
            diagnosticLogMaxEntries = LowDiagnosticLogMaxEntries,
            manualRefreshCooldownMillis = LowManualRefreshCooldownMillis,
            screenPinningSteadyPollMillis = LowScreenPinningSteadyPollMillis,
            accessibilityLivenessPollMillis = LowAccessibilityLivenessPollMillis,
            examServerProbeIntervalMillis = LowExamServerProbeIntervalMillis,
            detectorMetadataCacheMaxEntries = LowDetectorMetadataCacheMaxEntries,
            disableNonEssentialAnimations = true
        )
        LowRamProfileOverride.Ultra -> detectedProfile.copy(
            enabled = true,
            severe = true,
            ultra = true,
            lowRamOverride = override,
            detectedTier = detectedTier,
            qrMaxEdgePx = UltraQrMaxEdgePx,
            deferHeavyUi = true,
            slowPollingMultiplier = 6,
            diagnosticLogMaxEntries = UltraDiagnosticLogMaxEntries,
            manualRefreshCooldownMillis = UltraManualRefreshCooldownMillis,
            screenPinningSteadyPollMillis = UltraScreenPinningSteadyPollMillis,
            accessibilityLivenessPollMillis = UltraAccessibilityLivenessPollMillis,
            examServerProbeIntervalMillis = UltraExamServerProbeIntervalMillis,
            detectorMetadataCacheMaxEntries = UltraDetectorMetadataCacheMaxEntries,
            disableNonEssentialAnimations = true
        )
    }
}

internal fun lowRamProfileBadgeLabel(profile: LowRamProfile): String =
    when (profile.tier) {
        LowRamTier.Normal -> "Profil Normal"
        LowRamTier.Low -> "Profil Low"
        LowRamTier.Ultra -> "Profil Ultra"
    }

internal fun lowRamProfileBadgePalette(profile: LowRamProfile): LowRamProfileBadgePalette =
    when (profile.tier) {
        LowRamTier.Normal -> LowRamProfileBadgePalette(
            containerColorArgb = 0xFFF4F7FB.toInt(),
            contentColorArgb = 0xFF102E6A.toInt(),
            borderColorArgb = 0xFFD4DEE9.toInt(),
            dotColorArgb = 0xFF3D7AF5.toInt()
        )
        LowRamTier.Low -> LowRamProfileBadgePalette(
            containerColorArgb = 0xFFFFF7E6.toInt(),
            contentColorArgb = 0xFF6F4700.toInt(),
            borderColorArgb = 0xFFE6D3A3.toInt(),
            dotColorArgb = 0xFFD99200.toInt()
        )
        LowRamTier.Ultra -> LowRamProfileBadgePalette(
            containerColorArgb = 0xFF102E6A.toInt(),
            contentColorArgb = 0xFFFFFFFF.toInt(),
            borderColorArgb = 0xFF244B91.toInt(),
            dotColorArgb = 0xFFFFC247.toInt()
        )
    }

internal fun calculateLowRamProfile(
    isLowRamDevice: Boolean,
    totalMemoryBytes: Long?,
    memoryClassMb: Int?,
    availableMemoryBytes: Long? = null,
    memoryLow: Boolean = false
): LowRamProfile {
    val totalMemoryMb = totalMemoryBytes
        ?.takeIf { it > 0L }
        ?.let { it / OneMegabyteBytes }
    val availableMemoryMb = availableMemoryBytes
        ?.takeIf { it > 0L }
        ?.let { it / OneMegabyteBytes }
    val normalizedMemoryClassMb = memoryClassMb?.takeIf { it > 0 }

    val totalMemoryLow = totalMemoryMb?.let { it <= LowRamTotalMemoryMb } == true
    val memoryClassLow = normalizedMemoryClassMb?.let { it <= LowRamMemoryClassMb } == true
    val totalMemorySevere = totalMemoryMb?.let { it <= SevereLowRamTotalMemoryMb } == true
    val memoryClassSevere =
        normalizedMemoryClassMb?.let { it <= SevereLowRamMemoryClassMb } == true
    val availableMemoryUltra = availableMemoryMb?.let { it <= UltraLowRamAvailableMemoryMb } == true

    val ultra = totalMemorySevere || memoryClassSevere || availableMemoryUltra || memoryLow
    val severe = ultra
    val enabled = isLowRamDevice || totalMemoryLow || memoryClassLow || severe

    return LowRamProfile(
        enabled = enabled,
        severe = severe,
        ultra = ultra,
        totalMemoryMb = totalMemoryMb,
        availableMemoryMb = availableMemoryMb,
        memoryLow = memoryLow,
        qrMaxEdgePx = when {
            ultra -> UltraQrMaxEdgePx
            enabled -> LowQrMaxEdgePx
            else -> NormalQrMaxEdgePx
        },
        deferHeavyUi = enabled,
        slowPollingMultiplier = when {
            ultra -> 6
            enabled -> 2
            else -> 1
        },
        diagnosticLogMaxEntries = when {
            ultra -> UltraDiagnosticLogMaxEntries
            enabled -> LowDiagnosticLogMaxEntries
            else -> NormalDiagnosticLogMaxEntries
        },
        manualRefreshCooldownMillis = when {
            ultra -> UltraManualRefreshCooldownMillis
            enabled -> LowManualRefreshCooldownMillis
            else -> NormalManualRefreshCooldownMillis
        },
        screenPinningSteadyPollMillis = when {
            ultra -> UltraScreenPinningSteadyPollMillis
            enabled -> LowScreenPinningSteadyPollMillis
            else -> NormalScreenPinningSteadyPollMillis
        },
        accessibilityLivenessPollMillis = when {
            ultra -> UltraAccessibilityLivenessPollMillis
            enabled -> LowAccessibilityLivenessPollMillis
            else -> NormalAccessibilityLivenessPollMillis
        },
        examServerProbeIntervalMillis = when {
            ultra -> UltraExamServerProbeIntervalMillis
            enabled -> LowExamServerProbeIntervalMillis
            else -> NormalExamServerProbeIntervalMillis
        },
        detectorMetadataCacheMaxEntries = when {
            ultra -> UltraDetectorMetadataCacheMaxEntries
            enabled -> LowDetectorMetadataCacheMaxEntries
            else -> NormalDetectorMetadataCacheMaxEntries
        },
        disableNonEssentialAnimations = enabled
    )
}

@Suppress("DEPRECATION")
internal fun shouldEscalateRuntimeLowRamProfile(
    trimLevel: Int? = null,
    availableMemoryBytes: Long? = null,
    memoryLow: Boolean = false
): Boolean {
    val availableMemoryMb = availableMemoryBytes
        ?.takeIf { it > 0L }
        ?.let { it / OneMegabyteBytes }
    return memoryLow ||
        availableMemoryMb?.let { it <= UltraLowRamAvailableMemoryMb } == true ||
        trimLevel == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
        trimLevel == ComponentCallbacks2.TRIM_MEMORY_COMPLETE
}

@Suppress("DEPRECATION")
internal fun resolveRuntimePressureProfile(
    baseProfile: LowRamProfile,
    trimLevel: Int? = null,
    availableMemoryBytes: Long? = null,
    memoryLow: Boolean = false
): LowRamProfile {
    if (!shouldEscalateRuntimeLowRamProfile(trimLevel, availableMemoryBytes, memoryLow)) {
        return baseProfile
    }
    val availableMemoryMb = availableMemoryBytes
        ?.takeIf { it > 0L }
        ?.let { it / OneMegabyteBytes }
        ?: baseProfile.availableMemoryMb
    return baseProfile.copy(
        enabled = true,
        severe = true,
        ultra = true,
        availableMemoryMb = availableMemoryMb,
        memoryLow = baseProfile.memoryLow || memoryLow,
        qrMaxEdgePx = UltraQrMaxEdgePx,
        deferHeavyUi = true,
        slowPollingMultiplier = 6,
        diagnosticLogMaxEntries = UltraDiagnosticLogMaxEntries,
        manualRefreshCooldownMillis = UltraManualRefreshCooldownMillis,
        screenPinningSteadyPollMillis = UltraScreenPinningSteadyPollMillis,
        accessibilityLivenessPollMillis = UltraAccessibilityLivenessPollMillis,
        examServerProbeIntervalMillis = UltraExamServerProbeIntervalMillis,
        detectorMetadataCacheMaxEntries = UltraDetectorMetadataCacheMaxEntries,
        disableNonEssentialAnimations = true
    )
}

internal fun readLowRamProfileOverride(context: Context): LowRamProfileOverride {
    val rawValue = context.applicationContext
        .getSharedPreferences(AdminPreferencesName, Context.MODE_PRIVATE)
        .getString(AdminKeyLowRamProfileOverride, null)
    return parseLowRamProfileOverride(rawValue)
}

internal fun saveLowRamProfileOverride(
    context: Context,
    override: LowRamProfileOverride
) {
    context.applicationContext
        .getSharedPreferences(AdminPreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(AdminKeyLowRamProfileOverride, lowRamProfileOverrideToRaw(override))
        .apply()
}

internal fun resolveDetectedLowRamProfile(context: Context): LowRamProfile {
    val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)
    val memoryInfo = activityManager?.let {
        ActivityManager.MemoryInfo().also { info ->
            runCatching { activityManager.getMemoryInfo(info) }
        }
    }
    return calculateLowRamProfile(
        isLowRamDevice = activityManager?.isLowRamDevice == true,
        totalMemoryBytes = memoryInfo?.totalMem,
        memoryClassMb = activityManager?.memoryClass,
        availableMemoryBytes = memoryInfo?.availMem,
        memoryLow = memoryInfo?.lowMemory == true
    )
}

internal fun resolveEffectiveLowRamProfile(context: Context): LowRamProfile {
    return applyLowRamProfileOverride(
        detectedProfile = resolveDetectedLowRamProfile(context),
        override = readLowRamProfileOverride(context)
    )
}

internal fun resolveLowRamProfile(context: Context): LowRamProfile =
    resolveEffectiveLowRamProfile(context)

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

    fun latestTrimLevel(): Int? = lastTrimLevel

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

    @Suppress("DEPRECATION")
    fun shouldClearActiveWebViewCache(level: Int): Boolean {
        return level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE
    }
}
