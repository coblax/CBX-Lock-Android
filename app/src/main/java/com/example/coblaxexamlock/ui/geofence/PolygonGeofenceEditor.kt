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
internal fun PolygonGeofenceEditor(
    initialVertices: List<GeofenceVertex>,
    onDismiss: () -> Unit,
    onSave: (List<GeofenceVertex>) -> Unit
) {
    val context = LocalContext.current
    val mapsApiKey = remember { SecureStrings.mapsApiKey }
    val mapsReady = mapsApiKey.isNotBlank()
    val coroutineScope = rememberCoroutineScope()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var draftVertices by remember(initialVertices) { mutableStateOf(initialVertices.take(50)) }
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
    val placesClient = remember(mapsApiKey) { ensurePlacesSdkReady(context, mapsApiKey) }
    val searchSessionToken = remember { AutocompleteSessionToken.newInstance() }
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
        searchLoading = true
        runCatching {
            searchMapLocations(
                context = context,
                placesClient = placesClient,
                query = query,
                sessionToken = searchSessionToken
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
        searchLoading = true
        runCatching {
            if (result.latLng != null || resolvedPlacesClient == null) {
                result
            } else {
                resolvePlaceSearchResult(
                    placesClient = resolvedPlacesClient,
                    result = result,
                    sessionToken = searchSessionToken
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

    LaunchedEffect(mapsReady) {
        if (mapsReady) {
            runCatching { initializePlacesLegacy(context, mapsApiKey) }
        }
    }

    LaunchedEffect(mapsReady, initialVertices) {
        if (!mapsReady || initialVertices.isNotEmpty() || initialCameraResolved) {
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
            }
        }

        if (!mapsReady) {
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
                val latitudeText = lastPoint?.latitude ?: "-"
                val longitudeText = lastPoint?.longitude ?: "-"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(0.9f),
                        label = tr("Points", "Titik"),
                        value = "${draftVertices.size}/50"
                    )
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(1.2f),
                        label = tr("Latitude", "Latitude"),
                        value = latitudeText
                    )
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(1.2f),
                        label = tr("Longitude", "Longitude"),
                        value = longitudeText
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { draftVertices = draftVertices.dropLast(1) },
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
                        onClick = { draftVertices = emptyList() },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = draftVertices.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEFEF),
                            contentColor = Color(0xFFB42318)
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
                        onClick = { onSave(draftVertices) },
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
