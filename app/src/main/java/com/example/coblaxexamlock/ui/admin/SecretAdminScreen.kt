package com.example.coblaxexamlock.ui.admin

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
import androidx.compose.runtime.SideEffect
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
import com.example.coblaxexamlock.AdbBypassResolver
import com.example.coblaxexamlock.AppSwitchBypassResolver
import com.example.coblaxexamlock.AppSwitchMonitor
import com.example.coblaxexamlock.AppSwitchProtectionMode
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardChangeDecision
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassResolver
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.ExamQrCodec
import com.example.coblaxexamlock.ExamQrExportHelper
import com.example.coblaxexamlock.ExamQrLocationPolicy
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.FakeLocationBypassResolver
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceBypassResolver
import com.example.coblaxexamlock.GeofencePoint
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceShapeType
import com.example.coblaxexamlock.IntegrityCheckResult
import com.example.coblaxexamlock.IntegrityGuard
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.OverlayRiskAnalyzer
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.R
import com.example.coblaxexamlock.QrCodeGenerator
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ReverseEngineeringResult
import com.example.coblaxexamlock.RootBypassResolver
import com.example.coblaxexamlock.ScreenPinningPlatformBridge
import com.example.coblaxexamlock.DeviceCompatibilityProfile
import com.example.coblaxexamlock.DeviceSurvivalPolicy
import com.example.coblaxexamlock.WebViewCompatibilityStatus
import com.example.coblaxexamlock.WebViewHealthSeverity
import com.example.coblaxexamlock.buildDeviceSurvivalPolicy
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.LocalDeviceCompatibilityProfile
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.launchFirstPlatformIntentSafely
import com.example.coblaxexamlock.config.DefaultExamUserAgent
import com.example.coblaxexamlock.config.DeveloperGithubUrl
import com.example.coblaxexamlock.config.PickerDialogColorScheme
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofence
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.evaluateLocationFixQuality
import com.example.coblaxexamlock.inspectDeviceTimeSecurity
import com.example.coblaxexamlock.format.buildIntegrityPublicSummary
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.inspectAdb
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.openWebViewProviderSettings
import com.example.coblaxexamlock.readWebViewCompatibilityStatus
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.CustomQrAdminTab
import com.example.coblaxexamlock.model.DateTimeField
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.SecretAdminTab
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.model.directLinkLocationPolicy
import com.example.coblaxexamlock.model.effectiveExamUserAgent
import com.example.coblaxexamlock.model.usesDefaultExamUserAgent
import com.example.coblaxexamlock.model.withoutDirectLinkLocationPolicy
import com.example.coblaxexamlock.parseGeofenceConfig
import com.example.coblaxexamlock.parseStoredDateTime
import com.example.coblaxexamlock.platform.openExternalUrl
import com.example.coblaxexamlock.runtime.getRootDetectionDetails
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.runtime.readExamBatteryStatus
import com.example.coblaxexamlock.runtime.readExamNetworkStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatusWithProbe
import com.example.coblaxexamlock.runtime.sendTelegramSectionReport
import com.example.coblaxexamlock.ui.geofence.CircleGeofenceEditorScreen
import com.example.coblaxexamlock.ui.geofence.PolygonGeofenceEditor
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleVertexList
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertexList
import com.example.coblaxexamlock.ui.exam.ExamRuntimeHardeningDiagnostics
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
import com.example.coblaxexamlock.viewmodel.CustomQrDraftState
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

private const val SecretAdminPerfTag = "SecretAdminPerf"
private const val SecretAdminRapidRecomposeWindowMs = 250L
private const val SecretAdminRapidRecomposeThreshold = 3

private inline fun <T> debugMeasureSecretAdminWork(label: String, block: () -> T): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                SecretAdminPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

private suspend inline fun <T> debugMeasureSecretAdminSuspendWork(
    label: String,
    crossinline block: suspend () -> T
): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                SecretAdminPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

@Composable
private fun DebugSecretAdminRecomposeTrace(selectedTab: SecretAdminTab) {
    if (!BuildConfig.DEBUG) return

    var burstCount by remember { mutableIntStateOf(0) }
    var lastCommitAtMs by remember { mutableLongStateOf(0L) }

    SideEffect {
        val now = SystemClock.elapsedRealtime()
        burstCount = if (
            lastCommitAtMs != 0L &&
            now - lastCommitAtMs <= SecretAdminRapidRecomposeWindowMs
        ) {
            burstCount + 1
        } else {
            1
        }
        lastCommitAtMs = now
        if (burstCount == SecretAdminRapidRecomposeThreshold) {
            Log.d(
                SecretAdminPerfTag,
                "Rapid recomposition burst detected on tab=${selectedTab.name}"
            )
        }
    }
}

@Composable
internal fun SecretAdminScreen(
    settings: AdminSettings,
    examName: String,
    onSettingsChange: (AdminSettings) -> Unit,
    onResetDirectLink: () -> Unit,
    onBack: () -> Unit,
    deviceTimeBaselineWallClockMillis: Long,
    deviceTimeBaselineElapsedRealtimeMillis: Long,
    modifier: Modifier = Modifier,
    selectedTabName: String = SecretAdminTab.Setup.name,
    onSelectedTabNameChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uiLanguage = LocalUiLanguage.current
    val lowRamProfile = LocalLowRamProfile.current
    val deviceCompatibilityProfile = LocalDeviceCompatibilityProfile.current
    val vendorChecklist = remember(deviceCompatibilityProfile.manufacturer, deviceCompatibilityProfile.brand) {
        resolveDeviceVendorChecklist(
            manufacturer = deviceCompatibilityProfile.manufacturer,
            brand = deviceCompatibilityProfile.brand
        )
    }
    val effectiveExamUserAgent = remember(settings.examUserAgent) {
        debugMeasureSecretAdminWork("effectiveExamUserAgent") {
            settings.effectiveExamUserAgent()
        }
    }
    val usesDefaultExamUserAgent = remember(settings.examUserAgent) {
        debugMeasureSecretAdminWork("usesDefaultExamUserAgent") {
            settings.usesDefaultExamUserAgent()
        }
    }
    val deviceTimeBaseline = remember(
        deviceTimeBaselineWallClockMillis,
        deviceTimeBaselineElapsedRealtimeMillis
    ) {
        DeviceTimeBaseline(
            wallClockMillis = deviceTimeBaselineWallClockMillis,
            elapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
        )
    }
    val examUserAgentSourceLabel = remember(usesDefaultExamUserAgent, uiLanguage) {
        if (usesDefaultExamUserAgent) {
            localized(uiLanguage, "Default", "Default")
        } else {
            localized(uiLanguage, "Custom", "Custom")
        }
    }
    val overridesActive = remember(
        settings.bypassScreenPinning,
        settings.bypassBluetooth,
        settings.bypassAccessibility,
        settings.bypassAdb,
        settings.bypassRoot,
        settings.bypassVirtualEnvironment,
        settings.bypassKeyboardPolicy,
        settings.bypassClipboard,
        settings.bypassOverlay,
        settings.bypassGeofence,
        settings.bypassFakeLocation,
        settings.bypassDeviceTime,
        settings.bypassAppSwitch
    ) {
        debugMeasureSecretAdminWork("hasAnyBypass") {
            settings.hasAnyBypass()
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var healthIntegritySummary by rememberSaveable { mutableStateOf("OK") }
    var healthReverseDetected by rememberSaveable { mutableStateOf(false) }
    var healthLastCheckedAt by rememberSaveable { mutableStateOf<String?>(null) }
    var healthBaselineFingerprint by rememberSaveable { mutableStateOf<String?>(null) }
    var healthChecking by rememberSaveable { mutableStateOf(false) }
    var healthIntegrityResult by remember { mutableStateOf<IntegrityCheckResult?>(null) }
    var healthReverseResult by remember { mutableStateOf<ReverseEngineeringResult?>(null) }
    var healthDeviceTimeStatus by remember { mutableStateOf<DeviceTimeSecurityStatus?>(null) }
    var pendingSecurityHealthReport by rememberSaveable { mutableStateOf(false) }
    var sendingSecurityHealthReport by rememberSaveable { mutableStateOf(false) }
    var securityHealthFeedbackTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var securityHealthFeedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var advancedDiagnosticsExpanded by rememberSaveable { mutableStateOf(false) }
    var fieldReadinessRunning by rememberSaveable { mutableStateOf(false) }
    var fieldReadinessReport by remember { mutableStateOf<FieldReadinessReport?>(null) }
    var adminWebViewRefreshKey by rememberSaveable { mutableIntStateOf(0) }
    val adminWebViewCompatibilityStatus = remember(context, adminWebViewRefreshKey) {
        readWebViewCompatibilityStatus(context.applicationContext)
    }
    val fieldSurvivalPolicy = remember(
        lowRamProfile,
        deviceCompatibilityProfile,
        adminWebViewCompatibilityStatus,
        fieldReadinessReport
    ) {
        buildDeviceSurvivalPolicy(
            lowRamProfile = lowRamProfile,
            deviceCompatibilityProfile = deviceCompatibilityProfile,
            webViewCompatibilityStatus = adminWebViewCompatibilityStatus,
            fieldReadinessReport = fieldReadinessReport
        )
    }
    val adminReadinessSummary = remember(
        fieldReadinessReport,
        adminWebViewCompatibilityStatus,
        vendorChecklist
    ) {
        buildAdminReadinessSummary(
            report = fieldReadinessReport,
            webViewCompatibilityStatus = adminWebViewCompatibilityStatus,
            vendorChecklist = vendorChecklist
        )
    }
    LaunchedEffect(adminWebViewCompatibilityStatus.diagnosticSummary()) {
        Log.i(
            "ExamRuntimeHardening",
            "code=${ExamRuntimeHardeningDiagnostics.WebViewProviderHealthResolved} level=INFO details=${adminWebViewCompatibilityStatus.diagnosticSummary()}"
        )
        if (adminWebViewCompatibilityStatus.severity != WebViewHealthSeverity.Stable) {
            Log.w(
                "ExamRuntimeHardening",
                "code=${ExamRuntimeHardeningDiagnostics.WebViewProviderHealthWarning} level=WARNING details=${adminWebViewCompatibilityStatus.adminDetail}"
            )
        }
    }

    fun runFieldReadinessTest() {
        if (fieldReadinessRunning) return
        fieldReadinessRunning = true
        Log.i(
            "ExamRuntimeHardening",
            "code=${ExamRuntimeHardeningDiagnostics.FieldReadinessTestStarted} level=INFO details=family=${deviceCompatibilityProfile.family.name}"
        )
        coroutineScope.launch {
            runCatching {
                val appContext = context.applicationContext
                val accessibilityInspection = inspectAccessibility(appContext)
                val directLinkPolicy = settings.directLinkLocationPolicy()
                val screenPinningAvailable = ScreenPinningPlatformBridge.isAvailable()
                val overlayRiskResult = OverlayRiskAnalyzer.inspect(
                    bypassed = settings.bypassOverlay,
                    accessibilityEnabled = accessibilityInspection.blockingServiceActive,
                    riskyAccessibilityPackages = accessibilityInspection.riskyPackages,
                    violationCount = 0,
                    shieldStatus = OverlayShieldStatus(
                        supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        requested = false,
                        lastApplySucceeded = null,
                        lastApplyAt = null
                    ),
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                )
                buildFieldReadinessReport(
                    FieldReadinessInput(
                        generatedAt = diagnosticTimestamp(),
                        compatibilityProfile = deviceCompatibilityProfile,
                        screenPinningAvailable = screenPinningAvailable,
                        screenPinningSystemSetting = ScreenPinningPlatformBridge.readSystemSetting(appContext),
                        lockTaskState = readSecretAdminLockTaskStateLabel(appContext),
                        accessibilityGuardAvailable = isExamGuardAccessibilityAvailable(appContext),
                        accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(appContext),
                        overlayRiskResult = overlayRiskResult,
                        webViewCompatibilityStatus = readWebViewCompatibilityStatus(appContext),
                        networkReadinessStatus = withContext(Dispatchers.IO) {
                            readNetworkReadinessStatusWithProbe(appContext)
                        },
                        batteryStatus = readExamBatteryStatus(appContext),
                        locationPermissionGranted = hasLocationPermissionForWifi(appContext),
                        preciseLocationGranted = hasFineLocationPermission(appContext),
                        locationServicesEnabled = isLocationServicesEnabled(appContext),
                        geofencePolicyEnabled = directLinkPolicy?.geofenceEnabled == true,
                        fakeLocationMonitoringEnabled = !settings.bypassFakeLocation,
                        deviceTimeSecurityStatus = inspectDeviceTimeSecurity(
                            context = appContext,
                            baseline = deviceTimeBaseline,
                            bypassState = DeviceTimeBypassResolver.stateOf(
                                enabled = settings.bypassDeviceTime,
                                tampered = settings.deviceTimeBypassTampered
                            )
                        )
                    )
                )
            }.onSuccess { report ->
                fieldReadinessReport = report
                Log.i(
                    "ExamRuntimeHardening",
                    "code=${ExamRuntimeHardeningDiagnostics.FieldReadinessTestCompleted} level=INFO details=${report.diagnosticSummary()}"
                )
            }.onFailure { throwable ->
                securityHealthFeedbackTitle = localized(
                    uiLanguage,
                    "Field test failed",
                    "Field test gagal"
                )
                securityHealthFeedbackMessage =
                    throwable.message ?: throwable.javaClass.simpleName
            }
            fieldReadinessRunning = false
        }
    }

    fun openSettingsIntent(action: String) {
        launchFirstPlatformIntentSafely(
            context,
            listOf(
                Intent(action),
                Intent(Settings.ACTION_SETTINGS)
            )
        )
    }

    fun openAdminWebViewProviderSettings() {
        Log.i(
            "ExamRuntimeHardening",
            "code=${ExamRuntimeHardeningDiagnostics.WebViewProviderHealthFixOpened} level=INFO details=${adminWebViewCompatibilityStatus.adminDetail}"
        )
        openWebViewProviderSettings(
            context = context,
            providerPackageName = adminWebViewCompatibilityStatus.packageName
        )
    }
    val directLinkPolicySummary = remember(
        settings.directLinkLocationPolicySaved,
        settings.directLinkLocationPolicySerialized,
        settings.directLinkGeofenceEnabled,
        settings.directLinkGeofenceCenterLat,
        settings.directLinkGeofenceCenterLng,
        settings.directLinkGeofenceRadiusMeters,
        uiLanguage
    ) {
        debugMeasureSecretAdminWork("directLinkPolicySummary") {
            if (settings.directLinkLocationPolicySaved) {
                when (val directLinkPolicy = settings.directLinkLocationPolicy()) {
                    null -> localized(
                        uiLanguage,
                        "Direct Link has no saved geofence policy.",
                        "Direct Link belum punya policy geofence tersimpan."
                    )
                    else -> when (directLinkPolicy.shapeType) {
                        GeofenceShapeType.Circle -> localized(
                            uiLanguage,
                            "Direct Link circle geofence saved from QR: ${directLinkPolicy.effectiveCircleCenters.size} centers | ${directLinkPolicy.radiusMeters} m | primary ${
                                directLinkPolicy.effectiveCircleCenters.firstOrNull()?.let { center ->
                                    "${center.latitude}, ${center.longitude}"
                                } ?: "-"
                            }",
                            "Geofence lingkaran Direct Link tersimpan dari QR: ${directLinkPolicy.effectiveCircleCenters.size} center | ${directLinkPolicy.radiusMeters} m | utama ${
                                directLinkPolicy.effectiveCircleCenters.firstOrNull()?.let { center ->
                                    "${center.latitude}, ${center.longitude}"
                                } ?: "-"
                            }"
                        )
                        GeofenceShapeType.Polygon -> localized(
                            uiLanguage,
                            "Direct Link polygon geofence saved from QR: ${directLinkPolicy.vertices.size} points.",
                            "Geofence polygon Direct Link tersimpan dari QR: ${directLinkPolicy.vertices.size} titik."
                        )
                        GeofenceShapeType.Disabled -> localized(
                            uiLanguage,
                            "Direct Link location policy saved from QR: geofence disabled.",
                            "Policy lokasi Direct Link tersimpan dari QR: geofence nonaktif."
                        )
                    }
                }
            } else {
                localized(
                    uiLanguage,
                    "Direct Link has no saved geofence policy.",
                    "Direct Link belum punya policy geofence tersimpan."
                )
            }
        }
    }
    val healthDeviceTimeLabel = remember(healthDeviceTimeStatus, uiLanguage) {
        when {
            healthDeviceTimeStatus == null -> "-"
            healthDeviceTimeStatus?.bypassState == DeviceTimeBypassState.Tampered ->
                localized(uiLanguage, "Tampered", "Tampered")
            healthDeviceTimeStatus?.bypassActive == true ->
                localized(uiLanguage, "Bypassed", "Bypass")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.Safe ->
                localized(uiLanguage, "Safe", "Aman")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled ->
                localized(uiLanguage, "Auto Date/Time Off", "Tanggal/Waktu Otomatis Nonaktif")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled ->
                localized(uiLanguage, "Auto Time Zone Off", "Zona Waktu Otomatis Nonaktif")
            healthDeviceTimeStatus?.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected ->
                localized(uiLanguage, "Clock Change", "Perubahan Jam")
            else -> localized(uiLanguage, "Action Needed", "Perlu Aksi")
        }
    }

    suspend fun refreshSecurityHealth() {
        if (healthChecking) return
        healthChecking = true
        try {
            val integrityResult = debugMeasureSecretAdminSuspendWork("refreshSecurityHealth:integrity") {
                withContext(Dispatchers.IO) {
                    IntegrityGuard.check(context, healthBaselineFingerprint)
                }
            }
            val reverseResult = debugMeasureSecretAdminSuspendWork("refreshSecurityHealth:reverse") {
                withContext(Dispatchers.IO) {
                    ReverseEngineeringGuard.inspect(context)
                }
            }
            if (healthBaselineFingerprint.isNullOrBlank() &&
                integrityResult.currentFingerprint.isNotBlank() &&
                integrityResult.currentFingerprint != "-"
            ) {
                healthBaselineFingerprint = integrityResult.currentFingerprint
            }
            val checkedAt = diagnosticTimestamp()
            val deviceTimeStatus = inspectDeviceTimeSecurity(
                context = context,
                baseline = deviceTimeBaseline,
                bypassState = DeviceTimeBypassResolver.stateOf(
                    enabled = settings.bypassDeviceTime,
                    tampered = settings.deviceTimeBypassTampered
                )
            )
            healthIntegrityResult = integrityResult
            healthReverseResult = reverseResult
            healthDeviceTimeStatus = deviceTimeStatus
            healthIntegritySummary = buildIntegrityPublicSummary(integrityResult.issues)
            healthReverseDetected = reverseResult.tamperDetected
            healthLastCheckedAt = checkedAt
        } finally {
            healthChecking = false
            if (BuildConfig.DEBUG) {
                Log.d(SecretAdminPerfTag, "refreshSecurityHealth state updated")
            }
        }
    }

    suspend fun sendSecurityHealthReport() {
        if (sendingSecurityHealthReport || healthChecking) return
        sendingSecurityHealthReport = true
        try {
            debugMeasureSecretAdminSuspendWork("sendSecurityHealthReport:refresh") {
                refreshSecurityHealth()
            }
            val latestIntegrityResult = healthIntegrityResult
            val latestReverseResult = healthReverseResult
            val latestDeviceTimeStatus =
                healthDeviceTimeStatus ?: inspectDeviceTimeSecurity(
                    context = context,
                    baseline = deviceTimeBaseline,
                    bypassState = DeviceTimeBypassResolver.stateOf(
                        enabled = settings.bypassDeviceTime,
                        tampered = settings.deviceTimeBypassTampered
                    )
                )
            val latestCheckedAt = healthLastCheckedAt ?: diagnosticTimestamp()
            val resolvedExamName = examName.trim()
                .ifBlank { settings.fastExamLabel.trim() }
                .ifBlank { "-" }
            val section = DiagnosticSection.SecurityHealth
            val sectionLabel = diagnosticSectionLabel(section, uiLanguage)

            if (latestIntegrityResult == null || latestReverseResult == null) {
                securityHealthFeedbackTitle =
                    localized(uiLanguage, "Diagnostics failed", "Kirim diagnostik gagal")
                securityHealthFeedbackMessage = localized(
                    uiLanguage,
                    "Security Health data is not ready yet. Refresh and try again.",
                    "Data Security Health belum siap. Refresh lalu coba lagi."
                )
                return
            }

            debugMeasureSecretAdminSuspendWork("sendSecurityHealthReport:telegram") {
                sendTelegramSectionReport(
                context = context,
                section = section,
                examName = resolvedExamName,
                examUserAgent = effectiveExamUserAgent,
                examUserAgentSource = if (settings.usesDefaultExamUserAgent()) "default" else "custom",
                participantContext = null,
                examSessionStarted = false,
                examRuntimeGuardsArmed = false,
                adminOverridesSummary = settings.overrideSummary(),
                keyboardPackage = "",
                keyboardAllowed = false,
                usingBuiltInExamKeyboard = false,
                bluetoothPermissionGranted = false,
                bluetoothEnabled = false,
                accessibilityServiceEnabled = false,
                bypassAccessibility = settings.bypassAccessibility,
                accessibilityBypassTampered = settings.accessibilityBypassTampered,
                adbInspection = inspectAdb(context),
                adbBypassState = AdbBypassResolver.stateOf(
                    enabled = settings.bypassAdb,
                    tampered = settings.adbBypassTampered
                ),
                rootSecurityStatus = buildRootSecurityStatus(getRootDetectionDetails(context)),
                rootBypassState = RootBypassResolver.stateOf(
                    enabled = settings.bypassRoot,
                    tampered = settings.rootBypassTampered
                ),
                clipboardSignature = "",
                clipboardViolationCount = 0,
                lastClipboardChangeEvent = "-",
                networkStatus = readExamNetworkStatus(context),
                clipboardRuntimeStatus = ClipboardRuntimeStatus(
                    lastObservedAt = null,
                    lastConfirmedAt = null,
                    lastObservedSignature = null,
                    lastDecision = ClipboardChangeDecision.Idle.diagnosticLabel(),
                    baselineSemanticSignature = null,
                    detectedSemanticSignature = null,
                    currentSemanticSignature = null
                ),
                offlineRuntimeStatus = ExamOfflineRuntimeStatus(
                    offlineActive = false,
                    offlineStartedAt = null,
                    currentOfflineDurationMs = null,
                    offlineWarningShown = false,
                    lastOfflineWarningAt = null,
                    lastOfflineDurationMs = null
                ),
                geofenceRuntimeStatus = GeofenceRuntimeStatus(
                    evaluation = evaluateGeofence(
                        configResult = parseGeofenceConfig(false, "", "", ""),
                        permissionGranted = hasLocationPermissionForWifi(context),
                        locationServicesEnabled = isLocationServicesEnabled(context),
                        locationSnapshot = null
                    ),
                    securityStatus = evaluateGeofenceSecurity(
                        configResult = parseGeofenceConfig(false, "", "", ""),
                        permissionGranted = hasLocationPermissionForWifi(context),
                        preciseLocationGranted = hasFineLocationPermission(context),
                        locationServicesEnabled = isLocationServicesEnabled(context),
                        locationSnapshot = null,
                        bypassState = GeofenceBypassResolver.stateOf(
                            enabled = settings.bypassGeofence,
                            tampered = settings.geofenceBypassTampered
                        )
                    ),
                    policySource = if (settings.bypassGeofence) {
                        LocationPolicySource.Bypassed
                    } else {
                        LocationPolicySource.DisabledNoPolicy
                    },
                    violationCount = 0,
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                fakeLocationRuntimeStatus = FakeLocationRuntimeStatus(
                    securityStatus = evaluateFakeLocationSecurity(
                        monitoringEnabled = true,
                        permissionGranted = hasLocationPermissionForWifi(context),
                        locationServicesEnabled = isLocationServicesEnabled(context),
                        locationSnapshot = null,
                        fixQualityStatus = evaluateLocationFixQuality(null),
                        developerOptionsEnabled = false,
                        suspiciousFakeLocationPackages = emptyList(),
                        bypassState = FakeLocationBypassResolver.stateOf(
                            enabled = settings.bypassFakeLocation,
                            tampered = settings.fakeLocationBypassTampered
                        )
                    ),
                    violationCount = 0,
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                overlayViolationCount = 0,
                overlayRiskResult = OverlayRiskAnalyzer.inspect(
                    bypassed = settings.bypassOverlay,
                    accessibilityEnabled = false,
                    riskyAccessibilityPackages = emptyList(),
                    violationCount = 0,
                    shieldStatus = OverlayShieldStatus(
                        supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        requested = false,
                        lastApplySucceeded = null,
                        lastApplyAt = null
                    ),
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                overlayBypassTampered = settings.overlayBypassTampered,
                appSwitchStatus = AppSwitchMonitor.statusOf(
                    bypassState = AppSwitchBypassResolver.stateOf(
                        enabled = settings.bypassAppSwitch,
                        tampered = settings.appSwitchBypassTampered
                    ),
                    runtimeMonitoringActive = false,
                    protectionMode = if (settings.bypassAppSwitch) {
                        AppSwitchProtectionMode.Bypassed
                    } else {
                        AppSwitchProtectionMode.ProtectedByPinning
                    },
                    lockTaskActive = false,
                    violationCount = 0,
                    pendingViolation = false,
                    lastTrigger = null,
                    lastDetectedAt = null,
                    lastContext = null
                ),
                appSwitchBypassTampered = settings.appSwitchBypassTampered,
                screenPinningAvailable = false,
                screenPinningEnabledInSystem = "-",
                lockTaskStateBeforePinningRequest = "-",
                lockTaskStateAfterPinningRequest = "-",
                screenPinningRequestOutcome = "-",
                screenPinningDialogLikelyShown = false,
                screenPinningUserActionInference = "-",
                screenPinningActivationDurationMs = null,
                examSessionCancelledByPinningFailure = false,
                isScreenPinningActive = false,
                bypassScreenPinning = settings.bypassScreenPinning,
                bypassOverlay = settings.bypassOverlay,
                bypassAppSwitch = settings.bypassAppSwitch,
                deviceTimeSecurityStatus = latestDeviceTimeStatus,
                bypassDeviceTime = settings.bypassDeviceTime,
                integritySummary = healthIntegritySummary,
                diagnosticEvents = emptyList(),
                uiLanguage = uiLanguage,
                healthIntegrityResult = latestIntegrityResult,
                healthReverseResult = latestReverseResult,
                healthLastCheckedAt = latestCheckedAt,
                webViewCompatibilityStatus = adminWebViewCompatibilityStatus
            )
            }.onSuccess {
                securityHealthFeedbackTitle =
                    localized(uiLanguage, "Diagnostics sent", "Diagnostik terkirim")
                securityHealthFeedbackMessage = localized(
                    uiLanguage,
                    "$sectionLabel diagnostics have been sent to Telegram.",
                    "Diagnostik $sectionLabel sudah dikirim ke Telegram."
                )
            }.onFailure { throwable ->
                securityHealthFeedbackTitle =
                    localized(uiLanguage, "Diagnostics failed", "Kirim diagnostik gagal")
                securityHealthFeedbackMessage =
                    throwable.message ?: localized(
                        uiLanguage,
                        "Diagnostics could not be sent to Telegram.",
                        "Data diagnostik belum berhasil dikirim ke Telegram."
                    )
            }
        } finally {
            sendingSecurityHealthReport = false
        }
    }

    LaunchedEffect(Unit) {
        refreshSecurityHealth()
    }

    val selectedSecretAdminTab = remember(selectedTabName) {
        debugMeasureSecretAdminWork("selectedSecretAdminTab") {
            runCatching {
                SecretAdminTab.valueOf(selectedTabName)
            }.getOrDefault(SecretAdminTab.Setup)
        }
    }
    val scrollState = rememberScrollState()

    DebugSecretAdminRecomposeTrace(selectedSecretAdminTab)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        BackPillButton(onClick = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tr("Secret Admin", "Admin Rahasia"),
            color = LockTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr(
                "Manage the direct link and security overrides.",
                "Atur direct link dan override keamanan."
            ),
            color = LockTextSecondary,
            fontSize = 15.sp
        )

        if (settings.bypassMigrationResetNotice) {
            Spacer(modifier = Modifier.height(14.dp))
            StatusBanner(
                message = tr(
                    "Security storage was upgraded. Existing bypasses were reset to safe OFF and must be re-enabled manually.",
                    "Penyimpanan keamanan telah ditingkatkan. Semua bypass lama direset ke OFF aman dan harus diaktifkan ulang secara manual."
                ),
                isError = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SecretAdminTabSelector(
            selectedTab = selectedSecretAdminTab,
            onTabSelected = { onSelectedTabNameChange(it.name) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            if (selectedSecretAdminTab == SecretAdminTab.Setup) {

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("Direct Link", "Direct Link"),
                    color = LockTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                AdminInputField(
                    value = settings.fastExamUrl,
                    onValueChange = {
                        onSettingsChange(settings.copy(fastExamUrl = it).withoutDirectLinkLocationPolicy())
                    },
                    placeholder = tr("Direct link URL", "URL Direct Link"),
                    keyboardType = KeyboardType.Uri
                )
                AdminInputField(
                    value = settings.fastExamLabel,
                    onValueChange = { onSettingsChange(settings.copy(fastExamLabel = it)) },
                    placeholder = tr("Direct link label", "Label Direct Link")
                )
                Text(
                    text = directLinkPolicySummary,
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = tr("Official APK URL", "URL APK Resmi"),
                    color = LockTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                AdminInputField(
                    value = settings.officialApkUrl,
                    onValueChange = { onSettingsChange(settings.copy(officialApkUrl = it)) },
                    placeholder = tr("Official APK download URL", "URL unduhan APK resmi"),
                    keyboardType = KeyboardType.Uri
                )
                Text(
                    text = tr("WebView User-Agent", "User-Agent WebView"),
                    color = LockTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                AdminInputField(
                    value = settings.examUserAgent,
                    onValueChange = { onSettingsChange(settings.copy(examUserAgent = it)) },
                    placeholder = DefaultExamUserAgent
                )
                Text(
                    text = localized(
                        uiLanguage,
                        "Used by the internal exam browser. Leave blank to reset to $DefaultExamUserAgent.",
                        "Dipakai oleh browser ujian internal. Kosongkan untuk kembali ke $DefaultExamUserAgent."
                    ),
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Active User-Agent", "User-Agent Aktif"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = settings.effectiveExamUserAgent(),
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("User-Agent source", "Sumber User-Agent"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (usesDefaultExamUserAgent) {
                            tr("Default", "Default")
                        } else {
                            tr("Custom", "Custom")
                        },
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AdminToggleRow(
                    title = tr(
                        "Enable Save to Direct Link in Custom QR",
                        "Aktifkan Simpan ke Direct Link di Custom QR"
                    ),
                    description = tr(
                        "Show the save-to-direct-link checkbox on Custom QR.",
                        "Tampilkan checkbox simpan ke Direct Link di Custom QR."
                    ),
                    checked = settings.customQrSaveToDirectLinkEnabled,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(customQrSaveToDirectLinkEnabled = it))
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onResetDirectLink) {
                        Text(tr("Reset to default", "Reset ke default"), color = LockBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
            }

            if (selectedSecretAdminTab == SecretAdminTab.Security) {
        AdminReadinessSummaryCard(
            summary = adminReadinessSummary,
            fieldReadinessRunning = fieldReadinessRunning,
            webViewStatus = adminWebViewCompatibilityStatus,
            onRunCheck = ::runFieldReadinessTest,
            onOpenWebViewSettings = ::openAdminWebViewProviderSettings,
            onOpenAdvanced = { advancedDiagnosticsExpanded = true }
        )

        Spacer(modifier = Modifier.height(18.dp))

        AdminAdvancedDiagnosticsCard(
            expanded = advancedDiagnosticsExpanded,
            onToggleExpanded = {
                advancedDiagnosticsExpanded = !advancedDiagnosticsExpanded
                if (advancedDiagnosticsExpanded) {
                    Log.i(
                        "ExamRuntimeHardening",
                        "code=${ExamRuntimeHardeningDiagnostics.VendorChecklistOpened} level=INFO details=vendor=${vendorChecklist.family.name}"
                    )
                }
            },
            report = fieldReadinessReport,
            survivalPolicy = fieldSurvivalPolicy,
            webViewStatus = adminWebViewCompatibilityStatus,
            vendorChecklist = vendorChecklist,
            deviceCompatibilityProfile = deviceCompatibilityProfile,
            onRefreshWebView = { adminWebViewRefreshKey += 1 },
            onOpenWebViewSettings = ::openAdminWebViewProviderSettings,
            onOpenBatterySettings = { openSettingsIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS) },
            onOpenLocationSettings = { openSettingsIntent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
            onOpenOverlaySettings = { openSettingsIntent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) },
            onOpenAppSettings = {
                launchFirstPlatformIntentSafely(
                    context,
                    listOf(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        ),
                        Intent(Settings.ACTION_SETTINGS)
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = LockSurfaceSoft,
            border = BorderStroke(1.dp, LockOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Security Overrides", "Override Keamanan"),
                        color = LockTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (overridesActive) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = LockGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, LockGold.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = tr("OVERRIDES ACTIVE", "OVERRIDE AKTIF"),
                                color = LockGoldDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                AdminToggleRow(
                    title = tr("Bypass Screen Pinning", "Bypass Screen Pinning"),
                    description = tr(
                        "Skip lock-task and pin confirmation.",
                        "Lewati lock-task dan konfirmasi pin."
                    ),
                    checked = settings.bypassScreenPinning,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassScreenPinning = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Bluetooth Checks", "Bypass Cek Bluetooth"),
                    description = tr(
                        "Ignore Bluetooth permission and status checks.",
                        "Abaikan izin dan status Bluetooth."
                    ),
                    checked = settings.bypassBluetooth,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassBluetooth = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Accessibility Checks", "Bypass Cek Aksesibilitas"),
                    description = tr(
                        "Ignore accessibility service warnings and blocks.",
                        "Abaikan peringatan dan blokir aksesibilitas."
                    ),
                    checked = settings.bypassAccessibility,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassAccessibility = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass ADB Checks", "Bypass Cek ADB"),
                    description = tr(
                        "Ignore USB debugging checks.",
                        "Abaikan pemeriksaan USB debugging."
                    ),
                    checked = settings.bypassAdb,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassAdb = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Root Checks", "Bypass Cek Root"),
                    description = tr(
                        "Ignore root device detection.",
                        "Abaikan deteksi perangkat root."
                    ),
                    checked = settings.bypassRoot,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassRoot = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Virtual Environment", "Bypass Virtual Environment"),
                    description = tr(
                        "Ignore emulator/VM detection.",
                        "Abaikan deteksi emulator/VM."
                    ),
                    checked = settings.bypassVirtualEnvironment,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassVirtualEnvironment = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Keyboard Policy", "Bypass Kebijakan Keyboard"),
                    description = tr(
                        "Allow any system keyboard without fallback.",
                        "Izinkan keyboard sistem apa pun tanpa fallback."
                    ),
                    checked = settings.bypassKeyboardPolicy,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassKeyboardPolicy = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Clipboard Monitoring", "Bypass Monitoring Clipboard"),
                    description = tr(
                        "Disable clipboard change alarms.",
                        "Matikan alarm perubahan clipboard."
                    ),
                    checked = settings.bypassClipboard,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassClipboard = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Overlay Detection", "Bypass Deteksi Overlay"),
                    description = tr(
                        "Ignore obscured touch alerts.",
                        "Abaikan peringatan sentuhan tertutup."
                    ),
                    checked = settings.bypassOverlay,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassOverlay = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Geofence", "Bypass Geofence"),
                    description = tr(
                        "Skip exam-area position enforcement.",
                        "Lewati enforcement posisi area ujian."
                    ),
                    checked = settings.bypassGeofence,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassGeofence = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Anti-Fake-Location", "Bypass Anti-Fake-Location"),
                    description = tr(
                        "Skip mock-location and fake GPS enforcement.",
                        "Lewati enforcement mock-location dan fake GPS."
                    ),
                    checked = settings.bypassFakeLocation,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassFakeLocation = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Device Time", "Bypass Waktu Perangkat"),
                    description = tr(
                        "Skip automatic date & time, automatic time zone, and clock-change checks.",
                        "Lewati cek tanggal & waktu otomatis, zona waktu otomatis, dan perubahan jam."
                    ),
                    checked = settings.bypassDeviceTime,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassDeviceTime = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass App Switch Alerts", "Bypass Peringatan App Switch"),
                    description = tr(
                        "Disable forced-exit alarms on app switching.",
                        "Matikan alarm keluar paksa saat pindah aplikasi."
                    ),
                    checked = settings.bypassAppSwitch,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassAppSwitch = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
            }

            if (selectedSecretAdminTab == SecretAdminTab.Security) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = LockSurfaceSoft,
            border = BorderStroke(1.dp, LockOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("Checklist Details", "Detail Checklist"),
                    color = LockTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                AdminToggleRow(
                    title = tr("Show Checklist Details", "Tampilkan Detail Checklist"),
                    description = tr(
                        "Show the full technical checks on the preparation checklist.",
                        "Tampilkan detail teknis pemeriksaan di checklist persiapan."
                    ),
                    checked = settings.showChecklistDetails,
                    onCheckedChange = { onSettingsChange(settings.copy(showChecklistDetails = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
            }

            if (selectedSecretAdminTab == SecretAdminTab.Setup) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = LockSurfaceSoft,
            border = BorderStroke(1.dp, LockOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Security Health", "Kesehatan Keamanan"),
                        color = LockTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (healthChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LockBlue
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IntegrityGuard",
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = healthIntegritySummary.ifBlank { "-" },
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reverse Engineering",
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (healthReverseDetected) {
                            tr("Detected", "Terdeteksi")
                        } else {
                            "OK"
                        },
                        color = if (healthReverseDetected) Color(0xFFB42318) else LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Device Time", "Waktu Perangkat"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = healthDeviceTimeLabel,
                        color = if (healthDeviceTimeStatus?.blocking == true) Color(0xFFB42318) else LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Last checked", "Terakhir dicek"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = healthLastCheckedAt ?: "-",
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Exam User-Agent", "User-Agent Ujian"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = effectiveExamUserAgent,
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("User-Agent source", "Sumber User-Agent"),
                        color = LockTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = examUserAgentSourceLabel,
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val securityHealthSendEnabled =
                    !healthChecking && !sendingSecurityHealthReport &&
                        healthIntegrityResult != null && healthReverseResult != null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { coroutineScope.launch { refreshSecurityHealth() } },
                        enabled = !healthChecking,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockBlue,
                            contentColor = LockOnDark
                        )
                    ) {
                        Text(
                            text = tr("Refresh", "Refresh"),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { pendingSecurityHealthReport = true },
                        enabled = securityHealthSendEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2AABEE),
                            contentColor = LockOnDark,
                            disabledContainerColor = Color(0xFFB5DDF3),
                            disabledContentColor = LockOnDark.copy(alpha = 0.75f)
                        )
                    ) {
                        if (sendingSecurityHealthReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LockOnDark
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = tr(
                                    "Send Security Health diagnostics to Telegram",
                                    "Kirim diagnostik Security Health ke Telegram"
                                ),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Telegram",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (pendingSecurityHealthReport) {
        val sectionLabel = diagnosticSectionLabel(DiagnosticSection.SecurityHealth, uiLanguage)
        AlertDialog(
            onDismissRequest = { pendingSecurityHealthReport = false },
            title = { Text(tr("Send diagnostics?", "Kirim diagnostik?")) },
            text = {
                Text(
                    text = localized(
                        uiLanguage,
                        "Send diagnostics for $sectionLabel to Telegram?",
                        "Kirim diagnostik $sectionLabel ke Telegram?"
                    ),
                    color = LockTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSecurityHealthReport = false
                        coroutineScope.launch { sendSecurityHealthReport() }
                    }
                ) {
                    Text(tr("Send", "Kirim"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSecurityHealthReport = false }) {
                    Text(tr("Cancel", "Batal"))
                }
            }
        )
    }

    securityHealthFeedbackMessage?.let { message ->
        InfoDialog(
            title = securityHealthFeedbackTitle ?: "Info",
            message = message,
            onDismiss = {
                securityHealthFeedbackTitle = null
                securityHealthFeedbackMessage = null
            }
        )
    }
}

@Composable
private fun AdminReadinessSummaryCard(
    summary: AdminReadinessSummary,
    fieldReadinessRunning: Boolean,
    webViewStatus: WebViewCompatibilityStatus,
    onRunCheck: () -> Unit,
    onOpenWebViewSettings: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    val statusColor = adminReadinessVerdictColor(summary.verdict)
    val securityLabel = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> tr("Not checked", "Belum dicek")
        AdminReadinessVerdict.Ready -> tr("Ready", "Siap")
        AdminReadinessVerdict.NeedsSetup -> tr("Need Check", "Perlu Dicek")
        AdminReadinessVerdict.Blocked -> tr("Blocked", "Terblokir")
    }
    val primaryClick = when (summary.verdict) {
        AdminReadinessVerdict.NotRun -> onRunCheck
        AdminReadinessVerdict.Ready -> onOpenAdvanced
        AdminReadinessVerdict.NeedsSetup,
        AdminReadinessVerdict.Blocked -> {
            if (webViewStatus.severity != WebViewHealthSeverity.Stable) {
                onOpenWebViewSettings
            } else {
                onOpenAdvanced
            }
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.24f)),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Device Readiness", "Kesiapan Perangkat"),
                        color = LockTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary.detail,
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = summary.title,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminHealthLine(
                    label = tr("WebView", "WebView"),
                    value = summary.webViewLabel
                )
                AdminHealthLine(
                    label = tr("Security", "Keamanan"),
                    value = securityLabel
                )
                AdminHealthLine(
                    label = tr("Vendor", "Vendor"),
                    value = summary.vendorLabel
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = primaryClick,
                    enabled = !fieldReadinessRunning,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = LockOnDark,
                        disabledContainerColor = statusColor.copy(alpha = 0.42f),
                        disabledContentColor = LockOnDark.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (fieldReadinessRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LockOnDark
                        )
                    } else {
                        Text(
                            text = summary.nextActionLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (summary.verdict != AdminReadinessVerdict.NotRun) {
                    TextButton(
                        onClick = onRunCheck,
                        enabled = !fieldReadinessRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = tr("Run Check", "Cek Ulang"),
                            color = LockBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(
                    onClick = onOpenAdvanced,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tr("Details", "Detail"),
                        color = LockBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAdvancedDiagnosticsCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    report: FieldReadinessReport?,
    survivalPolicy: DeviceSurvivalPolicy,
    webViewStatus: WebViewCompatibilityStatus,
    vendorChecklist: DeviceVendorChecklist,
    deviceCompatibilityProfile: DeviceCompatibilityProfile,
    onRefreshWebView: () -> Unit,
    onOpenWebViewSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Advanced Diagnostics", "Diagnostik Lanjutan"),
                        color = LockTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            "Technical details are hidden until needed.",
                            "Detail teknis disembunyikan sampai dibutuhkan."
                        ),
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                TextButton(onClick = onToggleExpanded) {
                    Text(
                        text = if (expanded) tr("Hide", "Tutup") else tr("Open", "Buka"),
                        color = LockBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!expanded) {
                Text(
                    text = tr(
                        "Open only for troubleshooting.",
                        "Buka hanya saat troubleshooting."
                    ),
                    color = LockTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                return@Column
            }

            AdminDiagnosticDivider()
            AdminDiagnosticSectionTitle(tr("WebView Provider", "Provider WebView"))
            AdminHealthLine(tr("Status", "Status"), "${webViewStatus.verdict.name} / ${webViewStatus.severity.name}")
            AdminHealthLine(tr("Provider", "Provider"), webViewStatus.providerLabel)
            AdminHealthLine(tr("Package", "Package"), webViewStatus.packageName)
            AdminHealthLine(tr("Version", "Versi"), webViewStatus.versionLabel)
            AdminHealthLine(tr("Source", "Sumber"), webViewStatus.providerSource)
            AdminHealthLine(
                tr("Survival score", "Skor survival"),
                "${survivalPolicy.score.name} / ${survivalPolicy.runtimeTier.name}"
            )
            webViewStatus.quickFix?.takeIf { it.isNotBlank() }?.let { quickFix ->
                Text(
                    text = quickFix,
                    color = LockTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onRefreshWebView,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Refresh", "Refresh"), color = LockBlue, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onOpenWebViewSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Open Settings", "Buka Setelan"), color = LockBlue, fontWeight = FontWeight.Bold)
                }
            }

            AdminDiagnosticDivider()
            AdminDiagnosticSectionTitle(tr("Field Readiness Details", "Detail Field Readiness"))
            FieldReadinessReportCard(
                report = report,
                survivalPolicy = survivalPolicy
            )

            AdminDiagnosticDivider()
            AdminDiagnosticSectionTitle(tr("Device Setup Checklist", "Checklist Setup Perangkat"))
            AdminHealthLine(
                label = tr("Vendor", "Vendor"),
                value = vendorChecklist.displayName
            )
            AdminHealthLine(
                label = tr("Compatibility", "Kompatibilitas"),
                value = "${deviceCompatibilityProfile.family.name} | ${deviceCompatibilityProfile.model}"
            )
            vendorChecklist.items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.title,
                        color = LockTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.detail,
                        color = LockTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Battery", "Baterai"), color = LockBlue)
                }
                TextButton(
                    onClick = onOpenLocationSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Location", "Lokasi"), color = LockBlue)
                }
                TextButton(
                    onClick = onOpenOverlaySettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(tr("Overlay", "Overlay"), color = LockBlue)
                }
            }
            TextButton(
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr("Open App Settings", "Buka Setelan Aplikasi"),
                    color = LockBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AdminDiagnosticSectionTitle(text: String) {
    Text(
        text = text,
        color = LockTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun AdminDiagnosticDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LockOutline.copy(alpha = 0.8f))
    )
}

private fun adminReadinessVerdictColor(verdict: AdminReadinessVerdict): Color {
    return when (verdict) {
        AdminReadinessVerdict.NotRun -> LockBlue
        AdminReadinessVerdict.Ready -> Color(0xFF2F8F63)
        AdminReadinessVerdict.NeedsSetup -> LockGoldDark
        AdminReadinessVerdict.Blocked -> Color(0xFFB42318)
    }
}

@Composable
private fun AdminHealthLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = LockTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value.ifBlank { "-" },
            color = LockTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun FieldReadinessReportCard(
    report: FieldReadinessReport?,
    survivalPolicy: DeviceSurvivalPolicy
) {
    if (report == null) {
        Text(
            text = tr(
                "No field test yet. Run it on the actual device before exam day.",
                "Belum ada field test. Jalankan di perangkat asli sebelum hari ujian."
            ),
            color = LockTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        return
    }

    val statusColor = fieldReadinessVerdictColor(report.finalVerdict)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr("Last result", "Hasil terakhir"),
                color = LockTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "ready=${report.readyCount} warning=${report.warningCount} blocked=${report.blockedCount}",
                color = LockTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Text(
                text = "score=${survivalPolicy.score.name} runtime=${survivalPolicy.runtimeTier.name}",
                color = LockTextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Text(
                text = survivalPolicy.webViewRiskLabel,
                color = LockTextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = statusColor.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.36f))
        ) {
            Text(
                text = report.finalVerdict.name.uppercase(),
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        report.items.forEach { item ->
            val itemColor = fieldReadinessVerdictColor(item.verdict)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.verdict.name.take(1),
                    color = itemColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = LockTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.detail,
                        color = LockTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    if (!item.quickFix.isNullOrBlank()) {
                        Text(
                            text = item.quickFix,
                            color = itemColor,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun fieldReadinessVerdictColor(verdict: FieldReadinessVerdict): Color {
    return when (verdict) {
        FieldReadinessVerdict.Ready -> Color(0xFF2F8F63)
        FieldReadinessVerdict.Warning -> LockGoldDark
        FieldReadinessVerdict.Blocked -> Color(0xFFB42318)
    }
}

private fun readSecretAdminLockTaskStateLabel(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return "Unsupported"
    }
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val state = runCatching { activityManager?.lockTaskModeState }.getOrNull()
    return when (state) {
        ActivityManager.LOCK_TASK_MODE_LOCKED -> "LOCKED"
        ActivityManager.LOCK_TASK_MODE_PINNED -> "PINNED"
        ActivityManager.LOCK_TASK_MODE_NONE -> "NONE"
        null -> "Unknown"
        else -> "Unknown($state)"
    }
}

@Composable
internal fun CustomQrAdminTabSelector(
    selectedTab: CustomQrAdminTab,
    onTabSelected: (CustomQrAdminTab) -> Unit
) {
    val tabs = listOf(
        CustomQrAdminTab.Exam to tr("Exam", "Ujian"),
        CustomQrAdminTab.Location to tr("Location", "Lokasi"),
        CustomQrAdminTab.Generate to tr("Generate", "Generate")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, label) ->
                val selected = tab == selectedTab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onTabSelected(tab) }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) LockBlue else Color.Transparent
                ) {
                    Text(
                        text = label,
                        color = if (selected) LockOnDark else LockTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SecretAdminTabSelector(
    selectedTab: SecretAdminTab,
    onTabSelected: (SecretAdminTab) -> Unit
) {
    val tabs = listOf(
        SecretAdminTab.Setup to tr("Setup", "Setup"),
        SecretAdminTab.Security to tr("Security", "Security")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = LockSurfaceSoft,
        border = BorderStroke(1.dp, LockOutline)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, label) ->
                val selected = tab == selectedTab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onTabSelected(tab) }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) LockBlue else Color.Transparent
                ) {
                    Text(
                        text = label,
                        color = if (selected) LockOnDark else LockTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)
                    )
                }
            }
        }
    }
}
