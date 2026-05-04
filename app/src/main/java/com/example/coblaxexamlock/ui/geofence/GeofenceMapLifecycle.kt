package com.example.coblaxexamlock.ui.geofence

import android.annotation.SuppressLint
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView

internal fun MapView.startGeofenceMapLifecycle() {
    runCatching { onStart() }
    runCatching { onResume() }
}

@SuppressLint("MissingPermission")
internal fun GoogleMap.releaseGeofenceMapResources() {
    runCatching { setOnMapClickListener(null) }
    runCatching { setOnMapLongClickListener(null) }
    runCatching { setOnMarkerClickListener(null) }
    runCatching { setOnCameraIdleListener(null) }
    runCatching { setOnCameraMoveListener(null) }
    runCatching { setOnCameraMoveCanceledListener(null) }
    runCatching { setOnCameraMoveStartedListener(null) }
    runCatching { setOnPolygonClickListener(null) }
    runCatching { setOnPolylineClickListener(null) }
    runCatching { setOnCircleClickListener(null) }
    runCatching { isMyLocationEnabled = false }
    runCatching { uiSettings.isMyLocationButtonEnabled = false }
    runCatching { clear() }
}

internal fun MapView.disposeGeofenceMapLifecycle(googleMap: GoogleMap?) {
    googleMap?.releaseGeofenceMapResources()
    runCatching { onPause() }
    runCatching { onStop() }
    runCatching { onDestroy() }
}
