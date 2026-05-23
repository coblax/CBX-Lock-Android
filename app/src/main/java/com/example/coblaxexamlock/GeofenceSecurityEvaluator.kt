package com.example.coblaxexamlock

import android.location.Location
import android.os.Build

private const val LocationFixInsideBoundaryGraceMeters = 20f

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
