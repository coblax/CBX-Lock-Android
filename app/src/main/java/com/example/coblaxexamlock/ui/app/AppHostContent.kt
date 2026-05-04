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
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.QrPortraitCaptureActivity
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.TrustedNetworkTimeCoordinator
import com.example.coblaxexamlock.captureDeviceTimeBaseline
import com.example.coblaxexamlock.inspectDeviceTimeSecurity
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
import com.example.coblaxexamlock.persistence.readAdminSettings
import com.example.coblaxexamlock.persistence.readSavedUiLanguage
import com.example.coblaxexamlock.persistence.saveAdminSettings
import com.example.coblaxexamlock.persistence.saveUiLanguage
import com.example.coblaxexamlock.runtime.decodeQrPayloadFromImageUri
import com.example.coblaxexamlock.save.ExamQrPayloadSaver
import com.example.coblaxexamlock.ui.admin.AdminPasswordDialog
import com.example.coblaxexamlock.ui.admin.CustomQrAdminScreen
import com.example.coblaxexamlock.ui.admin.ExamLockHomeScreen
import com.example.coblaxexamlock.ui.admin.InfoDialog
import com.example.coblaxexamlock.ui.admin.ScanSourceDialog
import com.example.coblaxexamlock.ui.admin.SecretAdminScreen
import com.example.coblaxexamlock.ui.exam.ExamWebViewScreen
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

private enum class AppRecoveryRoute {
    Home,
    ExamFlowPreparation,
    ExamFlowRuntime,
    CustomQrAdmin,
    SecretAdmin
}

private fun parseAppRecoveryRoute(rawValue: String?): AppRecoveryRoute =
    rawValue
        ?.let { value -> runCatching { AppRecoveryRoute.valueOf(value) }.getOrNull() }
        ?: AppRecoveryRoute.Home

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
internal fun AppContent() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val coroutineScope = rememberCoroutineScope()
    val adminFlowViewModel = remember(activity) {
        ViewModelProvider(activity)[AdminFlowViewModel::class.java]
    }
    val adminFlowUiState by adminFlowViewModel.uiState.collectAsState()
    var uiLanguage by rememberSaveable { mutableStateOf(context.readSavedUiLanguage()) }
    var adminSettings by remember { mutableStateOf(context.readAdminSettings()) }
    val adminSettingsSaveRequests = remember { Channel<AdminSettings>(capacity = Channel.CONFLATED) }
    var activeExamPayload by rememberSaveable(stateSaver = ExamQrPayloadSaver) {
        mutableStateOf(null as ExamQrPayload?)
    }
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
    val deviceTimeBaseline = remember { captureDeviceTimeBaseline() }
    val savedRouteSnapshot = parseAppRecoveryRoute(savedRouteSnapshotRaw)
    val processDeathRecoveryPending = detectProcessDeathRecovery(
        shellStateRestored = savedShellInstanceId != shellInstanceId,
        currentViewModelInstanceId = adminFlowViewModel.instanceId,
        savedViewModelInstanceId = savedViewModelInstanceId,
        uiState = adminFlowUiState,
        routeSnapshot = savedRouteSnapshot,
        activeExamPayload = activeExamPayload
    )

    val directLinkLabel = adminSettings.fastExamLabel.trim().ifBlank { FastExamName }
    val directLinkUrl = adminSettings.fastExamUrl.trim().ifBlank { SecureStrings.fastExamUrl }

    fun updateAdminSettings(updated: AdminSettings) {
        val normalized = updated.copy(examUserAgent = updated.effectiveExamUserAgent())
        adminSettings = normalized
        val sendResult = adminSettingsSaveRequests.trySend(normalized)
        if (BuildConfig.DEBUG && sendResult.isFailure) {
            Log.d(AdminSettingsPerfTag, "Admin settings save request was dropped before enqueue.")
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
        context.saveUiLanguage(uiLanguage)
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
                    val recoveryDeviceTimeStatus = payload?.let {
                        inspectDeviceTimeSecurity(
                            context = context,
                            baseline = deviceTimeBaseline,
                            bypassState = currentDeviceTimeBypassState(adminSettings)
                        )
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
            runCatching { ExamQrCodec.decrypt(rawPayload) }
            .onSuccess { payload ->
                val deviceTimeStatus = inspectDeviceTimeSecurity(
                    context = context,
                    baseline = deviceTimeBaseline,
                    bypassState = currentDeviceTimeBypassState(adminSettings)
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
                    return@onSuccess
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
                        if (payload.saveToDirectLink) {
                            val updatedLabel = payload.examName.trim().ifBlank { FastExamName }
                            val savedLocationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy()
                            updateAdminSettings(
                                adminSettings.copy(
                                    fastExamUrl = payload.examUrl.trim(),
                                    fastExamLabel = updatedLabel
                                ).withDirectLinkLocationPolicy(savedLocationPolicy)
                            )
                            pendingDirectLinkSaveLog =
                                "url=${payload.examUrl.trim()} | label=$updatedLabel | geofence_shape=${
                                    savedLocationPolicy.shapeType.name.lowercase(Locale.US)
                                } | polygon_points=${savedLocationPolicy.vertices.size} | circle_centers=${
                                    savedLocationPolicy.effectiveCircleCenters.size
                                } | center=${
                                    savedLocationPolicy.effectiveCircleCenters.firstOrNull()?.let { center ->
                                        "${center.latitude.ifBlank { "-" }},${center.longitude.ifBlank { "-" }}"
                                    } ?: "${savedLocationPolicy.centerLat.ifBlank { "-" }},${savedLocationPolicy.centerLng.ifBlank { "-" }}"
                                } | radius_m=${
                                    savedLocationPolicy.radiusMeters.ifBlank { "-" }
                                }"
                        }
                        activeExamPayload = payload
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
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
            }
            .onFailure {
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        it.message ?: localized(
                            uiLanguage,
                            "The QR code could not be read.",
                            "QR tidak dapat dibaca."
                        )
                    )
                )
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(contract = ScanContract()) { result: ScanIntentResult ->
        val rawPayload = result.contents ?: return@rememberLauncherForActivityResult
        handleExamQrRawPayload(rawPayload)
    }

    val launchCameraScan = {
        scanLauncher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt(localized(uiLanguage, "Scan the encrypted exam QR", "Arahkan kamera ke QR ujian terenkripsi"))
                setBeepEnabled(false)
                setCaptureActivity(QrPortraitCaptureActivity::class.java)
                setOrientationLocked(true)
            }
        )
    }

    val fileScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val imageOpenFailedMessage = localized(
            uiLanguage,
            "The selected image could not be opened.",
            "Gambar yang dipilih tidak dapat dibuka."
        )
        val imageDecodeFailedMessage = localized(
            uiLanguage,
            "The selected image could not be processed.",
            "Gambar yang dipilih tidak dapat diproses."
        )
        val imageNoQrMessage = localized(
            uiLanguage,
            "No valid QR code was found in the selected image.",
            "QR yang valid tidak ditemukan di gambar yang dipilih."
        )
        coroutineScope.launch {
            val rawPayload = runCatching {
                decodeQrPayloadFromImageUri(context, uri)
            }.getOrElse { throwable ->
                adminFlowViewModel.dispatch(
                    AdminFlowUiAction.SetScanErrorMessage(
                        when (throwable.message) {
                            QrImageReadErrorOpen -> imageOpenFailedMessage
                            QrImageReadErrorDecode -> imageDecodeFailedMessage
                            else -> imageDecodeFailedMessage
                        }
                    )
                )
                return@launch
            }

            if (rawPayload.isNullOrBlank()) {
                adminFlowViewModel.dispatch(AdminFlowUiAction.SetScanErrorMessage(imageNoQrMessage))
                return@launch
            }

            handleExamQrRawPayload(rawPayload)
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

    CompositionLocalProvider(LocalUiLanguage provides uiLanguage) {
        BackHandler(enabled = adminFlowUiState.currentScreen == AppScreen.CustomQrAdmin) {
            adminFlowViewModel.dispatch(AdminFlowUiAction.CloseCustomQrAdmin)
        }
        BackHandler(enabled = adminFlowUiState.currentScreen == AppScreen.SecretAdmin) {
            AdminAuthSession.clear()
            adminFlowViewModel.dispatch(AdminFlowUiAction.CloseSecretAdmin)
        }

        when (adminFlowUiState.currentScreen) {
            AppScreen.Home -> ExamLockHomeScreen(
                uiLanguage = uiLanguage,
                onUiLanguageChange = { uiLanguage = it },
                onScanExam = { adminFlowViewModel.dispatch(AdminFlowUiAction.ShowScanSourceDialog) },
                onOpenAdmin = { adminFlowViewModel.dispatch(AdminFlowUiAction.OpenCustomQrAdmin) },
                onOpenFastExam = {
                    runCatching {
                        val normalizedDirectLinkUrl = directLinkUrl.trim()
                        val parsedDirectLinkUri = normalizedDirectLinkUrl.toUri()
                        val directLinkScheme = parsedDirectLinkUri.scheme.orEmpty().lowercase(Locale.US)
                        val directLinkHost = parsedDirectLinkUri.host.orEmpty()
                        if (directLinkScheme !in setOf("http", "https") || directLinkHost.isBlank()) {
                            error(
                                localized(
                                    uiLanguage,
                                    "Direct Link URL is invalid. Update it from Secret Admin.",
                                    "URL Direct Link tidak valid. Perbarui dari Secret Admin."
                                )
                            )
                        }
                        val nowMillis = System.currentTimeMillis()
                        val scheduleWindow = ExamScheduleDefaults.defaultDirectLinkWindow(nowMillis = nowMillis)
                        val directLinkLocationPolicy = runCatching {
                            adminSettings.directLinkLocationPolicy()
                        }.getOrNull()
                        val directLinkLocationPolicySource = when {
                            directLinkLocationPolicy != null && adminSettings.directLinkLocationPolicySaved ->
                                LocationPolicySource.DirectLinkSaved
                            else -> LocationPolicySource.DisabledNoPolicy
                        }
                        val directLinkPayload = ExamQrPayload(
                            examUrl = normalizedDirectLinkUrl,
                            examName = directLinkLabel,
                            startDateTime = scheduleWindow.startDateTime,
                            endDateTime = scheduleWindow.endDateTime,
                            issuedAt = nowMillis,
                            locationPolicy = directLinkLocationPolicy,
                            locationPolicySource = directLinkLocationPolicySource
                        )
                        activeExamPayload = directLinkPayload
                        savedRouteSnapshotRaw = AppRecoveryRoute.ExamFlowPreparation.name
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.ExamWebView))
                    }.onFailure { throwable ->
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
                },
                directLinkLabel = directLinkLabel,
                onSecretTap = ::registerSecretTap
            )

            AppScreen.CustomQrAdmin -> CustomQrAdminScreen(
                showSaveToDirectLinkOption = adminSettings.customQrSaveToDirectLinkEnabled,
                onBack = { adminFlowViewModel.dispatch(AdminFlowUiAction.CloseCustomQrAdmin) },
                selectedTabName = adminFlowUiState.selectedCustomQrTab,
                onSelectedTabNameChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SelectCustomQrTab(it))
                },
                draft = adminFlowUiState.customQrDraft,
                onDraftChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetCustomQrDraft(it))
                },
                showCircleMapEditor = adminFlowUiState.showCircleMapEditor,
                onShowCircleMapEditorChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetShowCircleMapEditor(it))
                },
                showPolygonMapEditor = adminFlowUiState.showPolygonMapEditor,
                onShowPolygonMapEditorChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetShowPolygonMapEditor(it))
                },
                generatedQrPayload = adminFlowUiState.generatedQrPayload,
                onGeneratedQrPayloadChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetGeneratedQrPayload(it))
                },
                generationStatus = adminFlowUiState.generationStatus,
                onGenerationStatusChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetGenerationStatus(it))
                },
                generationIsError = adminFlowUiState.generationIsError,
                onGenerationIsErrorChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SetGenerationIsError(it))
                }
            )

            AppScreen.SecretAdmin -> SecretAdminScreen(
                settings = adminSettings,
                examName = activeExamPayload?.examName?.trim().orEmpty().ifBlank {
                    adminSettings.fastExamLabel
                },
                onSettingsChange = { updateAdminSettings(it) },
                onResetDirectLink = {
                    updateAdminSettings(
                        adminSettings.copy(
                            fastExamUrl = SecureStrings.fastExamUrl,
                            fastExamLabel = FastExamName
                        ).withoutDirectLinkLocationPolicy()
                    )
                },
                onBack = {
                    AdminAuthSession.clear()
                    adminFlowViewModel.dispatch(AdminFlowUiAction.CloseSecretAdmin)
                },
                selectedTabName = adminFlowUiState.selectedSecretTab,
                onSelectedTabNameChange = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.SelectSecretTab(it))
                },
                deviceTimeBaselineWallClockMillis = deviceTimeBaseline.wallClockMillis,
                deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaseline.elapsedRealtimeMillis
            )

            AppScreen.ExamWebView -> {
                val payload = activeExamPayload
                if (payload != null) {
                    ExamWebViewScreen(
                        payload = payload,
                        adminSettings = adminSettings,
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
                        onExit = {
                            activeExamPayload = null
                            adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        savedRouteSnapshotRaw = AppRecoveryRoute.Home.name
                        adminFlowViewModel.dispatch(AdminFlowUiAction.SetCurrentScreen(AppScreen.Home))
                    }
                }
            }
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
                    if (AdminAuth.verify(context, adminFlowUiState.adminPasswordInput)) {
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
            ScanSourceDialog(
                onCameraClick = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.HideScanSourceDialog)
                    launchCameraScan()
                },
                onFileClick = {
                    adminFlowViewModel.dispatch(AdminFlowUiAction.HideScanSourceDialog)
                    fileScanLauncher.launch("image/*")
                },
                onDismiss = { adminFlowViewModel.dispatch(AdminFlowUiAction.HideScanSourceDialog) }
            )
        }
    }
}
