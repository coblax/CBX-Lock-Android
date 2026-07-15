package com.coblax.examlock

internal enum class LocationBypassState {
    Active,
    Inactive,
    Tampered
}

internal object LocationBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): LocationBypassState {
        return when {
            tampered -> LocationBypassState.Tampered
            enabled -> LocationBypassState.Active
            else -> LocationBypassState.Inactive
        }
    }
}

internal typealias GeofenceBypassState = LocationBypassState
internal typealias FakeLocationBypassState = LocationBypassState

internal object GeofenceBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): GeofenceBypassState {
        return LocationBypassResolver.stateOf(enabled = enabled, tampered = tampered)
    }
}

internal object FakeLocationBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): FakeLocationBypassState {
        return LocationBypassResolver.stateOf(enabled = enabled, tampered = tampered)
    }
}

enum class LocationPolicySource {
    CustomQr,
    DirectLinkSaved,
    DisabledNoPolicy,
    Bypassed
}

internal fun LocationPolicySource.diagnosticLabel(): String {
    return when (this) {
        LocationPolicySource.CustomQr -> "custom_qr"
        LocationPolicySource.DirectLinkSaved -> "direct_link_saved"
        LocationPolicySource.DisabledNoPolicy -> "disabled_no_policy"
        LocationPolicySource.Bypassed -> "bypassed"
    }
}

internal data class GeofenceConfig(
    val shapeType: GeofenceShapeType,
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Double,
    val vertices: List<GeofencePoint> = emptyList(),
    val circleCenters: List<GeofencePoint> = emptyList()
)

internal data class GeofencePoint(
    val latitude: Double,
    val longitude: Double
)

internal data class GeofenceConfigParseResult(
    val enabled: Boolean,
    val config: GeofenceConfig?,
    val error: String?
)

internal enum class GeofenceVerdict {
    Disabled,
    Inside,
    Outside,
    PermissionMissing,
    LocationDisabled,
    NoFix,
    ConfigInvalid
}

internal fun GeofenceVerdict.diagnosticLabel(): String {
    return when (this) {
        GeofenceVerdict.Disabled -> "disabled"
        GeofenceVerdict.Inside -> "inside"
        GeofenceVerdict.Outside -> "outside"
        GeofenceVerdict.PermissionMissing -> "permission_missing"
        GeofenceVerdict.LocationDisabled -> "location_disabled"
        GeofenceVerdict.NoFix -> "no_fix"
        GeofenceVerdict.ConfigInvalid -> "config_invalid"
    }
}

internal data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val provider: String,
    val fixTimestampMs: Long,
    val isMock: Boolean
)

internal const val LocationFixFreshThresholdMillis = 45_000L
internal const val BaseLocationFixMaxAccuracyMeters = 100f
internal const val MaxAdaptiveLocationFixAccuracyMeters = 250f
private const val LocationFixInsideBoundaryGraceMeters = 20f
private const val MinPolygonAreaDegrees = 1.0e-12

internal enum class LocationFixQualityVerdict {
    Fresh,
    Stale,
    LowAccuracy,
    MissingAccuracy,
    NoFix
}

internal fun LocationFixQualityVerdict.diagnosticLabel(): String {
    return when (this) {
        LocationFixQualityVerdict.Fresh -> "fresh"
        LocationFixQualityVerdict.Stale -> "stale"
        LocationFixQualityVerdict.LowAccuracy -> "low_accuracy"
        LocationFixQualityVerdict.MissingAccuracy -> "missing_accuracy"
        LocationFixQualityVerdict.NoFix -> "no_fix"
    }
}

internal data class LocationFixQualityStatus(
    val snapshot: LocationSnapshot?,
    val ageMs: Long?,
    val accuracyMeters: Float?,
    val accuracyThresholdMeters: Float,
    val verdict: LocationFixQualityVerdict
) {
    val usableForGeofence: Boolean
        get() = verdict == LocationFixQualityVerdict.Fresh
}

internal data class GeofenceEvaluation(
    val enabled: Boolean,
    val config: GeofenceConfig?,
    val configError: String?,
    val permissionGranted: Boolean,
    val locationServicesEnabled: Boolean,
    val locationSnapshot: LocationSnapshot?,
    val closestCircleCenter: GeofencePoint?,
    val distanceMeters: Double?,
    val verdict: GeofenceVerdict
) {
    val inside: Boolean
        get() = verdict == GeofenceVerdict.Inside

    val blocking: Boolean
        get() = enabled && verdict != GeofenceVerdict.Inside
}

internal enum class LocationSpoofSignal {
    MockLocationFlag,
    SuspiciousPackageInstalled,
    DeveloperOptionsEnabled,
    ApproximateOnly
}

internal fun LocationSpoofSignal.diagnosticLabel(): String {
    return when (this) {
        LocationSpoofSignal.MockLocationFlag -> "mock_location_flag"
        LocationSpoofSignal.SuspiciousPackageInstalled -> "suspicious_package_installed"
        LocationSpoofSignal.DeveloperOptionsEnabled -> "developer_options_enabled"
        LocationSpoofSignal.ApproximateOnly -> "approximate_only"
    }
}

internal enum class LocationSpoofConfidenceTier {
    Safe,
    Warning,
    Strong,
    Critical
}

internal fun LocationSpoofConfidenceTier.diagnosticLabel(): String {
    return when (this) {
        LocationSpoofConfidenceTier.Safe -> "safe"
        LocationSpoofConfidenceTier.Warning -> "warning"
        LocationSpoofConfidenceTier.Strong -> "strong"
        LocationSpoofConfidenceTier.Critical -> "critical"
    }
}

internal enum class LocationSpoofVerdict {
    None,
    PackageWarning,
    Strong
}

internal fun LocationSpoofVerdict.diagnosticLabel(): String {
    return when (this) {
        LocationSpoofVerdict.None -> "none"
        LocationSpoofVerdict.PackageWarning -> "package_warning"
        LocationSpoofVerdict.Strong -> "strong"
    }
}

internal enum class LocationSecurityVerdict {
    Bypassed,
    Disabled,
    Inside,
    Outside,
    PermissionMissing,
    PreciseRequired,
    LocationDisabled,
    NoFix,
    ConfigInvalid,
    SpoofDetected
}

internal fun LocationSecurityVerdict.diagnosticLabel(): String {
    return when (this) {
        LocationSecurityVerdict.Bypassed -> "bypassed"
        LocationSecurityVerdict.Disabled -> "disabled"
        LocationSecurityVerdict.Inside -> "inside"
        LocationSecurityVerdict.Outside -> "outside"
        LocationSecurityVerdict.PermissionMissing -> "permission_missing"
        LocationSecurityVerdict.PreciseRequired -> "precise_required"
        LocationSecurityVerdict.LocationDisabled -> "location_disabled"
        LocationSecurityVerdict.NoFix -> "no_fix"
        LocationSecurityVerdict.ConfigInvalid -> "config_invalid"
        LocationSecurityVerdict.SpoofDetected -> "spoof_detected"
    }
}

internal data class LocationSecurityStatus(
    val geofenceEvaluation: GeofenceEvaluation,
    val bypassState: LocationBypassState,
    val preciseLocationGranted: Boolean,
    val suspiciousFakeLocationPackages: List<String>,
    val developerOptionsEnabled: Boolean,
    val mockLocationDetected: Boolean,
    val spoofSignals: Set<LocationSpoofSignal>,
    val spoofVerdict: LocationSpoofVerdict,
    val finalVerdict: LocationSecurityVerdict
) {
    val blocking: Boolean
        get() = finalVerdict !in setOf(
            LocationSecurityVerdict.Bypassed,
            LocationSecurityVerdict.Disabled,
            LocationSecurityVerdict.Inside
        )

    val warningOnly: Boolean
        get() = !blocking && spoofVerdict == LocationSpoofVerdict.PackageWarning

    val safe: Boolean
        get() = !blocking

    val reasonLabel: String
        get() = when (finalVerdict) {
            LocationSecurityVerdict.Outside -> "outside_area"
            LocationSecurityVerdict.PermissionMissing,
            LocationSecurityVerdict.LocationDisabled,
            LocationSecurityVerdict.NoFix,
            LocationSecurityVerdict.ConfigInvalid -> "location_unavailable"
            LocationSecurityVerdict.PreciseRequired -> "precise_required"
            LocationSecurityVerdict.SpoofDetected -> "mock_location_detected"
            LocationSecurityVerdict.Bypassed -> "bypassed"
            LocationSecurityVerdict.Disabled -> "disabled"
            LocationSecurityVerdict.Inside -> "inside"
        }
}

internal data class GeofenceRuntimeStatus(
    val evaluation: GeofenceEvaluation,
    val securityStatus: GeofenceSecurityStatus,
    val policySource: LocationPolicySource,
    val violationCount: Int,
    val lastTrigger: String?,
    val lastDetectedAt: String?,
    val lastContext: String?
)

internal enum class GeofenceSecurityVerdict {
    Bypassed,
    Disabled,
    Inside,
    Outside,
    StaleFix,
    LowAccuracy,
    MissingAccuracy,
    PermissionMissing,
    PreciseRequired,
    LocationDisabled,
    NoFix,
    ConfigInvalid
}

internal fun GeofenceSecurityVerdict.diagnosticLabel(): String {
    return when (this) {
        GeofenceSecurityVerdict.Bypassed -> "bypassed"
        GeofenceSecurityVerdict.Disabled -> "disabled"
        GeofenceSecurityVerdict.Inside -> "inside"
        GeofenceSecurityVerdict.Outside -> "outside"
        GeofenceSecurityVerdict.StaleFix -> "stale_fix"
        GeofenceSecurityVerdict.LowAccuracy -> "low_accuracy"
        GeofenceSecurityVerdict.MissingAccuracy -> "missing_accuracy"
        GeofenceSecurityVerdict.PermissionMissing -> "permission_missing"
        GeofenceSecurityVerdict.PreciseRequired -> "precise_required"
        GeofenceSecurityVerdict.LocationDisabled -> "location_disabled"
        GeofenceSecurityVerdict.NoFix -> "no_fix"
        GeofenceSecurityVerdict.ConfigInvalid -> "config_invalid"
    }
}

internal data class GeofenceSecurityStatus(
    val geofenceEvaluation: GeofenceEvaluation,
    val bypassState: GeofenceBypassState,
    val preciseLocationGranted: Boolean,
    val fixQualityStatus: LocationFixQualityStatus,
    val finalVerdict: GeofenceSecurityVerdict
) {
    val blocking: Boolean
        get() = finalVerdict !in setOf(
            GeofenceSecurityVerdict.Bypassed,
            GeofenceSecurityVerdict.Disabled,
            GeofenceSecurityVerdict.Inside
        )

    val safe: Boolean
        get() = !blocking

    val reasonLabel: String
        get() = when (finalVerdict) {
            GeofenceSecurityVerdict.Outside -> "outside_area"
            GeofenceSecurityVerdict.StaleFix -> "stale_fix"
            GeofenceSecurityVerdict.LowAccuracy -> "low_accuracy"
            GeofenceSecurityVerdict.MissingAccuracy -> "missing_accuracy"
            GeofenceSecurityVerdict.PermissionMissing,
            GeofenceSecurityVerdict.LocationDisabled,
            GeofenceSecurityVerdict.NoFix,
            GeofenceSecurityVerdict.ConfigInvalid -> "location_unavailable"
            GeofenceSecurityVerdict.PreciseRequired -> "precise_required"
            GeofenceSecurityVerdict.Bypassed -> "bypassed"
            GeofenceSecurityVerdict.Disabled -> "disabled"
            GeofenceSecurityVerdict.Inside -> "inside"
        }
}

internal enum class LocationSpoofSecurityVerdict {
    Bypassed,
    Disabled,
    PermissionRequired,
    LocationServicesDisabled,
    LocationUnavailable,
    Safe,
    PackageWarning,
    SpoofDetected
}

internal fun LocationSpoofSecurityVerdict.diagnosticLabel(): String {
    return when (this) {
        LocationSpoofSecurityVerdict.Bypassed -> "bypassed"
        LocationSpoofSecurityVerdict.Disabled -> "disabled"
        LocationSpoofSecurityVerdict.PermissionRequired -> "permission_required"
        LocationSpoofSecurityVerdict.LocationServicesDisabled -> "location_services_disabled"
        LocationSpoofSecurityVerdict.LocationUnavailable -> "location_unavailable"
        LocationSpoofSecurityVerdict.Safe -> "safe"
        LocationSpoofSecurityVerdict.PackageWarning -> "package_warning"
        LocationSpoofSecurityVerdict.SpoofDetected -> "spoof_detected"
    }
}

internal data class LocationSpoofSecurityStatus(
    val monitoringEnabled: Boolean,
    val bypassState: FakeLocationBypassState,
    val permissionGranted: Boolean,
    val locationServicesEnabled: Boolean,
    val snapshotAvailable: Boolean,
    val suspiciousFakeLocationPackages: List<String>,
    val developerOptionsEnabled: Boolean,
    val mockLocationDetected: Boolean,
    val supportingSignals: Set<LocationSpoofSignal>,
    val confidenceTier: LocationSpoofConfidenceTier,
    val fixQualityStatus: LocationFixQualityStatus,
    val fixQualityEligible: Boolean,
    val finalVerdict: LocationSpoofSecurityVerdict
) {
    val blocking: Boolean
        get() = finalVerdict in setOf(
            LocationSpoofSecurityVerdict.PermissionRequired,
            LocationSpoofSecurityVerdict.LocationServicesDisabled,
            LocationSpoofSecurityVerdict.LocationUnavailable,
            LocationSpoofSecurityVerdict.SpoofDetected
        )

    val warningOnly: Boolean
        get() = finalVerdict == LocationSpoofSecurityVerdict.PackageWarning

    val safe: Boolean
        get() = !blocking

    val reasonLabel: String
        get() = when (finalVerdict) {
            LocationSpoofSecurityVerdict.PermissionRequired -> "location_permission_required"
            LocationSpoofSecurityVerdict.LocationServicesDisabled -> "location_services_required"
            LocationSpoofSecurityVerdict.LocationUnavailable -> "location_snapshot_unavailable"
            LocationSpoofSecurityVerdict.SpoofDetected -> when (confidenceTier) {
                LocationSpoofConfidenceTier.Critical -> "mock_location_detected_critical"
                else -> "mock_location_detected_strong"
            }
            LocationSpoofSecurityVerdict.PackageWarning -> "package_warning"
            LocationSpoofSecurityVerdict.Bypassed -> "bypassed"
            LocationSpoofSecurityVerdict.Disabled -> "disabled"
            LocationSpoofSecurityVerdict.Safe -> "safe"
        }
}

internal data class FakeLocationRuntimeStatus(
    val securityStatus: LocationSpoofSecurityStatus,
    val violationCount: Int,
    val lastTrigger: String?,
    val lastDetectedAt: String?,
    val lastContext: String?
)

internal data class SplitLocationSecurityStatus(
    val geofenceStatus: GeofenceSecurityStatus,
    val fakeLocationStatus: LocationSpoofSecurityStatus
)
