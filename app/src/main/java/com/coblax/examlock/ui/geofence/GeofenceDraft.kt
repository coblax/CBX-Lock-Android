package com.coblax.examlock.ui.geofence

import com.coblax.examlock.GeofencePoint
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.isSelfIntersectingPolygon
import com.coblax.examlock.validatePolygonVertices
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt

internal const val MaxCircleCenters = 5

/**
 * The whole policy rides in one QR at error-correction level H. Next to a long exam URL
 * and an installer link that QR holds about 20 polygon points before it overflows (the
 * generator then throws); 15 keeps a margin and keeps the code easy for a camera to read.
 */
internal const val MaxPolygonPoints = 15

internal const val DefaultCircleRadiusMeters = 100

/** Phone GPS is often off by 10–30 m indoors; a smaller circle locks students out. */
internal const val MinCircleRadiusMeters = 20
internal const val MaxCircleRadiusMeters = 50_000

/** The radius slider covers the usual school sizes; the field accepts the rest. */
internal const val RadiusSliderMaxMeters = 2_000

internal fun maxGeofencePoints(shape: GeofenceShapeType): Int =
    if (shape == GeofenceShapeType.Polygon) MaxPolygonPoints else MaxCircleCenters

internal fun minGeofencePoints(shape: GeofenceShapeType): Int =
    if (shape == GeofenceShapeType.Polygon) 3 else 1

/** Why a location draft cannot be saved yet, most fundamental first. */
internal enum class GeofenceDraftIssue {
    NeedPoints,
    InvalidPoint,
    TooManyPoints,
    InvalidRadius,
    RadiusTooSmall,
    RadiusTooLarge,
    PolygonTooSmall,
    PolygonCrossing
}

internal data class GeofenceCoordinate(val latitude: Double, val longitude: Double)

internal fun GeofenceVertex.toCoordinateOrNull(): GeofenceCoordinate? {
    val lat = latitude.trim().toDoubleOrNull() ?: return null
    val lng = longitude.trim().toDoubleOrNull() ?: return null
    if (!lat.isFinite() || !lng.isFinite() || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
        return null
    }
    return GeofenceCoordinate(lat, lng)
}

internal fun GeofenceCoordinate.toVertex(): GeofenceVertex = GeofenceVertex(
    latitude = formatCoordinateForPolicy(latitude),
    longitude = formatCoordinateForPolicy(longitude)
)

internal fun parseRadiusMeters(raw: String): Int? {
    val value = raw.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (!value.isFinite() || value <= 0.0) {
        return null
    }
    return value.roundToInt()
}

internal fun geofenceDraftIssue(
    shape: GeofenceShapeType,
    points: List<GeofenceVertex>,
    radiusText: String
): GeofenceDraftIssue? {
    if (points.size < minGeofencePoints(shape)) {
        return GeofenceDraftIssue.NeedPoints
    }
    if (points.any { it.toCoordinateOrNull() == null }) {
        return GeofenceDraftIssue.InvalidPoint
    }
    if (points.size > maxGeofencePoints(shape)) {
        return GeofenceDraftIssue.TooManyPoints
    }
    return when (shape) {
        GeofenceShapeType.Polygon -> when (validatePolygonVertices(points)) {
            null -> null
            "polygon_self_intersecting" -> GeofenceDraftIssue.PolygonCrossing
            // A crossed "bow tie" has zero area, so the validator calls it degenerate;
            // telling the admin the edges cross is what helps them fix it.
            "polygon_degenerate" -> if (crosses(points)) {
                GeofenceDraftIssue.PolygonCrossing
            } else {
                GeofenceDraftIssue.PolygonTooSmall
            }
            else -> GeofenceDraftIssue.InvalidPoint
        }
        else -> {
            val radius = parseRadiusMeters(radiusText)
            when {
                radius == null -> GeofenceDraftIssue.InvalidRadius
                radius < MinCircleRadiusMeters -> GeofenceDraftIssue.RadiusTooSmall
                radius > MaxCircleRadiusMeters -> GeofenceDraftIssue.RadiusTooLarge
                else -> null
            }
        }
    }
}

private fun crosses(points: List<GeofenceVertex>): Boolean {
    val coordinates = points.mapNotNull { it.toCoordinateOrNull() }
    return coordinates.size == points.size &&
        isSelfIntersectingPolygon(coordinates.map { GeofencePoint(it.latitude, it.longitude) })
}

/**
 * Reads a coordinate pair the way admins paste one: "-7.0251, 110.4171" from Google Maps,
 * a Maps link with "@-7.0251,110.4171,17z" or "q=-7.0251,110.4171" in it, or degrees,
 * minutes and seconds such as 7°01'30.4"S 110°25'01.6"E.
 */
internal fun parseCoordinateText(text: String): GeofenceCoordinate? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
        return null
    }
    parseDmsPair(trimmed)?.let { return it }
    DecimalPairPattern.findAll(trimmed).forEach { match ->
        val latText = match.groupValues[1]
        val lngText = match.groupValues[2]
        val lat = latText.toDoubleOrNull() ?: return@forEach
        val lng = lngText.toDoubleOrNull() ?: return@forEach
        val bothDecimal = '.' in latText && '.' in lngText
        val onlyPair = match.value.length == trimmed.length
        if ((bothDecimal || onlyPair) && lat in -90.0..90.0 && lng in -180.0..180.0) {
            return GeofenceCoordinate(lat, lng)
        }
    }
    return null
}

internal fun formatCoordinateText(vertex: GeofenceVertex): String {
    val lat = vertex.latitude.trim()
    val lng = vertex.longitude.trim()
    return if (lat.isEmpty() && lng.isEmpty()) "" else "$lat, $lng"
}

private val DecimalPairPattern =
    Regex("""(?<![\d.])([-+]?\d{1,3}(?:\.\d+)?)\s*[,;\s]\s*([-+]?\d{1,3}(?:\.\d+)?)(?![\d])""")

private val DmsPattern =
    Regex("""(\d{1,3})\s*°\s*(?:(\d{1,2})\s*['′]\s*)?(?:(\d{1,2}(?:\.\d+)?)\s*(?:"|″|''))?\s*([NSEWnsew])""")

private fun parseDmsPair(text: String): GeofenceCoordinate? {
    val parts = DmsPattern.findAll(text).toList()
    if (parts.size != 2) {
        return null
    }
    var lat: Double? = null
    var lng: Double? = null
    parts.forEach { match ->
        val degrees = match.groupValues[1].toDouble()
        val minutes = match.groupValues[2].toDoubleOrNull() ?: 0.0
        val seconds = match.groupValues[3].toDoubleOrNull() ?: 0.0
        val value = degrees + minutes / 60.0 + seconds / 3600.0
        when (match.groupValues[4].uppercase(Locale.US)) {
            "N" -> lat = value
            "S" -> lat = -value
            "E" -> lng = value
            "W" -> lng = -value
        }
    }
    val latitude = lat ?: return null
    val longitude = lng ?: return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        return null
    }
    return GeofenceCoordinate(latitude, longitude)
}

/**
 * Puts polygon points in order around their middle. Tapping corners out of order makes
 * the edges cross, which the runtime rejects; for the usual school yard this untangles it.
 */
internal fun orderPolygonAroundCenter(points: List<GeofenceVertex>): List<GeofenceVertex> {
    val coordinates = points.map { it.toCoordinateOrNull() }
    if (coordinates.any { it == null } || points.size < 3) {
        return points
    }
    val valid = coordinates.filterNotNull()
    val centerLat = valid.sumOf { it.latitude } / valid.size
    val centerLng = valid.sumOf { it.longitude } / valid.size
    val lngScale = cos(Math.toRadians(centerLat))
    return points.indices
        .sortedBy { index ->
            val point = valid[index]
            atan2(point.latitude - centerLat, (point.longitude - centerLng) * lngScale)
        }
        .map { points[it] }
}
