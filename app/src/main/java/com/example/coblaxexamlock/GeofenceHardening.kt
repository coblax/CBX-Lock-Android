package com.example.coblaxexamlock

import android.location.Location
import android.os.Build
import com.example.coblaxexamlock.nativebridge.NativeBridgeBackendMode
import com.example.coblaxexamlock.nativebridge.NativeBridgeTestControl
import com.example.coblaxexamlock.nativebridge.NativeGeofenceBridge


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

private data class ParsedGeofenceVertex(
    val point: GeofencePoint?,
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

internal fun parseGeofenceConfig(
    enabled: Boolean,
    centerLatRaw: String,
    centerLngRaw: String,
    radiusMetersRaw: String,
    shapeType: GeofenceShapeType = if (enabled) GeofenceShapeType.Circle else GeofenceShapeType.Disabled,
    polygonVertices: List<GeofenceVertex> = emptyList(),
    circleCenters: List<GeofenceVertex> = emptyList()
): GeofenceConfigParseResult {
    if (!enabled) {
        return GeofenceConfigParseResult(
            enabled = false,
            config = null,
            error = null
        )
    }

    if (shapeType == GeofenceShapeType.Polygon) {
        return parsePolygonGeofenceConfig(polygonVertices)
    }

    val parsedCircleCenters = if (circleCenters.isNotEmpty()) {
        circleCenters.take(5).map { vertex ->
            val parsed = parseGeofenceVertex(
                vertex = vertex,
                latitudeError = "invalid_latitude",
                longitudeError = "invalid_longitude"
            )
            parsed.point ?: return GeofenceConfigParseResult(
                enabled = true,
                config = null,
                error = parsed.error
            )
        }
    } else {
        emptyList()
    }

    val centerLat = if (parsedCircleCenters.isNotEmpty()) {
        parsedCircleCenters.first().latitude
    } else {
        centerLatRaw.trim().toDoubleOrNull()
            ?: return GeofenceConfigParseResult(enabled = true, config = null, error = "invalid_latitude")
    }
    val centerLng = if (parsedCircleCenters.isNotEmpty()) {
        parsedCircleCenters.first().longitude
    } else {
        centerLngRaw.trim().toDoubleOrNull()
            ?: return GeofenceConfigParseResult(enabled = true, config = null, error = "invalid_longitude")
    }
    val radiusMeters = radiusMetersRaw.trim().toDoubleOrNull()
        ?: return GeofenceConfigParseResult(enabled = true, config = null, error = "invalid_radius")

    if (!centerLat.isValidLatitude()) {
        return GeofenceConfigParseResult(enabled = true, config = null, error = "invalid_latitude")
    }
    if (!centerLng.isValidLongitude()) {
        return GeofenceConfigParseResult(enabled = true, config = null, error = "invalid_longitude")
    }
    if (!radiusMeters.isFinite() || radiusMeters <= 0.0) {
        return GeofenceConfigParseResult(enabled = true, config = null, error = "invalid_radius")
    }

    return GeofenceConfigParseResult(
        enabled = true,
        config = GeofenceConfig(
            shapeType = GeofenceShapeType.Circle,
            centerLat = parsedCircleCenters.firstOrNull()?.latitude ?: centerLat,
            centerLng = parsedCircleCenters.firstOrNull()?.longitude ?: centerLng,
            radiusMeters = radiusMeters,
            circleCenters = parsedCircleCenters.ifEmpty {
                listOf(
                    GeofencePoint(
                        latitude = centerLat,
                        longitude = centerLng
                    )
                )
            }
        ),
        error = null
    )
}

internal fun evaluateGeofence(
    configResult: GeofenceConfigParseResult,
    permissionGranted: Boolean,
    locationServicesEnabled: Boolean,
    locationSnapshot: LocationSnapshot?
): GeofenceEvaluation {
    val config = configResult.config
    val verdict = when {
        !configResult.enabled -> GeofenceVerdict.Disabled
        config == null -> GeofenceVerdict.ConfigInvalid
        !permissionGranted -> GeofenceVerdict.PermissionMissing
        !locationServicesEnabled -> GeofenceVerdict.LocationDisabled
        locationSnapshot == null -> GeofenceVerdict.NoFix
        isLocationInsideGeofence(config, locationSnapshot) -> GeofenceVerdict.Inside
        else -> GeofenceVerdict.Outside
    }
    val closestCircleCenter =
        if (config != null && locationSnapshot != null && config.shapeType == GeofenceShapeType.Circle) {
            findClosestCircleCenter(config, locationSnapshot)
        } else {
            null
        }

    return GeofenceEvaluation(
        enabled = configResult.enabled,
        config = config,
        configError = configResult.error,
        permissionGranted = permissionGranted,
        locationServicesEnabled = locationServicesEnabled,
        locationSnapshot = locationSnapshot,
        closestCircleCenter = closestCircleCenter,
        distanceMeters = if (config != null && locationSnapshot != null && config.shapeType == GeofenceShapeType.Circle) {
            calculateClosestCircleDistanceMeters(config, locationSnapshot)
        } else {
            null
        },
        verdict = verdict
    )
}

internal fun validatePolygonVertices(vertices: List<GeofenceVertex>): String? {
    if (vertices.size < 3) {
        return "polygon_min_3_vertices"
    }
    if (vertices.size > 50) {
        return "polygon_max_50_vertices"
    }
    val parsed = vertices.map { vertex ->
        val parsedVertex = parseGeofenceVertex(
            vertex = vertex,
            latitudeError = "invalid_polygon_latitude",
            longitudeError = "invalid_polygon_longitude"
        )
        parsedVertex.point ?: return parsedVertex.error
    }
    if (isDegeneratePolygon(parsed)) {
        return "polygon_degenerate"
    }
    if (isSelfIntersectingPolygon(parsed)) {
        return "polygon_self_intersecting"
    }
    return null
}

private fun parseGeofenceVertex(
    vertex: GeofenceVertex,
    latitudeError: String,
    longitudeError: String
): ParsedGeofenceVertex {
    val latitude = vertex.latitude.trim().toDoubleOrNull()
        ?: return ParsedGeofenceVertex(point = null, error = latitudeError)
    val longitude = vertex.longitude.trim().toDoubleOrNull()
        ?: return ParsedGeofenceVertex(point = null, error = longitudeError)
    if (!latitude.isValidLatitude()) {
        return ParsedGeofenceVertex(point = null, error = latitudeError)
    }
    if (!longitude.isValidLongitude()) {
        return ParsedGeofenceVertex(point = null, error = longitudeError)
    }
    return ParsedGeofenceVertex(
        point = GeofencePoint(latitude = latitude, longitude = longitude),
        error = null
    )
}

private fun Double.isValidLatitude(): Boolean = isFinite() && this in -90.0..90.0

private fun Double.isValidLongitude(): Boolean = isFinite() && this in -180.0..180.0

private fun isDegeneratePolygon(points: List<GeofencePoint>): Boolean {
    return kotlin.math.abs(polygonSignedArea(points)) <= MinPolygonAreaDegrees
}

private fun polygonSignedArea(points: List<GeofencePoint>): Double {
    if (points.size < 3) {
        return 0.0
    }
    var sum = 0.0
    for (index in points.indices) {
        val current = points[index]
        val next = points[(index + 1) % points.size]
        sum += current.longitude * next.latitude - next.longitude * current.latitude
    }
    return sum / 2.0
}

internal fun evaluateGeofenceSecurity(
    configResult: GeofenceConfigParseResult,
    permissionGranted: Boolean,
    preciseLocationGranted: Boolean,
    locationServicesEnabled: Boolean,
    locationSnapshot: LocationSnapshot?,
    bypassState: GeofenceBypassState
): GeofenceSecurityStatus {
    val fixQualityStatus = evaluateLocationFixQuality(
        locationSnapshot = locationSnapshot,
        config = configResult.config
    )
    val geofenceEvaluation = evaluateGeofence(
        configResult = configResult,
        permissionGranted = permissionGranted,
        locationServicesEnabled = locationServicesEnabled,
        locationSnapshot = locationSnapshot
    )

    val finalVerdict = when {
        bypassState == GeofenceBypassState.Active -> GeofenceSecurityVerdict.Bypassed
        !configResult.enabled -> GeofenceSecurityVerdict.Disabled
        geofenceEvaluation.config == null -> GeofenceSecurityVerdict.ConfigInvalid
        !permissionGranted -> GeofenceSecurityVerdict.PermissionMissing
        !preciseLocationGranted -> GeofenceSecurityVerdict.PreciseRequired
        !locationServicesEnabled -> GeofenceSecurityVerdict.LocationDisabled
        fixQualityStatus.verdict == LocationFixQualityVerdict.NoFix -> GeofenceSecurityVerdict.NoFix
        fixQualityStatus.verdict == LocationFixQualityVerdict.Stale -> GeofenceSecurityVerdict.StaleFix
        fixQualityStatus.verdict == LocationFixQualityVerdict.LowAccuracy -> GeofenceSecurityVerdict.LowAccuracy
        fixQualityStatus.verdict == LocationFixQualityVerdict.MissingAccuracy -> GeofenceSecurityVerdict.MissingAccuracy
        geofenceEvaluation.verdict == GeofenceVerdict.Outside -> GeofenceSecurityVerdict.Outside
        else -> GeofenceSecurityVerdict.Inside
    }

    return GeofenceSecurityStatus(
        geofenceEvaluation = geofenceEvaluation,
        bypassState = bypassState,
        preciseLocationGranted = preciseLocationGranted,
        fixQualityStatus = fixQualityStatus,
        finalVerdict = finalVerdict
    )
}

internal fun evaluateFakeLocationSecurity(
    monitoringEnabled: Boolean,
    permissionGranted: Boolean,
    locationServicesEnabled: Boolean,
    locationSnapshot: LocationSnapshot?,
    fixQualityStatus: LocationFixQualityStatus,
    developerOptionsEnabled: Boolean,
    suspiciousFakeLocationPackages: List<String>,
    bypassState: FakeLocationBypassState
): LocationSpoofSecurityStatus {
    val snapshotAvailable = locationSnapshot != null
    val mockLocationDetected = locationSnapshot?.isMock == true
    val supportingSignals = buildSet {
        if (mockLocationDetected) add(LocationSpoofSignal.MockLocationFlag)
        if (suspiciousFakeLocationPackages.isNotEmpty()) add(LocationSpoofSignal.SuspiciousPackageInstalled)
        if (developerOptionsEnabled) add(LocationSpoofSignal.DeveloperOptionsEnabled)
    }
    val confidenceTier = when {
        mockLocationDetected && suspiciousFakeLocationPackages.isNotEmpty() -> LocationSpoofConfidenceTier.Critical
        mockLocationDetected && developerOptionsEnabled -> LocationSpoofConfidenceTier.Critical
        mockLocationDetected -> LocationSpoofConfidenceTier.Strong
        suspiciousFakeLocationPackages.isNotEmpty() && developerOptionsEnabled -> LocationSpoofConfidenceTier.Strong
        suspiciousFakeLocationPackages.isNotEmpty() -> LocationSpoofConfidenceTier.Warning
        else -> LocationSpoofConfidenceTier.Safe
    }
    val finalVerdict = when {
        bypassState == FakeLocationBypassState.Active -> LocationSpoofSecurityVerdict.Bypassed
        !monitoringEnabled -> LocationSpoofSecurityVerdict.Disabled
        !permissionGranted -> LocationSpoofSecurityVerdict.PermissionRequired
        !locationServicesEnabled -> LocationSpoofSecurityVerdict.LocationServicesDisabled
        !snapshotAvailable -> LocationSpoofSecurityVerdict.LocationUnavailable
        confidenceTier == LocationSpoofConfidenceTier.Strong ||
            confidenceTier == LocationSpoofConfidenceTier.Critical -> LocationSpoofSecurityVerdict.SpoofDetected
        confidenceTier == LocationSpoofConfidenceTier.Warning -> LocationSpoofSecurityVerdict.PackageWarning
        else -> LocationSpoofSecurityVerdict.Safe
    }

    return LocationSpoofSecurityStatus(
        monitoringEnabled = monitoringEnabled,
        bypassState = bypassState,
        permissionGranted = permissionGranted,
        locationServicesEnabled = locationServicesEnabled,
        snapshotAvailable = snapshotAvailable,
        suspiciousFakeLocationPackages = suspiciousFakeLocationPackages,
        developerOptionsEnabled = developerOptionsEnabled,
        mockLocationDetected = mockLocationDetected,
        supportingSignals = supportingSignals,
        confidenceTier = confidenceTier,
        fixQualityStatus = fixQualityStatus,
        fixQualityEligible = fixQualityStatus.verdict == LocationFixQualityVerdict.Fresh,
        finalVerdict = finalVerdict
    )
}

internal fun evaluateLocationSecurity(
    configResult: GeofenceConfigParseResult,
    permissionGranted: Boolean,
    preciseLocationGranted: Boolean,
    locationServicesEnabled: Boolean,
    locationSnapshot: LocationSnapshot?,
    developerOptionsEnabled: Boolean,
    suspiciousFakeLocationPackages: List<String>,
    bypassState: LocationBypassState
): LocationSecurityStatus {
    val geofenceStatus = evaluateGeofenceSecurity(
        configResult = configResult,
        permissionGranted = permissionGranted,
        preciseLocationGranted = preciseLocationGranted,
        locationServicesEnabled = locationServicesEnabled,
        locationSnapshot = locationSnapshot,
        bypassState = bypassState
    )
    val approximateOnly = configResult.enabled && permissionGranted && !preciseLocationGranted
    val spoofStatus = evaluateFakeLocationSecurity(
        monitoringEnabled = true,
        permissionGranted = permissionGranted,
        locationServicesEnabled = locationServicesEnabled,
        locationSnapshot = locationSnapshot,
        fixQualityStatus = geofenceStatus.fixQualityStatus,
        developerOptionsEnabled = developerOptionsEnabled,
        suspiciousFakeLocationPackages = suspiciousFakeLocationPackages,
        bypassState = bypassState
    )
    val spoofSignals = buildSet {
        addAll(spoofStatus.supportingSignals)
        if (approximateOnly) add(LocationSpoofSignal.ApproximateOnly)
    }
    val finalVerdict = when {
        bypassState == LocationBypassState.Active -> LocationSecurityVerdict.Bypassed
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.Disabled -> LocationSecurityVerdict.Disabled
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> LocationSecurityVerdict.ConfigInvalid
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.PermissionMissing -> LocationSecurityVerdict.PermissionMissing
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.PreciseRequired -> LocationSecurityVerdict.PreciseRequired
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled -> LocationSecurityVerdict.LocationDisabled
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> LocationSecurityVerdict.NoFix
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> LocationSecurityVerdict.NoFix
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> LocationSecurityVerdict.NoFix
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> LocationSecurityVerdict.NoFix
        spoofStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired -> LocationSecurityVerdict.PermissionMissing
        spoofStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled -> LocationSecurityVerdict.LocationDisabled
        spoofStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable -> LocationSecurityVerdict.NoFix
        spoofStatus.finalVerdict == LocationSpoofSecurityVerdict.SpoofDetected -> LocationSecurityVerdict.SpoofDetected
        geofenceStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> LocationSecurityVerdict.Outside
        else -> LocationSecurityVerdict.Inside
    }

    return LocationSecurityStatus(
        geofenceEvaluation = geofenceStatus.geofenceEvaluation,
        bypassState = bypassState,
        preciseLocationGranted = preciseLocationGranted,
        suspiciousFakeLocationPackages = spoofStatus.suspiciousFakeLocationPackages,
        developerOptionsEnabled = spoofStatus.developerOptionsEnabled,
        mockLocationDetected = spoofStatus.mockLocationDetected,
        spoofSignals = spoofSignals,
        spoofVerdict = when (spoofStatus.confidenceTier) {
            LocationSpoofConfidenceTier.Warning -> LocationSpoofVerdict.PackageWarning
            LocationSpoofConfidenceTier.Strong, LocationSpoofConfidenceTier.Critical -> LocationSpoofVerdict.Strong
            LocationSpoofConfidenceTier.Safe -> LocationSpoofVerdict.None
        },
        finalVerdict = finalVerdict
    )
}

internal fun evaluateLocationFixQuality(
    locationSnapshot: LocationSnapshot?,
    config: GeofenceConfig? = null,
    nowMs: Long = System.currentTimeMillis()
): LocationFixQualityStatus {
    val accuracyThresholdMeters = resolveLocationFixAccuracyThresholdMeters(config)
    if (locationSnapshot == null) {
        return LocationFixQualityStatus(
            snapshot = null,
            ageMs = null,
            accuracyMeters = null,
            accuracyThresholdMeters = accuracyThresholdMeters,
            verdict = LocationFixQualityVerdict.NoFix
        )
    }

    val ageMs = (nowMs - locationSnapshot.fixTimestampMs).coerceAtLeast(0L)
    val accuracyMeters = locationSnapshot.accuracyMeters
    val boundaryMarginMeters = config?.let {
        calculateGeofenceBoundaryMarginMeters(
            config = it,
            locationSnapshot = locationSnapshot
        )
    }
    val verdict = when {
        ageMs > LocationFixFreshThresholdMillis -> LocationFixQualityVerdict.Stale
        accuracyMeters == null -> LocationFixQualityVerdict.MissingAccuracy
        accuracyMeters > accuracyThresholdMeters &&
            !isAccuracyAcceptableInsideBoundary(
                accuracyMeters = accuracyMeters,
                boundaryMarginMeters = boundaryMarginMeters
            ) -> LocationFixQualityVerdict.LowAccuracy
        else -> LocationFixQualityVerdict.Fresh
    }

    return LocationFixQualityStatus(
        snapshot = locationSnapshot,
        ageMs = ageMs,
        accuracyMeters = accuracyMeters,
        accuracyThresholdMeters = accuracyThresholdMeters,
        verdict = verdict
    )
}

private fun isAccuracyAcceptableInsideBoundary(
    accuracyMeters: Float,
    boundaryMarginMeters: Double?
): Boolean {
    if (boundaryMarginMeters == null || boundaryMarginMeters.isNaN()) {
        return false
    }
    return accuracyMeters <= boundaryMarginMeters + LocationFixInsideBoundaryGraceMeters
}

internal fun resolveLocationFixAccuracyThresholdMeters(
    config: GeofenceConfig?
): Float {
    if (config == null) {
        return BaseLocationFixMaxAccuracyMeters
    }
    val adaptiveThreshold = when (config.shapeType) {
        GeofenceShapeType.Disabled -> {
            BaseLocationFixMaxAccuracyMeters
        }
        GeofenceShapeType.Circle -> {
            (config.radiusMeters * 0.35).toFloat()
        }
        GeofenceShapeType.Polygon -> {
            val vertices = config.vertices
            if (vertices.size < 2) {
                BaseLocationFixMaxAccuracyMeters
            } else {
                val minLat = vertices.minOf { it.latitude }
                val maxLat = vertices.maxOf { it.latitude }
                val minLng = vertices.minOf { it.longitude }
                val maxLng = vertices.maxOf { it.longitude }
                val distanceResults = FloatArray(1)
                Location.distanceBetween(minLat, minLng, maxLat, maxLng, distanceResults)
                distanceResults[0] * 0.12f
            }
        }
    }
    return adaptiveThreshold.coerceIn(
        BaseLocationFixMaxAccuracyMeters,
        MaxAdaptiveLocationFixAccuracyMeters
    )
}

internal fun formatCoordinates(
    latitude: Double,
    longitude: Double
): String {
    return String.format(java.util.Locale.US, "%.6f, %.6f", latitude, longitude)
}

internal fun selectBestLocationSnapshot(
    locations: List<Location>
): LocationSnapshot? {
    val bestLocation = locations
        .filter { it.provider != null }
        .maxWithOrNull(locationPriorityComparator())
        ?: return null

    return bestLocation.toLocationSnapshot()
}

internal fun selectBestLocationSnapshotFromSnapshots(
    snapshots: List<LocationSnapshot>,
    preferFresh: Boolean = false
): LocationSnapshot? {
    val comparator = if (preferFresh) {
        compareBy<LocationSnapshot> { it.fixTimestampMs }
            .thenBy { if (it.accuracyMeters != null) 1 else 0 }
            .thenByDescending { it.accuracyMeters?.let { accuracy -> -accuracy } ?: Float.NEGATIVE_INFINITY }
    } else {
        compareBy<LocationSnapshot> { if (it.accuracyMeters != null) 1 else 0 }
            .thenByDescending { it.accuracyMeters?.let { accuracy -> -accuracy } ?: Float.NEGATIVE_INFINITY }
            .thenBy { it.fixTimestampMs }
    }
    return snapshots.maxWithOrNull(comparator)
}

internal fun selectPreferredGeofenceSnapshot(
    snapshots: List<LocationSnapshot>,
    config: GeofenceConfig?,
    preferFresh: Boolean
): LocationSnapshot? {
    val usableSnapshots = snapshots.filter { snapshot ->
        evaluateLocationFixQuality(
            locationSnapshot = snapshot,
            config = config
        ).usableForGeofence
    }
    val candidates = if (usableSnapshots.isNotEmpty()) usableSnapshots else snapshots
    return selectBestLocationSnapshotFromSnapshots(
        snapshots = candidates,
        preferFresh = preferFresh
    )
}

internal fun isMockLocation(location: Location): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        @Suppress("DEPRECATION")
        location.isFromMockProvider
    }
}

private fun parsePolygonGeofenceConfig(
    polygonVertices: List<GeofenceVertex>
): GeofenceConfigParseResult {
    validatePolygonVertices(polygonVertices)?.let { error ->
        return GeofenceConfigParseResult(enabled = true, config = null, error = error)
    }
    val parsedVertices = polygonVertices.map { vertex ->
        parseGeofenceVertex(
            vertex = vertex,
            latitudeError = "invalid_polygon_latitude",
            longitudeError = "invalid_polygon_longitude"
        ).point ?: return GeofenceConfigParseResult(
            enabled = true,
            config = null,
            error = "invalid_polygon_vertex"
        )
    }
    val centroidLat = parsedVertices.map { it.latitude }.average()
    val centroidLng = parsedVertices.map { it.longitude }.average()
    return GeofenceConfigParseResult(
        enabled = true,
        config = GeofenceConfig(
            shapeType = GeofenceShapeType.Polygon,
            centerLat = centroidLat,
            centerLng = centroidLng,
            radiusMeters = 0.0,
            vertices = parsedVertices,
            circleCenters = emptyList()
        ),
        error = null
    )
}

private fun isLocationInsideGeofence(
    config: GeofenceConfig,
    locationSnapshot: LocationSnapshot
): Boolean {
    return when (config.shapeType) {
        GeofenceShapeType.Circle ->
            calculateClosestCircleDistanceMeters(config, locationSnapshot) <= config.radiusMeters
        GeofenceShapeType.Polygon ->
            isPointInPolygon(
                point = GeofencePoint(
                    latitude = locationSnapshot.latitude,
                    longitude = locationSnapshot.longitude
                ),
                polygon = config.vertices
            )
        GeofenceShapeType.Disabled -> false
    }
}

internal fun isPointInPolygon(
    point: GeofencePoint,
    polygon: List<GeofencePoint>
): Boolean {
    return NativeGeofenceBridge.isPointInPolygon(point, polygon) {
        isPointInPolygonKotlin(point, polygon)
    }
}

private fun isPointInPolygonKotlin(
    point: GeofencePoint,
    polygon: List<GeofencePoint>
): Boolean {
    if (polygon.size < 3) {
        return false
    }
    var inside = false
    var previousIndex = polygon.lastIndex
    for (currentIndex in polygon.indices) {
        val current = polygon[currentIndex]
        val previous = polygon[previousIndex]
        if (isPointOnSegment(point, previous, current)) {
            return true
        }
        val intersects = ((current.latitude > point.latitude) != (previous.latitude > point.latitude)) &&
            (point.longitude < (previous.longitude - current.longitude) *
                (point.latitude - current.latitude) /
                (previous.latitude - current.latitude) + current.longitude)
        if (intersects) {
            inside = !inside
        }
        previousIndex = currentIndex
    }
    return inside
}

private fun isSelfIntersectingPolygon(points: List<GeofencePoint>): Boolean {
    return NativeGeofenceBridge.isSelfIntersectingPolygon(points) {
        isSelfIntersectingPolygonKotlin(points)
    }
}

private fun isSelfIntersectingPolygonKotlin(points: List<GeofencePoint>): Boolean {
    if (points.size < 4) {
        return false
    }
    for (firstIndex in points.indices) {
        val firstStart = points[firstIndex]
        val firstEnd = points[(firstIndex + 1) % points.size]
        for (secondIndex in firstIndex + 1 until points.size) {
            val adjacent =
                secondIndex == firstIndex ||
                    secondIndex == (firstIndex + 1) % points.size ||
                    firstIndex == (secondIndex + 1) % points.size
            if (adjacent) {
                continue
            }
            val secondStart = points[secondIndex]
            val secondEnd = points[(secondIndex + 1) % points.size]
            if (segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)) {
                return true
            }
        }
    }
    return false
}

private fun segmentsIntersect(
    firstStart: GeofencePoint,
    firstEnd: GeofencePoint,
    secondStart: GeofencePoint,
    secondEnd: GeofencePoint
): Boolean {
    val o1 = orientation(firstStart, firstEnd, secondStart)
    val o2 = orientation(firstStart, firstEnd, secondEnd)
    val o3 = orientation(secondStart, secondEnd, firstStart)
    val o4 = orientation(secondStart, secondEnd, firstEnd)

    if (o1 != o2 && o3 != o4) {
        return true
    }
    return (o1 == 0 && isPointOnSegment(secondStart, firstStart, firstEnd)) ||
        (o2 == 0 && isPointOnSegment(secondEnd, firstStart, firstEnd)) ||
        (o3 == 0 && isPointOnSegment(firstStart, secondStart, secondEnd)) ||
        (o4 == 0 && isPointOnSegment(firstEnd, secondStart, secondEnd))
}

private fun orientation(
    first: GeofencePoint,
    second: GeofencePoint,
    third: GeofencePoint
): Int {
    val value = (second.longitude - first.longitude) * (third.latitude - second.latitude) -
        (second.latitude - first.latitude) * (third.longitude - second.longitude)
    return when {
        kotlin.math.abs(value) < 1e-12 -> 0
        value > 0 -> 1
        else -> 2
    }
}

private fun isPointOnSegment(
    point: GeofencePoint,
    segmentStart: GeofencePoint,
    segmentEnd: GeofencePoint
): Boolean {
    val cross = (point.latitude - segmentStart.latitude) *
        (segmentEnd.longitude - segmentStart.longitude) -
        (point.longitude - segmentStart.longitude) *
        (segmentEnd.latitude - segmentStart.latitude)
    if (kotlin.math.abs(cross) > 1e-10) {
        return false
    }
    return point.latitude in minOf(segmentStart.latitude, segmentEnd.latitude) - 1e-10..
        maxOf(segmentStart.latitude, segmentEnd.latitude) + 1e-10 &&
        point.longitude in minOf(segmentStart.longitude, segmentEnd.longitude) - 1e-10..
        maxOf(segmentStart.longitude, segmentEnd.longitude) + 1e-10
}

private fun calculateDistanceMeters(
    center: GeofencePoint,
    locationSnapshot: LocationSnapshot
): Double {
    if (!center.latitude.isValidLatitude() ||
        !center.longitude.isValidLongitude() ||
        !locationSnapshot.latitude.isValidLatitude() ||
        !locationSnapshot.longitude.isValidLongitude()
    ) {
        return Double.NaN
    }
    val results = FloatArray(1)
    return runCatching {
        Location.distanceBetween(
            center.latitude,
            center.longitude,
            locationSnapshot.latitude,
            locationSnapshot.longitude,
            results
        )
        results.firstOrNull()?.toDouble() ?: Double.NaN
    }.getOrDefault(Double.NaN)
}

private fun calculateGeofenceBoundaryMarginMeters(
    config: GeofenceConfig,
    locationSnapshot: LocationSnapshot
): Double? {
    return when (config.shapeType) {
        GeofenceShapeType.Circle -> {
            val distanceToCenter = calculateClosestCircleDistanceMeters(config, locationSnapshot)
            if (distanceToCenter.isNaN() || distanceToCenter > config.radiusMeters) {
                null
            } else {
                (config.radiusMeters - distanceToCenter).coerceAtLeast(0.0)
            }
        }
        GeofenceShapeType.Polygon -> {
            val point = GeofencePoint(
                latitude = locationSnapshot.latitude,
                longitude = locationSnapshot.longitude
            )
            if (!isPointInPolygon(point, config.vertices) || config.vertices.size < 2) {
                null
            } else {
                config.vertices.indices.minOfOrNull { index ->
                    val start = config.vertices[index]
                    val end = config.vertices[(index + 1) % config.vertices.size]
                    distancePointToSegmentMeters(
                        point = point,
                        segmentStart = start,
                        segmentEnd = end
                    )
                }
            }
        }
        GeofenceShapeType.Disabled -> null
    }
}

private fun distancePointToSegmentMeters(
    point: GeofencePoint,
    segmentStart: GeofencePoint,
    segmentEnd: GeofencePoint
): Double {
    return NativeGeofenceBridge.distancePointToSegmentMeters(point, segmentStart, segmentEnd) {
        distancePointToSegmentMetersKotlin(point, segmentStart, segmentEnd)
    }
}

private fun distancePointToSegmentMetersKotlin(
    point: GeofencePoint,
    segmentStart: GeofencePoint,
    segmentEnd: GeofencePoint
): Double {
    val referenceLat = point.latitude
    val referenceLng = point.longitude
    val pointXY = point.toPlanarMeters(referenceLat, referenceLng)
    val startXY = segmentStart.toPlanarMeters(referenceLat, referenceLng)
    val endXY = segmentEnd.toPlanarMeters(referenceLat, referenceLng)
    val segmentDx = endXY.first - startXY.first
    val segmentDy = endXY.second - startXY.second
    val segmentLengthSquared = segmentDx * segmentDx + segmentDy * segmentDy
    if (segmentLengthSquared <= 1e-6) {
        val dx = pointXY.first - startXY.first
        val dy = pointXY.second - startXY.second
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    val projection = ((pointXY.first - startXY.first) * segmentDx +
        (pointXY.second - startXY.second) * segmentDy) / segmentLengthSquared
    val clampedProjection = projection.coerceIn(0.0, 1.0)
    val projectedX = startXY.first + clampedProjection * segmentDx
    val projectedY = startXY.second + clampedProjection * segmentDy
    val dx = pointXY.first - projectedX
    val dy = pointXY.second - projectedY
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun GeofencePoint.toPlanarMeters(
    referenceLat: Double,
    referenceLng: Double
): Pair<Double, Double> {
    val earthRadiusMeters = 6_371_000.0
    val latitudeRadians = Math.toRadians(latitude)
    val referenceLatRadians = Math.toRadians(referenceLat)
    val deltaLatRadians = latitudeRadians - referenceLatRadians
    val deltaLngRadians = Math.toRadians(longitude - referenceLng)
    val x = deltaLngRadians * kotlin.math.cos((latitudeRadians + referenceLatRadians) / 2.0) * earthRadiusMeters
    val y = deltaLatRadians * earthRadiusMeters
    return x to y
}

private fun calculateClosestCircleDistanceMeters(
    config: GeofenceConfig,
    locationSnapshot: LocationSnapshot
): Double {
    val centers = config.circleCenters.ifEmpty {
        listOf(
            GeofencePoint(
                latitude = config.centerLat,
                longitude = config.centerLng
            )
        )
    }
    return NativeGeofenceBridge.closestCircleDistanceMeters(
        locationLat = locationSnapshot.latitude,
        locationLng = locationSnapshot.longitude,
        centers = centers
    ) {
        calculateClosestCircleDistanceMetersKotlin(
            locationSnapshot = locationSnapshot,
            centers = centers
        )
    }
}

private fun calculateClosestCircleDistanceMetersKotlin(
    locationSnapshot: LocationSnapshot,
    centers: List<GeofencePoint>
): Double {
    return centers.minOfOrNull { center ->
        calculateDistanceMeters(center, locationSnapshot)
    } ?: Double.NaN
}

internal object GeofenceParityAccess {
    fun isPointInPolygonWithBackend(
        point: GeofencePoint,
        polygon: List<GeofencePoint>,
        backendMode: NativeBridgeBackendMode
    ): Boolean = NativeBridgeTestControl.withBackendMode(backendMode) {
        isPointInPolygon(point, polygon)
    }

    fun isPointInPolygonReference(
        point: GeofencePoint,
        polygon: List<GeofencePoint>
    ): Boolean = isPointInPolygonKotlin(point, polygon)

    fun isSelfIntersectingWithBackend(
        polygon: List<GeofencePoint>,
        backendMode: NativeBridgeBackendMode
    ): Boolean = NativeBridgeTestControl.withBackendMode(backendMode) {
        isSelfIntersectingPolygon(polygon)
    }

    fun isSelfIntersectingReference(
        polygon: List<GeofencePoint>
    ): Boolean = isSelfIntersectingPolygonKotlin(polygon)

    fun distancePointToSegmentMetersWithBackend(
        point: GeofencePoint,
        segmentStart: GeofencePoint,
        segmentEnd: GeofencePoint,
        backendMode: NativeBridgeBackendMode
    ): Double = NativeBridgeTestControl.withBackendMode(backendMode) {
        distancePointToSegmentMeters(point, segmentStart, segmentEnd)
    }

    fun distancePointToSegmentMetersReference(
        point: GeofencePoint,
        segmentStart: GeofencePoint,
        segmentEnd: GeofencePoint
    ): Double = distancePointToSegmentMetersKotlin(point, segmentStart, segmentEnd)

    fun closestCircleDistanceMetersWithBackend(
        locationSnapshot: LocationSnapshot,
        centers: List<GeofencePoint>,
        backendMode: NativeBridgeBackendMode
    ): Double = NativeBridgeTestControl.withBackendMode(backendMode) {
        NativeGeofenceBridge.closestCircleDistanceMeters(
            locationLat = locationSnapshot.latitude,
            locationLng = locationSnapshot.longitude,
            centers = centers
        ) {
            calculateClosestCircleDistanceMetersKotlin(locationSnapshot, centers)
        }
    }

    fun closestCircleDistanceMetersReference(
        locationSnapshot: LocationSnapshot,
        centers: List<GeofencePoint>
    ): Double = calculateClosestCircleDistanceMetersKotlin(locationSnapshot, centers)
}

private fun findClosestCircleCenter(
    config: GeofenceConfig,
    locationSnapshot: LocationSnapshot
): GeofencePoint? {
    val centers = config.circleCenters.ifEmpty {
        listOf(
            GeofencePoint(
                latitude = config.centerLat,
                longitude = config.centerLng
            )
        )
    }
    return centers.minByOrNull { center ->
        calculateDistanceMeters(center, locationSnapshot)
    }
}

internal fun Location.toLocationSnapshot(): LocationSnapshot {
    return LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        provider = provider.orEmpty().ifBlank { "-" },
        fixTimestampMs = time,
        isMock = isMockLocation(this)
    )
}

private fun locationPriorityComparator(): Comparator<Location> {
    return compareBy<Location> { if (it.hasAccuracy()) 1 else 0 }
        .thenByDescending { if (it.hasAccuracy()) -it.accuracy else Float.NEGATIVE_INFINITY }
        .thenBy { it.time }
}
