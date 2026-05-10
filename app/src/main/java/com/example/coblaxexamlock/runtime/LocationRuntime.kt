package com.example.coblaxexamlock.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.coblaxexamlock.GeofenceConfig
import com.example.coblaxexamlock.LocationSnapshot
import com.example.coblaxexamlock.config.GeofenceCurrentLocationTimeoutMillis
import com.example.coblaxexamlock.evaluateLocationFixQuality
import com.example.coblaxexamlock.selectBestLocationSnapshot
import com.example.coblaxexamlock.selectPreferredGeofenceSnapshot
import com.example.coblaxexamlock.toLocationSnapshot
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull


internal fun hasLocationPermissionForWifi(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

internal fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

internal val KnownFakeLocationPackageNames = listOf(
    // Classic fake GPS apps
    "com.lexa.fakegps",
    "com.incorporateapps.fakegps.fre",
    "com.blogspot.newapphorizons.fakegps",
    "com.rosteam.gpsemulator",
    "com.flygps",
    "com.gsmartstudio.fakegps",
    "com.evezzon.fakegps",
    "com.mock.mocklocations",
    "com.location.test",
    "com.fakegps.mock",
    // High-download fake GPS apps (2023+)
    "com.lkr.fakelocation",
    "com.theappninjas.fakegpsjoystick",
    "com.fakegps.route",
    "ru.gavrikov.mocklocations",
    "com.divi.fakeGPS",
    "com.usefullapps.fakegpslocationprofessional",
    "com.ltp.pro.fakelocation",
    "com.pe.fakegpsrun",
    "com.fakegps.joystick.go",
    "fr.dvilleneuve.lockito"
)

// Keywords that indicate a fake GPS / mock location app.
// Used for dynamic scan of all installed user packages.
private val FakeLocationPackageKeywords = listOf(
    "fakegps",
    "fake.gps",
    "fake_gps",
    "fakelocation",
    "fake.location",
    "fake_location",
    "mockgps",
    "mock.gps",
    "mock.location",
    "mocklocation",
    "gpsspoof",
    "gps.spoof",
    "gpsjoystick",
    "gps.joystick",
    "gpsemulator",
    "gps.emulator"
)

internal fun detectSuspiciousFakeLocationPackages(context: Context): List<String> {
    val packageManager = context.packageManager
    val results = mutableSetOf<String>()

    // Phase 1: Check known package names (fast exact-match lookup)
    for (packageName in KnownFakeLocationPackageNames) {
        val appInfo = runCatching {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull() ?: continue
        results.add(formatPackageLabel(packageManager, appInfo, packageName))
    }

    // Phase 2: Keyword-based scan of all installed user (non-system) packages
    val installedPackages = runCatching {
        @Suppress("DEPRECATION", "QueryPermissionsNeeded")
        packageManager.getInstalledApplications(0)
    }.getOrDefault(emptyList())
    for (appInfo in installedPackages) {
        // Skip system apps — only user-installed apps are suspicious
        if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue
        val pkg = appInfo.packageName.lowercase()
        if (FakeLocationPackageKeywords.any { keyword -> pkg.contains(keyword) }) {
            results.add(formatPackageLabel(packageManager, appInfo, appInfo.packageName))
        }
    }

    return results.toList()
}

private fun formatPackageLabel(
    packageManager: PackageManager,
    appInfo: android.content.pm.ApplicationInfo,
    packageName: String
): String {
    val label = runCatching {
        packageManager.getApplicationLabel(appInfo).toString().trim()
    }.getOrDefault("")
    return if (label.isNotBlank() && !label.equals(packageName, ignoreCase = true)) {
        "$label ($packageName)"
    } else {
        packageName
    }
}

@SuppressLint("MissingPermission")
internal suspend fun acquireBestEffortLocationSnapshot(
    context: Context,
    preferFresh: Boolean,
    geofenceConfig: GeofenceConfig?
): LocationSnapshot? {
    val locationManager = context.getSystemService(LocationManager::class.java) ?: return null
    val finePermissionGranted = hasFineLocationPermission(context)
    val providerOrder = buildList {
        if (finePermissionGranted) {
            add(LocationManager.GPS_PROVIDER)
        }
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.PASSIVE_PROVIDER)
    }.filter { provider ->
        runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
    }

    val lastKnownSnapshot = selectBestLocationSnapshot(
        providerOrder.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
    )
    val lastKnownFixQuality = evaluateLocationFixQuality(
        locationSnapshot = lastKnownSnapshot,
        config = geofenceConfig
    )

    if (providerOrder.isEmpty()) {
        return lastKnownSnapshot
    }

    val shouldTryFresh = lastKnownSnapshot == null ||
        preferFresh ||
        !lastKnownFixQuality.usableForGeofence
    if (!shouldTryFresh) {
        return lastKnownSnapshot
    }

    var freshSnapshot: LocationSnapshot? = null
    for (provider in providerOrder) {
        val candidate = requestCurrentLocationSnapshot(
            context = context,
            locationManager = locationManager,
            provider = provider
        ) ?: continue
        freshSnapshot = selectPreferredGeofenceSnapshot(
            snapshots = listOfNotNull(freshSnapshot, candidate),
            config = geofenceConfig,
            preferFresh = true
        ) ?: candidate
        if (
            evaluateLocationFixQuality(
                locationSnapshot = candidate,
                config = geofenceConfig
            ).usableForGeofence
        ) {
            break
        }
    }
    return selectPreferredGeofenceSnapshot(
        snapshots = listOfNotNull(lastKnownSnapshot, freshSnapshot),
        config = geofenceConfig,
        preferFresh = preferFresh
    ) ?: freshSnapshot ?: lastKnownSnapshot
}

@SuppressLint("MissingPermission")
internal suspend fun requestCurrentLocationSnapshot(
    context: Context,
    locationManager: LocationManager,
    provider: String
): LocationSnapshot? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return null
    }

    return withTimeoutOrNull(GeofenceCurrentLocationTimeoutMillis) {
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            locationManager.getCurrentLocation(
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location?.toLocationSnapshot())
                }
            }
        }
    }
}

internal fun LocationSnapshot.toPlatformLocation(): Location {
    return Location(provider).apply {
        latitude = this@toPlatformLocation.latitude
        longitude = this@toPlatformLocation.longitude
        time = this@toPlatformLocation.fixTimestampMs
        this@toPlatformLocation.accuracyMeters?.let { accuracy = it }
    }
}

internal fun hasDiagnosticPhoneNumberPermission(context: Context): Boolean {
    val hasPhoneStatePermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    val hasPhoneNumbersPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    return hasPhoneStatePermission || hasPhoneNumbersPermission
}

internal fun sanitizePhoneNumber(rawValue: String?): String {
    val normalized =
        rawValue
            .orEmpty()
            .trim()
            .replace(" ", "")
            .replace("-", "")
    return when {
        normalized.isBlank() -> ""
        normalized.equals("unknown", ignoreCase = true) -> ""
        normalized.equals("null", ignoreCase = true) -> ""
        else -> normalized
    }
}

@SuppressLint("HardwareIds", "MissingPermission")
@Suppress("DEPRECATION")
internal fun getDevicePhoneNumber(context: Context): String {
    if (!hasDiagnosticPhoneNumberPermission(context)) {
        return "-"
    }

    val candidates = buildList {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        runCatching {
            subscriptionManager?.activeSubscriptionInfoList
                .orEmpty()
                .mapNotNull { it.number }
        }.getOrDefault(emptyList()).forEach(::add)

        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        add(runCatching { telephonyManager?.line1Number.orEmpty() }.getOrDefault(""))
    }

    return candidates
        .map(::sanitizePhoneNumber)
        .firstOrNull { it.isNotBlank() }
        ?: "-"
}

@Suppress("DEPRECATION")
internal fun isLocationServicesEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val locationManager = context.getSystemService(LocationManager::class.java) ?: return false
        runCatching { locationManager.isLocationEnabled }.getOrDefault(false)
    } else {
        runCatching {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            ) != Settings.Secure.LOCATION_MODE_OFF
        }.getOrDefault(false)
    }
}
