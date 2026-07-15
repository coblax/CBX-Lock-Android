package com.coblax.examlock.ui.geofence

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
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
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.LockDangerBgSoft
import com.coblax.examlock.ui.theme.LockDialogDangerIcon
import com.coblax.examlock.ui.theme.LockBlueFill
import com.coblax.examlock.ui.theme.LockOutlineSubtle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
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
@SuppressLint("MissingPermission")
internal fun CircleGeofenceEditorScreen(
    initialCenters: List<GeofenceVertex>,
    initialRadiusMeters: String,
    onDismiss: () -> Unit,
    onSave: (List<GeofenceVertex>, String) -> Unit
) {
    val context = LocalContext.current
    val lowRamProfile = LocalLowRamProfile.current
    val mapsApiKey = remember { SecureStrings.mapsApiKey }
    val mapsReady = mapsApiKey.isNotBlank()
    val coroutineScope = rememberCoroutineScope()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var mapVisible by remember { mutableStateOf(!lowRamProfile.deferHeavyUi && !lowRamProfile.ultra) }
    var draftCenters by remember(initialCenters) { mutableStateOf(initialCenters.take(5)) }
    var draftRadiusMeters by remember(initialRadiusMeters) { mutableStateOf(initialRadiusMeters) }
    var selectedIndex by remember(initialCenters) {
        mutableIntStateOf(if (initialCenters.isNotEmpty()) 0 else -1)
    }
    var searchedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<MapSearchResult>()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedSearchResult by remember { mutableStateOf<MapSearchResult?>(null) }
    var mapTypeSelection by remember { mutableStateOf(GeofenceMapType.Default) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var precisePermissionGranted by remember { mutableStateOf(hasFineLocationPermission(context)) }
    var requestingLocation by remember { mutableStateOf(false) }
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
    val latestCenters by rememberUpdatedState(draftCenters)
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val maxPointsMessage = tr("Maximum 5 circle centers.", "Maksimal 5 titik center circle.")
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
    val permissionNeededMessage = tr(
        "Precise location permission is needed to use current location. You can still place points manually.",
        "Izin lokasi presisi diperlukan untuk memakai lokasi saat ini. Anda tetap bisa memilih titik secara manual."
    )
    val locationDisabledMessage = tr(
        "Location services are off. Turn on GPS/location or place the points manually.",
        "Layanan lokasi sedang nonaktif. Aktifkan GPS/lokasi atau pilih titik secara manual."
    )
    val noLocationFixMessage = tr(
        "Current location is not available yet. Try again or place the points manually.",
        "Lokasi saat ini belum tersedia. Coba lagi atau pilih titik secara manual."
    )
    val currentLocationLoadedMessage = tr(
        "Current location loaded into the selected point.",
        "Lokasi saat ini dimuat ke titik yang dipilih."
    )
    val noPointSelectedMessage = tr(
        "There is no selected point yet. Add a point first.",
        "Belum ada titik yang dipilih. Tambahkan titik dulu."
    )

    fun GeofenceVertex.toLatLngOrNull(): LatLng? {
        val latitude = latitude.trim().toDoubleOrNull() ?: return null
        val longitude = longitude.trim().toDoubleOrNull() ?: return null
        return LatLng(latitude, longitude)
    }

    fun LatLng.toGeofenceVertex(): GeofenceVertex {
        return GeofenceVertex(
            latitude = formatCoordinateForPolicy(latitude),
            longitude = formatCoordinateForPolicy(longitude)
        )
    }

    fun selectIndexIfNeeded(targetIndex: Int) {
        selectedIndex = when {
            latestCenters.isEmpty() -> -1
            targetIndex < 0 -> 0
            targetIndex >= latestCenters.size -> latestCenters.lastIndex
            else -> targetIndex
        }
    }

    fun updateSelectedPoint(latLng: LatLng) {
        val currentCenters = latestCenters
        if (currentCenters.isEmpty()) {
            draftCenters = listOf(latLng.toGeofenceVertex())
            selectedIndex = 0
            return
        }
        val targetIndex = latestSelectedIndex.takeIf { it in currentCenters.indices } ?: 0
        val updatedCenters = currentCenters.toMutableList()
        updatedCenters[targetIndex] = latLng.toGeofenceVertex()
        draftCenters = updatedCenters
        selectedIndex = targetIndex
    }

    fun updateSelectedCoordinate(
        latitude: String? = null,
        longitude: String? = null
    ) {
        val currentCenters = latestCenters.toMutableList()
        val targetIndex = selectedIndex.takeIf { it in currentCenters.indices }
            ?: currentCenters.indices.firstOrNull()
            ?: 0
        if (currentCenters.isEmpty()) {
            currentCenters += GeofenceVertex(
                latitude = latitude.orEmpty(),
                longitude = longitude.orEmpty()
            )
        } else {
            val current = currentCenters[targetIndex]
            currentCenters[targetIndex] = current.copy(
                latitude = latitude ?: current.latitude,
                longitude = longitude ?: current.longitude
            )
        }
        draftCenters = currentCenters.take(5)
        selectedIndex = targetIndex.coerceAtMost(draftCenters.lastIndex)
    }

    fun addPoint(latLng: LatLng) {
        val currentCenters = latestCenters
        if (currentCenters.size >= 5) {
            android.widget.Toast.makeText(
                context,
                maxPointsMessage,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        draftCenters = currentCenters + latLng.toGeofenceVertex()
        selectedIndex = currentCenters.size
    }

    fun moveOrCreatePoint(
        latLng: LatLng,
        updateSearchAnchor: Boolean
    ) {
        if (updateSearchAnchor) {
            searchedLatLng = latLng
        }
        if (latestCenters.isEmpty()) {
            addPoint(latLng)
        } else {
            updateSelectedPoint(latLng)
        }
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
    }

    suspend fun placeCurrentLocation(resetPointsFirst: Boolean = false) {
        if (!precisePermissionGranted) {
            infoMessage = permissionNeededMessage
            return
        }
        if (!isLocationServicesEnabled(context)) {
            if (resetPointsFirst) {
                draftCenters = emptyList()
                selectedIndex = -1
            }
            infoMessage = locationDisabledMessage
            return
        }
        requestingLocation = true
        val locationSnapshot = acquireBestEffortLocationSnapshot(
            context = context,
            preferFresh = true,
            geofenceConfig = null
        )
        requestingLocation = false
        if (locationSnapshot == null) {
            if (resetPointsFirst) {
                draftCenters = emptyList()
                selectedIndex = -1
            }
            infoMessage = noLocationFixMessage
            return
        }
        val latLng = LatLng(locationSnapshot.latitude, locationSnapshot.longitude)
        if (resetPointsFirst) {
            draftCenters = emptyList()
            selectedIndex = -1
        }
        moveOrCreatePoint(latLng, updateSearchAnchor = false)
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
                moveOrCreatePoint(latLng, updateSearchAnchor = false)
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        precisePermissionGranted = granted
        if (granted) {
            coroutineScope.launch {
                placeCurrentLocation(resetPointsFirst = latestCenters.isEmpty())
            }
        } else {
            infoMessage = permissionNeededMessage
        }
    }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(mapsReady, mapVisible) {
        if (mapsReady && mapVisible) {
            runCatching { initializePlacesLegacy(context, mapsApiKey) }
        }
    }

    LaunchedEffect(draftCenters) {
        selectedIndex = when {
            draftCenters.isEmpty() -> -1
            selectedIndex !in draftCenters.indices -> draftCenters.lastIndex.coerceAtLeast(0)
            else -> selectedIndex
        }
    }

    LaunchedEffect(Unit) {
        if (!lowRamProfile.deferHeavyUi && draftCenters.isEmpty()) {
            if (precisePermissionGranted) {
                placeCurrentLocation(resetPointsFirst = false)
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
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
                                text = tr("Manual circle geofence", "Input manual geofence lingkaran"),
                                color = LockTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Edit coordinates and radius first; open the map only when needed.",
                                    "Edit koordinat dan radius dulu; buka map hanya saat dibutuhkan."
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
                                "This saves memory on low-RAM devices. Manual coordinates below save the same policy.",
                                "Ini menghemat memori di perangkat low-RAM. Koordinat manual di bawah tetap menyimpan policy yang sama."
                            ),
                            color = LockTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { mapVisible = true },
                                modifier = Modifier.weight(1f),
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
                            Button(
                                onClick = {
                                    if (precisePermissionGranted) {
                                        coroutineScope.launch {
                                            placeCurrentLocation(resetPointsFirst = latestCenters.isEmpty())
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LockSurfaceSoft,
                                    contentColor = LockTextPrimary
                                ),
                                border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.8f))
                            ) {
                                Text(tr("Use Current", "Pakai Lokasi"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (!mapsReady) {
            StatusBanner(
                message = tr(
                    "Google Maps API key is not configured. Add maps.api.key in local.properties to enable the full map editor.",
                    "Google Maps API key belum dikonfigurasi. Tambahkan maps.api.key di local.properties untuk mengaktifkan editor map penuh."
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
                                if (precisePermissionGranted) {
                                    runCatching { map.isMyLocationEnabled = true }
                                }
                                val initialLatLng = draftCenters.firstOrNull()?.toLatLngOrNull()
                                    ?: searchedLatLng
                                    ?: LatLng(-2.5489, 118.0149)
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        initialLatLng,
                                        if (draftCenters.isEmpty() && searchedLatLng == null) 5f else 18f
                                    )
                                )
                                map.setOnMapClickListener { latLng ->
                                    searchResults = emptyList()
                                    searchError = null
                                    if (latestCenters.isEmpty()) {
                                        addPoint(latLng)
                                    } else {
                                        updateSelectedPoint(latLng)
                                    }
                                }
                                map.setOnMarkerClickListener { marker ->
                                    val targetIndex = (marker.tag as? Int) ?: return@setOnMarkerClickListener false
                                    selectedIndex = targetIndex
                                    true
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

        LaunchedEffect(googleMap, draftCenters, draftRadiusMeters, selectedIndex, searchedLatLng, selectedSearchResult, precisePermissionGranted) {
            val map = googleMap ?: return@LaunchedEffect
            // Debounce rapid updates (e.g. typing radius/coordinates) to avoid excessive map redraws
            delay(150)
            map.clear()
            map.mapType = mapTypeSelection.googleMapType
            if (precisePermissionGranted) {
                runCatching { map.isMyLocationEnabled = true }
            }
            searchedLatLng?.let { anchor ->
                map.addMarker(
                    MarkerOptions()
                        .position(anchor)
                        .title(selectedSearchResult?.title ?: "Search result")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }
            val radiusMeters = draftRadiusMeters.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
            val points = draftCenters.mapNotNull { it.toLatLngOrNull() }
            points.forEachIndexed { index, latLng ->
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Point ${index + 1}")
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                if (index == selectedIndex) {
                                    BitmapDescriptorFactory.HUE_RED
                                } else {
                                    BitmapDescriptorFactory.HUE_ORANGE
                                }
                            )
                        )
                )?.tag = index
                radiusMeters?.let { radius ->
                    map.addCircle(
                        CircleOptions()
                            .center(latLng)
                            .radius(radius)
                            .strokeColor(LockBlue.toArgb())
                            .fillColor(LockBlueFill.toArgb())
                            .strokeWidth(4f)
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
                if (requestingLocation) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LockBlue
                        )
                        Text(
                            text = tr("Resolving current location...", "Mencari lokasi saat ini..."),
                            color = LockTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                val showInlineInfo = infoMessage != null &&
                    infoMessage != currentLocationLoadedMessage
                showInlineInfo.takeIf { it }?.let {
                    val message = infoMessage ?: return@let
                    Text(
                        text = message,
                        color = if (!requestingLocation) {
                            LockDialogDangerIcon
                        } else {
                            LockBlueDeep
                        },
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }

                val selectedPoint = draftCenters.getOrNull(selectedIndex)
                val latitudeText = selectedPoint?.latitude.orEmpty()
                val longitudeText = selectedPoint?.longitude.orEmpty()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(0.8f),
                        label = tr("Point", "Titik"),
                        value = if (draftCenters.isEmpty()) "0/5" else "${selectedIndex + 1}/${draftCenters.size}"
                    )
                    CompactCoordinateMetricCard(
                        modifier = Modifier.weight(1.1f),
                        label = tr("Latitude", "Latitude"),
                        value = latitudeText,
                        onValueChange = { updateSelectedCoordinate(latitude = it) }
                    )
                    CompactCoordinateMetricCard(
                        modifier = Modifier.weight(1.1f),
                        label = tr("Longitude", "Longitude"),
                        value = longitudeText,
                        onValueChange = { updateSelectedCoordinate(longitude = it) }
                    )
                    CompactRadiusMetricCard(
                        modifier = Modifier.weight(0.9f),
                        value = draftRadiusMeters,
                        onValueChange = { draftRadiusMeters = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            val mapTarget = googleMap?.cameraPosition?.target
                                ?: searchedLatLng
                                ?: latestCenters.lastOrNull()?.toLatLngOrNull()
                                ?: LatLng(-2.5489, 118.0149)
                            addPoint(mapTarget)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftCenters.size < 5,
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
                            if (draftCenters.isEmpty()) {
                                infoMessage = noPointSelectedMessage
                                return@Button
                            }
                            val targetIndex = selectedIndex.takeIf { it in draftCenters.indices }
                                ?: draftCenters.lastIndex
                            val updated = draftCenters.toMutableList().apply { removeAt(targetIndex) }
                            draftCenters = updated
                            selectedIndex = when {
                                updated.isEmpty() -> -1
                                targetIndex > updated.lastIndex -> updated.lastIndex
                                else -> targetIndex
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftCenters.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockSurfaceSoft,
                            contentColor = LockTextPrimary
                        )
                    ) {
                        Text(
                            tr("Delete", "Hapus"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            draftCenters = emptyList()
                            selectedIndex = -1
                            infoMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockDangerBgSoft,
                            contentColor = LockDialogDangerIcon
                        )
                    ) {
                        Text(
                            tr("Reset", "Reset"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            val validCenters = draftCenters.filter { vertex ->
                                val lat = vertex.latitude.trim().toDoubleOrNull()
                                val lng = vertex.longitude.trim().toDoubleOrNull()
                                lat != null && lng != null &&
                                    lat in -90.0..90.0 && lng in -180.0..180.0
                            }
                            if (validCenters.isEmpty()) {
                                infoMessage = "Add at least one valid coordinate point."
                            } else {
                                onSave(validCenters.take(5), draftRadiusMeters)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftCenters.isNotEmpty() && draftRadiusMeters.trim().toDoubleOrNull()?.let { it > 0.0 } == true,
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
            }
        }
    }
}
