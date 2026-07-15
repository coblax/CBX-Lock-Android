package com.coblax.examlock.ui.geofence

import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

import com.coblax.examlock.formatCoordinates
import com.coblax.examlock.GeofenceConfig
import com.coblax.examlock.GeofencePoint
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.i18n.tr
import com.google.android.gms.maps.model.LatLng

import java.util.Locale

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

internal fun formatCoordinateForPolicy(value: Double): String {
    return String.format(Locale.US, "%.7f", value)
}

internal fun summarizePolygonVertices(vertices: List<GeofencePoint>): String {
    if (vertices.isEmpty()) {
        return "-"
    }
    val preview = vertices.take(4).joinToString("; ") { point ->
        formatCoordinates(point.latitude, point.longitude)
    }
    return if (vertices.size > 4) {
        "$preview; ... +${vertices.size - 4}"
    } else {
        preview
    }
}

internal fun summarizeCircleVertexList(vertices: List<GeofenceVertex>): String {
    if (vertices.isEmpty()) {
        return "-"
    }
    val preview = vertices.take(4).joinToString("; ") { vertex ->
        "${vertex.latitude.trim()}, ${vertex.longitude.trim()}"
    }
    return if (vertices.size > 4) {
        "$preview; ... +${vertices.size - 4}"
    } else {
        preview
    }
}

internal fun summarizePolygonVertexList(vertices: List<GeofenceVertex>): String {
    if (vertices.isEmpty()) {
        return "-"
    }
    val preview = vertices.take(4).joinToString("; ") { vertex ->
        "${vertex.latitude.trim()}, ${vertex.longitude.trim()}"
    }
    return if (vertices.size > 4) {
        "$preview; ... +${vertices.size - 4}"
    } else {
        preview
    }
}

internal fun summarizeCircleCenters(points: List<GeofencePoint>): String {
    if (points.isEmpty()) {
        return "-"
    }
    val preview = points.take(4).joinToString("; ") { point ->
        formatCoordinates(point.latitude, point.longitude)
    }
    return if (points.size > 4) {
        "$preview; ... +${points.size - 4}"
    } else {
        preview
    }
}

internal fun effectiveCircleCenters(config: GeofenceConfig?): List<GeofencePoint> {
    if (config == null || config.shapeType != GeofenceShapeType.Circle) {
        return emptyList()
    }
    return config.circleCenters.ifEmpty {
        listOf(
            GeofencePoint(
                latitude = config.centerLat,
                longitude = config.centerLng
            )
        )
    }
}

internal fun GeofencePoint.toLatLng(): LatLng = LatLng(latitude, longitude)

internal fun offsetLatLng(center: LatLng, distanceMeters: Double, bearingDegrees: Double): LatLng {
    val earthRadiusMeters = 6_371_000.0
    val angularDistance = distanceMeters / earthRadiusMeters
    val bearingRadians = Math.toRadians(bearingDegrees)
    val lat1 = Math.toRadians(center.latitude)
    val lng1 = Math.toRadians(center.longitude)

    val lat2 = kotlin.math.asin(
        kotlin.math.sin(lat1) * kotlin.math.cos(angularDistance) +
            kotlin.math.cos(lat1) * kotlin.math.sin(angularDistance) * kotlin.math.cos(bearingRadians)
    )
    val lng2 = lng1 + kotlin.math.atan2(
        kotlin.math.sin(bearingRadians) * kotlin.math.sin(angularDistance) * kotlin.math.cos(lat1),
        kotlin.math.cos(angularDistance) - kotlin.math.sin(lat1) * kotlin.math.sin(lat2)
    )
    return LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2))
}
