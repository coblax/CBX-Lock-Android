package com.example.coblaxexamlock.nativebridge

import android.util.Log
import com.example.coblaxexamlock.GeofencePoint

private const val NativeGeofenceTag = "NativeGeofenceBridge"

private object NativeGeofenceRuntime {
    val loadFailure: Throwable?
    val isAvailable: Boolean

    init {
        var failure: Throwable? = null
        val available = try {
            System.loadLibrary("examlock_native")
            true
        } catch (throwable: Throwable) {
            failure = throwable
            false
        }
        loadFailure = failure
        isAvailable = available
        failure?.let {
            Log.w(NativeGeofenceTag, "Native library unavailable; Kotlin fallback will be used.", it)
        }
    }
}

internal object NativeGeofenceBridge {
    fun isNativeAvailableForTests(): Boolean = NativeGeofenceRuntime.isAvailable

    private fun <T> invokeOrFallback(
        operation: String,
        fallback: () -> T,
        nativeCall: () -> T
    ): T {
        return when (NativeBridgeTestControl.currentMode) {
            NativeBridgeBackendMode.ForceKotlinFallback -> fallback()
            NativeBridgeBackendMode.ForceNative -> {
                check(NativeGeofenceRuntime.isAvailable) {
                    "Native library is unavailable while ForceNative mode is active for $operation."
                }
                nativeCall()
            }
            NativeBridgeBackendMode.Auto -> {
                if (!NativeGeofenceRuntime.isAvailable) {
                    fallback()
                } else {
                    runCatching(nativeCall)
                        .onFailure { throwable ->
                            Log.w(NativeGeofenceTag, "Native $operation failed; using Kotlin fallback.", throwable)
                        }
                        .getOrElse { fallback() }
                }
            }
        }
    }

    fun isPointInPolygon(
        point: GeofencePoint,
        polygon: List<GeofencePoint>,
        fallback: () -> Boolean
    ): Boolean = invokeOrFallback(operation = "isPointInPolygon", fallback = fallback) {
        val latitudes = polygon.map { it.latitude }.toDoubleArray()
        val longitudes = polygon.map { it.longitude }.toDoubleArray()
        nativeIsPointInPolygon(point.latitude, point.longitude, latitudes, longitudes)
    }

    fun isSelfIntersectingPolygon(
        polygon: List<GeofencePoint>,
        fallback: () -> Boolean
    ): Boolean = invokeOrFallback(operation = "isSelfIntersectingPolygon", fallback = fallback) {
        val latitudes = polygon.map { it.latitude }.toDoubleArray()
        val longitudes = polygon.map { it.longitude }.toDoubleArray()
        nativeIsSelfIntersectingPolygon(latitudes, longitudes)
    }

    fun distancePointToSegmentMeters(
        point: GeofencePoint,
        segmentStart: GeofencePoint,
        segmentEnd: GeofencePoint,
        fallback: () -> Double
    ): Double = invokeOrFallback(operation = "distancePointToSegmentMeters", fallback = fallback) {
        nativeDistancePointToSegmentMeters(
            point.latitude,
            point.longitude,
            segmentStart.latitude,
            segmentStart.longitude,
            segmentEnd.latitude,
            segmentEnd.longitude
        )
    }

    fun closestCircleDistanceMeters(
        locationLat: Double,
        locationLng: Double,
        centers: List<GeofencePoint>,
        fallback: () -> Double
    ): Double = invokeOrFallback(operation = "closestCircleDistanceMeters", fallback = fallback) {
        val latitudes = centers.map { it.latitude }.toDoubleArray()
        val longitudes = centers.map { it.longitude }.toDoubleArray()
        nativeClosestCircleDistanceMeters(locationLat, locationLng, latitudes, longitudes)
    }
}
