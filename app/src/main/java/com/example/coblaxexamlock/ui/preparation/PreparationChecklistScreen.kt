package com.example.coblaxexamlock.ui.preparation

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
import android.util.Log
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
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlayQuickFixTarget
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.config.AllowedExamKeyboardPackages
import com.example.coblaxexamlock.config.AllowedSystemKeyboardPackagePrefixes
import com.example.coblaxexamlock.config.BlockedExamKeyboardPackages
import com.example.coblaxexamlock.config.EmulatorPackagePrefixes
import com.example.coblaxexamlock.config.MagiskIndicatorPaths
import com.example.coblaxexamlock.config.RiskyAccessibilityKeywords
import com.example.coblaxexamlock.config.RootBinaryIndicatorPaths
import com.example.coblaxexamlock.config.RootPackageNames
import com.example.coblaxexamlock.config.SuspiciousKeyboardPackageTokens
import com.example.coblaxexamlock.config.TrustedOemKeyboardManufacturers
import com.example.coblaxexamlock.config.VirtualFingerprintTokens
import com.example.coblaxexamlock.config.VirtualHardwareTokens
import com.example.coblaxexamlock.config.VirtualManufacturerTokens
import com.example.coblaxexamlock.config.VirtualModelTokens
import com.example.coblaxexamlock.config.VirtualProductTokens
import com.example.coblaxexamlock.config.VirtualQemuFiles
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.GeofenceMapViewerScreen
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
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

private const val PreparationPerfTag = "PreparationPerf"

private inline fun <T> debugMeasurePreparationWork(label: String, block: () -> T): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                PreparationPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

private enum class QuickFixSeverity {
    Blocking,
    Warning
}

private enum class QuickFixTarget {
    All,
    Network,
    Location,
    DeviceTime,
    ScreenPinning
}

private data class PreparationQuickFixAction(
    val text: String,
    val severity: QuickFixSeverity,
    val target: QuickFixTarget?,
    val priority: Int,
    val filled: Boolean = false,
    val loading: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
internal fun ExamSecurityPreparationScreen(
    examName: String,
    keyboardPackage: String,
    keyboardAllowed: Boolean,
    usingBuiltInExamKeyboard: Boolean,
    bluetoothPermissionGranted: Boolean,
    bluetoothEnabled: Boolean,
    accessibilityServiceEnabled: Boolean,
    adbInspection: AdbInspection,
    adbBypassState: AdbBypassState,
    rootSecurityStatus: RootSecurityStatus,
    rootBypassState: RootBypassState,
    signatureMismatchDetected: Boolean,
    virtualEnvironmentDetected: Boolean,
    tamperDetected: Boolean,
    sendingSection: DiagnosticSection?,
    isStartingExam: Boolean,
    webViewSessionResetInFlight: Boolean,
    webViewSessionResetError: String?,
    isRefreshingGeofence: Boolean,
    isWarmingLocation: Boolean,
    isRefreshingNetwork: Boolean,
    lastGeofenceRefreshAt: String?,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    networkTimelinePreview: List<NetworkTimelineEntry>,
    lastNetworkChangeAt: String?,
    lastNetworkChangeSource: String?,
    lastConnectedNetworkLabel: String?,
    screenPinningAvailable: Boolean,
    isScreenPinningActive: Boolean,
    screenPinningFixNeeded: Boolean,
    clipboardViolationCount: Int,
    clipboardRuntimeStatus: ClipboardRuntimeStatus,
    clipboardBypassState: ClipboardBypassState,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    deviceTimeBypassState: DeviceTimeBypassState,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    overlayRiskResult: OverlayRiskResult,
    appSwitchStatus: AppSwitchStatus,
    reinstallApkFixNeeded: Boolean,
    bypassScreenPinning: Boolean,
    bypassBluetooth: Boolean,
    bypassAccessibility: Boolean,
    bypassAdb: Boolean,
    bypassRoot: Boolean,
    bypassVirtualEnvironment: Boolean,
    bypassKeyboardPolicy: Boolean,
    bypassClipboard: Boolean,
    bypassOverlay: Boolean,
    bypassGeofence: Boolean,
    geofenceBypassState: GeofenceBypassState,
    bypassFakeLocation: Boolean,
    fakeLocationBypassState: FakeLocationBypassState,
    bypassDeviceTime: Boolean,
    bypassAppSwitch: Boolean,
    showChecklistDetails: Boolean,
    onChooseKeyboard: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onGrantBluetoothPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlayAccessibilitySettings: () -> Unit,
    onOpenDeveloperOptionsSettings: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationServicesSettings: () -> Unit,
    onRefreshGeofenceLocation: () -> Unit,
    onOpenGeofenceMapViewer: () -> Unit,
    onOpenInternetSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenCellularSettings: () -> Unit,
    onOpenAirplaneModeSettings: () -> Unit,
    onRefreshNetworkStatus: () -> Unit,
    onOpenDateTimeSettings: () -> Unit,
    onOpenFakeLocationDeveloperOptionsSettings: () -> Unit,
    onOpenScreenPinningSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onReinstallOfficialApk: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRefreshAllSecurityChecks: () -> Unit,
    onRequestSectionReport: (DiagnosticSection) -> Unit,
    onStartExam: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiLanguage = LocalUiLanguage.current
    val context = LocalContext.current
    val accessibilityInspection = remember(
        context,
        accessibilityServiceEnabled,
        showChecklistDetails,
        bypassAccessibility
    ) {
        debugMeasurePreparationWork("inspectAccessibility") {
            inspectAccessibility(context)
        }
    }
    val scrollState = rememberScrollState()
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingQuickFixTarget by rememberSaveable { mutableStateOf<QuickFixTarget?>(null) }
    val refreshAllSecurityChecks by rememberUpdatedState(onRefreshAllSecurityChecks)
    val refreshPreparationStatus by rememberUpdatedState(onRefreshStatus)
    val refreshNetworkStatus by rememberUpdatedState(onRefreshNetworkStatus)
    val refreshLocationStatus by rememberUpdatedState(onRefreshGeofenceLocation)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) {
                return@LifecycleEventObserver
            }
            val target = pendingQuickFixTarget ?: return@LifecycleEventObserver
            pendingQuickFixTarget = null
            when (target) {
                QuickFixTarget.Network -> refreshNetworkStatus()
                QuickFixTarget.Location -> refreshLocationStatus()
                QuickFixTarget.DeviceTime,
                QuickFixTarget.ScreenPinning -> refreshPreparationStatus()
                QuickFixTarget.All -> refreshAllSecurityChecks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    fun runQuickFix(target: QuickFixTarget?, action: () -> Unit) {
        if (target != null) {
            pendingQuickFixTarget = target
        }
        action()
    }
    val checklistTitle = tr("Automatic Checklist", "Checklist Otomatis")
    val checklistSubtitle = tr("Quick checks before the exam starts.", "Pemeriksaan singkat sebelum mulai.")
    val examTitle = examName.ifBlank { tr("Exam Session", "Sesi Ujian") }
    val telegramHelperText = tr(
        "Tap the Telegram icon on each checklist item to send diagnostics for that section.",
        "Ketuk ikon Telegram di setiap item checklist untuk kirim diagnostik bagian tersebut."
    )
    fun preparationDetailOrNull(english: String, indonesian: String): String? =
        if (showChecklistDetails) localized(uiLanguage, english, indonesian) else null
    val enabledAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.activePackages else emptyList()
    val allowedAccessibilityServices =
        if (showChecklistDetails) accessibilityInspection.allowedServiceComponents else emptyList()
    val allowedAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.allowedPackages else emptyList()
    val effectiveAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.effectivePackages else emptyList()
    val riskyAccessibilityPackages =
        if (showChecklistDetails) accessibilityInspection.riskyPackages else emptyList()
    val needsBluetoothPermission = requiresBluetoothExamPermission()
    val accessibilityStatusLabel = when {
        bypassAccessibility -> tr("Bypassed", "Bypass")
        accessibilityInspection.allowedOnlyActive -> tr("Allowed", "Diizinkan")
        accessibilityServiceEnabled -> tr("Action needed", "Perlu aksi")
        else -> tr("Safe", "Aman")
    }
    val overlayStatusLabel = when {
        bypassOverlay -> tr("Bypassed", "Bypass")
        overlayRiskResult.confirmedInteractionDetected -> tr("Danger", "Bahaya")
        overlayRiskResult.heuristicRisk -> tr("Warning", "Peringatan")
        else -> tr("Safe", "Aman")
    }
    val geofenceStatusLabel = when {
        geofenceBypassState == GeofenceBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassGeofence -> tr("Bypassed", "Bypass")
        !geofenceRuntimeStatus.evaluation.enabled -> tr("Policy Off", "Policy Nonaktif")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Inside -> tr("Inside Area", "Di Dalam Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> tr("Outside Area", "Di Luar Area")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> tr("Stale Fix", "Fix Kedaluwarsa")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> tr("Low Accuracy", "Akurasi Rendah")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> tr("Missing Accuracy", "Akurasi Tidak Ada")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> tr("No Fix", "Belum Ada Fix")
        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> tr("Config Error", "Konfigurasi Salah")
        else -> tr("Needs Fix", "Perlu Perbaikan")
    }
    val geofenceProviderSummary = geofenceRuntimeStatus.evaluation.locationSnapshot
        ?.provider
        ?.ifBlank { "-" }
        ?: "-"
    val geofenceFixAgeSummary = formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)
    val geofenceFixResultSummary = geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()
    val geofenceRefreshAtSummary = lastGeofenceRefreshAt?.ifBlank { "-" } ?: "-"
    val geofenceFixMetaLine = tr(
        "Fix: $geofenceProviderSummary | $geofenceFixAgeSummary | $geofenceFixResultSummary",
        "Fix: $geofenceProviderSummary | $geofenceFixAgeSummary | $geofenceFixResultSummary"
    )
    val geofenceRefreshMetaLine = if (isRefreshingGeofence) {
        tr(
            "Refresh: running...",
            "Refresh: berjalan..."
        )
    } else if (isWarmingLocation) {
        tr(
            "Refresh: warming fresh location...",
            "Refresh: menyiapkan lokasi segar..."
        )
    } else {
        tr(
            "Refresh: $geofenceRefreshAtSummary",
            "Refresh: $geofenceRefreshAtSummary"
        )
    }
    val geofenceMeta = when {
        !geofenceRuntimeStatus.evaluation.enabled || bypassGeofence -> null
        else -> "$geofenceFixMetaLine\n$geofenceRefreshMetaLine"
    }
    val fakeLocationStatusLabel = when {
        fakeLocationBypassState == FakeLocationBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassFakeLocation -> tr("Bypassed", "Bypass")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired ->
            tr("Needs Location Permission", "Butuh Izin Lokasi")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled ->
            tr("Location Services Off", "Layanan Lokasi Off")
        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable ->
            tr("Waiting for Location", "Menunggu Lokasi")
        !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled -> tr("Policy Off", "Policy Nonaktif")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Critical -> tr("Spoof Critical", "Spoof Kritis")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Strong -> tr("Spoof Strong", "Spoof Kuat")
        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Warning -> tr("Package Warning", "Peringatan Paket")
        else -> tr("Clean", "Bersih")
    }
    val deviceTimeStatusLabel = when {
        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr("Tampered", "Tampered")
        bypassDeviceTime -> tr("Bypassed", "Bypass")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> tr("Safe", "Aman")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> tr("Auto Date/Time Off", "Tanggal/Waktu Otomatis Nonaktif")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> tr("Auto Time Zone Off", "Zona Waktu Otomatis Nonaktif")
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> tr("Clock Change", "Perubahan Jam")
        else -> tr("Action needed", "Perlu aksi")
    }
    val networkStatusLabel = when (networkReadinessStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable -> tr("Stable", "Stabil")
        NetworkReadinessVerdict.Offline -> tr("Offline", "Offline")
        NetworkReadinessVerdict.Unvalidated -> tr("Unvalidated", "Belum Tervalidasi")
        NetworkReadinessVerdict.CaptivePortal -> tr("Captive Portal", "Captive Portal")
        NetworkReadinessVerdict.AirplaneMode -> tr("Airplane Mode", "Mode Pesawat")
        NetworkReadinessVerdict.Unstable -> tr("Unstable", "Tidak Stabil")
    }
    val networkValue = when (networkReadinessStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable -> tr(
            "Connected and ready on ${networkReadinessStatus.transportLabel}.",
            "Terhubung dan siap di ${networkReadinessStatus.transportLabel}."
        )
        NetworkReadinessVerdict.Offline -> tr(
            "No active internet connection is available right now.",
            "Saat ini belum ada koneksi internet aktif."
        )
        NetworkReadinessVerdict.Unvalidated -> tr(
            "A network is connected, but Android has not validated internet access yet.",
            "Jaringan sudah terhubung, tetapi Android belum memvalidasi akses internet."
        )
        NetworkReadinessVerdict.CaptivePortal -> tr(
            "This network may still require a portal or login step before internet works.",
            "Jaringan ini mungkin masih membutuhkan portal atau langkah login sebelum internet bisa dipakai."
        )
        NetworkReadinessVerdict.AirplaneMode -> tr(
            "Airplane mode is on and no active connection is available.",
            "Mode pesawat aktif dan belum ada koneksi aktif."
        )
        NetworkReadinessVerdict.Unstable -> tr(
            "The connection has changed several times recently. A stable network is recommended before and during the exam.",
            "Koneksi berubah beberapa kali belakangan ini. Jaringan yang stabil disarankan sebelum dan selama ujian."
        )
    }
    val networkLastChangeSummary = lastNetworkChangeAt?.ifBlank { "-" } ?: "-"
    val networkMeta = when {
        networkReadinessStatus.verdict == NetworkReadinessVerdict.Unstable ||
            networkUnstableRuntimeStatus.flapCount > 0 ->
            tr(
                "Last change: $networkLastChangeSummary | Changes: ${networkUnstableRuntimeStatus.flapCount}",
                "Perubahan terakhir: $networkLastChangeSummary | Perubahan: ${networkUnstableRuntimeStatus.flapCount}"
            )
        else -> null
    }
    val networkDetail = when (networkReadinessStatus.verdict) {
        NetworkReadinessVerdict.ConnectedStable -> null
        NetworkReadinessVerdict.Offline -> tr(
            "Check Wi-Fi or mobile data, then tap Refresh.",
            "Periksa Wi-Fi atau data seluler, lalu tekan Refresh."
        )
        NetworkReadinessVerdict.Unvalidated -> tr(
            "Wait a moment or switch to a network with working internet, then tap Refresh.",
            "Tunggu sebentar atau pindah ke jaringan yang internetnya aktif, lalu tekan Refresh."
        )
        NetworkReadinessVerdict.CaptivePortal -> tr(
            "Complete the network login page first, then return here and tap Refresh.",
            "Selesaikan halaman login jaringan dahulu, lalu kembali dan tekan Refresh."
        )
        NetworkReadinessVerdict.AirplaneMode -> tr(
            "Turn off airplane mode or enable Wi-Fi/mobile data, then tap Refresh.",
            "Matikan mode pesawat atau aktifkan Wi-Fi/data seluler, lalu tekan Refresh."
        )
        NetworkReadinessVerdict.Unstable -> tr(
            "Use the most stable available network before starting the exam.",
            "Gunakan jaringan yang paling stabil sebelum mulai ujian."
        )
    }
    val deviceTimeDetail = when {
        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr(
            "Open Admin Secret to review the bypass integrity.",
            "Buka Admin Secret untuk memeriksa integritas bypass."
        )
        bypassDeviceTime -> null
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> null
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> tr(
            "Enable automatic date & time, then tap Refresh.",
            "Aktifkan tanggal & waktu otomatis, lalu tekan Refresh."
        )
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> tr(
            "Enable automatic time zone, then tap Refresh.",
            "Aktifkan zona waktu otomatis, lalu tekan Refresh."
        )
        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> tr(
            "Enable automatic time, then refresh the check before starting the exam.",
            "Aktifkan waktu otomatis, lalu refresh pemeriksaan sebelum mulai ujian."
        )
        else -> null
    }
    val bluetoothStatusLabel = when {
        bypassBluetooth -> tr("Bypassed", "Bypass")
        needsBluetoothPermission && !bluetoothPermissionGranted -> tr("Permission needed", "Butuh izin")
        bluetoothEnabled -> tr("Action needed", "Perlu aksi")
        else -> tr("Safe", "Aman")
    }
    val developerStatusLabel = when {
        adbBypassState == AdbBypassState.Tampered -> tr("Warning", "Peringatan")
        bypassAdb -> tr("Bypassed", "Bypass")
        adbInspection.blocking -> tr("Action needed", "Perlu aksi")
        adbInspection.insecureSystemProperty -> tr("Warning", "Peringatan")
        else -> tr("Safe", "Aman")
    }
    val keyboardStatusLabel = when {
        bypassKeyboardPolicy -> tr("Bypassed", "Bypass")
        keyboardAllowed -> tr("Ready", "Siap")
        else -> "Fallback"
    }
    val rootStatusLabel = when {
        rootBypassState == RootBypassState.Tampered -> tr("Warning", "Peringatan")
        bypassRoot -> tr("Bypassed", "Bypass")
        rootSecurityStatus.detected -> tr("Danger", "Bahaya")
        rootSecurityStatus.selinuxPermissive -> tr("Warning", "Peringatan")
        else -> tr("Safe", "Aman")
    }
    val signatureStatusLabel = when {
        signatureMismatchDetected -> tr("Danger", "Bahaya")
        else -> tr("Safe", "Aman")
    }
    val signatureValue = when {
        signatureMismatchDetected && reinstallApkFixNeeded -> tr(
            "Signature mismatch. Reinstall official APK.",
            "Signature tidak cocok. Instal ulang APK resmi."
        )
        signatureMismatchDetected -> tr(
            "Signature mismatch detected.",
            "Signature tidak cocok terdeteksi."
        )
        else -> tr(
            "Signature matches the official release.",
            "Signature cocok dengan rilis resmi."
        )
    }
    val virtualEnvironmentStatusLabel = when {
        bypassVirtualEnvironment -> tr("Bypassed", "Bypass")
        virtualEnvironmentDetected -> tr("Danger", "Bahaya")
        else -> tr("Safe", "Aman")
    }
    val screenPinningStatusLabel = when {
        bypassScreenPinning -> tr("Bypassed", "Bypass")
        isScreenPinningActive -> tr("Active", "Aktif")
        screenPinningAvailable -> tr("Available", "Tersedia")
        else -> tr("Unavailable", "Tidak tersedia")
    }
    val accessibilityGuardEnabled = remember(context, accessibilityInspection.rawEnabledServices) {
        isExamGuardAccessibilityEnabled(context)
    }
    val accessibilityGuardAvailable = remember(context) {
        isExamGuardAccessibilityAvailable(context)
    }
    val accessibilityGuardRequired =
        !screenPinningAvailable && !bypassScreenPinning && accessibilityGuardAvailable
    val accessibilityGuardStatusLabel = when {
        bypassScreenPinning -> tr("Not required", "Tidak wajib")
        accessibilityGuardRequired && accessibilityGuardEnabled -> tr("Required Active", "Wajib Aktif")
        accessibilityGuardRequired -> tr("Action needed", "Perlu aksi")
        accessibilityGuardEnabled -> tr("Optional Active", "Opsional Aktif")
        else -> tr("Optional", "Opsional")
    }
    val appSwitchStatusLabel = when {
        bypassAppSwitch -> tr("Bypassed", "Bypass")
        appSwitchStatus.hasViolations -> tr("Warning", "Peringatan")
        appSwitchStatus.fallbackGuardActive -> tr("Fallback", "Fallback")
        else -> tr("Monitored", "Dipantau")
    }
    val keyboardDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Default input method package from Settings.Secure.DEFAULT_INPUT_METHOD\n" +
            "- Allowlist packages: ${preparationListSummary(AllowedExamKeyboardPackages)}\n" +
            "- Blocklist packages: ${preparationListSummary(BlockedExamKeyboardPackages)}\n" +
            "- Suspicious tokens: ${preparationListSummary(SuspiciousKeyboardPackageTokens)}\n" +
            "- Must be system app OR trusted OEM keyboard\n" +
            "- Allowed system prefixes: ${preparationListSummary(AllowedSystemKeyboardPackagePrefixes)}\n" +
            "- Trusted OEMs: ${preparationListSummary(TrustedOemKeyboardManufacturers)}\n" +
            "Impact:\n" +
            "- Not allowed -> fallback to internal keyboard\n" +
            "- If keyboard changes during exam -> violation + alarm",
        "Dicek:\n" +
            "- Paket input method default dari Settings.Secure.DEFAULT_INPUT_METHOD\n" +
            "- Allowlist paket: ${preparationListSummary(AllowedExamKeyboardPackages)}\n" +
            "- Blocklist paket: ${preparationListSummary(BlockedExamKeyboardPackages)}\n" +
            "- Token mencurigakan: ${preparationListSummary(SuspiciousKeyboardPackageTokens)}\n" +
            "- Harus aplikasi sistem ATAU keyboard OEM tepercaya\n" +
            "- Prefix sistem yang diizinkan: ${preparationListSummary(AllowedSystemKeyboardPackagePrefixes)}\n" +
            "- OEM tepercaya: ${preparationListSummary(TrustedOemKeyboardManufacturers)}\n" +
            "Dampak:\n" +
            "- Tidak diizinkan -> fallback ke keyboard internal\n" +
            "- Jika berubah saat ujian -> pelanggaran + alarm"
    )
    val bluetoothDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Permission BLUETOOTH_CONNECT (Android 12+)\n" +
            "- Bluetooth adapter enabled state\n" +
            "- Listener for BluetoothAdapter.ACTION_STATE_CHANGED during exam\n" +
            "Impact:\n" +
            "- Start blocked if permission missing or Bluetooth enabled\n" +
            "- If enabled during exam -> violation + alarm",
        "Dicek:\n" +
            "- Izin BLUETOOTH_CONNECT (Android 12+)\n" +
            "- Status adapter Bluetooth\n" +
            "- Listener BluetoothAdapter.ACTION_STATE_CHANGED saat ujian\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika izin belum ada atau Bluetooth aktif\n" +
            "- Jika aktif saat ujian -> pelanggaran + alarm"
    )
    val accessibilityDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- AccessibilityManager.isEnabled\n" +
            "- Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES not empty\n" +
            "- Active packages: ${preparationListSummary(enabledAccessibilityPackages)}\n" +
            "- Allowed services: ${preparationListSummary(allowedAccessibilityServices)}\n" +
            "- Allowed packages: ${preparationListSummary(allowedAccessibilityPackages)}\n" +
            "- Effective packages after allowlist: ${preparationListSummary(effectiveAccessibilityPackages)}\n" +
            "- Risky keywords: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Risky packages matched: ${preparationListSummary(riskyAccessibilityPackages)}\n" +
            "Impact:\n" +
            "- Start blocked if accessibility service active\n" +
            "- If enabled during exam -> warning + alarm",
        "Dicek:\n" +
            "- AccessibilityManager.isEnabled\n" +
            "- Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES tidak kosong\n" +
            "- Paket aktif: ${preparationListSummary(enabledAccessibilityPackages)}\n" +
            "- Service yang diizinkan: ${preparationListSummary(allowedAccessibilityServices)}\n" +
            "- Paket yang diizinkan: ${preparationListSummary(allowedAccessibilityPackages)}\n" +
            "- Paket efektif setelah allowlist: ${preparationListSummary(effectiveAccessibilityPackages)}\n" +
            "- Keyword berisiko: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Paket berisiko terdeteksi: ${preparationListSummary(riskyAccessibilityPackages)}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika aksesibilitas aktif\n" +
            "- Jika aktif saat ujian -> peringatan + alarm"
    )
    val overlayDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Confirmed signal: obscured/partially obscured touch on SecureExamWebView\n" +
            "- Confirmed signal: suspicious exam window focus loss while app stays visible\n" +
            "- Heuristic signal: active accessibility service\n" +
            "- Overlay shield supported: ${if (overlayRiskResult.shieldStatus.supported) "Yes" else "No"}\n" +
            "- Overlay shield requested: ${if (overlayRiskResult.shieldStatus.requested) "Yes" else "No"}\n" +
            "- Overlay shield active: ${if (overlayRiskResult.shieldStatus.active) "Yes" else "No"}\n" +
            "- Overlay shield last apply result: ${
                overlayRiskResult.shieldStatus.lastApplySucceeded?.let { if (it) "success" else "failed" } ?: "unsupported"
            }\n" +
            "- Overlay shield last apply time: ${overlayRiskResult.shieldStatus.lastApplyAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Risky accessibility keywords: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Risky accessibility packages: ${preparationListSummary(overlayRiskResult.riskyAccessibilityPackages)}\n" +
            "- Overlay signals: ${preparationListSummary(overlayRiskResult.signals.map { it.diagnosticLabel() })}\n" +
            "- Overlay violations: ${overlayRiskResult.violationCount}\n" +
            "- Last trigger: ${overlayRiskResult.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Last timestamp: ${overlayRiskResult.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Last context: ${overlayRiskResult.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "Impact:\n" +
            "- Heuristic risk only updates warning status and quick fixes\n" +
            "- Confirmed obscured touch or suspicious focus loss triggers alarm + acknowledge dialog",
        "Dicek:\n" +
            "- Sinyal terkonfirmasi: touch obscured/partially obscured pada SecureExamWebView\n" +
            "- Sinyal terkonfirmasi: fokus jendela ujian hilang secara mencurigakan saat app masih terlihat\n" +
            "- Sinyal heuristik: accessibility service aktif\n" +
            "- Overlay shield didukung: ${if (overlayRiskResult.shieldStatus.supported) "Ya" else "Tidak"}\n" +
            "- Overlay shield diminta aktif: ${if (overlayRiskResult.shieldStatus.requested) "Ya" else "Tidak"}\n" +
            "- Overlay shield aktif: ${if (overlayRiskResult.shieldStatus.active) "Ya" else "Tidak"}\n" +
            "- Hasil apply overlay shield terakhir: ${
                overlayRiskResult.shieldStatus.lastApplySucceeded?.let { if (it) "berhasil" else "gagal" } ?: "tidak didukung"
            }\n" +
            "- Waktu apply overlay shield terakhir: ${overlayRiskResult.shieldStatus.lastApplyAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Keyword accessibility berisiko: ${preparationListSummary(RiskyAccessibilityKeywords)}\n" +
            "- Paket accessibility berisiko: ${preparationListSummary(overlayRiskResult.riskyAccessibilityPackages)}\n" +
            "- Sinyal overlay: ${preparationListSummary(overlayRiskResult.signals.map { it.diagnosticLabel() })}\n" +
            "- Jumlah pelanggaran overlay: ${overlayRiskResult.violationCount}\n" +
            "- Trigger terakhir: ${overlayRiskResult.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Waktu terakhir: ${overlayRiskResult.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Konteks terakhir: ${overlayRiskResult.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "Dampak:\n" +
            "- Risiko heuristik hanya mengubah status warning dan quick fix\n" +
            "- Obscured touch atau fokus hilang mencurigakan memicu alarm + dialog acknowledge"
    )
    val developerDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = ${adbInspection.developerOptionsRawValue}\n" +
            "- Settings.Global.ADB_ENABLED = ${adbInspection.adbRawValue}\n" +
            "- ro.adb.secure = ${adbInspection.adbSecureProperty}\n" +
            "- Integrity hint = ${adbInspection.integrityHintSummary}\n" +
            "Impact:\n" +
            "- Start blocked if Developer Mode or ADB enabled\n" +
            "- If enabled during exam -> warning + alarm",
        "Dicek:\n" +
            "- Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = ${adbInspection.developerOptionsRawValue}\n" +
            "- Settings.Global.ADB_ENABLED = ${adbInspection.adbRawValue}\n" +
            "- ro.adb.secure = ${adbInspection.adbSecureProperty}\n" +
            "- Hint integritas = ${adbInspection.integrityHintSummary}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika Developer Mode atau ADB aktif\n" +
            "- Jika aktif saat ujian -> peringatan + alarm"
    )
    val rootDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Build tags contain test-keys\n" +
            "- su/busybox paths: ${preparationListSummary(RootBinaryIndicatorPaths)}\n" +
            "- Root app packages: ${preparationListSummary(RootPackageNames)}\n" +
            "- Magisk paths: ${preparationListSummary(MagiskIndicatorPaths)}\n" +
            "- Zygisk detection: /data/adb/zygisk or /proc/self/maps scan\n" +
            "- Bootloader state from ro.boot.verifiedbootstate, ro.boot.vbmeta.device_state, ro.boot.flash.locked\n" +
            "- Dangerous props: ro.debuggable, ro.secure, ro.adb.secure, ro.build.type\n" +
            "- SELinux enabled/enforced\n" +
            "- Current primary indicator: ${rootSecurityStatus.primaryIndicatorLabel}\n" +
            "- Current evidence summary: ${rootSecurityStatus.evidenceSummary}\n" +
            "Impact:\n" +
            "- Start blocked if root indicators found\n" +
            "- If detected during exam -> warning + alarm",
        "Dicek:\n" +
            "- Build tags mengandung test-keys\n" +
            "- Path su/busybox: ${preparationListSummary(RootBinaryIndicatorPaths)}\n" +
            "- Paket aplikasi root: ${preparationListSummary(RootPackageNames)}\n" +
            "- Path Magisk: ${preparationListSummary(MagiskIndicatorPaths)}\n" +
            "- Deteksi Zygisk: /data/adb/zygisk atau scan /proc/self/maps\n" +
            "- Status bootloader dari ro.boot.verifiedbootstate, ro.boot.vbmeta.device_state, ro.boot.flash.locked\n" +
            "- Properti berbahaya: ro.debuggable, ro.secure, ro.adb.secure, ro.build.type\n" +
            "- Status SELinux enabled/enforced\n" +
            "- Indikator utama saat ini: ${rootSecurityStatus.primaryIndicatorLabel}\n" +
            "- Ringkasan bukti saat ini: ${rootSecurityStatus.evidenceSummary}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika indikator root ditemukan\n" +
            "- Jika terdeteksi saat ujian -> peringatan + alarm"
    )
    val signatureDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- SHA-256 fingerprint of signing certificate\n" +
            "- Expected fingerprints: release (and debug when BuildConfig.DEBUG)\n" +
            "Impact:\n" +
            "- Mismatch blocks start and prompts reinstall official APK",
        "Dicek:\n" +
            "- Fingerprint SHA-256 sertifikat penandatangan APK\n" +
            "- Fingerprint expected: rilis (dan debug saat BuildConfig.DEBUG)\n" +
            "Dampak:\n" +
            "- Tidak cocok -> blok mulai ujian dan sarankan reinstall APK resmi"
    )
    val virtualEnvironmentDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Build.FINGERPRINT tokens: ${preparationListSummary(VirtualFingerprintTokens)}\n" +
            "- Build.MODEL tokens: ${preparationListSummary(VirtualModelTokens)}\n" +
            "- Build.MANUFACTURER tokens: ${preparationListSummary(VirtualManufacturerTokens)}\n" +
            "- Build.BRAND/DEVICE generic prefix\n" +
            "- Build.PRODUCT tokens: ${preparationListSummary(VirtualProductTokens)}\n" +
            "- Build.HARDWARE tokens: ${preparationListSummary(VirtualHardwareTokens)}\n" +
            "- x86 ABIs in Build.SUPPORTED_ABIS\n" +
            "- ro.kernel.qemu=1\n" +
            "- QEMU files: ${preparationListSummary(VirtualQemuFiles)}\n" +
            "- Emulator package prefixes: ${preparationListSummary(EmulatorPackagePrefixes)}\n" +
            "Impact:\n" +
            "- Start blocked if emulator/VM detected\n" +
            "- If detected during exam -> warning + alarm",
        "Dicek:\n" +
            "- Token Build.FINGERPRINT: ${preparationListSummary(VirtualFingerprintTokens)}\n" +
            "- Token Build.MODEL: ${preparationListSummary(VirtualModelTokens)}\n" +
            "- Token Build.MANUFACTURER: ${preparationListSummary(VirtualManufacturerTokens)}\n" +
            "- Prefix generic pada Build.BRAND/DEVICE\n" +
            "- Token Build.PRODUCT: ${preparationListSummary(VirtualProductTokens)}\n" +
            "- Token Build.HARDWARE: ${preparationListSummary(VirtualHardwareTokens)}\n" +
            "- ABI x86 pada Build.SUPPORTED_ABIS\n" +
            "- ro.kernel.qemu=1\n" +
            "- File QEMU: ${preparationListSummary(VirtualQemuFiles)}\n" +
            "- Prefix paket emulator: ${preparationListSummary(EmulatorPackagePrefixes)}\n" +
            "Dampak:\n" +
            "- Mulai ujian diblokir jika emulator/VM terdeteksi\n" +
            "- Jika terdeteksi saat ujian -> peringatan + alarm"
    )
    val clipboardDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- Clipboard monitoring arms as soon as START EXAM MODE is pressed\n" +
            "- ClipboardManager.OnPrimaryClipChangedListener during exam\n" +
            "- Snapshot includes all clipboard items: text | htmlText | uri | intent.action | intent.data | intent.component\n" +
            "- Diagnostics expose baseline vs detected semantic clipboard signatures for false-positive analysis\n" +
            "- Short settling window confirms the final clipboard state before raising a violation\n" +
            "- Clipboard is re-checked when the app returns after leaving the exam screen\n" +
            "- Ignore synthetic warmup callbacks right after listener registration\n" +
            "- Last confirmed change: ${clipboardRuntimeStatus.lastConfirmedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Last listener decision: ${clipboardRuntimeStatus.lastDecision}\n" +
            "- Baseline semantic signature: ${clipboardRuntimeStatus.baselineSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Detected semantic signature: ${clipboardRuntimeStatus.detectedSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Current semantic signature: ${clipboardRuntimeStatus.currentSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Violations: $clipboardViolationCount\n" +
            "Impact:\n" +
            "- Clipboard changes trigger alarm (does not block start)",
        "Dicek:\n" +
            "- Monitoring clipboard aktif sejak START EXAM MODE ditekan\n" +
            "- ClipboardManager.OnPrimaryClipChangedListener saat ujian\n" +
            "- Snapshot mencakup semua item clipboard: text | htmlText | uri | intent.action | intent.data | intent.component\n" +
            "- Diagnostics menampilkan semantic signature baseline vs detected untuk analisis false positive\n" +
            "- Ada jendela stabilisasi singkat untuk memastikan state akhir sebelum dianggap pelanggaran\n" +
            "- Clipboard dicek ulang saat aplikasi kembali setelah keluar dari layar ujian\n" +
            "- Abaikan callback warmup sintetis sesaat setelah listener dipasang\n" +
            "- Perubahan terkonfirmasi terakhir: ${clipboardRuntimeStatus.lastConfirmedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Keputusan listener terakhir: ${clipboardRuntimeStatus.lastDecision}\n" +
            "- Baseline semantic signature: ${clipboardRuntimeStatus.baselineSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Detected semantic signature: ${clipboardRuntimeStatus.detectedSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Current semantic signature: ${clipboardRuntimeStatus.currentSemanticSignature?.ifBlank { "-" } ?: "-"}\n" +
            "- Jumlah pelanggaran: $clipboardViolationCount\n" +
            "Dampak:\n" +
        "- Perubahan clipboard memicu alarm (tidak memblokir start)"
    )
    val geofenceDetail = preparationDetailOrNull(
        english =
            "- Location policy source: ${geofenceRuntimeStatus.policySource.diagnosticLabel()}\n" +
                "- Geofence enabled: ${if (geofenceRuntimeStatus.evaluation.enabled) "yes" else "no"}\n" +
                "- Shape: ${geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"}\n" +
                "- Polygon points: ${geofenceRuntimeStatus.evaluation.config?.vertices?.size ?: 0}\n" +
                "- Polygon vertices: ${summarizePolygonVertices(geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty())}\n" +
                "- Circle centers: ${effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config).size}\n" +
                "- Circle centers summary: ${summarizeCircleCenters(effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config))}\n" +
                "- Bypass state: ${geofenceBypassState.name.lowercase(Locale.US)}\n" +
                "- Closest / primary center: ${
                    geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: geofenceRuntimeStatus.evaluation.config?.let {
                        formatCoordinates(it.centerLat, it.centerLng)
                    } ?: "-"
                }\n" +
                "- Shared radius: ${
                    geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Permission granted: ${if (geofenceRuntimeStatus.evaluation.permissionGranted) "yes" else "no"}\n" +
                "- Precise granted: ${if (geofenceRuntimeStatus.securityStatus.preciseLocationGranted) "yes" else "no"}\n" +
                "- Location services enabled: ${if (geofenceRuntimeStatus.evaluation.locationServicesEnabled) "yes" else "no"}\n" +
                "- Current coordinates: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: "-"
                }\n" +
                "- Provider: ${geofenceRuntimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"}\n" +
                "- Accuracy: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Fix quality: ${geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix age: ${formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)}\n" +
                "- Snapshot used for geofence: ${if (geofenceRuntimeStatus.securityStatus.fixQualityStatus.usableForGeofence) "yes" else "no"}\n" +
                "- Distance from closest center: ${formatGeofenceDistance(geofenceRuntimeStatus.evaluation.distanceMeters)}\n" +
                "- Final verdict: ${geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Geofence verdict: ${geofenceRuntimeStatus.evaluation.verdict.diagnosticLabel()}\n" +
                "- Violations: ${geofenceRuntimeStatus.violationCount}\n" +
                "- Last trigger: ${geofenceRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}",
        indonesian =
            "- Sumber policy lokasi: ${geofenceRuntimeStatus.policySource.diagnosticLabel()}\n" +
                "- Geofence aktif: ${if (geofenceRuntimeStatus.evaluation.enabled) "ya" else "tidak"}\n" +
                "- Bentuk: ${geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US) ?: "-"}\n" +
                "- Titik polygon: ${geofenceRuntimeStatus.evaluation.config?.vertices?.size ?: 0}\n" +
                "- Vertex polygon: ${summarizePolygonVertices(geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty())}\n" +
                "- Jumlah center circle: ${effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config).size}\n" +
                "- Ringkasan center circle: ${summarizeCircleCenters(effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config))}\n" +
                "- Status bypass: ${geofenceBypassState.name.lowercase(Locale.US)}\n" +
                "- Center terdekat / utama: ${
                    geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: geofenceRuntimeStatus.evaluation.config?.let {
                        formatCoordinates(it.centerLat, it.centerLng)
                    } ?: "-"
                }\n" +
                "- Radius bersama: ${
                    geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Izin lokasi: ${if (geofenceRuntimeStatus.evaluation.permissionGranted) "diberikan" else "belum"}\n" +
                "- Lokasi presisi: ${if (geofenceRuntimeStatus.securityStatus.preciseLocationGranted) "ya" else "belum"}\n" +
                "- Layanan lokasi: ${if (geofenceRuntimeStatus.evaluation.locationServicesEnabled) "aktif" else "nonaktif"}\n" +
                "- Koordinat saat ini: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                        formatCoordinates(it.latitude, it.longitude)
                    } ?: "-"
                }\n" +
                "- Provider: ${geofenceRuntimeStatus.evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"}\n" +
                "- Akurasi: ${
                    geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                        String.format(Locale.US, "%.1f m", it)
                    } ?: "-"
                }\n" +
                "- Kualitas fix: ${geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Umur fix: ${formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs)}\n" +
                "- Snapshot dipakai untuk geofence: ${if (geofenceRuntimeStatus.securityStatus.fixQualityStatus.usableForGeofence) "ya" else "tidak"}\n" +
                "- Jarak dari center terdekat: ${formatGeofenceDistance(geofenceRuntimeStatus.evaluation.distanceMeters)}\n" +
                "- Verdict final: ${geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Verdict geofence: ${geofenceRuntimeStatus.evaluation.verdict.diagnosticLabel()}\n" +
                "- Jumlah pelanggaran: ${geofenceRuntimeStatus.violationCount}\n" +
                "- Trigger terakhir: ${geofenceRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
    )
    val fakeLocationDetail = preparationDetailOrNull(
        english =
            "- Monitoring enabled: ${if (fakeLocationRuntimeStatus.securityStatus.monitoringEnabled) "yes" else "no"}\n" +
                "- Permission granted: ${if (fakeLocationRuntimeStatus.securityStatus.permissionGranted) "yes" else "no"}\n" +
                "- Location services enabled: ${if (fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled) "yes" else "no"}\n" +
                "- Snapshot available: ${if (fakeLocationRuntimeStatus.securityStatus.snapshotAvailable) "yes" else "no"}\n" +
                "- Bypass state: ${fakeLocationBypassState.name.lowercase(Locale.US)}\n" +
                "- Mock location flag: ${if (fakeLocationRuntimeStatus.securityStatus.mockLocationDetected) "yes" else "no"}\n" +
                "- Confidence tier: ${fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel()}\n" +
                "- Final verdict: ${fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Developer options: ${if (fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled) "enabled" else "disabled"}\n" +
                "- Fix quality: ${fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix-quality eligible: ${if (fakeLocationRuntimeStatus.securityStatus.fixQualityEligible) "yes" else "no"}\n" +
                "- Suspicious packages: ${fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }}\n" +
                "- Supporting signals: ${fakeLocationRuntimeStatus.securityStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}\n" +
                "- Violations: ${fakeLocationRuntimeStatus.violationCount}\n" +
                "- Last trigger: ${fakeLocationRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}",
        indonesian =
            "- Monitoring aktif: ${if (fakeLocationRuntimeStatus.securityStatus.monitoringEnabled) "ya" else "tidak"}\n" +
                "- Izin lokasi: ${if (fakeLocationRuntimeStatus.securityStatus.permissionGranted) "ya" else "tidak"}\n" +
                "- Layanan lokasi aktif: ${if (fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled) "ya" else "tidak"}\n" +
                "- Snapshot tersedia: ${if (fakeLocationRuntimeStatus.securityStatus.snapshotAvailable) "ya" else "tidak"}\n" +
                "- Status bypass: ${fakeLocationBypassState.name.lowercase(Locale.US)}\n" +
                "- Flag mock location: ${if (fakeLocationRuntimeStatus.securityStatus.mockLocationDetected) "ya" else "tidak"}\n" +
                "- Confidence tier: ${fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel()}\n" +
                "- Verdict final: ${fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel()}\n" +
                "- Developer options: ${if (fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled) "aktif" else "nonaktif"}\n" +
                "- Kualitas fix: ${fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel()}\n" +
                "- Fix layak dinilai: ${if (fakeLocationRuntimeStatus.securityStatus.fixQualityEligible) "ya" else "tidak"}\n" +
                "- Paket mencurigakan: ${fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }}\n" +
                "- Sinyal pendukung: ${fakeLocationRuntimeStatus.securityStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}\n" +
                "- Jumlah pelanggaran: ${fakeLocationRuntimeStatus.violationCount}\n" +
                "- Trigger terakhir: ${fakeLocationRuntimeStatus.lastTrigger?.ifBlank { "-" } ?: "-"}"
    )
    val screenPinningDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- lock_to_app_enabled from Settings.System then Settings.Secure\n" +
            "- ActivityManager.lockTaskModeState (LOCKED/PINNED)\n" +
            "- Screen Pinning support available: ${if (screenPinningAvailable) "Yes" else "No"}\n" +
            "Impact:\n" +
            "- Available but inactive -> Android pinning is requested only after START EXAM MODE is pressed\n" +
            "- Unavailable -> Start Exam is blocked; use a supported device or Secret Admin bypass\n" +
            "- If bypass enabled -> skip pin/lock-task flow",
        "Dicek:\n" +
            "- lock_to_app_enabled dari Settings.System lalu Settings.Secure\n" +
            "- ActivityManager.lockTaskModeState (LOCKED/PINNED)\n" +
            "- Dukungan Screen Pinning tersedia: ${if (screenPinningAvailable) "Ya" else "Tidak"}\n" +
            "Dampak:\n" +
            "- Tersedia tapi belum aktif -> pinning Android baru diminta setelah START EXAM MODE ditekan\n" +
            "- Tidak tersedia -> Start Exam diblokir; gunakan perangkat yang mendukung atau bypass Secret Admin\n" +
            "- Jika bypass aktif -> lewati alur pin/lock-task"
    )
    val accessibilityGuardDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- CBX Lock Exam Guard accessibility service enabled: ${if (accessibilityGuardEnabled) "Yes" else "No"}\n" +
            "- Required only when Screen Pinning is unavailable and Screen Pinning bypass is off\n" +
                "- Events monitored: TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED, TYPE_NOTIFICATION_STATE_CHANGED\n" +
            "- Screen text is not read\n" +
            "Impact:\n" +
            "- If required and disabled -> Start Exam is blocked\n" +
            "- During fallback mode, app switches are logged and the app returns to the exam with escalating alarm",
        "Dicek:\n" +
            "- Service aksesibilitas CBX Lock Exam Guard aktif: ${if (accessibilityGuardEnabled) "Ya" else "Tidak"}\n" +
            "- Wajib hanya saat Screen Pinning tidak tersedia dan bypass Screen Pinning nonaktif\n" +
                "- Event yang dipantau: TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOWS_CHANGED, TYPE_NOTIFICATION_STATE_CHANGED\n" +
            "- Teks layar tidak dibaca\n" +
            "Dampak:\n" +
            "- Jika wajib tetapi nonaktif -> Start Exam diblokir\n" +
            "- Saat mode fallback, app switch dicatat dan app kembali ke ujian dengan alarm eskalatif"
    )
    val appSwitchDetail = preparationDetailOrNull(
        "Checked:\n" +
            "- App Switch monitoring arms as soon as START EXAM MODE is pressed\n" +
            "- onUserLeaveHint() callback from host activity\n" +
            "- Lifecycle stop/resume fallback when onUserLeaveHint() is skipped by the system\n" +
            "- Resume confirmation after leaving the app\n" +
            "- Suppressed internal-flow logging during allowed transitions\n" +
            "- Protection mode: ${appSwitchStatus.protectionMode.diagnosticLabel()}\n" +
            "- Lock task active now: ${if (appSwitchStatus.lockTaskActive) "Yes" else "No"}\n" +
            "- Fallback guard active: ${if (appSwitchStatus.fallbackGuardActive) "Yes" else "No"}\n" +
            "- Accessibility Guard enabled: ${if (appSwitchStatus.accessibilityGuardEnabled) "Yes" else "No"}\n" +
            "- Accessibility fallback active: ${if (appSwitchStatus.accessibilityFallbackActive) "Yes" else "No"}\n" +
            "- Accessibility violation count: ${appSwitchStatus.accessibilityViolationCount}\n" +
            "- Last accessibility reason: ${appSwitchStatus.accessibilityLastReason?.ifBlank { "-" } ?: "-"}\n" +
            "- Last foreign package: ${appSwitchStatus.accessibilityLastForeignPackage?.ifBlank { "-" } ?: "-"}\n" +
            "- Last accessibility event: ${appSwitchStatus.accessibilityLastEventType?.ifBlank { "-" } ?: "-"}\n" +
            "- Current alarm severity: ${appSwitchStatus.accessibilityAlarmSeverity?.ifBlank { "-" } ?: "-"}\n" +
            "- Last trigger: ${appSwitchStatus.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Last timestamp: ${appSwitchStatus.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Last context: ${appSwitchStatus.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "- Violations: ${appSwitchStatus.violationCount}\n" +
            "- Pending violation: ${if (appSwitchStatus.pendingViolation) "Yes" else "No"}\n" +
            "Impact:\n" +
            "- Leaving the app during exam triggers alarm + acknowledge dialog\n" +
            "- If screen pinning is bypassed/unavailable, App Switch stays active as the fallback guard\n" +
            "- If bypass enabled -> App Switch monitoring is skipped",
        "Dicek:\n" +
            "- Monitoring App Switch aktif sejak START EXAM MODE ditekan\n" +
            "- Callback onUserLeaveHint() dari host activity\n" +
            "- Fallback lifecycle stop/resume jika onUserLeaveHint() dilewati oleh sistem\n" +
            "- Konfirmasi resume setelah keluar dari aplikasi\n" +
            "- Logging suppressed internal-flow saat transisi yang diizinkan\n" +
            "- Mode proteksi: ${appSwitchStatus.protectionMode.diagnosticLabel()}\n" +
            "- Lock task aktif saat ini: ${if (appSwitchStatus.lockTaskActive) "Ya" else "Tidak"}\n" +
            "- Fallback guard aktif: ${if (appSwitchStatus.fallbackGuardActive) "Ya" else "Tidak"}\n" +
            "- Accessibility Guard aktif: ${if (appSwitchStatus.accessibilityGuardEnabled) "Ya" else "Tidak"}\n" +
            "- Fallback accessibility aktif: ${if (appSwitchStatus.accessibilityFallbackActive) "Ya" else "Tidak"}\n" +
            "- Jumlah pelanggaran accessibility: ${appSwitchStatus.accessibilityViolationCount}\n" +
            "- Alasan accessibility terakhir: ${appSwitchStatus.accessibilityLastReason?.ifBlank { "-" } ?: "-"}\n" +
            "- Paket asing terakhir: ${appSwitchStatus.accessibilityLastForeignPackage?.ifBlank { "-" } ?: "-"}\n" +
            "- Event accessibility terakhir: ${appSwitchStatus.accessibilityLastEventType?.ifBlank { "-" } ?: "-"}\n" +
            "- Severity alarm saat ini: ${appSwitchStatus.accessibilityAlarmSeverity?.ifBlank { "-" } ?: "-"}\n" +
            "- Trigger terakhir: ${appSwitchStatus.lastTrigger?.ifBlank { "-" } ?: "-"}\n" +
            "- Waktu terakhir: ${appSwitchStatus.lastDetectedAt?.ifBlank { "-" } ?: "-"}\n" +
            "- Konteks terakhir: ${appSwitchStatus.lastContext?.ifBlank { "-" } ?: "-"}\n" +
            "- Jumlah pelanggaran: ${appSwitchStatus.violationCount}\n" +
            "- Pending violation: ${if (appSwitchStatus.pendingViolation) "Ya" else "Tidak"}\n" +
            "Dampak:\n" +
            "- Keluar dari aplikasi saat ujian memicu alarm + dialog acknowledge\n" +
            "- Jika screen pinning dibypass/tidak aktif, App Switch tetap aktif sebagai fallback guard\n" +
            "- Jika bypass aktif -> monitoring App Switch dilewati"
    )
    val keyboardReady = bypassKeyboardPolicy || keyboardAllowed || usingBuiltInExamKeyboard
    val bluetoothReady =
        bypassBluetooth || (!bluetoothEnabled && (!needsBluetoothPermission || bluetoothPermissionGranted))
    val accessibilityReady = bypassAccessibility || !accessibilityServiceEnabled
    val adbReady = bypassAdb || !adbInspection.blocking
    val rootReady = bypassRoot || !rootSecurityStatus.blocking
    val virtualEnvironmentReady = bypassVirtualEnvironment || !virtualEnvironmentDetected
    val clipboardReady = true
    val deviceTimeReady = bypassDeviceTime || !deviceTimeSecurityStatus.blocking
    val geofenceReady =
        bypassGeofence ||
            !geofenceRuntimeStatus.evaluation.enabled ||
            !geofenceRuntimeStatus.securityStatus.blocking
    val fakeLocationReady =
        bypassFakeLocation ||
            !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled ||
            !fakeLocationRuntimeStatus.securityStatus.blocking
    val overlayReady = bypassOverlay || !overlayRiskResult.hasAnyRisk
    val accessibilityGuardReady = !accessibilityGuardRequired || accessibilityGuardEnabled
    val screenPinningReady =
        bypassScreenPinning || screenPinningAvailable || (accessibilityGuardAvailable && accessibilityGuardEnabled)
    val appSwitchReady = bypassAppSwitch || !appSwitchStatus.hasViolations
    val signatureReady = !signatureMismatchDetected
    val canStartExam =
        bluetoothReady &&
            accessibilityReady &&
            adbReady &&
            rootReady &&
            deviceTimeReady &&
            screenPinningReady &&
            accessibilityGuardReady &&
            geofenceReady &&
            fakeLocationReady &&
            virtualEnvironmentReady &&
            signatureReady &&
            !tamperDetected
    val hasBypassIndicators = listOf(
        bypassKeyboardPolicy,
        bypassBluetooth,
        bypassAccessibility,
        bypassAdb,
        bypassRoot,
        bypassVirtualEnvironment,
        bypassClipboard,
        bypassScreenPinning,
        bypassOverlay,
        bypassGeofence,
        bypassFakeLocation,
        bypassDeviceTime,
        bypassAppSwitch
    ).any { it }
    val startButtonColor = when {
        !canStartExam -> Color(0xFFB34A4A)
        hasBypassIndicators -> LockGold
        else -> Color(0xFF2F8F63)
    }
    val startButtonContentColor =
        if (hasBypassIndicators && canStartExam) LockBlueDeep else Color.White
    val readinessSignals = listOf(
        keyboardReady,
        bluetoothReady,
        accessibilityReady,
        adbReady,
        rootReady,
        virtualEnvironmentReady,
        clipboardReady,
        deviceTimeReady,
        geofenceReady,
        fakeLocationReady,
        overlayReady,
        screenPinningReady,
        accessibilityGuardReady,
        appSwitchReady,
        signatureReady
    )
    val readyCount = readinessSignals.count { it }
    val attentionCount = readinessSignals.size - readyCount
    val readinessAccentColor = when {
        isStartingExam && !bypassScreenPinning -> LockGoldDark
        attentionCount == 0 -> Color(0xFF2F8F63)
        else -> Color(0xFFCC7A00)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LockBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 118.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            border = BorderStroke(1.dp, LockOutline),
            tonalElevation = 4.dp,
            shadowElevation = 10.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LockBlue.copy(alpha = 0.14f),
                                LockBlueSoft.copy(alpha = 0.10f),
                                Color.White
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = LockSurfaceSoft,
                                border = BorderStroke(1.dp, LockOutline)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clickable(onClick = onBackHome)
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Home,
                                        contentDescription = tr("Back to home", "Kembali ke menu utama"),
                                        tint = LockBlueDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = LockBlueDeep
                            ) {
                                Text(
                                    text = tr("PREPARATION MODE", "MODE PERSIAPAN"),
                                    color = LockOnDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.9.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = readinessAccentColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, readinessAccentColor.copy(alpha = 0.18f))
                        ) {
                            Text(
                                text = if (attentionCount == 0) tr("SECURE", "AMAN") else tr("CHECK", "CEK"),
                                color = readinessAccentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = examTitle,
                        color = LockBlueDeep,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tr(
                            "Check the device and keyboard briefly before the exam starts.",
                            "Periksa singkat perangkat dan keyboard sebelum ujian dimulai."
                        ),
                        color = LockTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PreparationSummaryChip(
                            label = tr("Status", "Status"),
                            value = tr(
                                "$readyCount/${readinessSignals.size} ready",
                                "$readyCount/${readinessSignals.size} siap"
                            ),
                            accentColor = readinessAccentColor,
                            modifier = Modifier.weight(1f)
                        )
                        PreparationSummaryChip(
                            label = tr("Exam", "Ujian"),
                            value = examTitle,
                            accentColor = LockBlueDeep,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = LockSurfaceSoft,
            border = BorderStroke(1.dp, LockOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = checklistTitle,
                            color = LockTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = checklistSubtitle,
                            color = LockTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.75f))
                ) {
                    Text(
                        text = telegramHelperText,
                        color = LockTextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                SecurityChecklistItem(
                    title = tr("Safe Keyboard", "Keyboard Aman"),
                    value = when {
                        bypassKeyboardPolicy -> tr("Bypass enabled", "Bypass aktif")
                        usingBuiltInExamKeyboard -> "internal.coblax.exam"
                        else -> keyboardPackage.ifBlank { tr("Not detected", "Tidak terdeteksi") }
                    },
                    detail = keyboardDetail,
                    status = keyboardStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Keyboard) },
                    isSending = sendingSection == DiagnosticSection.Keyboard,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Bluetooth", "Bluetooth"),
                    value = when {
                        bypassBluetooth -> tr("Bypass enabled", "Bypass aktif")
                        needsBluetoothPermission && !bluetoothPermissionGranted ->
                            tr("Bluetooth permission has not been granted.", "Izin Bluetooth belum diberikan")
                        bluetoothEnabled -> tr("Still enabled", "Masih aktif")
                        else -> tr("Disabled", "Nonaktif")
                    },
                    detail = bluetoothDetail,
                    status = bluetoothStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Bluetooth) },
                    isSending = sendingSection == DiagnosticSection.Bluetooth,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Network / Connectivity", "Network / Konektivitas"),
                    value = networkValue,
                    meta = networkMeta,
                    detail = networkDetail,
                    status = networkStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Network) },
                    isSending = sendingSection == DiagnosticSection.Network,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Device Time", "Waktu Perangkat"),
                    value = when {
                        deviceTimeBypassState == DeviceTimeBypassState.Tampered -> tr(
                            "Bypass storage tamper detected. Device Time enforcement remains active.",
                            "Tamper pada storage bypass terdeteksi. Enforcement Waktu Perangkat tetap aktif."
                        )
                        bypassDeviceTime -> tr(
                            "Bypass active. Device Time checks are skipped.",
                            "Bypass aktif. Cek Waktu Perangkat dilewati."
                        )
                        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.Safe -> tr(
                            "Automatic date & time and automatic time zone are enabled.",
                            "Tanggal & waktu otomatis dan zona waktu otomatis aktif."
                        )
                        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> tr(
                            "Automatic date & time is off.",
                            "Tanggal & waktu otomatis nonaktif."
                        )
                        deviceTimeSecurityStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> tr(
                            "Automatic time zone is off.",
                            "Zona waktu otomatis nonaktif."
                        )
                        else -> tr(
                            "A suspicious clock change was detected.",
                            "Terdeteksi perubahan jam yang mencurigakan."
                        )
                    },
                    detail = deviceTimeDetail,
                    status = deviceTimeStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.DeviceTime) },
                    isSending = sendingSection == DiagnosticSection.DeviceTime,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Accessibility Service", "Accessibility Service"),
                    value = when {
                        bypassAccessibility -> tr("Bypass enabled", "Bypass aktif")
                        accessibilityInspection.allowedOnlyActive -> tr(
                            "Allowed service active: ${accessibilityInspection.allowedPackages.joinToString().ifBlank { "-" }}",
                            "Service yang diizinkan aktif: ${accessibilityInspection.allowedPackages.joinToString().ifBlank { "-" }}"
                        )
                        accessibilityServiceEnabled -> tr(
                            "Detected as active on this device",
                            "Terdeteksi aktif di perangkat"
                        )
                        else -> tr("Inactive", "Tidak aktif")
                    },
                    detail = accessibilityDetail,
                    status = accessibilityStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Accessibility) },
                    isSending = sendingSection == DiagnosticSection.Accessibility,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Overlay / Floating App", "Overlay / Floating App"),
                    value = when {
                        bypassOverlay -> tr("Bypass enabled", "Bypass aktif")
                        overlayRiskResult.lastTrigger == OverlaySignal.WindowFocusLoss.diagnosticLabel() ->
                            tr(
                                "A floating app likely stole exam-window focus.",
                                "Floating app kemungkinan mengambil fokus jendela ujian."
                            )
                        overlayRiskResult.confirmedInteractionDetected -> tr(
                            "Overlay interaction was confirmed on the exam screen.",
                            "Interaksi overlay terkonfirmasi di layar ujian."
                        )
                        overlayRiskResult.shieldStatus.active -> tr(
                            "Overlay shield is active for this exam session.",
                            "Overlay shield aktif untuk sesi ujian ini."
                        )
                        overlayRiskResult.riskyAccessibilityPackages.isNotEmpty() -> tr(
                            "Risky accessibility package detected: ${overlayRiskResult.riskyAccessibilityPackages.joinToString()}",
                            "Paket accessibility berisiko terdeteksi: ${overlayRiskResult.riskyAccessibilityPackages.joinToString()}"
                        )
                        overlayRiskResult.heuristicRisk -> tr(
                            "Accessibility activity may create floating-app risk.",
                            "Aktivitas accessibility dapat menimbulkan risiko floating app."
                        )
                        else -> tr("No overlay risk detected", "Tidak ada risiko overlay terdeteksi")
                    },
                    detail = overlayDetail,
                    status = overlayStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Overlay) },
                    isSending = sendingSection == DiagnosticSection.Overlay,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Developer Mode / ADB", "Developer Mode / ADB"),
                    value = when {
                        adbBypassState == AdbBypassState.Tampered -> tr(
                            "Bypass tamper detected; ADB checks stay active",
                            "Tamper bypass terdeteksi; cek ADB tetap aktif"
                        )
                        bypassAdb -> tr("Bypass enabled", "Bypass aktif")
                        adbInspection.developerOptionsEnabled && adbInspection.adbEnabled ->
                            tr("Developer mode and USB debugging are enabled", "Mode developer dan USB debugging aktif")
                        adbInspection.developerOptionsEnabled ->
                            tr("Developer mode is enabled", "Mode developer aktif")
                        adbInspection.adbEnabled -> tr("USB debugging is enabled", "USB debugging aktif")
                        adbInspection.insecureSystemProperty -> tr(
                            "ADB security property is unsafe",
                            "Properti keamanan ADB tidak aman"
                        )
                        else -> tr("Disabled", "Nonaktif")
                    },
                    detail = developerDetail,
                    status = developerStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.DeveloperAdb) },
                    isSending = sendingSection == DiagnosticSection.DeveloperAdb,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Root Device", "Root Device"),
                    value = when {
                        rootBypassState == RootBypassState.Tampered -> tr(
                            "Bypass tamper detected; root checks stay active",
                            "Tamper bypass terdeteksi; cek root tetap aktif"
                        )
                        bypassRoot -> tr("Bypass enabled", "Bypass aktif")
                        rootSecurityStatus.detected -> rootSecurityStatus.primaryIndicatorLabel
                        rootSecurityStatus.selinuxPermissive -> tr("SELinux permissive", "SELinux permisif")
                        else -> tr("Not detected", "Tidak terdeteksi")
                    },
                    detail = rootDetail,
                    status = rootStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Root) },
                    isSending = sendingSection == DiagnosticSection.Root,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Official APK Signature", "Signature APK Resmi"),
                    value = signatureValue,
                    detail = signatureDetail,
                    status = signatureStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Signature) },
                    isSending = sendingSection == DiagnosticSection.Signature,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Virtual Environment", "Virtual Environment"),
                    value = if (bypassVirtualEnvironment) {
                        tr("Bypass enabled", "Bypass aktif")
                    } else if (virtualEnvironmentDetected) {
                        tr("Emulator/VM detected", "Emulator/VM terdeteksi")
                    } else {
                        tr("Not detected", "Tidak terdeteksi")
                    },
                    detail = virtualEnvironmentDetail,
                    status = virtualEnvironmentStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.VirtualEnvironment) },
                    isSending = sendingSection == DiagnosticSection.VirtualEnvironment,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Clipboard", "Clipboard"),
                    value = if (clipboardBypassState == ClipboardBypassState.Tampered) {
                        tr(
                            "Bypass tamper detected; clipboard monitoring stays active",
                            "Tamper bypass terdeteksi; monitoring clipboard tetap aktif"
                        )
                    } else if (clipboardViolationCount > 0) {
                        tr(
                            "Clipboard changes were confirmed during the exam",
                            "Perubahan clipboard terkonfirmasi saat ujian"
                        )
                    } else if (bypassClipboard) {
                        tr("Bypass enabled", "Bypass aktif")
                    } else {
                        tr("Clipboard changes will trigger an alarm", "Perubahan clipboard akan memicu alarm")
                    },
                    detail = clipboardDetail,
                    status = when {
                        clipboardBypassState == ClipboardBypassState.Tampered ->
                            tr("Warning", "Peringatan")

                        bypassClipboard -> tr("Bypassed", "Bypass")
                        clipboardViolationCount > 0 -> tr("Warning", "Peringatan")
                        else -> tr("Monitored", "Dipantau")
                    },
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Clipboard) },
                    isSending = sendingSection == DiagnosticSection.Clipboard,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Geofence", "Geofence"),
                    value = when {
                        geofenceBypassState == GeofenceBypassState.Tampered -> tr(
                            "Bypass seal mismatch was detected. Geofence enforcement stays active.",
                            "Seal bypass tidak cocok terdeteksi. Enforcement geofence tetap aktif."
                        )
                        bypassGeofence -> tr(
                            "Exam-area position checks are bypassed by admin.",
                            "Pemeriksaan posisi area ujian dibypass oleh admin."
                        )
                        !geofenceRuntimeStatus.evaluation.enabled -> tr(
                            "This exam policy does not require a geofence.",
                            "Policy ujian ini tidak mewajibkan geofence."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> tr(
                            "The geofence policy from QR or Direct Link is invalid.",
                            "Policy geofence dari QR atau Direct Link tidak valid."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.PermissionMissing -> tr(
                            "Location permission is still missing for geofence validation.",
                            "Izin lokasi masih kurang untuk validasi geofence."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.PreciseRequired -> tr(
                            "Precise location is required to validate the exam area.",
                            "Lokasi presisi diperlukan untuk memvalidasi area ujian."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled -> tr(
                            "Location services are off, so the exam area cannot be validated.",
                            "Layanan lokasi mati sehingga area ujian tidak bisa divalidasi."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> tr(
                            "A location fix is not available yet.",
                            "Fix lokasi belum tersedia."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> tr(
                            "The last location fix is too old to validate the exam area reliably. Refresh the location first.",
                            "Fix lokasi terakhir terlalu lama untuk memvalidasi area ujian dengan andal. Refresh lokasi terlebih dahulu."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> tr(
                            "The current location accuracy is still too weak for strict geofence validation. Move to a more open area, then refresh location.",
                            "Akurasi lokasi saat ini masih terlalu lemah untuk validasi geofence ketat. Pindah ke area yang lebih terbuka lalu refresh lokasi."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> tr(
                            "The current location fix has no usable accuracy value yet. Refresh location to get a better fix.",
                            "Fix lokasi saat ini belum punya nilai akurasi yang bisa dipakai. Refresh lokasi untuk mendapatkan fix yang lebih baik."
                        )
                        geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> tr(
                            "This device is currently outside the allowed exam area. Open the geofence map to compare the current position with the exam area.",
                            "Perangkat ini saat ini berada di luar area ujian yang diizinkan. Buka peta geofence untuk membandingkan posisi saat ini dengan area ujian."
                        )
                        else -> tr(
                            "This device is inside the configured exam area.",
                            "Perangkat ini berada di dalam area ujian yang dikonfigurasi."
                        )
                    },
                    meta = geofenceMeta,
                    detail = geofenceDetail,
                    status = geofenceStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.Geofence) },
                    isSending = sendingSection == DiagnosticSection.Geofence,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Anti-Fake-Location", "Anti-Fake-Location"),
                    value = when {
                        fakeLocationBypassState == FakeLocationBypassState.Tampered -> tr(
                            "Bypass seal mismatch was detected. Anti-fake-location stays active.",
                            "Seal bypass tidak cocok terdeteksi. Anti-fake-location tetap aktif."
                        )
                        bypassFakeLocation -> tr(
                            "Mock-location and fake GPS checks are bypassed by admin.",
                            "Pemeriksaan mock-location dan fake GPS dibypass oleh admin."
                        )
                        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired -> tr(
                            "Location permission is required before anti-fake-location can validate this exam.",
                            "Izin lokasi wajib tersedia sebelum anti-fake-location bisa memvalidasi ujian ini."
                        )
                        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled -> tr(
                            "Location services must be turned on before anti-fake-location can validate this exam.",
                            "Layanan lokasi harus diaktifkan sebelum anti-fake-location bisa memvalidasi ujian ini."
                        )
                        fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable -> tr(
                            "Anti-fake-location is still waiting for a usable location snapshot.",
                            "Anti-fake-location masih menunggu snapshot lokasi yang bisa dipakai."
                        )
                        !fakeLocationRuntimeStatus.securityStatus.monitoringEnabled -> tr(
                            "Anti-fake-location monitoring is currently inactive for this exam.",
                            "Monitoring anti-fake-location saat ini nonaktif untuk ujian ini."
                        )
                        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Critical -> tr(
                            "Critical fake-location confidence was reached from combined spoof signals.",
                            "Confidence fake-location kritis tercapai dari kombinasi sinyal spoof."
                        )
                        fakeLocationRuntimeStatus.securityStatus.confidenceTier == LocationSpoofConfidenceTier.Strong -> tr(
                            "Strong mock-location or fake GPS signals were detected.",
                            "Terdeteksi sinyal mock-location atau fake GPS yang kuat."
                        )
                        fakeLocationRuntimeStatus.securityStatus.warningOnly -> tr(
                            "A suspicious fake-location app was found, but no strong spoof signal yet.",
                            "Aplikasi fake-location mencurigakan ditemukan, tetapi belum ada sinyal spoof kuat."
                        )
                        else -> tr(
                            "No strong fake-location signal is currently detected.",
                            "Saat ini tidak ada sinyal fake-location kuat yang terdeteksi."
                        )
                    },
                    detail = fakeLocationDetail,
                    status = fakeLocationStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.FakeLocation) },
                    isSending = sendingSection == DiagnosticSection.FakeLocation,
                    sendEnabled = sendingSection == null
                )
                SecurityChecklistItem(
                    title = tr("Screen Pinning", "Screen Pinning"),
                    value = if (bypassScreenPinning) {
                        tr("Bypass enabled", "Bypass aktif")
                    } else if (isScreenPinningActive) {
                        tr("Already active", "Sudah aktif")
                    } else if (!screenPinningAvailable) {
                        tr(
                            "Screen Pinning is not available on this device. Use another supported device, or ask Secret Admin to enable Screen Pinning bypass.",
                            "Screen Pinning tidak tersedia di perangkat ini. Gunakan perangkat lain yang mendukung, atau minta Secret Admin mengaktifkan bypass Screen Pinning."
                        )
                    } else if (screenPinningFixNeeded) {
                        tr(
                            "Screen Pinning is available but not active yet. Press START EXAM MODE, then confirm the Android Screen Pinning dialog.",
                            "Screen Pinning tersedia tetapi belum aktif. Tekan START EXAM MODE, lalu konfirmasi dialog Screen Pinning dari Android."
                        )
                    } else {
                        tr(
                            "Screen Pinning is available. It is not active yet and will be requested when START EXAM MODE is pressed.",
                            "Screen Pinning tersedia. Saat ini belum aktif dan akan diminta ketika START EXAM MODE ditekan."
                        )
                    },
                    detail = screenPinningDetail,
                    status = screenPinningStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.ScreenPinning) },
                    isSending = sendingSection == DiagnosticSection.ScreenPinning,
                    sendEnabled = sendingSection == null
                )
                if (!screenPinningAvailable && accessibilityGuardAvailable) {
                    SecurityChecklistItem(
                        title = tr("Accessibility Exam Guard", "Accessibility Exam Guard"),
                        value = when {
                            bypassScreenPinning -> tr(
                                "Screen Pinning bypass is active, so this fallback guard is not required.",
                                "Bypass Screen Pinning aktif, jadi guard fallback ini tidak wajib."
                            )
                            accessibilityGuardRequired && accessibilityGuardEnabled -> tr(
                                "Required fallback is enabled for this device.",
                                "Fallback wajib sudah aktif untuk perangkat ini."
                            )
                            accessibilityGuardRequired -> tr(
                                "Enable CBX Lock Exam Guard in Accessibility Settings before starting.",
                                "Aktifkan CBX Lock Exam Guard di Pengaturan Aksesibilitas sebelum mulai."
                            )
                            else -> tr(
                                "This fallback guard is only needed when Screen Pinning is unavailable.",
                                "Guard fallback ini hanya diperlukan saat Screen Pinning tidak tersedia."
                            )
                        },
                        detail = accessibilityGuardDetail,
                        status = accessibilityGuardStatusLabel,
                        onSendTelegram = { onRequestSectionReport(DiagnosticSection.AppSwitch) },
                        isSending = sendingSection == DiagnosticSection.AppSwitch,
                        sendEnabled = sendingSection == null
                    )
                }
                SecurityChecklistItem(
                    title = tr("App Switch", "App Switch"),
                    value = when {
                        bypassAppSwitch -> tr("Bypass enabled", "Bypass aktif")
                        appSwitchStatus.hasViolations -> tr(
                            "App switch violations recorded: ${appSwitchStatus.violationCount}",
                            "Pelanggaran App Switch tercatat: ${appSwitchStatus.violationCount}"
                        )
                        appSwitchStatus.fallbackGuardActive -> tr(
                            "Fallback guard is active because screen pinning is bypassed or inactive.",
                            "Fallback guard aktif karena screen pinning dibypass atau tidak aktif."
                        )
                        appSwitchStatus.runtimeMonitoringActive -> tr(
                            "Monitoring is active for this exam session.",
                            "Monitoring aktif untuk sesi ujian ini."
                        )
                        else -> tr(
                            "Monitoring will activate when the exam session starts.",
                            "Monitoring akan aktif saat sesi ujian dimulai."
                        )
                    },
                    detail = appSwitchDetail,
                    status = appSwitchStatusLabel,
                    onSendTelegram = { onRequestSectionReport(DiagnosticSection.AppSwitch) },
                    isSending = sendingSection == DiagnosticSection.AppSwitch,
                    sendEnabled = sendingSection == null
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (tamperDetected) {
            PreparationNoticeCard(
                title = tr("Security Check Failed", "Pemeriksaan Keamanan Gagal"),
                message = tr(
                    "Security checks failed. Close debugging or hooking tools and reopen the app.",
                    "Pemeriksaan keamanan gagal. Tutup tool debugging/hooking lalu buka ulang aplikasi."
                ),
                accentColor = Color(0xFFB34A4A),
                backgroundColor = Color(0xFFFFEFEF)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (webViewSessionResetInFlight) {
            PreparationNoticeCard(
                title = tr("Preparing Clean Exam Session", "Menyiapkan Sesi Ujian Bersih"),
                message = tr(
                    "CBX Lock is clearing cookies, local storage, and cached WebView data before the exam opens.",
                    "CBX Lock sedang membersihkan cookie, local storage, dan cache WebView sebelum ujian dibuka."
                ),
                accentColor = LockGoldDark,
                backgroundColor = Color(0xFFFFF8E8)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        webViewSessionResetError?.let { resetError ->
            PreparationNoticeCard(
                title = tr("Clean Session Retry Needed", "Perlu Ulangi Sesi Bersih"),
                message = resetError,
                accentColor = Color(0xFFB34A4A),
                backgroundColor = Color(0xFFFFEFEF)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isStartingExam && !bypassScreenPinning) {
            PreparationNoticeCard(
                title = tr("Waiting for Screen Pinning", "Menunggu Screen Pinning"),
                message = tr(
                    "Confirm the Android dialog with \"Got it\" or \"Pin\" so the exam is fully locked to this app.",
                    "Konfirmasi dialog Android dengan tombol \"Got it\" atau \"Pin\" agar ujian benar-benar terkunci di aplikasi ini."
                ),
                accentColor = LockGoldDark,
                backgroundColor = Color(0xFFFFF8E8)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (usingBuiltInExamKeyboard && !bypassKeyboardPolicy) {
            PreparationNoticeCard(
                title = tr("System Keyboard Is Not Compatible", "Keyboard Sistem Tidak Cocok"),
                message = tr(
                    "CBX Lock will switch to its internal keyboard when the exam starts.",
                    "CBX Lock akan beralih ke keyboard internal saat ujian dimulai."
                ),
                accentColor = LockGoldDark,
                backgroundColor = Color(0xFFFFF8E8)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (rootSecurityStatus.detected && !bypassRoot) {
            PreparationNoticeCard(
                title = tr("Root Device Detected", "Perangkat Root Terdeteksi"),
                message = tr(
                    "For security, continue the exam on a non-rooted device.",
                    "Demi keamanan, lanjutkan ujian pada perangkat yang tidak di-root."
                ),
                accentColor = Color(0xFFB34A4A),
                backgroundColor = Color(0xFFFFEFEF)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (rootSecurityStatus.selinuxPermissive && !bypassRoot && !rootSecurityStatus.detected) {
            PreparationNoticeCard(
                title = tr("SELinux Permissive", "SELinux Permisif"),
                message = tr(
                    "SELinux is not enforcing. The exam can continue, but security is reduced.",
                    "SELinux tidak enforcing. Ujian bisa lanjut, namun tingkat keamanan berkurang."
                ),
                accentColor = LockGoldDark,
                backgroundColor = Color(0xFFFFF8E8)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (networkReadinessStatus.verdict != NetworkReadinessVerdict.ConnectedStable) {
            PreparationNoticeCard(
                title = tr("Network Warning", "Peringatan Network"),
                message = when (networkReadinessStatus.verdict) {
                    NetworkReadinessVerdict.Offline -> tr(
                        "The exam can still start, but the device is offline right now. Stabilize Wi-Fi or cellular data first if the exam depends on internet.",
                        "Ujian tetap bisa dimulai, tetapi perangkat sedang offline. Stabilkan Wi-Fi atau data seluler terlebih dahulu jika ujian bergantung pada internet."
                    )
                    NetworkReadinessVerdict.Unstable -> tr(
                        "The connection has changed several times recently. The exam can continue, but a more stable network is recommended.",
                        "Koneksi berubah beberapa kali belakangan ini. Ujian bisa lanjut, tetapi jaringan yang lebih stabil sangat disarankan."
                    )
                    NetworkReadinessVerdict.CaptivePortal -> tr(
                        "This connection may still need a login or captive-portal confirmation before internet access is fully ready.",
                        "Koneksi ini mungkin masih membutuhkan login atau konfirmasi captive portal sebelum internet benar-benar siap."
                    )
                    NetworkReadinessVerdict.Unvalidated -> tr(
                        "Android has not validated this connection yet. The exam can still continue, but internet access may still be limited.",
                        "Android belum memvalidasi koneksi ini. Ujian tetap bisa lanjut, tetapi akses internet mungkin masih terbatas."
                    )
                    NetworkReadinessVerdict.AirplaneMode -> tr(
                        "Airplane mode is active. The exam can still continue, but no internet connection is currently available.",
                        "Mode pesawat sedang aktif. Ujian tetap bisa lanjut, tetapi saat ini tidak ada koneksi internet."
                    )
                    NetworkReadinessVerdict.ConnectedStable -> tr(
                        "Network monitoring found a connectivity warning.",
                        "Monitoring network menemukan peringatan konektivitas."
                    )
                },
                accentColor = LockGoldDark,
                backgroundColor = Color(0xFFFFF8E8)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        val showKeyboardFix = usingBuiltInExamKeyboard && !bypassKeyboardPolicy
        val showBluetoothPermissionFix =
            !bypassBluetooth && needsBluetoothPermission && !bluetoothPermissionGranted
        val showBluetoothFix = !bypassBluetooth && bluetoothEnabled
        val showAccessibilityFix = !bypassAccessibility && accessibilityServiceEnabled
        val showAccessibilityGuardFix = accessibilityGuardRequired && !accessibilityGuardEnabled
        val showOverlayAccessibilityFix =
            !bypassOverlay &&
                overlayRiskResult.quickFixTargets.contains(OverlayQuickFixTarget.AccessibilitySettings) &&
                !showAccessibilityFix
        val showOverlaySettingsFix =
            !bypassOverlay &&
                overlayRiskResult.quickFixTargets.contains(OverlayQuickFixTarget.OverlaySettings)
        val showAdbFix = !bypassAdb && adbInspection.blocking
        val showGeofenceRequestPermissionFix =
            !bypassGeofence &&
                geofenceRuntimeStatus.evaluation.enabled &&
                geofenceRuntimeStatus.securityStatus.finalVerdict in setOf(
                    GeofenceSecurityVerdict.PermissionMissing,
                    GeofenceSecurityVerdict.PreciseRequired
                )
        val showGeofenceOpenLocationServicesFix =
            !bypassGeofence &&
                geofenceRuntimeStatus.evaluation.enabled &&
                geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled
        val showGeofenceRefreshFix =
            !bypassGeofence &&
                geofenceRuntimeStatus.evaluation.enabled &&
                geofenceRuntimeStatus.securityStatus.finalVerdict in setOf(
                    GeofenceSecurityVerdict.NoFix,
                    GeofenceSecurityVerdict.StaleFix,
                    GeofenceSecurityVerdict.LowAccuracy,
                    GeofenceSecurityVerdict.MissingAccuracy,
                    GeofenceSecurityVerdict.Outside
                )
        val showGeofenceMapFix =
            !bypassGeofence &&
                geofenceRuntimeStatus.evaluation.enabled &&
                geofenceRuntimeStatus.securityStatus.finalVerdict == GeofenceSecurityVerdict.Outside
        val showFakeLocationRequestPermissionFix =
            !bypassFakeLocation &&
                fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired
        val showFakeLocationOpenLocationServicesFix =
            !bypassFakeLocation &&
                fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled
        val showFakeLocationRefreshFix =
            !bypassFakeLocation &&
                fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable
        val showLocationPermissionFix = showGeofenceRequestPermissionFix || showFakeLocationRequestPermissionFix
        val showLocationServicesFix = showGeofenceOpenLocationServicesFix || showFakeLocationOpenLocationServicesFix
        val showLocationRefreshFix = showGeofenceRefreshFix || showFakeLocationRefreshFix
        val showNetworkInternetSettingsFix =
            networkReadinessStatus.verdict in setOf(
                NetworkReadinessVerdict.Offline,
                NetworkReadinessVerdict.Unvalidated,
                NetworkReadinessVerdict.CaptivePortal,
                NetworkReadinessVerdict.AirplaneMode,
                NetworkReadinessVerdict.Unstable
            )
        val showNetworkWifiSettingsFix =
            networkReadinessStatus.verdict in setOf(
                NetworkReadinessVerdict.Offline,
                NetworkReadinessVerdict.Unvalidated,
                NetworkReadinessVerdict.CaptivePortal,
                NetworkReadinessVerdict.Unstable
            )
        val networkLooksCellular =
            networkReadinessStatus.transportLabel.contains("cellular", ignoreCase = true) ||
                (lastConnectedNetworkLabel?.contains("cellular", ignoreCase = true) == true)
        val showNetworkCellularSettingsFix =
            networkReadinessStatus.verdict != NetworkReadinessVerdict.ConnectedStable &&
                networkLooksCellular
        val showNetworkAirplaneModeSettingsFix =
            networkReadinessStatus.verdict == NetworkReadinessVerdict.AirplaneMode
        val showNetworkRefreshFix = networkReadinessStatus.verdict != NetworkReadinessVerdict.ConnectedStable
        val showDeviceTimeFix = !bypassDeviceTime && deviceTimeSecurityStatus.blocking
        val showFakeLocationDeveloperOptionsFix =
            !showAdbFix &&
                !bypassFakeLocation &&
                fakeLocationRuntimeStatus.securityStatus.monitoringEnabled &&
                fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled &&
                (
                    fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.PackageWarning ||
                        (
                            fakeLocationRuntimeStatus.securityStatus.finalVerdict == LocationSpoofSecurityVerdict.SpoofDetected &&
                                fakeLocationRuntimeStatus.securityStatus.confidenceTier in setOf(
                                    LocationSpoofConfidenceTier.Strong,
                                    LocationSpoofConfidenceTier.Critical
                                )
                            )
                    )
        val quickFixIssueActions = buildList<PreparationQuickFixAction> {
            fun addQuickFix(
                text: String,
                severity: QuickFixSeverity,
                target: QuickFixTarget?,
                priority: Int,
                filled: Boolean = false,
                loading: Boolean = false,
                enabled: Boolean = true,
                onClick: () -> Unit
            ) {
                add(
                    PreparationQuickFixAction(
                        text = text,
                        severity = severity,
                        target = target,
                        priority = priority,
                        filled = filled,
                        loading = loading,
                        enabled = enabled,
                        onClick = { runQuickFix(target, onClick) }
                    )
                )
            }

            if (showDeviceTimeFix) {
                addQuickFix(
                    text = tr("Enable Automatic Date & Time", "Aktifkan Tanggal & Waktu Otomatis"),
                    severity = QuickFixSeverity.Blocking,
                    target = QuickFixTarget.DeviceTime,
                    priority = 10,
                    filled = true,
                    onClick = onOpenDateTimeSettings
                )
            }
            if (reinstallApkFixNeeded) {
                addQuickFix(
                    text = tr("Install Official APK Again", "Instal Ulang APK Resmi"),
                    severity = QuickFixSeverity.Blocking,
                    target = null,
                    priority = 15,
                    filled = true,
                    onClick = onReinstallOfficialApk
                )
            }
            if (showAdbFix) {
                addQuickFix(
                    text = tr("Turn Off USB Debugging", "Matikan USB Debugging"),
                    severity = QuickFixSeverity.Blocking,
                    target = QuickFixTarget.All,
                    priority = 20,
                    onClick = onOpenDeveloperOptionsSettings
                )
            }
            if (showFakeLocationDeveloperOptionsFix) {
                addQuickFix(
                    text = tr("Turn Off Mock Location App", "Matikan Aplikasi Lokasi Palsu"),
                    severity = if (fakeLocationReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                    target = QuickFixTarget.Location,
                    priority = 30,
                    onClick = onOpenFakeLocationDeveloperOptionsSettings
                )
            }
            if (showAccessibilityFix) {
                addQuickFix(
                    text = tr("Disable Risky Accessibility Services", "Nonaktifkan Layanan Aksesibilitas Berisiko"),
                    severity = QuickFixSeverity.Blocking,
                    target = QuickFixTarget.All,
                    priority = 35,
                    onClick = onOpenAccessibilitySettings
                )
            }
            if (showAccessibilityGuardFix) {
                addQuickFix(
                    text = tr("Enable CBX Lock Exam Guard", "Aktifkan CBX Lock Exam Guard"),
                    severity = QuickFixSeverity.Blocking,
                    target = QuickFixTarget.All,
                    priority = 36,
                    filled = true,
                    onClick = onOpenAccessibilitySettings
                )
            }
            if (showLocationPermissionFix) {
                addQuickFix(
                    text = if (showGeofenceRequestPermissionFix) {
                        tr("Allow Precise Location", "Izinkan Lokasi Presisi")
                    } else {
                        tr("Allow Location Permission", "Izinkan Akses Lokasi")
                    },
                    severity = QuickFixSeverity.Blocking,
                    target = null,
                    priority = 40,
                    filled = true,
                    onClick = onRequestLocationPermission
                )
            }
            if (showLocationServicesFix) {
                addQuickFix(
                    text = tr("Turn On Location Services", "Aktifkan Layanan Lokasi"),
                    severity = QuickFixSeverity.Blocking,
                    target = QuickFixTarget.Location,
                    priority = 45,
                    onClick = onOpenLocationServicesSettings
                )
            }
            if (showLocationRefreshFix) {
                addQuickFix(
                    text = if (isRefreshingGeofence) {
                        tr("Refreshing Location...", "Sedang Refresh Lokasi...")
                    } else {
                        tr("Refresh Location Now", "Refresh Lokasi Sekarang")
                    },
                    severity = if (geofenceReady && fakeLocationReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                    target = null,
                    priority = 50,
                    loading = isRefreshingGeofence,
                    enabled = !isRefreshingGeofence,
                    onClick = onRefreshGeofenceLocation
                )
            }
            if (showGeofenceMapFix) {
                addQuickFix(
                    text = tr("Open Geofence Map", "Buka Peta Geofence"),
                    severity = if (geofenceReady) QuickFixSeverity.Warning else QuickFixSeverity.Blocking,
                    target = null,
                    priority = 55,
                    onClick = onOpenGeofenceMapViewer
                )
            }
            if (showBluetoothPermissionFix) {
                addQuickFix(
                    text = tr("Allow Bluetooth Access", "Izinkan Akses Bluetooth"),
                    severity = QuickFixSeverity.Blocking,
                    target = null,
                    priority = 60,
                    filled = true,
                    onClick = onGrantBluetoothPermission
                )
            }
            if (showBluetoothFix) {
                addQuickFix(
                    text = tr("Turn Off Bluetooth", "Matikan Bluetooth"),
                    severity = QuickFixSeverity.Blocking,
                    target = QuickFixTarget.All,
                    priority = 65,
                    onClick = onOpenBluetoothSettings
                )
            }

            val networkPrimaryIsRefresh = networkReadinessStatus.verdict == NetworkReadinessVerdict.Unstable
            if (showNetworkAirplaneModeSettingsFix) {
                addQuickFix(
                    text = tr("Turn Off Airplane Mode", "Matikan Mode Pesawat"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.Network,
                    priority = 70,
                    onClick = onOpenAirplaneModeSettings
                )
            } else if (networkPrimaryIsRefresh && showNetworkRefreshFix) {
                addQuickFix(
                    text = if (isRefreshingNetwork) {
                        tr("Refreshing Network...", "Sedang Refresh Network...")
                    } else {
                        tr("Refresh Network Status", "Refresh Status Network")
                    },
                    severity = QuickFixSeverity.Warning,
                    target = null,
                    priority = 70,
                    loading = isRefreshingNetwork,
                    enabled = !isRefreshingNetwork,
                    onClick = onRefreshNetworkStatus
                )
            } else if (showNetworkInternetSettingsFix) {
                addQuickFix(
                    text = tr("Open Internet Settings", "Buka Pengaturan Internet"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.Network,
                    priority = 70,
                    onClick = onOpenInternetSettings
                )
            }
            if (showNetworkWifiSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                addQuickFix(
                    text = tr("Open Wi-Fi Settings", "Buka Pengaturan Wi-Fi"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.Network,
                    priority = 75,
                    onClick = onOpenWifiSettings
                )
            }
            if (showNetworkCellularSettingsFix && !showNetworkAirplaneModeSettingsFix) {
                addQuickFix(
                    text = tr("Open Cellular Settings", "Buka Pengaturan Seluler"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.Network,
                    priority = 76,
                    onClick = onOpenCellularSettings
                )
            }
            if (showNetworkRefreshFix && !networkPrimaryIsRefresh) {
                addQuickFix(
                    text = if (isRefreshingNetwork) {
                        tr("Refreshing Network...", "Sedang Refresh Network...")
                    } else {
                        tr("Refresh Network Status", "Refresh Status Network")
                    },
                    severity = QuickFixSeverity.Warning,
                    target = null,
                    priority = 80,
                    loading = isRefreshingNetwork,
                    enabled = !isRefreshingNetwork,
                    onClick = onRefreshNetworkStatus
                )
            }

            if (showKeyboardFix) {
                addQuickFix(
                    text = tr("Choose System Keyboard", "Pilih Keyboard Sistem"),
                    severity = QuickFixSeverity.Warning,
                    target = null,
                    priority = 200,
                    filled = true,
                    onClick = onChooseKeyboard
                )
                addQuickFix(
                    text = tr("Open Keyboard Settings", "Buka Pengaturan Keyboard"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.All,
                    priority = 205,
                    onClick = onOpenKeyboardSettings
                )
            }
            if (screenPinningFixNeeded) {
                addQuickFix(
                    text = tr("Enable Screen Pinning", "Aktifkan Screen Pinning"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.ScreenPinning,
                    priority = 210,
                    onClick = onOpenScreenPinningSettings
                )
            }
            if (showOverlayAccessibilityFix) {
                addQuickFix(
                    text = tr("Review Accessibility for Overlay Risk", "Tinjau Aksesibilitas untuk Risiko Overlay"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.All,
                    priority = 220,
                    onClick = onOpenOverlayAccessibilitySettings
                )
            }
            if (showOverlaySettingsFix) {
                addQuickFix(
                    text = tr("Open Overlay Settings", "Buka Izin Overlay"),
                    severity = QuickFixSeverity.Warning,
                    target = QuickFixTarget.All,
                    priority = 225,
                    onClick = onOpenOverlaySettings
                )
            }
        }
        val quickFixActions = if (quickFixIssueActions.isEmpty()) {
            emptyList()
        } else {
            quickFixIssueActions + PreparationQuickFixAction(
                text = if (isRefreshingGeofence || isRefreshingNetwork) {
                    tr("Refreshing Checks...", "Sedang Refresh Pemeriksaan...")
                } else {
                    tr("Refresh All Security Checks", "Refresh Semua Pemeriksaan Keamanan")
                },
                severity = QuickFixSeverity.Warning,
                target = null,
                priority = 900,
                filled = false,
                loading = isRefreshingGeofence || isRefreshingNetwork,
                enabled = !(isRefreshingGeofence || isRefreshingNetwork),
                onClick = onRefreshAllSecurityChecks
            )
        }.sortedWith(
            compareBy<PreparationQuickFixAction> { action ->
                if (action.severity == QuickFixSeverity.Blocking) 0 else 1
            }.thenBy { it.priority }
        )
        val primaryQuickFixAction = quickFixActions.firstOrNull()
        val remainingQuickFixActions = quickFixActions.drop(1)
        val blockingQuickFixActions = remainingQuickFixActions.filter { it.severity == QuickFixSeverity.Blocking }
        val warningQuickFixActions = remainingQuickFixActions.filter { it.severity == QuickFixSeverity.Warning }
        val blockingQuickFixCount = quickFixActions.count { it.severity == QuickFixSeverity.Blocking }
        val warningQuickFixCount = quickFixActions.count { it.severity == QuickFixSeverity.Warning && it.priority != 900 }
        val showQuickFixesCard = quickFixActions.isNotEmpty()

        if (showQuickFixesCard) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, LockOutline),
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (blockingQuickFixCount > 0) {
                            tr("Fix Start Exam Blockers", "Beresi Penghambat Start Exam")
                        } else {
                            tr("Review Warnings", "Tinjau Peringatan")
                        },
                        color = LockTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (blockingQuickFixCount > 0) {
                            tr(
                                "$blockingQuickFixCount blocker(s) must be resolved before START EXAM MODE.",
                                "$blockingQuickFixCount penghambat harus dibereskan sebelum START EXAM MODE."
                            )
                        } else {
                            tr(
                                "$warningQuickFixCount warning(s) need review before starting the exam.",
                                "$warningQuickFixCount peringatan perlu ditinjau sebelum mulai ujian."
                            )
                        } + " " + tr(
                            "Complete the first action, then return here; checks refresh automatically after Settings.",
                            "Selesaikan tindakan pertama, lalu kembali ke sini; pemeriksaan otomatis refresh setelah dari Settings."
                        ),
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    val primaryAction = primaryQuickFixAction
                    if (primaryAction != null) {
                        PreparationAssistButton(
                            text = primaryAction.text,
                            labelPrefix = tr("Fix First", "Perbaiki Dulu"),
                            filled = true,
                            loading = primaryAction.loading,
                            enabled = primaryAction.enabled,
                            onClick = primaryAction.onClick
                        )
                    }

                    if (blockingQuickFixActions.isNotEmpty()) {
                        Text(
                            text = tr("Blocking Fixes", "Perbaikan Wajib"),
                            color = LockTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        blockingQuickFixActions.forEach { action ->
                            PreparationAssistButton(
                                text = action.text,
                                compact = true,
                                filled = action.filled,
                                loading = action.loading,
                                enabled = action.enabled,
                                onClick = action.onClick
                            )
                        }
                    }

                    if (warningQuickFixActions.isNotEmpty()) {
                        Text(
                            text = tr("Optional Checks", "Cek Opsional"),
                            color = LockTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        warningQuickFixActions.forEach { action ->
                            PreparationAssistButton(
                                text = action.text,
                                compact = true,
                                filled = action.filled,
                                loading = action.loading,
                                enabled = action.enabled,
                                onClick = action.onClick
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

            Spacer(modifier = Modifier.height(6.dp))
        }

        PreparationFloatingActionBar(
            startButtonColor = startButtonColor,
            startButtonContentColor = startButtonContentColor,
            isStartingExam = isStartingExam,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            onRefreshStatus = onRefreshStatus,
            onStartExam = onStartExam,
            onBackHome = onBackHome,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
