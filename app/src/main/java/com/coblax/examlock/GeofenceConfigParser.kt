package com.coblax.examlock

private data class ParsedGeofenceVertex(
    val point: GeofencePoint?,
    val error: String?
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

