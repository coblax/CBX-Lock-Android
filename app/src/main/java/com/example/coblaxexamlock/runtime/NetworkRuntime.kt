package com.example.coblaxexamlock.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import com.example.coblaxexamlock.model.BatteryDiagnostics
import com.example.coblaxexamlock.model.CellularDiagnostics
import com.example.coblaxexamlock.model.ExamBatteryStatus
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.NetworkDiagnostics
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.WifiDiagnostics
import java.util.Locale


internal fun calculateWifiSignalLevel(rssi: Int): Int {
    return when {
        rssi >= -55 -> 4
        rssi >= -65 -> 3
        rssi >= -75 -> 2
        rssi >= -85 -> 1
        else -> 0
    }
}

internal fun getWifiBandLabel(frequencyMHz: Int?): String {
    return when (frequencyMHz) {
        null -> "-"
        in 2400..2500 -> "2.4 GHz"
        in 4900..5900 -> "5 GHz"
        in 5925..7125 -> "6 GHz"
        else -> "Lainnya"
    }
}

internal fun sanitizeWifiValue(rawValue: String?): String {
    val normalized = rawValue.orEmpty().trim().removePrefix("\"").removeSuffix("\"")
    return when {
        normalized.isBlank() -> "-"
        normalized.equals("<unknown ssid>", ignoreCase = true) -> "-"
        normalized.equals("02:00:00:00:00:00", ignoreCase = true) -> "-"
        else -> normalized
    }
}

internal fun getSimStateLabel(simState: Int): String {
    return when (simState) {
        TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "NETWORK_LOCKED"
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN_REQUIRED"
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK_REQUIRED"
        TelephonyManager.SIM_STATE_READY -> "READY"
        TelephonyManager.SIM_STATE_NOT_READY -> "NOT_READY"
        TelephonyManager.SIM_STATE_PERM_DISABLED -> "PERM_DISABLED"
        TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "CARD_IO_ERROR"
        TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "CARD_RESTRICTED"
        else -> "UNKNOWN"
    }
}

@Suppress("DEPRECATION")
internal fun getTelephonyNetworkTypeLabel(networkType: Int): String {
    return when (networkType) {
        TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
        TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
        TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
        TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
        TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
        TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
        TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
        TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
        TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
        TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
        TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
        TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
        TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
        TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
        TelephonyManager.NETWORK_TYPE_NR -> "NR / 5G"
        else -> "UNKNOWN"
    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
internal fun getNetworkDiagnostics(context: Context): NetworkDiagnostics {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val isAirplaneModeEnabled = runCatching {
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) == 1
    }.getOrDefault(false)

    if (connectivityManager == null) {
        return NetworkDiagnostics(
            activeNetworkAvailable = false,
            transports = emptyList(),
            hasInternetCapability = false,
            isValidated = false,
            isCaptivePortal = false,
            isMetered = false,
            isVpnActive = false,
            isAirplaneModeEnabled = isAirplaneModeEnabled,
            notRoaming = null,
            interfaceName = "-",
            wifi = null,
            cellular = null
        )
    }

    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
    val linkProperties = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

    if (activeNetwork == null || capabilities == null) {
        return NetworkDiagnostics(
            activeNetworkAvailable = false,
            transports = emptyList(),
            hasInternetCapability = false,
            isValidated = false,
            isCaptivePortal = false,
            isMetered = connectivityManager.isActiveNetworkMetered,
            isVpnActive = false,
            isAirplaneModeEnabled = isAirplaneModeEnabled,
            notRoaming = null,
            interfaceName = "-",
            wifi = null,
            cellular = null
        )
    }

    val transports = buildList {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("WIFI")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("CELLULAR")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ETHERNET")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("BLUETOOTH")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
    }

    val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val wifiDiagnostics =
        if (isWifi) {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = runCatching { wifiManager?.connectionInfo }.getOrNull()
            val rssi = wifiInfo?.rssi?.takeIf { it in -127..0 }
            val frequency = runCatching { wifiInfo?.frequency }.getOrNull()?.takeIf { it > 0 }
            val linkSpeed = runCatching { wifiInfo?.linkSpeed }.getOrNull()?.takeIf { it > 0 }
            WifiDiagnostics(
                ssid = sanitizeWifiValue(runCatching { wifiInfo?.ssid }.getOrNull()),
                bssid = sanitizeWifiValue(runCatching { wifiInfo?.bssid }.getOrNull()),
                rssiDbm = rssi,
                signalLevel = rssi?.let(::calculateWifiSignalLevel),
                linkSpeedMbps = linkSpeed,
                frequencyMHz = frequency,
                bandLabel = getWifiBandLabel(frequency),
                hiddenSsid = runCatching { wifiInfo?.hiddenSSID }.getOrNull(),
                locationPermissionGranted = hasLocationPermissionForWifi(context),
                locationServicesEnabled = isLocationServicesEnabled(context)
            )
        } else {
            null
        }
    val cellularDiagnostics =
        if (isCellular) {
            val telephonyManager = context.getSystemService(TelephonyManager::class.java)
            val networkType = runCatching {
                val dataType = telephonyManager?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                val voiceType = telephonyManager?.voiceNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                if (dataType != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                    dataType
                } else {
                    voiceType
                }
            }.getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN)
            CellularDiagnostics(
                providerName = runCatching {
                    telephonyManager?.networkOperatorName.orEmpty().ifBlank { "-" }
                }.getOrDefault("-"),
                operatorCode = runCatching {
                    telephonyManager?.networkOperator.orEmpty().ifBlank { "-" }
                }.getOrDefault("-"),
                networkType = getTelephonyNetworkTypeLabel(networkType),
                roaming = runCatching { telephonyManager?.isNetworkRoaming }.getOrNull(),
                signalLevel = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        telephonyManager?.signalStrength?.level
                    } else {
                        null
                    }
                }.getOrNull(),
                simState = getSimStateLabel(
                    runCatching { telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN }.getOrDefault(
                        TelephonyManager.SIM_STATE_UNKNOWN
                    )
                )
            )
        } else {
            null
        }

    return NetworkDiagnostics(
        activeNetworkAvailable = true,
        transports = transports,
        hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        isCaptivePortal = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL),
        isMetered = connectivityManager.isActiveNetworkMetered,
        isVpnActive = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
        isAirplaneModeEnabled = isAirplaneModeEnabled,
        notRoaming = if (isCellular && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
        } else {
            null
        },
        interfaceName = linkProperties?.interfaceName ?: "-",
        wifi = wifiDiagnostics,
        cellular = cellularDiagnostics
    )
}

internal fun getBatteryDiagnostics(context: Context): BatteryDiagnostics {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val rawStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val rawPlugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val rawHealth = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val rawTemperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val rawVoltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    val rawLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val rawScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val powerManager = context.getSystemService(PowerManager::class.java)

    val statusLabel = when (rawStatus) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
        BatteryManager.BATTERY_STATUS_FULL -> "FULL"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
        else -> "UNKNOWN"
    }

    val pluggedSources = buildList {
        if (rawPlugged and BatteryManager.BATTERY_PLUGGED_USB != 0) add("USB")
        if (rawPlugged and BatteryManager.BATTERY_PLUGGED_AC != 0) add("AC")
        if (rawPlugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0) add("WIRELESS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            rawPlugged and BatteryManager.BATTERY_PLUGGED_DOCK != 0
        ) {
            add("DOCK")
        }
    }

    val healthLabel = when (rawHealth) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
        BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "FAILURE"
        BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
        else -> "UNKNOWN"
    }

    return BatteryDiagnostics(
        statusLabel = statusLabel,
        pluggedSource = pluggedSources.joinToString().ifBlank { "UNPLUGGED" },
        batterySaverEnabled = powerManager?.isPowerSaveMode == true,
        healthLabel = healthLabel,
        temperatureCelsius = if (rawTemperature >= 0) rawTemperature / 10f else null,
        voltageMillivolts = rawVoltage.takeIf { it >= 0 },
        levelRaw = rawLevel,
        scaleRaw = rawScale
    )
}

internal fun ExamNetworkStatus.toTransportLabel(): String {
    val normalizedLabel = label.trim().lowercase(Locale.US)
    val baseTransportLabel = when (normalizedLabel) {
        "wi-fi" -> "WiFi"
        "seluler", "cellular" -> "Cellular"
        "ethernet" -> "Ethernet"
        "online" -> "Online"
        "offline" -> "Offline"
        else -> label.ifBlank { "Unknown" }
    }
    return if (baseTransportLabel == "Cellular" && !cellularProvider.isNullOrBlank()) {
        "Cellular ($cellularProvider)"
    } else {
        baseTransportLabel
    }
}

internal fun readNetworkReadinessStatus(context: Context): NetworkReadinessStatus {
    val examStatus = readExamNetworkStatus(context)
    val diagnostics = getNetworkDiagnostics(context)
    val verdict = when {
        diagnostics.isAirplaneModeEnabled && !examStatus.isConnected -> NetworkReadinessVerdict.AirplaneMode
        !examStatus.isConnected -> NetworkReadinessVerdict.Offline
        diagnostics.isCaptivePortal -> NetworkReadinessVerdict.CaptivePortal
        diagnostics.hasInternetCapability && !diagnostics.isValidated -> NetworkReadinessVerdict.Unvalidated
        else -> NetworkReadinessVerdict.ConnectedStable
    }
    val quickFixReason = when (verdict) {
        NetworkReadinessVerdict.Offline -> "offline"
        NetworkReadinessVerdict.Unvalidated -> "unvalidated"
        NetworkReadinessVerdict.CaptivePortal -> "captive_portal"
        NetworkReadinessVerdict.AirplaneMode -> "airplane_mode"
        NetworkReadinessVerdict.Unstable -> "unstable"
        NetworkReadinessVerdict.ConnectedStable -> null
    }
    return NetworkReadinessStatus(
        examStatus = examStatus,
        diagnostics = diagnostics,
        verdict = verdict,
        transportLabel = examStatus.toTransportLabel(),
        quickFixReason = quickFixReason
    )
}

internal fun readExamNetworkStatus(context: Context): ExamNetworkStatus {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        ?: return ExamNetworkStatus(
            label = "Offline",
            detail = "Perangkat belum menemukan koneksi aktif",
            isConnected = false
        )

    val activeNetwork = connectivityManager.activeNetwork
        ?: return ExamNetworkStatus(
            label = "Offline",
            detail = "Periksa Wi-Fi atau data seluler",
            isConnected = false
        )

    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        ?: return ExamNetworkStatus(
            label = "Offline",
            detail = "Koneksi aktif tidak bisa dibaca",
            isConnected = false
        )

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
            ExamNetworkStatus(
                label = "Wi-Fi",
                detail = "Jaringan lokal siap dipakai",
                isConnected = true
            )

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
            ExamNetworkStatus(
                label = "Seluler",
                detail = "Pastikan sinyal stabil",
                isConnected = true,
                cellularProvider = runCatching {
                    context.getSystemService(TelephonyManager::class.java)
                        ?.networkOperatorName
                        ?.trim()
                }.getOrNull().orEmpty().ifBlank { null }
            )

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
            ExamNetworkStatus(
                label = "Ethernet",
                detail = "Koneksi kabel aktif",
                isConnected = true
            )

        else ->
            ExamNetworkStatus(
                label = "Online",
                detail = "Koneksi terdeteksi",
                isConnected = true
            )
    }
}

internal fun readExamBatteryStatus(context: Context): ExamBatteryStatus {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return readExamBatteryStatus(batteryIntent)
}

internal fun readExamBatteryStatus(intent: Intent?): ExamBatteryStatus {
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val rawStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging =
        rawStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            rawStatus == BatteryManager.BATTERY_STATUS_FULL

    val levelPercent =
        if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            0
        }

    return ExamBatteryStatus(
        levelPercent = levelPercent,
        isCharging = isCharging
    )
}
