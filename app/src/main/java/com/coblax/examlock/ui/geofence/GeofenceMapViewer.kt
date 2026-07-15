package com.coblax.examlock.ui.geofence

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle

import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.format.formatGeofenceDistance
import com.coblax.examlock.formatCoordinates
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.i18n.tr
import com.coblax.examlock.LocalLowRamProfile
import com.coblax.examlock.ui.theme.LockBackground
import com.coblax.examlock.ui.theme.LockBlue
import com.coblax.examlock.ui.theme.LockBlueDeep
import com.coblax.examlock.ui.theme.LockGold
import com.coblax.examlock.ui.theme.LockOnDark
import com.coblax.examlock.ui.theme.LockOutline
import com.coblax.examlock.ui.theme.LockSurface
import com.coblax.examlock.ui.theme.LockSurfaceSoft
import com.coblax.examlock.ui.theme.LockTextMuted
import com.coblax.examlock.ui.theme.LockTextPrimary
import com.coblax.examlock.ui.theme.LockTextSecondary
import com.coblax.examlock.ui.theme.UiTokens
import com.coblax.examlock.ui.theme.flatPill
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
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions

import java.util.Date
import java.util.Locale

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun GeofenceMapViewerScreen(
    runtimeStatus: GeofenceRuntimeStatus,
    isRefreshingLocation: Boolean,
    onDismiss: () -> Unit,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lowRamProfile = LocalLowRamProfile.current
    if (lowRamProfile.ultra) {
        GeofenceMapViewerTextFallback(
            runtimeStatus = runtimeStatus,
            isRefreshingLocation = isRefreshingLocation,
            onDismiss = onDismiss,
            onRefreshLocation = onRefreshLocation,
            modifier = modifier
        )
        return
    }
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var mapTypeSelection by remember { mutableStateOf(GeofenceMapType.Default) }
    val mapView = remember {
        MapView(context).apply {
            id = View.generateViewId()
            onCreate(null)
        }
    }
    val currentLatLng = runtimeStatus.evaluation.locationSnapshot?.let {
        LatLng(it.latitude, it.longitude)
    }
    val closestCenter = runtimeStatus.evaluation.closestCircleCenter
    val circleCenters = effectiveCircleCenters(runtimeStatus.evaluation.config)
    val fixQuality = runtimeStatus.securityStatus.fixQualityStatus
    val summaryDistance = formatGeofenceDistance(runtimeStatus.evaluation.distanceMeters)
    val summaryCoordinates = currentLatLng?.let { formatCoordinates(it.latitude, it.longitude) } ?: "-"
    val providerText = runtimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"
    val accuracyText = runtimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
        String.format(Locale.US, "%.1f m", it)
    } ?: "-"
    val verdictText = runtimeStatus.securityStatus.finalVerdict.diagnosticLabel()
    val fixQualityText = fixQuality.verdict.diagnosticLabel()
    val shapeText = runtimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"
    val policyText = runtimeStatus.policySource.diagnosticLabel()
    val shapeBadgeText = when (runtimeStatus.evaluation.config?.shapeType) {
        GeofenceShapeType.Circle -> tr("Circle", "Lingkaran")
        GeofenceShapeType.Polygon -> tr("Polygon", "Poligon")
        else -> tr("Unknown", "Tidak diketahui")
    }

    fun estimateViewerMapPadding(): Int {
        val config = runtimeStatus.evaluation.config ?: return 120
        return when (config.shapeType) {
            GeofenceShapeType.Circle -> {
                when {
                    config.radiusMeters >= 1200.0 -> 260
                    config.radiusMeters >= 500.0 -> 210
                    config.radiusMeters >= 180.0 -> 160
                    else -> 120
                }
            }
            GeofenceShapeType.Polygon -> {
                val vertices = config.vertices
                if (vertices.size < 2) {
                    120
                } else {
                    val minLat = vertices.minOf { it.latitude }
                    val maxLat = vertices.maxOf { it.latitude }
                    val minLng = vertices.minOf { it.longitude }
                    val maxLng = vertices.maxOf { it.longitude }
                    val distanceResults = FloatArray(1)
                    Location.distanceBetween(minLat, minLng, maxLat, maxLng, distanceResults)
                    when {
                        distanceResults[0] >= 3500f -> 260
                        distanceResults[0] >= 1200f -> 210
                        distanceResults[0] >= 350f -> 160
                        else -> 120
                    }
                }
            }
            else -> 120
        }
    }

    fun estimateViewerFallbackZoom(mapPadding: Int): Float {
        return when {
            mapPadding >= 260 -> 14f
            mapPadding >= 210 -> 14.75f
            mapPadding >= 160 -> 15.5f
            else -> 16f
        }
    }

    fun fitMapToGeofence(map: GoogleMap) {
        val config = runtimeStatus.evaluation.config
        val boundsBuilder = LatLngBounds.Builder()
        var hasPoint = false
        val mapPadding = estimateViewerMapPadding()
        when (config?.shapeType) {
            GeofenceShapeType.Circle -> {
                val radius = config.radiusMeters.takeIf { it > 0.0 } ?: 0.0
                circleCenters.forEach { center ->
                    val centerLatLng = center.toLatLng()
                    listOf(
                        centerLatLng,
                        offsetLatLng(centerLatLng, radius, 0.0),
                        offsetLatLng(centerLatLng, radius, 90.0),
                        offsetLatLng(centerLatLng, radius, 180.0),
                        offsetLatLng(centerLatLng, radius, 270.0)
                    ).forEach {
                        boundsBuilder.include(it)
                        hasPoint = true
                    }
                }
            }
            GeofenceShapeType.Polygon -> {
                config.vertices.forEach { point ->
                    boundsBuilder.include(point.toLatLng())
                    hasPoint = true
                }
            }
            else -> Unit
        }
        currentLatLng?.let {
            boundsBuilder.include(it)
            hasPoint = true
        }
        if (!hasPoint) {
            return
        }
        runCatching {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), mapPadding))
        }.onFailure {
            val fallback = currentLatLng
                ?: closestCenter?.toLatLng()
                ?: circleCenters.firstOrNull()?.toLatLng()
                ?: runtimeStatus.evaluation.config?.vertices?.firstOrNull()?.toLatLng()
            fallback?.let {
                val fallbackZoom = estimateViewerFallbackZoom(mapPadding)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, fallbackZoom))
            }
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

    LaunchedEffect(googleMap, runtimeStatus) {
        val map = googleMap ?: return@LaunchedEffect
        map.clear()
        map.mapType = mapTypeSelection.googleMapType

        currentLatLng?.let {
            map.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("Current location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }

        when (runtimeStatus.evaluation.config?.shapeType) {
            GeofenceShapeType.Circle -> {
                val radius = runtimeStatus.evaluation.config.radiusMeters
                circleCenters.forEach { center ->
                    val centerLatLng = center.toLatLng()
                    val hue = if (closestCenter == center) {
                        BitmapDescriptorFactory.HUE_RED
                    } else {
                        BitmapDescriptorFactory.HUE_ORANGE
                    }
                    map.addMarker(
                        MarkerOptions()
                            .position(centerLatLng)
                            .title(if (closestCenter == center) "Closest center" else "Geofence center")
                            .icon(BitmapDescriptorFactory.defaultMarker(hue))
                    )
                    if (radius > 0.0) {
                        map.addCircle(
                            CircleOptions()
                                .center(centerLatLng)
                                .radius(radius)
                                .strokeColor(LockBlue.toArgb())
                                .fillColor(LockBlueFill.toArgb())
                                .strokeWidth(4f)
                        )
                    }
                }
            }
            GeofenceShapeType.Polygon -> {
                val vertices = runtimeStatus.evaluation.config.vertices.map { it.toLatLng() }
                if (vertices.isNotEmpty()) {
                    vertices.forEachIndexed { index, point ->
                        map.addMarker(
                            MarkerOptions()
                                .position(point)
                                .title("Point ${index + 1}")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    }
                }
                when {
                    vertices.size >= 3 -> {
                        map.addPolygon(
                            PolygonOptions()
                                .addAll(vertices)
                                .strokeColor(LockBlue.toArgb())
                                .fillColor(LockBlue.copy(alpha = 0.18f).toArgb())
                                .strokeWidth(5f)
                        )
                    }
                    vertices.size >= 2 -> {
                        map.addPolyline(
                            PolylineOptions()
                                .addAll(vertices)
                                .color(LockBlue.toArgb())
                                .width(5f)
                        )
                    }
                }
            }
            else -> Unit
        }

        fitMapToGeofence(map)
    }

    LaunchedEffect(mapTypeSelection, googleMap) {
        googleMap?.mapType = mapTypeSelection.googleMapType
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockBackground)
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
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactBackIconButton(onClick = onDismiss)
                    Text(
                        text = tr("Geofence Map", "Peta Geofence"),
                        color = LockTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    GeofenceViewerBadge(
                        text = verdictText,
                        backgroundColor = LockBlueFill,
                        textColor = LockBlueDeep
                    )
                    GeofenceViewerBadge(
                        text = shapeBadgeText,
                        backgroundColor = LockSurfaceSoft,
                        textColor = LockTextPrimary
                    )
                }
                Text(
                    text = tr(
                        "Exam area and current device position.",
                        "Area ujian dan posisi perangkat saat ini."
                    ),
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (runtimeStatus.evaluation.config?.shapeType == GeofenceShapeType.Circle) {
                        GeofenceViewerLegendItem(
                            color = Color(0xFFE14B4B),
                            text = tr("Primary center", "Center utama")
                        )
                    }
                    GeofenceViewerLegendItem(
                        color = Color(0xFFF29A2E),
                        text = if (runtimeStatus.evaluation.config?.shapeType == GeofenceShapeType.Circle) {
                            tr("Exam area", "Area ujian")
                        } else {
                            tr("Boundary points", "Titik batas")
                        }
                    )
                    GeofenceViewerLegendItem(
                        color = Color(0xFF2AABEE),
                        text = tr("Device", "Perangkat")
                    )
                }
                Text(
                    text = tr(
                        "Read-only map. You can inspect the area here, but editing stays in the geofence editor.",
                        "Peta baca-saja. Anda bisa memeriksa area di sini, tetapi pengeditan tetap di editor geofence."
                    ),
                    color = LockTextMuted,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            googleMap = map
                            runCatching {
                                map.mapType = mapTypeSelection.googleMapType
                                map.uiSettings.apply {
                                    isMapToolbarEnabled = false
                                    isCompassEnabled = true
                                    isRotateGesturesEnabled = false
                                    isTiltGesturesEnabled = false
                                }
                            }
                        }
                    }
                }
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

            if (currentLatLng == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(UiTokens.RadiusSm))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(1.dp, LockOutline, RoundedCornerShape(UiTokens.RadiusSm))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tr(
                            "Current location unavailable. Showing saved exam area only.",
                            "Lokasi saat ini belum tersedia. Menampilkan area ujian tersimpan saja."
                        ),
                        color = LockTextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
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
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(1f),
                        label = tr("Verdict", "Verdict"),
                        value = verdictText
                    )
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(1f),
                        label = tr("Fix", "Fix"),
                        value = fixQualityText
                    )
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(1f),
                        label = tr("Distance", "Jarak"),
                        value = summaryDistance
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(1.25f),
                        label = tr("Current", "Posisi"),
                        value = summaryCoordinates
                    )
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(0.85f),
                        label = tr("Provider", "Provider"),
                        value = providerText
                    )
                    CompactInfoMetricCard(
                        modifier = Modifier.weight(0.9f),
                        label = tr("Accuracy", "Akurasi"),
                        value = accuracyText
                    )
                }
                Text(
                    text = tr(
                        "Policy: $policyText | Shape: $shapeText",
                        "Policy: $policyText | Bentuk: $shapeText"
                    ),
                    color = LockTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onRefreshLocation,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = !isRefreshingLocation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockBlue,
                            contentColor = LockOnDark
                        ),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        if (isRefreshingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = LockOnDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isRefreshingLocation) {
                                tr("Refreshing...", "Refreshing...")
                            } else {
                                tr("Refresh", "Refresh")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            val map = googleMap
                            val current = currentLatLng
                            if (map != null && current != null) {
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 17f))
                            } else if (map != null) {
                                fitMapToGeofence(map)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockSurfaceSoft,
                            contentColor = LockTextPrimary
                        )
                    ) {
                        Text(
                            tr("Center", "Pusat"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockGold,
                            contentColor = LockTextPrimary
                        )
                    ) {
                        Text(
                            tr("Close", "Tutup"),
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

@Composable
private fun GeofenceMapViewerTextFallback(
    runtimeStatus: GeofenceRuntimeStatus,
    isRefreshingLocation: Boolean,
    onDismiss: () -> Unit,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    val currentLatLng = runtimeStatus.evaluation.locationSnapshot?.let {
        LatLng(it.latitude, it.longitude)
    }
    val summaryDistance = formatGeofenceDistance(runtimeStatus.evaluation.distanceMeters)
    val summaryCoordinates = currentLatLng?.let { formatCoordinates(it.latitude, it.longitude) } ?: "-"
    val providerText = runtimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"
    val accuracyText = runtimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
        String.format(Locale.US, "%.1f m", it)
    } ?: "-"
    val verdictText = runtimeStatus.securityStatus.finalVerdict.diagnosticLabel()
    val fixQualityText = runtimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()
    val shapeText = runtimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"
    val policyText = runtimeStatus.policySource.diagnosticLabel()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockBackground)
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
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactBackIconButton(onClick = onDismiss)
                    Text(
                        text = tr("Geofence Summary", "Ringkasan Geofence"),
                        color = LockTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    GeofenceViewerBadge(
                        text = verdictText,
                        backgroundColor = LockBlueFill,
                        textColor = LockBlueDeep
                    )
                }
                Text(
                    text = tr(
                        "Lightweight view â€” interactive map disabled to save memory on this device.",
                        "Tampilan ringan â€” peta interaktif dinonaktifkan untuk menghemat memori perangkat ini."
                    ),
                    color = LockTextMuted,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactInfoMetricCard(modifier = Modifier.weight(1f), label = tr("Verdict", "Verdict"), value = verdictText)
                CompactInfoMetricCard(modifier = Modifier.weight(1f), label = tr("Fix", "Fix"), value = fixQualityText)
                CompactInfoMetricCard(modifier = Modifier.weight(1f), label = tr("Distance", "Jarak"), value = summaryDistance)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactInfoMetricCard(modifier = Modifier.weight(1.25f), label = tr("Current", "Posisi"), value = summaryCoordinates)
                CompactInfoMetricCard(modifier = Modifier.weight(0.85f), label = tr("Provider", "Provider"), value = providerText)
                CompactInfoMetricCard(modifier = Modifier.weight(0.9f), label = tr("Accuracy", "Akurasi"), value = accuracyText)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(UiTokens.RadiusSm))
                    .background(LockSurfaceSoft)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = tr("Shape: $shapeText | Policy: $policyText", "Bentuk: $shapeText | Policy: $policyText"),
                        color = LockTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val config = runtimeStatus.evaluation.config
                    if (config != null) {
                        when (config.shapeType) {
                            GeofenceShapeType.Circle -> {
                                val centers = effectiveCircleCenters(config)
                                Text(
                                    text = tr(
                                        "Radius: ${String.format(Locale.US, "%.1f", config.radiusMeters)} m | Centers: ${centers.size}",
                                        "Radius: ${String.format(Locale.US, "%.1f", config.radiusMeters)} m | Pusat: ${centers.size}"
                                    ),
                                    color = LockTextSecondary,
                                    fontSize = 10.sp
                                )
                                centers.forEachIndexed { index, point ->
                                    Text(
                                        text = "  #${index + 1}: ${formatCoordinates(point.latitude, point.longitude)}",
                                        color = LockTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            GeofenceShapeType.Polygon -> {
                                Text(
                                    text = tr("Vertices: ${config.vertices.size}", "Titik: ${config.vertices.size}"),
                                    color = LockTextSecondary,
                                    fontSize = 10.sp
                                )
                                config.vertices.forEachIndexed { index, vertex ->
                                    Text(
                                        text = "  #${index + 1}: ${formatCoordinates(vertex.latitude, vertex.longitude)}",
                                        color = LockTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                    Text(
                        text = tr(
                            "Violations: ${runtimeStatus.violationCount}",
                            "Pelanggaran: ${runtimeStatus.violationCount}"
                        ),
                        color = if (runtimeStatus.violationCount > 0) Color(0xFFE14B4B) else LockTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (runtimeStatus.violationCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, LockOutlineSubtle, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onRefreshLocation,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    enabled = !isRefreshingLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LockBlue,
                        contentColor = LockOnDark
                    )
                ) {
                    if (isRefreshingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = LockOnDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isRefreshingLocation) tr("Refreshing...", "Refreshing...") else tr("Refresh", "Refresh"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LockGold,
                        contentColor = LockTextPrimary
                    )
                ) {
                    Text(
                        tr("Close", "Tutup"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun GeofenceViewerBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .flatPill(containerColor = backgroundColor)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

@Composable
internal fun GeofenceViewerLegendItem(
    color: Color,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = LockTextSecondary,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun GeofenceViewerMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(UiTokens.RadiusSm))
            .background(Color(0xFFF2F5FA))
            .border(1.dp, LockOutline, RoundedCornerShape(UiTokens.RadiusSm))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = LockTextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = LockTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun GeofenceViewerActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 38.dp),
        enabled = enabled,
        shape = RoundedCornerShape(UiTokens.RadiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) LockBlue else Color(0xFFF2F5FA),
            contentColor = if (filled) LockOnDark else LockBlueDeep
        ),
        border = if (filled) null else BorderStroke(1.dp, LockOutline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = if (filled) LockOnDark else LockBlueDeep
                )
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
