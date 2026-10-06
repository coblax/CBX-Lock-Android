package com.coblax.examlock.ui.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.i18n.LocalUiLanguage
import com.coblax.examlock.i18n.localized
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.runtime.acquireBestEffortLocationSnapshot
import com.coblax.examlock.runtime.isLocationServicesEnabled
import com.coblax.examlock.ui.theme.AppColors
import com.coblax.examlock.ui.theme.AppTextStyles
import com.coblax.examlock.ui.theme.ScopedUiTokens
import com.coblax.examlock.ui.theme.UpgradeUiScope
import com.coblax.examlock.ui.theme.primaryActionColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.PlacesClient
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

internal const val GeofenceEditorSaveTestTag = "geofence_editor_save"
internal const val GeofenceEditorCoordinateTestTag = "geofence_editor_coordinate"
internal const val GeofenceEditorAddPointTestTag = "geofence_editor_add_point"
internal const val GeofenceEditorRadiusTestTag = "geofence_editor_radius"

private val IndonesiaOverview = LatLng(-2.5489, 118.0149)
private const val IndonesiaOverviewZoom = 4.5f

/** Below this the zoom buttons would collide with the tools above them; pinch still works. */
private val ZoomButtonsMinMapHeight = 300.dp

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * One editor for both kinds of exam area. The map is the canvas (tap to add a point,
 * long-press a point to drag it), the panel below holds the exact numbers, and the same
 * panel works on its own when the map is off for a low-memory phone or a build without
 * a Maps key.
 */
@Composable
internal fun GeofenceAreaEditorScreen(
    shape: GeofenceShapeType,
    initialPoints: List<GeofenceVertex>,
    initialRadiusMeters: String,
    onDismiss: () -> Unit,
    onSave: (points: List<GeofenceVertex>, radiusMeters: String) -> Unit
) {
    UpgradeUiScope {
        GeofenceAreaEditorContent(shape, initialPoints, initialRadiusMeters, onDismiss, onSave)
    }
}

@Composable
private fun GeofenceAreaEditorContent(
    shape: GeofenceShapeType,
    initialPoints: List<GeofenceVertex>,
    initialRadiusMeters: String,
    onDismiss: () -> Unit,
    onSave: (points: List<GeofenceVertex>, radiusMeters: String) -> Unit
) {
    val context = LocalContext.current
    val uiLanguage = LocalUiLanguage.current
    val lowRamProfile = LocalLowRamProfile.current
    val scope = rememberCoroutineScope()
    val mapsApiKey = remember { SecureStrings.mapsApiKey }
    val canOpenMap = mapsApiKey.isNotBlank() && !lowRamProfile.memoryLow
    val state = rememberGeofenceEditorState(
        shape = shape,
        initialPoints = initialPoints,
        initialRadiusMeters = initialRadiusMeters,
        mapOpenAtStart = canOpenMap && !lowRamProfile.deferHeavyUi
    )
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val target = state.pendingLocate
        state.pendingLocate = PendingLocate.None
        if (grants.values.any { it }) {
            scope.launch { state.locate(context, uiLanguage, target) }
        } else {
            state.showNotice(
                localized(
                    uiLanguage,
                    "Location permission was not given. You can still tap the map or paste coordinates.",
                    "Izin lokasi tidak diberikan. Anda tetap bisa mengetuk peta atau menempel koordinat."
                )
            )
        }
    }
    val requestLocate: (PendingLocate) -> Unit = { target ->
        state.requestLocate(target, context, uiLanguage, scope, permissionLauncher)
    }
    val requestDismiss: () -> Unit = {
        when {
            state.searchResults.isNotEmpty() || state.searchError != null -> state.clearSearch()
            state.changed -> showDiscardDialog = true
            else -> onDismiss()
        }
    }
    val save: () -> Unit = {
        val issue = state.issue
        if (issue != null) {
            state.showNotice(geofenceIssueMessage(uiLanguage, shape, issue))
        } else {
            onSave(state.savedPoints(), if (state.isCircle) state.radiusMeters?.toString().orEmpty() else initialRadiusMeters)
        }
    }

    BackHandler(onBack = requestDismiss)
    EditorEffects(state)

    val mapShown = state.mapOpen && canOpenMap
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.current.background)
    ) {
        // The map is the main tool, so the panel stops at 42%; it scrolls past that.
        val panelMaxHeight = maxHeight * 0.42f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EditorTopBar(
                title = if (state.isCircle) tr("Circle", "Lingkaran") else tr("Polygon", "Polygon"),
                subtitle = tr(
                    "${state.points.size} of ${state.maxPoints} points",
                    "${state.points.size} dari ${state.maxPoints} titik"
                ),
                saveEnabled = state.issue == null,
                onBack = requestDismiss,
                onSave = save
            )
            if (mapShown) {
                EditorMapArea(
                    state = state,
                    mapsApiKey = mapsApiKey,
                    onLocate = requestLocate,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            EditorPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (mapShown) Modifier.heightIn(max = panelMaxHeight) else Modifier.weight(1f))
            ) {
                if (!mapShown) {
                    // Inside the scrolling panel, so on a small phone it scrolls away
                    // instead of holding half the screen.
                    MapClosedCard(
                        reason = when {
                            mapsApiKey.isBlank() -> tr(
                                "This build has no Google Maps key, so fill in the points by hand or from GPS.",
                                "Build ini belum punya kunci Google Maps. Isi titik secara manual atau dari GPS."
                            )
                            lowRamProfile.memoryLow -> tr(
                                "The phone is low on memory right now, so the map stays closed. Fill in the points by hand or from GPS.",
                                "Memori HP sedang penuh, jadi peta tidak dibuka. Isi titik secara manual atau dari GPS."
                            )
                            else -> tr(
                                "The map is closed to save memory on this phone. Paste coordinates, use GPS, or open the map.",
                                "Peta ditutup untuk menghemat memori HP ini. Tempel koordinat, pakai GPS, atau buka peta."
                            )
                        },
                        canOpen = canOpenMap,
                        onOpen = { state.mapOpen = true }
                    )
                }
                EditorPanelContent(
                    state = state,
                    mapShown = mapShown,
                    onLocate = requestLocate,
                    onRequestClear = { showClearDialog = true }
                )
            }
        }
    }

    if (showDiscardDialog) {
        ConfirmDialog(
            title = tr("Discard changes?", "Buang perubahan?"),
            message = tr(
                "The points you changed here are not saved yet.",
                "Titik yang diubah di sini belum disimpan."
            ),
            confirmLabel = tr("Discard", "Buang"),
            dismissLabel = tr("Keep editing", "Lanjut edit"),
            onConfirm = {
                showDiscardDialog = false
                onDismiss()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
    if (showClearDialog) {
        ConfirmDialog(
            title = tr("Remove all points?", "Hapus semua titik?"),
            message = tr("You can bring them back with Undo.", "Titik bisa dikembalikan dengan Urungkan."),
            confirmLabel = tr("Remove all", "Hapus semua"),
            dismissLabel = tr("Cancel", "Batal"),
            onConfirm = {
                showClearDialog = false
                state.clearAll()
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

/** Notice timeout and keeping the map drawing in step with the draft. */
@Composable
private fun EditorEffects(state: GeofenceEditorState) {
    val mapStyle = rememberGeofenceMapStyle()
    LaunchedEffect(state.noticeSerial) {
        if (state.notice != null) {
            delay(4_500)
            state.dismissNotice()
        }
    }
    LaunchedEffect(state.renderer, state.points, state.radiusMeters, state.selectedIndex, state.drawInvalid, mapStyle) {
        state.renderer?.render(
            shape = state.shape,
            points = state.mapPoints(),
            radiusMeters = state.radiusMeters?.toDouble(),
            selectedIndex = state.selectedIndex,
            invalid = state.drawInvalid,
            style = mapStyle
        )
    }
    LaunchedEffect(state.renderer, state.searchAnchor) {
        state.renderer?.showSearchAnchor(state.searchAnchor?.latLng, state.searchAnchor?.title.orEmpty())
    }
    LaunchedEffect(state.googleMap, state.satellite) {
        state.googleMap?.mapType = if (state.satellite) GoogleMap.MAP_TYPE_HYBRID else GoogleMap.MAP_TYPE_NORMAL
    }
}

@Composable
private fun rememberGeofenceMapStyle(): GeofenceMapStyle {
    val colors = AppColors.current
    return remember(colors) {
        GeofenceMapStyle(
            areaStroke = colors.blue.toArgb(),
            areaFill = colors.blue.copy(alpha = 0.18f).toArgb(),
            invalidStroke = colors.statusDanger.toArgb(),
            invalidFill = colors.statusDanger.copy(alpha = 0.16f).toArgb(),
            marker = colors.blueDark.toArgb(),
            markerText = Color.White.toArgb(),
            selectedMarker = colors.gold.toArgb(),
            selectedMarkerText = Color(0xFF1B1B1B).toArgb(),
            markerRing = Color.White.toArgb()
        )
    }
}

@Composable
private fun EditorMapArea(
    state: GeofenceEditorState,
    mapsApiKey: String,
    onLocate: (PendingLocate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiLanguage = LocalUiLanguage.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current.density
    val mapStyle = rememberGeofenceMapStyle()
    val placesClient = remember { ensurePlacesSdkReady(context, mapsApiKey) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }

    Column(modifier = modifier) {
        EditorSearchBar(
            query = state.searchQuery,
            loading = state.searchLoading,
            onQueryChange = {
                state.searchQuery = it
                if (it.isBlank()) state.clearSearch(keepQuery = false)
            },
            onSearch = {
                keyboard?.hide()
                state.runSearch(context, uiLanguage, scope, placesClient, sessionToken)
            },
            onClear = { state.clearSearch(keepQuery = false) }
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            EditorMapView(state = state, density = density, mapStyle = mapStyle)
            MapOverlayButtons(
                satellite = state.satellite,
                locating = state.locating,
                canFit = state.hasPlacedPoint,
                onToggleSatellite = { state.satellite = !state.satellite },
                onFit = { state.fitCamera(animate = true, density = density) },
                onMyLocation = { onLocate(PendingLocate.Camera) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            )
            if (maxHeight >= ZoomButtonsMinMapHeight) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapIconButton(icon = Icons.Rounded.Add, label = tr("Zoom in", "Perbesar"), onClick = { state.zoomBy(1f) })
                    MapIconButton(icon = Icons.Rounded.Remove, label = tr("Zoom out", "Perkecil"), onClick = { state.zoomBy(-1f) })
                }
            }
            if (state.searchResults.isNotEmpty() || state.searchError != null) {
                SearchResultsCard(
                    results = state.searchResults,
                    error = state.searchError,
                    onSelect = { result -> state.applySearchResult(result, uiLanguage, scope, placesClient, sessionToken) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp, end = 66.dp)
                )
            } else if (state.points.isEmpty()) {
                MapHint(
                    text = if (state.isCircle) {
                        tr("Tap the map where the exam takes place.", "Ketuk peta di tempat ujian berlangsung.")
                    } else {
                        tr(
                            "Tap each corner of the area in turn, all the way round.",
                            "Ketuk tiap sudut area secara berurutan mengelilingi area."
                        )
                    },
                    // Top, not bottom: the Google logo in the bottom corner must stay visible.
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp, end = 66.dp)
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun EditorMapView(state: GeofenceEditorState, density: Float, mapStyle: GeofenceMapStyle) {
    val context = LocalContext.current
    val uiLanguage = LocalUiLanguage.current
    val mapView = rememberLifecycleMapView()
    LaunchedEffect(mapView) {
        state.mapView = mapView
        val map = mapView.awaitMap()
        configureEditorMap(
            map = map,
            onTap = { latLng ->
                state.clearSearch()
                state.addPointOrWarn(GeofenceCoordinate(latLng.latitude, latLng.longitude), uiLanguage)
            },
            onSelect = state::select,
            onDragPreview = { index, position -> state.renderer?.previewDrag(index, position) },
            onDragEnd = { index, position ->
                state.renderer?.endDrag()
                state.movePoint(index, GeofenceCoordinate(position.latitude, position.longitude))
            }
        )
        if (hasAnyLocationPermission(context)) {
            runCatching { map.isMyLocationEnabled = true }
        }
        val renderer = GeofenceEditorMapRenderer(map, density)
        renderer.render(
            shape = state.shape,
            points = state.mapPoints(),
            radiusMeters = state.radiusMeters?.toDouble(),
            selectedIndex = state.selectedIndex,
            invalid = state.drawInvalid,
            style = mapStyle
        )
        state.renderer = renderer
        state.googleMap = map
        if (renderer.contentBounds() != null) {
            // Once the map has a size the bounds fit properly; this first move is a guess.
            map.setOnMapLoadedCallback { state.fitCamera(animate = false, density = density) }
            state.fitCamera(animate = false, density = density)
        } else {
            state.moveCamera(IndonesiaOverview, IndonesiaOverviewZoom, animate = false)
            if (hasAnyLocationPermission(context) && isLocationServicesEnabled(context)) {
                val last = runCatching {
                    acquireBestEffortLocationSnapshot(context, preferFresh = false, geofenceConfig = null)
                }.getOrNull()
                if (last != null && state.points.isEmpty()) {
                    state.moveCamera(LatLng(last.latitude, last.longitude))
                }
            }
        }
    }
    DisposableEffect(mapView) {
        onDispose {
            state.renderer?.release()
            state.googleMap?.let { map ->
                runCatching { map.setOnMapClickListener(null) }
                runCatching { map.setOnPoiClickListener(null) }
                runCatching { map.setOnMarkerClickListener(null) }
                runCatching { map.setOnMarkerDragListener(null) }
                runCatching { map.setOnMapLoadedCallback(null) }
                runCatching { map.isMyLocationEnabled = false }
            }
            state.renderer = null
            state.googleMap = null
            state.mapView = null
        }
    }
    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun ColumnScope.EditorPanelContent(
    state: GeofenceEditorState,
    mapShown: Boolean,
    onLocate: (PendingLocate) -> Unit,
    onRequestClear: () -> Unit
) {
    val uiLanguage = LocalUiLanguage.current
    val density = LocalDensity.current.density
    val issue = state.issue
    StatusLine(
        notice = state.notice,
        issueText = issue?.let { geofenceIssueMessage(uiLanguage, state.shape, it) },
        readyText = if (state.isCircle) {
            tr(
                "Ready: ${state.points.size} ${if (state.points.size == 1) "center" else "centers"}, radius ${state.radiusMeters ?: "-"} m",
                "Siap: ${state.points.size} titik pusat, radius ${state.radiusMeters ?: "-"} m"
            )
        } else {
            tr("Ready: area with ${state.points.size} corners", "Siap: area dengan ${state.points.size} sudut")
        }
    )
    if (state.isCircle) {
        RadiusEditor(
            radiusText = state.radiusText,
            radiusMeters = state.radiusMeters,
            onRadiusTextChange = { state.radiusText = it.filter { ch -> ch.isDigit() }.take(5) }
        )
    }
    PointsSection(
        shape = state.shape,
        points = state.points,
        selectedIndex = state.selectedIndex,
        maxPoints = state.maxPoints,
        mapOpen = mapShown,
        locating = state.locating,
        onSelect = { index ->
            state.select(index)
            state.points.getOrNull(index)?.toCoordinateOrNull()?.let { point ->
                state.googleMap?.let {
                    runCatching { it.animateCamera(CameraUpdateFactory.newLatLng(LatLng(point.latitude, point.longitude))) }
                }
            }
        },
        onAdd = {
            val target = state.googleMap?.cameraPosition?.target
            state.addPointOrWarn(target?.let { GeofenceCoordinate(it.latitude, it.longitude) }, uiLanguage)
        },
        onAddFromGps = { onLocate(PendingLocate.AddPoint) }
    )
    state.points.getOrNull(state.selectedIndex)?.let { selectedPoint ->
        key(state.selectedIndex) {
            SelectedPointEditor(
                number = state.selectedIndex + 1,
                point = selectedPoint,
                onCoordinate = state::typeCoordinate,
                onDelete = state::deleteSelected
            )
        }
    }
    EditorActions(
        canUndo = state.undoStack.isNotEmpty(),
        showOrder = !state.isCircle && state.points.size >= 4,
        highlightOrder = issue == GeofenceDraftIssue.PolygonCrossing,
        canClear = state.points.isNotEmpty(),
        onUndo = state::undo,
        onOrder = {
            state.orderPolygon()
            state.fitCamera(animate = true, density = density)
        },
        onClear = onRequestClear
    )
    Text(
        text = if (mapShown) {
            tr(
                "Tap a number to select a point; long-press its marker on the map to drag it.",
                "Ketuk nomor untuk memilih titik; tekan lama penandanya di peta untuk menggeser."
            )
        } else {
            tr(
                "In Google Maps, long-press the spot and copy the coordinates, then paste them here.",
                "Di Google Maps, tekan lama lokasinya lalu salin koordinat, kemudian tempel di sini."
            )
        },
        color = AppColors.current.textSecondary,
        style = AppTextStyles.diagnostic
    )
    Spacer(Modifier.size(ScopedUiTokens.current.spaceXSmall))
}

private fun GeofenceEditorState.addPointOrWarn(coordinate: GeofenceCoordinate?, language: UiLanguage) {
    if (!addPoint(coordinate)) {
        showNotice(geofenceLimitMessage(language, shape))
    }
}

private fun GeofenceEditorState.requestLocate(
    target: PendingLocate,
    context: Context,
    language: UiLanguage,
    scope: CoroutineScope,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
    if (hasAnyLocationPermission(context)) {
        scope.launch { locate(context, language, target) }
    } else {
        pendingLocate = target
        permissionLauncher.launch(LocationPermissions)
    }
}

@SuppressLint("MissingPermission")
private suspend fun GeofenceEditorState.locate(context: Context, language: UiLanguage, target: PendingLocate) {
    if (!isLocationServicesEnabled(context)) {
        showNotice(
            localized(
                language,
                "Location is off. Turn on location in the quick settings, then try again.",
                "Lokasi HP sedang mati. Nyalakan lokasi di panel cepat, lalu coba lagi."
            )
        )
        return
    }
    locating = true
    val snapshot = runCatching {
        acquireBestEffortLocationSnapshot(context = context, preferFresh = true, geofenceConfig = null)
    }.getOrNull()
    locating = false
    if (snapshot == null) {
        showNotice(
            localized(
                language,
                "Your location is not available yet. Step outside or wait a moment, then try again.",
                "Lokasi belum didapat. Coba di tempat terbuka atau tunggu sebentar, lalu ulangi."
            )
        )
        return
    }
    googleMap?.let { runCatching { it.isMyLocationEnabled = true } }
    moveCamera(LatLng(snapshot.latitude, snapshot.longitude))
    if (target == PendingLocate.AddPoint) {
        addPointOrWarn(GeofenceCoordinate(snapshot.latitude, snapshot.longitude), language)
    }
}

private fun GeofenceEditorState.runSearch(
    context: Context,
    language: UiLanguage,
    scope: CoroutineScope,
    placesClient: PlacesClient?,
    sessionToken: AutocompleteSessionToken
) {
    val query = searchQuery.trim()
    clearSearch()
    // A pasted coordinate or Maps link needs no search: go straight there.
    parseCoordinateText(query)?.let { coordinate ->
        val latLng = LatLng(coordinate.latitude, coordinate.longitude)
        searchAnchor = MapSearchResult(placeId = "", title = query, subtitle = "", latLng = latLng)
        moveCamera(latLng)
        if (isCircle && points.isEmpty()) {
            addPointOrWarn(coordinate, language)
        }
        return
    }
    if (query.length < 3) {
        searchError = localized(language, "Type at least 3 letters.", "Ketik minimal 3 huruf.")
        return
    }
    if (placesClient == null && !Geocoder.isPresent()) {
        searchError = localized(
            language,
            "Place search is not available on this phone. Paste coordinates instead.",
            "Pencarian tempat tidak tersedia di HP ini. Tempel koordinat saja."
        )
        return
    }
    searchLoading = true
    scope.launch {
        val lookup = runCatching {
            searchMapLocations(context = context, placesClient = placesClient, query = query, sessionToken = sessionToken)
        }
        searchLoading = false
        lookup.onSuccess { result ->
            searchResults = result.results
            if (result.results.isEmpty()) {
                searchError = mapSearchFailureMessage(
                    throwable = result.failure,
                    defaultMessage = localized(language, "No matching place found.", "Tempat tidak ditemukan."),
                    configMessage = localized(
                        language,
                        "Place search is not set up for this build. Paste coordinates instead.",
                        "Pencarian tempat belum diatur di build ini. Tempel koordinat saja."
                    )
                )
            }
        }.onFailure {
            searchError = localized(language, "Search failed. Check the connection.", "Pencarian gagal. Periksa koneksi.")
        }
    }
}

private fun GeofenceEditorState.applySearchResult(
    result: MapSearchResult,
    language: UiLanguage,
    scope: CoroutineScope,
    placesClient: PlacesClient?,
    sessionToken: AutocompleteSessionToken
) {
    searchLoading = true
    scope.launch {
        val resolved = runCatching {
            if (result.latLng != null || placesClient == null) {
                result
            } else {
                resolvePlaceSearchResult(placesClient = placesClient, result = result, sessionToken = sessionToken)
            }
        }.getOrNull()
        searchLoading = false
        val latLng = resolved?.latLng
        if (resolved == null || latLng == null) {
            searchError = localized(language, "That place has no map position.", "Posisi tempat itu tidak tersedia.")
            return@launch
        }
        clearSearch()
        searchQuery = resolved.title
        searchAnchor = resolved
        moveCamera(latLng)
        if (isCircle && points.isEmpty()) {
            addPointOrWarn(GeofenceCoordinate(latLng.latitude, latLng.longitude), language)
        }
    }
}

internal fun geofenceLimitMessage(language: UiLanguage, shape: GeofenceShapeType): String =
    if (shape == GeofenceShapeType.Polygon) {
        localized(
            language,
            "At most $MaxPolygonPoints corners fit in one QR. Drag or delete a point instead.",
            "Maksimal $MaxPolygonPoints sudut agar muat di satu QR. Geser atau hapus titik yang ada."
        )
    } else {
        localized(
            language,
            "At most $MaxCircleCenters centers. Drag or delete a point instead.",
            "Maksimal $MaxCircleCenters titik pusat. Geser atau hapus titik yang ada."
        )
    }

internal fun geofenceIssueMessage(
    language: UiLanguage,
    shape: GeofenceShapeType,
    issue: GeofenceDraftIssue
): String = when (issue) {
    GeofenceDraftIssue.NeedPoints -> if (shape == GeofenceShapeType.Polygon) {
        localized(language, "Add at least 3 corners.", "Tambahkan minimal 3 sudut.")
    } else {
        localized(language, "Add at least 1 center point.", "Tambahkan minimal 1 titik pusat.")
    }
    GeofenceDraftIssue.InvalidPoint -> localized(
        language,
        "A point has no valid coordinates. Fill it in or delete it.",
        "Ada titik tanpa koordinat yang benar. Isi atau hapus titik itu."
    )
    GeofenceDraftIssue.TooManyPoints -> geofenceLimitMessage(language, shape)
    GeofenceDraftIssue.InvalidRadius -> localized(language, "Fill in the radius in meters.", "Isi radius dalam meter.")
    GeofenceDraftIssue.RadiusTooSmall -> localized(
        language,
        "Use a radius of at least $MinCircleRadiusMeters m; phone GPS is not more precise than that.",
        "Radius minimal $MinCircleRadiusMeters m; GPS HP tidak lebih tepat dari itu."
    )
    GeofenceDraftIssue.RadiusTooLarge -> localized(
        language,
        "The radius can be at most $MaxCircleRadiusMeters m.",
        "Radius maksimal $MaxCircleRadiusMeters m."
    )
    GeofenceDraftIssue.PolygonTooSmall -> localized(
        language,
        "The corners are almost in a line. Spread them around the area.",
        "Sudut-sudutnya hampir segaris. Sebar mengelilingi area."
    )
    GeofenceDraftIssue.PolygonCrossing -> localized(
        language,
        "The edges cross. Tap Fix order, or move the points round the area in turn.",
        "Garis area bersilangan. Ketuk Rapikan urutan, atau urutkan titik mengelilingi area."
    )
}

private fun hasAnyLocationPermission(context: Context): Boolean =
    LocationPermissions.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

private suspend fun MapView.awaitMap(): GoogleMap = suspendCancellableCoroutine { continuation ->
    getMapAsync { map -> if (continuation.isActive) continuation.resume(map) }
}

private fun configureEditorMap(
    map: GoogleMap,
    onTap: (LatLng) -> Unit,
    onSelect: (Int) -> Unit,
    onDragPreview: (Int, LatLng) -> Unit,
    onDragEnd: (Int, LatLng) -> Unit
) {
    // Our own zoom buttons sit with the other map tools; Google's would overlap them.
    map.uiSettings.isZoomControlsEnabled = false
    map.uiSettings.isMyLocationButtonEnabled = false
    map.uiSettings.isMapToolbarEnabled = false
    map.uiSettings.isTiltGesturesEnabled = false
    map.uiSettings.isRotateGesturesEnabled = false
    map.setOnMapClickListener { latLng -> onTap(latLng) }
    // A tap on a shop or school icon goes to the POI listener, not the map one; without
    // this, corners tapped on a labelled building were silently not added.
    map.setOnPoiClickListener { poi -> onTap(poi.latLng) }
    map.setOnMarkerClickListener { marker ->
        val index = marker.tag as? Int ?: return@setOnMarkerClickListener false
        onSelect(index)
        true
    }
    map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
        override fun onMarkerDragStart(marker: Marker) {
            (marker.tag as? Int)?.let { index ->
                onDragPreview(index, marker.position)
                onSelect(index)
            }
        }

        override fun onMarkerDrag(marker: Marker) {
            (marker.tag as? Int)?.let { index -> onDragPreview(index, marker.position) }
        }

        override fun onMarkerDragEnd(marker: Marker) {
            (marker.tag as? Int)?.let { index -> onDragEnd(index, marker.position) }
        }
    })
}

@Composable
private fun EditorTopBar(
    title: String,
    subtitle: String,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val colors = AppColors.current
    val action = primaryActionColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.cardBg)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = tr("Back", "Kembali"),
                tint = colors.textPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = AppTextStyles.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                style = AppTextStyles.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
            colors = ButtonDefaults.buttonColors(
                containerColor = action.container,
                contentColor = action.content,
                disabledContainerColor = colors.surfaceSoft,
                disabledContentColor = colors.textMuted
            ),
            modifier = Modifier
                .padding(end = 8.dp)
                .heightIn(min = 44.dp)
                .testTag(GeofenceEditorSaveTestTag)
        ) {
            Text(text = tr("Save", "Simpan"), style = AppTextStyles.button, maxLines = 1)
        }
    }
}

@Composable
private fun EditorSearchBar(
    query: String,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    val colors = AppColors.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.cardBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        singleLine = true,
        textStyle = AppTextStyles.bodyCompact.copy(color = colors.textPrimary),
        placeholder = {
            Text(
                text = tr("Search a place or paste coordinates", "Cari tempat atau tempel koordinat"),
                style = AppTextStyles.bodyCompact,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary) },
        trailingIcon = {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colors.blue
                )
                query.isNotEmpty() -> IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, contentDescription = tr("Clear", "Hapus"), tint = colors.textSecondary)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
        colors = editorFieldColors()
    )
}

@Composable
private fun SearchResultsCard(
    results: List<MapSearchResult>,
    error: String?,
    onSelect: (MapSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
        color = colors.cardBg,
        border = BorderStroke(1.dp, colors.outlineStrong),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            error?.let {
                Text(
                    text = it,
                    color = colors.issueText,
                    style = AppTextStyles.bodyCompact,
                    modifier = Modifier.padding(12.dp)
                )
            }
            results.forEach { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onSelect(result) }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = result.title,
                        color = colors.textPrimary,
                        style = AppTextStyles.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (result.subtitle.isNotBlank()) {
                        Text(
                            text = result.subtitle,
                            color = colors.textSecondary,
                            style = AppTextStyles.label,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapOverlayButtons(
    satellite: Boolean,
    locating: Boolean,
    canFit: Boolean,
    onToggleSatellite: () -> Unit,
    onFit: () -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapIconButton(
            icon = Icons.Rounded.Layers,
            label = if (satellite) tr("Show map", "Tampilkan peta") else tr("Show satellite", "Tampilkan satelit"),
            active = satellite,
            onClick = onToggleSatellite
        )
        if (canFit) {
            MapIconButton(icon = Icons.Rounded.ZoomOutMap, label = tr("Show all points", "Lihat semua titik"), onClick = onFit)
        }
        MapIconButton(
            icon = Icons.Rounded.MyLocation,
            label = tr("My location", "Lokasi saya"),
            busy = locating,
            onClick = onMyLocation
        )
    }
}

@Composable
private fun MapIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    busy: Boolean = false
) {
    val colors = AppColors.current
    Surface(
        modifier = Modifier
            .size(46.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
        color = if (active) colors.blueDark else colors.cardBg,
        border = BorderStroke(1.dp, colors.outlineStrong),
        shadowElevation = 3.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.blue)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) Color.White else colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MapHint(text: String, modifier: Modifier = Modifier) {
    val colors = AppColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
        color = colors.cardBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, colors.outlineStrong),
        shadowElevation = 3.dp
    ) {
        Text(
            text = text,
            color = colors.textPrimary,
            style = AppTextStyles.bodyCompact,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MapClosedCard(reason: String, canOpen: Boolean, onOpen: () -> Unit) {
    val colors = AppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ScopedUiTokens.current.radiusMedium),
        color = colors.surfaceSoft,
        border = BorderStroke(1.dp, colors.outlineMedium)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Map, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = reason, color = colors.textPrimary, style = AppTextStyles.bodyCompact)
                if (canOpen) {
                    OutlinedButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
                        border = BorderStroke(1.dp, colors.blue.copy(alpha = 0.6f))
                    ) {
                        Text(text = tr("Open map", "Buka peta"), color = colors.brandText, style = AppTextStyles.button)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = AppColors.current
    Column(
        modifier = modifier
            .background(colors.cardBg)
            .border(BorderStroke(1.dp, colors.outlineSubtle))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun StatusLine(notice: String?, issueText: String?, readyText: String) {
    val colors = AppColors.current
    val (icon, tint, text) = when {
        notice != null -> Triple(Icons.Rounded.Warning, colors.statusWarn, notice)
        issueText != null -> Triple(Icons.Rounded.Warning, colors.statusWarn, issueText)
        else -> Triple(Icons.Rounded.CheckCircle, colors.statusSafe, readyText)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ScopedUiTokens.current.radiusSmall))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = text, color = colors.textPrimary, style = AppTextStyles.bodyCompact, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RadiusEditor(
    radiusText: String,
    radiusMeters: Int?,
    onRadiusTextChange: (String) -> Unit
) {
    val colors = AppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tr("Radius", "Radius"), color = colors.textPrimary, style = AppTextStyles.cardTitle)
                Text(
                    text = tr("The same for every center", "Sama untuk semua titik pusat"),
                    color = colors.textSecondary,
                    style = AppTextStyles.label
                )
            }
            OutlinedTextField(
                value = radiusText,
                onValueChange = onRadiusTextChange,
                modifier = Modifier
                    .widthIn(min = 104.dp, max = 132.dp)
                    .testTag(GeofenceEditorRadiusTestTag),
                singleLine = true,
                textStyle = AppTextStyles.body.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold),
                suffix = { Text("m", color = colors.textSecondary, style = AppTextStyles.bodyCompact) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
                colors = editorFieldColors()
            )
        }
        Slider(
            value = radiusToSliderPosition(radiusMeters ?: DefaultCircleRadiusMeters),
            onValueChange = { position -> onRadiusTextChange(sliderPositionToRadius(position).toString()) },
            colors = SliderDefaults.colors(
                thumbColor = colors.blueDark,
                activeTrackColor = colors.blue,
                inactiveTrackColor = colors.outlineMedium
            ),
            modifier = Modifier.semantics { contentDescription = "Radius" }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PointsSection(
    shape: GeofenceShapeType,
    points: List<GeofenceVertex>,
    selectedIndex: Int,
    maxPoints: Int,
    mapOpen: Boolean,
    locating: Boolean,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onAddFromGps: () -> Unit
) {
    val colors = AppColors.current
    val full = points.size >= maxPoints
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (shape == GeofenceShapeType.Polygon) {
                tr("Corners (${points.size}/$maxPoints)", "Sudut area (${points.size}/$maxPoints)")
            } else {
                tr("Center points (${points.size}/$maxPoints)", "Titik pusat (${points.size}/$maxPoints)")
            },
            color = colors.textPrimary,
            style = AppTextStyles.cardTitle
        )
        if (points.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                points.forEachIndexed { index, point ->
                    PointChip(
                        number = index + 1,
                        selected = index == selectedIndex,
                        valid = point.toCoordinateOrNull() != null,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PanelButton(
                icon = Icons.Rounded.Add,
                label = if (mapOpen) tr("Add at map center", "Tambah di tengah peta") else tr("Add point", "Tambah titik"),
                enabled = !full,
                onClick = onAdd,
                modifier = Modifier.testTag(GeofenceEditorAddPointTestTag)
            )
            PanelButton(
                icon = Icons.Rounded.MyLocation,
                label = tr("Add my location", "Tambah lokasi saya"),
                enabled = !full && !locating,
                busy = locating,
                onClick = onAddFromGps
            )
        }
    }
}

@Composable
private fun PointChip(number: Int, selected: Boolean, valid: Boolean, onClick: () -> Unit) {
    val colors = AppColors.current
    val container = when {
        selected -> colors.gold
        else -> colors.surfaceSoft
    }
    val borderColor = when {
        !valid -> colors.statusDanger
        selected -> colors.goldDark
        else -> colors.outlineStrong
    }
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clip(RoundedCornerShape(ScopedUiTokens.current.radiusSmall))
            .background(container)
            .border(BorderStroke(if (selected || !valid) 2.dp else 1.dp, borderColor), RoundedCornerShape(ScopedUiTokens.current.radiusSmall))
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = if (selected) Color(0xFF1B1B1B) else colors.textPrimary,
            style = AppTextStyles.button.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun SelectedPointEditor(
    number: Int,
    point: GeofenceVertex,
    onCoordinate: (GeofenceCoordinate) -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppColors.current
    var text by remember { mutableStateOf(formatCoordinateText(point)) }
    var invalid by remember { mutableStateOf(false) }
    // A change from outside (tap, drag, GPS, undo) rewrites the field; the admin's own
    // typing does not, or the text would jump while they type.
    LaunchedEffect(point) {
        val typed = parseCoordinateText(text)
        val current = point.toCoordinateOrNull()
        if (typed == null || current == null || !typed.sameSpotAs(current)) {
            text = formatCoordinateText(point)
            invalid = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                val parsed = parseCoordinateText(value)
                // Only once a second value has started: "-7.02" on its way to a full
                // pair is not a mistake yet.
                invalid = parsed == null && value.trim().contains(CoordinateSeparator)
                parsed?.let(onCoordinate)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GeofenceEditorCoordinateTestTag),
            // Wraps rather than scrolling sideways, so the whole pair stays visible on a
            // narrow phone with large text.
            singleLine = false,
            maxLines = 3,
            label = { Text(tr("Point $number coordinates", "Koordinat titik $number"), maxLines = 1) },
            placeholder = { Text("-7.123456, 110.123456", color = colors.textMuted, maxLines = 1) },
            textStyle = AppTextStyles.bodyCompact.copy(color = colors.textPrimary, fontFamily = FontFamily.Monospace),
            isError = invalid,
            supportingText = {
                Text(
                    text = if (invalid) {
                        tr("Write it as latitude, longitude, e.g. -7.123456, 110.123456", "Tulis lintang, bujur, mis. -7.123456, 110.123456")
                    } else {
                        tr("Paste from Google Maps or type latitude, longitude.", "Tempel dari Google Maps atau ketik lintang, bujur.")
                    },
                    style = AppTextStyles.label
                )
            },
            // Text, not Decimal: many number pads have no minus key, and Indonesia's
            // latitudes are negative.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
            colors = editorFieldColors()
        )
        TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 44.dp)) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = colors.issueText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = tr("Delete point $number", "Hapus titik $number"), color = colors.issueText, style = AppTextStyles.button)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorActions(
    canUndo: Boolean,
    showOrder: Boolean,
    highlightOrder: Boolean,
    canClear: Boolean,
    onUndo: () -> Unit,
    onOrder: () -> Unit,
    onClear: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PanelButton(icon = Icons.AutoMirrored.Rounded.Undo, label = tr("Undo", "Urungkan"), enabled = canUndo, onClick = onUndo)
        if (showOrder) {
            PanelButton(
                icon = Icons.Rounded.AutoFixHigh,
                label = tr("Fix order", "Rapikan urutan"),
                emphasized = highlightOrder,
                onClick = onOrder
            )
        }
        PanelButton(
            icon = Icons.Rounded.DeleteSweep,
            label = tr("Remove all", "Hapus semua"),
            enabled = canClear,
            danger = true,
            onClick = onClear
        )
    }
}

@Composable
private fun PanelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    danger: Boolean = false,
    emphasized: Boolean = false
) {
    val colors = AppColors.current
    val content = when {
        danger -> colors.issueText
        emphasized -> colors.onDark
        else -> colors.brandText
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(ScopedUiTokens.current.radiusSmall),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (emphasized) colors.blueDark else Color.Transparent,
            contentColor = content,
            disabledContentColor = colors.textMuted
        ),
        border = BorderStroke(1.dp, if (enabled) content.copy(alpha = 0.45f) else colors.outlineSubtle),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.blue)
        } else {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = AppTextStyles.button)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBg,
        title = { Text(title, color = colors.textPrimary, style = AppTextStyles.cardTitle) },
        text = { Text(message, color = colors.textSecondary, style = AppTextStyles.bodyCompact) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = colors.issueText, style = AppTextStyles.button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = colors.brandText, style = AppTextStyles.button)
            }
        }
    )
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AppColors.current.surfaceSoft,
    unfocusedContainerColor = AppColors.current.surfaceSoft,
    focusedBorderColor = AppColors.current.blue,
    unfocusedBorderColor = AppColors.current.outlineMedium,
    focusedTextColor = AppColors.current.textPrimary,
    unfocusedTextColor = AppColors.current.textPrimary,
    cursorColor = AppColors.current.blue,
    focusedLabelColor = AppColors.current.brandText,
    unfocusedLabelColor = AppColors.current.textSecondary,
    focusedSupportingTextColor = AppColors.current.textSecondary,
    unfocusedSupportingTextColor = AppColors.current.textSecondary
)

private val CoordinateSeparator = Regex("""[,;\s]""")

/**
 * The slider is logarithmic: most schools need 50–300 m, which a linear 20–2000 m track
 * squeezed into its first sliver.
 */
internal fun radiusToSliderPosition(radiusMeters: Int): Float {
    val clamped = radiusMeters.coerceIn(MinCircleRadiusMeters, RadiusSliderMaxMeters).toDouble()
    return (ln(clamped / MinCircleRadiusMeters) / ln(RadiusSliderMaxMeters.toDouble() / MinCircleRadiusMeters))
        .toFloat()
        .coerceIn(0f, 1f)
}

internal fun sliderPositionToRadius(position: Float): Int {
    val raw = MinCircleRadiusMeters *
        (RadiusSliderMaxMeters.toDouble() / MinCircleRadiusMeters).pow(position.coerceIn(0f, 1f).toDouble())
    val step = when {
        raw < 200 -> 5
        raw < 1_000 -> 10
        else -> 50
    }
    return ((raw / step).roundToInt() * step).coerceIn(MinCircleRadiusMeters, RadiusSliderMaxMeters)
}

/** Two readings of one spot, allowing for the 6-decimal rounding the draft stores. */
private fun GeofenceCoordinate.sameSpotAs(other: GeofenceCoordinate): Boolean =
    abs(latitude - other.latitude) <= 1e-6 && abs(longitude - other.longitude) <= 1e-6
