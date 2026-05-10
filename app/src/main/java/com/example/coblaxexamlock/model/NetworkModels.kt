package com.example.coblaxexamlock.model
internal data class ExamNetworkStatus(
    val label: String,
    val detail: String,
    val isConnected: Boolean,
    val cellularProvider: String? = null
)

internal enum class NetworkReadinessVerdict {
    ConnectedStable,
    Offline,
    Unvalidated,
    CaptivePortal,
    VpnActive,
    AirplaneMode,
    Unstable
}

internal enum class NetworkDnsProbeVerdict {
    NotRun,
    Skipped,
    Resolved,
    Failed,
    Timeout
}

internal enum class NetworkLatencyBucket {
    Unknown,
    Fast,
    Moderate,
    Slow,
    Timeout
}

internal enum class NetworkReadinessUserVerdict {
    Stable,
    Offline,
    CaptivePortal,
    Unvalidated,
    DnsFailed,
    Slow,
    VpnActive,
    AirplaneMode,
    Unstable
}

internal data class NetworkDnsProbeStatus(
    val verdict: NetworkDnsProbeVerdict = NetworkDnsProbeVerdict.NotRun,
    val host: String = "-",
    val latencyMillis: Long? = null,
    val latencyBucket: NetworkLatencyBucket = NetworkLatencyBucket.Unknown,
    val error: String? = null
)

internal data class NetworkReadinessStatus(
    val examStatus: ExamNetworkStatus,
    val diagnostics: NetworkDiagnostics,
    val verdict: NetworkReadinessVerdict,
    val transportLabel: String,
    val quickFixReason: String?,
    val dnsProbeStatus: NetworkDnsProbeStatus = NetworkDnsProbeStatus(),
    val userFacingVerdict: NetworkReadinessUserVerdict = NetworkReadinessUserVerdict.Stable,
    val userFacingQuickFixText: String? = null
)

internal data class NetworkTimelineEntry(
    val timestamp: String,
    val source: String,
    val transportLabel: String,
    val connected: Boolean,
    val validated: Boolean,
    val captivePortal: Boolean,
    val summary: String
)

internal data class NetworkUnstableRuntimeStatus(
    val unstableActive: Boolean,
    val episodeStartedAt: String?,
    val flapCount: Int,
    val lastFlapAt: String?,
    val warningShown: Boolean,
    val lastWarningAt: String?,
    val lastTransportLabel: String?
)

internal data class ExamOfflineRuntimeStatus(
    val offlineActive: Boolean,
    val offlineStartedAt: String?,
    val currentOfflineDurationMs: Long?,
    val offlineWarningShown: Boolean,
    val lastOfflineWarningAt: String?,
    val lastOfflineDurationMs: Long?
)

internal data class ExamBatteryStatus(
    val levelPercent: Int,
    val isCharging: Boolean
)
internal data class NetworkDiagnostics(
    val activeNetworkAvailable: Boolean,
    val transports: List<String>,
    val hasInternetCapability: Boolean,
    val isValidated: Boolean,
    val isCaptivePortal: Boolean,
    val isMetered: Boolean,
    val isVpnActive: Boolean,
    val isAirplaneModeEnabled: Boolean,
    val notRoaming: Boolean?,
    val interfaceName: String,
    val wifi: WifiDiagnostics?,
    val cellular: CellularDiagnostics?
)

internal data class BatteryDiagnostics(
    val statusLabel: String,
    val pluggedSource: String,
    val batterySaverEnabled: Boolean,
    val healthLabel: String,
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val levelRaw: Int,
    val scaleRaw: Int
)

internal data class WifiDiagnostics(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int?,
    val signalLevel: Int?,
    val linkSpeedMbps: Int?,
    val frequencyMHz: Int?,
    val bandLabel: String,
    val hiddenSsid: Boolean?,
    val locationPermissionGranted: Boolean,
    val locationServicesEnabled: Boolean
)

internal data class CellularDiagnostics(
    val providerName: String,
    val operatorCode: String,
    val networkType: String,
    val roaming: Boolean?,
    val signalLevel: Int?,
    val simState: String
)
