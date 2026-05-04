package com.example.coblaxexamlock.ui.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.style.CharacterStyle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.coblaxexamlock.GeofenceConfig
import com.example.coblaxexamlock.GeofencePoint
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceShapeType
import com.example.coblaxexamlock.GeofenceVertex
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.runtime.acquireBestEffortLocationSnapshot
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.ui.admin.StatusBanner
import com.example.coblaxexamlock.ui.theme.COBLAXEXAMLOCKTheme
import com.example.coblaxexamlock.ui.theme.LockBackground
import com.example.coblaxexamlock.ui.theme.LockBlue
import com.example.coblaxexamlock.ui.theme.LockBlueDeep
import com.example.coblaxexamlock.ui.theme.LockBlueMid
import com.example.coblaxexamlock.ui.theme.LockBlueSoft
import com.example.coblaxexamlock.ui.theme.LockGold
import com.example.coblaxexamlock.ui.theme.LockGoldDark
import com.example.coblaxexamlock.ui.theme.LockOnDark
import com.example.coblaxexamlock.ui.theme.LockOutline
import com.example.coblaxexamlock.ui.theme.LockSurface
import com.example.coblaxexamlock.ui.theme.LockSurfaceSoft
import com.example.coblaxexamlock.ui.theme.LockTextMuted
import com.example.coblaxexamlock.ui.theme.LockTextPrimary
import com.example.coblaxexamlock.ui.theme.LockTextSecondary
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    var mapVisible by remember { mutableStateOf(!lowRamProfile.deferHeavyUi) }
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
        Surface(
            color = Color.White,
            shadowElevation = 6.dp
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
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.8f))
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
                                enabled = mapsReady,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LockBlue,
                                    contentColor = LockOnDark
                                )
                            ) {
                                Text(tr("Open Map", "Buka Map"), fontWeight = FontWeight.Bold)
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
                            .fillColor(LockBlue.copy(alpha = 0.12f).toArgb())
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

        Surface(
            color = Color.White,
            shadowElevation = 8.dp
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
                            Color(0xFFB42318)
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
                            containerColor = Color(0xFFFFEFEF),
                            contentColor = Color(0xFFB42318)
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
                        onClick = { onSave(draftCenters.take(5), draftRadiusMeters) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
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
