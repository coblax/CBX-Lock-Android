package com.example.coblaxexamlock

import android.location.Location
import com.example.coblaxexamlock.nativebridge.NativeBridgeBackendMode
import com.example.coblaxexamlock.nativebridge.NativeBridgeTestControl
import com.example.coblaxexamlock.nativebridge.NativeGeofenceBridge

private const val MinPolygonAreaDegrees = 1.0e-12

private fun Double.isValidLatitude(): Boolean = isFinite() && this in -90.0..90.0

private fun Double.isValidLongitude(): Boolean = isFinite() && this in -180.0..180.0

internal fun isDegeneratePolygon(points: List<GeofencePoint>): Boolean {
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
internal fun isLocationInsideGeofence(
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

internal fun isSelfIntersectingPolygon(points: List<GeofencePoint>): Boolean {
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

internal fun calculateGeofenceBoundaryMarginMeters(
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

internal fun calculateClosestCircleDistanceMeters(
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

internal fun findClosestCircleCenter(
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
