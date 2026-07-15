package com.coblax.examlock.ui.geofence

import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle

import com.coblax.examlock.GeofenceConfig
import com.coblax.examlock.GeofenceVertex
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.runtime.acquireBestEffortLocationSnapshot
import com.coblax.examlock.runtime.hasFineLocationPermission
import com.coblax.examlock.runtime.isLocationServicesEnabled
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.ui.admin.StatusBanner
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockDangerBgSoft
import com.coblax.examlock.ui.theme.LockDialogDangerIcon
import com.coblax.examlock.ui.theme.LockOutlineSubtle
import com.coblax.examlock.validatePolygonVertices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.Places

import java.util.Date

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PolygonGeofenceEditor(
    initialVertices: List<GeofenceVertex>,
    onDismiss: () -> Unit,
    onSave: (List<GeofenceVertex>) -> Unit
) {
    val context = LocalContext.current
    val lowRamProfile = LocalLowRamProfile.current
    val mapsApiKey = remember { SecureStrings.mapsApiKey }
    val mapsReady = mapsApiKey.isNotBlank()
    val coroutineScope = rememberCoroutineScope()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var mapVisible by remember { mutableStateOf(!lowRamProfile.deferHeavyUi && !lowRamProfile.ultra) }
    var draftVertices by remember(initialVertices) { mutableStateOf(initialVertices.take(50)) }
    var saveValidationError by remember { mutableStateOf<String?>(null) }
    var searchedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<MapSearchResult>()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedSearchResult by remember { mutableStateOf<MapSearchResult?>(null) }
    var mapTypeSelection by remember { mutableStateOf(GeofenceMapType.Default) }
    var initialCameraLatLng by remember { mutableStateOf<LatLng?>(null) }
    var initialCameraResolved by remember { mutableStateOf(false) }
    var initialCameraApplied by remember { mutableStateOf(false) }
    val placesClient = remember(mapsApiKey, mapsReady, mapVisible) {
        if (mapsReady && mapVisible) {
            ensurePlacesSdkReady(context, mapsApiKey)
        } else {
            null
        }
    }
    val searchSessionToken = remember(mapVisible) {
        if (mapVisible) AutocompleteSessionToken.newInstance() else null
    }
    val latestVertices by rememberUpdatedState(draftVertices)
    val maxPointsMessage = tr("Maximum 50 polygon points.", "Maksimal 50 titik polygon.")
    val searchFailedMessage = tr("Location search failed.", "Pencarian lokasi gagal.")
    val searchQueryRequiredMessage = tr(
        "Enter at least 3 characters to search.",
        "Masukkan minimal 3 karakter untuk mencari."
    )
    val searchNoResultMessage = tr(
        "No matching locations found.",
        "Lokasi yang cocok tidak ditemukan."
    )
    val searchConfigMessage = tr(
        "Location search is not ready. Check Google Maps / Places API key configuration.",
        "Pencarian lokasi belum siap. Periksa konfigurasi API key Google Maps / Places."
    )

    fun GeofenceVertex.toLatLngOrNull(): LatLng? {
        val latitude = latitude.trim().toDoubleOrNull() ?: return null
        val longitude = longitude.trim().toDoubleOrNull() ?: return null
        return LatLng(latitude, longitude)
    }

    fun updateLastCoordinate(
        latitude: String? = null,
        longitude: String? = null
    ) {
        val currentVertices = latestVertices.toMutableList()
        if (currentVertices.isEmpty()) {
            currentVertices += GeofenceVertex(
                latitude = latitude.orEmpty(),
                longitude = longitude.orEmpty()
            )
        } else {
            val targetIndex = currentVertices.lastIndex
            val current = currentVertices[targetIndex]
            currentVertices[targetIndex] = current.copy(
                latitude = latitude ?: current.latitude,
                longitude = longitude ?: current.longitude
            )
        }
        draftVertices = currentVertices.take(50)
    }

    suspend fun runSearch() {
        val query = searchQuery.trim()
        searchResults = emptyList()
        searchError = null
        if (query.length < 3) {
            searchError = searchQueryRequiredMessage
            return
        }
        if (!mapsReady && !Geocoder.isPresent()) {
            searchError = searchConfigMessage
            return
        }
        val activeSearchSessionToken = searchSessionToken ?: run {
            searchError = searchConfigMessage
            return
        }
        searchLoading = true
        runCatching {
            searchMapLocations(
                context = context,
                placesClient = placesClient,
                query = query,
                sessionToken = activeSearchSessionToken
            )
        }.onSuccess { lookup ->
            searchResults = lookup.results
            if (lookup.results.isEmpty()) {
                searchError = lookup.failure?.let {
                    mapSearchFailureMessage(
                        throwable = it,
                        defaultMessage = searchNoResultMessage,
                        configMessage = searchConfigMessage
                    )
                } ?: searchNoResultMessage
            } else {
                searchError = null
            }
        }.onFailure {
            searchError = mapSearchFailureMessage(
                throwable = it,
                defaultMessage = searchFailedMessage,
                configMessage = searchConfigMessage
            )
        }
        searchLoading = false
    }

    suspend fun applySearchResult(result: MapSearchResult) {
        val resolvedPlacesClient = placesClient
        val activeSearchSessionToken = searchSessionToken ?: run {
            searchError = searchConfigMessage
            return
        }
        searchLoading = true
        runCatching {
            if (result.latLng != null || resolvedPlacesClient == null) {
                result
            } else {
                resolvePlaceSearchResult(
                    placesClient = resolvedPlacesClient,
                    result = result,
                    sessionToken = activeSearchSessionToken
                )
            }
        }.onSuccess { resolvedResult ->
            val latLng = resolvedResult.latLng
            if (latLng != null) {
                selectedSearchResult = resolvedResult
                searchedLatLng = latLng
                searchQuery = resolvedResult.title
                searchResults = emptyList()
                searchError = null
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            } else {
                searchError = searchFailedMessage
            }
        }.onFailure {
            searchError = mapSearchFailureMessage(
                throwable = it,
                defaultMessage = searchFailedMessage,
                configMessage = searchConfigMessage
            )
        }
        searchLoading = false
    }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(mapsReady, mapVisible) {
        if (mapsReady && mapVisible) {
            runCatching { initializePlacesLegacy(context, mapsApiKey) }
        }
    }

    LaunchedEffect(mapsReady, mapVisible, initialVertices) {
        if (
            lowRamProfile.deferHeavyUi ||
            !mapVisible ||
            !mapsReady ||
            initialVertices.isNotEmpty() ||
            initialCameraResolved
        ) {
            return@LaunchedEffect
        }
        initialCameraResolved = true
        if (!hasFineLocationPermission(context) || !isLocationServicesEnabled(context)) {
            return@LaunchedEffect
        }
        val snapshot = acquireBestEffortLocationSnapshot(
            context = context,
            preferFresh = false,
            geofenceConfig = null
        )
        initialCameraLatLng = snapshot?.let { LatLng(it.latitude, it.longitude) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LockBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, LockOutlineSubtle, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (mapVisible) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactBackIconButton(onClick = onDismiss)
                        InlineMapSearchBar(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                if (it.isBlank()) {
                                    searchResults = emptyList()
                                    searchError = null
                                    selectedSearchResult = null
                                    searchedLatLng = null
                                }
                            },
                            onSearch = {
                                coroutineScope.launch { runSearch() }
                            },
                            loading = searchLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    InlineMapSearchResults(
                        results = searchResults,
                        error = searchError,
                        onSelect = { result ->
                            coroutineScope.launch { applySearchResult(result) }
                        }
                    )
                    selectedSearchResult?.let { result ->
                        val targetText = buildString {
                            append("Target: ")
                            append(result.title)
                            result.subtitle.takeIf { it.isNotBlank() }?.let { subtitle ->
                                append(" | ")
                                append(subtitle)
                            }
                        }
                        Text(
                            text = tr(targetText, targetText),
                            color = LockTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            maxLines = 2
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactBackIconButton(onClick = onDismiss)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Manual polygon geofence", "Input manual geofence polygon"),
                                color = LockTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Add and edit boundary points first; open the map only when needed.",
                                    "Tambah dan edit titik batas dulu; buka map hanya saat dibutuhkan."
                                ),
                                color = LockTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }

        if (!mapVisible) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, LockOutline.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr("Map is paused", "Map dijeda"),
                            color = LockTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                "This saves memory on low-RAM devices. Manual points below save the same policy.",
                                "Ini menghemat memori di perangkat low-RAM. Titik manual di bawah tetap menyimpan policy yang sama."
                            ),
                            color = LockTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { mapVisible = true },
                            enabled = mapsReady && !lowRamProfile.ultra,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LockBlue,
                                contentColor = LockOnDark
                            )
                        ) {
                            Text(
                                text = if (lowRamProfile.ultra) tr("Disabled on Ultra RAM", "Dinonaktifkan di Ultra RAM") else tr("Open Map", "Buka Map"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (!mapsReady) {
            StatusBanner(
                message = tr(
                    "Google Maps API key is not configured. Add maps.api.key in local.properties to enable the polygon map editor.",
                    "Google Maps API key belum dikonfigurasi. Tambahkan maps.api.key di local.properties untuk mengaktifkan editor polygon."
                ),
                isError = true
            )
        } else {
            val mapView = remember {
                MapView(context).apply {
                    onCreate(Bundle())
                }
            }
            DisposableEffect(mapView) {
                mapView.startGeofenceMapLifecycle()
                onDispose {
                    val activeMap = googleMap
                    googleMap = null
                    searchResults = emptyList()
                    searchLoading = false
                    searchError = null
                    searchedLatLng = null
                    selectedSearchResult = null
                    mapView.disposeGeofenceMapLifecycle(activeMap)
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = {
                        mapView.apply {
                            getMapAsync { map ->
                                googleMap = map
                                map.mapType = mapTypeSelection.googleMapType
                                map.uiSettings.isZoomControlsEnabled = true
                                map.uiSettings.isMyLocationButtonEnabled = false
                                val initialLatLng = draftVertices.lastOrNull()?.toLatLngOrNull()
                                    ?: searchedLatLng
                                    ?: initialCameraLatLng
                                    ?: LatLng(-2.5489, 118.0149)
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        initialLatLng,
                                        if (draftVertices.isEmpty() && searchedLatLng == null && initialCameraLatLng == null) 4.5f else 18f
                                    )
                                )
                                initialCameraApplied = initialCameraLatLng != null
                                map.setOnMapClickListener { latLng ->
                                    searchResults = emptyList()
                                    searchError = null
                                    if (latestVertices.size >= 50) {
                                        android.widget.Toast.makeText(
                                            context,
                                            maxPointsMessage,
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        draftVertices = latestVertices + GeofenceVertex(
                                            latitude = formatCoordinateForPolicy(latLng.latitude),
                                            longitude = formatCoordinateForPolicy(latLng.longitude)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                MapTypeSelectorOverlay(
                    selectedType = mapTypeSelection,
                    onTypeSelected = { type ->
                        mapTypeSelection = type
                        googleMap?.mapType = type.googleMapType
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp)
                )
            }
        }

        LaunchedEffect(mapTypeSelection, googleMap) {
            googleMap?.mapType = mapTypeSelection.googleMapType
        }

        LaunchedEffect(googleMap, initialCameraLatLng, draftVertices, searchedLatLng) {
            val map = googleMap ?: return@LaunchedEffect
            val target = initialCameraLatLng ?: return@LaunchedEffect
            if (initialCameraApplied || draftVertices.isNotEmpty() || searchedLatLng != null) {
                return@LaunchedEffect
            }
            initialCameraApplied = true
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 18f))
        }

        LaunchedEffect(googleMap, draftVertices, searchedLatLng, selectedSearchResult) {
            val map = googleMap ?: return@LaunchedEffect
            // Debounce rapid updates (e.g. typing coordinates) to avoid excessive map redraws
            delay(150)
            map.clear()
            map.mapType = mapTypeSelection.googleMapType
            searchedLatLng?.let {
                map.addMarker(
                    MarkerOptions()
                        .position(it)
                        .title(selectedSearchResult?.title ?: "Search result")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }
            val points = draftVertices.mapNotNull { vertex ->
                vertex.toLatLngOrNull()
            }
            points.forEachIndexed { index, latLng ->
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Point ${index + 1}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )
            }
            when {
                points.size >= 3 -> {
                    map.addPolygon(
                        PolygonOptions()
                            .addAll(points)
                            .strokeColor(LockBlue.toArgb())
                            .fillColor(LockBlue.copy(alpha = 0.18f).toArgb())
                            .strokeWidth(5f)
                    )
                }
                points.size >= 2 -> {
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(LockBlue.toArgb())
                            .width(5f)
                    )
                }
            }
            if (points.size > 1) {
                val boundsBuilder = LatLngBounds.Builder()
                points.forEach(boundsBuilder::include)
                runCatching {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 96))
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, LockOutlineSubtle, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tr(
                        "Tap map to add polygon boundary points.",
                        "Tap map untuk menambah titik batas polygon."
                    ),
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 2
                )

                val lastPoint = draftVertices.lastOrNull()
                val latitudeText = lastPoint?.latitude.orEmpty()
                val longitudeText = lastPoint?.longitude.orEmpty()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(0.9f),
                        label = tr("Points", "Titik"),
                        value = "${draftVertices.size}/50"
                    )
                    CompactCoordinateMetricCard(
                        modifier = Modifier.weight(1.2f),
                        label = tr("Latitude", "Latitude"),
                        value = latitudeText,
                        onValueChange = { updateLastCoordinate(latitude = it) }
                    )
                    CompactCoordinateMetricCard(
                        modifier = Modifier.weight(1.2f),
                        label = tr("Longitude", "Longitude"),
                        value = longitudeText,
                        onValueChange = { updateLastCoordinate(longitude = it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            if (draftVertices.size < 50) {
                                draftVertices = draftVertices + GeofenceVertex("", "")
                                saveValidationError = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftVertices.size < 50,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockBlue,
                            contentColor = LockOnDark
                        ),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(
                            tr("Add", "Tambah"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            draftVertices = draftVertices.dropLast(1)
                            saveValidationError = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftVertices.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockSurfaceSoft,
                            contentColor = LockTextPrimary
                        )
                    ) {
                        Text(
                            tr("Undo", "Undo"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            draftVertices = emptyList()
                            saveValidationError = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftVertices.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockDangerBgSoft,
                            contentColor = LockDialogDangerIcon
                        )
                    ) {
                        Text(
                            tr("Clear", "Hapus"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            val error = validatePolygonVertices(draftVertices)
                            if (error != null) {
                                saveValidationError = when (error) {
                                    "polygon_min_3_vertices" -> "Need at least 3 valid points."
                                    "polygon_degenerate" -> "Polygon area is too small."
                                    "polygon_self_intersecting" -> "Lines must not cross each other."
                                    else -> "Invalid polygon: $error"
                                }
                            } else {
                                saveValidationError = null
                                onSave(draftVertices)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftVertices.size >= 3,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockGold,
                            contentColor = LockTextPrimary
                        )
                    ) {
                        Text(
                            tr("Save", "Simpan"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }

                saveValidationError?.let { errorMsg ->
                    Text(
                        text = "âš  $errorMsg",
                        color = LockDialogDangerIcon,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}
