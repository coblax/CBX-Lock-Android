package com.coblax.examlock.ui.geofence

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coblax.examlock.GeofenceShapeType
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

/**
 * A [MapView] that follows the screen's lifecycle, so the map pauses while the admin is
 * in another app (copying coordinates from Google Maps, say) and comes back drawn.
 */
@Composable
internal fun rememberLifecycleMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        var started = false
        var resumed = false
        // Adding the observer replays ON_START/ON_RESUME up to the current state.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    runCatching { mapView.onStart() }
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    runCatching { mapView.onResume() }
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    runCatching { mapView.onPause() }
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    runCatching { mapView.onStop() }
                    started = false
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) runCatching { mapView.onPause() }
            if (started) runCatching { mapView.onStop() }
            runCatching { mapView.onDestroy() }
        }
    }
    return mapView
}

internal data class GeofenceMapStyle(
    val areaStroke: Int,
    val areaFill: Int,
    val invalidStroke: Int,
    val invalidFill: Int,
    val marker: Int,
    val markerText: Int,
    val selectedMarker: Int,
    val selectedMarkerText: Int,
    val markerRing: Int
)

/**
 * Keeps the drawn markers, circles and polygon in step with the draft by moving the
 * existing objects. Clearing and redrawing the map, as the old editors did, also cancels
 * a marker drag in progress and makes the whole map flicker on every tap.
 */
internal class GeofenceEditorMapRenderer(
    private val map: GoogleMap,
    private val density: Float
) {
    private val markers = ArrayList<Marker?>()
    private val markerIconKeys = ArrayList<Int>()
    private val circles = ArrayList<Circle?>()
    private var polygon: Polygon? = null
    private var polyline: Polyline? = null
    private var searchMarker: Marker? = null
    private val icons = HashMap<Int, BitmapDescriptor>()
    private var points: List<LatLng?> = emptyList()
    private var shape: GeofenceShapeType = GeofenceShapeType.Circle
    private var radiusMeters: Double? = null
    private var invalid = false
    private var style: GeofenceMapStyle? = null
    private var dragIndex = -1
    private var dragPosition: LatLng? = null

    fun render(
        shape: GeofenceShapeType,
        points: List<LatLng?>,
        radiusMeters: Double?,
        selectedIndex: Int,
        invalid: Boolean,
        style: GeofenceMapStyle
    ) {
        if (this.style != style) {
            icons.clear()
            for (index in markerIconKeys.indices) {
                markerIconKeys[index] = -1
            }
        }
        this.shape = shape
        val dragged = dragPosition
        // A redraw during a drag (the drag selects its point) must not pull the marker
        // back to where the draft last had it.
        this.points = if (dragged != null && dragIndex in points.indices) {
            points.toMutableList().also { it[dragIndex] = dragged }
        } else {
            points
        }
        this.radiusMeters = radiusMeters
        this.invalid = invalid
        this.style = style
        syncMarkers(selectedIndex, style)
        syncArea(style)
    }

    /** Moves the area with a marker while it is dragged, before the draft changes. */
    fun previewDrag(index: Int, position: LatLng) {
        if (index !in points.indices) {
            return
        }
        dragIndex = index
        dragPosition = position
        points = points.toMutableList().also { it[index] = position }
        circles.getOrNull(index)?.center = position
        style?.let { syncArea(it) }
    }

    fun endDrag() {
        dragIndex = -1
        dragPosition = null
    }

    fun showSearchAnchor(position: LatLng?, title: String) {
        if (position == null) {
            searchMarker?.remove()
            searchMarker = null
            return
        }
        val marker = searchMarker
        if (marker == null) {
            searchMarker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .zIndex(1f)
            )
        } else {
            marker.position = position
            marker.title = title
        }
    }

    /** Bounds of every valid point, grown by the circle radius so whole circles fit. */
    fun contentBounds(): LatLngBounds? {
        val valid = points.filterNotNull()
        if (valid.isEmpty()) {
            return null
        }
        val builder = LatLngBounds.Builder()
        val radius = radiusMeters?.takeIf { shape == GeofenceShapeType.Circle }
        valid.forEach { point ->
            builder.include(point)
            if (radius != null) {
                builder.include(offsetLatLng(point, radius, 0.0))
                builder.include(offsetLatLng(point, radius, 90.0))
                builder.include(offsetLatLng(point, radius, 180.0))
                builder.include(offsetLatLng(point, radius, 270.0))
            }
        }
        return builder.build()
    }

    fun release() {
        markers.forEach { runCatching { it?.remove() } }
        markers.clear()
        markerIconKeys.clear()
        circles.forEach { runCatching { it?.remove() } }
        circles.clear()
        runCatching { polygon?.remove() }
        runCatching { polyline?.remove() }
        runCatching { searchMarker?.remove() }
        polygon = null
        polyline = null
        searchMarker = null
        icons.clear()
    }

    private fun syncMarkers(selectedIndex: Int, style: GeofenceMapStyle) {
        while (markers.size > points.size) {
            markers.removeAt(markers.lastIndex)?.remove()
            markerIconKeys.removeAt(markerIconKeys.lastIndex)
        }
        points.forEachIndexed { index, position ->
            if (index >= markers.size) {
                markers += null
                markerIconKeys += -1
            }
            if (position == null) {
                markers[index]?.remove()
                markers[index] = null
                markerIconKeys[index] = -1
                return@forEachIndexed
            }
            val selected = index == selectedIndex
            val iconKey = (index + 1) * 2 + if (selected) 1 else 0
            val marker = markers[index]
            if (marker == null) {
                markers[index] = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .icon(markerIcon(index + 1, selected, style))
                        .anchor(0.5f, 0.5f)
                        .draggable(true)
                        .zIndex(if (selected) 3f else 2f)
                )?.also { it.tag = index }
                markerIconKeys[index] = iconKey
            } else {
                if (index != dragIndex && marker.position != position) {
                    marker.position = position
                }
                if (markerIconKeys[index] != iconKey) {
                    marker.setIcon(markerIcon(index + 1, selected, style))
                    marker.zIndex = if (selected) 3f else 2f
                    markerIconKeys[index] = iconKey
                }
                marker.tag = index
            }
        }
    }

    private fun syncArea(style: GeofenceMapStyle) {
        val stroke = if (invalid) style.invalidStroke else style.areaStroke
        val fill = if (invalid) style.invalidFill else style.areaFill
        val strokeWidth = 3f * density
        if (shape == GeofenceShapeType.Circle) {
            removePolygon()
            val radius = radiusMeters
            while (circles.size > points.size) {
                circles.removeAt(circles.lastIndex)?.remove()
            }
            points.forEachIndexed { index, position ->
                if (index >= circles.size) {
                    circles += null
                }
                if (position == null || radius == null) {
                    circles[index]?.remove()
                    circles[index] = null
                    return@forEachIndexed
                }
                val circle = circles[index]
                if (circle == null) {
                    circles[index] = map.addCircle(
                        CircleOptions()
                            .center(position)
                            .radius(radius)
                            .strokeColor(stroke)
                            .fillColor(fill)
                            .strokeWidth(strokeWidth)
                    )
                } else {
                    circle.center = position
                    circle.radius = radius
                    circle.strokeColor = stroke
                    circle.fillColor = fill
                }
            }
            return
        }

        circles.forEach { it?.remove() }
        circles.clear()
        val valid = points.filterNotNull()
        when {
            valid.size >= 3 -> {
                polyline?.remove()
                polyline = null
                val current = polygon
                if (current == null) {
                    polygon = map.addPolygon(
                        PolygonOptions()
                            .addAll(valid)
                            .strokeColor(stroke)
                            .fillColor(fill)
                            .strokeWidth(strokeWidth)
                    )
                } else {
                    current.points = valid
                    current.strokeColor = stroke
                    current.fillColor = fill
                }
            }
            valid.size == 2 -> {
                polygon?.remove()
                polygon = null
                val current = polyline
                if (current == null) {
                    polyline = map.addPolyline(
                        PolylineOptions()
                            .addAll(valid)
                            .color(stroke)
                            .width(strokeWidth)
                    )
                } else {
                    current.points = valid
                    current.color = stroke
                }
            }
            else -> removePolygon()
        }
    }

    private fun removePolygon() {
        polygon?.remove()
        polygon = null
        polyline?.remove()
        polyline = null
    }

    private fun markerIcon(number: Int, selected: Boolean, style: GeofenceMapStyle): BitmapDescriptor {
        val key = number * 2 + if (selected) 1 else 0
        return icons.getOrPut(key) {
            BitmapDescriptorFactory.fromBitmap(drawMarker(number, selected, style))
        }
    }

    private fun drawMarker(number: Int, selected: Boolean, style: GeofenceMapStyle): Bitmap {
        val sizePx = ((if (selected) 38 else 30) * density).toInt().coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = sizePx / 2f
        val ring = 2.5f * density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = style.markerRing
        canvas.drawCircle(radius, radius, radius, paint)
        paint.color = if (selected) style.selectedMarker else style.marker
        canvas.drawCircle(radius, radius, radius - ring, paint)
        paint.color = if (selected) style.selectedMarkerText else style.markerText
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = (if (selected) 16 else 13) * density
        val baseline = radius - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(number.toString(), radius, baseline, paint)
        return bitmap
    }
}
