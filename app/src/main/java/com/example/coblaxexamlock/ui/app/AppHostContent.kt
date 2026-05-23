package com.example.coblaxexamlock.ui.app

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
import android.util.Log
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.runtime.withFrameNanos
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.coblaxexamlock.AdminAuth
import com.example.coblaxexamlock.AdminAuthSession
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassResolver
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.ExamScheduleDefaults
import com.example.coblaxexamlock.ExamQrCodec
import com.example.coblaxexamlock.ExamQrLocationPolicy
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.ExamScheduleValidationResult
import com.example.coblaxexamlock.ExamScheduleValidator
import com.example.coblaxexamlock.ExamUrlValidationError
import com.example.coblaxexamlock.GeofenceShapeType
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.LocalDeviceCompatibilityProfile
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.LowRamProfileOverride
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.MemoryPressureCoordinator
import com.example.coblaxexamlock.QrPortraitCaptureActivity
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.StartupTrace
import com.example.coblaxexamlock.TrustedNetworkTimeCoordinator
import com.example.coblaxexamlock.applyLowRamProfileOverride
import com.example.coblaxexamlock.captureDeviceTimeBaseline
import com.example.coblaxexamlock.currentDeviceCompatibilityProfile
import com.example.coblaxexamlock.inspectDeviceTimeSecurity
import com.example.coblaxexamlock.validateExamUrl
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.config.QrImageReadErrorDecode
import com.example.coblaxexamlock.config.QrImageReadErrorOpen
import com.example.coblaxexamlock.config.SecretTapWindowMs
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.AppScreen
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.model.directLinkLocationPolicy
import com.example.coblaxexamlock.model.effectiveExamUserAgent
import com.example.coblaxexamlock.model.withDirectLinkLocationPolicy
import com.example.coblaxexamlock.model.withoutDirectLinkLocationPolicy
import com.example.coblaxexamlock.persistence.HomeAdminSettings
import com.example.coblaxexamlock.persistence.readHomeAdminSettings
import com.example.coblaxexamlock.persistence.readAdminSettings
import com.example.coblaxexamlock.persistence.readSavedUiLanguage
import com.example.coblaxexamlock.persistence.saveAdminSettings
import com.example.coblaxexamlock.persistence.saveUiLanguage
import com.example.coblaxexamlock.resolveDetectedLowRamProfile
import com.example.coblaxexamlock.resolveLowRamProfile
import com.example.coblaxexamlock.resolveRuntimePressureProfile
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
import com.example.coblaxexamlock.runtime.decodeQrPayloadFromImageUri
import com.example.coblaxexamlock.save.ExamQrPayloadSaver
import com.example.coblaxexamlock.ui.admin.AdminPasswordDialog
import com.example.coblaxexamlock.ui.admin.CustomQrAdminScreen
import com.example.coblaxexamlock.ui.admin.ExamLockLowRamHomeScreen
import com.example.coblaxexamlock.ui.admin.ExamLockHomeScreen
import com.example.coblaxexamlock.ui.admin.InfoDialog
import com.example.coblaxexamlock.ui.admin.PublicPerformanceProfileDialog
import com.example.coblaxexamlock.ui.admin.ScanSourceDialog
import com.example.coblaxexamlock.ui.admin.SecretAdminScreen
import com.example.coblaxexamlock.ui.exam.ExamWebViewScreen
import com.example.coblaxexamlock.ui.exam.ExamRuntimeHardeningDiagnostics
import com.example.coblaxexamlock.ui.exam.ExamRuntimeHardeningLogTag
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
import com.example.coblaxexamlock.viewmodel.AdminFlowUiAction
import com.example.coblaxexamlock.viewmodel.AdminFlowUiState
import com.example.coblaxexamlock.viewmodel.AdminFlowViewModel
import com.example.coblaxexamlock.viewmodel.ExamRuntimeUiAction
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val AdminSettingsPerfTag = "AdminSettingsPerf"
private const val LowRamProfilePerfTag = "LowRamProfile"

private enum class AppRecoveryRoute {
    Home,
    ExamFlowPreparation,
    ExamFlowRuntime,
    CustomQrAdmin,
    SecretAdmin
}

internal enum class PendingHomeAction {
    RuntimeHome,
    ScanExam,
    CustomQrAdmin,
    DirectLink,
    SecretAdmin
}

private fun parseAppRecoveryRoute(rawValue: String?): AppRecoveryRoute =
    rawValue
        ?.let { value -> runCatching { AppRecoveryRoute.valueOf(value) }.getOrNull() }
        ?: AppRecoveryRoute.Home

private fun readLowRamRuntimeMemoryInfo(context: Context): ActivityManager.MemoryInfo? {
    val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)
        ?: return null
    return ActivityManager.MemoryInfo().also { info ->
        runCatching { activityManager.getMemoryInfo(info) }
    }
}

private fun isFreshAdminFlowUiState(uiState: AdminFlowUiState): Boolean = uiState == AdminFlowUiState()

private fun detectProcessDeathRecovery(
    shellStateRestored: Boolean,
    currentViewModelInstanceId: String,
    savedViewModelInstanceId: String,
    uiState: AdminFlowUiState,
    routeSnapshot: AppRecoveryRoute,
    activeExamPayload: ExamQrPayload?
): Boolean {
    if (!shellStateRestored) {
        return false
    }
    if (savedViewModelInstanceId == currentViewModelInstanceId) {
        return false
    }
    if (!isFreshAdminFlowUiState(uiState)) {
        return false
    }
    return routeSnapshot != AppRecoveryRoute.Home || activeExamPayload != null
}

private fun currentDeviceTimeBypassState(settings: AdminSettings): DeviceTimeBypassState {
    return DeviceTimeBypassResolver.stateOf(
        enabled = settings.bypassDeviceTime,
        tampered = settings.deviceTimeBypassTampered
    )
}

private fun deviceTimeQrBlockMessage(
    status: DeviceTimeSecurityStatus,
    uiLanguage: UiLanguage
): String {
    return when {
        status.bypassState == DeviceTimeBypassState.Tampered -> localized(
            uiLanguage,
            "Device Time bypass storage was tampered with. Device Time enforcement remains active.",
            "Tamper terdeteksi pada storage bypass Waktu Perangkat. Enforcement Waktu Perangkat tetap aktif."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> localized(
            uiLanguage,
            "Turn on automatic date & time before opening this QR.",
            "Aktifkan tanggal & waktu otomatis sebelum membuka QR ini."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> localized(
            uiLanguage,
            "Turn on automatic time zone before opening this QR.",
            "Aktifkan zona waktu otomatis sebelum membuka QR ini."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> localized(
            uiLanguage,
            "A suspicious clock change was detected. Turn automatic date & time back on, then scan again.",
            "Terdeteksi perubahan jam yang mencurigakan. Aktifkan kembali tanggal & waktu otomatis, lalu pindai lagi."
        )
        else -> localized(
            uiLanguage,
            "Device time could not be trusted. Check the date & time settings, then try again.",
            "Waktu perangkat tidak dapat dipercaya. Periksa pengaturan tanggal & waktu, lalu coba lagi."
        )
    }
}

private fun deviceTimeEventDetails(status: DeviceTimeSecurityStatus, trigger: String): String {
    return buildString {
        append("trigger=")
        append(trigger)
        append(" | verdict=")
        append(status.finalVerdict.name.lowercase(Locale.US))
        append(" | auto_time=")
        append(if (status.autoTimeEnabled) "on" else "off")
        append(" | auto_time_zone=")
        append(if (status.autoTimeZoneEnabled) "on" else "off")
        append(" | drift_ms=")
        append(status.clockDriftMillis)
        append(" | timezone=")
        append(status.timezoneSummary)
        append(" | bypass=")
        append(status.bypassState.name.lowercase(Locale.US))
    }
}

@Composable
internal fun AppHostRuntimeContent(
    initialUiLanguageOverride: UiLanguage? = null,
    initialHomeAdminSettings: HomeAdminSettings? = null,
    initialLowRamProfile: LowRamProfile? = null,
    initialHomeAction: PendingHomeAction? = null,
    onInitialHomeActionConsumed: () -> Unit = {}
) {
    remember {
        StartupTrace.mark("app_runtime_content_start")
        true
    }
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val coroutineScope = rememberCoroutineScope()
    val detectedLowRamProfile = remember(context) {
        resolveDetectedLowRamProfile(context)
    }
    var lowRamProfile by remember(context, initialLowRamProfile) {
        mutableStateOf(initialLowRamProfile ?: resolveLowRamProfile(context))
    }
    LaunchedEffect(lowRamProfile) {
        applyLowRamRuntimeDetectorBudget(lowRamProfile)
    }
    val deviceCompatibilityProfile = remember(lowRamProfile) {
        currentDeviceCompatibilityProfile(lowRamProfile)
    }
    val adminFlowViewModel = remember(activity) {
        ViewModelProvider(activity)[AdminFlowViewModel::class.java]
    }
    val adminFlowUiState by adminFlowViewModel.uiState.collectAsState()
    val initialUiLanguage = remember(initialUiLanguageOverride) {
        initialUiLanguageOverride ?: context.readSavedUiLanguage()
    }
    var persistedUiLanguage by remember { mutableStateOf(initialUiLanguage) }
    var uiLanguage by rememberSaveable { mutableStateOf(initialUiLanguage) }
    var homeAdminSettings by remember(initialHomeAdminSettings) {
        mutableStateOf(initialHomeAdminSettings ?: HomeAdminSettings())
    }
    var adminSettings by remember {
        mutableStateOf(if (lowRamProfile.deferHeavyUi) null else context.readAdminSettings())
    }
    var showDeferredHomeChrome by rememberSaveable { mutableStateOf(!lowRamProfile.severe) }
    var showPerformanceProfileDialog by rememberSaveable { mutableStateOf(false) }
    val adminSettingsSaveRequests = remember { Channel<AdminSettings>(capacity = Channel.CONFLATED) }
    var activeExamPayload by rememberSaveable(stateSaver = ExamQrPayloadSaver) {
        mutableStateOf(null as ExamQrPayload?)
    }
    var pendingScanConfirmPayload by remember { mutableStateOf<ExamQrPayload?>(null) }
    var pendingScanConfirmError by remember { mutableStateOf<String?>(null) }
    var pendingScanConfirmInFlight by remember { mutableStateOf(false) }
    var pendingDirectLinkSaveLog by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRecoveryEventDetails by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRecoveryNoticeTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRecoveryNoticeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var savedRouteSnapshotRaw by rememberSaveable { mutableStateOf(AppRecoveryRoute.Home.name) }
    var examSessionRecoveryNonce by rememberSaveable { mutableLongStateOf(0L) }
    val shellInstanceId = remember { "shell-${SystemClock.elapsedRealtimeNanos()}" }
    var savedShellInstanceId by rememberSaveable { mutableStateOf(shellInstanceId) }
    var savedViewModelInstanceId by rememberSaveable { mutableStateOf(adminFlowViewModel.instanceId) }
    var secretTapCount by rememberSaveable { mutableIntStateOf(0) }
    var lastSecretTapAt by rememberSaveable { mutableLongStateOf(0L) }
    var homeDeferredChromeMarked by remember { mutableStateOf(false) }
    val deviceTimeBaseline = remember { captureDeviceTimeBaseline() }
    val savedRouteSnapshot = parseAppRecoveryRoute(savedRouteSnapshotRaw)
    val latestCurrentScreen by rememberUpdatedState(adminFlowUiState.currentScreen)
    val processDeathRecoveryPending = detectProcessDeathRecovery(
        shellStateRestored = savedShellInstanceId != shellInstanceId,
        currentViewModelInstanceId = adminFlowViewModel.instanceId,
        savedViewModelInstanceId = savedViewModelInstanceId,
        uiState = adminFlowUiState,
        routeSnapshot = savedRouteSnapshot,
        activeExamPayload = activeExamPayload
    )

    val directLinkLabel = homeAdminSettings.fastExamLabel.trim().ifBlank { FastExamName }
    val directLinkUrl = homeAdminSettings.fastExamUrl.trim().ifBlank { SecureStrings.fastExamUrl }
    val latestLowRamProfile by rememberUpdatedState(lowRamProfile)

    fun cacheAdminSettings(loaded: AdminSettings): AdminSettings {
        adminSettings = loaded
        lowRamProfile = applyLowRamProfileOverride(
            detectedProfile = detectedLowRamProfile,
            override = loaded.lowRamProfileOverride
        )
        applyLowRamRuntimeDetectorBudget(lowRamProfile)
        homeAdminSettings = HomeAdminSettings(
            fastExamUrl = loaded.fastExamUrl,
            fastExamLabel = loaded.fastExamLabel
        )
        return loaded
    }

    suspend fun loadCurrentAdminSettings(): AdminSettings {
        adminSettings?.let { cached -> return cached }
        val startedAt = SystemClock.elapsedRealtime()
        StartupTrace.mark("admin_settings_load_start", "thread=io")
        val loaded = withContext(Dispatchers.IO) {
            context.readAdminSettings()
        }
        StartupTrace.mark(
            "admin_settings_loaded",
            "duration_ms=${SystemClock.elapsedRealtime() - startedAt}"
        )
        return cacheAdminSettings(loaded)
    }

    fun activeAdminSettingsSnapshot(): AdminSettings {
        return adminSettings ?: run {
            StartupTrace.mark("admin_settings_sync_fallback", "screen=${adminFlowUiState.currentScreen.name}")
            cacheAdminSettings(context.readAdminSettings())
        }
    }

    fun updateAdminSettings(updated: AdminSettings) {
        val normalized = updated.copy(examUserAgent = updated.effectiveExamUserAgent())
        adminSettings = normalized
        val updatedProfile = applyLowRamProfileOverride(
            detectedProfile = detectedLowRamProfile,
            override = normalized.lowRamProfileOverride
        )
        lowRamProfile = updatedProfile
        applyLowRamRuntimeDetectorBudget(updatedProfile)
        homeAdminSettings = HomeAdminSettings(
            fastExamUrl = normalized.fastExamUrl,
            fastExamLabel = normalized.fastExamLabel
        )
        val sendResult = adminSettingsSaveRequests.trySend(normalized)
        if (BuildConfig.DEBUG && sendResult.isFailure) {
            Log.d(AdminSettingsPerfTag, "Admin settings save request was dropped before enqueue.")
        }
    }

    fun updateLowRamProfileOverride(override: LowRamProfileOverride) {
        val updatedProfile = applyLowRamProfileOverride(
            detectedProfile = detectedLowRamProfile,
            override = override
        )
        lowRamProfile = updatedProfile
        applyLowRamRuntimeDetectorBudget(updatedProfile)
        adminSettings?.let { cachedSettings ->
            updateAdminSettings(cachedSettings.copy(lowRamProfileOverride = override))
            return
        }
        coroutineScope.launch {
            val currentSettings = withContext(Dispatchers.IO) {
                context.readAdminSettings()
            }
            updateAdminSettings(currentSettings.copy(lowRamProfileOverride = override))
        }
    }

    suspend fun persistAdminSettingsImmediately(updated: AdminSettings): AdminSettings {
        val normalized = updated.copy(examUserAgent = updated.effectiveExamUserAgent())
        val refreshed = withContext(Dispatchers.IO) {
            context.saveAdminSettings(normalized)
            context.readAdminSettings()
        }
        return cacheAdminSettings(refreshed)
    }

    fun invalidExamUrlMessage(error: ExamUrlValidationError?): String {
        return when (error) {
            ExamUrlValidationError.Blank -> localized(
                uiLanguage,
                "Exam URL is required.",
                "URL ujian wajib diisi."
            )

            ExamUrlValidationError.Invalid,
            null -> localized(
                uiLanguage,
                "Exam URL must start with http:// or https:// and include a domain.",
                "URL ujian harus diawali http:// atau https:// dan memiliki domain."
            )
        }
    }

    fun directLinkSavedFromQrLog(
        payload: ExamQrPayload,
        normalizedExamUrl: String,
        updatedLabel: String,
        savedLocationPolicy: ExamQrLocationPolicy
    ): String {
        return "url=$normalizedExamUrl | label=$updatedLabel | geofence_shape=${
            savedLocationPolicy.shapeType.name.lowercase(Locale.US)
        } | polygon_points=${savedLocationPolicy.vertices.size} | circle_centers=${
            savedLocationPolicy.effectiveCircleCenters.size
        } | center=${
            savedLocationPolicy.effectiveCircleCenters.firstOrNull()?.let { center ->
                "${center.latitude.ifBlank { "-" }},${center.longitude.ifBlank { "-" }}"
            } ?: "${savedLocationPolicy.centerLat.ifBlank { "-" }},${savedLocationPolicy.centerLng.ifBlank { "-" }}"
        } | radius_m=${
            savedLocationPolicy.radiusMeters.ifBlank { "-" }
        } | exam=${payload.examName.trim().ifBlank { FastExamName }}"
    }

    suspend fun saveDirectLinkFromConfirmedQr(
        payload: ExamQrPayload,
        normalizedExamUrl: String
    ): String {
        val activeSettings = loadCurrentAdminSettings()
        val updatedLabel = payload.examName.trim().ifBlank { FastExamName }
        val savedLocationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy()
        persistAdminSettingsImmediately(
            activeSettings.copy(
                fastExamUrl = normalizedExamUrl,
                fastExamLabel = updatedLabel
            ).withDirectLinkLocationPolicy(savedLocationPolicy)
        )
        return directLinkSavedFromQrLog(
            payload = payload,
            normalizedExamUrl = normalizedExamUrl,
            updatedLabel = updatedLabel,
            savedLocationPolicy = savedLocationPolicy
        )
    }

    fun confirmPendingScanPayload(payload: ExamQrPayload) {
        if (pendingScanConfirmInFlight) {
            return
        }
        coroutineScope.launch {
            val examUrlValidation = validateExamUrl(payload.examUrl)
            val normalizedExamUrl = examUrlValidation.normalizedUrl
            if (normalizedExamUrl == null) {
                pendingScanConfirmError = invalidExamUrlMessage(examUrlValidation.error)
                return@launch
            }

            pendingScanConfirmInFlight = true
            pendingScanConfirmError = null
            try {
                val normalizedPayload = payload.copy(examUrl = normalizedExamUrl)
                if (normalizedPayload.saveToDirectLink) {
                    pendingDirectLinkSaveLog = saveDirectLinkFromConfirmedQr(
                        payload = normalizedPayload,
                        normalizedExamUrl = normalizedExamUrl
                    )
                }
                activeExamPayload = normalizedPayload
                pendingScanConfirmPayload = null
                pendingScanConfirmError = null
                savedRouteSnapshotRaw = AppRecoveryRoute.ExamFlowPreparation.name
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
            } catch (throwable: Throwable) {
                pendingScanConfirmError = throwable.message ?: localized(
                    uiLanguage,
                    "The QR could not be opened.",
                    "QR tidak dapat dibuka."
                )
            } finally {
                pendingScanConfirmInFlight = false
            }
        }
    }

    LaunchedEffect(context) {
        withFrameNanos { }
        homeAdminSettings = withContext(Dispatchers.IO) {
            context.readHomeAdminSettings()
        }
        StartupTrace.mark(
            "home_settings_loaded",
            "direct_link_label=${homeAdminSettings.fastExamLabel.trim().ifBlank { FastExamName }}"
        )
    }

    LaunchedEffect(lowRamProfile.severe, adminFlowUiState.currentScreen, showDeferredHomeChrome) {
        if (!lowRamProfile.severe) {
            showDeferredHomeChrome = true
            if (adminFlowUiState.currentScreen == AppScreen.Home && !homeDeferredChromeMarked) {
                homeDeferredChromeMarked = true
                StartupTrace.mark("home_deferred_chrome_shown", "mode=normal")
            }
            return@LaunchedEffect
        }
        if (adminFlowUiState.currentScreen == AppScreen.Home && !showDeferredHomeChrome) {
            withFrameNanos { }
            delay(750)
            showDeferredHomeChrome = true
            if (!homeDeferredChromeMarked) {
                homeDeferredChromeMarked = true
                StartupTrace.mark("home_deferred_chrome_shown", "mode=severe")
            }
        }
    }

    DisposableEffect(context) {
        val listener: (Int) -> Unit = { level ->
            val baseProfile = latestLowRamProfile
            val memoryInfo = readLowRamRuntimeMemoryInfo(context)
            val escalatedProfile = resolveRuntimePressureProfile(
                baseProfile = baseProfile,
                trimLevel = level,
                availableMemoryBytes = memoryInfo?.availMem,
                memoryLow = memoryInfo?.lowMemory == true
            )
            if (escalatedProfile != baseProfile) {
                lowRamProfile = escalatedProfile
                applyLowRamRuntimeDetectorBudget(escalatedProfile)
                SecurityDetectorCache.invalidateStaticSecurity()
                Log.i(
                    ExamRuntimeHardeningLogTag,
                    "code=LOW_RAM_RUNTIME_ESCALATED level=INFO details=trim=$level " +
                        "| avail=${escalatedProfile.availableMemoryMb ?: "-"}MB " +
                        "| detectorCacheMax=${escalatedProfile.detectorMetadataCacheMaxEntries}"
                )
            }
            val effectiveProfile = if (escalatedProfile != baseProfile) escalatedProfile else baseProfile
            if (
                effectiveProfile.enabled &&
                latestCurrentScreen == AppScreen.Home &&
                MemoryPressureCoordinator.shouldReleaseUiBitmaps(level)
            ) {
                showDeferredHomeChrome = false
                Log.i("HomeMemory", "trim=$level action=hide_deferred_home_chrome")
            }
        }
        MemoryPressureCoordinator.addListener(listener)
        onDispose {
            MemoryPressureCoordinator.removeListener(listener)
        }
    }

    LaunchedEffect(context) {
        for (pendingSettings in adminSettingsSaveRequests) {
            val startedAt = SystemClock.elapsedRealtime()
            val refreshed = withContext(Dispatchers.IO) {
                val saveStartedAt = SystemClock.elapsedRealtime()
                context.saveAdminSettings(pendingSettings)
                val saveFinishedAt = SystemClock.elapsedRealtime()
                val refreshedSettings = context.readAdminSettings()
                Triple(refreshedSettings, saveFinishedAt - saveStartedAt, SystemClock.elapsedRealtime() - saveFinishedAt)
            }
            if (BuildConfig.DEBUG) {
                Log.d(
                    AdminSettingsPerfTag,
                    "Admin settings persisted in ${refreshed.second} ms and reloaded in ${refreshed.third} ms (total ${SystemClock.elapsedRealtime() - startedAt} ms)"
                )
            }
            if (adminSettings == pendingSettings) {
                adminSettings = refreshed.first
            }
        }
    }

    DisposableEffect(adminSettingsSaveRequests) {
        onDispose {
            adminSettingsSaveRequests.close()
        }
    }

    fun registerSecretTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSecretTapAt > SecretTapWindowMs) {
            secretTapCount = 0
        }
        lastSecretTapAt = now
        secretTapCount += 1
        if (secretTapCount >= 4) {
            secretTapCount = 0
            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
            adminFlowViewModel.dispatch(AdminFlowUiAction.ShowAdminPasswordDialog)
        }
    }

    fun queueRecoveryNotice(title: String, message: String) {
        pendingRecoveryNoticeTitle = title
        pendingRecoveryNoticeMessage = message
    }

    LaunchedEffect(uiLanguage) {
        if (uiLanguage != persistedUiLanguage) {
            context.saveUiLanguage(uiLanguage)
            persistedUiLanguage = uiLanguage
        }
    }

    LaunchedEffect(lowRamProfile) {
        Log.i(
            LowRamProfilePerfTag,
            lowRamProfile.diagnosticSummary()
        )
    }

    LaunchedEffect(deviceCompatibilityProfile) {
        Log.i(
            ExamRuntimeHardeningLogTag,
            "code=${ExamRuntimeHardeningDiagnostics.DeviceCompatProfileResolved} " +
                "level=INFO details=${deviceCompatibilityProfile.diagnosticSummary()}"
        )
    }

    LaunchedEffect(
        shellInstanceId,
        adminFlowViewModel.instanceId,
        adminFlowUiState,
        savedRouteSnapshotRaw,
        activeExamPayload,
        uiLanguage
    ) {
        if (processDeathRecoveryPending) {
            when (savedRouteSnapshot) {
                AppRecoveryRoute.ExamFlowPreparation,
                AppRecoveryRoute.ExamFlowRuntime -> {
                    val payload = activeExamPayload
                    val recoveryAdminSettings = payload?.let {
                        loadCurrentAdminSettings()
                    }
                    val recoveryDeviceTimeStatus = if (payload != null && recoveryAdminSettings != null) {
                        inspectDeviceTimeSecurity(
                            context = context,
                            baseline = deviceTimeBaseline,
                            bypassState = currentDeviceTimeBypassState(recoveryAdminSettings)
                        )
                    } else {
                        null
                    }
                    val validationResult = if (payload != null && recoveryDeviceTimeStatus != null) {
                        val recoveryNetworkNowMillis = TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context)
                        ExamScheduleValidator.validateAfterDeviceTimeCheck(
                            payload = payload,
                            deviceTimeStatus = recoveryDeviceTimeStatus,
                            networkNowMillis = recoveryNetworkNowMillis
                        )
                    } else {
                        null
                    }
                    if (payload != null && validationResult == ExamScheduleValidationResult.Valid) {
                        val previousRoute = savedRouteSnapshot.name
                        examSessionRecoveryNonce = SystemClock.elapsedRealtime()
                        pendingRecoveryEventDetails =
                            "route=$previousRoute | payload_restored=yes | validation=valid"
                        queueRecoveryNotice(
                            title = localized(uiLanguage, "Session Restored", "Sesi Dipulihkan"),
                            message = localized(
                                uiLanguage,
                                "The app was restarted by Android. Review the checks and start the exam again.",
                                "Aplikasi dimulai ulang oleh Android. Periksa kembali pengecekan lalu mulai ujian lagi."
                            )
                        )
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
                    } else {
                        activeExamPayload = null
                        queueRecoveryNotice(
                            title = localized(uiLanguage, "Session Unavailable", "Sesi Tidak Tersedia"),
                            message = when (validationResult) {
                                ExamScheduleValidationResult.NotStarted -> localized(
                                    uiLanguage,
                                    "The saved exam session is not active yet. Please scan or open the exam again later.",
                                    "Sesi ujian yang tersimpan belum aktif. Silakan scan atau buka lagi nanti."
                                )

                                ExamScheduleValidationResult.Finished -> localized(
                                    uiLanguage,
                                    "The saved exam session has expired. Please scan or open a valid exam again.",
                                    "Sesi ujian yang tersimpan sudah berakhir. Silakan scan atau buka lagi ujian yang masih valid."
                                )

                                ExamScheduleValidationResult.InvalidSchedule -> localized(
                                    uiLanguage,
                                    "The saved exam session is no longer valid because its schedule is invalid.",
                                    "Sesi ujian yang tersimpan tidak lagi valid karena jadwalnya tidak valid."
                                )

                                ExamScheduleValidationResult.TimeSpoofDetected -> localized(
                                    uiLanguage,
                                    "The saved exam session could not be restored because the device time could not be trusted. Check automatic date, time, and time zone, then scan again.",
                                    "Sesi ujian yang tersimpan tidak dapat dipulihkan karena waktu perangkat tidak dapat dipercaya. Periksa tanggal, waktu, dan zona waktu otomatis, lalu scan ulang."
                                )

                                else -> localized(
                                    uiLanguage,
                                    "The saved exam session could not be restored. Please scan or open the exam again.",
                                    "Sesi ujian yang tersimpan tidak dapat dipulihkan. Silakan scan atau buka lagi ujian."
                                )
                            }
                        )
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                    }
                }

                AppRecoveryRoute.CustomQrAdmin,
                AppRecoveryRoute.SecretAdmin -> {
                    AdminAuthSession.clear()
                    activeExamPayload = null
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                }

                AppRecoveryRoute.Home -> {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                }
            }
        }
        savedShellInstanceId = shellInstanceId
        savedViewModelInstanceId = adminFlowViewModel.instanceId
    }

    fun handleExamQrRawPayload(rawPayload: String) {
        coroutineScope.launch {
            val payload = runCatching {
                withContext(Dispatchers.Default) {
                    ExamQrCodec.decrypt(rawPayload)
                }
            }.getOrElse {
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        it.message ?: localized(
                            uiLanguage,
                            "The QR code could not be read.",
                            "QR tidak dapat dibaca."
                        )
                    )
                )
                return@launch
            }

            try {
                val activeSettings = loadCurrentAdminSettings()
                val deviceTimeStatus = inspectDeviceTimeSecurity(
                    context = context,
                    baseline = deviceTimeBaseline,
                    bypassState = currentDeviceTimeBypassState(activeSettings)
                )
                if (deviceTimeStatus.blocking) {
                    Log.w(
                        "AppHostDeviceTime",
                        "QR_BLOCKED_DEVICE_TIME ${deviceTimeEventDetails(deviceTimeStatus, "qr_scan")}"
                    )
                    adminFlowViewModel.dispatch(
                        AdminFlowUiAction.SetScanErrorMessage(
                            deviceTimeQrBlockMessage(deviceTimeStatus, uiLanguage)
                        )
                    )
                    return@launch
                }
                val networkNowMillis = TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context)
                when (
                    ExamScheduleValidator.validateAfterDeviceTimeCheck(
                        payload = payload,
                        deviceTimeStatus = deviceTimeStatus,
                        networkNowMillis = networkNowMillis
                    )
                ) {
                    ExamScheduleValidationResult.Valid -> {
                        pendingScanConfirmError = null
                        pendingScanConfirmPayload = payload
                    }

                    ExamScheduleValidationResult.NotStarted -> {
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                localized(
                                    uiLanguage,
                                    "This QR is not active yet. The exam can only be opened starting ${payload.startDateTime}.",
                                    "QR ini belum aktif. Ujian baru bisa dibuka mulai ${payload.startDateTime}."
                                )
                            )
                        )
                    }

                    ExamScheduleValidationResult.Finished -> {
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                localized(
                                    uiLanguage,
                                    "This QR is no longer valid. The exam ended at ${payload.endDateTime}.",
                                    "QR ini sudah tidak berlaku. Waktu ujian berakhir pada ${payload.endDateTime}."
                                )
                            )
                        )
                    }

                    ExamScheduleValidationResult.InvalidSchedule -> {
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                localized(
                                    uiLanguage,
                                    "This QR has an invalid exam schedule. Please check the start and end time.",
                                    "QR ini memiliki jadwal ujian yang tidak valid. Periksa waktu mulai dan selesai."
                                )
                            )
                        )
                    }

                    ExamScheduleValidationResult.TimeSpoofDetected -> {
                        Log.w(
                            "AppHostDeviceTime",
                            "QR_BLOCKED_DEVICE_TIME schedule_result=time_spoof_detected | network_now_ms=${networkNowMillis ?: "unavailable"} | " +
                                deviceTimeEventDetails(deviceTimeStatus, "qr_scan_network_time")
                        )
                        adminFlowViewModel.dispatch(
                            AdminFlowUiAction.SetScanErrorMessage(
                                deviceTimeQrBlockMessage(deviceTimeStatus, uiLanguage)
                            )
                        )
                    }
                }
            } catch (throwable: Throwable) {
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        throwable.message ?: localized(
                            uiLanguage,
                            "The QR code could not be read.",
                            "QR tidak dapat dibaca."
                        )
                    )
                )
            }
        }
    }

    fun launchDirectLink() {
        coroutineScope.launch {
            try {
                val activeSettings = loadCurrentAdminSettings()
                val activeDirectLinkLabel = activeSettings.fastExamLabel.trim().ifBlank { FastExamName }
                val configuredDirectLinkUrl = activeSettings.fastExamUrl.trim().ifBlank {
                    SecureStrings.fastExamUrl
                }
                val directLinkUrlValidation = validateExamUrl(configuredDirectLinkUrl)
                val normalizedDirectLinkUrl = directLinkUrlValidation.normalizedUrl
                if (normalizedDirectLinkUrl == null) {
                    error(
                        localized(
                            uiLanguage,
                            "Direct Link URL must start with http:// or https:// and include a domain. Update it from Secret Admin.",
                            "URL Direct Link harus diawali http:// atau https:// dan memiliki domain. Perbarui dari Secret Admin."
                        )
                    )
                }
                val nowMillis = System.currentTimeMillis()
                val scheduleWindow = ExamScheduleDefaults.defaultDirectLinkWindow(nowMillis = nowMillis)
                val directLinkLocationPolicy = runCatching {
                    activeSettings.directLinkLocationPolicy()
                }.getOrNull()
                val directLinkLocationPolicySource = when {
                    directLinkLocationPolicy != null && activeSettings.directLinkLocationPolicySaved ->
                        LocationPolicySource.DirectLinkSaved
                    else -> LocationPolicySource.DisabledNoPolicy
                }
                val directLinkPayload = ExamQrPayload(
                    examUrl = normalizedDirectLinkUrl,
                    examName = activeDirectLinkLabel,
                    startDateTime = scheduleWindow.startDateTime,
                    endDateTime = scheduleWindow.endDateTime,
                    issuedAt = nowMillis,
                    locationPolicy = directLinkLocationPolicy,
                    locationPolicySource = directLinkLocationPolicySource
                )
                activeExamPayload = directLinkPayload
                savedRouteSnapshotRaw = AppRecoveryRoute.ExamFlowPreparation.name
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
            } catch (throwable: Throwable) {
                activeExamPayload = null
                savedRouteSnapshotRaw = AppRecoveryRoute.Home.name
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        throwable.message ?: localized(
                            uiLanguage,
                            "Direct Link could not be opened.",
                            "Direct Link tidak dapat dibuka."
                        )
                    )
                )
            }
        }
    }

    LaunchedEffect(initialHomeAction) {
        when (initialHomeAction) {
            PendingHomeAction.RuntimeHome -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.RuntimeHome.name}")
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.ScanExam -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.ScanExam.name}")
                adminFlowViewModel.dispatch(AdminFlowUiAction.ShowScanSourceDialog)
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.CustomQrAdmin -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.CustomQrAdmin.name}")
                loadCurrentAdminSettings()
                adminFlowViewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.DirectLink -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.DirectLink.name}")
                launchDirectLink()
                onInitialHomeActionConsumed()
            }

            PendingHomeAction.SecretAdmin -> {
                StartupTrace.mark("pending_home_action_consumed", "action=${PendingHomeAction.SecretAdmin.name}")
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                adminFlowViewModel.dispatch(AdminFlowUiAction.ShowAdminPasswordDialog)
                onInitialHomeActionConsumed()
            }

            null -> Unit
        }
    }

    LaunchedEffect(adminFlowUiState.currentScreen, processDeathRecoveryPending) {
        if (processDeathRecoveryPending && adminFlowUiState.currentScreen == AppScreen.Home) {
            return@LaunchedEffect
        }
        savedRouteSnapshotRaw = when (adminFlowUiState.currentScreen) {
            AppScreen.Home -> AppRecoveryRoute.Home.name
            AppScreen.CustomQrAdmin -> AppRecoveryRoute.CustomQrAdmin.name
            AppScreen.SecretAdmin -> AppRecoveryRoute.SecretAdmin.name
            AppScreen.ExamWebView -> {
                if (parseAppRecoveryRoute(savedRouteSnapshotRaw) == AppRecoveryRoute.ExamFlowRuntime) {
                    AppRecoveryRoute.ExamFlowRuntime.name
                } else {
                    AppRecoveryRoute.ExamFlowPreparation.name
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalUiLanguage provides uiLanguage,
        LocalLowRamProfile provides lowRamProfile,
        LocalDeviceCompatibilityProfile provides deviceCompatibilityProfile
    ) {
        BackHandler(enabled = adminFlowUiState.currentScreen == AppScreen.CustomQrAdmin) {
            adminFlowViewModel.dispatch(AdminFlowUiAction.CloseCustomQrAdmin)
        }
        BackHandler(enabled = adminFlowUiState.currentScreen == AppScreen.SecretAdmin) {
            AdminAuthSession.clear()
            adminFlowViewModel.dispatch(AdminFlowUiAction.CloseSecretAdmin)
        }

        if (adminFlowUiState.currentScreen == AppScreen.Home) {
            remember {
                StartupTrace.mark("home_compose_start")
                true
            }
            if (lowRamProfile.severe) {
                ExamLockLowRamHomeScreen(
                    uiLanguage = uiLanguage,
                    onUiLanguageChange = { uiLanguage = it },
                    onScanExam = { adminFlowViewModel.dispatch(AdminFlowUiAction.ShowScanSourceDialog) },
                    onOpenAdmin = {
                        coroutineScope.launch {
                            loadCurrentAdminSettings()
                            adminFlowViewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)
                        }
                    },
                    onOpenFastExam = ::launchDirectLink,
                    directLinkLabel = directLinkLabel,
                    onSecretTap = ::registerSecretTap,
                    onOpenPerformanceProfile = { showPerformanceProfileDialog = true },
                    showDeferredChrome = showDeferredHomeChrome
                )
            } else {
                ExamLockHomeScreen(
                    uiLanguage = uiLanguage,
                    onUiLanguageChange = { uiLanguage = it },
                    onScanExam = { adminFlowViewModel.dispatch(AdminFlowUiAction.ShowScanSourceDialog) },
                    onOpenAdmin = {
                        coroutineScope.launch {
                            loadCurrentAdminSettings()
                            adminFlowViewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin)
                        }
                    },
                    onOpenFastExam = ::launchDirectLink,
                    directLinkLabel = directLinkLabel,
                    onSecretTap = ::registerSecretTap,
                    onOpenPerformanceProfile = { showPerformanceProfileDialog = true },
                    showDeferredChrome = showDeferredHomeChrome
                )
            }
        } else {
            AppNonHomeRouteHost(
                screen = adminFlowUiState.currentScreen,
                uiState = adminFlowUiState,
                activeExamPayload = activeExamPayload,
                adminSettingsSnapshot = ::activeAdminSettingsSnapshot,
                updateAdminSettings = ::updateAdminSettings,
                dispatch = adminFlowViewModel::dispatch,
                pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
                pendingRecoveryEventDetails = pendingRecoveryEventDetails,
                onDirectLinkSaveLogConsumed = { pendingDirectLinkSaveLog = null },
                onRecoveryEventConsumed = { pendingRecoveryEventDetails = null },
                examSessionRecoveryNonce = examSessionRecoveryNonce,
                deviceTimeBaselineWallClockMillis = deviceTimeBaseline.wallClockMillis,
                deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaseline.elapsedRealtimeMillis,
                onExamSessionStartedStateChange = { started ->
                    savedRouteSnapshotRaw = if (started) {
                        AppRecoveryRoute.ExamFlowRuntime.name
                    } else {
                        AppRecoveryRoute.ExamFlowPreparation.name
                    }
                },
                onExamExit = {
                    activeExamPayload = null
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                },
                onMissingExamPayload = {
                        savedRouteSnapshotRaw = AppRecoveryRoute.Home.name
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                }
            )
        }

        if (showPerformanceProfileDialog) {
            PublicPerformanceProfileDialog(
                selectedOverride = lowRamProfile.lowRamOverride,
                detectedProfile = detectedLowRamProfile,
                effectiveProfile = lowRamProfile,
                onOverrideChange = ::updateLowRamProfileOverride,
                onDismiss = { showPerformanceProfileDialog = false }
            )
        }

        if (adminFlowUiState.showAdminPasswordDialog) {
            AdminPasswordDialog(
                password = adminFlowUiState.adminPasswordInput,
                errorMessage = adminFlowUiState.adminPasswordError,
                onPasswordChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(it))
                    if (adminFlowUiState.adminPasswordError != null) {
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                    }
                },
                onConfirm = {
                    val passwordInput = adminFlowUiState.adminPasswordInput
                    coroutineScope.launch {
                        val verified = withContext(Dispatchers.Default) {
                            AdminAuth.verify(context, passwordInput)
                        }
                        if (verified) {
                            loadCurrentAdminSettings()
                            adminFlowViewModel.dispatch(AdminFlowUiAction.HideAdminPasswordDialog)
                            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
                            adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                            adminFlowViewModel.dispatch(AdminFlowUiAction.OpenSecretAdmin)
                        } else {
                            adminFlowViewModel.dispatch(
                                AdminFlowUiAction.SetAdminPasswordError(
                                    localized(uiLanguage, "Incorrect password.", "Password salah.")
                                )
                            )
                        }
                    }
                },
                onDismiss = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.HideAdminPasswordDialog)
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordInput(""))
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetAdminPasswordError(null))
                }
            )
        }

        adminFlowUiState.scanErrorMessage?.let { message ->
            InfoDialog(
                title = tr("QR Scan", "Scan QR"),
                message = message,
                onDismiss = { adminFlowViewModel.dispatch(AdminFlowUiAction.SetScanErrorMessage(null)) }
            )
        }

        pendingScanConfirmPayload?.let { payload ->
            val geofenceInfo = when (payload.locationPolicy?.shapeType) {
                GeofenceShapeType.Circle -> "Circle | ${payload.locationPolicy.effectiveCircleCenters.size} centers | ${payload.locationPolicy.radiusMeters} m"
                GeofenceShapeType.Polygon -> "Polygon | ${payload.locationPolicy.vertices.size} points"
                else -> "Disabled"
            }
            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LockBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "QR",
                                color = LockBlueDeep,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = tr("Review Exam QR", "Review QR Ujian"),
                                color = LockTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = tr(
                                    "Check details before opening preparation.",
                                    "Cek detail sebelum membuka preparation."
                                ),
                                color = LockTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = LockSurfaceSoft,
                            border = BorderStroke(1.dp, LockOutline.copy(alpha = 0.65f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = tr("Exam", "Ujian"),
                                        color = LockTextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = payload.examName.trim().ifBlank { "-" },
                                        color = LockTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 18.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tr("Start", "Mulai"),
                                            color = LockTextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = payload.startDateTime,
                                            color = LockTextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tr("End", "Selesai"),
                                            color = LockTextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = payload.endDateTime,
                                            color = LockTextPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = tr("Geofence", "Geofence"),
                                        color = LockTextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = geofenceInfo,
                                        color = LockTextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (payload.saveToDirectLink) {
                                Color(0xFFEAF7EF)
                            } else {
                                LockBlue.copy(alpha = 0.07f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (payload.saveToDirectLink) {
                                    Color(0xFF1F7A4D).copy(alpha = 0.22f)
                                } else {
                                    LockBlue.copy(alpha = 0.16f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (payload.saveToDirectLink) {
                                                Color(0xFF1F7A4D)
                                            } else {
                                                LockBlue
                                            }
                                        )
                                )
                                Text(
                                    text = if (payload.saveToDirectLink) {
                                        tr(
                                            "Direct Link will be saved after you tap Yes.",
                                            "Direct Link akan disimpan setelah tombol Ya ditekan."
                                        )
                                    } else {
                                        tr(
                                            "Direct Link will not be changed.",
                                            "Direct Link tidak akan diubah."
                                        )
                                    },
                                    color = if (payload.saveToDirectLink) {
                                        Color(0xFF155C3B)
                                    } else {
                                        LockBlueDeep
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        pendingScanConfirmError?.let { message ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFEF3F2),
                                border = BorderStroke(1.dp, Color(0xFFB42318).copy(alpha = 0.30f))
                            ) {
                                Text(
                                    text = message,
                                    color = Color(0xFFB42318),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                        confirmPendingScanPayload(payload)
                        },
                        enabled = !pendingScanConfirmInFlight,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockBlue,
                            contentColor = LockOnDark,
                            disabledContainerColor = LockBlue.copy(alpha = 0.45f),
                            disabledContentColor = LockOnDark.copy(alpha = 0.75f)
                        )
                    ) {
                        Text(
                            text = when {
                                pendingScanConfirmInFlight -> tr("Processing...", "Memproses...")
                                payload.saveToDirectLink -> tr(
                                    "Yes, save & continue",
                                    "Ya, simpan & lanjut"
                                )
                                else -> tr(
                                    "Yes, continue to Preparation",
                                    "Ya, lanjut ke Preparation"
                                )
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingScanConfirmPayload = null
                        pendingScanConfirmError = null
                    }, enabled = !pendingScanConfirmInFlight) {
                        Text(
                            text = tr("Cancel", "Batal"),
                            color = LockTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        if (pendingRecoveryNoticeTitle != null && pendingRecoveryNoticeMessage != null) {
            InfoDialog(
                title = pendingRecoveryNoticeTitle.orEmpty(),
                message = pendingRecoveryNoticeMessage.orEmpty(),
                onDismiss = {
                    pendingRecoveryNoticeTitle = null
                    pendingRecoveryNoticeMessage = null
                }
            )
        }

        if (adminFlowUiState.showScanSourceDialog) {
            ExamScanSourceDialogHost(
                uiLanguage = uiLanguage,
                onRawPayload = ::handleExamQrRawPayload,
                onScanError = { message ->
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetScanErrorMessage(message))
                },
                onDismiss = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.HideScanSourceDialog)
                }
            )
        }
    }
}
