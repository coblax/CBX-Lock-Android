package com.coblax.examlock

import android.os.Build
import java.util.Locale

private const val DefaultOverlayFocusLossConfirmWindowMillis = 1_500L
private const val DefaultOverlayChromeActionSuppressionMillis = 800L
private const val DefaultScreenPinningLostConfirmWindowMillis = 2_000L
private const val SamsungLegacyOverlayFocusLossConfirmWindowMillis = 1_500L
private const val SamsungLegacyOverlayChromeActionSuppressionMillis = 3_000L
private const val SamsungLegacyScreenPinningLostConfirmWindowMillis = 7_000L

internal enum class DeviceCompatibilityFamily {
    SamsungLegacyTablet,
    SamsungModern,
    XiaomiFamily,
    OppoRealme,
    VivoIqoo,
    Generic
}

internal enum class OverlayPartialObscuredPolicy {
    Block,
    WarnAndAllow
}

internal data class DeviceCompatibilityProfile(
    val manufacturer: String = "unknown",
    val brand: String = "unknown",
    val model: String = "unknown",
    val sdkInt: Int = 0,
    val family: DeviceCompatibilityFamily = DeviceCompatibilityFamily.Generic,
    val lowRamProfile: LowRamProfile = LowRamProfile(),
    val skipScreenPinningRequestWhenAlreadyActive: Boolean = true,
    val overlayFocusLossConfirmWindowMillis: Long = DefaultOverlayFocusLossConfirmWindowMillis,
    val overlayChromeActionSuppressionMillis: Long = DefaultOverlayChromeActionSuppressionMillis,
    val screenPinningLostConfirmWindowMillis: Long = DefaultScreenPinningLostConfirmWindowMillis,
    val partialObscuredWebViewPolicy: OverlayPartialObscuredPolicy = OverlayPartialObscuredPolicy.WarnAndAllow,
    val manualFirstGeofenceEditor: Boolean = lowRamProfile.severe,
    val useLightweightPreparationUi: Boolean = lowRamProfile.enabled
) {
    val samsungLegacyTablet: Boolean
        get() = family == DeviceCompatibilityFamily.SamsungLegacyTablet

    val allowPartialObscuredWebViewTouch: Boolean
        get() = partialObscuredWebViewPolicy == OverlayPartialObscuredPolicy.WarnAndAllow

    val vendorDisplayName: String
        get() = when (family) {
            DeviceCompatibilityFamily.SamsungLegacyTablet -> "Samsung legacy tablet"
            DeviceCompatibilityFamily.SamsungModern -> "Samsung"
            DeviceCompatibilityFamily.XiaomiFamily -> "Xiaomi / Redmi / POCO"
            DeviceCompatibilityFamily.OppoRealme -> "Oppo / Realme"
            DeviceCompatibilityFamily.VivoIqoo -> "Vivo / iQOO"
            DeviceCompatibilityFamily.Generic -> "Android"
        }

    fun diagnosticSummary(): String = buildString {
        append("family="); append(family.name)
        append(" | manufacturer="); append(manufacturer)
        append(" | brand="); append(brand)
        append(" | model="); append(model)
        append(" | sdk="); append(sdkInt)
        append(" | low_ram="); append(lowRamProfile.enabled)
        append(" | severe="); append(lowRamProfile.severe)
        append(" | ultra="); append(lowRamProfile.ultra)
        append(" | low_ram_override="); append(lowRamProfile.lowRamOverride.name)
        append(" | detected_profile="); append(lowRamProfile.detectedTier?.name ?: lowRamProfile.tier.name)
        append(" | effective_profile="); append(lowRamProfile.tier.name)
        append(" | total_ram_mb="); append(lowRamProfile.totalMemoryMb ?: "-")
        append(" | available_ram_mb="); append(lowRamProfile.availableMemoryMb ?: "-")
        append(" | memory_low="); append(lowRamProfile.memoryLow)
        append(" | skip_pinning_if_active="); append(skipScreenPinningRequestWhenAlreadyActive)
        append(" | partial_overlay="); append(partialObscuredWebViewPolicy.name.lowercase(Locale.US))
    }
}

internal fun currentDeviceCompatibilityProfile(
    lowRamProfile: LowRamProfile
): DeviceCompatibilityProfile {
    return resolveDeviceCompatibilityProfile(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        sdkInt = Build.VERSION.SDK_INT,
        lowRamProfile = lowRamProfile
    )
}

internal fun resolveDeviceCompatibilityProfile(
    manufacturer: String?,
    brand: String?,
    model: String?,
    sdkInt: Int,
    lowRamProfile: LowRamProfile = LowRamProfile()
): DeviceCompatibilityProfile {
    val normalizedManufacturer = manufacturer.normalizedDeviceToken()
    val normalizedBrand = brand.normalizedDeviceToken()
    val normalizedModel = model.normalizedDeviceToken()
    val key = listOf(normalizedManufacturer, normalizedBrand, normalizedModel)
        .joinToString(" ")
        .trim()
    val isSamsung = normalizedManufacturer == "samsung" || normalizedBrand == "samsung"
    val samsungLegacyTablet =
        isSamsung &&
            (
                normalizedModel == "sm-t295" ||
                    (normalizedModel.startsWith("sm-t") && sdkInt <= 30)
                )

    val family = when {
        samsungLegacyTablet -> DeviceCompatibilityFamily.SamsungLegacyTablet
        isSamsung -> DeviceCompatibilityFamily.SamsungModern
        key.contains("xiaomi") || key.contains("redmi") || key.contains("poco") ->
            DeviceCompatibilityFamily.XiaomiFamily
        key.contains("oppo") || key.contains("realme") ->
            DeviceCompatibilityFamily.OppoRealme
        key.contains("vivo") || key.contains("iqoo") ->
            DeviceCompatibilityFamily.VivoIqoo
        else -> DeviceCompatibilityFamily.Generic
    }

    return DeviceCompatibilityProfile(
        manufacturer = normalizedManufacturer.ifBlank { "unknown" },
        brand = normalizedBrand.ifBlank { "unknown" },
        model = normalizedModel.ifBlank { "unknown" },
        sdkInt = sdkInt,
        family = family,
        lowRamProfile = lowRamProfile,
        overlayFocusLossConfirmWindowMillis = if (samsungLegacyTablet) {
            SamsungLegacyOverlayFocusLossConfirmWindowMillis
        } else {
            DefaultOverlayFocusLossConfirmWindowMillis
        },
        overlayChromeActionSuppressionMillis = if (samsungLegacyTablet) {
            SamsungLegacyOverlayChromeActionSuppressionMillis
        } else {
            DefaultOverlayChromeActionSuppressionMillis
        },
        screenPinningLostConfirmWindowMillis = if (samsungLegacyTablet) {
            SamsungLegacyScreenPinningLostConfirmWindowMillis
        } else {
            DefaultScreenPinningLostConfirmWindowMillis
        },
        partialObscuredWebViewPolicy = OverlayPartialObscuredPolicy.WarnAndAllow,
        manualFirstGeofenceEditor = lowRamProfile.severe || samsungLegacyTablet,
        useLightweightPreparationUi = lowRamProfile.enabled
    )
}

private fun String?.normalizedDeviceToken(): String {
    return orEmpty()
        .trim()
        .lowercase(Locale.US)
}
