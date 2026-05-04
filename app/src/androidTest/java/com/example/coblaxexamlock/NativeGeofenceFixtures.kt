package com.example.coblaxexamlock

internal object NativeGeofenceFixtures {
    val squarePolygon = listOf(
        GeofencePoint(latitude = 0.0, longitude = 0.0),
        GeofencePoint(latitude = 0.0, longitude = 10.0),
        GeofencePoint(latitude = 10.0, longitude = 10.0),
        GeofencePoint(latitude = 10.0, longitude = 0.0)
    )

    val concavePolygon = listOf(
        GeofencePoint(latitude = 0.0, longitude = 0.0),
        GeofencePoint(latitude = 0.0, longitude = 8.0),
        GeofencePoint(latitude = 4.0, longitude = 8.0),
        GeofencePoint(latitude = 4.0, longitude = 4.0),
        GeofencePoint(latitude = 8.0, longitude = 4.0),
        GeofencePoint(latitude = 8.0, longitude = 0.0)
    )

    val bowTiePolygon = listOf(
        GeofencePoint(latitude = 0.0, longitude = 0.0),
        GeofencePoint(latitude = 4.0, longitude = 4.0),
        GeofencePoint(latitude = 0.0, longitude = 4.0),
        GeofencePoint(latitude = 4.0, longitude = 0.0)
    )

    val locationSnapshot = LocationSnapshot(
        latitude = -6.200000,
        longitude = 106.816666,
        accuracyMeters = 5f,
        provider = "gps",
        fixTimestampMs = 123456789L,
        isMock = false
    )

    val circleCenters = listOf(
        GeofencePoint(latitude = -6.199500, longitude = 106.816000),
        GeofencePoint(latitude = -6.201000, longitude = 106.817000),
        GeofencePoint(latitude = -6.198750, longitude = 106.817250)
    )
}
