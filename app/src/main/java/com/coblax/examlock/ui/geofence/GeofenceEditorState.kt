package com.coblax.examlock.ui.geofence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs
import kotlin.math.ln

internal enum class PendingLocate { None, Camera, AddPoint }

internal const val PointZoom = 17f
private const val MaxUndoSteps = 40
private const val MaxAnimatedZoomJump = 4f

/**
 * Everything the area editor remembers: the draft points and radius with their undo
 * history, the place search, and the live map handles. Kept out of the screen
 * composable so that stays a set of small functions the runtime compiles cheaply.
 */
@Stable
internal class GeofenceEditorState(
    val shape: GeofenceShapeType,
    val initialPoints: List<GeofenceVertex>,
    val startingRadius: String,
    points: List<GeofenceVertex>,
    radiusText: String,
    selectedIndex: Int,
    mapOpen: Boolean,
    satellite: Boolean,
    searchQuery: String
) {
    val isCircle: Boolean = shape != GeofenceShapeType.Polygon
    val maxPoints: Int = maxGeofencePoints(shape)

    var points by mutableStateOf(points)
        private set
    var radiusText by mutableStateOf(radiusText)
    var selectedIndex by mutableIntStateOf(selectedIndex)
        private set
    var mapOpen by mutableStateOf(mapOpen)
    var satellite by mutableStateOf(satellite)
    var undoStack by mutableStateOf(emptyList<List<GeofenceVertex>>())
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var noticeSerial by mutableIntStateOf(0)
        private set
    var locating by mutableStateOf(false)
    var pendingLocate by mutableStateOf(PendingLocate.None)
    private var typingUndoIndex = -1

    var googleMap by mutableStateOf<GoogleMap?>(null)
    var renderer by mutableStateOf<GeofenceEditorMapRenderer?>(null)
    var mapView: MapView? = null

    var searchQuery by mutableStateOf(searchQuery)
    var searchResults by mutableStateOf(emptyList<MapSearchResult>())
    var searchLoading by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    var searchAnchor by mutableStateOf<MapSearchResult?>(null)

    val issue: GeofenceDraftIssue?
        get() = geofenceDraftIssue(shape, points, radiusText)

    val radiusMeters: Int?
        get() = parseRadiusMeters(radiusText)

    val changed: Boolean
        get() = points != initialPoints || (isCircle && radiusText.trim() != startingRadius)

    /** Crossed or flat polygons are drawn in red so the admin sees what is wrong. */
    val drawInvalid: Boolean
        get() = issue == GeofenceDraftIssue.PolygonCrossing || issue == GeofenceDraftIssue.PolygonTooSmall

    val hasPlacedPoint: Boolean
        get() = points.any { it.toCoordinateOrNull() != null }

    fun mapPoints(): List<LatLng?> =
        points.map { vertex -> vertex.toCoordinateOrNull()?.let { LatLng(it.latitude, it.longitude) } }

    fun showNotice(message: String) {
        notice = message
        noticeSerial++
    }

    fun dismissNotice() {
        notice = null
    }

    fun select(index: Int) {
        selectedIndex = index.coerceIn(-1, points.lastIndex)
        typingUndoIndex = -1
    }

    private fun commit(newPoints: List<GeofenceVertex>, newSelected: Int) {
        undoStack = (undoStack + listOf(points)).takeLast(MaxUndoSteps)
        points = newPoints
        selectedIndex = newSelected.coerceIn(-1, newPoints.lastIndex)
        typingUndoIndex = -1
    }

    /** Adds a point (blank when [coordinate] is null); false when the shape is full. */
    fun addPoint(coordinate: GeofenceCoordinate?): Boolean {
        if (points.size >= maxPoints) {
            return false
        }
        commit(points + (coordinate?.toVertex() ?: GeofenceVertex("", "")), points.size)
        return true
    }

    fun movePoint(index: Int, coordinate: GeofenceCoordinate) {
        if (index !in points.indices) {
            return
        }
        commit(points.toMutableList().also { it[index] = coordinate.toVertex() }, index)
    }

    /** Typing records one undo step per point, not one per keystroke. */
    fun typeCoordinate(coordinate: GeofenceCoordinate) {
        val index = selectedIndex
        if (index !in points.indices) {
            return
        }
        val updated = points.toMutableList().also { it[index] = coordinate.toVertex() }
        if (typingUndoIndex == index) {
            points = updated
        } else {
            commit(updated, index)
            typingUndoIndex = index
        }
    }

    fun deleteSelected() {
        val index = selectedIndex
        if (index !in points.indices) {
            return
        }
        val updated = points.toMutableList().also { it.removeAt(index) }
        commit(updated, if (updated.isEmpty()) -1 else index.coerceAtMost(updated.lastIndex))
    }

    fun clearAll() {
        commit(emptyList(), -1)
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        points = previous
        selectedIndex = when {
            previous.isEmpty() -> -1
            selectedIndex !in previous.indices -> previous.lastIndex
            else -> selectedIndex
        }
        typingUndoIndex = -1
    }

    fun orderPolygon() {
        val selectedVertex = points.getOrNull(selectedIndex)
        val ordered = orderPolygonAroundCenter(points)
        if (ordered != points) {
            commit(ordered, selectedVertex?.let { ordered.indexOf(it) } ?: -1)
        }
    }

    fun clearSearch(keepQuery: Boolean = true) {
        if (!keepQuery) {
            searchQuery = ""
            searchAnchor = null
        }
        searchResults = emptyList()
        searchError = null
    }

    fun moveCamera(latLng: LatLng, zoom: Float = PointZoom, animate: Boolean = true) {
        val map = googleMap ?: return
        val update = CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        // Flying from the whole-country view down to a street streams tiles for every
        // zoom level on the way, which a low-end phone (and its data plan) pays for.
        val longJump = runCatching { abs(map.cameraPosition.zoom - zoom) > MaxAnimatedZoomJump }.getOrDefault(true)
        runCatching { if (animate && !longJump) map.animateCamera(update) else map.moveCamera(update) }
    }

    fun zoomBy(step: Float) {
        runCatching { googleMap?.animateCamera(CameraUpdateFactory.zoomBy(step)) }
    }

    /** Frames every placed point, whole circles included. */
    fun fitCamera(animate: Boolean, density: Float) {
        val map = googleMap ?: return
        val bounds = renderer?.contentBounds() ?: return
        val view = mapView
        val padding = (48 * density).toInt()
        val singlePoint = bounds.northeast == bounds.southwest
        val fitsView = view != null && view.width > padding * 3 && view.height > padding * 3
        val update = when {
            singlePoint -> CameraUpdateFactory.newLatLngZoom(bounds.center, PointZoom)
            fitsView -> CameraUpdateFactory.newLatLngBounds(bounds, view!!.width, view.height, padding)
            else -> CameraUpdateFactory.newLatLngZoom(bounds.center, 16f)
        }
        val targetZoom = when {
            singlePoint -> PointZoom
            fitsView -> approximateBoundsZoom(
                latitudeSpan = bounds.northeast.latitude - bounds.southwest.latitude,
                longitudeSpan = bounds.northeast.longitude - bounds.southwest.longitude,
                viewWidthDp = (view!!.width - 2 * padding) / density,
                viewHeightDp = (view.height - 2 * padding) / density
            )
            else -> 16f
        }
        // Framing a school from the whole-country view is the same long flight moveCamera
        // avoids: every zoom level's tiles on the way, too much for a low-end phone (the
        // emulator's renderer died on it). Past a few levels, jump straight there.
        val longJump = runCatching { abs(map.cameraPosition.zoom - targetZoom) > MaxAnimatedZoomJump }.getOrDefault(true)
        runCatching { if (animate && !longJump) map.animateCamera(update) else map.moveCamera(update) }
    }

    fun savedPoints(): List<GeofenceVertex> = points.mapNotNull { it.toCoordinateOrNull()?.toVertex() }

    companion object {
        fun saver(shape: GeofenceShapeType, initialPoints: List<GeofenceVertex>, startingRadius: String) =
            listSaver<GeofenceEditorState, Any>(
                save = { state ->
                    listOf(
                        state.points.joinToString(";") { "${it.latitude}|${it.longitude}" },
                        state.radiusText,
                        state.selectedIndex,
                        state.mapOpen,
                        state.satellite,
                        state.searchQuery
                    )
                },
                restore = { saved ->
                    val pointText = saved[0] as String
                    GeofenceEditorState(
                        shape = shape,
                        initialPoints = initialPoints,
                        startingRadius = startingRadius,
                        points = if (pointText.isEmpty()) emptyList() else pointText.split(';').map { entry ->
                            val parts = entry.split('|', limit = 2)
                            GeofenceVertex(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
                        },
                        radiusText = saved[1] as String,
                        selectedIndex = saved[2] as Int,
                        mapOpen = saved[3] as Boolean,
                        satellite = saved[4] as Boolean,
                        searchQuery = saved[5] as String
                    )
                }
            )
    }
}

@Composable
internal fun rememberGeofenceEditorState(
    shape: GeofenceShapeType,
    initialPoints: List<GeofenceVertex>,
    initialRadiusMeters: String,
    mapOpenAtStart: Boolean
): GeofenceEditorState {
    val startingRadius = parseRadiusMeters(initialRadiusMeters)?.toString() ?: DefaultCircleRadiusMeters.toString()
    return rememberSaveable(saver = GeofenceEditorState.saver(shape, initialPoints, startingRadius)) {
        GeofenceEditorState(
            shape = shape,
            initialPoints = initialPoints,
            startingRadius = startingRadius,
            points = initialPoints,
            radiusText = startingRadius,
            selectedIndex = if (initialPoints.isEmpty()) -1 else 0,
            mapOpen = mapOpenAtStart,
            satellite = false,
            searchQuery = ""
        )
    }
}

/**
 * About the zoom at which a box of [latitudeSpan] x [longitudeSpan] degrees fills a view of
 * the given size: the world is 256 dp wide at zoom 0 and doubles each level. Near the
 * equator a degree of latitude and of longitude project about the same, which is close
 * enough to decide whether a camera move is worth animating.
 */
internal fun approximateBoundsZoom(
    latitudeSpan: Double,
    longitudeSpan: Double,
    viewWidthDp: Float,
    viewHeightDp: Float
): Float {
    fun zoomFor(spanDegrees: Double, viewDp: Float): Double =
        if (spanDegrees <= 0.0 || viewDp <= 0f) {
            PointZoom.toDouble()
        } else {
            ln(viewDp * 360.0 / (256.0 * spanDegrees)) / ln(2.0)
        }
    val zoom = minOf(zoomFor(abs(latitudeSpan), viewHeightDp), zoomFor(abs(longitudeSpan), viewWidthDp))
    return zoom.toFloat().coerceIn(2f, 21f)
}
