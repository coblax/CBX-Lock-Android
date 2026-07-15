package com.coblax.examlock.persistence

import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import java.nio.charset.StandardCharsets
import java.util.Locale

internal fun serializeExamLocationPolicy(policy: ExamQrLocationPolicy): String {
    return listOf(
        policy.shapeType.name.lowercase(Locale.US),
        encodePolicyField(policy.centerLat),
        encodePolicyField(policy.centerLng),
        encodePolicyField(policy.radiusMeters),
        encodePolicyField(
            policy.vertices.joinToString(";") { vertex ->
                "${vertex.latitude.trim()},${vertex.longitude.trim()}"
            }
        ),
        encodePolicyField(
            policy.effectiveCircleCenters.joinToString(";") { vertex ->
                "${vertex.latitude.trim()},${vertex.longitude.trim()}"
            }
        )
    ).joinToString("|")
}

internal fun deserializeExamLocationPolicy(rawValue: String): ExamQrLocationPolicy? {
    if (rawValue.isBlank()) {
        return null
    }
    val parts = rawValue.split("|")
    if (parts.size < 5) {
        return null
    }
    val shapeType = when (parts[0].trim().lowercase(Locale.US)) {
        "circle" -> GeofenceShapeType.Circle
        "polygon" -> GeofenceShapeType.Polygon
        else -> GeofenceShapeType.Disabled
    }
    val centerLat = decodePolicyField(parts[1])
    val centerLng = decodePolicyField(parts[2])
    val circleCenters = if (parts.size >= 6) {
        decodePolicyField(parts[5])
            .split(';')
            .mapNotNull { rawPoint ->
                val pointParts = rawPoint.split(',')
                if (pointParts.size == 2) {
                    GeofenceVertex(
                        latitude = pointParts[0].trim(),
                        longitude = pointParts[1].trim()
                    )
                } else {
                    null
                }
            }
    } else {
        emptyList()
    }
    return ExamQrLocationPolicy(
        shapeType = shapeType,
        centerLat = centerLat,
        centerLng = centerLng,
        radiusMeters = decodePolicyField(parts[3]),
        vertices = decodePolicyField(parts[4])
            .split(';')
            .mapNotNull { rawPoint ->
                val pointParts = rawPoint.split(',')
                if (pointParts.size == 2) {
                    GeofenceVertex(
                        latitude = pointParts[0].trim(),
                        longitude = pointParts[1].trim()
                    )
                } else {
                    null
                }
            },
        circleCenters = when {
            shapeType != GeofenceShapeType.Circle -> emptyList()
            circleCenters.isNotEmpty() -> circleCenters
            centerLat.isNotBlank() && centerLng.isNotBlank() -> listOf(
                GeofenceVertex(
                    latitude = centerLat,
                    longitude = centerLng
                )
            )
            else -> emptyList()
        }
    )
}

internal fun encodePolicyField(value: String): String {
    return android.util.Base64.encodeToString(
        value.toByteArray(StandardCharsets.UTF_8),
        android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE
    )
}

internal fun decodePolicyField(value: String): String {
    return runCatching {
        String(
            android.util.Base64.decode(value, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE),
            StandardCharsets.UTF_8
        )
    }.getOrDefault("")
}
