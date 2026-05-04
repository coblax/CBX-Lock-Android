package com.example.coblaxexamlock.ui.exam

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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.RenderProcessGoneDetail
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
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
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
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.coblaxexamlock.AccessibilityBypassResolver
import com.example.coblaxexamlock.AccessibilityBypassState
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AdbBypassResolver
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmSessionIdentity
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.AppSwitchBypassResolver
import com.example.coblaxexamlock.AppSwitchMonitor
import com.example.coblaxexamlock.AppSwitchProtectionMode
import com.example.coblaxexamlock.AppSwitchSignal
import com.example.coblaxexamlock.AppSwitchSuppressionReason
import com.example.coblaxexamlock.AccessibilityExamGuardStore
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AccessibilityInspectionResult
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardBypassResolver
import com.example.coblaxexamlock.ClipboardBypassState
import com.example.coblaxexamlock.ClipboardChangeDecision
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.ClipboardSnapshot
import com.example.coblaxexamlock.DeviceTimeBaseline
import com.example.coblaxexamlock.DeviceTimeBypassResolver
import com.example.coblaxexamlock.DeviceTimeBypassState
import com.example.coblaxexamlock.DeviceTimeSecurityStatus
import com.example.coblaxexamlock.DeviceTimeSecurityVerdict
import com.example.coblaxexamlock.ExamAlarmSeverity
import com.example.coblaxexamlock.ExamParticipantCaptureBridge
import com.example.coblaxexamlock.ExamParticipantCaptureProbeScript
import com.example.coblaxexamlock.ExamParticipantCaptureResult
import com.example.coblaxexamlock.ExamParticipantContext
import com.example.coblaxexamlock.ExamPolicyEngine
import com.example.coblaxexamlock.ExamQrLocationPolicy
import com.example.coblaxexamlock.ExamQrPayload
import com.example.coblaxexamlock.ExamScheduleValidationResult
import com.example.coblaxexamlock.ExamScheduleValidator
import com.example.coblaxexamlock.FakeLocationBypassResolver
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.FatalSecuritySignal
import com.example.coblaxexamlock.GeofenceBypassResolver
import com.example.coblaxexamlock.GeofenceBypassState
import com.example.coblaxexamlock.GeofenceConfigParseResult
import com.example.coblaxexamlock.GeofenceEvaluation
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.IntegrityCheckResult
import com.example.coblaxexamlock.IntegrityGuard
import com.example.coblaxexamlock.LocationPolicySource
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.MainActivity
import com.example.coblaxexamlock.MemoryPressureCoordinator
import com.example.coblaxexamlock.OverlayBypassResolver
import com.example.coblaxexamlock.OverlayBypassState
import com.example.coblaxexamlock.OverlayRiskAnalyzer
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ReverseEngineeringResult
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.RootBypassResolver
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.ScreenPinningBypassResolver
import com.example.coblaxexamlock.ScreenPinningEnforcer
import com.example.coblaxexamlock.ScreenPinningMode
import com.example.coblaxexamlock.ScreenPinningMonitor
import com.example.coblaxexamlock.ScreenPinningPlatformBridge
import com.example.coblaxexamlock.ScreenPinningSignals
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.SignatureIntegrity
import com.example.coblaxexamlock.SignatureIntegrityResult
import com.example.coblaxexamlock.SplitLocationSecurityStatus
import com.example.coblaxexamlock.applyExamWebViewSettings
import com.example.coblaxexamlock.attachExamKeyboardBridge
import com.example.coblaxexamlock.attachExamNativeFullscreenBridge
import com.example.coblaxexamlock.attachExamParticipantCaptureBridge
import com.example.coblaxexamlock.alarmSeverityForAppSwitchViolationCount
import com.example.coblaxexamlock.buildAlarmSessionIdentity
import com.example.coblaxexamlock.buildExamKeyboardInsertScript
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.clearExamWebViewSessionData
import com.example.coblaxexamlock.config.AlarmAcknowledgeDedupWindowMillis
import com.example.coblaxexamlock.config.AppSwitchSuppressionWindowMillis
import com.example.coblaxexamlock.config.ClipboardListenerWarmupIgnoreMillis
import com.example.coblaxexamlock.config.ClipboardResumeConfirmWindowMillis
import com.example.coblaxexamlock.config.ClipboardResumeSettleWindowMillis
import com.example.coblaxexamlock.config.ClipboardSettleWindowMillis
import com.example.coblaxexamlock.config.ExamFullscreenRequestHookScript
import com.example.coblaxexamlock.config.ExamKeyboardArrowLeftScript
import com.example.coblaxexamlock.config.ExamKeyboardArrowRightScript
import com.example.coblaxexamlock.config.ExamKeyboardBackspaceScript
import com.example.coblaxexamlock.config.ExamKeyboardEnterScript
import com.example.coblaxexamlock.config.ExamNativeFullscreenBridgeInstallScript
import com.example.coblaxexamlock.config.GeofenceRuntimeRecheckIntervalMillis
import com.example.coblaxexamlock.config.InstallExamKeyboardScript
import com.example.coblaxexamlock.config.InstallExamSideArrowControlsScript
import com.example.coblaxexamlock.config.MaxDiagnosticActionLogEntries
import com.example.coblaxexamlock.config.MaxNetworkTimelineEntries
import com.example.coblaxexamlock.config.NetworkUnstableFlipThreshold
import com.example.coblaxexamlock.config.NetworkUnstableRecoveryQuietPeriodMillis
import com.example.coblaxexamlock.config.NetworkUnstableWindowMillis
import com.example.coblaxexamlock.config.OfflineTooLongWarningThresholdMillis
import com.example.coblaxexamlock.config.OverlayFocusLossConfirmWindowMillis
import com.example.coblaxexamlock.config.RemoveExamSideArrowControlsScript
import com.example.coblaxexamlock.detachExamKeyboardBridge
import com.example.coblaxexamlock.detachExamNativeFullscreenBridge
import com.example.coblaxexamlock.detachExamParticipantCaptureBridge
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofence
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.evaluateLocationFixQuality
import com.example.coblaxexamlock.format.buildExamNativeFullscreenStateSyncScript
import com.example.coblaxexamlock.format.buildIntegrityPublicSummary
import com.example.coblaxexamlock.format.diagnosticSectionEventCodes
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.format.formatElapsedDuration
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.inspectAdb
import com.example.coblaxexamlock.inspectDeviceTimeSecurity
import com.example.coblaxexamlock.isExamGuardAccessibilityAvailable
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.installExamNativeFullscreenDocumentStartScriptIfSupported
import com.example.coblaxexamlock.TrustedNetworkTimeCoordinator
import com.example.coblaxexamlock.model.AdminSettings
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkReadinessVerdict
import com.example.coblaxexamlock.model.NetworkTimelineEntry
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.model.effectiveExamUserAgent
import com.example.coblaxexamlock.model.usesDefaultExamUserAgent
import com.example.coblaxexamlock.openAccessibilitySettings
import com.example.coblaxexamlock.openAirplaneModeSettings
import com.example.coblaxexamlock.openBluetoothSettings
import com.example.coblaxexamlock.openCellularSettings
import com.example.coblaxexamlock.openDateTimeSettings
import com.example.coblaxexamlock.openDeveloperOptionsSettings
import com.example.coblaxexamlock.openInternetConnectivitySettings
import com.example.coblaxexamlock.openKeyboardSettings
import com.example.coblaxexamlock.openLocationServicesSettings
import com.example.coblaxexamlock.openOverlaySettings
import com.example.coblaxexamlock.openScreenPinningSettings
import com.example.coblaxexamlock.openWifiSettings
import com.example.coblaxexamlock.parseExamParticipantContext
import com.example.coblaxexamlock.parseExamAlarmSeverity
import com.example.coblaxexamlock.parseGeofenceConfig
import com.example.coblaxexamlock.platform.openExternalUrl
import com.example.coblaxexamlock.prepareForFreshExamSession
import com.example.coblaxexamlock.readClipboardSnapshotFull
import com.example.coblaxexamlock.readClipboardSnapshotLite
import com.example.coblaxexamlock.resolveExpectedSigningFingerprints
import com.example.coblaxexamlock.runtime.acquireBestEffortLocationSnapshot
import com.example.coblaxexamlock.runtime.buildRootIssueMessage
import com.example.coblaxexamlock.runtime.detectSuspiciousFakeLocationPackages
import com.example.coblaxexamlock.runtime.getBluetoothConnectPermission
import com.example.coblaxexamlock.runtime.getCurrentInputMethodPackage
import com.example.coblaxexamlock.runtime.getRootDetectionDetails
import com.example.coblaxexamlock.runtime.getVirtualEnvironmentDiagnostics
import com.example.coblaxexamlock.runtime.hasBluetoothExamPermission
import com.example.coblaxexamlock.runtime.hasFineLocationPermission
import com.example.coblaxexamlock.runtime.hasLocationPermissionForWifi
import com.example.coblaxexamlock.runtime.isAllowedExamKeyboard
import com.example.coblaxexamlock.runtime.isBluetoothEnabledForExam
import com.example.coblaxexamlock.runtime.isLocationServicesEnabled
import com.example.coblaxexamlock.runtime.readExamBatteryStatus
import com.example.coblaxexamlock.runtime.readNetworkReadinessStatus
import com.example.coblaxexamlock.runtime.requiresBluetoothExamPermission
import com.example.coblaxexamlock.runtime.resolveKeyboardAppLabel
import com.example.coblaxexamlock.runtime.sendTelegramAlarmAcknowledge
import com.example.coblaxexamlock.runtime.sendTelegramSectionReport
import com.example.coblaxexamlock.save.DiagnosticEventLogSaver
import com.example.coblaxexamlock.sendExamArrowKeyFallback
import com.example.coblaxexamlock.showKeyboardPicker
import com.example.coblaxexamlock.ui.admin.InfoDialog
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsActions
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsHost
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsState
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
import com.example.coblaxexamlock.ui.preparation.ExamPreparationScene
import com.example.coblaxexamlock.ui.preparation.PreparationScreenActions
import com.example.coblaxexamlock.ui.preparation.PreparationScreenState
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
import com.example.coblaxexamlock.viewmodel.AdminFlowViewModel
import com.example.coblaxexamlock.viewmodel.ExamRuntimeUiAction
import com.example.coblaxexamlock.viewmodel.rememberBoundExamRuntimeViewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val ExamStartPerfTag = "ExamStartPerf"
private const val PreparationLocationWarmupIntervalMillis = 10_000L
private const val StartExamWarmLocationReuseWindowMillis = 12_000L
private const val NetworkReadinessPollingStableIntervalMillis = 10_000L
private const val NetworkReadinessPollingUnstableIntervalMillis = 3_000L
private const val ExamServerProbeIntervalMillis = 30_000L
private const val ExamServerProbeTimeoutMillis = 3_500
private const val ExamServerProbeSlowThresholdMillis = 3_000L
private const val ScreenPinningMonitorWarmupIntervalMillis = 300L
private const val ScreenPinningMonitorSteadyIntervalMillis = 1_000L
private const val ScreenPinningMonitorWarmupWindowMillis = 5_000L
private const val ScreenPinningMonitorStartupGraceMillis = 8_000L
private const val ScreenPinningMonitorLostConfirmWindowMillis = 2_000L
private const val ScreenPinningMonitorStartupRecoveryMaxAttempts = 3
private const val RuntimeSecurityRefreshCacheTtlMillis = 1_500L

private data class WarmLocationValidationCache(
    val result: SplitLocationSecurityStatus,
    val validationKey: String,
    val completedAtElapsedMs: Long,
    val completedAtTimestamp: String
)

private data class RuntimeReverseEngineeringRefreshCache(
    val result: ReverseEngineeringResult,
    val capturedAtElapsedMs: Long
)

private data class RuntimeIntegrityRefreshCache(
    val result: IntegrityCheckResult,
    val baselineFingerprint: String?,
    val capturedAtElapsedMs: Long
)

private data class ExamServerHttpProbeOutcome(
    val method: String,
    val code: Int?,
    val latencyMs: Long,
    val failure: String?
)

private data class ExamServerProbeResult(
    val status: ExamServerFooterStatus,
    val host: String,
    val method: String,
    val code: Int?,
    val latencyMs: Long?,
    val reason: String
) {
    val eventCode: String
        get() = when (status) {
            ExamServerFooterStatus.Online -> "EXAM_SERVER_PROBE_ONLINE"
            ExamServerFooterStatus.Warning -> "EXAM_SERVER_PROBE_WARNING"
            ExamServerFooterStatus.Offline -> "EXAM_SERVER_PROBE_OFFLINE"
            ExamServerFooterStatus.Checking -> "EXAM_SERVER_PROBE_STARTED"
        }

    val eventLevel: DiagnosticEventLevel
        get() = when (status) {
            ExamServerFooterStatus.Online,
            ExamServerFooterStatus.Checking -> DiagnosticEventLevel.INFO
            ExamServerFooterStatus.Warning -> DiagnosticEventLevel.WARNING
            ExamServerFooterStatus.Offline -> DiagnosticEventLevel.ERROR
        }
}

private fun safeExamServerHost(examUrl: String): String {
    return runCatching { URL(examUrl).host.orEmpty().trim() }
        .getOrDefault("")
        .ifBlank { "-" }
}

private fun buildExamServerProbeDetails(
    trigger: String,
    host: String,
    method: String? = null,
    code: Int? = null,
    latencyMs: Long? = null,
    reason: String? = null
): String {
    return buildString {
        append("trigger=").append(trigger)
        append(" | host=").append(host.ifBlank { "-" })
        method?.let { append(" | method=").append(it.ifBlank { "-" }) }
        append(" | code=").append(code?.toString() ?: "-")
        append(" | latency_ms=").append(latencyMs?.toString() ?: "-")
        reason?.let { append(" | reason=").append(it.ifBlank { "-" }) }
    }
}

private fun executeExamServerHttpProbe(
    url: URL,
    method: String
): ExamServerHttpProbeOutcome {
    var connection: HttpURLConnection? = null
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = ExamServerProbeTimeoutMillis
            readTimeout = ExamServerProbeTimeoutMillis
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("User-Agent", "CBX-Exam-Server-Probe/${BuildConfig.VERSION_NAME}")
            if (method == "GET") {
                setRequestProperty("Range", "bytes=0-0")
            }
        }
        ExamServerHttpProbeOutcome(
            method = method,
            code = connection.responseCode,
            latencyMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L),
            failure = null
        )
    } catch (throwable: Exception) {
        ExamServerHttpProbeOutcome(
            method = method,
            code = null,
            latencyMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L),
            failure = throwable.javaClass.simpleName.ifBlank { "connection_failed" }
        )
    } finally {
        connection?.disconnect()
    }
}

private fun classifyExamServerProbeOutcome(
    host: String,
    outcome: ExamServerHttpProbeOutcome
): ExamServerProbeResult {
    val code = outcome.code
    val status = when {
        code == null -> ExamServerFooterStatus.Offline
        code in 200..399 || code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN ->
            if (outcome.latencyMs > ExamServerProbeSlowThresholdMillis) {
                ExamServerFooterStatus.Warning
            } else {
                ExamServerFooterStatus.Online
            }
        code in 400..499 -> ExamServerFooterStatus.Warning
        code >= 500 -> ExamServerFooterStatus.Offline
        else -> ExamServerFooterStatus.Warning
    }
    val reason = when {
        code == null -> outcome.failure ?: "connection_failed"
        status == ExamServerFooterStatus.Online -> "reachable"
        outcome.latencyMs > ExamServerProbeSlowThresholdMillis &&
            (code in 200..399 || code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) ->
            "slow_response"
        code in 400..499 -> "http_client_error"
        code >= 500 -> "http_server_error"
        else -> "unexpected_http_status"
    }
    return ExamServerProbeResult(
        status = status,
        host = host,
        method = outcome.method,
        code = code,
        latencyMs = outcome.latencyMs,
        reason = reason
    )
}

private suspend fun probeExamServerFooterStatus(examUrl: String): ExamServerProbeResult =
    withContext(Dispatchers.IO) {
        val url = runCatching { URL(examUrl) }.getOrNull()
            ?: return@withContext ExamServerProbeResult(
                status = ExamServerFooterStatus.Warning,
                host = "-",
                method = "-",
                code = null,
                latencyMs = null,
                reason = "invalid_exam_url"
            )
        val host = url.host.orEmpty().ifBlank { "-" }
        val headOutcome = executeExamServerHttpProbe(url, method = "HEAD")
        val finalOutcome =
            if (headOutcome.code == HttpURLConnection.HTTP_BAD_METHOD || headOutcome.code == HttpURLConnection.HTTP_NOT_IMPLEMENTED) {
                executeExamServerHttpProbe(url, method = "GET")
            } else {
                headOutcome
            }
        classifyExamServerProbeOutcome(host, finalOutcome)
    }

private fun buildWarmLocationValidationPolicySignature(
    geofenceConfigParseResult: GeofenceConfigParseResult,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState
): String {
    val config = geofenceConfigParseResult.config
    val verticesSignature = config?.vertices?.joinToString(";") {
        "${it.latitude},${it.longitude}"
    } ?: "-"
    val circleCentersSignature = config?.circleCenters?.joinToString(";") {
        "${it.latitude},${it.longitude}"
    } ?: "-"
    return buildString {
        append("enabled=").append(geofenceConfigParseResult.enabled)
        append("|error=").append(geofenceConfigParseResult.error ?: "-")
        append("|shape=").append(config?.shapeType?.name ?: "-")
        append("|center=").append(config?.centerLat ?: "-")
        append(',').append(config?.centerLng ?: "-")
        append("|radius=").append(config?.radiusMeters ?: "-")
        append("|vertices=").append(verticesSignature)
        append("|circle_centers=").append(circleCentersSignature)
        append("|geofence_bypass=").append(geofenceBypassState.name)
        append("|fake_location_bypass=").append(fakeLocationBypassState.name)
    }
}

private fun buildWarmLocationValidationKey(
    permissionGranted: Boolean,
    locationServicesEnabled: Boolean,
    policySignature: String
): String {
    return buildString {
        append(policySignature)
        append("|permission=").append(permissionGranted)
        append("|location_services=").append(locationServicesEnabled)
    }
}

private fun SplitLocationSecurityStatus.reuseBlockingReason(): String? {
    return when {
        !geofenceStatus.safe -> "geofence_unsafe"
        !fakeLocationStatus.safe -> "fake_location_unsafe"
        !geofenceStatus.fixQualityStatus.usableForGeofence -> "geofence_fix_quality_unusable"
        !fakeLocationStatus.fixQualityEligible -> "fake_location_fix_quality_ineligible"
        else -> null
    }
}

private fun WarmLocationValidationCache.reuseFailureReason(
    currentValidationKey: String,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): String? {
    val ageMs = (nowElapsedMs - completedAtElapsedMs).coerceAtLeast(0L)
    return when {
        validationKey != currentValidationKey -> "location_inputs_changed"
        ageMs > StartExamWarmLocationReuseWindowMillis -> "warm_cache_stale"
        else -> result.reuseBlockingReason()
    }
}

private fun WarmLocationValidationCache.isReusableForStart(
    currentValidationKey: String,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): Boolean {
    return reuseFailureReason(
        currentValidationKey = currentValidationKey,
        nowElapsedMs = nowElapsedMs
    ) == null
}

private fun RuntimeReverseEngineeringRefreshCache.isFresh(
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): Boolean {
    return (nowElapsedMs - capturedAtElapsedMs).coerceAtLeast(0L) <= RuntimeSecurityRefreshCacheTtlMillis
}

private fun RuntimeIntegrityRefreshCache.isFreshFor(
    baselineFingerprint: String?,
    nowElapsedMs: Long = SystemClock.elapsedRealtime()
): Boolean {
    return baselineFingerprint == this.baselineFingerprint &&
        (nowElapsedMs - capturedAtElapsedMs).coerceAtLeast(0L) <= RuntimeSecurityRefreshCacheTtlMillis
}

private class ExamRuntimeFlowUiState(
    val examSessionStarted: MutableState<Boolean>,
    val lockTaskRequestPending: MutableState<Boolean>,
    val screenPinningMessage: MutableState<String?>,
    val showExitExamDialog: MutableState<Boolean>,
    val exitSessionClearInFlight: MutableState<Boolean>,
    val webViewErrorMessage: MutableState<String?>,
    val useBuiltInExamKeyboard: MutableState<Boolean>,
    val showBuiltInExamKeyboard: MutableState<Boolean>,
    val sideArrowControlsVisible: MutableState<Boolean>,
    val hasEditableFocus: MutableState<Boolean>,
    val builtInKeyboardShiftEnabled: MutableState<Boolean>,
    val geofencePermissionRequestInFlight: MutableState<Boolean>,
    val geofenceStartValidationInFlight: MutableState<Boolean>,
    val webViewSessionResetInFlight: MutableState<Boolean>,
    val webViewSessionResetError: MutableState<String?>,
    val geofenceManualRefreshInFlight: MutableState<Boolean>,
    val pendingStartExamAfterLocationPermission: MutableState<Boolean>,
    val retryStartExamAfterLocationPermissionGrant: MutableState<Boolean>,
    val geofenceViolationCount: MutableIntState,
    val showGeofenceViolationDialog: MutableState<Boolean>,
    val showGeofenceMapViewer: MutableState<Boolean>,
    val lastGeofenceTrigger: MutableState<String?>,
    val lastGeofenceAt: MutableState<String?>,
    val lastGeofenceContext: MutableState<String?>,
    val lastGeofenceRefreshAt: MutableState<String?>,
    val geofenceRuntimeEpisodeKey: MutableState<String?>,
    val fakeLocationViolationCount: MutableIntState,
    val showFakeLocationViolationDialog: MutableState<Boolean>,
    val lastFakeLocationTrigger: MutableState<String?>,
    val lastFakeLocationAt: MutableState<String?>,
    val lastFakeLocationContext: MutableState<String?>,
    val fakeLocationRuntimeEpisodeKey: MutableState<String?>,
    val lastFakeLocationWarningKey: MutableState<String?>,
    val currentKeyboardPackage: MutableState<String>,
    val currentKeyboardLabel: MutableState<String>,
    val lastKeyboardAllowed: MutableState<Boolean>
)

@Composable
private fun rememberExamRuntimeFlowUiState(
    context: Context,
    bypassKeyboardPolicy: Boolean
): ExamRuntimeFlowUiState {
    val examSessionStarted = rememberSaveable { mutableStateOf(false) }
    val lockTaskRequestPending = rememberSaveable { mutableStateOf(false) }
    val screenPinningMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val showExitExamDialog = rememberSaveable { mutableStateOf(false) }
    val exitSessionClearInFlight = rememberSaveable { mutableStateOf(false) }
    val webViewErrorMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val useBuiltInExamKeyboard = rememberSaveable { mutableStateOf(false) }
    val showBuiltInExamKeyboard = rememberSaveable { mutableStateOf(false) }
    val sideArrowControlsVisible = rememberSaveable { mutableStateOf(true) }
    val hasEditableFocus = rememberSaveable { mutableStateOf(false) }
    val builtInKeyboardShiftEnabled = rememberSaveable { mutableStateOf(false) }
    val geofencePermissionRequestInFlight = rememberSaveable { mutableStateOf(false) }
    val geofenceStartValidationInFlight = rememberSaveable { mutableStateOf(false) }
    val webViewSessionResetInFlight = rememberSaveable { mutableStateOf(false) }
    val webViewSessionResetError = rememberSaveable { mutableStateOf<String?>(null) }
    val geofenceManualRefreshInFlight = rememberSaveable { mutableStateOf(false) }
    val pendingStartExamAfterLocationPermission = rememberSaveable { mutableStateOf(false) }
    val retryStartExamAfterLocationPermissionGrant = rememberSaveable { mutableStateOf(false) }
    val geofenceViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showGeofenceViolationDialog = rememberSaveable { mutableStateOf(false) }
    val showGeofenceMapViewer = rememberSaveable { mutableStateOf(false) }
    val lastGeofenceTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastGeofenceAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastGeofenceContext = rememberSaveable { mutableStateOf<String?>(null) }
    val lastGeofenceRefreshAt = rememberSaveable { mutableStateOf<String?>(null) }
    val geofenceRuntimeEpisodeKey = rememberSaveable { mutableStateOf<String?>(null) }
    val fakeLocationViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showFakeLocationViolationDialog = rememberSaveable { mutableStateOf(false) }
    val lastFakeLocationTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastFakeLocationAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastFakeLocationContext = rememberSaveable { mutableStateOf<String?>(null) }
    val fakeLocationRuntimeEpisodeKey = rememberSaveable { mutableStateOf<String?>(null) }
    val lastFakeLocationWarningKey = rememberSaveable { mutableStateOf<String?>(null) }
    val currentKeyboardPackage = rememberSaveable {
        mutableStateOf(getCurrentInputMethodPackage(context).orEmpty())
    }
    val currentKeyboardLabel = rememberSaveable {
        mutableStateOf(resolveKeyboardAppLabel(context, currentKeyboardPackage.value))
    }
    val lastKeyboardAllowed = rememberSaveable {
        mutableStateOf(
            bypassKeyboardPolicy || isAllowedExamKeyboard(context, currentKeyboardPackage.value)
        )
    }
    return remember {
        ExamRuntimeFlowUiState(
            examSessionStarted = examSessionStarted,
            lockTaskRequestPending = lockTaskRequestPending,
            screenPinningMessage = screenPinningMessage,
            showExitExamDialog = showExitExamDialog,
            exitSessionClearInFlight = exitSessionClearInFlight,
            webViewErrorMessage = webViewErrorMessage,
            useBuiltInExamKeyboard = useBuiltInExamKeyboard,
            showBuiltInExamKeyboard = showBuiltInExamKeyboard,
            sideArrowControlsVisible = sideArrowControlsVisible,
            hasEditableFocus = hasEditableFocus,
            builtInKeyboardShiftEnabled = builtInKeyboardShiftEnabled,
            geofencePermissionRequestInFlight = geofencePermissionRequestInFlight,
            geofenceStartValidationInFlight = geofenceStartValidationInFlight,
            webViewSessionResetInFlight = webViewSessionResetInFlight,
            webViewSessionResetError = webViewSessionResetError,
            geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
            pendingStartExamAfterLocationPermission = pendingStartExamAfterLocationPermission,
            retryStartExamAfterLocationPermissionGrant = retryStartExamAfterLocationPermissionGrant,
            geofenceViolationCount = geofenceViolationCount,
            showGeofenceViolationDialog = showGeofenceViolationDialog,
            showGeofenceMapViewer = showGeofenceMapViewer,
            lastGeofenceTrigger = lastGeofenceTrigger,
            lastGeofenceAt = lastGeofenceAt,
            lastGeofenceContext = lastGeofenceContext,
            lastGeofenceRefreshAt = lastGeofenceRefreshAt,
            geofenceRuntimeEpisodeKey = geofenceRuntimeEpisodeKey,
            fakeLocationViolationCount = fakeLocationViolationCount,
            showFakeLocationViolationDialog = showFakeLocationViolationDialog,
            lastFakeLocationTrigger = lastFakeLocationTrigger,
            lastFakeLocationAt = lastFakeLocationAt,
            lastFakeLocationContext = lastFakeLocationContext,
            fakeLocationRuntimeEpisodeKey = fakeLocationRuntimeEpisodeKey,
            lastFakeLocationWarningKey = lastFakeLocationWarningKey,
            currentKeyboardPackage = currentKeyboardPackage,
            currentKeyboardLabel = currentKeyboardLabel,
            lastKeyboardAllowed = lastKeyboardAllowed
        )
    }
}

private class ExamRuntimeNetworkUiState(
    val networkUnstableEpisodeStartedAt: MutableState<String?>,
    val networkUnstableEpisodeStartedElapsedMs: MutableState<Long?>,
    val networkUnstableLastFlapAt: MutableState<String?>,
    val networkUnstableLastFlapElapsedMs: MutableState<Long?>,
    val networkUnstableWarningShown: MutableState<Boolean>,
    val lastNetworkUnstableWarningAt: MutableState<String?>,
    val showNetworkUnstableDialog: MutableState<Boolean>,
    val networkUnstableFlapCount: MutableIntState,
    val networkUnstableLastTransportLabel: MutableState<String?>,
    val lastNetworkChangeAt: MutableState<String?>,
    val lastNetworkChangeSource: MutableState<String?>,
    val networkManualRefreshInFlight: MutableState<Boolean>,
    val lastConnectedNetworkLabel: MutableState<String?>,
    val offlineStartedAtElapsedMs: MutableState<Long?>,
    val offlineStartedAtTimestamp: MutableState<String?>,
    val offlineWarningShown: MutableState<Boolean>,
    val lastOfflineWarningAt: MutableState<String?>,
    val lastOfflineWarningElapsedMs: MutableState<Long?>,
    val lastOfflineDurationMs: MutableState<Long?>,
    val offlineWarningDurationMs: MutableState<Long?>,
    val showOfflineWarningDialog: MutableState<Boolean>
)

@Composable
private fun rememberExamRuntimeNetworkUiState(
    baseNetworkReadiness: NetworkReadinessStatus
): ExamRuntimeNetworkUiState {
    val networkUnstableEpisodeStartedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val networkUnstableEpisodeStartedElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val networkUnstableLastFlapAt = rememberSaveable { mutableStateOf<String?>(null) }
    val networkUnstableLastFlapElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val networkUnstableWarningShown = rememberSaveable { mutableStateOf(false) }
    val lastNetworkUnstableWarningAt = rememberSaveable { mutableStateOf<String?>(null) }
    val showNetworkUnstableDialog = rememberSaveable { mutableStateOf(false) }
    val networkUnstableFlapCount = rememberSaveable { mutableIntStateOf(0) }
    val networkUnstableLastTransportLabel = rememberSaveable { mutableStateOf<String?>(null) }
    val lastNetworkChangeAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastNetworkChangeSource = rememberSaveable { mutableStateOf<String?>(null) }
    val networkManualRefreshInFlight = rememberSaveable { mutableStateOf(false) }
    val lastConnectedNetworkLabel = rememberSaveable {
        mutableStateOf<String?>(
            baseNetworkReadiness.transportLabel.takeIf { baseNetworkReadiness.examStatus.isConnected }
        )
    }
    val offlineStartedAtElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val offlineStartedAtTimestamp = rememberSaveable { mutableStateOf<String?>(null) }
    val offlineWarningShown = rememberSaveable { mutableStateOf(false) }
    val lastOfflineWarningAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOfflineWarningElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val lastOfflineDurationMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val offlineWarningDurationMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val showOfflineWarningDialog = rememberSaveable { mutableStateOf(false) }
    return remember {
        ExamRuntimeNetworkUiState(
            networkUnstableEpisodeStartedAt = networkUnstableEpisodeStartedAt,
            networkUnstableEpisodeStartedElapsedMs = networkUnstableEpisodeStartedElapsedMs,
            networkUnstableLastFlapAt = networkUnstableLastFlapAt,
            networkUnstableLastFlapElapsedMs = networkUnstableLastFlapElapsedMs,
            networkUnstableWarningShown = networkUnstableWarningShown,
            lastNetworkUnstableWarningAt = lastNetworkUnstableWarningAt,
            showNetworkUnstableDialog = showNetworkUnstableDialog,
            networkUnstableFlapCount = networkUnstableFlapCount,
            networkUnstableLastTransportLabel = networkUnstableLastTransportLabel,
            lastNetworkChangeAt = lastNetworkChangeAt,
            lastNetworkChangeSource = lastNetworkChangeSource,
            networkManualRefreshInFlight = networkManualRefreshInFlight,
            lastConnectedNetworkLabel = lastConnectedNetworkLabel,
            offlineStartedAtElapsedMs = offlineStartedAtElapsedMs,
            offlineStartedAtTimestamp = offlineStartedAtTimestamp,
            offlineWarningShown = offlineWarningShown,
            lastOfflineWarningAt = lastOfflineWarningAt,
            lastOfflineWarningElapsedMs = lastOfflineWarningElapsedMs,
            lastOfflineDurationMs = lastOfflineDurationMs,
            offlineWarningDurationMs = offlineWarningDurationMs,
            showOfflineWarningDialog = showOfflineWarningDialog
        )
    }
}

private class ExamRuntimeSecurityUiState(
    val forcedExitViolationCount: MutableIntState,
    val pendingForcedExitViolation: MutableState<Boolean>,
    val showForcedExitAlarm: MutableState<Boolean>,
    val keyboardViolationCount: MutableIntState,
    val showKeyboardViolationDialog: MutableState<Boolean>,
    val overlayViolationCount: MutableIntState,
    val showOverlayViolationDialog: MutableState<Boolean>,
    val overlayShieldRequested: MutableState<Boolean>,
    val overlayShieldLastApplySucceeded: MutableState<Boolean?>,
    val overlayShieldLastAppliedAt: MutableState<String?>,
    val lastOverlayTrigger: MutableState<String?>,
    val lastOverlayAt: MutableState<String?>,
    val lastOverlayContext: MutableState<String?>,
    val overlayWindowHasFocus: MutableState<Boolean>,
    val overlayWindowFocusLossPending: MutableState<Boolean>,
    val overlayFocusLossConfirmRunnable: MutableState<Runnable?>,
    val bluetoothPermissionGranted: MutableState<Boolean>,
    val bluetoothEnabled: MutableState<Boolean>,
    val accessibilityInspection: MutableState<AccessibilityInspectionResult>,
    val accessibilityServiceEnabled: MutableState<Boolean>,
    val adbInspection: MutableState<AdbInspection>,
    val developerOptionsEnabled: MutableState<Boolean>,
    val adbEnabled: MutableState<Boolean>,
    val rootSecurityStatus: MutableState<RootSecurityStatus>,
    val rootDetected: MutableState<Boolean>,
    val selinuxPermissiveWarning: MutableState<Boolean>,
    val signatureMismatchDetected: MutableState<Boolean>,
    val virtualEnvironmentDetected: MutableState<Boolean>,
    val tamperDetected: MutableState<Boolean>,
    val tamperSummary: MutableState<String>,
    val tamperLastLoggedSummary: MutableState<String?>,
    val integrityTamperDetected: MutableState<Boolean>,
    val integritySummary: MutableState<String>,
    val integrityPublicSummary: MutableState<String>,
    val integrityLastLoggedSummary: MutableState<String?>,
    val integrityBaselineFingerprint: MutableState<String?>,
    val bluetoothViolationCount: MutableIntState,
    val showBluetoothViolationDialog: MutableState<Boolean>,
    val geofenceEvaluation: MutableState<GeofenceEvaluation>,
    val geofenceSecurityStatus: MutableState<GeofenceSecurityStatus>,
    val fakeLocationSecurityStatus: MutableState<LocationSpoofSecurityStatus>
)

@Composable
private fun rememberExamRuntimeSecurityUiState(
    context: Context,
    geofenceConfigParseResult: GeofenceConfigParseResult,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState
): ExamRuntimeSecurityUiState {
    val forcedExitViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val pendingForcedExitViolation = rememberSaveable { mutableStateOf(false) }
    val showForcedExitAlarm = rememberSaveable { mutableStateOf(false) }
    val keyboardViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showKeyboardViolationDialog = rememberSaveable { mutableStateOf(false) }
    val overlayViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showOverlayViolationDialog = rememberSaveable { mutableStateOf(false) }
    val overlayShieldRequested = rememberSaveable { mutableStateOf(false) }
    val overlayShieldLastApplySucceeded = rememberSaveable { mutableStateOf<Boolean?>(null) }
    val overlayShieldLastAppliedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOverlayTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOverlayAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastOverlayContext = rememberSaveable { mutableStateOf<String?>(null) }
    val overlayWindowHasFocus = rememberSaveable { mutableStateOf(true) }
    val overlayWindowFocusLossPending = rememberSaveable { mutableStateOf(false) }
    val overlayFocusLossConfirmRunnable = remember { mutableStateOf<Runnable?>(null) }
    val bluetoothPermissionGranted = rememberSaveable {
        mutableStateOf(hasBluetoothExamPermission(context))
    }
    val bluetoothEnabled = rememberSaveable {
        mutableStateOf(isBluetoothEnabledForExam(context))
    }
    val initialAccessibilityInspection = remember(context) { inspectAccessibility(context) }
    val accessibilityInspection = remember { mutableStateOf(initialAccessibilityInspection) }
    val accessibilityServiceEnabled = rememberSaveable {
        mutableStateOf(initialAccessibilityInspection.blockingServiceActive)
    }
    val initialAdbInspection = remember(context) { inspectAdb(context) }
    val adbInspection = remember { mutableStateOf(initialAdbInspection) }
    val developerOptionsEnabled = rememberSaveable {
        mutableStateOf(initialAdbInspection.developerOptionsEnabled)
    }
    val adbEnabled = rememberSaveable {
        mutableStateOf(initialAdbInspection.adbEnabled)
    }
    val initialRootStatus = remember(context) {
        buildRootSecurityStatus(getRootDetectionDetails(context))
    }
    val rootSecurityStatus = remember { mutableStateOf(initialRootStatus) }
    val rootDetected = rememberSaveable {
        mutableStateOf(initialRootStatus.detected)
    }
    val selinuxPermissiveWarning = rememberSaveable {
        mutableStateOf(initialRootStatus.selinuxPermissive)
    }
    val signatureMismatchDetected = rememberSaveable { mutableStateOf(false) }
    val virtualEnvironmentDetected = rememberSaveable {
        mutableStateOf(getVirtualEnvironmentDiagnostics(context).detected)
    }
    val tamperDetected = rememberSaveable { mutableStateOf(false) }
    val tamperSummary = rememberSaveable { mutableStateOf("-") }
    val tamperLastLoggedSummary = rememberSaveable { mutableStateOf<String?>(null) }
    val integrityTamperDetected = rememberSaveable { mutableStateOf(false) }
    val integritySummary = rememberSaveable { mutableStateOf("-") }
    val integrityPublicSummary = rememberSaveable { mutableStateOf("OK") }
    val integrityLastLoggedSummary = rememberSaveable { mutableStateOf<String?>(null) }
    val integrityBaselineFingerprint = rememberSaveable { mutableStateOf<String?>(null) }
    val bluetoothViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val showBluetoothViolationDialog = rememberSaveable { mutableStateOf(false) }
    val initialGeofenceEvaluation = remember(
        geofenceConfigParseResult,
        context
    ) {
        evaluateGeofence(
            configResult = geofenceConfigParseResult,
            permissionGranted = hasLocationPermissionForWifi(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            locationSnapshot = null
        )
    }
    val geofenceEvaluation = remember(geofenceConfigParseResult) {
        mutableStateOf(initialGeofenceEvaluation)
    }
    val initialGeofenceSecurityStatus = remember(
        geofenceConfigParseResult,
        context,
        geofenceBypassState
    ) {
        evaluateGeofenceSecurity(
            configResult = geofenceConfigParseResult,
            permissionGranted = hasLocationPermissionForWifi(context),
            preciseLocationGranted = hasFineLocationPermission(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            locationSnapshot = null,
            bypassState = geofenceBypassState
        )
    }
    val geofenceSecurityStatus = remember(
        geofenceConfigParseResult,
        geofenceBypassState
    ) {
        mutableStateOf(initialGeofenceSecurityStatus)
    }
    val initialFakeLocationSecurityStatus = remember(
        geofenceConfigParseResult,
        context,
        fakeLocationBypassState
    ) {
        evaluateFakeLocationSecurity(
            monitoringEnabled = true,
            permissionGranted = hasLocationPermissionForWifi(context),
            locationServicesEnabled = isLocationServicesEnabled(context),
            locationSnapshot = null,
            fixQualityStatus = initialGeofenceSecurityStatus.fixQualityStatus,
            developerOptionsEnabled = inspectAdb(context).developerOptionsEnabled,
            suspiciousFakeLocationPackages = detectSuspiciousFakeLocationPackages(context),
            bypassState = fakeLocationBypassState
        )
    }
    val fakeLocationSecurityStatus = remember(
        geofenceConfigParseResult,
        fakeLocationBypassState
    ) {
        mutableStateOf(initialFakeLocationSecurityStatus)
    }
    return ExamRuntimeSecurityUiState(
        forcedExitViolationCount = forcedExitViolationCount,
        pendingForcedExitViolation = pendingForcedExitViolation,
        showForcedExitAlarm = showForcedExitAlarm,
        keyboardViolationCount = keyboardViolationCount,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        overlayViolationCount = overlayViolationCount,
        showOverlayViolationDialog = showOverlayViolationDialog,
        overlayShieldRequested = overlayShieldRequested,
        overlayShieldLastApplySucceeded = overlayShieldLastApplySucceeded,
        overlayShieldLastAppliedAt = overlayShieldLastAppliedAt,
        lastOverlayTrigger = lastOverlayTrigger,
        lastOverlayAt = lastOverlayAt,
        lastOverlayContext = lastOverlayContext,
        overlayWindowHasFocus = overlayWindowHasFocus,
        overlayWindowFocusLossPending = overlayWindowFocusLossPending,
        overlayFocusLossConfirmRunnable = overlayFocusLossConfirmRunnable,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bluetoothEnabled = bluetoothEnabled,
        accessibilityInspection = accessibilityInspection,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        adbInspection = adbInspection,
        developerOptionsEnabled = developerOptionsEnabled,
        adbEnabled = adbEnabled,
        rootSecurityStatus = rootSecurityStatus,
        rootDetected = rootDetected,
        selinuxPermissiveWarning = selinuxPermissiveWarning,
        signatureMismatchDetected = signatureMismatchDetected,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        tamperDetected = tamperDetected,
        tamperSummary = tamperSummary,
        tamperLastLoggedSummary = tamperLastLoggedSummary,
        integrityTamperDetected = integrityTamperDetected,
        integritySummary = integritySummary,
        integrityPublicSummary = integrityPublicSummary,
        integrityLastLoggedSummary = integrityLastLoggedSummary,
        integrityBaselineFingerprint = integrityBaselineFingerprint,
        bluetoothViolationCount = bluetoothViolationCount,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        geofenceEvaluation = geofenceEvaluation,
        geofenceSecurityStatus = geofenceSecurityStatus,
        fakeLocationSecurityStatus = fakeLocationSecurityStatus
    )
}

private class ExamRuntimeClipboardUiState(
    val clipboardSignature: MutableState<String>,
    val clipboardDecisionFingerprint: MutableState<String>,
    val clipboardDecisionSemanticSignature: MutableState<String>,
    val clipboardViolationCount: MutableIntState,
    val lastClipboardChangeEvent: MutableState<String>,
    val lastClipboardObservedAt: MutableState<String?>,
    val lastClipboardConfirmedAt: MutableState<String?>,
    val lastClipboardObservedSignature: MutableState<String?>,
    val lastClipboardBaselineSemanticSignature: MutableState<String?>,
    val lastClipboardDetectedSemanticSignature: MutableState<String?>,
    val lastClipboardDecision: MutableState<String>,
    val clipboardPreBackgroundFingerprint: MutableState<String?>,
    val clipboardPreBackgroundSignature: MutableState<String?>,
    val clipboardPreBackgroundSemanticSignature: MutableState<String?>,
    val clipboardConfirmRunnable: MutableState<Runnable?>,
    val clipboardResumeCheckRunnable: MutableState<Runnable?>,
    val clipboardResumeCheckPending: MutableState<Boolean>,
    val showClipboardViolationDialog: MutableState<Boolean>
)

@Composable
private fun rememberExamRuntimeClipboardUiState(context: Context): ExamRuntimeClipboardUiState {
    val initialClipboardSnapshot = remember(context) { readClipboardSnapshotLite(context) }
    val clipboardSignature = rememberSaveable {
        mutableStateOf(initialClipboardSnapshot.rawSignature)
    }
    val clipboardDecisionFingerprint = rememberSaveable {
        mutableStateOf(initialClipboardSnapshot.decisionFingerprint)
    }
    val clipboardDecisionSemanticSignature = rememberSaveable {
        mutableStateOf(initialClipboardSnapshot.semanticSignature)
    }
    val clipboardViolationCount = rememberSaveable { mutableIntStateOf(0) }
    val lastClipboardChangeEvent = rememberSaveable { mutableStateOf("Belum ada") }
    val lastClipboardObservedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardConfirmedAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardObservedSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardBaselineSemanticSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardDetectedSemanticSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val lastClipboardDecision = rememberSaveable {
        mutableStateOf(ClipboardChangeDecision.Idle.diagnosticLabel())
    }
    val clipboardPreBackgroundFingerprint = rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardPreBackgroundSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardPreBackgroundSemanticSignature = rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardConfirmRunnable = remember { mutableStateOf<Runnable?>(null) }
    val clipboardResumeCheckRunnable = remember { mutableStateOf<Runnable?>(null) }
    val clipboardResumeCheckPending = rememberSaveable { mutableStateOf(false) }
    val showClipboardViolationDialog = rememberSaveable { mutableStateOf(false) }
    return ExamRuntimeClipboardUiState(
        clipboardSignature = clipboardSignature,
        clipboardDecisionFingerprint = clipboardDecisionFingerprint,
        clipboardDecisionSemanticSignature = clipboardDecisionSemanticSignature,
        clipboardViolationCount = clipboardViolationCount,
        lastClipboardChangeEvent = lastClipboardChangeEvent,
        lastClipboardObservedAt = lastClipboardObservedAt,
        lastClipboardConfirmedAt = lastClipboardConfirmedAt,
        lastClipboardObservedSignature = lastClipboardObservedSignature,
        lastClipboardBaselineSemanticSignature = lastClipboardBaselineSemanticSignature,
        lastClipboardDetectedSemanticSignature = lastClipboardDetectedSemanticSignature,
        lastClipboardDecision = lastClipboardDecision,
        clipboardPreBackgroundFingerprint = clipboardPreBackgroundFingerprint,
        clipboardPreBackgroundSignature = clipboardPreBackgroundSignature,
        clipboardPreBackgroundSemanticSignature = clipboardPreBackgroundSemanticSignature,
        clipboardConfirmRunnable = clipboardConfirmRunnable,
        clipboardResumeCheckRunnable = clipboardResumeCheckRunnable,
        clipboardResumeCheckPending = clipboardResumeCheckPending,
        showClipboardViolationDialog = showClipboardViolationDialog
    )
}

private class ExamRuntimeAdminUiState(
    val securityIssueDialogTitle: MutableState<String?>,
    val securityIssueDialogMessage: MutableState<String?>,
    val exitOnSecurityIssueDialogDismiss: MutableState<Boolean>,
    val screenPinningBypassTamperLogged: MutableState<Boolean>,
    val accessibilityBypassTamperLogged: MutableState<Boolean>,
    val adbBypassTamperLogged: MutableState<Boolean>,
    val clipboardBypassTamperLogged: MutableState<Boolean>,
    val overlayBypassTamperLogged: MutableState<Boolean>,
    val geofenceBypassTamperLogged: MutableState<Boolean>,
    val fakeLocationBypassTamperLogged: MutableState<Boolean>,
    val deviceTimeBypassTamperLogged: MutableState<Boolean>,
    val appSwitchBypassTamperLogged: MutableState<Boolean>,
    val rootBypassTamperLogged: MutableState<Boolean>,
    val lastAppSwitchTrigger: MutableState<String?>,
    val lastAppSwitchAt: MutableState<String?>,
    val lastAppSwitchContext: MutableState<String?>,
    val appSwitchSuppressionReason: MutableState<AppSwitchSuppressionReason?>,
    val appSwitchSuppressedUntilElapsedMs: MutableState<Long?>,
    val appSwitchLifecycleResumePending: MutableState<Boolean>,
    val appSwitchFallbackArmedLogged: MutableState<Boolean>,
    val screenPinningAvailable: MutableState<Boolean>,
    val screenPinningEnabledInSystem: MutableState<String>,
    val lockTaskStateBeforePinningRequest: MutableState<String>,
    val lockTaskStateAfterPinningRequest: MutableState<String>,
    val screenPinningRequestOutcome: MutableState<String>,
    val screenPinningDialogLikelyShown: MutableState<Boolean>,
    val screenPinningUserActionInference: MutableState<String>,
    val screenPinningActivationDurationMs: MutableState<Long?>,
    val examSessionCancelledByPinningFailure: MutableState<Boolean>,
    val sendingSection: MutableState<DiagnosticSection?>,
    val pendingSection: MutableState<DiagnosticSection?>,
    val bugReportFeedbackTitle: MutableState<String?>,
    val bugReportFeedbackMessage: MutableState<String?>,
    val appStartedAtElapsedMs: Long,
    val examRuntimeMonitoringArmed: MutableState<Boolean>,
    val examSessionStartedAtElapsedMs: MutableState<Long?>,
    val lastParticipantCaptureLogKey: MutableState<String?>,
    val participantContext: MutableState<ExamParticipantContext?>,
    val diagnosticEvents: MutableState<List<DiagnosticEvent>>,
    val lastAlarmAcknowledgeDedupKey: MutableState<String?>,
    val lastAlarmAcknowledgeAtElapsedMs: MutableLongState
)

@Composable
private fun rememberExamRuntimeAdminUiState(
    context: Context,
    payload: ExamQrPayload
): ExamRuntimeAdminUiState {
    val securityIssueDialogTitle = rememberSaveable { mutableStateOf<String?>(null) }
    val securityIssueDialogMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val exitOnSecurityIssueDialogDismiss = rememberSaveable { mutableStateOf(false) }
    val screenPinningBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val accessibilityBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val adbBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val clipboardBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val overlayBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val geofenceBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val fakeLocationBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val deviceTimeBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val appSwitchBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val rootBypassTamperLogged = rememberSaveable { mutableStateOf(false) }
    val lastAppSwitchTrigger = rememberSaveable { mutableStateOf<String?>(null) }
    val lastAppSwitchAt = rememberSaveable { mutableStateOf<String?>(null) }
    val lastAppSwitchContext = rememberSaveable { mutableStateOf<String?>(null) }
    val appSwitchSuppressionReason = rememberSaveable {
        mutableStateOf<AppSwitchSuppressionReason?>(null)
    }
    val appSwitchSuppressedUntilElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val appSwitchLifecycleResumePending = rememberSaveable { mutableStateOf(false) }
    val appSwitchFallbackArmedLogged = rememberSaveable { mutableStateOf(false) }
    val screenPinningAvailable = rememberSaveable {
        mutableStateOf(ScreenPinningPlatformBridge.isAvailable())
    }
    val screenPinningEnabledInSystem = rememberSaveable {
        mutableStateOf(ScreenPinningPlatformBridge.readSystemSetting(context))
    }
    val lockTaskStateBeforePinningRequest = rememberSaveable { mutableStateOf("Unknown") }
    val lockTaskStateAfterPinningRequest = rememberSaveable { mutableStateOf("Unknown") }
    val screenPinningRequestOutcome = rememberSaveable { mutableStateOf("Belum diminta") }
    val screenPinningDialogLikelyShown = rememberSaveable { mutableStateOf(false) }
    val screenPinningUserActionInference = rememberSaveable { mutableStateOf("Belum ada") }
    val screenPinningActivationDurationMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val examSessionCancelledByPinningFailure = rememberSaveable { mutableStateOf(false) }
    val sendingSection = rememberSaveable { mutableStateOf<DiagnosticSection?>(null) }
    val pendingSection = rememberSaveable { mutableStateOf<DiagnosticSection?>(null) }
    val bugReportFeedbackTitle = rememberSaveable { mutableStateOf<String?>(null) }
    val bugReportFeedbackMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val appStartedAtElapsedMs = rememberSaveable { SystemClock.elapsedRealtime() }
    val examRuntimeMonitoringArmed = rememberSaveable { mutableStateOf(false) }
    val examSessionStartedAtElapsedMs = rememberSaveable { mutableStateOf<Long?>(null) }
    val lastParticipantCaptureLogKey = remember(payload.examUrl, payload.examName) {
        mutableStateOf<String?>(null)
    }
    val participantContext = remember(payload.examUrl, payload.examName) {
        mutableStateOf<ExamParticipantContext?>(null)
    }
    val diagnosticEvents = rememberSaveable(stateSaver = DiagnosticEventLogSaver) {
        mutableStateOf(
            listOf(
                DiagnosticEvent(
                    timestamp = diagnosticTimestamp(),
                    level = DiagnosticEventLevel.INFO.name,
                    code = "APP_OPENED",
                    screen = "preparation",
                    appElapsedMs = 0L,
                    sessionElapsedMs = null,
                    details = "Aplikasi dibuka"
                )
            )
        )
    }
    val lastAlarmAcknowledgeDedupKey = rememberSaveable { mutableStateOf<String?>(null) }
    val lastAlarmAcknowledgeAtElapsedMs = rememberSaveable { mutableLongStateOf(0L) }
    return ExamRuntimeAdminUiState(
        securityIssueDialogTitle = securityIssueDialogTitle,
        securityIssueDialogMessage = securityIssueDialogMessage,
        exitOnSecurityIssueDialogDismiss = exitOnSecurityIssueDialogDismiss,
        screenPinningBypassTamperLogged = screenPinningBypassTamperLogged,
        accessibilityBypassTamperLogged = accessibilityBypassTamperLogged,
        adbBypassTamperLogged = adbBypassTamperLogged,
        clipboardBypassTamperLogged = clipboardBypassTamperLogged,
        overlayBypassTamperLogged = overlayBypassTamperLogged,
        geofenceBypassTamperLogged = geofenceBypassTamperLogged,
        fakeLocationBypassTamperLogged = fakeLocationBypassTamperLogged,
        deviceTimeBypassTamperLogged = deviceTimeBypassTamperLogged,
        appSwitchBypassTamperLogged = appSwitchBypassTamperLogged,
        rootBypassTamperLogged = rootBypassTamperLogged,
        lastAppSwitchTrigger = lastAppSwitchTrigger,
        lastAppSwitchAt = lastAppSwitchAt,
        lastAppSwitchContext = lastAppSwitchContext,
        appSwitchSuppressionReason = appSwitchSuppressionReason,
        appSwitchSuppressedUntilElapsedMs = appSwitchSuppressedUntilElapsedMs,
        appSwitchLifecycleResumePending = appSwitchLifecycleResumePending,
        appSwitchFallbackArmedLogged = appSwitchFallbackArmedLogged,
        screenPinningAvailable = screenPinningAvailable,
        screenPinningEnabledInSystem = screenPinningEnabledInSystem,
        lockTaskStateBeforePinningRequest = lockTaskStateBeforePinningRequest,
        lockTaskStateAfterPinningRequest = lockTaskStateAfterPinningRequest,
        screenPinningRequestOutcome = screenPinningRequestOutcome,
        screenPinningDialogLikelyShown = screenPinningDialogLikelyShown,
        screenPinningUserActionInference = screenPinningUserActionInference,
        screenPinningActivationDurationMs = screenPinningActivationDurationMs,
        examSessionCancelledByPinningFailure = examSessionCancelledByPinningFailure,
        sendingSection = sendingSection,
        pendingSection = pendingSection,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        appStartedAtElapsedMs = appStartedAtElapsedMs,
        examRuntimeMonitoringArmed = examRuntimeMonitoringArmed,
        examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
        lastParticipantCaptureLogKey = lastParticipantCaptureLogKey,
        participantContext = participantContext,
        diagnosticEvents = diagnosticEvents,
        lastAlarmAcknowledgeDedupKey = lastAlarmAcknowledgeDedupKey,
        lastAlarmAcknowledgeAtElapsedMs = lastAlarmAcknowledgeAtElapsedMs
    )
}

private class ExamRuntimeLocationWarmupUiState(
    val locationWarmupInFlight: MutableState<Boolean>,
    val reusableWarmLocationValidation: MutableState<WarmLocationValidationCache?>
)

@Composable
private fun rememberExamRuntimeLocationWarmupUiState(): ExamRuntimeLocationWarmupUiState {
    val locationWarmupInFlight = rememberSaveable { mutableStateOf(false) }
    val reusableWarmLocationValidation = remember { mutableStateOf<WarmLocationValidationCache?>(null) }
    return remember {
        ExamRuntimeLocationWarmupUiState(
            locationWarmupInFlight = locationWarmupInFlight,
            reusableWarmLocationValidation = reusableWarmLocationValidation
        )
    }
}

private inline fun <T> debugMeasureExamStartWork(label: String, block: () -> T): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                ExamStartPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

private suspend inline fun <T> debugMeasureExamStartSuspendWork(
    label: String,
    crossinline block: suspend () -> T
): T {
    val startedAt = SystemClock.elapsedRealtime()
    return try {
        block()
    } finally {
        if (BuildConfig.DEBUG) {
            Log.d(
                ExamStartPerfTag,
                "$label finished in ${SystemClock.elapsedRealtime() - startedAt} ms"
            )
        }
    }
}

private fun debugLogExamStart(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d(ExamStartPerfTag, message)
    }
}

@Composable
private fun RuntimeSetupEffects(
    context: Context,
    mainActivity: MainActivity?,
    bypassKeyboardPolicy: Boolean,
    examSessionStarted: Boolean,
    nativeExamFullscreenActive: Boolean,
    webViewInstance: WebView?,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    refreshScreenPinningDiagnostics: () -> Unit,
    refreshKeyboardSecurity: (Boolean) -> Unit,
    refreshBluetoothSecurity: (Boolean) -> Unit,
    refreshDeviceIntegritySecurity: (Boolean) -> Unit,
    updateBluetoothPermissionGranted: (Boolean) -> Unit,
    updateUseBuiltInExamKeyboard: (Boolean) -> Unit,
    updateShowBuiltInExamKeyboard: (Boolean) -> Unit,
    cleanupActiveExamWebViewInstance: () -> Unit
) {
    LaunchedEffect(Unit) {
        refreshScreenPinningDiagnostics()
        refreshKeyboardSecurity(false)
        refreshBluetoothSecurity(false)
        refreshDeviceIntegritySecurity(false)
    }

    LaunchedEffect(Unit) {
        updateBluetoothPermissionGranted(hasBluetoothExamPermission(context))
    }

    LaunchedEffect(bypassKeyboardPolicy) {
        if (bypassKeyboardPolicy) {
            updateUseBuiltInExamKeyboard(false)
            updateShowBuiltInExamKeyboard(false)
        } else {
            refreshKeyboardSecurity(false)
        }
    }

    LaunchedEffect(mainActivity, nativeExamFullscreenActive, webViewInstance) {
        nativeFullscreenBridge.updateActive(nativeExamFullscreenActive)
        mainActivity?.setExamLockMode(
            enabled = nativeExamFullscreenActive,
            allowLockTask = false
        )
        webViewInstance?.evaluateJavascript(ExamNativeFullscreenBridgeInstallScript, null)
        webViewInstance?.evaluateJavascript(
            buildExamNativeFullscreenStateSyncScript(nativeExamFullscreenActive),
            null
        )
    }

    LaunchedEffect(examSessionStarted) {
        if (!examSessionStarted) {
            cleanupActiveExamWebViewInstance()
        }
    }
}

@Composable
private fun BypassTamperLoggingEffects(
    adminSettings: AdminSettings,
    screenPinningBypassTamperLogged: Boolean,
    updateScreenPinningBypassTamperLogged: (Boolean) -> Unit,
    accessibilityBypassTamperLogged: Boolean,
    updateAccessibilityBypassTamperLogged: (Boolean) -> Unit,
    adbBypassTamperLogged: Boolean,
    updateAdbBypassTamperLogged: (Boolean) -> Unit,
    clipboardBypassTamperLogged: Boolean,
    updateClipboardBypassTamperLogged: (Boolean) -> Unit,
    overlayBypassTamperLogged: Boolean,
    updateOverlayBypassTamperLogged: (Boolean) -> Unit,
    geofenceBypassTamperLogged: Boolean,
    updateGeofenceBypassTamperLogged: (Boolean) -> Unit,
    fakeLocationBypassTamperLogged: Boolean,
    updateFakeLocationBypassTamperLogged: (Boolean) -> Unit,
    deviceTimeBypassTamperLogged: Boolean,
    updateDeviceTimeBypassTamperLogged: (Boolean) -> Unit,
    appSwitchBypassTamperLogged: Boolean,
    updateAppSwitchBypassTamperLogged: (Boolean) -> Unit,
    rootBypassTamperLogged: Boolean,
    updateRootBypassTamperLogged: (Boolean) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    LaunchedEffect(adminSettings.screenPinningBypassTampered) {
        if (adminSettings.screenPinningBypassTampered && !screenPinningBypassTamperLogged) {
            recordAction(
                ScreenPinningSignals.eventBypassTampered(),
                ScreenPinningSignals.bypassTamperDetail(),
                DiagnosticEventLevel.SECURITY
            )
            updateScreenPinningBypassTamperLogged(true)
        } else if (!adminSettings.screenPinningBypassTampered) {
            updateScreenPinningBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.accessibilityBypassTampered) {
        if (adminSettings.accessibilityBypassTampered && !accessibilityBypassTamperLogged) {
            recordAction(
                "ACCESSIBILITY_BYPASS_TAMPER_DETECTED",
                "Accessibility bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateAccessibilityBypassTamperLogged(true)
        } else if (!adminSettings.accessibilityBypassTampered) {
            updateAccessibilityBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.adbBypassTampered) {
        if (adminSettings.adbBypassTampered && !adbBypassTamperLogged) {
            recordAction(
                "ADB_BYPASS_TAMPER_DETECTED",
                "ADB bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateAdbBypassTamperLogged(true)
        } else if (!adminSettings.adbBypassTampered) {
            updateAdbBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.clipboardBypassTampered) {
        if (adminSettings.clipboardBypassTampered && !clipboardBypassTamperLogged) {
            recordAction(
                "CLIPBOARD_BYPASS_TAMPER_DETECTED",
                "Clipboard bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateClipboardBypassTamperLogged(true)
        } else if (!adminSettings.clipboardBypassTampered) {
            updateClipboardBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.overlayBypassTampered) {
        if (adminSettings.overlayBypassTampered && !overlayBypassTamperLogged) {
            recordAction(
                "OVERLAY_BYPASS_TAMPER_DETECTED",
                "Overlay bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateOverlayBypassTamperLogged(true)
        } else if (!adminSettings.overlayBypassTampered) {
            updateOverlayBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.geofenceBypassTampered) {
        if (adminSettings.geofenceBypassTampered && !geofenceBypassTamperLogged) {
            recordAction(
                "GEOFENCE_BYPASS_TAMPER_DETECTED",
                "Geofence bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateGeofenceBypassTamperLogged(true)
        } else if (!adminSettings.geofenceBypassTampered) {
            updateGeofenceBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.fakeLocationBypassTampered) {
        if (adminSettings.fakeLocationBypassTampered && !fakeLocationBypassTamperLogged) {
            recordAction(
                "FAKE_LOCATION_BYPASS_TAMPER_DETECTED",
                "Fake-location bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateFakeLocationBypassTamperLogged(true)
        } else if (!adminSettings.fakeLocationBypassTampered) {
            updateFakeLocationBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.deviceTimeBypassTampered) {
        if (adminSettings.deviceTimeBypassTampered && !deviceTimeBypassTamperLogged) {
            recordAction(
                "DEVICE_TIME_BYPASS_TAMPER_DETECTED",
                "Device Time bypass seal mismatch; bypass disabled automatically",
                DiagnosticEventLevel.SECURITY
            )
            updateDeviceTimeBypassTamperLogged(true)
        } else if (!adminSettings.deviceTimeBypassTampered) {
            updateDeviceTimeBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.appSwitchBypassTampered) {
        if (adminSettings.appSwitchBypassTampered && !appSwitchBypassTamperLogged) {
            recordAction(
                "APP_SWITCH_BYPASS_TAMPER_DETECTED",
                "App Switch bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateAppSwitchBypassTamperLogged(true)
        } else if (!adminSettings.appSwitchBypassTampered) {
            updateAppSwitchBypassTamperLogged(false)
        }
    }

    LaunchedEffect(adminSettings.rootBypassTampered) {
        if (adminSettings.rootBypassTampered && !rootBypassTamperLogged) {
            recordAction(
                "ROOT_BYPASS_TAMPER_DETECTED",
                "Root bypass seal mismatch; bypass dinonaktifkan otomatis",
                DiagnosticEventLevel.SECURITY
            )
            updateRootBypassTamperLogged(true)
        } else if (!adminSettings.rootBypassTampered) {
            updateRootBypassTamperLogged(false)
        }
    }
}

@Composable
private fun RuntimeHostActivityLifecycleEffect(
    context: Context,
    componentActivity: ComponentActivity,
    coroutineScope: CoroutineScope,
    examAlarmController: ExamAlarmController,
    examGuardArmed: Boolean,
    geofenceEnabled: Boolean,
    clipboardBypassState: ClipboardBypassState,
    bypassClipboard: Boolean,
    appSwitchRuntimeMonitoringActive: Boolean,
    appSwitchSuppressionReason: AppSwitchSuppressionReason?,
    appSwitchSuppressedUntilElapsedMs: Long?,
    accessibilityGuardEnabledState: MutableState<Boolean>,
    accessibilityGuardFallbackActiveState: MutableState<Boolean>,
    accessibilityGuardLastReasonState: MutableState<String?>,
    accessibilityGuardLastForeignPackageState: MutableState<String?>,
    accessibilityGuardLastEventTypeState: MutableState<String?>,
    accessibilityGuardLastDetectedAtState: MutableState<String?>,
    accessibilityGuardAlarmSeverityState: MutableState<String>,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    adminUiState: ExamRuntimeAdminUiState,
    currentAppSwitchSuppressionReason: () -> AppSwitchSuppressionReason?,
    currentAppSwitchEventDetails: (AppSwitchSignal) -> String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    recordAppSwitchEvent: (String, AppSwitchSignal, DiagnosticEventLevel) -> Unit,
    armClipboardResumeCheck: (String) -> Unit,
    refreshReverseEngineeringStatus: () -> Unit,
    refreshKeyboardSecurity: (Boolean) -> Unit,
    refreshBluetoothSecurity: (Boolean) -> Unit,
    refreshDeviceIntegritySecurity: (Boolean) -> Unit,
    refreshDeviceTimeSecurity: (String) -> Unit,
    refreshGeofenceStatus: suspend (Boolean, String, Boolean) -> Unit,
    confirmClipboardViolation: (
        ClipboardSnapshot,
        ClipboardChangeDecision,
        String,
        Boolean,
        String?
    ) -> Unit,
    diagnosticTimestamp: () -> String
) {
    val clipboardMainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(
        componentActivity,
        securityUiState.pendingForcedExitViolation.value,
        examGuardArmed,
        geofenceEnabled,
        clipboardBypassState,
        bypassClipboard,
        appSwitchRuntimeMonitoringActive,
        appSwitchSuppressionReason,
        appSwitchSuppressedUntilElapsedMs,
        accessibilityGuardFallbackActiveState.value,
        accessibilityGuardEnabledState.value
    ) {
        val hostActivity = componentActivity
        val lifecycleCallbacks = object : EmptyActivityLifecycleCallbacks() {
            override fun onActivityStopped(activity: Activity) {
                if (
                    activity === hostActivity &&
                    examGuardArmed &&
                    !appSwitchRuntimeMonitoringActive &&
                    clipboardBypassState != ClipboardBypassState.Active &&
                    !bypassClipboard
                ) {
                    armClipboardResumeCheck("activity_stopped")
                }
                if (
                    activity === hostActivity &&
                    appSwitchRuntimeMonitoringActive &&
                    currentAppSwitchSuppressionReason() == null &&
                    !accessibilityGuardFallbackActiveState.value
                ) {
                    adminUiState.appSwitchLifecycleResumePending.value = true
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity !== hostActivity) {
                    return
                }

                refreshReverseEngineeringStatus()
                refreshKeyboardSecurity(true)
                refreshBluetoothSecurity(true)
                refreshDeviceIntegritySecurity(true)
                refreshDeviceTimeSecurity("activity_resumed")
                coroutineScope.launch {
                    refreshGeofenceStatus(false, "activity_resumed", true)
                }

                if (accessibilityGuardFallbackActiveState.value) {
                    val guardSnapshot = AccessibilityExamGuardStore.snapshot(context)
                    accessibilityGuardEnabledState.value = guardSnapshot.enabled
                    accessibilityGuardFallbackActiveState.value =
                        guardSnapshot.fallbackActive && guardSnapshot.armed
                    accessibilityGuardLastReasonState.value = guardSnapshot.lastReason
                    accessibilityGuardLastForeignPackageState.value = guardSnapshot.lastForeignPackage
                    accessibilityGuardLastEventTypeState.value = guardSnapshot.lastEventType
                    accessibilityGuardLastDetectedAtState.value = guardSnapshot.lastDetectedAt
                    accessibilityGuardAlarmSeverityState.value = guardSnapshot.alarmSeverity.name

                    val currentViolationCount = securityUiState.forcedExitViolationCount.intValue
                    val guardViolation = guardSnapshot.toRuntimeViolationIfNewer(
                        currentViolationCount = currentViolationCount,
                        source = "activity_resumed"
                    )
                    if (guardViolation != null || !guardSnapshot.enabled) {
                        val violation = guardViolation ?: AccessibilityGuardRuntimeViolation(
                            foreignPackage = "accessibility_service_disabled",
                            eventType = "service_state_changed",
                            detectedAt = diagnosticTimestamp(),
                            violationCount = currentViolationCount + 1,
                            severity = alarmSeverityForAppSwitchViolationCount(currentViolationCount + 1),
                            reason = ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED,
                            source = "service_disabled"
                        )
                        securityUiState.forcedExitViolationCount.intValue = maxOf(
                            currentViolationCount,
                            violation.violationCount.coerceAtLeast(1)
                        )
                        securityUiState.pendingForcedExitViolation.value = true
                        securityUiState.showForcedExitAlarm.value = true
                        accessibilityGuardLastReasonState.value = violation.reason
                        accessibilityGuardLastForeignPackageState.value = violation.foreignPackage
                        accessibilityGuardLastEventTypeState.value = violation.eventType
                        accessibilityGuardLastDetectedAtState.value = violation.detectedAt
                        accessibilityGuardAlarmSeverityState.value = violation.severity.name
                        adminUiState.lastAppSwitchTrigger.value =
                            AppSwitchSignal.AccessibilityGuard.diagnosticLabel()
                        adminUiState.lastAppSwitchAt.value =
                            violation.detectedAt ?: diagnosticTimestamp()
                        val details = buildAccessibilityGuardViolationDetails(
                            currentAppSwitchEventDetails(AppSwitchSignal.AccessibilityGuard),
                            violation
                        )
                        adminUiState.lastAppSwitchContext.value = details
                        recordAction(
                            accessibilityGuardEventCodeForReason(violation.reason),
                            details,
                            DiagnosticEventLevel.SECURITY
                        )
                        recordAction(
                            "ACCESSIBILITY_GUARD_RETURN_TO_EXAM_REQUESTED",
                            "foreign_package=${violation.foreignPackage?.ifBlank { "-" } ?: "-"}",
                            DiagnosticEventLevel.INFO
                        )
                        examAlarmController.start(violation.severity)
                    }
                }

                if (
                    examGuardArmed &&
                    appSwitchRuntimeMonitoringActive &&
                    adminUiState.appSwitchLifecycleResumePending.value &&
                    !securityUiState.pendingForcedExitViolation.value &&
                    !accessibilityGuardFallbackActiveState.value
                ) {
                    recordAppSwitchEvent(
                        "APP_SWITCH_DETECTED",
                        AppSwitchSignal.LifecycleResumeFallback,
                        DiagnosticEventLevel.SECURITY
                    )
                    securityUiState.forcedExitViolationCount.intValue += 1
                    securityUiState.pendingForcedExitViolation.value = true
                }
                adminUiState.appSwitchLifecycleResumePending.value = false

                if (
                    examGuardArmed &&
                    clipboardUiState.clipboardResumeCheckPending.value &&
                    !appSwitchRuntimeMonitoringActive &&
                    clipboardBypassState != ClipboardBypassState.Active &&
                    !bypassClipboard
                ) {
                    clipboardUiState.clipboardConfirmRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.clipboardConfirmRunnable.value = null
                    clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.lastClipboardDecision.value = "resume_check_pending"
                    val preBackgroundFingerprint =
                        clipboardUiState.clipboardPreBackgroundFingerprint.value
                            ?: clipboardUiState.clipboardDecisionFingerprint.value
                    val preBackgroundSignature =
                        clipboardUiState.clipboardPreBackgroundSignature.value
                    val preBackgroundSemanticSignature =
                        clipboardUiState.clipboardPreBackgroundSemanticSignature.value
                            ?: clipboardUiState.clipboardDecisionSemanticSignature.value
                    val resumeCheckRunnable = Runnable {
                        if (
                            !examGuardArmed ||
                            clipboardBypassState == ClipboardBypassState.Active ||
                            bypassClipboard
                        ) {
                            clipboardUiState.clipboardResumeCheckPending.value = false
                            clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                            clipboardUiState.clipboardPreBackgroundSignature.value = null
                            clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                            clipboardUiState.clipboardResumeCheckRunnable.value = null
                            return@Runnable
                        }

                        val resumedSnapshot = readClipboardSnapshotLite(context)
                        if (
                            resumedSnapshot.semanticSignature == preBackgroundSemanticSignature ||
                            resumedSnapshot.decisionFingerprint == preBackgroundFingerprint
                        ) {
                            clipboardUiState.lastClipboardDecision.value =
                                if (resumedSnapshot.semanticSignature == preBackgroundSemanticSignature) {
                                    ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                                } else {
                                    ClipboardChangeDecision.IgnoredNoSubstantiveChange.diagnosticLabel()
                                }
                            clipboardUiState.clipboardDecisionFingerprint.value =
                                resumedSnapshot.decisionFingerprint
                            clipboardUiState.clipboardDecisionSemanticSignature.value =
                                resumedSnapshot.semanticSignature
                            clipboardUiState.clipboardResumeCheckPending.value = false
                            clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                            clipboardUiState.clipboardPreBackgroundSignature.value = null
                            clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                            clipboardUiState.clipboardResumeCheckRunnable.value = null
                        } else {
                            clipboardUiState.lastClipboardDecision.value = "resume_check_reconfirm_pending"
                            val firstChangedSnapshot = resumedSnapshot
                            val confirmResumedRunnable = Runnable {
                                if (
                                    !examGuardArmed ||
                                    clipboardBypassState == ClipboardBypassState.Active ||
                                    bypassClipboard
                                ) {
                                    clipboardUiState.clipboardResumeCheckPending.value = false
                                    clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                                    clipboardUiState.clipboardPreBackgroundSignature.value = null
                                    clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                                    clipboardUiState.clipboardResumeCheckRunnable.value = null
                                    return@Runnable
                                }

                                val confirmedSnapshot = readClipboardSnapshotLite(context)
                                val returnedToBaseline =
                                    confirmedSnapshot.semanticSignature == preBackgroundSemanticSignature ||
                                        confirmedSnapshot.decisionFingerprint == preBackgroundFingerprint
                                if (returnedToBaseline) {
                                    clipboardUiState.lastClipboardDecision.value =
                                        if (confirmedSnapshot.semanticSignature == preBackgroundSemanticSignature) {
                                            ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                                        } else {
                                            ClipboardChangeDecision.IgnoredReturnedToBaseline.diagnosticLabel()
                                        }
                                    clipboardUiState.clipboardDecisionFingerprint.value =
                                        confirmedSnapshot.decisionFingerprint
                                    clipboardUiState.clipboardDecisionSemanticSignature.value =
                                        confirmedSnapshot.semanticSignature
                                } else if (
                                    confirmedSnapshot.semanticSignature !=
                                        firstChangedSnapshot.semanticSignature ||
                                    confirmedSnapshot.decisionFingerprint !=
                                        firstChangedSnapshot.decisionFingerprint
                                ) {
                                    clipboardUiState.lastClipboardDecision.value =
                                        ClipboardChangeDecision.IgnoredResumeNotStable.diagnosticLabel()
                                } else {
                                    clipboardUiState.lastClipboardObservedAt.value =
                                        diagnosticTimestamp()
                                    clipboardUiState.lastClipboardObservedSignature.value =
                                        preBackgroundSignature
                                            ?: clipboardUiState.lastClipboardObservedSignature.value
                                    confirmClipboardViolation(
                                        confirmedSnapshot,
                                        ClipboardChangeDecision.ConfirmedResumeCheck,
                                        "resume_after_background",
                                        false,
                                        preBackgroundSemanticSignature
                                    )
                                }
                                clipboardUiState.clipboardResumeCheckPending.value = false
                                clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                                clipboardUiState.clipboardPreBackgroundSignature.value = null
                                clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                                clipboardUiState.clipboardResumeCheckRunnable.value = null
                            }
                            clipboardUiState.clipboardResumeCheckRunnable.value =
                                confirmResumedRunnable
                            clipboardMainHandler.postDelayed(
                                confirmResumedRunnable,
                                ClipboardResumeConfirmWindowMillis
                            )
                        }
                    }
                    clipboardUiState.clipboardResumeCheckRunnable.value = resumeCheckRunnable
                    clipboardMainHandler.postDelayed(
                        resumeCheckRunnable,
                        ClipboardResumeSettleWindowMillis
                    )
                } else if (
                    appSwitchRuntimeMonitoringActive &&
                    clipboardUiState.clipboardResumeCheckPending.value
                ) {
                    clipboardUiState.lastClipboardDecision.value =
                        ClipboardChangeDecision.IgnoredCoveredByAppSwitch.diagnosticLabel()
                    clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.clipboardResumeCheckRunnable.value = null
                    clipboardUiState.clipboardResumeCheckPending.value = false
                    clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                    clipboardUiState.clipboardPreBackgroundSignature.value = null
                    clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                } else if (
                    !examGuardArmed ||
                    clipboardBypassState == ClipboardBypassState.Active ||
                    bypassClipboard
                ) {
                    clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                    clipboardUiState.clipboardResumeCheckRunnable.value = null
                    clipboardUiState.clipboardResumeCheckPending.value = false
                    clipboardUiState.clipboardPreBackgroundFingerprint.value = null
                    clipboardUiState.clipboardPreBackgroundSignature.value = null
                    clipboardUiState.clipboardPreBackgroundSemanticSignature.value = null
                }

                if (securityUiState.pendingForcedExitViolation.value) {
                    recordAppSwitchEvent(
                        "APP_SWITCH_RESUME_AFTER_LEAVE",
                        AppSwitchSignal.ResumeAfterLeave,
                        DiagnosticEventLevel.INFO
                    )
                    securityUiState.showForcedExitAlarm.value = true
                    examAlarmController.start(
                        alarmSeverityForAppSwitchViolationCount(
                            securityUiState.forcedExitViolationCount.intValue
                        )
                    )
                }
            }
        }
        hostActivity.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        onDispose {
            hostActivity.application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }
}

@Composable
private fun RuntimePrimaryGuardEffects(
    mainActivity: MainActivity?,
    examGuardArmed: Boolean,
    overlayBypassState: OverlayBypassState,
    clipboardBypassState: ClipboardBypassState,
    bypassClipboard: Boolean,
    appSwitchRuntimeMonitoringActive: Boolean,
    appSwitchProtectionMode: AppSwitchProtectionMode,
    appSwitchLockTaskActive: Boolean,
    accessibilityGuardFallbackActive: Boolean,
    accessibilityGuardEnabled: Boolean,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    adminUiState: ExamRuntimeAdminUiState,
    examAlarmController: ExamAlarmController,
    fullScreenCustomView: View?,
    showOfflineWarningDialog: Boolean,
    showExitExamDialog: Boolean,
    pendingSection: DiagnosticSection?,
    securityIssueDialogMessage: String?,
    bugReportFeedbackMessage: String?,
    currentAppSwitchSuppressionReason: () -> AppSwitchSuppressionReason?,
    currentAppSwitchEventDetails: (AppSwitchSignal, AppSwitchSuppressionReason?) -> String,
    currentOverlayEventDetails: (OverlaySignal, String?) -> String,
    currentInternalDialogReason: () -> String?,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    recordAppSwitchEvent: (String, AppSwitchSignal, DiagnosticEventLevel) -> Unit,
    recordOverlayEvent: (String, OverlaySignal, DiagnosticEventLevel) -> Unit,
    armClipboardResumeCheck: (String) -> Unit
) {
    val overlayMainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(mainActivity, examGuardArmed, overlayBypassState) {
        val hostActivity = mainActivity
        val shieldShouldBeRequested =
            hostActivity != null &&
                examGuardArmed &&
                overlayBypassState != OverlayBypassState.Active

        securityUiState.overlayShieldRequested.value = shieldShouldBeRequested
        if (hostActivity != null) {
            val applyResult = hostActivity.setOverlayShieldMode(shieldShouldBeRequested)
            securityUiState.overlayShieldLastApplySucceeded.value = applyResult
            securityUiState.overlayShieldLastAppliedAt.value = diagnosticTimestamp()
            val eventCode = when {
                shieldShouldBeRequested && applyResult == null -> "OVERLAY_SHIELD_UNSUPPORTED"
                shieldShouldBeRequested && applyResult == false -> "OVERLAY_SHIELD_APPLY_FAILED"
                shieldShouldBeRequested -> "OVERLAY_SHIELD_APPLIED"
                else -> "OVERLAY_SHIELD_DISABLED"
            }
            recordAction(
                eventCode,
                buildString {
                    append("requested=")
                    append(if (shieldShouldBeRequested) "yes" else "no")
                    append(" | supported=")
                    append(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "yes" else "no")
                    append(" | result=")
                    append(
                        when (applyResult) {
                            null -> "unsupported"
                            true -> "success"
                            false -> "failed"
                        }
                    )
                },
                if (shieldShouldBeRequested && applyResult == false) {
                    DiagnosticEventLevel.WARNING
                } else {
                    DiagnosticEventLevel.INFO
                }
            )
        } else {
            securityUiState.overlayShieldLastApplySucceeded.value = null
            securityUiState.overlayShieldLastAppliedAt.value = null
        }

        onDispose {
            if (shieldShouldBeRequested) {
                hostActivity.setOverlayShieldMode(false)
            }
        }
    }

    DisposableEffect(
        mainActivity,
        adminUiState.examRuntimeMonitoringArmed.value,
        adminUiState.appSwitchSuppressionReason.value,
        adminUiState.appSwitchSuppressedUntilElapsedMs.value,
        appSwitchRuntimeMonitoringActive,
        appSwitchProtectionMode,
        appSwitchLockTaskActive,
        accessibilityGuardFallbackActive,
        accessibilityGuardEnabled
    ) {
        if (mainActivity == null || !appSwitchRuntimeMonitoringActive) {
            mainActivity?.setOnUserLeaveExamHandler(null)
            onDispose { mainActivity?.setOnUserLeaveExamHandler(null) }
        } else {
            mainActivity.setOnUserLeaveExamHandler {
                val suppressionReason = currentAppSwitchSuppressionReason()
                if (suppressionReason != null) {
                    recordAction(
                        "APP_SWITCH_MONITOR_SUPPRESSED",
                        currentAppSwitchEventDetails(
                            AppSwitchSignal.SuppressedInternalFlow,
                            suppressionReason
                        ),
                        DiagnosticEventLevel.INFO
                    )
                } else {
                    if (accessibilityGuardFallbackActive && accessibilityGuardEnabled) {
                        recordAction(
                            "APP_SWITCH_MONITOR_SUPPRESSED",
                            currentAppSwitchEventDetails(
                                AppSwitchSignal.SuppressedInternalFlow,
                                null
                            ) + " | reason=accessibility_guard_primary",
                            DiagnosticEventLevel.INFO
                        )
                        adminUiState.appSwitchLifecycleResumePending.value = false
                        return@setOnUserLeaveExamHandler
                    }
                    if (
                        examGuardArmed &&
                        !appSwitchRuntimeMonitoringActive &&
                        clipboardBypassState != ClipboardBypassState.Active &&
                        !bypassClipboard
                    ) {
                        armClipboardResumeCheck("user_leave_hint")
                    }
                    adminUiState.appSwitchLifecycleResumePending.value = false
                    recordAppSwitchEvent(
                        "APP_SWITCH_DETECTED",
                        AppSwitchSignal.UserLeaveHint,
                        DiagnosticEventLevel.SECURITY
                    )
                    securityUiState.forcedExitViolationCount.intValue += 1
                    securityUiState.pendingForcedExitViolation.value = true
                }
            }
            onDispose {
                mainActivity.setOnUserLeaveExamHandler(null)
            }
        }
    }

    DisposableEffect(
        mainActivity,
        examGuardArmed,
        overlayBypassState,
        appSwitchRuntimeMonitoringActive,
        showOfflineWarningDialog,
        securityUiState.showForcedExitAlarm.value,
        securityUiState.showKeyboardViolationDialog.value,
        securityUiState.showOverlayViolationDialog.value,
        securityUiState.showBluetoothViolationDialog.value,
        clipboardUiState.showClipboardViolationDialog.value,
        showExitExamDialog,
        pendingSection,
        securityIssueDialogMessage,
        bugReportFeedbackMessage,
        fullScreenCustomView
    ) {
        val hostActivity = mainActivity
        if (hostActivity == null || !examGuardArmed || overlayBypassState == OverlayBypassState.Active) {
            securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
            securityUiState.overlayFocusLossConfirmRunnable.value = null
            securityUiState.overlayWindowFocusLossPending.value = false
            securityUiState.overlayWindowHasFocus.value = true
            hostActivity?.setOnExamWindowFocusChangedHandler(null)
            onDispose {
                hostActivity?.setOnExamWindowFocusChangedHandler(null)
            }
        } else {
            hostActivity.setOnExamWindowFocusChangedHandler { hasFocus ->
                securityUiState.overlayWindowHasFocus.value = hasFocus
                if (hasFocus) {
                    securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
                    securityUiState.overlayFocusLossConfirmRunnable.value = null
                    securityUiState.overlayWindowFocusLossPending.value = false
                    return@setOnExamWindowFocusChangedHandler
                }

                if (!examGuardArmed || securityUiState.showOverlayViolationDialog.value) {
                    return@setOnExamWindowFocusChangedHandler
                }

                val internalDialogReason = currentInternalDialogReason()
                val suppressionReason = currentAppSwitchSuppressionReason()
                when {
                    internalDialogReason != null -> {
                        recordAction(
                            "OVERLAY_MONITOR_SUPPRESSED",
                            currentOverlayEventDetails(
                                OverlaySignal.WindowFocusLoss,
                                "reason=internal_dialog:$internalDialogReason"
                            ),
                            DiagnosticEventLevel.INFO
                        )
                    }
                    suppressionReason != null -> {
                        recordAction(
                            "OVERLAY_MONITOR_SUPPRESSED",
                            currentOverlayEventDetails(
                                OverlaySignal.WindowFocusLoss,
                                "reason=app_switch_suppression:${suppressionReason.diagnosticLabel()}"
                            ),
                            DiagnosticEventLevel.INFO
                        )
                    }
                    fullScreenCustomView != null -> {
                        recordAction(
                            "OVERLAY_MONITOR_SUPPRESSED",
                            currentOverlayEventDetails(
                                OverlaySignal.WindowFocusLoss,
                                "reason=fullscreen_custom_view"
                            ),
                            DiagnosticEventLevel.INFO
                        )
                    }
                    else -> {
                        securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
                        securityUiState.overlayWindowFocusLossPending.value = true
                        val confirmRunnable = Runnable {
                            securityUiState.overlayFocusLossConfirmRunnable.value = null
                            if (
                                !examGuardArmed ||
                                overlayBypassState == OverlayBypassState.Active ||
                                securityUiState.overlayWindowHasFocus.value ||
                                securityUiState.showOverlayViolationDialog.value
                            ) {
                                securityUiState.overlayWindowFocusLossPending.value = false
                                return@Runnable
                            }

                            val confirmInternalDialogReason = currentInternalDialogReason()
                            if (confirmInternalDialogReason != null) {
                                securityUiState.overlayWindowFocusLossPending.value = false
                                recordAction(
                                    "OVERLAY_MONITOR_SUPPRESSED",
                                    currentOverlayEventDetails(
                                        OverlaySignal.WindowFocusLoss,
                                        "reason=internal_dialog:$confirmInternalDialogReason"
                                    ),
                                    DiagnosticEventLevel.INFO
                                )
                                return@Runnable
                            }

                            val coveredByAppSwitch =
                                appSwitchRuntimeMonitoringActive &&
                                    (
                                        securityUiState.pendingForcedExitViolation.value ||
                                            adminUiState.appSwitchLifecycleResumePending.value
                                        )
                            if (coveredByAppSwitch) {
                                securityUiState.overlayWindowFocusLossPending.value = false
                                recordAction(
                                    "OVERLAY_MONITOR_SUPPRESSED",
                                    currentOverlayEventDetails(
                                        OverlaySignal.WindowFocusLoss,
                                        "reason=covered_by_app_switch"
                                    ),
                                    DiagnosticEventLevel.INFO
                                )
                                return@Runnable
                            }

                            securityUiState.overlayWindowFocusLossPending.value = false
                            recordOverlayEvent(
                                "OVERLAY_WINDOW_FOCUS_LOSS",
                                OverlaySignal.WindowFocusLoss,
                                DiagnosticEventLevel.SECURITY
                            )
                            securityUiState.overlayViolationCount.intValue += 1
                            securityUiState.showOverlayViolationDialog.value = true
                            examAlarmController.start()
                        }
                        securityUiState.overlayFocusLossConfirmRunnable.value = confirmRunnable
                        overlayMainHandler.postDelayed(
                            confirmRunnable,
                            OverlayFocusLossConfirmWindowMillis
                        )
                    }
                }
            }
            onDispose {
                securityUiState.overlayFocusLossConfirmRunnable.value?.let(overlayMainHandler::removeCallbacks)
                securityUiState.overlayFocusLossConfirmRunnable.value = null
                securityUiState.overlayWindowFocusLossPending.value = false
                hostActivity.setOnExamWindowFocusChangedHandler(null)
            }
        }
    }
}

@Composable
private fun RuntimeLocationAndClipboardEffects(
    context: Context,
    deviceTimeBaseline: DeviceTimeBaseline,
    deviceTimeBypassState: DeviceTimeBypassState,
    geofenceConfigParseResult: GeofenceConfigParseResult,
    geofenceEnabled: Boolean,
    bypassGeofence: Boolean,
    bypassFakeLocation: Boolean,
    examGuardArmed: Boolean,
    bypassClipboard: Boolean,
    clipboardBypassState: ClipboardBypassState,
    bypassBluetooth: Boolean,
    flowUiState: ExamRuntimeFlowUiState,
    securityUiState: ExamRuntimeSecurityUiState,
    clipboardUiState: ExamRuntimeClipboardUiState,
    clipboardMainHandler: Handler,
    refreshDeviceTimeSecurity: (String, Boolean) -> DeviceTimeSecurityStatus,
    refreshGeofenceStatus: suspend (Boolean, String, Boolean) -> Unit,
    confirmClipboardViolation: (
        ClipboardSnapshot,
        ClipboardChangeDecision,
        String,
        Boolean,
        String?
    ) -> Unit,
    examAlarmController: ExamAlarmController,
    diagnosticTimestamp: () -> String
) {
    val examSessionStarted = flowUiState.examSessionStarted.value

    LaunchedEffect(deviceTimeBaseline, deviceTimeBypassState) {
        refreshDeviceTimeSecurity("runtime_initial", false)
    }

    LaunchedEffect(examSessionStarted, geofenceConfigParseResult, bypassGeofence, bypassFakeLocation) {
        val geofenceMonitoringActive = geofenceEnabled && !bypassGeofence
        val fakeLocationMonitoringActive = !bypassFakeLocation
        if (!examSessionStarted || (!geofenceMonitoringActive && !fakeLocationMonitoringActive)) {
            flowUiState.geofenceRuntimeEpisodeKey.value = null
            flowUiState.fakeLocationRuntimeEpisodeKey.value = null
            if (!examSessionStarted) {
                flowUiState.showGeofenceViolationDialog.value = false
                flowUiState.showFakeLocationViolationDialog.value = false
            }
            if (!geofenceMonitoringActive) {
                flowUiState.geofenceStartValidationInFlight.value = false
                flowUiState.pendingStartExamAfterLocationPermission.value = false
                flowUiState.retryStartExamAfterLocationPermissionGrant.value = false
                flowUiState.showGeofenceViolationDialog.value = false
            }
            if (!fakeLocationMonitoringActive) {
                flowUiState.showFakeLocationViolationDialog.value = false
            }
            return@LaunchedEffect
        }

        while (flowUiState.examSessionStarted.value && (geofenceMonitoringActive || fakeLocationMonitoringActive)) {
            delay(GeofenceRuntimeRecheckIntervalMillis)
            refreshGeofenceStatus(false, "periodic_recheck", true)
        }
    }

    DisposableEffect(context, examGuardArmed, bypassClipboard, clipboardBypassState) {
        if (!examGuardArmed || clipboardBypassState == ClipboardBypassState.Active || bypassClipboard) {
            clipboardUiState.clipboardResumeCheckRunnable.value?.let(clipboardMainHandler::removeCallbacks)
            clipboardUiState.clipboardResumeCheckRunnable.value = null
            clipboardUiState.clipboardResumeCheckPending.value = false
            clipboardUiState.clipboardPreBackgroundFingerprint.value = null
            clipboardUiState.clipboardPreBackgroundSignature.value = null
            onDispose { }
        } else {
            val clipboardManager = context.getSystemService(ClipboardManager::class.java)
            val initialSnapshot = readClipboardSnapshotLite(context)
            clipboardUiState.clipboardDecisionFingerprint.value = initialSnapshot.decisionFingerprint
            clipboardUiState.clipboardDecisionSemanticSignature.value = initialSnapshot.semanticSignature
            if (
                clipboardUiState.lastClipboardObservedAt.value == null &&
                clipboardUiState.lastClipboardConfirmedAt.value == null &&
                clipboardUiState.lastClipboardDecision.value == ClipboardChangeDecision.Idle.diagnosticLabel()
            ) {
                clipboardUiState.lastClipboardDecision.value = ClipboardChangeDecision.Idle.diagnosticLabel()
            }
            val listenerAttachedAtElapsedMs = SystemClock.elapsedRealtime()
            var pendingObservedFingerprint: String? = null
            var pendingObservedSemanticSignature: String? = null
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                val observedSnapshot = readClipboardSnapshotLite(context)
                clipboardUiState.lastClipboardObservedAt.value = diagnosticTimestamp()
                clipboardUiState.lastClipboardObservedSignature.value = null

                if (
                    observedSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value ||
                    observedSnapshot.decisionFingerprint == clipboardUiState.clipboardDecisionFingerprint.value
                ) {
                    clipboardUiState.lastClipboardDecision.value =
                        if (observedSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value) {
                            ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                        } else {
                            ClipboardChangeDecision.IgnoredNoSubstantiveChange.diagnosticLabel()
                        }
                    clipboardUiState.clipboardDecisionFingerprint.value = observedSnapshot.decisionFingerprint
                    clipboardUiState.clipboardDecisionSemanticSignature.value = observedSnapshot.semanticSignature
                    return@OnPrimaryClipChangedListener
                }

                val observedAtElapsedMs = SystemClock.elapsedRealtime()
                if (
                    observedAtElapsedMs - listenerAttachedAtElapsedMs <=
                        ClipboardListenerWarmupIgnoreMillis
                ) {
                    clipboardUiState.lastClipboardDecision.value =
                        ClipboardChangeDecision.IgnoredWarmup.diagnosticLabel()
                    clipboardUiState.clipboardDecisionFingerprint.value = observedSnapshot.decisionFingerprint
                    clipboardUiState.clipboardDecisionSemanticSignature.value = observedSnapshot.semanticSignature
                    return@OnPrimaryClipChangedListener
                }

                pendingObservedFingerprint = observedSnapshot.decisionFingerprint
                pendingObservedSemanticSignature = observedSnapshot.semanticSignature
                clipboardUiState.lastClipboardDecision.value =
                    ClipboardChangeDecision.ObservedPending.diagnosticLabel()
                clipboardUiState.clipboardConfirmRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                val confirmRunnable = Runnable {
                    val settledSnapshot = readClipboardSnapshotLite(context)
                    val expectedObservedFingerprint = pendingObservedFingerprint
                    val expectedObservedSemanticSignature = pendingObservedSemanticSignature
                    pendingObservedFingerprint = null
                    pendingObservedSemanticSignature = null
                    if (
                        settledSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value ||
                        settledSnapshot.decisionFingerprint == clipboardUiState.clipboardDecisionFingerprint.value ||
                        expectedObservedSemanticSignature == null ||
                        settledSnapshot.semanticSignature != expectedObservedSemanticSignature ||
                        expectedObservedFingerprint == null ||
                        settledSnapshot.decisionFingerprint != expectedObservedFingerprint
                    ) {
                        clipboardUiState.lastClipboardDecision.value =
                            if (settledSnapshot.semanticSignature == clipboardUiState.clipboardDecisionSemanticSignature.value) {
                                ClipboardChangeDecision.IgnoredSemanticMatch.diagnosticLabel()
                            } else {
                                ClipboardChangeDecision.IgnoredReturnedToBaseline.diagnosticLabel()
                            }
                        clipboardUiState.clipboardDecisionFingerprint.value = settledSnapshot.decisionFingerprint
                        clipboardUiState.clipboardDecisionSemanticSignature.value = settledSnapshot.semanticSignature
                        return@Runnable
                    }

                    confirmClipboardViolation(
                        settledSnapshot,
                        ClipboardChangeDecision.Confirmed,
                        "listener_settle",
                        false,
                        null
                    )
                }
                clipboardUiState.clipboardConfirmRunnable.value = confirmRunnable
                clipboardMainHandler.postDelayed(confirmRunnable, ClipboardSettleWindowMillis)
            }

            clipboardManager?.addPrimaryClipChangedListener(listener)
            onDispose {
                clipboardUiState.clipboardConfirmRunnable.value?.let(clipboardMainHandler::removeCallbacks)
                clipboardUiState.clipboardConfirmRunnable.value = null
                runCatching {
                    clipboardManager?.removePrimaryClipChangedListener(listener)
                }
            }
        }
    }

    DisposableEffect(context, examSessionStarted, securityUiState.bluetoothPermissionGranted.value, bypassBluetooth) {
        if (!examSessionStarted || !securityUiState.bluetoothPermissionGranted.value || bypassBluetooth) {
            securityUiState.bluetoothEnabled.value = isBluetoothEnabledForExam(context)
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                        return
                    }

                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    val enabledNow =
                        state == BluetoothAdapter.STATE_ON ||
                            state == BluetoothAdapter.STATE_TURNING_ON

                    securityUiState.bluetoothEnabled.value = enabledNow

                    if (enabledNow) {
                        securityUiState.bluetoothViolationCount.intValue += 1
                        securityUiState.showBluetoothViolationDialog.value = true
                        examAlarmController.start()
                    }
                }
            }
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }
}

@Composable
private fun RuntimeConnectivityEffects(
    context: Context,
    examSessionStarted: Boolean,
    networkReadinessStatus: NetworkReadinessStatus,
    baseNetworkReadiness: NetworkReadinessStatus,
    networkUiState: ExamRuntimeNetworkUiState,
    batteryStatusState: MutableState<com.example.coblaxexamlock.model.ExamBatteryStatus>,
    networkMainHandler: Handler,
    updateNetworkReadiness: (String) -> Unit,
    currentNetworkPollingIntervalMillis: () -> Long,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    currentNetworkEventDetails: (String, NetworkReadinessStatus, String?) -> String,
    clearNetworkFlapHistory: () -> Unit,
    diagnosticTimestamp: () -> String
) {
    val networkStatus = networkReadinessStatus.examStatus

    DisposableEffect(context, examSessionStarted) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val pushNetworkStatusUpdate = { source: String ->
            networkMainHandler.post {
                updateNetworkReadiness(source)
            }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                pushNetworkStatusUpdate("callback_available")
            }

            override fun onLost(network: Network) {
                pushNetworkStatusUpdate("callback_lost")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                pushNetworkStatusUpdate("callback_capabilities")
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: android.net.LinkProperties
            ) {
                pushNetworkStatusUpdate("callback_link_properties")
            }

            override fun onUnavailable() {
                pushNetworkStatusUpdate("callback_unavailable")
            }
        }

        pushNetworkStatusUpdate("initial")
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        }

        onDispose {
            networkMainHandler.removeCallbacksAndMessages(null)
            runCatching {
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        }
    }

    LaunchedEffect(context, examSessionStarted) {
        if (!examSessionStarted) {
            return@LaunchedEffect
        }
        while (true) {
            delay(currentNetworkPollingIntervalMillis())
            updateNetworkReadiness("poll")
        }
    }

    LaunchedEffect(examSessionStarted, networkStatus.isConnected, networkStatus.label) {
        if (!examSessionStarted) {
            networkUiState.offlineStartedAtElapsedMs.value = null
            networkUiState.offlineStartedAtTimestamp.value = null
            networkUiState.offlineWarningShown.value = false
            networkUiState.lastOfflineWarningElapsedMs.value = null
            networkUiState.showOfflineWarningDialog.value = false
            networkUiState.offlineWarningDurationMs.value = null
            networkUiState.showNetworkUnstableDialog.value = false
            if (networkStatus.isConnected) {
                networkUiState.lastConnectedNetworkLabel.value = networkReadinessStatus.transportLabel
            }
            return@LaunchedEffect
        }

        if (networkStatus.isConnected) {
            networkUiState.lastConnectedNetworkLabel.value = networkReadinessStatus.transportLabel
            val previousOfflineStarted = networkUiState.offlineStartedAtElapsedMs.value
            if (previousOfflineStarted != null) {
                val recoveredDurationMs =
                    (SystemClock.elapsedRealtime() - previousOfflineStarted).coerceAtLeast(0L)
                recordAction(
                    "NETWORK_OFFLINE_RECOVERED",
                    buildString {
                        append("transport=")
                        append(networkUiState.lastConnectedNetworkLabel.value?.ifBlank { "-" } ?: "-")
                        append(" | duration_ms=")
                        append(recoveredDurationMs)
                        append(" | warning_shown=")
                        append(if (networkUiState.offlineWarningShown.value) "yes" else "no")
                    },
                    DiagnosticEventLevel.INFO
                )
            }
            networkUiState.offlineStartedAtElapsedMs.value = null
            networkUiState.offlineStartedAtTimestamp.value = null
            networkUiState.offlineWarningShown.value = false
            networkUiState.lastOfflineWarningElapsedMs.value = null
            networkUiState.showOfflineWarningDialog.value = false
            networkUiState.offlineWarningDurationMs.value = null
        } else if (networkUiState.offlineStartedAtElapsedMs.value == null) {
            networkUiState.offlineStartedAtElapsedMs.value = SystemClock.elapsedRealtime()
            networkUiState.offlineStartedAtTimestamp.value = diagnosticTimestamp()
            networkUiState.offlineWarningShown.value = false
            networkUiState.lastOfflineWarningElapsedMs.value = null
            networkUiState.showOfflineWarningDialog.value = false
            networkUiState.offlineWarningDurationMs.value = null
            recordAction(
                "NETWORK_OFFLINE_STARTED",
                buildString {
                    append("last_transport=")
                    append(networkUiState.lastConnectedNetworkLabel.value?.ifBlank { "-" } ?: "-")
                    append(" | threshold_ms=")
                    append(OfflineTooLongWarningThresholdMillis)
                },
                DiagnosticEventLevel.WARNING
            )
        }
    }

    LaunchedEffect(
        examSessionStarted,
        networkStatus.isConnected,
        networkUiState.offlineStartedAtElapsedMs.value,
        networkUiState.lastOfflineWarningElapsedMs.value,
        networkUiState.showOfflineWarningDialog.value
    ) {
        val startedAt = networkUiState.offlineStartedAtElapsedMs.value ?: return@LaunchedEffect
        val previousWarningElapsed = networkUiState.lastOfflineWarningElapsedMs.value
        if (!examSessionStarted || networkStatus.isConnected || networkUiState.showOfflineWarningDialog.value) {
            return@LaunchedEffect
        }
        val referenceElapsed = previousWarningElapsed ?: startedAt
        val elapsedMs = (SystemClock.elapsedRealtime() - referenceElapsed).coerceAtLeast(0L)
        val remainingMs = OfflineTooLongWarningThresholdMillis - elapsedMs
        if (remainingMs > 0L) {
            delay(remainingMs)
        }

        if (
            examSessionStarted &&
            !networkStatus.isConnected &&
            networkUiState.offlineStartedAtElapsedMs.value == startedAt &&
            networkUiState.lastOfflineWarningElapsedMs.value == previousWarningElapsed &&
            !networkUiState.showOfflineWarningDialog.value
        ) {
            val warningElapsed = SystemClock.elapsedRealtime()
            val warningDurationMs = (warningElapsed - startedAt).coerceAtLeast(0L)
            networkUiState.offlineWarningShown.value = true
            networkUiState.lastOfflineWarningAt.value = diagnosticTimestamp()
            networkUiState.lastOfflineWarningElapsedMs.value = warningElapsed
            networkUiState.lastOfflineDurationMs.value = warningDurationMs
            networkUiState.offlineWarningDurationMs.value = warningDurationMs
            networkUiState.showOfflineWarningDialog.value = true
            recordAction(
                "NETWORK_OFFLINE_TOO_LONG_WARNING",
                buildString {
                    append("last_transport=")
                    append(networkUiState.lastConnectedNetworkLabel.value?.ifBlank { "-" } ?: "-")
                    append(" | duration_ms=")
                    append(warningDurationMs)
                    append(" | threshold_ms=")
                    append(OfflineTooLongWarningThresholdMillis)
                },
                DiagnosticEventLevel.WARNING
            )
        }
    }

    LaunchedEffect(
        networkStatus.isConnected,
        networkUiState.networkUnstableEpisodeStartedElapsedMs.value,
        networkUiState.networkUnstableLastFlapElapsedMs.value
    ) {
        val episodeStartedAt = networkUiState.networkUnstableEpisodeStartedElapsedMs.value ?: return@LaunchedEffect
        val lastFlapElapsed = networkUiState.networkUnstableLastFlapElapsedMs.value ?: return@LaunchedEffect
        if (!networkStatus.isConnected) {
            return@LaunchedEffect
        }
        val elapsedSinceLastFlap = (SystemClock.elapsedRealtime() - lastFlapElapsed).coerceAtLeast(0L)
        val remainingMs = NetworkUnstableRecoveryQuietPeriodMillis - elapsedSinceLastFlap
        if (remainingMs > 0L) {
            delay(remainingMs)
        }
        if (
            networkStatus.isConnected &&
            networkUiState.networkUnstableEpisodeStartedElapsedMs.value == episodeStartedAt &&
            networkUiState.networkUnstableLastFlapElapsedMs.value == lastFlapElapsed
        ) {
            recordAction(
                "NETWORK_UNSTABLE_EPISODE_RECOVERED",
                currentNetworkEventDetails(
                    "unstable_recovered",
                    baseNetworkReadiness,
                    "flap_count=${networkUiState.networkUnstableFlapCount.intValue}"
                ),
                DiagnosticEventLevel.INFO
            )
            networkUiState.networkUnstableEpisodeStartedElapsedMs.value = null
            networkUiState.networkUnstableEpisodeStartedAt.value = null
            networkUiState.networkUnstableWarningShown.value = false
            clearNetworkFlapHistory()
            networkUiState.networkUnstableFlapCount.intValue = 0
        }
    }

    DisposableEffect(context, examSessionStarted) {
        if (!examSessionStarted) {
            batteryStatusState.value = readExamBatteryStatus(context)
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    batteryStatusState.value = readExamBatteryStatus(intent)
                }
            }
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyIntent = ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            batteryStatusState.value = readExamBatteryStatus(stickyIntent)

            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }
}

@Composable
private fun RuntimeRecoveryAndMemoryEffects(
    pendingDirectLinkSaveLog: String?,
    pendingRecoveryEventDetails: String?,
    examSessionRecoveryNonce: Long,
    recordInfoAction: (String, String) -> Unit,
    onDirectLinkSaveLogConsumed: () -> Unit,
    onRecoveryEventConsumed: () -> Unit,
    refreshReverseEngineeringStatus: () -> Unit,
    refreshIntegrityGuard: () -> Unit,
    onSimulateRendererGone: () -> Unit,
    onTrimMemory: (Int) -> Unit
) {
    LaunchedEffect(pendingDirectLinkSaveLog) {
        val details = pendingDirectLinkSaveLog ?: return@LaunchedEffect
        recordInfoAction("DIRECT_LINK_SAVED_FROM_QR", details)
        onDirectLinkSaveLogConsumed()
    }

    LaunchedEffect(pendingRecoveryEventDetails, examSessionRecoveryNonce) {
        val details = pendingRecoveryEventDetails ?: return@LaunchedEffect
        recordInfoAction("PROCESS_DEATH_RECOVERED", details)
        onRecoveryEventConsumed()
    }

    LaunchedEffect(Unit) {
        refreshReverseEngineeringStatus()
        refreshIntegrityGuard()
    }

    val latestRendererGoneRecoveryHandler by rememberUpdatedState(newValue = onSimulateRendererGone)
    DisposableEffect(Unit) {
        ExamWebViewRecoveryTestHooks.registerRendererGoneSimulation {
            latestRendererGoneRecoveryHandler()
        }
        onDispose {
            ExamWebViewRecoveryTestHooks.registerRendererGoneSimulation(null)
        }
    }

    val latestTrimMemoryHandler by rememberUpdatedState(newValue = onTrimMemory)
    DisposableEffect(Unit) {
        val listener: (Int) -> Unit = { level -> latestTrimMemoryHandler(level) }
        MemoryPressureCoordinator.addListener(listener)
        onDispose {
            MemoryPressureCoordinator.removeListener(listener)
        }
    }
}

@Composable
private fun RuntimeAppSwitchFallbackLoggingEffect(
    examGuardArmed: Boolean,
    appSwitchStatus: AppSwitchStatus,
    screenPinningMode: ScreenPinningMode,
    appSwitchFallbackArmedLogged: Boolean,
    updateAppSwitchFallbackArmedLogged: (Boolean) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit
) {
    LaunchedEffect(
        examGuardArmed,
        appSwitchStatus.runtimeMonitoringActive,
        appSwitchStatus.fallbackGuardActive,
        appSwitchStatus.bypassed
    ) {
        if (
            examGuardArmed &&
            appSwitchStatus.runtimeMonitoringActive &&
            appSwitchStatus.fallbackGuardActive &&
            !appSwitchStatus.bypassed &&
            !appSwitchFallbackArmedLogged
        ) {
            recordAction(
                "APP_SWITCH_FALLBACK_ARMED",
                AppSwitchMonitor.eventDetails(
                    protectionMode = appSwitchStatus.protectionMode,
                    screenPinningMode = screenPinningMode,
                    lockTaskActive = appSwitchStatus.lockTaskActive
                ),
                DiagnosticEventLevel.INFO
            )
            updateAppSwitchFallbackArmedLogged(true)
        } else if (!examGuardArmed || !appSwitchStatus.fallbackGuardActive) {
            updateAppSwitchFallbackArmedLogged(false)
        }
    }
}

@Composable
private fun RuntimeDisposeCleanupEffect(
    examSessionStarted: Boolean,
    lockTaskRequestPending: Boolean,
    lockTaskBridge: ActivityLockTaskBridge,
    cleanupActiveExamWebViewInstance: () -> Unit,
    launchExitSessionClearBestEffort: () -> Unit,
    disarmAccessibilityGuard: () -> Unit,
    stopAlarm: () -> Unit
) {
    val latestExamSessionStarted by rememberUpdatedState(examSessionStarted)
    val latestLockTaskRequestPending by rememberUpdatedState(lockTaskRequestPending)
    val latestCleanupActiveExamWebViewInstance by rememberUpdatedState(cleanupActiveExamWebViewInstance)
    val latestLaunchExitSessionClearBestEffort by rememberUpdatedState(launchExitSessionClearBestEffort)
    val latestDisarmAccessibilityGuard by rememberUpdatedState(disarmAccessibilityGuard)
    val latestStopAlarm by rememberUpdatedState(stopAlarm)

    DisposableEffect(Unit) {
        onDispose {
            if (latestExamSessionStarted || latestLockTaskRequestPending) {
                lockTaskBridge.disengage()
                latestLaunchExitSessionClearBestEffort()
            }
            latestCleanupActiveExamWebViewInstance()
            latestDisarmAccessibilityGuard()
            latestStopAlarm()
        }
    }
}

@Composable
private fun RuntimeScreenPinningActivationEffect(
    mainActivity: MainActivity?,
    lockTaskBridge: ActivityLockTaskBridge,
    isIndonesian: Boolean,
    flowUiState: ExamRuntimeFlowUiState,
    adminUiState: ExamRuntimeAdminUiState,
    coroutineScope: CoroutineScope,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    clearAppSwitchSuppression: () -> Unit,
    disarmExamRuntimeMonitoring: () -> Unit,
    resetPreparationSecurityEpisodes: () -> Unit,
    prepareCleanExamWebViewSessionForStart: suspend () -> Boolean,
    finalizeExamSessionStart: (Boolean) -> Unit
) {
    val lockTaskRequestPending = flowUiState.lockTaskRequestPending.value
    val examSessionStarted = flowUiState.examSessionStarted.value

    LaunchedEffect(lockTaskRequestPending, mainActivity) {
        if (lockTaskRequestPending) {
            if (mainActivity == null) {
                recordAction(
                    ScreenPinningSignals.eventRequestFailed(),
                    ScreenPinningSignals.unavailableActivityDetail(),
                    DiagnosticEventLevel.ERROR
                )
                flowUiState.lockTaskRequestPending.value = false
                adminUiState.lockTaskStateAfterPinningRequest.value = "Unknown"
                adminUiState.screenPinningRequestOutcome.value = ScreenPinningSignals.failureOutcome()
                adminUiState.screenPinningUserActionInference.value = "Tidak dapat diproses"
                adminUiState.examSessionCancelledByPinningFailure.value = true
                clearAppSwitchSuppression()
                disarmExamRuntimeMonitoring()
                flowUiState.screenPinningMessage.value =
                    ScreenPinningEnforcer.unavailableActivityMessage(isIndonesian)
                return@LaunchedEffect
            }

            val screenPinningReport = ScreenPinningEnforcer.requestAndAwaitActivation(
                bridge = lockTaskBridge,
                isIndonesian = isIndonesian
            )
            if (screenPinningReport.dialogLikelyShown) {
                recordAction(
                    ScreenPinningSignals.eventPending(),
                    "Belum aktif setelah 1000ms",
                    DiagnosticEventLevel.WARNING
                )
                flowUiState.screenPinningMessage.value =
                    ScreenPinningEnforcer.pendingMessage(isIndonesian)
            }
            flowUiState.lockTaskRequestPending.value = false
            adminUiState.lockTaskStateAfterPinningRequest.value = screenPinningReport.afterState
            adminUiState.screenPinningDialogLikelyShown.value = screenPinningReport.dialogLikelyShown
            adminUiState.screenPinningRequestOutcome.value = screenPinningReport.outcome
            adminUiState.screenPinningUserActionInference.value = screenPinningReport.userActionInference
            adminUiState.screenPinningActivationDurationMs.value = screenPinningReport.activationDurationMs

            if (screenPinningReport.active) {
                recordAction(
                    ScreenPinningSignals.eventActive(),
                    "Lock task state ${screenPinningReport.afterState}",
                    DiagnosticEventLevel.INFO
                )
                adminUiState.examSessionCancelledByPinningFailure.value = false
                flowUiState.screenPinningMessage.value = null
                flowUiState.webViewErrorMessage.value = null
                adminUiState.exitOnSecurityIssueDialogDismiss.value = false
                resetPreparationSecurityEpisodes()
                coroutineScope.launch {
                    if (!prepareCleanExamWebViewSessionForStart()) {
                        adminUiState.examSessionCancelledByPinningFailure.value = true
                        lockTaskBridge.disengage()
                        disarmExamRuntimeMonitoring()
                        flowUiState.examSessionStarted.value = false
                        adminUiState.examSessionStartedAtElapsedMs.value = null
                        flowUiState.showBuiltInExamKeyboard.value = false
                        flowUiState.hasEditableFocus.value = false
                        clearAppSwitchSuppression()
                        return@launch
                    }
                    finalizeExamSessionStart(true)
                    delay(500)
                    clearAppSwitchSuppression()
                }
            } else {
                recordAction(
                    ScreenPinningSignals.eventFailed(),
                    "Timeout atau ditolak pengguna",
                    DiagnosticEventLevel.WARNING
                )
                adminUiState.examSessionCancelledByPinningFailure.value = true
                lockTaskBridge.disengage()
                disarmExamRuntimeMonitoring()
                flowUiState.examSessionStarted.value = false
                flowUiState.showBuiltInExamKeyboard.value = false
                flowUiState.hasEditableFocus.value = false
                clearAppSwitchSuppression()
                flowUiState.screenPinningMessage.value = screenPinningReport.guidanceMessage
            }
        } else if (!examSessionStarted) {
            flowUiState.lockTaskRequestPending.value = false
            clearAppSwitchSuppression()
            disarmExamRuntimeMonitoring()
            adminUiState.examSessionStartedAtElapsedMs.value = null
        }
    }
}

@Composable
private fun RuntimeScreenPinningMonitorEffect(
    mainActivity: MainActivity?,
    screenPinningMode: ScreenPinningMode,
    examSessionStarted: Boolean,
    examSessionStartedAtElapsedMs: Long?,
    lockTaskRequestPending: Boolean,
    accessibilityGuardFallbackActive: Boolean,
    exitOnSecurityIssueDialogDismiss: Boolean,
    lockTaskBridge: ActivityLockTaskBridge,
    isIndonesian: Boolean,
    currentScreenPinningMonitorIntervalMillis: () -> Long,
    applyFatalSecuritySignal: (FatalSecuritySignal) -> Unit
) {
    val latestExamSessionStarted by rememberUpdatedState(examSessionStarted)
    val latestExamSessionStartedAtElapsedMs by rememberUpdatedState(examSessionStartedAtElapsedMs)
    val latestLockTaskRequestPending by rememberUpdatedState(lockTaskRequestPending)
    val latestIsIndonesian by rememberUpdatedState(isIndonesian)
    val latestScreenPinningMode by rememberUpdatedState(screenPinningMode)
    val latestFatalSecurityExitPending by rememberUpdatedState(exitOnSecurityIssueDialogDismiss)

    LaunchedEffect(
        mainActivity,
        screenPinningMode,
        examSessionStarted,
        examSessionStartedAtElapsedMs,
        lockTaskRequestPending,
        accessibilityGuardFallbackActive,
        exitOnSecurityIssueDialogDismiss
    ) {
        if (
            mainActivity == null ||
            screenPinningMode != ScreenPinningMode.Enforced ||
            (!examSessionStarted && !lockTaskRequestPending) ||
            accessibilityGuardFallbackActive ||
            exitOnSecurityIssueDialogDismiss
        ) {
            return@LaunchedEffect
        }

        var firstInactiveDetectedAtElapsedMs: Long? = null
        var startupRecoveryAttempts = 0

        while (true) {
            delay(currentScreenPinningMonitorIntervalMillis())
            if (latestFatalSecurityExitPending) {
                continue
            }
            val fatalSignal = ScreenPinningMonitor.detectViolation(
                mode = latestScreenPinningMode,
                sessionStarted = latestExamSessionStarted,
                requestPending = latestLockTaskRequestPending,
                bridge = lockTaskBridge,
                isIndonesian = latestIsIndonesian
            )
            if (fatalSignal != null) {
                val nowElapsedMs = SystemClock.elapsedRealtime()
                val sessionAgeMs = latestExamSessionStartedAtElapsedMs?.let { startedAt ->
                    (nowElapsedMs - startedAt).coerceAtLeast(0L)
                }
                val withinStartupGrace =
                    sessionAgeMs != null && sessionAgeMs <= ScreenPinningMonitorStartupGraceMillis

                if (withinStartupGrace) {
                    firstInactiveDetectedAtElapsedMs = null
                    if (startupRecoveryAttempts < ScreenPinningMonitorStartupRecoveryMaxAttempts) {
                        lockTaskBridge.engage(allowLockTask = true)
                        startupRecoveryAttempts += 1
                    }
                    continue
                }

                val firstInactiveAt = firstInactiveDetectedAtElapsedMs
                if (firstInactiveAt == null) {
                    firstInactiveDetectedAtElapsedMs = nowElapsedMs
                    continue
                }
                if (nowElapsedMs - firstInactiveAt < ScreenPinningMonitorLostConfirmWindowMillis) {
                    continue
                }
                applyFatalSecuritySignal(fatalSignal)
                break
            } else {
                firstInactiveDetectedAtElapsedMs = null
            }
        }
    }
}

@Composable
private fun PreparationLocationWarmupEffect(
    context: Context,
    examSessionStarted: Boolean,
    geofenceEnabled: Boolean,
    warmLocationPolicySignature: String,
    geofenceBypassState: GeofenceBypassState,
    fakeLocationBypassState: FakeLocationBypassState,
    geofencePermissionRequestInFlight: Boolean,
    geofenceStartValidationInFlight: Boolean,
    geofenceManualRefreshInFlight: Boolean,
    webViewSessionResetInFlight: Boolean,
    locationWarmupInFlight: Boolean,
    warmupIntervalMillis: Long,
    updateLocationWarmupInFlight: (Boolean) -> Unit,
    updateReusableWarmLocationValidation: (WarmLocationValidationCache?) -> Unit,
    updateLastGeofenceRefreshAt: (String?) -> Unit,
    refreshGeofenceStatus: suspend (Boolean, String, Boolean) -> SplitLocationSecurityStatus
) {
    LaunchedEffect(
        examSessionStarted,
        geofenceEnabled,
        warmLocationPolicySignature,
        geofenceBypassState,
        fakeLocationBypassState,
        geofencePermissionRequestInFlight,
        geofenceStartValidationInFlight,
        geofenceManualRefreshInFlight,
        webViewSessionResetInFlight,
        warmupIntervalMillis
    ) {
        val geofenceMonitoringActive =
            geofenceEnabled && geofenceBypassState != GeofenceBypassState.Active
        val fakeLocationMonitoringActive =
            fakeLocationBypassState != FakeLocationBypassState.Active

        if (examSessionStarted || (!geofenceMonitoringActive && !fakeLocationMonitoringActive)) {
            updateLocationWarmupInFlight(false)
            updateReusableWarmLocationValidation(null)
            return@LaunchedEffect
        }

        while (!examSessionStarted && (geofenceMonitoringActive || fakeLocationMonitoringActive)) {
            val permissionsReady = hasLocationPermissionForWifi(context)
            val servicesEnabled = isLocationServicesEnabled(context)
            val canWarmNow =
                permissionsReady &&
                    servicesEnabled &&
                    !geofencePermissionRequestInFlight &&
                    !geofenceStartValidationInFlight &&
                    !geofenceManualRefreshInFlight &&
                    !webViewSessionResetInFlight &&
                    !locationWarmupInFlight

            if (canWarmNow) {
                updateLocationWarmupInFlight(true)
                try {
                    val warmStatus = debugMeasureExamStartSuspendWork("preparationWarmup:location_validation") {
                        refreshGeofenceStatus(true, "preparation_warmup", false)
                    }
                    val completedAtElapsedMs = SystemClock.elapsedRealtime()
                    val completedAtTimestamp = diagnosticTimestamp()
                    updateLastGeofenceRefreshAt(completedAtTimestamp)
                    val warmValidationKey = buildWarmLocationValidationKey(
                        permissionGranted = permissionsReady,
                        locationServicesEnabled = servicesEnabled,
                        policySignature = warmLocationPolicySignature
                    )
                    updateReusableWarmLocationValidation(
                        WarmLocationValidationCache(
                            result = warmStatus,
                            validationKey = warmValidationKey,
                            completedAtElapsedMs = completedAtElapsedMs,
                            completedAtTimestamp = completedAtTimestamp
                        ).takeIf {
                            it.isReusableForStart(
                                currentValidationKey = warmValidationKey,
                                nowElapsedMs = completedAtElapsedMs
                            )
                        }
                    )
                } finally {
                    updateLocationWarmupInFlight(false)
                }
            } else if (!permissionsReady || !servicesEnabled) {
                updateReusableWarmLocationValidation(null)
            }

            delay(warmupIntervalMillis)
        }
    }
}

private fun buildExamRuntimeDialogsActions(
    forcedExitViolationCount: Int,
    appSwitchStatus: AppSwitchStatus,
    keyboardViolationCount: Int,
    currentKeyboardLabel: String,
    overlayViolationCount: Int,
    overlayRiskResult: OverlayRiskResult,
    lastConnectedNetworkLabel: String?,
    offlineWarningDurationMs: Long?,
    currentOfflineDurationMs: Long?,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    geofenceViolationCount: Int,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationViolationCount: Int,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    bluetoothViolationCount: Int,
    bluetoothEnabled: Boolean,
    clipboardViolationCount: Int,
    lastClipboardConfirmedAt: String?,
    lastClipboardDecision: String,
    clipboardRuntimeStatus: ClipboardRuntimeStatus,
    alarmSessionIdentity: AlarmSessionIdentity,
    appVersionName: String,
    adminOverridesSummary: String,
    examSessionStarted: Boolean,
    examGuardArmed: Boolean,
    acknowledgeRuntimeAlarm: (
        AlarmAcknowledgeType,
        Int,
        (String) -> AlarmAcknowledgePayload,
        () -> Unit
    ) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    currentNetworkEventDetails: (String, NetworkReadinessStatus, String?) -> String,
    dismissForcedExitAlarm: () -> Unit,
    dismissKeyboardViolationDialog: () -> Unit,
    dismissOverlayViolationDialog: () -> Unit,
    dismissOfflineWarningDialog: () -> Unit,
    dismissNetworkUnstableDialog: () -> Unit,
    dismissGeofenceViolationDialog: () -> Unit,
    dismissFakeLocationViolationDialog: () -> Unit,
    openBluetoothSettings: () -> Unit,
    dismissBluetoothViolationDialog: () -> Unit,
    refreshBluetoothSecurity: () -> Unit,
    dismissClipboardViolationDialog: () -> Unit,
    dismissExitExamDialog: () -> Unit,
    confirmExitExam: () -> Unit
): ExamRuntimeDialogsActions {
    return ExamRuntimeDialogsActions(
        onAcknowledgeForcedExit = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.AppSwitch,
                forcedExitViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.AppSwitch,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = forcedExitViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        lastTrigger = appSwitchStatus.lastTrigger,
                        fallbackGuardActive = appSwitchStatus.fallbackGuardActive
                    )
                },
                dismissForcedExitAlarm
            )
        },
        onAcknowledgeKeyboard = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Keyboard,
                keyboardViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Keyboard,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = keyboardViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        keyboardLabel = currentKeyboardLabel.ifBlank { "-" }
                    )
                },
                dismissKeyboardViolationDialog
            )
        },
        onAcknowledgeOverlay = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Overlay,
                overlayViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Overlay,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = overlayViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        overlayHeuristicRisk = overlayRiskResult.heuristicRisk,
                        overlayConfirmed = overlayRiskResult.confirmedInteractionDetected,
                        overlayLastTrigger = overlayRiskResult.lastTrigger,
                        overlayLastDetectedAt = overlayRiskResult.lastDetectedAt,
                        overlayLastContext = overlayRiskResult.lastContext,
                        overlayShieldActive = overlayRiskResult.shieldStatus.active
                    )
                },
                dismissOverlayViolationDialog
            )
        },
        onAcknowledgeOffline = {
            recordAction(
                "NETWORK_OFFLINE_WARNING_ACKNOWLEDGED",
                buildString {
                    append("last_transport=")
                    append(lastConnectedNetworkLabel?.ifBlank { "-" } ?: "-")
                    append(" | duration_ms=")
                    append(offlineWarningDurationMs ?: currentOfflineDurationMs ?: 0L)
                },
                DiagnosticEventLevel.INFO
            )
            dismissOfflineWarningDialog()
        },
        onAcknowledgeNetworkUnstable = {
            recordAction(
                "NETWORK_UNSTABLE_WARNING_ACKNOWLEDGED",
                currentNetworkEventDetails(
                    "unstable_ack",
                    networkReadinessStatus,
                    "flap_count=${networkUnstableRuntimeStatus.flapCount}"
                ),
                DiagnosticEventLevel.INFO
            )
            dismissNetworkUnstableDialog()
        },
        onAcknowledgeGeofence = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Geofence,
                geofenceViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Geofence,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim(),
                        appVersion = BuildConfig.VERSION_NAME,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = geofenceViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        geofencePolicySource = geofenceRuntimeStatus.policySource.diagnosticLabel(),
                        geofenceEnabled = geofenceRuntimeStatus.evaluation.enabled,
                        geofenceShapeType = geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US),
                        geofencePolygonVertexCount = geofenceRuntimeStatus.evaluation.config?.vertices?.size,
                        geofencePolygonVerticesSummary = summarizePolygonVertices(
                            geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty()
                        ),
                        geofenceCircleCenterCount = effectiveCircleCenters(
                            geofenceRuntimeStatus.evaluation.config
                        ).size,
                        geofenceCircleCentersSummary = summarizeCircleCenters(
                            effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config)
                        ),
                        geofenceVerdict = geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel(),
                        geofenceCurrentCoordinates = geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                            formatCoordinates(it.latitude, it.longitude)
                        },
                        geofenceCenterCoordinates = geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                            formatCoordinates(it.latitude, it.longitude)
                        } ?: geofenceRuntimeStatus.evaluation.config?.let {
                            formatCoordinates(it.centerLat, it.centerLng)
                        },
                        geofenceRadiusMeters = geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                            String.format(Locale.US, "%.1f", it)
                        },
                        geofenceDistanceMeters = geofenceRuntimeStatus.evaluation.distanceMeters?.let {
                            String.format(Locale.US, "%.1f", it)
                        },
                        geofenceProvider = geofenceRuntimeStatus.evaluation.locationSnapshot?.provider,
                        geofenceAccuracyMeters = geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                            String.format(Locale.US, "%.1f", it)
                        },
                        geofenceFixQuality = geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel(),
                        geofenceFixAge = formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs),
                        geofencePermissionGranted = geofenceRuntimeStatus.evaluation.permissionGranted,
                        geofenceServicesEnabled = geofenceRuntimeStatus.evaluation.locationServicesEnabled,
                        geofencePreciseGranted = geofenceRuntimeStatus.securityStatus.preciseLocationGranted
                    )
                },
                dismissGeofenceViolationDialog
            )
        },
        onAcknowledgeFakeLocation = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.FakeLocation,
                fakeLocationViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.FakeLocation,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim(),
                        appVersion = BuildConfig.VERSION_NAME,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = fakeLocationViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        fakeLocationBypassState = fakeLocationRuntimeStatus.securityStatus.bypassState.name.lowercase(Locale.US),
                        fakeLocationVerdict = fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel(),
                        fakeLocationConfidenceTier = fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel(),
                        fakeLocationFixQuality = fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel(),
                        fakeLocationFixQualityEligible = fakeLocationRuntimeStatus.securityStatus.fixQualityEligible,
                        fakeLocationPermissionGranted = fakeLocationRuntimeStatus.securityStatus.permissionGranted,
                        fakeLocationServicesEnabled = fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled,
                        fakeLocationSnapshotAvailable = fakeLocationRuntimeStatus.securityStatus.snapshotAvailable,
                        fakeLocationMockDetected = fakeLocationRuntimeStatus.securityStatus.mockLocationDetected,
                        fakeLocationDeveloperOptionsEnabled = fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled,
                        fakeLocationSuspiciousPackages = fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" },
                        fakeLocationSignals = fakeLocationRuntimeStatus.securityStatus.supportingSignals
                            .map { it.diagnosticLabel() }
                            .joinToString()
                            .ifBlank { "-" }
                    )
                },
                dismissFakeLocationViolationDialog
            )
        },
        onOpenBluetoothSettings = openBluetoothSettings,
        onAcknowledgeBluetooth = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Bluetooth,
                bluetoothViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Bluetooth,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = bluetoothViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        bluetoothEnabled = bluetoothEnabled
                    )
                },
                {
                    dismissBluetoothViolationDialog()
                    refreshBluetoothSecurity()
                }
            )
        },
        onAcknowledgeClipboard = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Clipboard,
                clipboardViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Clipboard,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = clipboardViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        lastConfirmedAt = lastClipboardConfirmedAt,
                        lastDecision = lastClipboardDecision,
                        clipboardBaselineSemanticSignature = clipboardRuntimeStatus.baselineSemanticSignature,
                        clipboardDetectedSemanticSignature = clipboardRuntimeStatus.detectedSemanticSignature,
                        clipboardCurrentSemanticSignature = clipboardRuntimeStatus.currentSemanticSignature
                    )
                },
                dismissClipboardViolationDialog
            )
        },
        onDismissExitExam = dismissExitExamDialog,
        onConfirmExitExam = confirmExitExam
    )
}

private fun resolveExamFooterShieldStatus(
    examGuardArmed: Boolean,
    bypassKeyboardPolicy: Boolean,
    isKeyboardAllowed: Boolean,
    useBuiltInExamKeyboard: Boolean,
    bypassBluetooth: Boolean,
    bluetoothEnabled: Boolean,
    bluetoothPermissionGranted: Boolean,
    bypassAccessibility: Boolean,
    accessibilityServiceEnabled: Boolean,
    bypassAdb: Boolean,
    adbInspection: AdbInspection,
    bypassRoot: Boolean,
    rootSecurityStatus: RootSecurityStatus,
    bypassVirtualEnvironment: Boolean,
    virtualEnvironmentDetected: Boolean,
    bypassGeofence: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    bypassFakeLocation: Boolean,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    bypassDeviceTime: Boolean,
    deviceTimeSecurityStatus: DeviceTimeSecurityStatus,
    bypassOverlay: Boolean,
    overlayRiskResult: OverlayRiskResult,
    bypassAppSwitch: Boolean,
    appSwitchStatus: AppSwitchStatus,
    bypassClipboard: Boolean,
    signatureMismatchDetected: Boolean,
    securityTamperDetected: Boolean,
    forcedExitViolationCount: Int,
    keyboardViolationCount: Int,
    overlayViolationCount: Int,
    geofenceViolationCount: Int,
    fakeLocationViolationCount: Int,
    bluetoothViolationCount: Int,
    clipboardViolationCount: Int,
    showForcedExitAlarm: Boolean,
    showKeyboardViolationDialog: Boolean,
    showOverlayViolationDialog: Boolean,
    showGeofenceViolationDialog: Boolean,
    showFakeLocationViolationDialog: Boolean,
    showBluetoothViolationDialog: Boolean,
    showClipboardViolationDialog: Boolean
): ExamFooterShieldStatus {
    val activeViolationDialog =
        showForcedExitAlarm ||
            showKeyboardViolationDialog ||
            showOverlayViolationDialog ||
            showGeofenceViolationDialog ||
            showFakeLocationViolationDialog ||
            showBluetoothViolationDialog ||
            showClipboardViolationDialog
    val keyboardBlocked = !bypassKeyboardPolicy && !isKeyboardAllowed && !useBuiltInExamKeyboard
    val bluetoothBlocked =
        !bypassBluetooth &&
            (bluetoothEnabled || (requiresBluetoothExamPermission() && !bluetoothPermissionGranted))
    val blockingSecurityIssue =
        keyboardBlocked ||
            bluetoothBlocked ||
            (!bypassAccessibility && accessibilityServiceEnabled) ||
            (!bypassAdb && adbInspection.blocking) ||
            (!bypassRoot && rootSecurityStatus.blocking) ||
            (!bypassVirtualEnvironment && virtualEnvironmentDetected) ||
            (!bypassGeofence && geofenceRuntimeStatus.securityStatus.blocking) ||
            (!bypassFakeLocation && fakeLocationRuntimeStatus.securityStatus.blocking) ||
            (!bypassDeviceTime && deviceTimeSecurityStatus.blocking) ||
            (!bypassOverlay && overlayRiskResult.hasAnyRisk) ||
            (!bypassAppSwitch && appSwitchStatus.pendingViolation) ||
            signatureMismatchDetected

    if (securityTamperDetected || activeViolationDialog || blockingSecurityIssue) {
        return ExamFooterShieldStatus.Danger
    }

    val bypassActive =
        bypassKeyboardPolicy ||
            bypassBluetooth ||
            bypassAccessibility ||
            bypassAdb ||
            bypassRoot ||
            bypassVirtualEnvironment ||
            bypassGeofence ||
            bypassFakeLocation ||
            bypassDeviceTime ||
            bypassOverlay ||
            bypassAppSwitch ||
            bypassClipboard
    val historicalViolation =
        forcedExitViolationCount > 0 ||
            keyboardViolationCount > 0 ||
            overlayViolationCount > 0 ||
            geofenceViolationCount > 0 ||
            fakeLocationViolationCount > 0 ||
            bluetoothViolationCount > 0 ||
            clipboardViolationCount > 0 ||
            appSwitchStatus.hasViolations
    val nonBlockingSecurityWarning =
        bypassActive ||
            historicalViolation ||
            (!bypassFakeLocation && fakeLocationRuntimeStatus.securityStatus.warningOnly) ||
            (!bypassRoot && rootSecurityStatus.selinuxPermissive) ||
            !examGuardArmed

    return if (nonBlockingSecurityWarning) {
        ExamFooterShieldStatus.Warning
    } else {
        ExamFooterShieldStatus.Safe
    }
}

private fun buildExamRuntimeChromeState(
    examSessionStarted: Boolean,
    examDisplayName: String,
    loadingProgress: Float,
    webViewErrorMessage: String?,
    hasFullscreenCustomView: Boolean,
    useBuiltInExamKeyboard: Boolean,
    showBuiltInExamKeyboard: Boolean,
    showSideArrowControls: Boolean,
    hasEditableFocus: Boolean,
    builtInKeyboardShiftEnabled: Boolean,
    networkStatus: NetworkReadinessStatus,
    serverStatus: ExamServerFooterStatus,
    batteryStatus: com.example.coblaxexamlock.model.ExamBatteryStatus,
    shieldStatus: ExamFooterShieldStatus
): ExamRuntimeChromeState {
    return ExamRuntimeChromeState(
        examSessionStarted = examSessionStarted,
        examDisplayName = examDisplayName,
        loadingProgress = loadingProgress,
        webViewErrorMessage = webViewErrorMessage,
        hasFullscreenCustomView = hasFullscreenCustomView,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        showBuiltInExamKeyboard = showBuiltInExamKeyboard,
        showSideArrowControls = showSideArrowControls,
        hasEditableFocus = hasEditableFocus,
        builtInKeyboardShiftEnabled = builtInKeyboardShiftEnabled,
        networkStatus = networkStatus,
        serverStatus = serverStatus,
        batteryStatus = batteryStatus,
        shieldStatus = shieldStatus
    )
}

private fun buildExamRuntimeChromeActions(
    onRetryLoading: () -> Unit,
    onRefreshPage: () -> Unit,
    onGoHome: () -> Unit,
    onTextKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onArrowLeft: () -> Unit,
    onArrowRight: () -> Unit,
    onToggleSideArrowControls: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onShiftToggle: () -> Unit
): ExamRuntimeChromeActions {
    return ExamRuntimeChromeActions(
        onRetryLoading = onRetryLoading,
        onRefreshPage = onRefreshPage,
        onGoHome = onGoHome,
        onTextKey = onTextKey,
        onBackspace = onBackspace,
        onArrowLeft = onArrowLeft,
        onArrowRight = onArrowRight,
        onToggleSideArrowControls = onToggleSideArrowControls,
        onEnter = onEnter,
        onSpace = onSpace,
        onShiftToggle = onShiftToggle
    )
}

private fun buildExamRuntimeDialogsState(
    showForcedExitAlarm: Boolean,
    forcedExitViolationCount: Int,
    appSwitchStatus: AppSwitchStatus,
    showKeyboardViolationDialog: Boolean,
    keyboardViolationCount: Int,
    currentKeyboardLabel: String,
    showOverlayViolationDialog: Boolean,
    overlayViolationCount: Int,
    overlayTrigger: String?,
    showOfflineWarningDialog: Boolean,
    offlineDurationMs: Long?,
    currentOfflineDurationMs: Long?,
    uiLanguage: UiLanguage,
    showNetworkUnstableDialog: Boolean,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    showGeofenceViolationDialog: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    showFakeLocationViolationDialog: Boolean,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    showBluetoothViolationDialog: Boolean,
    bluetoothEnabled: Boolean,
    bluetoothViolationCount: Int,
    showClipboardViolationDialog: Boolean,
    clipboardViolationCount: Int,
    clipboardLastConfirmedAt: String?,
    clipboardLastDecision: String,
    showExitExamDialog: Boolean,
    exitSessionClearInFlight: Boolean
): ExamRuntimeDialogsState {
    return ExamRuntimeDialogsState(
        showForcedExitAlarm = showForcedExitAlarm,
        forcedExitViolationCount = forcedExitViolationCount,
        appSwitchStatus = appSwitchStatus,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        keyboardViolationCount = keyboardViolationCount,
        currentKeyboardLabel = currentKeyboardLabel,
        showOverlayViolationDialog = showOverlayViolationDialog,
        overlayViolationCount = overlayViolationCount,
        overlayTrigger = overlayTrigger,
        showOfflineWarningDialog = showOfflineWarningDialog,
        offlineDurationText = formatElapsedDuration(
            offlineDurationMs ?: currentOfflineDurationMs ?: OfflineTooLongWarningThresholdMillis,
            uiLanguage
        ),
        showNetworkUnstableDialog = showNetworkUnstableDialog,
        networkTransportLabel = networkReadinessStatus.transportLabel,
        networkUnstableFlapCount = networkUnstableRuntimeStatus.flapCount,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        geofenceStatus = geofenceRuntimeStatus.securityStatus,
        geofenceViolationCount = geofenceRuntimeStatus.violationCount,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        fakeLocationStatus = fakeLocationRuntimeStatus.securityStatus,
        fakeLocationViolationCount = fakeLocationRuntimeStatus.violationCount,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothViolationCount = bluetoothViolationCount,
        showClipboardViolationDialog = showClipboardViolationDialog,
        clipboardViolationCount = clipboardViolationCount,
        clipboardLastConfirmedAt = clipboardLastConfirmedAt,
        clipboardLastDecision = clipboardLastDecision,
        showExitExamDialog = showExitExamDialog,
        exitSessionClearInFlight = exitSessionClearInFlight
    )
}

private fun buildPreparationScreenActions(
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
    onBackHome: () -> Unit
): PreparationScreenActions {
    return PreparationScreenActions(
        onChooseKeyboard = onChooseKeyboard,
        onOpenKeyboardSettings = onOpenKeyboardSettings,
        onGrantBluetoothPermission = onGrantBluetoothPermission,
        onOpenBluetoothSettings = onOpenBluetoothSettings,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onOpenOverlayAccessibilitySettings = onOpenOverlayAccessibilitySettings,
        onOpenDeveloperOptionsSettings = onOpenDeveloperOptionsSettings,
        onRequestLocationPermission = onRequestLocationPermission,
        onOpenLocationServicesSettings = onOpenLocationServicesSettings,
        onRefreshGeofenceLocation = onRefreshGeofenceLocation,
        onOpenGeofenceMapViewer = onOpenGeofenceMapViewer,
        onOpenInternetSettings = onOpenInternetSettings,
        onOpenWifiSettings = onOpenWifiSettings,
        onOpenCellularSettings = onOpenCellularSettings,
        onOpenAirplaneModeSettings = onOpenAirplaneModeSettings,
        onRefreshNetworkStatus = onRefreshNetworkStatus,
        onOpenDateTimeSettings = onOpenDateTimeSettings,
        onOpenFakeLocationDeveloperOptionsSettings = onOpenFakeLocationDeveloperOptionsSettings,
        onOpenScreenPinningSettings = onOpenScreenPinningSettings,
        onOpenOverlaySettings = onOpenOverlaySettings,
        onReinstallOfficialApk = onReinstallOfficialApk,
        onRefreshStatus = onRefreshStatus,
        onRefreshAllSecurityChecks = onRefreshAllSecurityChecks,
        onRequestSectionReport = onRequestSectionReport,
        onStartExam = onStartExam,
        onBackHome = onBackHome
    )
}

private fun buildDeviceTimeEventDetails(
    trigger: String,
    status: DeviceTimeSecurityStatus
): String {
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

private fun deviceTimeBlockedTitle(uiLanguage: UiLanguage): String {
    return localized(
        uiLanguage,
        "Device Time Check Required",
        "Pemeriksaan Waktu Perangkat Diperlukan"
    )
}

private fun deviceTimeBlockedMessage(
    uiLanguage: UiLanguage,
    status: DeviceTimeSecurityStatus
): String {
    return when {
        status.bypassState == DeviceTimeBypassState.Tampered -> localized(
            uiLanguage,
            "Device Time bypass storage was tampered with. Device Time enforcement remains active.",
            "Tamper terdeteksi pada storage bypass Waktu Perangkat. Enforcement Waktu Perangkat tetap aktif."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled -> localized(
            uiLanguage,
            "Turn on automatic date & time before starting the exam.",
            "Aktifkan tanggal & waktu otomatis sebelum memulai ujian."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled -> localized(
            uiLanguage,
            "Turn on automatic time zone before starting the exam.",
            "Aktifkan zona waktu otomatis sebelum memulai ujian."
        )
        status.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected -> localized(
            uiLanguage,
            "A suspicious clock change was detected. Turn automatic date & time back on, then try again.",
            "Terdeteksi perubahan jam yang mencurigakan. Aktifkan kembali tanggal & waktu otomatis, lalu coba lagi."
        )
        else -> localized(
            uiLanguage,
            "Device time could not be trusted. Check the date & time settings, then try again.",
            "Waktu perangkat tidak dapat dipercaya. Periksa pengaturan tanggal & waktu, lalu coba lagi."
        )
    }
}

private fun scheduleBlockedMessage(
    uiLanguage: UiLanguage,
    payload: ExamQrPayload,
    validationResult: ExamScheduleValidationResult
): String {
    return when (validationResult) {
        ExamScheduleValidationResult.NotStarted -> localized(
            uiLanguage,
            "This exam has not started yet. It becomes active at ${payload.startDateTime}.",
            "Ujian ini belum dimulai. Ujian baru aktif pada ${payload.startDateTime}."
        )
        ExamScheduleValidationResult.Finished -> localized(
            uiLanguage,
            "This exam is no longer valid. It ended at ${payload.endDateTime}.",
            "Ujian ini sudah tidak berlaku. Ujian berakhir pada ${payload.endDateTime}."
        )
        ExamScheduleValidationResult.InvalidSchedule -> localized(
            uiLanguage,
            "This exam QR has an invalid schedule. Check the start and end time in Custom QR.",
            "QR ujian ini memiliki jadwal yang tidak valid. Periksa waktu mulai dan selesai di Custom QR."
        )
        ExamScheduleValidationResult.TimeSpoofDetected -> localized(
            uiLanguage,
            "Device time could not be trusted. Enable automatic date, time, and time zone, then try again.",
            "Waktu perangkat tidak dapat dipercaya. Aktifkan tanggal, waktu, dan zona waktu otomatis, lalu coba lagi."
        )
        ExamScheduleValidationResult.Valid -> localized(
            uiLanguage,
            "The exam schedule is valid.",
            "Jadwal ujian valid."
        )
    }
}

@Composable
private fun ExamRuntimeSessionMainContent(
    examSessionStarted: Boolean,
    showGeofenceMapViewer: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    geofenceManualRefreshInFlight: Boolean,
    onDismissGeofenceMapViewer: () -> Unit,
    onRefreshGeofenceMapViewer: () -> Unit,
    preparationState: PreparationScreenState,
    preparationActions: PreparationScreenActions,
    runtimeChromeState: ExamRuntimeChromeState,
    runtimeChromeActions: ExamRuntimeChromeActions,
    payload: ExamQrPayload,
    bypassOverlay: Boolean,
    examAlarmController: ExamAlarmController,
    participantCaptureBridge: ExamParticipantCaptureBridge,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    keyboardBridge: ExamKeyboardBridge,
    useBuiltInExamKeyboard: Boolean,
    effectiveExamUserAgent: String,
    fullScreenContainer: FrameLayout,
    fullScreenCustomView: View?,
    nativeExamFullscreenActive: Boolean,
    onRefreshMapViewerActionLogged: () -> Unit,
    onOverlayObscuredTouch: () -> Unit,
    onShowBuiltInExamKeyboardChange: (Boolean) -> Unit,
    onWebViewInstanceChange: (SecureExamWebView?) -> Unit,
    onHideSystemKeyboard: () -> Unit,
    onWebViewLoadStart: (String?) -> Unit,
    onWebViewLoadFinish: (WebView?, String?) -> Unit,
    onWebViewLoadError: (String) -> Unit,
    onWebViewHttpError: (Int?) -> Unit,
    onWebViewRenderProcessGone: (SecureExamWebView?, Boolean, Int?) -> Boolean,
    onLoadingProgressChange: (Float) -> Unit,
    onWebViewErrorMessageChange: (String?) -> Unit,
    onShowCustomView: (View?, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!examSessionStarted) {
        ExamPreparationScene(
            showGeofenceMapViewer = showGeofenceMapViewer,
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            isRefreshingGeofence = geofenceManualRefreshInFlight,
            onDismissGeofenceMapViewer = onDismissGeofenceMapViewer,
            onRefreshGeofenceLocation = {
                onRefreshGeofenceMapViewer()
                onRefreshMapViewerActionLogged()
            },
            preparationState = preparationState,
            preparationActions = preparationActions,
            modifier = modifier
        )
        return
    }

    ExamRuntimeChrome(
        state = runtimeChromeState,
        actions = runtimeChromeActions,
        modifier = modifier,
        webViewLayer = {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    SecureExamWebView(
                        context = context,
                        onObscuredTouchDetected = {
                            if (!bypassOverlay) {
                                onOverlayObscuredTouch()
                            }
                        }
                    ).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = true
                        isFocusableInTouchMode = true
                        onWebViewInstanceChange(this)
                        setBackgroundColor(LockBackground.toArgb())
                        isLongClickable = false
                        isHapticFeedbackEnabled = false
                        setOnLongClickListener { true }
                        attachExamParticipantCaptureBridge(participantCaptureBridge)
                        attachExamNativeFullscreenBridge(nativeFullscreenBridge)
                        installExamNativeFullscreenDocumentStartScriptIfSupported()
                        attachExamKeyboardBridge(
                            bridge = keyboardBridge,
                            onHideSystemKeyboard = if (useBuiltInExamKeyboard) onHideSystemKeyboard else null
                        )
                        if (!useBuiltInExamKeyboard) {
                            onShowBuiltInExamKeyboardChange(false)
                            post {
                                requestFocus(View.FOCUS_DOWN)
                                requestFocus()
                            }
                        }
                        applyExamWebViewSettings(effectiveExamUserAgent)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                onLoadingProgressChange(newProgress / 100f)
                            }

                            override fun onShowCustomView(
                                view: View?,
                                callback: CustomViewCallback?
                            ) {
                                if (view == null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                onShowCustomView(view, callback)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onShowCustomView(
                                view: View?,
                                requestedOrientation: Int,
                                callback: CustomViewCallback?
                            ) {
                                onShowCustomView(view, callback)
                            }

                            override fun onHideCustomView() {
                                onHideCustomView()
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                onWebViewLoadStart(url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                onWebViewLoadFinish(view, url)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    onWebViewLoadError(
                                        error?.description?.toString() ?: "Halaman ujian gagal dimuat."
                                    )
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    onWebViewHttpError(errorResponse?.statusCode)
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                val didCrash =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && detail != null) {
                                        detail.didCrash()
                                    } else {
                                        false
                                    }
                                val rendererPriorityAtExit =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && detail != null) {
                                        detail.rendererPriorityAtExit()
                                    } else {
                                        null
                                    }
                                return onWebViewRenderProcessGone(
                                    view as? SecureExamWebView,
                                    didCrash,
                                    rendererPriorityAtExit
                                )
                            }
                        }
                        loadUrl(payload.examUrl)
                        requestedExamUrl = payload.examUrl
                    }
                },
                update = { webView ->
                    webView.attachExamParticipantCaptureBridge(participantCaptureBridge)
                    webView.attachExamNativeFullscreenBridge(nativeFullscreenBridge)
                    webView.attachExamKeyboardBridge(
                        bridge = keyboardBridge,
                        onHideSystemKeyboard = if (useBuiltInExamKeyboard) onHideSystemKeyboard else null
                    )
                    webView.evaluateJavascript(InstallExamKeyboardScript, null)
                    webView.evaluateJavascript(
                        if (runtimeChromeState.showSideArrowControls) {
                            InstallExamSideArrowControlsScript
                        } else {
                            RemoveExamSideArrowControlsScript
                        },
                        null
                    )
                    if (!useBuiltInExamKeyboard) {
                        onShowBuiltInExamKeyboardChange(false)
                        webView.post {
                            webView.requestFocus(View.FOCUS_DOWN)
                            webView.requestFocus()
                        }
                    }
                    if (webView.requestedExamUrl != payload.examUrl) {
                        webView.loadUrl(payload.examUrl)
                        webView.requestedExamUrl = payload.examUrl
                    }
                    webView.settings.userAgentString = effectiveExamUserAgent
                    webView.evaluateJavascript(ExamNativeFullscreenBridgeInstallScript, null)
                    webView.evaluateJavascript(
                        buildExamNativeFullscreenStateSyncScript(nativeExamFullscreenActive),
                        null
                    )
                    onWebViewErrorMessageChange(null)
                }
            )
        },
        fullscreenLayer = {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { fullScreenContainer },
                update = { container ->
                    val view = fullScreenCustomView
                    if (view != null && view.parent != container) {
                        (view.parent as? ViewGroup)?.removeView(view)
                        container.removeAllViews()
                        container.addView(
                            view,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun ExamRuntimeSessionRenderedUi(
    examSessionStarted: Boolean,
    showGeofenceMapViewer: Boolean,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    geofenceManualRefreshInFlight: Boolean,
    preparationState: PreparationScreenState,
    preparationActions: PreparationScreenActions,
    runtimeChromeState: ExamRuntimeChromeState,
    runtimeChromeActions: ExamRuntimeChromeActions,
    payload: ExamQrPayload,
    bypassOverlay: Boolean,
    examAlarmController: ExamAlarmController,
    participantCaptureBridge: ExamParticipantCaptureBridge,
    nativeFullscreenBridge: ExamNativeFullscreenBridge,
    keyboardBridge: ExamKeyboardBridge,
    useBuiltInExamKeyboard: Boolean,
    effectiveExamUserAgent: String,
    fullScreenContainer: FrameLayout,
    fullScreenCustomView: View?,
    nativeExamFullscreenActive: Boolean,
    runtimeDialogsState: ExamRuntimeDialogsState,
    runtimeDialogsActions: ExamRuntimeDialogsActions,
    pendingSection: DiagnosticSection?,
    uiLanguage: UiLanguage,
    screenPinningMessage: String?,
    securityIssueDialogTitle: String?,
    securityIssueDialogMessage: String?,
    bugReportFeedbackTitle: String?,
    bugReportFeedbackMessage: String?,
    onDismissGeofenceMapViewer: () -> Unit,
    onRefreshGeofenceMapViewer: () -> Unit,
    onRefreshMapViewerActionLogged: () -> Unit,
    onOverlayObscuredTouch: () -> Unit,
    onShowBuiltInExamKeyboardChange: (Boolean) -> Unit,
    onWebViewInstanceChange: (SecureExamWebView?) -> Unit,
    onHideSystemKeyboard: () -> Unit,
    onWebViewLoadStart: (String?) -> Unit,
    onWebViewLoadFinish: (WebView?, String?) -> Unit,
    onWebViewLoadError: (String) -> Unit,
    onWebViewHttpError: (Int?) -> Unit,
    onWebViewRenderProcessGone: (SecureExamWebView?, Boolean, Int?) -> Boolean,
    onLoadingProgressChange: (Float) -> Unit,
    onWebViewErrorMessageChange: (String?) -> Unit,
    onShowCustomView: (View?, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onDismissPendingSection: () -> Unit,
    onConfirmPendingSection: (DiagnosticSection) -> Unit,
    onDismissScreenPinningMessage: () -> Unit,
    onDismissSecurityIssueDialog: () -> Unit,
    onDismissBugReportFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockBackground)
    ) {
        ExamRuntimeSessionMainContent(
            examSessionStarted = examSessionStarted,
            showGeofenceMapViewer = showGeofenceMapViewer,
            geofenceRuntimeStatus = geofenceRuntimeStatus,
            geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
            onDismissGeofenceMapViewer = onDismissGeofenceMapViewer,
            onRefreshGeofenceMapViewer = onRefreshGeofenceMapViewer,
            preparationState = preparationState,
            preparationActions = preparationActions,
            runtimeChromeState = runtimeChromeState,
            runtimeChromeActions = runtimeChromeActions,
            payload = payload,
            bypassOverlay = bypassOverlay,
            examAlarmController = examAlarmController,
            participantCaptureBridge = participantCaptureBridge,
            nativeFullscreenBridge = nativeFullscreenBridge,
            keyboardBridge = keyboardBridge,
            useBuiltInExamKeyboard = useBuiltInExamKeyboard,
            effectiveExamUserAgent = effectiveExamUserAgent,
            fullScreenContainer = fullScreenContainer,
            fullScreenCustomView = fullScreenCustomView,
            nativeExamFullscreenActive = nativeExamFullscreenActive,
            onRefreshMapViewerActionLogged = onRefreshMapViewerActionLogged,
            onOverlayObscuredTouch = onOverlayObscuredTouch,
            onShowBuiltInExamKeyboardChange = onShowBuiltInExamKeyboardChange,
            onWebViewInstanceChange = onWebViewInstanceChange,
            onHideSystemKeyboard = onHideSystemKeyboard,
            onWebViewLoadStart = onWebViewLoadStart,
            onWebViewLoadFinish = onWebViewLoadFinish,
            onWebViewLoadError = onWebViewLoadError,
            onWebViewHttpError = onWebViewHttpError,
            onWebViewRenderProcessGone = onWebViewRenderProcessGone,
            onLoadingProgressChange = onLoadingProgressChange,
            onWebViewErrorMessageChange = onWebViewErrorMessageChange,
            onShowCustomView = onShowCustomView,
            onHideCustomView = onHideCustomView,
            modifier = Modifier.weight(1f)
        )

        ExamRuntimeDialogsCoordinator(
            pendingSection = pendingSection,
            uiLanguage = uiLanguage,
            runtimeDialogsState = runtimeDialogsState,
            runtimeDialogsActions = runtimeDialogsActions,
            screenPinningMessage = screenPinningMessage,
            securityIssueDialogTitle = securityIssueDialogTitle,
            securityIssueDialogMessage = securityIssueDialogMessage,
            bugReportFeedbackTitle = bugReportFeedbackTitle,
            bugReportFeedbackMessage = bugReportFeedbackMessage,
            onDismissPendingSection = onDismissPendingSection,
            onConfirmPendingSection = onConfirmPendingSection,
            onDismissScreenPinningMessage = onDismissScreenPinningMessage,
            onDismissSecurityIssueDialog = onDismissSecurityIssueDialog,
            onDismissBugReportFeedback = onDismissBugReportFeedback
        )
    }
}

private data class ExamRuntimeSessionInputs(
    val payload: ExamQrPayload,
    val adminSettings: AdminSettings,
    val pendingDirectLinkSaveLog: String?,
    val pendingRecoveryEventDetails: String?,
    val examSessionRecoveryNonce: Long,
    val deviceTimeBaselineWallClockMillis: Long,
    val deviceTimeBaselineElapsedRealtimeMillis: Long
)

private data class ExamRuntimeSessionCallbacks(
    val onDirectLinkSaveLogConsumed: () -> Unit,
    val onRecoveryEventConsumed: () -> Unit,
    val onExamSessionStartedStateChange: (Boolean) -> Unit,
    val onExit: () -> Unit
)

@Composable
internal fun ExamRuntimeSessionScreen(
    payload: ExamQrPayload,
    adminSettings: AdminSettings,
    pendingDirectLinkSaveLog: String?,
    pendingRecoveryEventDetails: String?,
    onDirectLinkSaveLogConsumed: () -> Unit,
    onRecoveryEventConsumed: () -> Unit,
    examSessionRecoveryNonce: Long,
    deviceTimeBaselineWallClockMillis: Long,
    deviceTimeBaselineElapsedRealtimeMillis: Long,
    onExamSessionStartedStateChange: (Boolean) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExamRuntimeSessionScreenImpl(
        inputs = ExamRuntimeSessionInputs(
            payload = payload,
            adminSettings = adminSettings,
            pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
            pendingRecoveryEventDetails = pendingRecoveryEventDetails,
            examSessionRecoveryNonce = examSessionRecoveryNonce,
            deviceTimeBaselineWallClockMillis = deviceTimeBaselineWallClockMillis,
            deviceTimeBaselineElapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
        ),
        callbacks = ExamRuntimeSessionCallbacks(
            onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
            onRecoveryEventConsumed = onRecoveryEventConsumed,
            onExamSessionStartedStateChange = onExamSessionStartedStateChange,
            onExit = onExit
        ),
        modifier = modifier
    )
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
@NonRestartableComposable
private fun ExamRuntimeSessionScreenImpl(
    inputs: ExamRuntimeSessionInputs,
    callbacks: ExamRuntimeSessionCallbacks,
    modifier: Modifier = Modifier
) {
    val payload = inputs.payload
    val adminSettings = inputs.adminSettings
    val pendingDirectLinkSaveLog = inputs.pendingDirectLinkSaveLog
    val pendingRecoveryEventDetails = inputs.pendingRecoveryEventDetails
    val examSessionRecoveryNonce = inputs.examSessionRecoveryNonce
    val deviceTimeBaselineWallClockMillis = inputs.deviceTimeBaselineWallClockMillis
    val deviceTimeBaselineElapsedRealtimeMillis = inputs.deviceTimeBaselineElapsedRealtimeMillis
    val onDirectLinkSaveLogConsumed = callbacks.onDirectLinkSaveLogConsumed
    val onRecoveryEventConsumed = callbacks.onRecoveryEventConsumed
    val onExamSessionStartedStateChange = callbacks.onExamSessionStartedStateChange
    val onExit = callbacks.onExit
    val context = LocalContext.current
    val activity = context as? Activity
    val componentActivity = activity as? ComponentActivity
        ?: error("ExamWebViewScreen requires a ComponentActivity host")
    val mainActivity = activity as? MainActivity
    val lockTaskBridge = remember(mainActivity) { ActivityLockTaskBridge { mainActivity } }
    val lowRamProfile = LocalLowRamProfile.current
    val uiLanguage = LocalUiLanguage.current
    val isIndonesian = uiLanguage == UiLanguage.Indonesian
    val deviceTimeBaseline = remember(
        deviceTimeBaselineWallClockMillis,
        deviceTimeBaselineElapsedRealtimeMillis
    ) {
        DeviceTimeBaseline(
            wallClockMillis = deviceTimeBaselineWallClockMillis,
            elapsedRealtimeMillis = deviceTimeBaselineElapsedRealtimeMillis
        )
    }
    val screenPinningBypassState = remember(
        adminSettings.bypassScreenPinning,
        adminSettings.screenPinningBypassTampered
    ) {
        ScreenPinningBypassResolver.stateOf(
            enabled = adminSettings.bypassScreenPinning,
            tampered = adminSettings.screenPinningBypassTampered
        )
    }
    val screenPinningMode = remember(screenPinningBypassState) {
        ScreenPinningBypassResolver.modeOf(screenPinningBypassState)
    }
    val overlayBypassState = remember(
        adminSettings.bypassOverlay,
        adminSettings.overlayBypassTampered
    ) {
        OverlayBypassResolver.stateOf(
            enabled = adminSettings.bypassOverlay,
            tampered = adminSettings.overlayBypassTampered
        )
    }
    val appSwitchBypassState = remember(
        adminSettings.bypassAppSwitch,
        adminSettings.appSwitchBypassTampered
    ) {
        AppSwitchBypassResolver.stateOf(
            enabled = adminSettings.bypassAppSwitch,
            tampered = adminSettings.appSwitchBypassTampered
        )
    }
    val accessibilityBypassState = remember(
        adminSettings.bypassAccessibility,
        adminSettings.accessibilityBypassTampered
    ) {
        AccessibilityBypassResolver.stateOf(
            enabled = adminSettings.bypassAccessibility,
            tampered = adminSettings.accessibilityBypassTampered
        )
    }
    val clipboardBypassState = remember(
        adminSettings.bypassClipboard,
        adminSettings.clipboardBypassTampered
    ) {
        ClipboardBypassResolver.stateOf(
            enabled = adminSettings.bypassClipboard,
            tampered = adminSettings.clipboardBypassTampered
        )
    }
    val adbBypassState = remember(
        adminSettings.bypassAdb,
        adminSettings.adbBypassTampered
    ) {
        AdbBypassResolver.stateOf(
            enabled = adminSettings.bypassAdb,
            tampered = adminSettings.adbBypassTampered
        )
    }
    val rootBypassState = remember(
        adminSettings.bypassRoot,
        adminSettings.rootBypassTampered
    ) {
        RootBypassResolver.stateOf(
            enabled = adminSettings.bypassRoot,
            tampered = adminSettings.rootBypassTampered
        )
    }
    val geofenceBypassState = remember(
        adminSettings.bypassGeofence,
        adminSettings.geofenceBypassTampered
    ) {
        GeofenceBypassResolver.stateOf(
            enabled = adminSettings.bypassGeofence,
            tampered = adminSettings.geofenceBypassTampered
        )
    }
    val fakeLocationBypassState = remember(
        adminSettings.bypassFakeLocation,
        adminSettings.fakeLocationBypassTampered
    ) {
        FakeLocationBypassResolver.stateOf(
            enabled = adminSettings.bypassFakeLocation,
            tampered = adminSettings.fakeLocationBypassTampered
        )
    }
    val deviceTimeBypassState = remember(
        adminSettings.bypassDeviceTime,
        adminSettings.deviceTimeBypassTampered
    ) {
        DeviceTimeBypassResolver.stateOf(
            enabled = adminSettings.bypassDeviceTime,
            tampered = adminSettings.deviceTimeBypassTampered
        )
    }
    val locationBypassState = geofenceBypassState
    val bypassScreenPinning = adminSettings.bypassScreenPinning
    val bypassBluetooth = adminSettings.bypassBluetooth
    val bypassAccessibility = accessibilityBypassState == AccessibilityBypassState.Active
    val bypassAdb = adbBypassState == AdbBypassState.Active
    val bypassRoot = rootBypassState == RootBypassState.Active
    val bypassVirtualEnvironment = adminSettings.bypassVirtualEnvironment
    val bypassKeyboardPolicy = adminSettings.bypassKeyboardPolicy
    val bypassClipboard = adminSettings.bypassClipboard
    val bypassOverlay = adminSettings.bypassOverlay
    val bypassGeofence = geofenceBypassState == GeofenceBypassState.Active
    val bypassFakeLocation = fakeLocationBypassState == FakeLocationBypassState.Active
    val bypassDeviceTime = deviceTimeBypassState == DeviceTimeBypassState.Active
    val bypassLocation = bypassGeofence
    val bypassAppSwitch = adminSettings.bypassAppSwitch
    val adminOverridesSummary = adminSettings.overrideSummary()
    val effectiveLocationPolicy = payload.locationPolicy ?: ExamQrLocationPolicy()
    val effectiveLocationPolicySource = if (bypassGeofence) {
        LocationPolicySource.Bypassed
    } else if (payload.locationPolicy != null) {
        payload.locationPolicySource
    } else {
        LocationPolicySource.DisabledNoPolicy
    }
    val geofenceConfigParseResult = remember(
        effectiveLocationPolicy.geofenceEnabled,
        effectiveLocationPolicy.centerLat,
        effectiveLocationPolicy.centerLng,
        effectiveLocationPolicy.radiusMeters,
        effectiveLocationPolicy.shapeType,
        effectiveLocationPolicy.vertices,
        effectiveLocationPolicy.effectiveCircleCenters
    ) {
        parseGeofenceConfig(
            enabled = effectiveLocationPolicy.geofenceEnabled,
            centerLatRaw = effectiveLocationPolicy.centerLat,
            centerLngRaw = effectiveLocationPolicy.centerLng,
            radiusMetersRaw = effectiveLocationPolicy.radiusMeters,
            shapeType = effectiveLocationPolicy.shapeType,
            polygonVertices = effectiveLocationPolicy.vertices,
            circleCenters = effectiveLocationPolicy.effectiveCircleCenters
        )
    }
    val warmLocationPolicySignature = remember(
        geofenceConfigParseResult,
        geofenceBypassState,
        fakeLocationBypassState
    ) {
        buildWarmLocationValidationPolicySignature(
            geofenceConfigParseResult = geofenceConfigParseResult,
            geofenceBypassState = geofenceBypassState,
            fakeLocationBypassState = fakeLocationBypassState
        )
    }
    val geofenceEnabled = geofenceConfigParseResult.enabled
    val officialApkUrl = adminSettings.officialApkUrl.trim()
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var webViewInstance by remember { mutableStateOf<SecureExamWebView?>(null) }
    val flowUiState = rememberExamRuntimeFlowUiState(
        context = context,
        bypassKeyboardPolicy = bypassKeyboardPolicy
    )
    val locationWarmupUiState = rememberExamRuntimeLocationWarmupUiState()
    var examSessionStarted by flowUiState.examSessionStarted
    var lockTaskRequestPending by flowUiState.lockTaskRequestPending
    var screenPinningMessage by flowUiState.screenPinningMessage
    var showExitExamDialog by flowUiState.showExitExamDialog
    var webViewErrorMessage by flowUiState.webViewErrorMessage
    var examServerStatus by rememberSaveable(payload.examUrl) {
        mutableStateOf(ExamServerFooterStatus.Checking)
    }
    LaunchedEffect(examSessionRecoveryNonce, examSessionStarted) {
        onExamSessionStartedStateChange(examSessionStarted)
    }
    var baseNetworkReadiness by remember { mutableStateOf(readNetworkReadinessStatus(context)) }
    val networkUiState = rememberExamRuntimeNetworkUiState(baseNetworkReadiness)
    val networkTimeline = remember { mutableStateListOf<NetworkTimelineEntry>() }
    val networkFlapElapsedMs = remember { mutableStateListOf<Long>() }
    var networkUnstableEpisodeStartedAt by networkUiState.networkUnstableEpisodeStartedAt
    var networkUnstableEpisodeStartedElapsedMs by networkUiState.networkUnstableEpisodeStartedElapsedMs
    var networkUnstableLastFlapAt by networkUiState.networkUnstableLastFlapAt
    var networkUnstableLastFlapElapsedMs by networkUiState.networkUnstableLastFlapElapsedMs
    var networkUnstableWarningShown by networkUiState.networkUnstableWarningShown
    var lastNetworkUnstableWarningAt by networkUiState.lastNetworkUnstableWarningAt
    var showNetworkUnstableDialog by networkUiState.showNetworkUnstableDialog
    var networkUnstableFlapCount by networkUiState.networkUnstableFlapCount
    var networkUnstableLastTransportLabel by networkUiState.networkUnstableLastTransportLabel
    var lastNetworkChangeAt by networkUiState.lastNetworkChangeAt
    var lastNetworkChangeSource by networkUiState.lastNetworkChangeSource
    var networkManualRefreshInFlight by networkUiState.networkManualRefreshInFlight
    var lastConnectedNetworkLabel by networkUiState.lastConnectedNetworkLabel
    var offlineStartedAtElapsedMs by networkUiState.offlineStartedAtElapsedMs
    var offlineStartedAtTimestamp by networkUiState.offlineStartedAtTimestamp
    var offlineWarningShown by networkUiState.offlineWarningShown
    var lastOfflineWarningAt by networkUiState.lastOfflineWarningAt
    var lastOfflineWarningElapsedMs by networkUiState.lastOfflineWarningElapsedMs
    var lastOfflineDurationMs by networkUiState.lastOfflineDurationMs
    var offlineWarningDurationMs by networkUiState.offlineWarningDurationMs
    var showOfflineWarningDialog by networkUiState.showOfflineWarningDialog
    val batteryStatusState = remember { mutableStateOf(readExamBatteryStatus(context)) }
    var batteryStatus by batteryStatusState
    val networkMainHandler = remember { Handler(Looper.getMainLooper()) }
    val clipboardMainHandler = remember { Handler(Looper.getMainLooper()) }
    val overlayMainHandler = remember { Handler(Looper.getMainLooper()) }
    var fullScreenCustomView by remember { mutableStateOf<View?>(null) }
    var fullScreenCustomViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val fullScreenContainer = remember {
        FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(LockBackground.toArgb())
        }
    }
    var useBuiltInExamKeyboard by flowUiState.useBuiltInExamKeyboard
    var exitSessionClearInFlight by flowUiState.exitSessionClearInFlight
    var exitSessionClearRequested by rememberSaveable { mutableStateOf(false) }
    var showBuiltInExamKeyboard by flowUiState.showBuiltInExamKeyboard
    var sideArrowControlsVisible by flowUiState.sideArrowControlsVisible
    var hasEditableFocus by flowUiState.hasEditableFocus
    var builtInKeyboardShiftEnabled by flowUiState.builtInKeyboardShiftEnabled
    var geofencePermissionRequestInFlight by flowUiState.geofencePermissionRequestInFlight
    var geofenceStartValidationInFlight by flowUiState.geofenceStartValidationInFlight
    var webViewSessionResetInFlight by flowUiState.webViewSessionResetInFlight
    var webViewSessionResetError by flowUiState.webViewSessionResetError
    var geofenceManualRefreshInFlight by flowUiState.geofenceManualRefreshInFlight
    var pendingStartExamAfterLocationPermission by flowUiState.pendingStartExamAfterLocationPermission
    var retryStartExamAfterLocationPermissionGrant by flowUiState.retryStartExamAfterLocationPermissionGrant
    var geofenceViolationCount by flowUiState.geofenceViolationCount
    var showGeofenceViolationDialog by flowUiState.showGeofenceViolationDialog
    var showGeofenceMapViewer by flowUiState.showGeofenceMapViewer
    var lastGeofenceTrigger by flowUiState.lastGeofenceTrigger
    var lastGeofenceAt by flowUiState.lastGeofenceAt
    var lastGeofenceContext by flowUiState.lastGeofenceContext
    var lastGeofenceRefreshAt by flowUiState.lastGeofenceRefreshAt
    var geofenceRuntimeEpisodeKey by flowUiState.geofenceRuntimeEpisodeKey
    var fakeLocationViolationCount by flowUiState.fakeLocationViolationCount
    var showFakeLocationViolationDialog by flowUiState.showFakeLocationViolationDialog
    var lastFakeLocationTrigger by flowUiState.lastFakeLocationTrigger
    var lastFakeLocationAt by flowUiState.lastFakeLocationAt
    var lastFakeLocationContext by flowUiState.lastFakeLocationContext
    var fakeLocationRuntimeEpisodeKey by flowUiState.fakeLocationRuntimeEpisodeKey
    var lastFakeLocationWarningKey by flowUiState.lastFakeLocationWarningKey
    var currentKeyboardPackage by flowUiState.currentKeyboardPackage
    var currentKeyboardLabel by flowUiState.currentKeyboardLabel
    var lastKeyboardAllowed by flowUiState.lastKeyboardAllowed
    var locationWarmupInFlight by locationWarmupUiState.locationWarmupInFlight
    var reusableWarmLocationValidation by locationWarmupUiState.reusableWarmLocationValidation
    val securityUiState = rememberExamRuntimeSecurityUiState(
        context = context,
        geofenceConfigParseResult = geofenceConfigParseResult,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState
    )
    var forcedExitViolationCount by securityUiState.forcedExitViolationCount
    var pendingForcedExitViolation by securityUiState.pendingForcedExitViolation
    var showForcedExitAlarm by securityUiState.showForcedExitAlarm
    var keyboardViolationCount by securityUiState.keyboardViolationCount
    var showKeyboardViolationDialog by securityUiState.showKeyboardViolationDialog
    var overlayViolationCount by securityUiState.overlayViolationCount
    var showOverlayViolationDialog by securityUiState.showOverlayViolationDialog
    var overlayShieldRequested by securityUiState.overlayShieldRequested
    var overlayShieldLastApplySucceeded by securityUiState.overlayShieldLastApplySucceeded
    var overlayShieldLastAppliedAt by securityUiState.overlayShieldLastAppliedAt
    var lastOverlayTrigger by securityUiState.lastOverlayTrigger
    var lastOverlayAt by securityUiState.lastOverlayAt
    var lastOverlayContext by securityUiState.lastOverlayContext
    var overlayWindowHasFocus by securityUiState.overlayWindowHasFocus
    var overlayWindowFocusLossPending by securityUiState.overlayWindowFocusLossPending
    var overlayFocusLossConfirmRunnable by securityUiState.overlayFocusLossConfirmRunnable
    var bluetoothPermissionGranted by securityUiState.bluetoothPermissionGranted
    var bluetoothEnabled by securityUiState.bluetoothEnabled
    var accessibilityInspection by securityUiState.accessibilityInspection
    var accessibilityServiceEnabled by securityUiState.accessibilityServiceEnabled
    var adbInspection by securityUiState.adbInspection
    var developerOptionsEnabled by securityUiState.developerOptionsEnabled
    var adbEnabled by securityUiState.adbEnabled
    var rootSecurityStatus by securityUiState.rootSecurityStatus
    var rootDetected by securityUiState.rootDetected
    var selinuxPermissiveWarning by securityUiState.selinuxPermissiveWarning
    var signatureMismatchDetected by securityUiState.signatureMismatchDetected
    var virtualEnvironmentDetected by securityUiState.virtualEnvironmentDetected
    var tamperDetected by securityUiState.tamperDetected
    var tamperSummary by securityUiState.tamperSummary
    var tamperLastLoggedSummary by securityUiState.tamperLastLoggedSummary
    var integrityTamperDetected by securityUiState.integrityTamperDetected
    var integritySummary by securityUiState.integritySummary
    var integrityPublicSummary by securityUiState.integrityPublicSummary
    var integrityLastLoggedSummary by securityUiState.integrityLastLoggedSummary
    var integrityBaselineFingerprint by securityUiState.integrityBaselineFingerprint
    var bluetoothViolationCount by securityUiState.bluetoothViolationCount
    var showBluetoothViolationDialog by securityUiState.showBluetoothViolationDialog
    var geofenceEvaluation by securityUiState.geofenceEvaluation
    var geofenceSecurityStatus by securityUiState.geofenceSecurityStatus
    var fakeLocationSecurityStatus by securityUiState.fakeLocationSecurityStatus
    var deviceTimeSecurityStatus by remember(
        deviceTimeBaseline,
        deviceTimeBypassState
    ) {
        mutableStateOf(
            inspectDeviceTimeSecurity(
                context = context,
                baseline = deviceTimeBaseline,
                bypassState = deviceTimeBypassState
            )
        )
    }
    var lastDeviceTimeDiagnosticKey by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardUiState = rememberExamRuntimeClipboardUiState(context)
    var clipboardSignature by clipboardUiState.clipboardSignature
    var clipboardDecisionFingerprint by clipboardUiState.clipboardDecisionFingerprint
    var clipboardDecisionSemanticSignature by clipboardUiState.clipboardDecisionSemanticSignature
    var clipboardViolationCount by clipboardUiState.clipboardViolationCount
    var lastClipboardChangeEvent by clipboardUiState.lastClipboardChangeEvent
    var lastClipboardObservedAt by clipboardUiState.lastClipboardObservedAt
    var lastClipboardConfirmedAt by clipboardUiState.lastClipboardConfirmedAt
    var lastClipboardObservedSignature by clipboardUiState.lastClipboardObservedSignature
    var lastClipboardBaselineSemanticSignature by clipboardUiState.lastClipboardBaselineSemanticSignature
    var lastClipboardDetectedSemanticSignature by clipboardUiState.lastClipboardDetectedSemanticSignature
    var lastClipboardDecision by clipboardUiState.lastClipboardDecision
    var clipboardPreBackgroundFingerprint by clipboardUiState.clipboardPreBackgroundFingerprint
    var clipboardPreBackgroundSignature by clipboardUiState.clipboardPreBackgroundSignature
    var clipboardPreBackgroundSemanticSignature by clipboardUiState.clipboardPreBackgroundSemanticSignature
    var clipboardConfirmRunnable by clipboardUiState.clipboardConfirmRunnable
    var clipboardResumeCheckRunnable by clipboardUiState.clipboardResumeCheckRunnable
    var clipboardResumeCheckPending by clipboardUiState.clipboardResumeCheckPending
    var showClipboardViolationDialog by clipboardUiState.showClipboardViolationDialog
    val adminUiState = rememberExamRuntimeAdminUiState(
        context = context,
        payload = payload
    )
    var securityIssueDialogTitle by adminUiState.securityIssueDialogTitle
    var securityIssueDialogMessage by adminUiState.securityIssueDialogMessage
    var exitOnSecurityIssueDialogDismiss by adminUiState.exitOnSecurityIssueDialogDismiss
    var screenPinningBypassTamperLogged by adminUiState.screenPinningBypassTamperLogged
    var accessibilityBypassTamperLogged by adminUiState.accessibilityBypassTamperLogged
    var adbBypassTamperLogged by adminUiState.adbBypassTamperLogged
    var clipboardBypassTamperLogged by adminUiState.clipboardBypassTamperLogged
    var overlayBypassTamperLogged by adminUiState.overlayBypassTamperLogged
    var geofenceBypassTamperLogged by adminUiState.geofenceBypassTamperLogged
    var fakeLocationBypassTamperLogged by adminUiState.fakeLocationBypassTamperLogged
    var deviceTimeBypassTamperLogged by adminUiState.deviceTimeBypassTamperLogged
    var appSwitchBypassTamperLogged by adminUiState.appSwitchBypassTamperLogged
    var rootBypassTamperLogged by adminUiState.rootBypassTamperLogged
    var lastAppSwitchTrigger by adminUiState.lastAppSwitchTrigger
    var lastAppSwitchAt by adminUiState.lastAppSwitchAt
    var lastAppSwitchContext by adminUiState.lastAppSwitchContext
    var appSwitchSuppressionReason by adminUiState.appSwitchSuppressionReason
    var appSwitchSuppressedUntilElapsedMs by adminUiState.appSwitchSuppressedUntilElapsedMs
    var appSwitchLifecycleResumePending by adminUiState.appSwitchLifecycleResumePending
    var appSwitchFallbackArmedLogged by adminUiState.appSwitchFallbackArmedLogged
    val accessibilityGuardEnabledState = remember {
        mutableStateOf(isExamGuardAccessibilityEnabled(context))
    }
    var accessibilityGuardEnabled by accessibilityGuardEnabledState
    val accessibilityGuardFallbackActiveState = remember { mutableStateOf(false) }
    var accessibilityGuardFallbackActive by accessibilityGuardFallbackActiveState
    val accessibilityGuardLastReasonState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastReason by accessibilityGuardLastReasonState
    val accessibilityGuardLastForeignPackageState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastForeignPackage by accessibilityGuardLastForeignPackageState
    val accessibilityGuardLastEventTypeState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastEventType by accessibilityGuardLastEventTypeState
    val accessibilityGuardLastDetectedAtState = remember { mutableStateOf<String?>(null) }
    var accessibilityGuardLastDetectedAt by accessibilityGuardLastDetectedAtState
    val accessibilityGuardAlarmSeverityState = remember {
        mutableStateOf(ExamAlarmSeverity.Warning.name)
    }
    var accessibilityGuardAlarmSeverity by accessibilityGuardAlarmSeverityState
    var screenPinningAvailable by adminUiState.screenPinningAvailable
    var screenPinningEnabledInSystem by adminUiState.screenPinningEnabledInSystem
    var lockTaskStateBeforePinningRequest by adminUiState.lockTaskStateBeforePinningRequest
    var lockTaskStateAfterPinningRequest by adminUiState.lockTaskStateAfterPinningRequest
    var screenPinningRequestOutcome by adminUiState.screenPinningRequestOutcome
    var screenPinningDialogLikelyShown by adminUiState.screenPinningDialogLikelyShown
    var screenPinningUserActionInference by adminUiState.screenPinningUserActionInference
    var screenPinningActivationDurationMs by adminUiState.screenPinningActivationDurationMs
    var examSessionCancelledByPinningFailure by adminUiState.examSessionCancelledByPinningFailure
    var sendingSection by adminUiState.sendingSection
    var pendingSection by adminUiState.pendingSection
    var bugReportFeedbackTitle by adminUiState.bugReportFeedbackTitle
    var bugReportFeedbackMessage by adminUiState.bugReportFeedbackMessage
    val appStartedAtElapsedMs = adminUiState.appStartedAtElapsedMs
    var examRuntimeMonitoringArmed by adminUiState.examRuntimeMonitoringArmed
    var examSessionStartedAtElapsedMs by adminUiState.examSessionStartedAtElapsedMs
    var lastParticipantCaptureLogKey by adminUiState.lastParticipantCaptureLogKey
    var participantContext by adminUiState.participantContext
    val examDisplayName = payload.examName.ifBlank { tr("Exam Session", "Sesi Ujian") }
    val alarmSessionIdentity = remember(payload.examName, payload.examUrl, participantContext) {
        buildAlarmSessionIdentity(
            payload = payload,
            participantContext = participantContext
        )
    }
    val appVersionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                ?: BuildConfig.VERSION_NAME
        }.getOrDefault(BuildConfig.VERSION_NAME)
    }
    val overlayRiskResult = OverlayRiskAnalyzer.inspect(
        bypassed = overlayBypassState == OverlayBypassState.Active,
        accessibilityEnabled = accessibilityInspection.blockingServiceActive,
        riskyAccessibilityPackages = accessibilityInspection.riskyPackages,
        violationCount = overlayViolationCount,
        shieldStatus = OverlayShieldStatus(
            supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            requested = overlayShieldRequested,
            lastApplySucceeded = overlayShieldLastApplySucceeded,
            lastApplyAt = overlayShieldLastAppliedAt
        ),
        lastTrigger = lastOverlayTrigger,
        lastDetectedAt = lastOverlayAt,
        lastContext = lastOverlayContext
    )
    val effectiveExamUserAgent = adminSettings.effectiveExamUserAgent()
    val examUserAgentSourceLabel = if (adminSettings.usesDefaultExamUserAgent()) {
        tr("Default", "Default")
    } else {
        tr("Custom", "Custom")
    }
    var diagnosticEvents by adminUiState.diagnosticEvents
    val examAlarmController = remember(context) {
        ExamAlarmController(context.applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()
    var reverseEngineeringRefreshCache by remember {
        mutableStateOf<RuntimeReverseEngineeringRefreshCache?>(null)
    }
    var integrityRefreshCache by remember {
        mutableStateOf<RuntimeIntegrityRefreshCache?>(null)
    }
    var lastAlarmAcknowledgeDedupKey by adminUiState.lastAlarmAcknowledgeDedupKey
    var lastAlarmAcknowledgeAtElapsedMs by adminUiState.lastAlarmAcknowledgeAtElapsedMs
    val isKeyboardAllowed = bypassKeyboardPolicy || isAllowedExamKeyboard(context, currentKeyboardPackage)
    val securityTamperDetected = tamperDetected || integrityTamperDetected
    val examGuardArmed = examRuntimeMonitoringArmed || lockTaskRequestPending || examSessionStarted
    val nativeExamFullscreenActive = examGuardArmed || fullScreenCustomView != null
    val networkReadinessStatus =
        if (
            networkUnstableEpisodeStartedElapsedMs != null &&
            baseNetworkReadiness.verdict == NetworkReadinessVerdict.ConnectedStable &&
            baseNetworkReadiness.examStatus.isConnected
        ) {
            baseNetworkReadiness.copy(
                verdict = NetworkReadinessVerdict.Unstable,
                quickFixReason = "unstable"
            )
        } else {
            baseNetworkReadiness
        }
    val networkStatus = networkReadinessStatus.examStatus
    class RuntimeDiagnosticsOps {
        fun currentNetworkPollingIntervalMillis(): Long {
            val baseInterval = if (
                !networkStatus.isConnected ||
                networkUnstableEpisodeStartedElapsedMs != null
            ) {
                NetworkReadinessPollingUnstableIntervalMillis
            } else {
                NetworkReadinessPollingStableIntervalMillis
            }
            return baseInterval * lowRamProfile.slowPollingMultiplier
        }

        fun currentScreenPinningMonitorIntervalMillis(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long {
            val sessionStartedAt = examSessionStartedAtElapsedMs
            val withinWarmupWindow =
                lockTaskRequestPending ||
                    (
                        sessionStartedAt != null &&
                            (nowElapsedMs - sessionStartedAt).coerceAtLeast(0L) <=
                            ScreenPinningMonitorWarmupWindowMillis
                        )
            return if (withinWarmupWindow) {
                ScreenPinningMonitorWarmupIntervalMillis
            } else {
                ScreenPinningMonitorSteadyIntervalMillis
            }
        }

        val currentOfflineDurationMs = if (
            examSessionStarted &&
                !networkStatus.isConnected &&
                offlineStartedAtElapsedMs != null
        ) {
            (SystemClock.elapsedRealtime() - offlineStartedAtElapsedMs!!).coerceAtLeast(0L)
        } else {
            null
        }
        val offlineRuntimeStatus = ExamOfflineRuntimeStatus(
            offlineActive = examSessionStarted && !networkStatus.isConnected && offlineStartedAtElapsedMs != null,
            offlineStartedAt = offlineStartedAtTimestamp,
            currentOfflineDurationMs = currentOfflineDurationMs,
            offlineWarningShown = offlineWarningShown,
            lastOfflineWarningAt = lastOfflineWarningAt,
            lastOfflineDurationMs = lastOfflineDurationMs
        )
        val networkTimelinePreview = networkTimeline.takeLast(5).asReversed()
        val networkUnstableRuntimeStatus = NetworkUnstableRuntimeStatus(
            unstableActive = networkUnstableEpisodeStartedElapsedMs != null,
            episodeStartedAt = networkUnstableEpisodeStartedAt,
            flapCount = networkUnstableFlapCount,
            lastFlapAt = networkUnstableLastFlapAt,
            warningShown = networkUnstableWarningShown,
            lastWarningAt = lastNetworkUnstableWarningAt,
            lastTransportLabel = networkUnstableLastTransportLabel
        )
        val geofenceRuntimeStatus = GeofenceRuntimeStatus(
            evaluation = geofenceEvaluation,
            securityStatus = geofenceSecurityStatus,
            policySource = effectiveLocationPolicySource,
            violationCount = geofenceViolationCount,
            lastTrigger = lastGeofenceTrigger,
            lastDetectedAt = lastGeofenceAt,
            lastContext = lastGeofenceContext
        )
        val fakeLocationRuntimeStatus = FakeLocationRuntimeStatus(
            securityStatus = fakeLocationSecurityStatus,
            violationCount = fakeLocationViolationCount,
            lastTrigger = lastFakeLocationTrigger,
            lastDetectedAt = lastFakeLocationAt,
            lastContext = lastFakeLocationContext
        )
        val overlayShieldStatus = OverlayShieldStatus(
            supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            requested = overlayShieldRequested,
            lastApplySucceeded = overlayShieldLastApplySucceeded,
            lastApplyAt = overlayShieldLastAppliedAt
        )
        val clipboardRuntimeStatus = ClipboardRuntimeStatus(
            lastObservedAt = lastClipboardObservedAt,
            lastConfirmedAt = lastClipboardConfirmedAt,
            lastObservedSignature = lastClipboardObservedSignature,
            lastDecision = lastClipboardDecision,
            baselineSemanticSignature = lastClipboardBaselineSemanticSignature,
            detectedSemanticSignature = lastClipboardDetectedSemanticSignature,
            currentSemanticSignature = clipboardDecisionSemanticSignature
        )
        val appSwitchLockTaskActive = lockTaskBridge.active()
        val appSwitchProtectionMode = AppSwitchMonitor.protectionModeOf(
            bypassState = appSwitchBypassState,
            screenPinningMode = screenPinningMode,
            guardArmed = examGuardArmed,
            lockTaskActive = appSwitchLockTaskActive
        )
        val appSwitchStatus = AppSwitchMonitor.statusOf(
            bypassState = appSwitchBypassState,
            runtimeMonitoringActive = AppSwitchMonitor.shouldMonitor(
                hostAvailable = mainActivity != null,
                guardArmed = examGuardArmed,
                bypassState = appSwitchBypassState
            ),
            protectionMode = appSwitchProtectionMode,
            lockTaskActive = appSwitchLockTaskActive,
            violationCount = forcedExitViolationCount,
            pendingViolation = pendingForcedExitViolation,
            lastTrigger = lastAppSwitchTrigger,
            lastDetectedAt = lastAppSwitchAt,
            lastContext = lastAppSwitchContext,
            accessibilityGuardEnabled = accessibilityGuardEnabled,
            accessibilityFallbackActive = accessibilityGuardFallbackActive,
            accessibilityViolationCount = AccessibilityExamGuardStore.snapshot(context).violationCount,
            accessibilityLastReason = accessibilityGuardLastReason,
            accessibilityLastForeignPackage = accessibilityGuardLastForeignPackage,
            accessibilityLastEventType = accessibilityGuardLastEventType,
            accessibilityLastDetectedAt = accessibilityGuardLastDetectedAt,
            accessibilityAlarmSeverity = accessibilityGuardAlarmSeverity
        )

        fun currentDiagnosticScreen(): String {
            return examRuntimeDiagnosticScreen(
                lockTaskRequestPending = lockTaskRequestPending,
                examSessionStarted = examSessionStarted,
                examRuntimeMonitoringArmed = examRuntimeMonitoringArmed
            )
        }

        fun clearAppSwitchSuppression() {
            appSwitchSuppressionReason = null
            appSwitchSuppressedUntilElapsedMs = null
        }

        fun setAppSwitchSuppression(
            reason: AppSwitchSuppressionReason,
            durationMs: Long = AppSwitchSuppressionWindowMillis
        ) {
            appSwitchSuppressionReason = reason
            appSwitchSuppressedUntilElapsedMs = SystemClock.elapsedRealtime() + durationMs
        }

        fun currentAppSwitchSuppressionReason(): AppSwitchSuppressionReason? {
            return resolveAppSwitchSuppressionReason(
                reason = appSwitchSuppressionReason,
                expiresAtElapsedMs = appSwitchSuppressedUntilElapsedMs
            )
        }

        fun currentAppSwitchEventDetails(
            signal: AppSwitchSignal,
            suppressionReason: AppSwitchSuppressionReason? = null
        ): String {
            return buildAppSwitchEventDetails(
                signal = signal,
                appSwitchStatus = appSwitchStatus,
                screenPinningMode = screenPinningMode,
                lockTaskActive = lockTaskBridge.active(),
                suppressionReason = suppressionReason
            )
        }

        fun currentOverlayEventDetails(
            signal: OverlaySignal,
            extraContext: String? = null
        ): String {
            return buildOverlayEventDetails(
                signal = signal,
                overlayShieldStatus = overlayShieldStatus,
                appSwitchStatus = appSwitchStatus,
                pendingForcedExitViolation = pendingForcedExitViolation,
                appSwitchLifecycleResumePending = appSwitchLifecycleResumePending,
                overlayWindowHasFocus = overlayWindowHasFocus,
                suppressionReason = currentAppSwitchSuppressionReason(),
                hasFullScreenCustomView = fullScreenCustomView != null,
                extraContext = extraContext
            )
        }

        fun currentInternalDialogReason(): String? {
            return resolveInternalDialogReason(
                showOfflineWarningDialog = showOfflineWarningDialog,
                showNetworkUnstableDialog = showNetworkUnstableDialog,
                showForcedExitAlarm = showForcedExitAlarm,
                showKeyboardViolationDialog = showKeyboardViolationDialog,
                showOverlayViolationDialog = showOverlayViolationDialog,
                showGeofenceViolationDialog = showGeofenceViolationDialog,
                showFakeLocationViolationDialog = showFakeLocationViolationDialog,
                showBluetoothViolationDialog = showBluetoothViolationDialog,
                showClipboardViolationDialog = showClipboardViolationDialog,
                showExitExamDialog = showExitExamDialog,
                pendingSectionPresent = pendingSection != null,
                securityIssueDialogMessagePresent = securityIssueDialogMessage != null,
                bugReportFeedbackMessagePresent = bugReportFeedbackMessage != null
            )
        }

        fun recordOverlayEvent(
            code: String,
            signal: OverlaySignal,
            level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
            extraContext: String? = null
        ) {
            val details = currentOverlayEventDetails(signal, extraContext)
            lastOverlayTrigger = signal.diagnosticLabel()
            lastOverlayAt = diagnosticTimestamp()
            lastOverlayContext = details
            diagnosticEvents = prependDiagnosticEvent(
                existingEvents = diagnosticEvents,
                code = code,
                details = details,
                level = level,
                screen = currentDiagnosticScreen(),
                appStartedAtElapsedMs = appStartedAtElapsedMs,
                examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
                maxEntries = MaxDiagnosticActionLogEntries
            )
        }

        fun recordAction(
            code: String,
            details: String = "-",
            level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
        ) {
            diagnosticEvents = prependDiagnosticEvent(
                existingEvents = diagnosticEvents,
                code = code,
                details = details,
                level = level,
                screen = currentDiagnosticScreen(),
                appStartedAtElapsedMs = appStartedAtElapsedMs,
                examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
                maxEntries = MaxDiagnosticActionLogEntries
            )
        }

        fun currentGeofenceEventDetails(
            trigger: String,
            geofenceStatus: GeofenceSecurityStatus,
            extraContext: String? = null
        ): String {
            return buildGeofenceEventDetails(
                trigger = trigger,
                geofenceStatus = geofenceStatus,
                policySource = effectiveLocationPolicySource,
                extraContext = extraContext
            )
        }

        fun currentFakeLocationEventDetails(
            trigger: String,
            fakeLocationStatus: LocationSpoofSecurityStatus,
            extraContext: String? = null
        ): String {
            return buildFakeLocationEventDetails(
                trigger = trigger,
                fakeLocationStatus = fakeLocationStatus,
                extraContext = extraContext
            )
        }

        fun currentNetworkEventDetails(
            trigger: String,
            status: NetworkReadinessStatus,
            extraContext: String? = null
        ): String {
            return buildNetworkEventDetails(
                trigger = trigger,
                status = status,
                extraContext = extraContext
            )
        }

        fun refreshDeviceTimeSecurity(
            trigger: String,
            emitDiagnosticEvent: Boolean = true
        ): DeviceTimeSecurityStatus {
            val refreshedStatus = inspectDeviceTimeSecurity(
                context = context,
                baseline = deviceTimeBaseline,
                bypassState = deviceTimeBypassState
            )
            deviceTimeSecurityStatus = refreshedStatus
            if (emitDiagnosticEvent) {
                val eventCode = when {
                    refreshedStatus.bypassState == DeviceTimeBypassState.Tampered ->
                        "DEVICE_TIME_BYPASS_TAMPER_DETECTED"
                    refreshedStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeDisabled ->
                        "DEVICE_TIME_AUTO_DISABLED"
                    refreshedStatus.finalVerdict == DeviceTimeSecurityVerdict.AutoTimeZoneDisabled ->
                        "DEVICE_TIME_AUTO_TIME_ZONE_DISABLED"
                    refreshedStatus.finalVerdict == DeviceTimeSecurityVerdict.ClockDriftDetected ->
                        "DEVICE_TIME_DRIFT_DETECTED"
                    else -> null
                }
                val eventKey = eventCode?.plus("|")?.plus(refreshedStatus.finalVerdict.name)
                if (eventCode != null && eventKey != lastDeviceTimeDiagnosticKey) {
                    recordAction(
                        code = eventCode,
                        details = buildDeviceTimeEventDetails(trigger, refreshedStatus),
                        level = DiagnosticEventLevel.WARNING
                    )
                    lastDeviceTimeDiagnosticKey = eventKey
                } else if (eventCode == null) {
                    lastDeviceTimeDiagnosticKey = null
                }
            }
            return refreshedStatus
        }

        fun appendNetworkTimelineEntry(entry: NetworkTimelineEntry) {
            networkTimeline.add(entry)
            while (networkTimeline.size > MaxNetworkTimelineEntries) {
                networkTimeline.removeAt(0)
            }
        }

        fun updateNetworkReadiness(source: String) {
            val previousStatus = baseNetworkReadiness
            val refreshedStatus = readNetworkReadinessStatus(context)
            val coreStateChanged =
                previousStatus.examStatus.isConnected != refreshedStatus.examStatus.isConnected ||
                    previousStatus.transportLabel != refreshedStatus.transportLabel ||
                    previousStatus.diagnostics.isValidated != refreshedStatus.diagnostics.isValidated ||
                    previousStatus.diagnostics.isCaptivePortal != refreshedStatus.diagnostics.isCaptivePortal ||
                    previousStatus.diagnostics.isAirplaneModeEnabled != refreshedStatus.diagnostics.isAirplaneModeEnabled ||
                    previousStatus.verdict != refreshedStatus.verdict
            baseNetworkReadiness = refreshedStatus

            if (refreshedStatus.examStatus.isConnected) {
                lastConnectedNetworkLabel = refreshedStatus.transportLabel
            }

            if (coreStateChanged) {
                val timelineTimestamp = diagnosticTimestamp()
                appendNetworkTimelineEntry(
                    NetworkTimelineEntry(
                        timestamp = timelineTimestamp,
                        source = source,
                        transportLabel = refreshedStatus.transportLabel,
                        connected = refreshedStatus.examStatus.isConnected,
                        validated = refreshedStatus.diagnostics.isValidated,
                        captivePortal = refreshedStatus.diagnostics.isCaptivePortal,
                        summary = buildString {
                            append(refreshedStatus.verdict.name)
                            append(" | ")
                            append(refreshedStatus.examStatus.detail.ifBlank { "-" })
                        }
                    )
                )
                lastNetworkChangeAt = timelineTimestamp
                lastNetworkChangeSource = source
            }

            val flapRelevantChanged =
                previousStatus.examStatus.isConnected != refreshedStatus.examStatus.isConnected ||
                    previousStatus.transportLabel != refreshedStatus.transportLabel ||
                    previousStatus.diagnostics.isValidated != refreshedStatus.diagnostics.isValidated

            if (flapRelevantChanged) {
                val nowElapsed = SystemClock.elapsedRealtime()
                val flapTimestamp = diagnosticTimestamp()
                networkFlapElapsedMs.add(nowElapsed)
                while (
                    networkFlapElapsedMs.isNotEmpty() &&
                    nowElapsed - networkFlapElapsedMs.first() > NetworkUnstableWindowMillis
                ) {
                    networkFlapElapsedMs.removeAt(0)
                }
                networkUnstableLastFlapAt = flapTimestamp
                networkUnstableLastFlapElapsedMs = nowElapsed
                networkUnstableLastTransportLabel = refreshedStatus.transportLabel
                networkUnstableFlapCount = networkFlapElapsedMs.size

                if (
                    networkFlapElapsedMs.size >= NetworkUnstableFlipThreshold &&
                    networkUnstableEpisodeStartedElapsedMs == null
                ) {
                    networkUnstableEpisodeStartedElapsedMs = nowElapsed
                    networkUnstableEpisodeStartedAt = flapTimestamp
                    networkUnstableWarningShown = false
                    recordAction(
                        code = "NETWORK_UNSTABLE_EPISODE_STARTED",
                        details = currentNetworkEventDetails(
                            trigger = source,
                            status = refreshedStatus,
                            extraContext = "flap_count=${networkFlapElapsedMs.size}"
                        ),
                        level = DiagnosticEventLevel.WARNING
                    )
                }
            }

            if (
                examSessionStarted &&
                networkUnstableEpisodeStartedElapsedMs != null &&
                !networkUnstableWarningShown
            ) {
                networkUnstableWarningShown = true
                lastNetworkUnstableWarningAt = diagnosticTimestamp()
                showNetworkUnstableDialog = true
                recordAction(
                    code = "NETWORK_UNSTABLE_WARNING_SHOWN",
                    details = currentNetworkEventDetails(
                        trigger = source,
                        status = refreshedStatus,
                        extraContext = "flap_count=${networkUnstableFlapCount}"
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
            }
        }

        fun launchNetworkManualRefresh(trigger: String) {
            if (networkManualRefreshInFlight) {
                return
            }
            coroutineScope.launch {
                networkManualRefreshInFlight = true
                updateNetworkReadiness(trigger)
                delay(250L)
                networkManualRefreshInFlight = false
            }
        }

        suspend fun evaluateLocationSecurityNow(preferFresh: Boolean): SplitLocationSecurityStatus {
            val permissionGranted = hasLocationPermissionForWifi(context)
            val preciseGranted = hasFineLocationPermission(context)
            val servicesEnabled = isLocationServicesEnabled(context)
            val developerOptionsForLocation = inspectAdb(context).developerOptionsEnabled
            val geofenceSnapshotRequired =
                geofenceConfigParseResult.enabled &&
                    geofenceConfigParseResult.config != null &&
                    geofenceBypassState != GeofenceBypassState.Active &&
                    permissionGranted &&
                    servicesEnabled
            val fakeLocationSnapshotRequired =
                fakeLocationBypassState != FakeLocationBypassState.Active &&
                    permissionGranted &&
                    servicesEnabled
            val locationSnapshot =
                if (
                    geofenceSnapshotRequired ||
                    fakeLocationSnapshotRequired
                ) {
                    acquireBestEffortLocationSnapshot(
                        context = context,
                        preferFresh = preferFresh,
                        geofenceConfig = geofenceConfigParseResult.config.takeIf { geofenceSnapshotRequired }
                    )
                } else {
                    null
                }
            val latestGeofenceStatus = evaluateGeofenceSecurity(
                configResult = geofenceConfigParseResult,
                permissionGranted = permissionGranted,
                preciseLocationGranted = preciseGranted,
                locationServicesEnabled = servicesEnabled,
                locationSnapshot = locationSnapshot,
                bypassState = geofenceBypassState
            )
            val latestFakeLocationStatus = evaluateFakeLocationSecurity(
                monitoringEnabled = true,
                permissionGranted = permissionGranted,
                locationServicesEnabled = servicesEnabled,
                locationSnapshot = locationSnapshot,
                fixQualityStatus = latestGeofenceStatus.fixQualityStatus,
                developerOptionsEnabled = developerOptionsForLocation,
                suspiciousFakeLocationPackages = detectSuspiciousFakeLocationPackages(context),
                bypassState = fakeLocationBypassState
            )
            geofenceEvaluation = latestGeofenceStatus.geofenceEvaluation
            geofenceSecurityStatus = latestGeofenceStatus
            fakeLocationSecurityStatus = latestFakeLocationStatus
            return SplitLocationSecurityStatus(
                geofenceStatus = latestGeofenceStatus,
                fakeLocationStatus = latestFakeLocationStatus
            )
        }

        fun applyGeofenceRuntimeEvaluation(
            geofenceStatus: GeofenceSecurityStatus,
            trigger: String
        ) {
            val evaluation = geofenceStatus.geofenceEvaluation
            if (!evaluation.enabled) {
                geofenceRuntimeEpisodeKey = null
                return
            }

            if (!geofenceStatus.blocking) {
                val previousEpisode = geofenceRuntimeEpisodeKey
                if (previousEpisode != null) {
                    recordAction(
                        code = "GEOFENCE_RUNTIME_RECOVERED",
                        details = currentGeofenceEventDetails(
                            trigger = trigger,
                            geofenceStatus = geofenceStatus,
                            extraContext = "previous_verdict=$previousEpisode"
                        ),
                        level = DiagnosticEventLevel.INFO
                    )
                }
                geofenceRuntimeEpisodeKey = null
                return
            }

            val nextEpisodeKey = geofenceStatus.finalVerdict.diagnosticLabel()
            if (geofenceRuntimeEpisodeKey == nextEpisodeKey) {
                return
            }

            geofenceRuntimeEpisodeKey = nextEpisodeKey
            val eventCode = when (geofenceStatus.finalVerdict) {
                GeofenceSecurityVerdict.Outside -> "GEOFENCE_RUNTIME_OUTSIDE"
                GeofenceSecurityVerdict.PreciseRequired -> "GEOFENCE_RUNTIME_PRECISE_REQUIRED"
                else -> "GEOFENCE_RUNTIME_LOCATION_UNAVAILABLE"
            }
            val details = currentGeofenceEventDetails(trigger = trigger, geofenceStatus = geofenceStatus)
            lastGeofenceTrigger = trigger
            lastGeofenceAt = diagnosticTimestamp()
            lastGeofenceContext = details
            geofenceViolationCount += 1
            showGeofenceViolationDialog = true
            recordAction(
                code = eventCode,
                details = details,
                level = DiagnosticEventLevel.SECURITY
            )
            examAlarmController.start()
        }

        fun applyFakeLocationRuntimeEvaluation(
            fakeLocationStatus: LocationSpoofSecurityStatus,
            trigger: String
        ) {
            if (!fakeLocationStatus.monitoringEnabled) {
                fakeLocationRuntimeEpisodeKey = null
                return
            }

            if (!fakeLocationStatus.blocking) {
                val previousEpisode = fakeLocationRuntimeEpisodeKey
                if (previousEpisode != null) {
                    recordAction(
                        code = "FAKE_LOCATION_RUNTIME_RECOVERED",
                        details = currentFakeLocationEventDetails(
                            trigger = trigger,
                            fakeLocationStatus = fakeLocationStatus,
                            extraContext = "previous_verdict=$previousEpisode"
                        ),
                        level = DiagnosticEventLevel.INFO
                    )
                }
                fakeLocationRuntimeEpisodeKey = null
                return
            }

            val nextEpisodeKey = buildString {
                append(fakeLocationStatus.finalVerdict.diagnosticLabel())
                append(':')
                append(fakeLocationStatus.confidenceTier.diagnosticLabel())
            }
            if (fakeLocationRuntimeEpisodeKey == nextEpisodeKey) {
                return
            }

            fakeLocationRuntimeEpisodeKey = nextEpisodeKey
            val eventCode = when (fakeLocationStatus.finalVerdict) {
                LocationSpoofSecurityVerdict.PermissionRequired -> "FAKE_LOCATION_RUNTIME_PERMISSION_REQUIRED"
                LocationSpoofSecurityVerdict.LocationServicesDisabled -> "FAKE_LOCATION_RUNTIME_LOCATION_SERVICES_REQUIRED"
                LocationSpoofSecurityVerdict.LocationUnavailable -> "FAKE_LOCATION_RUNTIME_LOCATION_UNAVAILABLE"
                else -> "FAKE_LOCATION_RUNTIME_SPOOF_DETECTED"
            }
            val details = currentFakeLocationEventDetails(
                trigger = trigger,
                fakeLocationStatus = fakeLocationStatus
            )
            lastFakeLocationTrigger = trigger
            lastFakeLocationAt = diagnosticTimestamp()
            lastFakeLocationContext = details
            fakeLocationViolationCount += 1
            showFakeLocationViolationDialog = true
            recordAction(
                code = eventCode,
                details = details,
                level = DiagnosticEventLevel.SECURITY
            )
            examAlarmController.start()
        }

        suspend fun refreshGeofenceStatus(
            preferFresh: Boolean,
            trigger: String,
            allowRuntimeViolation: Boolean
        ): SplitLocationSecurityStatus {
            val latestLocationStatus = evaluateLocationSecurityNow(preferFresh = preferFresh)
            if (examSessionStarted && geofenceEnabled && allowRuntimeViolation && !bypassGeofence) {
                applyGeofenceRuntimeEvaluation(
                    geofenceStatus = latestLocationStatus.geofenceStatus,
                    trigger = trigger
                )
            } else if (!latestLocationStatus.geofenceStatus.geofenceEvaluation.enabled || !examSessionStarted) {
                geofenceRuntimeEpisodeKey = null
            }
            if (
                examSessionStarted &&
                allowRuntimeViolation &&
                latestLocationStatus.fakeLocationStatus.monitoringEnabled &&
                !bypassFakeLocation
            ) {
                applyFakeLocationRuntimeEvaluation(
                    fakeLocationStatus = latestLocationStatus.fakeLocationStatus,
                    trigger = trigger
                )
            } else if (!latestLocationStatus.fakeLocationStatus.monitoringEnabled || !examSessionStarted) {
                fakeLocationRuntimeEpisodeKey = null
            }
            if (
                latestLocationStatus.fakeLocationStatus.monitoringEnabled &&
                latestLocationStatus.fakeLocationStatus.bypassState != FakeLocationBypassState.Active &&
                latestLocationStatus.fakeLocationStatus.warningOnly &&
                latestLocationStatus.fakeLocationStatus.suspiciousFakeLocationPackages.isNotEmpty()
            ) {
                val warningKey = buildString {
                    append(latestLocationStatus.fakeLocationStatus.confidenceTier.diagnosticLabel())
                    append(':')
                    append(latestLocationStatus.fakeLocationStatus.suspiciousFakeLocationPackages.joinToString())
                }
                if (warningKey != lastFakeLocationWarningKey) {
                    recordAction(
                        code = "FAKE_LOCATION_PACKAGE_WARNING",
                        details = currentFakeLocationEventDetails(
                            trigger = trigger,
                            fakeLocationStatus = latestLocationStatus.fakeLocationStatus
                        ),
                        level = DiagnosticEventLevel.WARNING
                    )
                    lastFakeLocationWarningKey = warningKey
                }
            } else {
                lastFakeLocationWarningKey = null
            }
            return latestLocationStatus
        }

        fun buildCurrentWarmLocationValidationKey(): String {
            return buildWarmLocationValidationKey(
                permissionGranted = hasLocationPermissionForWifi(context),
                locationServicesEnabled = isLocationServicesEnabled(context),
                policySignature = warmLocationPolicySignature
            )
        }

        fun invalidateWarmLocationValidationCache() {
            reusableWarmLocationValidation = null
        }

        suspend fun resolveStartExamLocationValidation(): SplitLocationSecurityStatus {
            val currentValidationKey = buildCurrentWarmLocationValidationKey()
            val reusableWarmLocationForStart = reusableWarmLocationValidation?.takeIf {
                it.isReusableForStart(currentValidationKey = currentValidationKey)
            }
            if (reusableWarmLocationForStart != null) {
                val warmAgeMs = (
                    SystemClock.elapsedRealtime() - reusableWarmLocationForStart.completedAtElapsedMs
                ).coerceAtLeast(0L)
                debugLogExamStart(
                    "startExamSession reused warm location validation prepared ${warmAgeMs} ms ago"
                )
                geofenceEvaluation = reusableWarmLocationForStart.result.geofenceStatus.geofenceEvaluation
                geofenceSecurityStatus = reusableWarmLocationForStart.result.geofenceStatus
                fakeLocationSecurityStatus = reusableWarmLocationForStart.result.fakeLocationStatus
                return reusableWarmLocationForStart.result
            }

            val forcedRefreshReason = reusableWarmLocationValidation
                ?.reuseFailureReason(currentValidationKey = currentValidationKey)
                ?: "no_warm_validation"
            debugLogExamStart(
                "startExamSession forcing full location validation (reason=$forcedRefreshReason)"
            )
            return refreshGeofenceStatus(
                preferFresh = true,
                trigger = "start_exam_validation",
                allowRuntimeViolation = false
            )
        }

        fun launchLocationSecurityManualRefresh(trigger: String) {
            if (geofenceManualRefreshInFlight) {
                return
            }
            invalidateWarmLocationValidationCache()
            geofenceManualRefreshInFlight = true
            coroutineScope.launch {
                try {
                    val refreshedStatus = debugMeasureExamStartSuspendWork("locationRefresh:$trigger") {
                        refreshGeofenceStatus(
                            preferFresh = true,
                            trigger = trigger,
                            allowRuntimeViolation = false
                        )
                    }
                    val refreshedAt = diagnosticTimestamp()
                    val validationKey = buildCurrentWarmLocationValidationKey()
                    lastGeofenceRefreshAt = refreshedAt
                    reusableWarmLocationValidation = WarmLocationValidationCache(
                        result = refreshedStatus,
                        validationKey = validationKey,
                        completedAtElapsedMs = SystemClock.elapsedRealtime(),
                        completedAtTimestamp = refreshedAt
                    ).takeIf { it.isReusableForStart(currentValidationKey = validationKey) }
                } finally {
                    geofenceManualRefreshInFlight = false
                }
            }
        }

    }
    val runtimeDiagnosticsOps = RuntimeDiagnosticsOps()
    val currentOfflineDurationMs = runtimeDiagnosticsOps.currentOfflineDurationMs
    val offlineRuntimeStatus = runtimeDiagnosticsOps.offlineRuntimeStatus
    val networkTimelinePreview = runtimeDiagnosticsOps.networkTimelinePreview
    val networkUnstableRuntimeStatus = runtimeDiagnosticsOps.networkUnstableRuntimeStatus
    val geofenceRuntimeStatus = runtimeDiagnosticsOps.geofenceRuntimeStatus
    val fakeLocationRuntimeStatus = runtimeDiagnosticsOps.fakeLocationRuntimeStatus
    val overlayShieldStatus = runtimeDiagnosticsOps.overlayShieldStatus
    val clipboardRuntimeStatus = runtimeDiagnosticsOps.clipboardRuntimeStatus
    val appSwitchLockTaskActive = runtimeDiagnosticsOps.appSwitchLockTaskActive
    val appSwitchProtectionMode = runtimeDiagnosticsOps.appSwitchProtectionMode
    val appSwitchStatus = runtimeDiagnosticsOps.appSwitchStatus
    fun currentNetworkPollingIntervalMillis(): Long = runtimeDiagnosticsOps.currentNetworkPollingIntervalMillis()
    fun currentScreenPinningMonitorIntervalMillis(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long = runtimeDiagnosticsOps.currentScreenPinningMonitorIntervalMillis(nowElapsedMs)
    fun currentDiagnosticScreen(): String = runtimeDiagnosticsOps.currentDiagnosticScreen()
    fun clearAppSwitchSuppression() = runtimeDiagnosticsOps.clearAppSwitchSuppression()
    fun setAppSwitchSuppression(
        reason: AppSwitchSuppressionReason,
        durationMs: Long = AppSwitchSuppressionWindowMillis
    ) = runtimeDiagnosticsOps.setAppSwitchSuppression(reason, durationMs)
    fun currentAppSwitchSuppressionReason(): AppSwitchSuppressionReason? = runtimeDiagnosticsOps.currentAppSwitchSuppressionReason()
    fun currentAppSwitchEventDetails(
        signal: AppSwitchSignal,
        suppressionReason: AppSwitchSuppressionReason? = null
    ): String = runtimeDiagnosticsOps.currentAppSwitchEventDetails(signal, suppressionReason)
    fun currentOverlayEventDetails(
        signal: OverlaySignal,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentOverlayEventDetails(signal, extraContext)
    fun currentInternalDialogReason(): String? = runtimeDiagnosticsOps.currentInternalDialogReason()
    fun recordOverlayEvent(
        code: String,
        signal: OverlaySignal,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        extraContext: String? = null
    ) = runtimeDiagnosticsOps.recordOverlayEvent(code, signal, level, extraContext)
    fun recordAction(
        code: String,
        details: String = "-",
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO
    ) = runtimeDiagnosticsOps.recordAction(code, details, level)
    suspend fun runExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) {
        val host = safeExamServerHost(payload.examUrl)
        if (markChecking) {
            examServerStatus = ExamServerFooterStatus.Checking
        }
        recordAction(
            code = "EXAM_SERVER_PROBE_STARTED",
            details = buildExamServerProbeDetails(
                trigger = trigger,
                host = host,
                reason = "started"
            )
        )
        val result = probeExamServerFooterStatus(payload.examUrl)
        examServerStatus = result.status
        recordAction(
            code = result.eventCode,
            details = buildExamServerProbeDetails(
                trigger = trigger,
                host = result.host,
                method = result.method,
                code = result.code,
                latencyMs = result.latencyMs,
                reason = result.reason
            ),
            level = result.eventLevel
        )
    }
    fun launchExamServerProbe(
        trigger: String,
        markChecking: Boolean = true
    ) {
        coroutineScope.launch {
            runExamServerProbe(trigger = trigger, markChecking = markChecking)
        }
    }
    fun handleAccessibilityGuardViolation(violation: AccessibilityGuardRuntimeViolation) {
        val currentViolationCount = forcedExitViolationCount
        forcedExitViolationCount = maxOf(
            currentViolationCount,
            violation.violationCount.coerceAtLeast(1)
        )
        pendingForcedExitViolation = true
        showForcedExitAlarm = true
        accessibilityGuardLastReason = violation.reason
        accessibilityGuardLastForeignPackage = violation.foreignPackage
        accessibilityGuardLastEventType = violation.eventType
        accessibilityGuardLastDetectedAt = violation.detectedAt
        accessibilityGuardAlarmSeverity = violation.severity.name
        lastAppSwitchTrigger = AppSwitchSignal.AccessibilityGuard.diagnosticLabel()
        lastAppSwitchAt = violation.detectedAt ?: diagnosticTimestamp()
        val details = buildAccessibilityGuardViolationDetails(
            currentAppSwitchEventDetails(AppSwitchSignal.AccessibilityGuard),
            violation
        )
        lastAppSwitchContext = details
        recordAction(
            accessibilityGuardEventCodeForReason(violation.reason),
            details,
            DiagnosticEventLevel.SECURITY
        )
        recordAction(
            "ACCESSIBILITY_GUARD_RETURN_TO_EXAM_REQUESTED",
            "reason=${violation.reason?.ifBlank { "-" } ?: "-"} | " +
                "foreign_package=${violation.foreignPackage?.ifBlank { "-" } ?: "-"}",
            DiagnosticEventLevel.INFO
        )
        examAlarmController.start(violation.severity)
    }
    fun currentGeofenceEventDetails(
        trigger: String,
        geofenceStatus: GeofenceSecurityStatus,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentGeofenceEventDetails(trigger, geofenceStatus, extraContext)
    fun currentFakeLocationEventDetails(
        trigger: String,
        fakeLocationStatus: LocationSpoofSecurityStatus,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentFakeLocationEventDetails(trigger, fakeLocationStatus, extraContext)
    fun currentNetworkEventDetails(
        trigger: String,
        status: NetworkReadinessStatus,
        extraContext: String? = null
    ): String = runtimeDiagnosticsOps.currentNetworkEventDetails(trigger, status, extraContext)
    fun refreshDeviceTimeSecurity(
        trigger: String,
        emitDiagnosticEvent: Boolean = true
    ): DeviceTimeSecurityStatus = runtimeDiagnosticsOps.refreshDeviceTimeSecurity(trigger, emitDiagnosticEvent)
    fun appendNetworkTimelineEntry(entry: NetworkTimelineEntry) = runtimeDiagnosticsOps.appendNetworkTimelineEntry(entry)
    fun updateNetworkReadiness(source: String) = runtimeDiagnosticsOps.updateNetworkReadiness(source)
    fun launchNetworkManualRefresh(trigger: String) = runtimeDiagnosticsOps.launchNetworkManualRefresh(trigger)
    suspend fun evaluateLocationSecurityNow(preferFresh: Boolean): SplitLocationSecurityStatus = runtimeDiagnosticsOps.evaluateLocationSecurityNow(preferFresh)
    fun applyGeofenceRuntimeEvaluation(
        geofenceStatus: GeofenceSecurityStatus,
        trigger: String
    ) = runtimeDiagnosticsOps.applyGeofenceRuntimeEvaluation(geofenceStatus, trigger)
    fun applyFakeLocationRuntimeEvaluation(
        fakeLocationStatus: LocationSpoofSecurityStatus,
        trigger: String
    ) = runtimeDiagnosticsOps.applyFakeLocationRuntimeEvaluation(fakeLocationStatus, trigger)
    suspend fun refreshGeofenceStatus(
        preferFresh: Boolean,
        trigger: String,
        allowRuntimeViolation: Boolean
    ): SplitLocationSecurityStatus = runtimeDiagnosticsOps.refreshGeofenceStatus(preferFresh, trigger, allowRuntimeViolation)
    fun buildCurrentWarmLocationValidationKey(): String = runtimeDiagnosticsOps.buildCurrentWarmLocationValidationKey()
    fun invalidateWarmLocationValidationCache() = runtimeDiagnosticsOps.invalidateWarmLocationValidationCache()
    suspend fun resolveStartExamLocationValidation(): SplitLocationSecurityStatus = runtimeDiagnosticsOps.resolveStartExamLocationValidation()
    fun launchLocationSecurityManualRefresh(trigger: String) = runtimeDiagnosticsOps.launchLocationSecurityManualRefresh(trigger)
    LaunchedEffect(warmLocationPolicySignature) {
        invalidateWarmLocationValidationCache()
    }
    LaunchedEffect(examSessionStarted, payload.examUrl) {
        if (!examSessionStarted) {
            examServerStatus = ExamServerFooterStatus.Checking
            return@LaunchedEffect
        }
        var firstProbe = true
        while (true) {
            runExamServerProbe(
                trigger = if (firstProbe) "exam_start" else "periodic",
                markChecking = examServerStatus == ExamServerFooterStatus.Checking
            )
            firstProbe = false
            delay(ExamServerProbeIntervalMillis)
        }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            geofencePermissionRequestInFlight = false
            invalidateWarmLocationValidationCache()
            val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                hasFineLocationPermission(context)
            val anyGranted = result.values.any { it } || hasLocationPermissionForWifi(context)
            val preciseRequiredForStart = geofenceEnabled && !bypassGeofence
            val permissionReadyForStart = if (preciseRequiredForStart) fineGranted else anyGranted
            if (permissionReadyForStart && pendingStartExamAfterLocationPermission) {
                pendingStartExamAfterLocationPermission = false
                retryStartExamAfterLocationPermissionGrant = true
            } else if (pendingStartExamAfterLocationPermission) {
                pendingStartExamAfterLocationPermission = false
                val blockedByGeofencePrecision = preciseRequiredForStart && anyGranted
                recordAction(
                    code = when {
                        blockedByGeofencePrecision -> "START_EXAM_BLOCKED_GEOFENCE_PRECISE_REQUIRED"
                        preciseRequiredForStart -> "START_EXAM_BLOCKED_GEOFENCE_PERMISSION"
                        else -> "START_EXAM_BLOCKED_FAKE_LOCATION_PERMISSION"
                    },
                    details = when {
                        blockedByGeofencePrecision -> "reason=approximate_only"
                        anyGranted -> "reason=permission_not_ready"
                        else -> "reason=permission_denied"
                    },
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    if (blockedByGeofencePrecision) "Precise Location Required" else "Location Permission Required",
                    if (blockedByGeofencePrecision) "Lokasi Presisi Diperlukan" else "Izin Lokasi Diperlukan"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    when {
                        blockedByGeofencePrecision ->
                            "Precise location must be granted before the exam can start."
                        preciseRequiredForStart ->
                            "Location permission must be granted before the exam can start."
                        else ->
                            "Location access is required so anti-fake-location can validate the exam before it starts."
                    },
                    when {
                        blockedByGeofencePrecision ->
                            "Lokasi presisi harus diberikan sebelum ujian bisa dimulai."
                        preciseRequiredForStart ->
                            "Izin lokasi harus diberikan sebelum ujian bisa dimulai."
                        else ->
                            "Akses lokasi wajib tersedia agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                    }
                )
            } else {
                launchLocationSecurityManualRefresh(trigger = "location_permission_quick_fix")
            }
        }
    val bluetoothPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            bluetoothPermissionGranted = granted || !requiresBluetoothExamPermission()
            bluetoothEnabled = if (bluetoothPermissionGranted) {
                isBluetoothEnabledForExam(context)
            } else {
                false
            }
        }

    class RuntimeMonitoringOps {
        fun armExamRuntimeMonitoring(reason: String) {
            examRuntimeMonitoringArmed = true
            recordAction(
                code = "EXAM_RUNTIME_GUARDS_ARMED",
                details = "reason=$reason | screen_pinning_mode=${screenPinningMode.name.lowercase()}",
                level = DiagnosticEventLevel.INFO
            )
        }

        fun disarmExamRuntimeMonitoring() {
            examRuntimeMonitoringArmed = false
            if (accessibilityGuardFallbackActive || AccessibilityExamGuardStore.snapshot(context).armed) {
                AccessibilityExamGuardStore.disarm(context)
                recordAction(
                    code = "ACCESSIBILITY_GUARD_DISARMED",
                    details = "reason=runtime_monitoring_disarmed",
                    level = DiagnosticEventLevel.INFO
                )
            }
            accessibilityGuardFallbackActive = false
            appSwitchLifecycleResumePending = false
            clipboardResumeCheckRunnable?.let(clipboardMainHandler::removeCallbacks)
            clipboardResumeCheckRunnable = null
            overlayFocusLossConfirmRunnable?.let(overlayMainHandler::removeCallbacks)
            overlayFocusLossConfirmRunnable = null
            overlayWindowFocusLossPending = false
            clipboardResumeCheckPending = false
            clipboardPreBackgroundFingerprint = null
            clipboardPreBackgroundSignature = null
            clipboardPreBackgroundSemanticSignature = null
            participantContext = null
        }

        fun recordAppSwitchEvent(
            code: String,
            signal: AppSwitchSignal,
            level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
            updateLastDetectedAt: Boolean = true
        ) {
            val details = currentAppSwitchEventDetails(signal)
            lastAppSwitchTrigger = signal.diagnosticLabel()
            if (updateLastDetectedAt) {
                lastAppSwitchAt = diagnosticTimestamp()
            }
            lastAppSwitchContext = details
            recordAction(
                code = code,
                details = details,
                level = level
            )
        }

        fun acknowledgeRuntimeAlarm(
            type: AlarmAcknowledgeType,
            violationCount: Int,
            buildPayload: (detailRef: String) -> AlarmAcknowledgePayload,
            onUiAcknowledge: () -> Unit
        ) {
            val detailRef = latestAlarmDetailRef(
                diagnosticEvents = diagnosticEvents,
                type = type
            )
            val alarmPayload = buildPayload(detailRef)
            onUiAcknowledge()

            if (!examGuardArmed && !examSessionStarted) {
                return
            }

            val dedupeKey = listOf(
                alarmPayload.alarmType.wireName,
                violationCount.toString(),
                alarmPayload.examName,
                alarmPayload.examUrlHost,
                alarmPayload.examUrlHashShort
            ).joinToString("|")
            val nowElapsedMs = SystemClock.elapsedRealtime()
            if (
                lastAlarmAcknowledgeDedupKey == dedupeKey &&
                nowElapsedMs - lastAlarmAcknowledgeAtElapsedMs <= AlarmAcknowledgeDedupWindowMillis
            ) {
                return
            }

            lastAlarmAcknowledgeDedupKey = dedupeKey
            lastAlarmAcknowledgeAtElapsedMs = nowElapsedMs
            recordAction(
                code = "ALARM_ACKNOWLEDGED",
                details = buildAlarmAckEventDetails(
                    payload = alarmPayload,
                    result = "queued"
                ),
                level = DiagnosticEventLevel.INFO
            )

            coroutineScope.launch {
                sendTelegramAlarmAcknowledge(alarmPayload)
                    .onSuccess {
                        recordAction(
                            code = "ALARM_ACK_TG_SENT",
                            details = buildAlarmAckEventDetails(
                                payload = alarmPayload,
                                result = "sent"
                            ),
                            level = DiagnosticEventLevel.INFO
                        )
                    }
                    .onFailure { error ->
                        val errorSummary = error.message?.take(160)
                            ?: error.javaClass.simpleName.take(160)
                        recordAction(
                            code = "ALARM_ACK_TG_FAILED",
                            details = buildAlarmAckEventDetails(
                                payload = alarmPayload,
                                result = "failed",
                                extra = "error=$errorSummary"
                            ),
                            level = DiagnosticEventLevel.ERROR
                        )
                    }
            }
        }

        fun confirmClipboardViolation(
            snapshot: ClipboardSnapshot,
            decision: ClipboardChangeDecision,
            eventSuffix: String,
            updateObservedSnapshot: Boolean,
            baselineSemanticSignatureOverride: String? = null
        ) {
            val eventTimestamp = diagnosticTimestamp()
            val baselineSemanticSignature =
                baselineSemanticSignatureOverride ?: clipboardDecisionSemanticSignature
            val diagnosticSnapshot =
                if (snapshot.rawSignature.isBlank()) readClipboardSnapshotFull(context) else snapshot
            clipboardSignature = diagnosticSnapshot.rawSignature
            clipboardDecisionFingerprint = diagnosticSnapshot.decisionFingerprint
            clipboardDecisionSemanticSignature = diagnosticSnapshot.semanticSignature
            if (updateObservedSnapshot) {
                lastClipboardObservedAt = eventTimestamp
                lastClipboardObservedSignature = diagnosticSnapshot.rawSignature.ifBlank { null }
            }
            lastClipboardBaselineSemanticSignature = baselineSemanticSignature.ifBlank { null }
            lastClipboardDetectedSemanticSignature = diagnosticSnapshot.semanticSignature.ifBlank { null }
            lastClipboardConfirmedAt = eventTimestamp
            lastClipboardDecision = decision.diagnosticLabel()
            recordAction(
                code = "CLIPBOARD_CHANGED",
                details = "decision=${decision.diagnosticLabel()};source=$eventSuffix",
                level = DiagnosticEventLevel.SECURITY
            )
            lastClipboardChangeEvent = "$eventTimestamp - Clipboard berubah saat sesi ujian ($eventSuffix)"
            clipboardViolationCount += 1
            showClipboardViolationDialog = true
            examAlarmController.start()
        }

        fun armClipboardResumeCheck(reason: String) {
            if (clipboardBypassState == ClipboardBypassState.Active || bypassClipboard) {
                clipboardResumeCheckPending = false
                clipboardPreBackgroundFingerprint = null
                clipboardPreBackgroundSignature = null
                clipboardPreBackgroundSemanticSignature = null
                return
            }
            val beforeBackgroundSnapshot = readClipboardSnapshotLite(context)
            clipboardPreBackgroundFingerprint = beforeBackgroundSnapshot.decisionFingerprint
            clipboardPreBackgroundSignature = null
            clipboardPreBackgroundSemanticSignature = beforeBackgroundSnapshot.semanticSignature.ifBlank { null }
            clipboardResumeCheckPending = true
            lastClipboardDecision = "resume_check_armed:$reason"
        }

        fun applyFatalSecuritySignal(signal: FatalSecuritySignal) {
            recordAction(
                code = signal.eventCode,
                details = signal.details,
                level = DiagnosticEventLevel.SECURITY
            )
            examSessionCancelledByPinningFailure = true
            lockTaskRequestPending = false
            clearAppSwitchSuppression()
            disarmExamRuntimeMonitoring()
            pendingForcedExitViolation = false
            showForcedExitAlarm = false
            screenPinningMessage = null
            showBuiltInExamKeyboard = false
            hasEditableFocus = false
            securityIssueDialogTitle = signal.title
            securityIssueDialogMessage = signal.message
            exitOnSecurityIssueDialogDismiss = true
            examSessionStarted = false
            examSessionStartedAtElapsedMs = null
            webViewErrorMessage = null
            lockTaskBridge.disengage()
            examAlarmController.start()
        }

        fun refreshReverseEngineeringStatus() {
            val cachedResult = reverseEngineeringRefreshCache
            val result =
                if (cachedResult != null && cachedResult.isFresh()) {
                    cachedResult.result
                } else {
                    ReverseEngineeringGuard.inspect(context).also { refreshed ->
                        reverseEngineeringRefreshCache = RuntimeReverseEngineeringRefreshCache(
                            result = refreshed,
                            capturedAtElapsedMs = SystemClock.elapsedRealtime()
                        )
                    }
                }
            tamperDetected = result.tamperDetected
            tamperSummary = result.summary()
            if (result.tamperDetected && tamperSummary != tamperLastLoggedSummary) {
                recordAction(
                    code = "TAMPER_DETECTED",
                    details = tamperSummary,
                    level = DiagnosticEventLevel.SECURITY
                )
                tamperLastLoggedSummary = tamperSummary
            }
            if (!result.tamperDetected && tamperLastLoggedSummary != null) {
                tamperLastLoggedSummary = null
            }
        }

        fun refreshIntegrityGuard() {
            val cachedResult = integrityRefreshCache
            val result =
                if (cachedResult != null && cachedResult.isFreshFor(integrityBaselineFingerprint)) {
                    cachedResult.result
                } else {
                    IntegrityGuard.check(context, integrityBaselineFingerprint).also { refreshed ->
                        integrityRefreshCache = RuntimeIntegrityRefreshCache(
                            result = refreshed,
                            baselineFingerprint = integrityBaselineFingerprint,
                            capturedAtElapsedMs = SystemClock.elapsedRealtime()
                        )
                    }
                }
            if (integrityBaselineFingerprint.isNullOrBlank() &&
                result.currentFingerprint.isNotBlank() &&
                result.currentFingerprint != "-"
            ) {
                integrityBaselineFingerprint = result.currentFingerprint
            }
            integrityTamperDetected = !result.ok
            integritySummary = result.details
            integrityPublicSummary = buildIntegrityPublicSummary(result.issues)
            val issueSignature = integritySummary.ifBlank { "-" }
            if (!result.ok && issueSignature != integrityLastLoggedSummary) {
                val issueSet = result.issues.toSet()
                if ("dex_hash_mismatch" in issueSet) {
                    recordAction(
                        code = "TAMPER_APK_HASH",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                if ("signature_changed" in issueSet) {
                    recordAction(
                        code = "TAMPER_SIGNATURE_CHANGED",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                if (issueSet.any { it.startsWith("sysprop_") } || "test_keys" in issueSet) {
                    recordAction(
                        code = "TAMPER_SYSTEM_PROP",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                if ("hook_class" in issueSet) {
                    recordAction(
                        code = "TAMPER_HOOK_CLASS",
                        details = integritySummary,
                        level = DiagnosticEventLevel.SECURITY
                    )
                }
                integrityLastLoggedSummary = issueSignature
            }
            if (result.ok && integrityLastLoggedSummary != null) {
                integrityLastLoggedSummary = null
            }
        }

        fun hideSystemKeyboard() {
            val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
            webViewInstance?.windowToken?.let { windowToken ->
                inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
            }
            componentActivity.currentFocus?.windowToken?.let { windowToken ->
                inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
            }
        }

        fun showSystemKeyboard() {
            if (useBuiltInExamKeyboard) {
                return
            }
            val inputMethodManager = context.getSystemService(InputMethodManager::class.java) ?: return
            webViewInstance?.post {
                webViewInstance?.isFocusable = true
                webViewInstance?.isFocusableInTouchMode = true
                webViewInstance?.requestFocus(View.FOCUS_DOWN)
                webViewInstance?.requestFocus()
                webViewInstance?.let { inputMethodManager.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
            }
        }

        fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback?) {
            if (fullScreenCustomView != null) {
                callback?.onCustomViewHidden()
                return
            }
            fullScreenCustomView = view
            fullScreenCustomViewCallback = callback
            webViewInstance?.visibility = View.GONE
            (view.parent as? ViewGroup)?.removeView(view)
            fullScreenContainer.removeAllViews()
            fullScreenContainer.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            lockTaskBridge.engage(allowLockTask = screenPinningMode.allowsLockTask())
        }

        fun hideCustomView() {
            val view = fullScreenCustomView ?: return
            fullScreenContainer.removeView(view)
            fullScreenCustomViewCallback?.onCustomViewHidden()
            fullScreenCustomViewCallback = null
            fullScreenCustomView = null
            webViewInstance?.visibility = View.VISIBLE
            lockTaskBridge.engage(allowLockTask = screenPinningMode.allowsLockTask())
        }

        fun cleanupActiveExamWebViewInstance() {
            fullScreenCustomView?.let { view ->
                runCatching { fullScreenContainer.removeView(view) }
                runCatching { fullScreenCustomViewCallback?.onCustomViewHidden() }
            }
            fullScreenCustomViewCallback = null
            fullScreenCustomView = null
            hasEditableFocus = false
            webViewInstance?.apply {
                runCatching { stopLoading() }
                runCatching { detachExamKeyboardBridge() }
                runCatching { detachExamParticipantCaptureBridge() }
                runCatching { detachExamNativeFullscreenBridge() }
                runCatching { prepareForFreshExamSession() }
                runCatching { removeAllViews() }
                runCatching { destroy() }
            }
            webViewInstance = null
        }

        suspend fun clearExamSessionOnExit(
            reason: String,
            waitForResult: Boolean
        ): Result<Unit> {
            if (exitSessionClearRequested) {
                return Result.success(Unit)
            }

            exitSessionClearRequested = true
            if (waitForResult) {
                exitSessionClearInFlight = true
            }

            val existingWebView = if (waitForResult) webViewInstance else null
            val details = buildString {
                append("reason=")
                append(reason)
                append(" | wait=")
                append(if (waitForResult) "yes" else "no")
                append(" | webview=")
                append(if (existingWebView != null) "present" else "none")
            }
            recordAction(code = "WEBVIEW_EXIT_SESSION_CLEAR_STARTED", details = details)

            val clearResult = try {
                clearExamWebViewSessionData(
                    context = context.applicationContext,
                    existingWebView = existingWebView
                )
            } catch (throwable: Throwable) {
                Result.failure(throwable)
            }

            if (waitForResult) {
                cleanupActiveExamWebViewInstance()
                exitSessionClearInFlight = false
            }

            if (clearResult.isSuccess) {
                recordAction(code = "WEBVIEW_EXIT_SESSION_CLEAR_SUCCEEDED", details = details)
            } else {
                val error = clearResult.exceptionOrNull()
                val errorSummary = error?.message?.take(160)
                    ?: error?.javaClass?.simpleName?.take(160)
                    ?: "unknown"
                recordAction(
                    code = "WEBVIEW_EXIT_SESSION_CLEAR_FAILED",
                    details = "$details | error=$errorSummary",
                    level = DiagnosticEventLevel.ERROR
                )
            }

            return clearResult
        }

        fun launchExitSessionClearBestEffort(reason: String) {
            componentActivity.lifecycleScope.launch {
                clearExamSessionOnExit(
                    reason = reason,
                    waitForResult = false
                )
            }
        }

        fun handleWebViewRendererGone(
            view: SecureExamWebView?,
            didCrash: Boolean,
            rendererPriorityAtExit: Int?
        ): Boolean {
            val details = buildString {
                append("did_crash=")
                append(if (didCrash) "yes" else "no")
                append(" | priority_at_exit=")
                append(rendererPriorityAtExit ?: "-")
                append(" | low_ram=")
                append(if (lowRamProfile.enabled) "yes" else "no")
            }
            recordAction(
                code = "WEBVIEW_RENDERER_GONE",
                details = details,
                level = DiagnosticEventLevel.ERROR
            )
            if (view != null && view !== webViewInstance) {
                runCatching {
                    view.stopLoading()
                    view.detachExamKeyboardBridge()
                    view.detachExamParticipantCaptureBridge()
                    view.detachExamNativeFullscreenBridge()
                    view.removeAllViews()
                    view.destroy()
                }
            } else {
                cleanupActiveExamWebViewInstance()
            }
            lockTaskBridge.disengage()
            disarmExamRuntimeMonitoring()
            clearAppSwitchSuppression()
            lockTaskRequestPending = false
            examSessionStarted = false
            examSessionStartedAtElapsedMs = null
            showBuiltInExamKeyboard = false
            hasEditableFocus = false
            loadingProgress = 0f
            webViewErrorMessage = null
            webViewSessionResetInFlight = false
            webViewSessionResetError = localized(
                uiLanguage,
                "The exam page ran out of memory and was closed safely. Retry Start Exam Mode to reopen a clean session.",
                "Halaman ujian kehabisan memori dan ditutup dengan aman. Coba lagi Mulai Ujian untuk membuka sesi bersih."
            )
            return true
        }

        fun handleRuntimeTrimMemory(level: Int) {
            if (!MemoryPressureCoordinator.shouldRespondToPressure(level)) {
                return
            }
            reusableWarmLocationValidation = null
            reverseEngineeringRefreshCache = null
            integrityRefreshCache = null
            if (!examSessionStarted) {
                cleanupActiveExamWebViewInstance()
                if (fullScreenCustomView == null) {
                    runCatching { fullScreenContainer.removeAllViews() }
                }
            }
        }
    }
    val runtimeMonitoringOps = RuntimeMonitoringOps()
    fun armExamRuntimeMonitoring(reason: String) = runtimeMonitoringOps.armExamRuntimeMonitoring(reason)
    fun disarmExamRuntimeMonitoring() = runtimeMonitoringOps.disarmExamRuntimeMonitoring()
    fun recordAppSwitchEvent(
        code: String,
        signal: AppSwitchSignal,
        level: DiagnosticEventLevel = DiagnosticEventLevel.INFO,
        updateLastDetectedAt: Boolean = true
    ) = runtimeMonitoringOps.recordAppSwitchEvent(code, signal, level, updateLastDetectedAt)
    fun acknowledgeRuntimeAlarm(
        type: AlarmAcknowledgeType,
        violationCount: Int,
        buildPayload: (detailRef: String) -> AlarmAcknowledgePayload,
        onUiAcknowledge: () -> Unit
    ) = runtimeMonitoringOps.acknowledgeRuntimeAlarm(type, violationCount, buildPayload, onUiAcknowledge)
    fun confirmClipboardViolation(
        snapshot: ClipboardSnapshot,
        decision: ClipboardChangeDecision,
        eventSuffix: String,
        updateObservedSnapshot: Boolean,
        baselineSemanticSignatureOverride: String? = null
    ) = runtimeMonitoringOps.confirmClipboardViolation(snapshot, decision, eventSuffix, updateObservedSnapshot, baselineSemanticSignatureOverride)
    fun armClipboardResumeCheck(reason: String) = runtimeMonitoringOps.armClipboardResumeCheck(reason)
    fun applyFatalSecuritySignal(signal: FatalSecuritySignal) = runtimeMonitoringOps.applyFatalSecuritySignal(signal)
    fun refreshReverseEngineeringStatus() = runtimeMonitoringOps.refreshReverseEngineeringStatus()
    fun refreshIntegrityGuard() = runtimeMonitoringOps.refreshIntegrityGuard()
    fun hideSystemKeyboard() = runtimeMonitoringOps.hideSystemKeyboard()
    fun showSystemKeyboard() = runtimeMonitoringOps.showSystemKeyboard()
    fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback?) = runtimeMonitoringOps.showCustomView(view, callback)
    fun hideCustomView() = runtimeMonitoringOps.hideCustomView()
    fun cleanupActiveExamWebViewInstance() = runtimeMonitoringOps.cleanupActiveExamWebViewInstance()
    suspend fun clearExamSessionOnExit(reason: String, waitForResult: Boolean): Result<Unit> =
        runtimeMonitoringOps.clearExamSessionOnExit(reason, waitForResult)
    fun launchExitSessionClearBestEffort(reason: String) =
        runtimeMonitoringOps.launchExitSessionClearBestEffort(reason)
    fun handleWebViewRendererGone(
        view: SecureExamWebView?,
        didCrash: Boolean,
        rendererPriorityAtExit: Int?
    ): Boolean = runtimeMonitoringOps.handleWebViewRendererGone(view, didCrash, rendererPriorityAtExit)
    fun handleRuntimeTrimMemory(level: Int) = runtimeMonitoringOps.handleRuntimeTrimMemory(level)
    RuntimeRecoveryAndMemoryEffects(
        pendingDirectLinkSaveLog = pendingDirectLinkSaveLog,
        pendingRecoveryEventDetails = pendingRecoveryEventDetails,
        examSessionRecoveryNonce = examSessionRecoveryNonce,
        recordInfoAction = { code, details -> recordAction(code = code, details = details) },
        onDirectLinkSaveLogConsumed = onDirectLinkSaveLogConsumed,
        onRecoveryEventConsumed = onRecoveryEventConsumed,
        refreshReverseEngineeringStatus = ::refreshReverseEngineeringStatus,
        refreshIntegrityGuard = ::refreshIntegrityGuard,
        onSimulateRendererGone = {
            handleWebViewRendererGone(
                view = webViewInstance,
                didCrash = false,
                rendererPriorityAtExit = null
            )
        },
        onTrimMemory = ::handleRuntimeTrimMemory
    )

    val keyboardBridge: ExamKeyboardBridge = remember {
        ExamKeyboardBridge(
            onEditableFocusChangedCallback = { focused ->
                hasEditableFocus = focused
                showBuiltInExamKeyboard = useBuiltInExamKeyboard && focused
                if (useBuiltInExamKeyboard && focused) {
                    hideSystemKeyboard()
                }
            }
        )
    }
    val participantCaptureBridge = ExamParticipantCaptureBridge { rawPayload, sourceKey ->
        val result = parseExamParticipantContext(rawPayload, sourceKey)
        when (result) {
            is ExamParticipantCaptureResult.Captured -> {
                val capturedContext = result.context
                if (participantContext != capturedContext) {
                    participantContext = capturedContext
                }
                val details = capturedContext.diagnosticSummary()
                if (lastParticipantCaptureLogKey != "captured|$details") {
                    lastParticipantCaptureLogKey = "captured|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_CAPTURED",
                        details = details
                    )
                }
            }

            is ExamParticipantCaptureResult.Ignored -> {
                val details = "source_key=${result.sourceKey} | reason=${result.reason}"
                if (lastParticipantCaptureLogKey != "ignored|$details") {
                    lastParticipantCaptureLogKey = "ignored|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_IGNORED",
                        details = details
                    )
                }
            }

            is ExamParticipantCaptureResult.Failed -> {
                val details = "source_key=${result.sourceKey} | reason=${result.reason}"
                if (lastParticipantCaptureLogKey != "failed|$details") {
                    lastParticipantCaptureLogKey = "failed|$details"
                    recordAction(
                        code = "PARTICIPANT_CONTEXT_CAPTURE_FAILED",
                        details = details,
                        level = DiagnosticEventLevel.ERROR
                    )
                }
            }
        }
    }
    val nativeFullscreenBridge = remember(mainActivity) {
        ExamNativeFullscreenBridge {
            val hostActivity = mainActivity ?: return@ExamNativeFullscreenBridge false
            hostActivity.setExamLockMode(enabled = true, allowLockTask = false)
            true
        }
    }

    class RuntimeSecurityOps {
        fun refreshKeyboardSecurity(triggerViolation: Boolean) {
            val latestPackage = getCurrentInputMethodPackage(context).orEmpty()
            val latestLabel = resolveKeyboardAppLabel(context, latestPackage)
            val allowedNow = if (bypassKeyboardPolicy) true else isAllowedExamKeyboard(context, latestPackage)

            if (bypassKeyboardPolicy) {
                currentKeyboardPackage = latestPackage
                currentKeyboardLabel = latestLabel
                lastKeyboardAllowed = true
                if (!examSessionStarted) {
                    useBuiltInExamKeyboard = false
                    showBuiltInExamKeyboard = false
                    hasEditableFocus = false
                }
                return
            }

            if (!examSessionStarted && allowedNow) {
                useBuiltInExamKeyboard = false
            }

            if (triggerViolation && !useBuiltInExamKeyboard && lastKeyboardAllowed && !allowedNow) {
                recordAction(
                    code = "KEYBOARD_POLICY_VIOLATION",
                    details = latestPackage,
                    level = DiagnosticEventLevel.SECURITY
                )
                keyboardViolationCount += 1
                showKeyboardViolationDialog = true
                examAlarmController.start()
            }

            currentKeyboardPackage = latestPackage
            currentKeyboardLabel = latestLabel
            lastKeyboardAllowed = allowedNow
        }

        fun refreshBluetoothSecurity(triggerViolation: Boolean) {
            bluetoothPermissionGranted = hasBluetoothExamPermission(context)
            val enabledNow = if (bluetoothPermissionGranted) {
                isBluetoothEnabledForExam(context)
            } else {
                false
            }

            if (!bypassBluetooth && triggerViolation && examSessionStarted && enabledNow) {
                recordAction(
                    code = "BLUETOOTH_ENABLED_DURING_EXAM",
                    level = DiagnosticEventLevel.SECURITY
                )
                bluetoothViolationCount += 1
                showBluetoothViolationDialog = true
                examAlarmController.start()
            }

            bluetoothEnabled = enabledNow
        }

        fun refreshScreenPinningDiagnostics() {
            screenPinningAvailable = ScreenPinningPlatformBridge.isAvailable()
            screenPinningEnabledInSystem = ScreenPinningPlatformBridge.readSystemSetting(context)
            val currentLockTaskState = lockTaskBridge.stateLabel()
            lockTaskStateAfterPinningRequest = currentLockTaskState
            if (screenPinningRequestOutcome == "Belum diminta") {
                lockTaskStateBeforePinningRequest = currentLockTaskState
            }
        }

        fun checkSignatureIntegrity(triggerViolation: Boolean): SignatureIntegrityResult {
            val expectedFingerprints = resolveExpectedSigningFingerprints(
                isDebugBuild = BuildConfig.DEBUG,
                releaseFingerprint = SecureStrings.signingFingerprintRelease,
                debugFingerprint = SecureStrings.signingFingerprintDebug
            )
            val result = SignatureIntegrity.check(context, expectedFingerprints)
            signatureMismatchDetected = !result.isMatch
            if (!result.isMatch && triggerViolation) {
                recordAction(
                    code = "SIGNATURE_MISMATCH_DETECTED",
                    details = result.reason,
                    level = DiagnosticEventLevel.SECURITY
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "App Integrity Warning",
                    "Integritas Aplikasi Bermasalah"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "The app signature does not match the official release. Reinstall the official APK.",
                    "Signature aplikasi tidak cocok dengan APK resmi. Instal ulang APK resmi."
                )
            }
            return result
        }

        fun refreshDeviceIntegritySecurity(triggerViolation: Boolean) {
            val latestAccessibilityInspection = inspectAccessibility(context)
            val latestAccessibilityServiceEnabled = latestAccessibilityInspection.blockingServiceActive
            val latestAdbInspection = inspectAdb(context)
            val rootDetectionDetails = getRootDetectionDetails(context)
            val latestRootSecurityStatus = buildRootSecurityStatus(rootDetectionDetails)
            val virtualEnvironmentDiagnostics = getVirtualEnvironmentDiagnostics(context)
            val latestVirtualEnvironmentDetected = virtualEnvironmentDiagnostics.detected
            checkSignatureIntegrity(triggerViolation)

            if (triggerViolation && examSessionStarted) {
                if (!bypassAccessibility && !accessibilityServiceEnabled && latestAccessibilityServiceEnabled) {
                    recordAction(
                        code = "ACCESSIBILITY_ENABLED_DURING_EXAM",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "Accessibility Service Terdeteksi"
                    securityIssueDialogMessage =
                        "Aksesibilitas aktif saat ujian berjalan. Nonaktifkan accessibility service agar ujian tetap aman."
                    examAlarmController.start()
                }

                if (!bypassAdb && !developerOptionsEnabled && latestAdbInspection.developerOptionsEnabled) {
                    recordAction(
                        code = "DEVELOPER_OPTIONS_ENABLED_DURING_EXAM",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "Developer Mode Aktif"
                    securityIssueDialogMessage =
                        "Developer Mode terdeteksi aktif saat ujian berjalan. Nonaktifkan sebelum melanjutkan."
                    examAlarmController.start()
                }

                if (!bypassAdb && !adbEnabled && latestAdbInspection.adbEnabled) {
                    recordAction(
                        code = "ADB_ENABLED_DURING_EXAM",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "USB Debugging (ADB) Aktif"
                    securityIssueDialogMessage =
                        "USB debugging terdeteksi aktif saat ujian berjalan. Nonaktifkan ADB sebelum melanjutkan."
                    examAlarmController.start()
                }

                if (!bypassVirtualEnvironment &&
                    !virtualEnvironmentDetected &&
                    latestVirtualEnvironmentDetected
                ) {
                    recordAction(
                        code = "VIRTUAL_ENVIRONMENT_DETECTED",
                        details = virtualEnvironmentDiagnostics.indicators.joinToString().ifBlank { "-" },
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "Virtual Environment Terdeteksi"
                    securityIssueDialogMessage =
                        "Perangkat ini terdeteksi berjalan di emulator/VM. Gunakan perangkat fisik untuk melanjutkan ujian."
                    examAlarmController.start()
                }

                if (!bypassRoot && !rootDetected && latestRootSecurityStatus.detected) {
                    recordAction(
                        code = "ROOT_INDICATOR_DETECTED",
                        level = DiagnosticEventLevel.SECURITY
                    )
                    securityIssueDialogTitle = "Root Device Terdeteksi"
                    securityIssueDialogMessage = buildRootIssueMessage(latestRootSecurityStatus.details)
                    examAlarmController.start()
                }
            }

            accessibilityInspection = latestAccessibilityInspection
            accessibilityServiceEnabled = latestAccessibilityServiceEnabled
            accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
            adbInspection = latestAdbInspection
            developerOptionsEnabled = latestAdbInspection.developerOptionsEnabled
            adbEnabled = latestAdbInspection.adbEnabled
            rootSecurityStatus = latestRootSecurityStatus
            rootDetected = latestRootSecurityStatus.detected
            selinuxPermissiveWarning = latestRootSecurityStatus.selinuxPermissive
            virtualEnvironmentDetected = latestVirtualEnvironmentDetected
        }

        fun launchTelegramSectionReport(section: DiagnosticSection) {
            if (sendingSection != null) {
                return
            }
            val sectionLabel = diagnosticSectionLabel(section, uiLanguage)
            val latestAccessibilityInspection = inspectAccessibility(context)
            val latestOverlayRiskResult = OverlayRiskAnalyzer.inspect(
                bypassed = overlayBypassState == OverlayBypassState.Active,
                accessibilityEnabled = latestAccessibilityInspection.blockingServiceActive,
                riskyAccessibilityPackages = latestAccessibilityInspection.riskyPackages,
                violationCount = overlayViolationCount,
                shieldStatus = overlayShieldStatus,
                lastTrigger = lastOverlayTrigger,
                lastDetectedAt = lastOverlayAt,
                lastContext = lastOverlayContext
            )
            val latestAdbInspection = inspectAdb(context)
            val latestRootSecurityStatus = buildRootSecurityStatus(getRootDetectionDetails(context))
            val latestAppSwitchStatus = AppSwitchMonitor.statusOf(
                bypassState = appSwitchBypassState,
                runtimeMonitoringActive = AppSwitchMonitor.shouldMonitor(
                    hostAvailable = mainActivity != null,
                    guardArmed = examGuardArmed,
                    bypassState = appSwitchBypassState
                ),
                protectionMode = AppSwitchMonitor.protectionModeOf(
                    bypassState = appSwitchBypassState,
                    screenPinningMode = screenPinningMode,
                    guardArmed = examGuardArmed,
                    lockTaskActive = lockTaskBridge.active()
                ),
                lockTaskActive = lockTaskBridge.active(),
                violationCount = forcedExitViolationCount,
                pendingViolation = pendingForcedExitViolation,
                lastTrigger = lastAppSwitchTrigger,
                lastDetectedAt = lastAppSwitchAt,
                lastContext = lastAppSwitchContext,
                accessibilityGuardEnabled = accessibilityGuardEnabled,
                accessibilityFallbackActive = accessibilityGuardFallbackActive,
                accessibilityViolationCount = AccessibilityExamGuardStore.snapshot(context).violationCount,
                accessibilityLastReason = accessibilityGuardLastReason,
                accessibilityLastForeignPackage = accessibilityGuardLastForeignPackage,
                accessibilityLastEventType = accessibilityGuardLastEventType,
                accessibilityLastDetectedAt = accessibilityGuardLastDetectedAt,
                accessibilityAlarmSeverity = accessibilityGuardAlarmSeverity
            )
            recordAction(code = "DIAGNOSTIC_SECTION_REQUESTED", details = section.name)
            refreshScreenPinningDiagnostics()
            refreshKeyboardSecurity(triggerViolation = false)
            refreshBluetoothSecurity(triggerViolation = false)
            refreshDeviceIntegritySecurity(triggerViolation = false)
            refreshIntegrityGuard()
            val latestDeviceTimeStatus = refreshDeviceTimeSecurity(
                trigger = "diagnostic_request",
                emitDiagnosticEvent = false
            )
            sendingSection = section

            coroutineScope.launch {
                val latestLocationStatus = refreshGeofenceStatus(
                    preferFresh = false,
                    trigger = "diagnostic_request",
                    allowRuntimeViolation = false
                )
                val latestGeofenceRuntimeStatus = GeofenceRuntimeStatus(
                    evaluation = latestLocationStatus.geofenceStatus.geofenceEvaluation,
                    securityStatus = latestLocationStatus.geofenceStatus,
                    policySource = effectiveLocationPolicySource,
                    violationCount = geofenceViolationCount,
                    lastTrigger = lastGeofenceTrigger,
                    lastDetectedAt = lastGeofenceAt,
                    lastContext = lastGeofenceContext
                )
                val latestFakeLocationRuntimeStatus = FakeLocationRuntimeStatus(
                    securityStatus = latestLocationStatus.fakeLocationStatus,
                    violationCount = fakeLocationViolationCount,
                    lastTrigger = lastFakeLocationTrigger,
                    lastDetectedAt = lastFakeLocationAt,
                    lastContext = lastFakeLocationContext
                )
                sendTelegramSectionReport(
                    context = context,
                    section = section,
                    examName = payload.examName,
                    examUserAgent = effectiveExamUserAgent,
                    examUserAgentSource = if (adminSettings.usesDefaultExamUserAgent()) "default" else "custom",
                    participantContext = participantContext,
                    examSessionStarted = examSessionStarted,
                    examRuntimeGuardsArmed = examGuardArmed,
                    adminOverridesSummary = adminOverridesSummary,
                    keyboardPackage = currentKeyboardPackage,
                    keyboardAllowed = isKeyboardAllowed,
                    usingBuiltInExamKeyboard = useBuiltInExamKeyboard,
                    bluetoothPermissionGranted = bluetoothPermissionGranted,
                    bluetoothEnabled = bluetoothEnabled,
                    accessibilityServiceEnabled = accessibilityServiceEnabled,
                    bypassAccessibility = bypassAccessibility,
                    accessibilityBypassTampered = adminSettings.accessibilityBypassTampered,
                    adbInspection = latestAdbInspection,
                    adbBypassState = adbBypassState,
                    rootSecurityStatus = latestRootSecurityStatus,
                    rootBypassState = rootBypassState,
                    clipboardSignature = clipboardSignature,
                    clipboardViolationCount = clipboardViolationCount,
                    lastClipboardChangeEvent = lastClipboardChangeEvent,
                    networkStatus = networkStatus,
                    clipboardRuntimeStatus = clipboardRuntimeStatus,
                    offlineRuntimeStatus = offlineRuntimeStatus,
                    geofenceRuntimeStatus = latestGeofenceRuntimeStatus,
                    fakeLocationRuntimeStatus = latestFakeLocationRuntimeStatus,
                    overlayViolationCount = overlayViolationCount,
                    overlayRiskResult = latestOverlayRiskResult,
                    overlayBypassTampered = adminSettings.overlayBypassTampered,
                    appSwitchStatus = latestAppSwitchStatus,
                    appSwitchBypassTampered = adminSettings.appSwitchBypassTampered,
                    screenPinningAvailable = screenPinningAvailable,
                    screenPinningEnabledInSystem = screenPinningEnabledInSystem,
                    lockTaskStateBeforePinningRequest = lockTaskStateBeforePinningRequest,
                    lockTaskStateAfterPinningRequest = lockTaskStateAfterPinningRequest,
                    screenPinningRequestOutcome = screenPinningRequestOutcome,
                    screenPinningDialogLikelyShown = screenPinningDialogLikelyShown,
                    screenPinningUserActionInference = screenPinningUserActionInference,
                    screenPinningActivationDurationMs = screenPinningActivationDurationMs,
                    examSessionCancelledByPinningFailure = examSessionCancelledByPinningFailure,
                    isScreenPinningActive = lockTaskBridge.active(),
                    bypassScreenPinning = bypassScreenPinning,
                    bypassOverlay = bypassOverlay,
                    bypassAppSwitch = bypassAppSwitch,
                    deviceTimeSecurityStatus = latestDeviceTimeStatus,
                    bypassDeviceTime = bypassDeviceTime,
                    integritySummary = integrityPublicSummary,
                    diagnosticEvents = diagnosticEvents,
                    uiLanguage = uiLanguage,
                    networkReadinessStatus = networkReadinessStatus,
                    networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
                    networkTimelinePreview = networkTimelinePreview,
                    lastNetworkChangeAt = lastNetworkChangeAt,
                    lastNetworkChangeSource = lastNetworkChangeSource,
                    lastConnectedNetworkLabel = lastConnectedNetworkLabel
                ).onSuccess {
                    recordAction(code = "DIAGNOSTIC_SECTION_SENT", details = section.name)
                    bugReportFeedbackTitle = localized(uiLanguage, "Diagnostics sent", "Diagnostik terkirim")
                    bugReportFeedbackMessage = localized(
                        uiLanguage,
                        "$sectionLabel diagnostics have been sent to Telegram.",
                        "Diagnostik $sectionLabel sudah dikirim ke Telegram."
                    )
                }.onFailure { throwable ->
                    recordAction(
                        code = "DIAGNOSTIC_SECTION_FAILED",
                        details = throwable.message ?: "-",
                        level = DiagnosticEventLevel.ERROR
                    )
                    bugReportFeedbackTitle = localized(uiLanguage, "Diagnostics failed", "Kirim diagnostik gagal")
                    bugReportFeedbackMessage =
                        throwable.message ?: localized(
                            uiLanguage,
                            "Diagnostics could not be sent to Telegram.",
                            "Data diagnostik belum berhasil dikirim ke Telegram."
                        )
                }

                sendingSection = null
            }
        }

    }
    val runtimeSecurityOps = RuntimeSecurityOps()
    fun refreshKeyboardSecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshKeyboardSecurity(triggerViolation)
    fun refreshBluetoothSecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshBluetoothSecurity(triggerViolation)
    fun refreshScreenPinningDiagnostics() = runtimeSecurityOps.refreshScreenPinningDiagnostics()
    fun checkSignatureIntegrity(triggerViolation: Boolean): SignatureIntegrityResult = runtimeSecurityOps.checkSignatureIntegrity(triggerViolation)
    fun refreshDeviceIntegritySecurity(triggerViolation: Boolean) = runtimeSecurityOps.refreshDeviceIntegritySecurity(triggerViolation)
    fun launchTelegramSectionReport(section: DiagnosticSection) = runtimeSecurityOps.launchTelegramSectionReport(section)

    class StartExamController {
        fun resetPreparationSecurityEpisodes() {
            geofenceViolationCount = 0
            geofenceRuntimeEpisodeKey = null
            lastGeofenceTrigger = null
            lastGeofenceAt = null
            lastGeofenceContext = null
            lastGeofenceRefreshAt = null
            showGeofenceViolationDialog = false
            fakeLocationViolationCount = 0
            fakeLocationRuntimeEpisodeKey = null
            lastFakeLocationTrigger = null
            lastFakeLocationAt = null
            lastFakeLocationContext = null
            showFakeLocationViolationDialog = false
        }

        fun finalizeExamSessionStart(lockTaskAlreadyActive: Boolean) {
            webViewSessionResetError = null
            examSessionStartedAtElapsedMs = SystemClock.elapsedRealtime()
            if (!lockTaskAlreadyActive) {
                lockTaskBridge.engage(allowLockTask = false)
            }
            examSessionStarted = true
            val clipboardSnapshot = readClipboardSnapshotLite(context)
            clipboardDecisionFingerprint = clipboardSnapshot.decisionFingerprint
            clipboardDecisionSemanticSignature = clipboardSnapshot.semanticSignature
            lastClipboardObservedAt = null
            lastClipboardConfirmedAt = null
            lastClipboardObservedSignature = null
            lastClipboardBaselineSemanticSignature = null
            lastClipboardDetectedSemanticSignature = null
            lastClipboardDecision = ClipboardChangeDecision.Idle.diagnosticLabel()
            clipboardPreBackgroundFingerprint = null
            clipboardPreBackgroundSignature = null
            clipboardPreBackgroundSemanticSignature = null
            clipboardResumeCheckPending = false
            if (useBuiltInExamKeyboard) {
                hideSystemKeyboard()
            } else {
                showBuiltInExamKeyboard = false
                showSystemKeyboard()
            }
            sideArrowControlsVisible = true
        }

        suspend fun prepareCleanExamWebViewSessionForStart(): Boolean {
            if (webViewSessionResetInFlight) {
                return false
            }

            webViewSessionResetInFlight = true
            webViewSessionResetError = null
            recordAction(code = "WEBVIEW_SESSION_RESET_STARTED", details = "strict_all")

            val resetResult = debugMeasureExamStartSuspendWork("prepareCleanExamWebViewSessionForStart") {
                clearExamWebViewSessionData(
                    context = context,
                    existingWebView = webViewInstance
                )
            }

            webViewSessionResetInFlight = false
            if (resetResult.isSuccess) {
                recordAction(code = "WEBVIEW_SESSION_RESET_SUCCEEDED", details = "strict_all")
                return true
            }

            val failureDetails = resetResult.exceptionOrNull()?.message ?: "unknown"
            val userMessage = localized(
                uiLanguage,
                "The app could not clear the previous WebView session data yet. Retry Start Exam Mode. If this keeps happening, close and reopen the app.",
                "Aplikasi belum bisa membersihkan data sesi WebView sebelumnya. Coba lagi Mulai Ujian. Jika tetap gagal, tutup lalu buka ulang aplikasi."
            )
            recordAction(
                code = "WEBVIEW_SESSION_RESET_FAILED",
                details = failureDetails,
                level = DiagnosticEventLevel.ERROR
            )
            webViewSessionResetError = userMessage
            securityIssueDialogTitle = localized(
                uiLanguage,
                "Unable to Prepare Clean Exam Session",
                "Gagal Menyiapkan Sesi Ujian Bersih"
            )
            securityIssueDialogMessage = userMessage
            return false
        }

        fun completeStartExamSessionAfterPrechecks() {
            if (lockTaskRequestPending || geofenceStartValidationInFlight || webViewSessionResetInFlight) {
                return
            }

            if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                accessibilityGuardEnabled
            ) {
                launchAccessibilityGuardFallbackExamStart(
                    context = context,
                    lockTaskBridge = lockTaskBridge,
                    coroutineScope = coroutineScope,
                    examGuardArmed = examGuardArmed,
                    updateFallbackUiState = { beforeState ->
                        accessibilityGuardFallbackActive = true
                        accessibilityGuardLastReason = null
                        accessibilityGuardLastForeignPackage = null
                        accessibilityGuardLastEventType = null
                        accessibilityGuardLastDetectedAt = null
                        accessibilityGuardAlarmSeverity = ExamAlarmSeverity.Warning.name
                        forcedExitViolationCount = 0
                        pendingForcedExitViolation = false
                        showForcedExitAlarm = false
                        lockTaskStateBeforePinningRequest = beforeState
                        lockTaskStateAfterPinningRequest = beforeState
                        screenPinningRequestOutcome = "Accessibility guard fallback"
                        screenPinningDialogLikelyShown = false
                        screenPinningUserActionInference = "Tidak diminta; Accessibility Exam Guard aktif"
                        screenPinningActivationDurationMs = 0L
                        examSessionCancelledByPinningFailure = false
                        lockTaskRequestPending = false
                        screenPinningMessage = null
                        webViewErrorMessage = null
                        exitOnSecurityIssueDialogDismiss = false
                    },
                    recordAction = { code, details, level -> recordAction(code, details, level) },
                    clearAppSwitchSuppression = ::clearAppSwitchSuppression,
                    resetPreparationSecurityEpisodes = this::resetPreparationSecurityEpisodes,
                    prepareCleanExamWebViewSessionForStart = this::prepareCleanExamWebViewSessionForStart,
                    armExamRuntimeMonitoring = ::armExamRuntimeMonitoring,
                    finalizeExamSessionStart = this::finalizeExamSessionStart,
                    onCleanSessionFailed = { accessibilityGuardFallbackActive = false }
                )
                return
            }

            AccessibilityExamGuardStore.disarm(context)
            accessibilityGuardFallbackActive = false

            if (screenPinningMode == ScreenPinningMode.Bypassed) {
                val bypassState = ScreenPinningEnforcer.launchState(screenPinningMode, lockTaskBridge)
                recordAction(code = bypassState.eventCode, details = bypassState.eventDetails)
                lockTaskStateBeforePinningRequest = bypassState.beforeState
                lockTaskStateAfterPinningRequest = bypassState.afterState
                screenPinningRequestOutcome = bypassState.outcome
                screenPinningDialogLikelyShown = bypassState.dialogLikelyShown
                screenPinningUserActionInference = bypassState.userActionInference
                screenPinningActivationDurationMs = bypassState.activationDurationMs
                examSessionCancelledByPinningFailure = false
                lockTaskRequestPending = false
                clearAppSwitchSuppression()
                screenPinningMessage = null
                webViewErrorMessage = null
                exitOnSecurityIssueDialogDismiss = false
                resetPreparationSecurityEpisodes()
                coroutineScope.launch {
                    if (!prepareCleanExamWebViewSessionForStart()) {
                        return@launch
                    }
                    if (!examGuardArmed) {
                        armExamRuntimeMonitoring(reason = "start_exam_pressed")
                    }
                    finalizeExamSessionStart(lockTaskAlreadyActive = false)
                }
                return
            }

            if (!examGuardArmed) {
                armExamRuntimeMonitoring(reason = "start_exam_pressed")
            }

            val requestState = ScreenPinningEnforcer.launchState(screenPinningMode, lockTaskBridge)
            lockTaskStateBeforePinningRequest = requestState.beforeState
            lockTaskStateAfterPinningRequest = requestState.afterState
            screenPinningRequestOutcome = requestState.outcome
            screenPinningDialogLikelyShown = requestState.dialogLikelyShown
            screenPinningUserActionInference = requestState.userActionInference
            screenPinningActivationDurationMs = requestState.activationDurationMs
            examSessionCancelledByPinningFailure = false
            lockTaskRequestPending = true
            recordAction(code = requestState.eventCode, details = requestState.eventDetails)
            setAppSwitchSuppression(AppSwitchSuppressionReason.ScreenPinningRequest)
            screenPinningMessage = null
            webViewErrorMessage = null
            exitOnSecurityIssueDialogDismiss = false
        }

        fun startExamSession() {
            if (webViewSessionResetInFlight) {
                return
            }
            val startExamPressedAt = SystemClock.elapsedRealtime()
            webViewSessionResetError = null
            recordAction(code = "START_EXAM_PRESSED")
            debugMeasureExamStartWork("startExamSession:tampers") {
                refreshReverseEngineeringStatus()
                refreshIntegrityGuard()
            }
            val securityTamperDetectedNow = tamperDetected || integrityTamperDetected
            if (securityTamperDetectedNow) {
                recordAction(
                    code = "START_EXAM_BLOCKED_TAMPER",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Security Check Failed",
                    "Pemeriksaan Keamanan Gagal"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "Security checks failed. Close debugging or hooking tools and reopen the app.",
                    "Pemeriksaan keamanan gagal. Tutup tool debugging/hooking lalu buka ulang aplikasi."
                )
                return
            }
            refreshScreenPinningDiagnostics()
            val latestAccessibilityGuardAvailable = isExamGuardAccessibilityAvailable(context)
            val latestAccessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
            accessibilityGuardEnabled = latestAccessibilityGuardEnabled
            if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                latestAccessibilityGuardAvailable &&
                !latestAccessibilityGuardEnabled
            ) {
                recordAction(
                    code = "ACCESSIBILITY_GUARD_MISSING_BLOCKED",
                    details = "screen_pinning_available=false | accessibility_guard_available=true | accessibility_guard_enabled=false | bypass=false",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Accessibility Exam Guard Required",
                    "Accessibility Exam Guard Diperlukan"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "This device does not support Screen Pinning. Enable CBX Lock Exam Guard in Accessibility Settings, or use the Secret Admin Screen Pinning bypass.",
                    "Perangkat ini tidak mendukung Screen Pinning. Aktifkan CBX Lock Exam Guard di Pengaturan Aksesibilitas, atau gunakan bypass Screen Pinning melalui Secret Admin."
                )
                return
            } else if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                !latestAccessibilityGuardAvailable
            ) {
                recordAction(
                    code = "START_EXAM_BLOCKED_SCREEN_PINNING_UNAVAILABLE",
                    details = "screen_pinning_available=false | accessibility_guard_available=false | bypass=false",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Screen Pinning Unavailable",
                    "Screen Pinning Tidak Tersedia"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "This device does not support Screen Pinning. Use a supported device or Secret Admin Screen Pinning bypass.",
                    "Perangkat ini tidak mendukung Screen Pinning. Gunakan perangkat yang mendukung atau bypass Screen Pinning melalui Secret Admin."
                )
                return
            } else if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                latestAccessibilityGuardEnabled
            ) {
                recordAction(
                    code = "ACCESSIBILITY_GUARD_ENABLED_REQUIRED",
                    details = "screen_pinning_available=false | accessibility_guard_enabled=true",
                    level = DiagnosticEventLevel.INFO
                )
            }
            debugMeasureExamStartWork("startExamSession:device_prechecks") {
                refreshScreenPinningDiagnostics()
                accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
                refreshKeyboardSecurity(triggerViolation = false)
                refreshBluetoothSecurity(triggerViolation = false)
                refreshDeviceIntegritySecurity(triggerViolation = false)
            }
            if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                isExamGuardAccessibilityAvailable(context) &&
                !accessibilityGuardEnabled
            ) {
                recordAction(
                    code = "ACCESSIBILITY_GUARD_MISSING_BLOCKED",
                    details = "screen_pinning_available=false | accessibility_guard_available=true | accessibility_guard_enabled=false | bypass=false | phase=device_prechecks",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Accessibility Exam Guard Required",
                    "Accessibility Exam Guard Diperlukan"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "This device does not support Screen Pinning. Enable CBX Lock Exam Guard in Accessibility Settings, or use the Secret Admin Screen Pinning bypass.",
                    "Perangkat ini tidak mendukung Screen Pinning. Aktifkan CBX Lock Exam Guard di Pengaturan Aksesibilitas, atau gunakan bypass Screen Pinning melalui Secret Admin."
                )
                return
            } else if (
                screenPinningMode == ScreenPinningMode.Enforced &&
                !screenPinningAvailable &&
                !isExamGuardAccessibilityAvailable(context)
            ) {
                recordAction(
                    code = "START_EXAM_BLOCKED_SCREEN_PINNING_UNAVAILABLE",
                    details = "screen_pinning_available=false | accessibility_guard_available=false | bypass=false | phase=device_prechecks",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Screen Pinning Unavailable",
                    "Screen Pinning Tidak Tersedia"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "This device does not support Screen Pinning. Use a supported device or Secret Admin Screen Pinning bypass.",
                    "Perangkat ini tidak mendukung Screen Pinning. Gunakan perangkat yang mendukung atau bypass Screen Pinning melalui Secret Admin."
                )
                return
            }
            val startDeviceTimeStatus = refreshDeviceTimeSecurity(trigger = "start_exam_precheck")
            if (startDeviceTimeStatus.blocking) {
                recordAction(
                    code = "START_EXAM_BLOCKED_DEVICE_TIME",
                    details = buildDeviceTimeEventDetails("start_exam_precheck", startDeviceTimeStatus),
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = deviceTimeBlockedTitle(uiLanguage)
                securityIssueDialogMessage = deviceTimeBlockedMessage(uiLanguage, startDeviceTimeStatus)
                return
            }
            val signatureResult = debugMeasureExamStartWork("startExamSession:signature_check") {
                checkSignatureIntegrity(triggerViolation = true)
            }
            if (ExamPolicyEngine.shouldBlock(signatureResult)) {
                recordAction(
                    code = "START_EXAM_BLOCKED_SIGNATURE",
                    level = DiagnosticEventLevel.WARNING
                )
                return
            }
            val keyboardAllowedNow = lastKeyboardAllowed
            val builtInKeyboardNeeded = !bypassKeyboardPolicy && !keyboardAllowedNow
            val bluetoothPermissionReady =
                bluetoothPermissionGranted || !requiresBluetoothExamPermission()

            useBuiltInExamKeyboard = builtInKeyboardNeeded
            showBuiltInExamKeyboard = builtInKeyboardNeeded
            builtInKeyboardShiftEnabled = false

            if (!bypassBluetooth) {
                if (!bluetoothPermissionReady) {
                    bluetoothPermissionLauncher.launch(getBluetoothConnectPermission())
                    return
                }

                if (bluetoothEnabled) {
                    showBluetoothViolationDialog = true
                    return
                }
            }

            if (!bypassAccessibility && accessibilityServiceEnabled) {
                recordAction(
                    code = "START_EXAM_BLOCKED_ACCESSIBILITY",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = "Accessibility Service Masih Aktif"
                securityIssueDialogMessage =
                    "Nonaktifkan accessibility service sebelum memulai ujian."
                return
            }

            if (!bypassAdb && developerOptionsEnabled) {
                recordAction(
                    code = "START_EXAM_BLOCKED_DEVELOPER_OPTIONS",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = "Developer Mode Masih Aktif"
                securityIssueDialogMessage =
                    "Nonaktifkan Developer Mode sebelum memulai ujian."
                return
            }

            if (!bypassVirtualEnvironment && virtualEnvironmentDetected) {
                recordAction(
                    code = "START_EXAM_BLOCKED_VIRTUAL_ENV",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = "Virtual Environment Terdeteksi"
                securityIssueDialogMessage =
                    "Perangkat ini terdeteksi berjalan di emulator/VM. Gunakan perangkat fisik untuk melanjutkan ujian."
                return
            }

            if (!bypassAdb && adbEnabled) {
                recordAction(
                    code = "START_EXAM_BLOCKED_ADB",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = "USB Debugging (ADB) Masih Aktif"
                securityIssueDialogMessage =
                    "USB debugging terdeteksi aktif. Nonaktifkan ADB sebelum memulai ujian."
                return
            }

            val latestRootSecurityStatus = rootSecurityStatus
            if (!bypassRoot && latestRootSecurityStatus.detected) {
                recordAction(
                    code = "START_EXAM_BLOCKED_ROOT",
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = "Root Device Terdeteksi"
                securityIssueDialogMessage = buildRootIssueMessage(latestRootSecurityStatus.details)
                return
            }

            if (geofenceEnabled && !bypassGeofence && geofenceConfigParseResult.config == null) {
                geofenceSecurityStatus = evaluateGeofenceSecurity(
                    configResult = geofenceConfigParseResult,
                    permissionGranted = hasLocationPermissionForWifi(context),
                    preciseLocationGranted = hasFineLocationPermission(context),
                    locationServicesEnabled = isLocationServicesEnabled(context),
                    locationSnapshot = null,
                    bypassState = geofenceBypassState
                )
                geofenceEvaluation = geofenceSecurityStatus.geofenceEvaluation
                recordAction(
                    code = "START_EXAM_BLOCKED_GEOFENCE_CONFIG",
                    details = currentGeofenceEventDetails(
                        trigger = "start_exam",
                        geofenceStatus = geofenceSecurityStatus
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Geofence Configuration Invalid",
                    "Konfigurasi Geofence Tidak Valid"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "Check the geofence latitude, longitude, and radius in the Custom QR before starting the exam.",
                    "Periksa latitude, longitude, dan radius geofence di Custom QR sebelum memulai ujian."
                )
                return
            }

            val coarseOrFineGranted = hasLocationPermissionForWifi(context)
            val preciseLocationGranted = hasFineLocationPermission(context)
            val preciseLocationRequiredForStart = geofenceEnabled && !bypassGeofence
            if (
                (preciseLocationRequiredForStart && !preciseLocationGranted) ||
                (!preciseLocationRequiredForStart && !bypassFakeLocation && !coarseOrFineGranted)
            ) {
                pendingStartExamAfterLocationPermission = true
                geofencePermissionRequestInFlight = true
                recordAction(
                    code = "LOCATION_PERMISSION_REQUESTED",
                    details = "trigger=start_exam",
                    level = DiagnosticEventLevel.WARNING
                )
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                debugLogExamStart(
                    "startExamSession waiting for location permission after ${SystemClock.elapsedRealtime() - startExamPressedAt} ms"
                )
                return
            }

            if (!bypassGeofence && geofenceEnabled && !isLocationServicesEnabled(context)) {
                geofenceSecurityStatus = evaluateGeofenceSecurity(
                    configResult = geofenceConfigParseResult,
                    permissionGranted = true,
                    preciseLocationGranted = true,
                    locationServicesEnabled = false,
                    locationSnapshot = null,
                    bypassState = geofenceBypassState
                )
                geofenceEvaluation = geofenceSecurityStatus.geofenceEvaluation
                recordAction(
                    code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_DISABLED",
                    details = currentGeofenceEventDetails(
                        trigger = "start_exam",
                        geofenceStatus = geofenceSecurityStatus
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Location Services Disabled",
                    "Layanan Lokasi Nonaktif"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "Turn on location services before starting the exam.",
                    "Aktifkan layanan lokasi sebelum memulai ujian."
                )
                return
            }

            if (!bypassFakeLocation && !isLocationServicesEnabled(context)) {
                fakeLocationSecurityStatus = evaluateFakeLocationSecurity(
                    monitoringEnabled = true,
                    permissionGranted = hasLocationPermissionForWifi(context),
                    locationServicesEnabled = false,
                    locationSnapshot = null,
                    fixQualityStatus = evaluateLocationFixQuality(null),
                    developerOptionsEnabled = developerOptionsEnabled,
                    suspiciousFakeLocationPackages = detectSuspiciousFakeLocationPackages(context),
                    bypassState = fakeLocationBypassState
                )
                recordAction(
                    code = "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_DISABLED",
                    details = currentFakeLocationEventDetails(
                        trigger = "start_exam",
                        fakeLocationStatus = fakeLocationSecurityStatus
                    ),
                    level = DiagnosticEventLevel.WARNING
                )
                securityIssueDialogTitle = localized(
                    uiLanguage,
                    "Location Services Disabled",
                    "Layanan Lokasi Nonaktif"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "Turn on location services so anti-fake-location can validate the exam before it starts.",
                    "Aktifkan layanan lokasi agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                )
                return
            }

            if (geofenceStartValidationInFlight) {
                return
            }

            geofenceStartValidationInFlight = true
            coroutineScope.launch {
                val latestLocationStatus = debugMeasureExamStartSuspendWork("startExamSession:location_validation") {
                    resolveStartExamLocationValidation()
                }
                geofenceStartValidationInFlight = false
                when {
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.Outside -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_OUTSIDE",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Outside Allowed Exam Area",
                            "Di Luar Area Ujian"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "This device is outside the allowed exam radius. Move into the approved area before starting the exam.",
                            "Perangkat ini berada di luar radius ujian yang diizinkan. Masuk ke area yang disetujui sebelum memulai ujian."
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.PermissionMissing -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_PERMISSION",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Permission Required",
                            "Izin Lokasi Diperlukan"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "Location permission must be granted before the exam can start.",
                            "Izin lokasi harus diberikan sebelum ujian bisa dimulai."
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.PreciseRequired -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_PRECISE_REQUIRED",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Precise Location Required",
                            "Lokasi Presisi Diperlukan"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "Precise location must be granted before the exam can start.",
                            "Lokasi presisi harus diberikan sebelum ujian bisa dimulai."
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.LocationDisabled -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_DISABLED",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Services Disabled",
                            "Layanan Lokasi Nonaktif"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "Turn on location services before starting the exam.",
                            "Aktifkan layanan lokasi sebelum memulai ujian."
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.NoFix -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Not Available",
                            "Lokasi Belum Tersedia"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "The device location could not be validated yet. Wait for a location fix, then try again.",
                            "Lokasi perangkat belum bisa divalidasi. Tunggu hingga lokasi tersedia lalu coba lagi."
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.StaleFix -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Fix Too Old",
                            "Fix Lokasi Terlalu Lama"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "The latest location fix is too old. Wait for a fresh location update, then try again.",
                            "Fix lokasi terbaru terlalu lama. Tunggu pembaruan lokasi yang baru lalu coba lagi."
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.LowAccuracy -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Accuracy Too Low",
                            "Akurasi Lokasi Terlalu Rendah"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "The current location accuracy is still too weak for strict geofence validation. Wait for a better fix, then try again.",
                            "Akurasi lokasi saat ini masih terlalu lemah untuk validasi geofence ketat. Tunggu fix yang lebih baik lalu coba lagi."
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.MissingAccuracy -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_LOCATION_UNAVAILABLE",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Accuracy Missing",
                            "Akurasi Lokasi Belum Ada"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "The current location fix does not include a usable accuracy value yet. Wait for a better fix, then try again.",
                            "Fix lokasi saat ini belum memiliki nilai akurasi yang bisa dipakai. Tunggu fix yang lebih baik lalu coba lagi."
                        )
                    }
                    !bypassFakeLocation &&
                        latestLocationStatus.fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.PermissionRequired -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_FAKE_LOCATION_PERMISSION",
                            details = currentFakeLocationEventDetails(
                                trigger = "start_exam",
                                fakeLocationStatus = latestLocationStatus.fakeLocationStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Permission Required",
                            "Izin Lokasi Diperlukan"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "Location access is required so anti-fake-location can validate the exam before it starts.",
                            "Akses lokasi wajib tersedia agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                        )
                    }
                    !bypassFakeLocation &&
                        latestLocationStatus.fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationServicesDisabled -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_DISABLED",
                            details = currentFakeLocationEventDetails(
                                trigger = "start_exam",
                                fakeLocationStatus = latestLocationStatus.fakeLocationStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Services Disabled",
                            "Layanan Lokasi Nonaktif"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "Turn on location services so anti-fake-location can validate the exam before it starts.",
                            "Aktifkan layanan lokasi agar anti-fake-location bisa memvalidasi ujian sebelum dimulai."
                        )
                    }
                    !bypassFakeLocation &&
                        latestLocationStatus.fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.LocationUnavailable -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_FAKE_LOCATION_LOCATION_UNAVAILABLE",
                            details = currentFakeLocationEventDetails(
                                trigger = "start_exam",
                                fakeLocationStatus = latestLocationStatus.fakeLocationStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            "Location Not Available",
                            "Lokasi Belum Tersedia"
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            "Anti-fake-location is still waiting for a usable location snapshot. Refresh the location, then try again.",
                            "Anti-fake-location masih menunggu snapshot lokasi yang bisa dipakai. Refresh lokasi lalu coba lagi."
                        )
                    }
                    !bypassFakeLocation &&
                        latestLocationStatus.fakeLocationStatus.finalVerdict == LocationSpoofSecurityVerdict.SpoofDetected -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_FAKE_LOCATION_SPOOF",
                            details = currentFakeLocationEventDetails(
                                trigger = "start_exam",
                                fakeLocationStatus = latestLocationStatus.fakeLocationStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                            if (latestLocationStatus.fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                                "Critical Fake Location Detected"
                            } else {
                                "Mock Location Detected"
                            },
                            if (latestLocationStatus.fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                                "Fake Location Kritis Terdeteksi"
                            } else {
                                "Lokasi Palsu Terdeteksi"
                            }
                        )
                        securityIssueDialogMessage = localized(
                            uiLanguage,
                            if (latestLocationStatus.fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                                "Critical combined fake-location signals were detected. Disable Fake GPS, mock providers, or related developer tools before starting the exam."
                            } else {
                                "Location spoofing or mock-location signals were detected. Disable Fake GPS or developer mock providers before starting the exam."
                            },
                            if (latestLocationStatus.fakeLocationStatus.confidenceTier == LocationSpoofConfidenceTier.Critical) {
                                "Terdeteksi kombinasi sinyal fake-location kritis. Nonaktifkan Fake GPS, mock provider, atau alat developer terkait sebelum memulai ujian."
                            } else {
                                "Terdeteksi sinyal spoofing lokasi atau mock location. Nonaktifkan Fake GPS atau mock provider developer sebelum memulai ujian."
                            }
                        )
                    }
                    !bypassGeofence &&
                        latestLocationStatus.geofenceStatus.finalVerdict == GeofenceSecurityVerdict.ConfigInvalid -> {
                        recordAction(
                            code = "START_EXAM_BLOCKED_GEOFENCE_CONFIG",
                            details = currentGeofenceEventDetails(
                                trigger = "start_exam",
                                geofenceStatus = latestLocationStatus.geofenceStatus
                            ),
                            level = DiagnosticEventLevel.WARNING
                        )
                        securityIssueDialogTitle = localized(
                            uiLanguage,
                    "Geofence Configuration Invalid",
                    "Konfigurasi Geofence Tidak Valid"
                )
                securityIssueDialogMessage = localized(
                    uiLanguage,
                    "Check the geofence latitude, longitude, and radius in the Custom QR before starting the exam.",
                    "Periksa latitude, longitude, dan radius geofence di Custom QR sebelum memulai ujian."
                )
                    }
                    else -> {
                        val finalDeviceTimeStatus = refreshDeviceTimeSecurity(
                            trigger = "start_exam_final",
                            emitDiagnosticEvent = false
                        )
                        if (finalDeviceTimeStatus.blocking) {
                            recordAction(
                                code = "START_EXAM_BLOCKED_DEVICE_TIME",
                                details = buildDeviceTimeEventDetails("start_exam_final", finalDeviceTimeStatus),
                                level = DiagnosticEventLevel.WARNING
                            )
                            securityIssueDialogTitle = deviceTimeBlockedTitle(uiLanguage)
                            securityIssueDialogMessage = deviceTimeBlockedMessage(uiLanguage, finalDeviceTimeStatus)
                            return@launch
                        }
                        val networkNowMillis = TrustedNetworkTimeCoordinator.currentNetworkNowMillis(context)
                        val scheduleValidationResult = ExamScheduleValidator.validateAfterDeviceTimeCheck(
                            payload = payload,
                            deviceTimeStatus = finalDeviceTimeStatus,
                            networkNowMillis = networkNowMillis
                        )
                        if (scheduleValidationResult != ExamScheduleValidationResult.Valid) {
                            recordAction(
                                code = "START_EXAM_BLOCKED_DEVICE_TIME",
                                details =
                                    "schedule_result=${scheduleValidationResult.name.lowercase(Locale.US)} | " +
                                        "network_now_ms=${networkNowMillis ?: "unavailable"} | " +
                                        buildDeviceTimeEventDetails("start_exam_schedule", finalDeviceTimeStatus),
                                level = DiagnosticEventLevel.WARNING
                            )
                            securityIssueDialogTitle =
                                if (scheduleValidationResult == ExamScheduleValidationResult.TimeSpoofDetected) {
                                    deviceTimeBlockedTitle(uiLanguage)
                                } else {
                                    localized(
                                        uiLanguage,
                                        "Exam Schedule Not Active",
                                        "Jadwal Ujian Tidak Aktif"
                                    )
                                }
                            securityIssueDialogMessage = scheduleBlockedMessage(
                                uiLanguage = uiLanguage,
                                payload = payload,
                                validationResult = scheduleValidationResult
                            )
                            return@launch
                        }
                        debugLogExamStart(
                            "startExamSession passed all prechecks in ${SystemClock.elapsedRealtime() - startExamPressedAt} ms"
                        )
                        completeStartExamSessionAfterPrechecks()
                    }
                }
            }
        }

    }
    val startExamController = StartExamController()

    LaunchedEffect(retryStartExamAfterLocationPermissionGrant) {
        if (retryStartExamAfterLocationPermissionGrant) {
            retryStartExamAfterLocationPermissionGrant = false
            startExamController.startExamSession()
        }
    }

    fun sendBuiltInKeyboardText(rawText: String) {
        val text =
            if (builtInKeyboardShiftEnabled) {
                rawText.uppercase(Locale.US)
            } else {
                rawText
            }
        webViewInstance?.evaluateJavascript(buildExamKeyboardInsertScript(text), null)
        hideSystemKeyboard()
        if (builtInKeyboardShiftEnabled && rawText.any { it.isLetter() }) {
            builtInKeyboardShiftEnabled = false
        }
    }

    fun sendBuiltInKeyboardBackspace() {
        webViewInstance?.evaluateJavascript(ExamKeyboardBackspaceScript, null)
        hideSystemKeyboard()
    }

    fun sendKeyboardArrowLeft() {
        val webView = webViewInstance ?: return
        webView.requestFocus()
        webView.evaluateJavascript(ExamKeyboardArrowLeftScript) { result ->
            if (result?.trim() != "true") {
                webView.sendExamArrowKeyFallback(KeyEvent.KEYCODE_DPAD_LEFT)
            }
        }
    }

    fun sendKeyboardArrowRight() {
        val webView = webViewInstance ?: return
        webView.requestFocus()
        webView.evaluateJavascript(ExamKeyboardArrowRightScript) { result ->
            if (result?.trim() != "true") {
                webView.sendExamArrowKeyFallback(KeyEvent.KEYCODE_DPAD_RIGHT)
            }
        }
    }

    fun sendBuiltInKeyboardEnter() {
        webViewInstance?.evaluateJavascript(ExamKeyboardEnterScript, null)
        hideSystemKeyboard()
    }

    RuntimeSetupEffects(
        context = context,
        mainActivity = mainActivity,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        examSessionStarted = examSessionStarted,
        nativeExamFullscreenActive = nativeExamFullscreenActive,
        webViewInstance = webViewInstance,
        nativeFullscreenBridge = nativeFullscreenBridge,
        refreshScreenPinningDiagnostics = ::refreshScreenPinningDiagnostics,
        refreshKeyboardSecurity = ::refreshKeyboardSecurity,
        refreshBluetoothSecurity = ::refreshBluetoothSecurity,
        refreshDeviceIntegritySecurity = ::refreshDeviceIntegritySecurity,
        updateBluetoothPermissionGranted = { bluetoothPermissionGranted = it },
        updateUseBuiltInExamKeyboard = { useBuiltInExamKeyboard = it },
        updateShowBuiltInExamKeyboard = { showBuiltInExamKeyboard = it },
        cleanupActiveExamWebViewInstance = ::cleanupActiveExamWebViewInstance
    )

    PreparationLocationWarmupEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        geofenceEnabled = geofenceEnabled,
        warmLocationPolicySignature = warmLocationPolicySignature,
        geofenceBypassState = geofenceBypassState,
        fakeLocationBypassState = fakeLocationBypassState,
        geofencePermissionRequestInFlight = geofencePermissionRequestInFlight,
        geofenceStartValidationInFlight = geofenceStartValidationInFlight,
        geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
        webViewSessionResetInFlight = webViewSessionResetInFlight,
        locationWarmupInFlight = locationWarmupInFlight,
        warmupIntervalMillis = PreparationLocationWarmupIntervalMillis *
            lowRamProfile.slowPollingMultiplier,
        updateLocationWarmupInFlight = { locationWarmupInFlight = it },
        updateReusableWarmLocationValidation = { reusableWarmLocationValidation = it },
        updateLastGeofenceRefreshAt = { lastGeofenceRefreshAt = it },
        refreshGeofenceStatus = ::refreshGeofenceStatus
    )

    BypassTamperLoggingEffects(
        adminSettings = adminSettings,
        screenPinningBypassTamperLogged = screenPinningBypassTamperLogged,
        updateScreenPinningBypassTamperLogged = { screenPinningBypassTamperLogged = it },
        accessibilityBypassTamperLogged = accessibilityBypassTamperLogged,
        updateAccessibilityBypassTamperLogged = { accessibilityBypassTamperLogged = it },
        adbBypassTamperLogged = adbBypassTamperLogged,
        updateAdbBypassTamperLogged = { adbBypassTamperLogged = it },
        clipboardBypassTamperLogged = clipboardBypassTamperLogged,
        updateClipboardBypassTamperLogged = { clipboardBypassTamperLogged = it },
        overlayBypassTamperLogged = overlayBypassTamperLogged,
        updateOverlayBypassTamperLogged = { overlayBypassTamperLogged = it },
        geofenceBypassTamperLogged = geofenceBypassTamperLogged,
        updateGeofenceBypassTamperLogged = { geofenceBypassTamperLogged = it },
        fakeLocationBypassTamperLogged = fakeLocationBypassTamperLogged,
        updateFakeLocationBypassTamperLogged = { fakeLocationBypassTamperLogged = it },
        deviceTimeBypassTamperLogged = deviceTimeBypassTamperLogged,
        updateDeviceTimeBypassTamperLogged = { deviceTimeBypassTamperLogged = it },
        appSwitchBypassTamperLogged = appSwitchBypassTamperLogged,
        updateAppSwitchBypassTamperLogged = { appSwitchBypassTamperLogged = it },
        rootBypassTamperLogged = rootBypassTamperLogged,
        updateRootBypassTamperLogged = { rootBypassTamperLogged = it },
        recordAction = ::recordAction
    )

    DisposableEffect(mainActivity) {
        onDispose {
            mainActivity?.setExamLockMode(enabled = false, allowLockTask = false)
        }
    }

    RuntimeAppSwitchFallbackLoggingEffect(
        examGuardArmed = examGuardArmed,
        appSwitchStatus = appSwitchStatus,
        screenPinningMode = screenPinningMode,
        appSwitchFallbackArmedLogged = appSwitchFallbackArmedLogged,
        updateAppSwitchFallbackArmedLogged = { appSwitchFallbackArmedLogged = it },
        recordAction = ::recordAction
    )

    AccessibilityExamGuardViolationEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        onViolation = ::handleAccessibilityGuardViolation
    )

    AccessibilityExamGuardLivenessEffect(
        context = context,
        examSessionStarted = examSessionStarted,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        recordAction = ::recordAction
    )

    RuntimeDisposeCleanupEffect(
        examSessionStarted = examSessionStarted,
        lockTaskRequestPending = lockTaskRequestPending,
        lockTaskBridge = lockTaskBridge,
        cleanupActiveExamWebViewInstance = ::cleanupActiveExamWebViewInstance,
        launchExitSessionClearBestEffort = {
            launchExitSessionClearBestEffort("runtime_dispose")
        },
        disarmAccessibilityGuard = { AccessibilityExamGuardStore.disarm(context) },
        stopAlarm = examAlarmController::stop
    )

    RuntimeScreenPinningActivationEffect(
        mainActivity = mainActivity,
        lockTaskBridge = lockTaskBridge,
        isIndonesian = isIndonesian,
        flowUiState = flowUiState,
        adminUiState = adminUiState,
        coroutineScope = coroutineScope,
        recordAction = ::recordAction,
        clearAppSwitchSuppression = ::clearAppSwitchSuppression,
        disarmExamRuntimeMonitoring = ::disarmExamRuntimeMonitoring,
        resetPreparationSecurityEpisodes = startExamController::resetPreparationSecurityEpisodes,
        prepareCleanExamWebViewSessionForStart = startExamController::prepareCleanExamWebViewSessionForStart,
        finalizeExamSessionStart = startExamController::finalizeExamSessionStart
    )

    RuntimeScreenPinningMonitorEffect(
        mainActivity = mainActivity,
        screenPinningMode = screenPinningMode,
        examSessionStarted = examSessionStarted,
        examSessionStartedAtElapsedMs = examSessionStartedAtElapsedMs,
        lockTaskRequestPending = lockTaskRequestPending,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        exitOnSecurityIssueDialogDismiss = exitOnSecurityIssueDialogDismiss,
        lockTaskBridge = lockTaskBridge,
        isIndonesian = isIndonesian,
        currentScreenPinningMonitorIntervalMillis = ::currentScreenPinningMonitorIntervalMillis,
        applyFatalSecuritySignal = ::applyFatalSecuritySignal
    )

    RuntimePrimaryGuardEffects(
        mainActivity = mainActivity,
        examGuardArmed = examGuardArmed,
        overlayBypassState = overlayBypassState,
        clipboardBypassState = clipboardBypassState,
        bypassClipboard = bypassClipboard,
        appSwitchRuntimeMonitoringActive = appSwitchStatus.runtimeMonitoringActive,
        appSwitchProtectionMode = appSwitchStatus.protectionMode,
        appSwitchLockTaskActive = appSwitchStatus.lockTaskActive,
        accessibilityGuardFallbackActive = accessibilityGuardFallbackActive,
        accessibilityGuardEnabled = accessibilityGuardEnabled,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        adminUiState = adminUiState,
        examAlarmController = examAlarmController,
        fullScreenCustomView = fullScreenCustomView,
        showOfflineWarningDialog = showOfflineWarningDialog,
        showExitExamDialog = showExitExamDialog,
        pendingSection = pendingSection,
        securityIssueDialogMessage = securityIssueDialogMessage,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        currentAppSwitchSuppressionReason = ::currentAppSwitchSuppressionReason,
        currentAppSwitchEventDetails = { signal, suppressionReason ->
            currentAppSwitchEventDetails(
                signal = signal,
                suppressionReason = suppressionReason
            )
        },
        currentOverlayEventDetails = { signal, extraContext ->
            currentOverlayEventDetails(
                signal = signal,
                extraContext = extraContext
            )
        },
        currentInternalDialogReason = ::currentInternalDialogReason,
        recordAction = ::recordAction,
        recordAppSwitchEvent = ::recordAppSwitchEvent,
        recordOverlayEvent = ::recordOverlayEvent,
        armClipboardResumeCheck = ::armClipboardResumeCheck
    )

    RuntimeHostActivityLifecycleEffect(
        context = context,
        componentActivity = componentActivity,
        coroutineScope = coroutineScope,
        examAlarmController = examAlarmController,
        examGuardArmed = examGuardArmed,
        geofenceEnabled = geofenceEnabled,
        clipboardBypassState = clipboardBypassState,
        bypassClipboard = bypassClipboard,
        appSwitchRuntimeMonitoringActive = appSwitchStatus.runtimeMonitoringActive,
        appSwitchSuppressionReason = appSwitchSuppressionReason,
        appSwitchSuppressedUntilElapsedMs = appSwitchSuppressedUntilElapsedMs,
        accessibilityGuardEnabledState = accessibilityGuardEnabledState,
        accessibilityGuardFallbackActiveState = accessibilityGuardFallbackActiveState,
        accessibilityGuardLastReasonState = accessibilityGuardLastReasonState,
        accessibilityGuardLastForeignPackageState = accessibilityGuardLastForeignPackageState,
        accessibilityGuardLastEventTypeState = accessibilityGuardLastEventTypeState,
        accessibilityGuardLastDetectedAtState = accessibilityGuardLastDetectedAtState,
        accessibilityGuardAlarmSeverityState = accessibilityGuardAlarmSeverityState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        adminUiState = adminUiState,
        currentAppSwitchSuppressionReason = ::currentAppSwitchSuppressionReason,
        currentAppSwitchEventDetails = ::currentAppSwitchEventDetails,
        recordAction = ::recordAction,
        recordAppSwitchEvent = ::recordAppSwitchEvent,
        armClipboardResumeCheck = ::armClipboardResumeCheck,
        refreshReverseEngineeringStatus = ::refreshReverseEngineeringStatus,
        refreshKeyboardSecurity = ::refreshKeyboardSecurity,
        refreshBluetoothSecurity = ::refreshBluetoothSecurity,
        refreshDeviceIntegritySecurity = ::refreshDeviceIntegritySecurity,
        refreshDeviceTimeSecurity = { trigger ->
            refreshDeviceTimeSecurity(trigger = trigger)
        },
        refreshGeofenceStatus = { preferFresh, trigger, allowRuntimeViolation ->
            refreshGeofenceStatus(
                preferFresh = preferFresh,
                trigger = trigger,
                allowRuntimeViolation = allowRuntimeViolation
            )
            Unit
        },
        confirmClipboardViolation = { snapshot, decision, eventSuffix, updateObservedSnapshot, baselineSemanticSignatureOverride ->
            confirmClipboardViolation(
                snapshot = snapshot,
                decision = decision,
                eventSuffix = eventSuffix,
                updateObservedSnapshot = updateObservedSnapshot,
                baselineSemanticSignatureOverride = baselineSemanticSignatureOverride
            )
        },
        diagnosticTimestamp = ::diagnosticTimestamp
    )

    RuntimeLocationAndClipboardEffects(
        context = context,
        deviceTimeBaseline = deviceTimeBaseline,
        deviceTimeBypassState = deviceTimeBypassState,
        geofenceConfigParseResult = geofenceConfigParseResult,
        geofenceEnabled = geofenceEnabled,
        bypassGeofence = bypassGeofence,
        bypassFakeLocation = bypassFakeLocation,
        examGuardArmed = examGuardArmed,
        bypassClipboard = bypassClipboard,
        clipboardBypassState = clipboardBypassState,
        bypassBluetooth = bypassBluetooth,
        flowUiState = flowUiState,
        securityUiState = securityUiState,
        clipboardUiState = clipboardUiState,
        clipboardMainHandler = clipboardMainHandler,
        refreshDeviceTimeSecurity = ::refreshDeviceTimeSecurity,
        refreshGeofenceStatus = { preferFresh, trigger, allowRuntimeViolation ->
            refreshGeofenceStatus(
                preferFresh = preferFresh,
                trigger = trigger,
                allowRuntimeViolation = allowRuntimeViolation
            )
            Unit
        },
        confirmClipboardViolation = { snapshot, decision, eventSuffix, updateObservedSnapshot, baselineSemanticSignatureOverride ->
            confirmClipboardViolation(
                snapshot = snapshot,
                decision = decision,
                eventSuffix = eventSuffix,
                updateObservedSnapshot = updateObservedSnapshot,
                baselineSemanticSignatureOverride = baselineSemanticSignatureOverride
            )
        },
        examAlarmController = examAlarmController,
        diagnosticTimestamp = ::diagnosticTimestamp
    )

    RuntimeConnectivityEffects(
        context = context,
        examSessionStarted = examSessionStarted,
        networkReadinessStatus = networkReadinessStatus,
        baseNetworkReadiness = baseNetworkReadiness,
        networkUiState = networkUiState,
        batteryStatusState = batteryStatusState,
        networkMainHandler = networkMainHandler,
        updateNetworkReadiness = ::updateNetworkReadiness,
        currentNetworkPollingIntervalMillis = ::currentNetworkPollingIntervalMillis,
        recordAction = ::recordAction,
        currentNetworkEventDetails = ::currentNetworkEventDetails,
        clearNetworkFlapHistory = { networkFlapElapsedMs.clear() },
        diagnosticTimestamp = ::diagnosticTimestamp
    )

    BackHandler {
        if (fullScreenCustomView != null) {
            hideCustomView()
            return@BackHandler
        }
        val webView = webViewInstance
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else {
            showExitExamDialog = true
        }
    }

    fun handleChooseKeyboard() {
        recordAction(code = "KEYBOARD_PICKER_OPENED")
        if (!showKeyboardPicker(activity)) {
            openKeyboardSettings(context)
        }
    }

    fun handleOpenKeyboardSettings() {
        recordAction(code = "KEYBOARD_SETTINGS_OPENED")
        openKeyboardSettings(context)
    }

    fun handleGrantBluetoothPermission() {
        recordAction(code = "BLUETOOTH_PERMISSION_REQUESTED")
        bluetoothPermissionLauncher.launch(getBluetoothConnectPermission())
    }

    fun handleOpenBluetoothSettings() {
        recordAction(code = "BLUETOOTH_SETTINGS_OPENED")
        openBluetoothSettings(context)
    }

    fun handleOpenAccessibilitySettings() {
        recordAction(code = "ACCESSIBILITY_SETTINGS_OPENED")
        openAccessibilitySettings(context)
    }

    fun handleOpenOverlayAccessibilitySettings() {
        recordAction(code = "OVERLAY_ACCESSIBILITY_SETTINGS_OPENED")
        openAccessibilitySettings(context)
    }

    fun handleOpenDeveloperOptionsSettings() {
        recordAction(code = "DEVELOPER_OPTIONS_OPENED")
        openDeveloperOptionsSettings(context)
    }

    fun handleRequestLocationPermission() {
        invalidateWarmLocationValidationCache()
        recordAction(
            code = "LOCATION_PERMISSION_REQUESTED",
            details = "trigger=location_quick_fix",
            level = DiagnosticEventLevel.INFO
        )
        geofencePermissionRequestInFlight = true
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun handleOpenLocationServicesSettings() {
        invalidateWarmLocationValidationCache()
        recordAction(
            code = "LOCATION_SERVICES_SETTINGS_OPENED",
            details = "trigger=location_quick_fix",
            level = DiagnosticEventLevel.INFO
        )
        openLocationServicesSettings(context)
    }

    fun handleRefreshLocationSecurity() {
        launchLocationSecurityManualRefresh(trigger = "location_quick_fix")
        recordAction(
            code = "LOCATION_QUICK_FIX_REFRESH_REQUESTED",
            details = "trigger=checklist",
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenGeofenceMapViewer() {
        showGeofenceMapViewer = true
        recordAction(
            code = "GEOFENCE_MAP_VIEW_OPENED",
            details = currentGeofenceEventDetails(
                trigger = "checklist_map_view",
                geofenceStatus = geofenceSecurityStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenInternetSettings() {
        recordAction(
            code = "INTERNET_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openInternetConnectivitySettings(context)
    }

    fun handleOpenDateTimeSettings() {
        recordAction(
            code = "DEVICE_TIME_SETTINGS_OPENED",
            details = buildDeviceTimeEventDetails(
                trigger = "device_time_quick_fix",
                status = refreshDeviceTimeSecurity(
                    trigger = "device_time_quick_fix",
                    emitDiagnosticEvent = false
                )
            ),
            level = DiagnosticEventLevel.INFO
        )
        openDateTimeSettings(context)
    }

    fun handleOpenWifiSettings() {
        recordAction(
            code = "WIFI_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_wifi_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openWifiSettings(context)
    }

    fun handleOpenCellularSettings() {
        recordAction(
            code = "CELLULAR_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_cellular_quick_fix",
                status = networkReadinessStatus,
                extraContext = "last_connected=" + (lastConnectedNetworkLabel?.ifBlank { "-" } ?: "-")
            ),
            level = DiagnosticEventLevel.INFO
        )
        openCellularSettings(context)
    }

    fun handleOpenAirplaneModeSettings() {
        recordAction(
            code = "AIRPLANE_MODE_SETTINGS_OPENED",
            details = currentNetworkEventDetails(
                trigger = "network_airplane_mode_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openAirplaneModeSettings(context)
    }

    fun handleRefreshNetworkStatus() {
        launchNetworkManualRefresh(trigger = "network_quick_fix")
        recordAction(
            code = "NETWORK_QUICK_FIX_REFRESH_REQUESTED",
            details = currentNetworkEventDetails(
                trigger = "network_quick_fix",
                status = networkReadinessStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
    }

    fun handleOpenFakeLocationDeveloperOptionsSettings() {
        invalidateWarmLocationValidationCache()
        recordAction(
            code = "FAKE_LOCATION_DEVELOPER_OPTIONS_OPENED",
            details = currentFakeLocationEventDetails(
                trigger = "fake_location_developer_options_quick_fix",
                fakeLocationStatus = fakeLocationRuntimeStatus.securityStatus
            ),
            level = DiagnosticEventLevel.INFO
        )
        openDeveloperOptionsSettings(context)
    }

    fun handleOpenScreenPinningSettings() {
        recordAction(code = "SCREEN_PINNING_SETTINGS_OPENED")
        openScreenPinningSettings(context)
    }

    fun handleOpenOverlaySettings() {
        recordAction(code = "OVERLAY_SETTINGS_OPENED")
        openOverlaySettings(context)
    }

    fun handleReinstallOfficialApk() {
        recordAction(code = "OFFICIAL_APK_REINSTALL_OPENED")
        openExternalUrl(context, officialApkUrl)
    }

    fun refreshPreparationStatusChecks() {
        val startedAt = SystemClock.elapsedRealtime()
        if (!networkManualRefreshInFlight) {
            launchNetworkManualRefresh(trigger = "checklist_refresh")
        } else {
            updateNetworkReadiness("checklist_refresh")
        }
        if (!geofenceManualRefreshInFlight) {
            launchLocationSecurityManualRefresh(trigger = "checklist_refresh")
        }
        refreshReverseEngineeringStatus()
        refreshIntegrityGuard()
        refreshScreenPinningDiagnostics()
        accessibilityGuardEnabled = isExamGuardAccessibilityEnabled(context)
        refreshKeyboardSecurity(triggerViolation = false)
        refreshBluetoothSecurity(triggerViolation = false)
        refreshDeviceIntegritySecurity(triggerViolation = false)
        refreshDeviceTimeSecurity(trigger = "checklist_refresh")
        debugLogExamStart(
            "refreshPreparationStatusChecks scheduled in ${SystemClock.elapsedRealtime() - startedAt} ms"
        )
    }

    fun handleRefreshPreparationStatus() {
        recordAction(code = "SECURITY_STATUS_REFRESHED")
        refreshPreparationStatusChecks()
    }

    fun handleRefreshAllSecurityChecks() {
        recordAction(
            code = "ALL_SECURITY_CHECKS_REFRESH_REQUESTED",
            details = "source=quick_fixes",
            level = DiagnosticEventLevel.INFO
        )
        refreshPreparationStatusChecks()
    }

    fun handleRequestSectionReport(section: DiagnosticSection) {
        pendingSection = section
    }

    fun handleStartExam() {
        startExamController.startExamSession()
    }
    val examRuntimeViewModel = rememberBoundExamRuntimeViewModel(
        activity = componentActivity,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        isKeyboardAllowed = isKeyboardAllowed,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        bypassBluetooth = bypassBluetooth,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bypassAccessibility = bypassAccessibility,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        bypassAdb = bypassAdb,
        adbInspection = adbInspection,
        bypassRoot = bypassRoot,
        rootSecurityStatus = rootSecurityStatus,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        bypassGeofence = bypassGeofence,
        geofenceBypassState = geofenceBypassState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationBypassState = fakeLocationBypassState,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bypassDeviceTime = bypassDeviceTime,
        deviceTimeBypassState = deviceTimeBypassState,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        bypassOverlay = bypassOverlay,
        overlayRiskResult = overlayRiskResult,
        bypassAppSwitch = bypassAppSwitch,
        appSwitchStatus = appSwitchStatus,
        signatureMismatchDetected = signatureMismatchDetected,
        securityTamperDetected = securityTamperDetected,
        networkReadinessStatus = networkReadinessStatus,
        examSessionStarted = examSessionStarted,
        loadingProgress = loadingProgress,
        webViewErrorMessage = webViewErrorMessage,
        hasFullscreenCustomView = fullScreenCustomView != null,
        builtInKeyboardVisible = useBuiltInExamKeyboard && showBuiltInExamKeyboard,
        hasEditableFocus = hasEditableFocus,
        pendingSection = pendingSection,
        showForcedExitAlarm = showForcedExitAlarm,
        showOfflineWarningDialog = showOfflineWarningDialog,
        showNetworkUnstableDialog = showNetworkUnstableDialog,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        showOverlayViolationDialog = showOverlayViolationDialog,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        showClipboardViolationDialog = showClipboardViolationDialog,
        showExitExamDialog = showExitExamDialog,
        onRefreshAllSecurityChecks = ::handleRefreshAllSecurityChecks,
        onRequestSectionReport = ::handleRequestSectionReport,
        onRequestLocationPermission = ::handleRequestLocationPermission,
        onOpenInternetSettings = ::handleOpenInternetSettings
    )
    val footerShieldStatus = resolveExamFooterShieldStatus(
        examGuardArmed = examGuardArmed,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        isKeyboardAllowed = isKeyboardAllowed,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        bypassBluetooth = bypassBluetooth,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bypassAccessibility = bypassAccessibility,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        bypassAdb = bypassAdb,
        adbInspection = adbInspection,
        bypassRoot = bypassRoot,
        rootSecurityStatus = rootSecurityStatus,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        bypassGeofence = bypassGeofence,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bypassDeviceTime = bypassDeviceTime,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        bypassOverlay = bypassOverlay,
        overlayRiskResult = overlayRiskResult,
        bypassAppSwitch = bypassAppSwitch,
        appSwitchStatus = appSwitchStatus,
        bypassClipboard = bypassClipboard,
        signatureMismatchDetected = signatureMismatchDetected,
        securityTamperDetected = securityTamperDetected,
        forcedExitViolationCount = forcedExitViolationCount,
        keyboardViolationCount = keyboardViolationCount,
        overlayViolationCount = overlayViolationCount,
        geofenceViolationCount = geofenceViolationCount,
        fakeLocationViolationCount = fakeLocationViolationCount,
        bluetoothViolationCount = bluetoothViolationCount,
        clipboardViolationCount = clipboardViolationCount,
        showForcedExitAlarm = showForcedExitAlarm,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        showOverlayViolationDialog = showOverlayViolationDialog,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        showClipboardViolationDialog = showClipboardViolationDialog
    )
    val runtimeChromeState = buildExamRuntimeChromeState(
        examSessionStarted = examSessionStarted,
        examDisplayName = examDisplayName,
        loadingProgress = loadingProgress,
        webViewErrorMessage = webViewErrorMessage,
        hasFullscreenCustomView = fullScreenCustomView != null,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        showBuiltInExamKeyboard = showBuiltInExamKeyboard,
        showSideArrowControls = examSessionStarted && sideArrowControlsVisible,
        hasEditableFocus = hasEditableFocus,
        builtInKeyboardShiftEnabled = builtInKeyboardShiftEnabled,
        networkStatus = networkReadinessStatus,
        serverStatus = examServerStatus,
        batteryStatus = batteryStatus,
        shieldStatus = footerShieldStatus
    )
    val runtimeChromeActions = buildExamRuntimeChromeActions(
        onRetryLoading = {
            webViewErrorMessage = null
            webViewInstance?.reload()
        },
        onRefreshPage = {
            recordAction(code = "WEBVIEW_REFRESH_REQUESTED")
            webViewErrorMessage = null
            loadingProgress = 0.05f
            launchExamServerProbe(trigger = "manual_refresh", markChecking = true)
            webViewInstance?.reload()
        },
        onGoHome = {
            recordAction(code = "EXIT_TO_MENU_REQUESTED")
            showExitExamDialog = true
        },
        onTextKey = ::sendBuiltInKeyboardText,
        onBackspace = ::sendBuiltInKeyboardBackspace,
        onArrowLeft = ::sendKeyboardArrowLeft,
        onArrowRight = ::sendKeyboardArrowRight,
        onToggleSideArrowControls = {
            sideArrowControlsVisible = !sideArrowControlsVisible
            recordAction(
                code = "EXAM_ARROW_CONTROLS_TOGGLED",
                details = if (sideArrowControlsVisible) "visible" else "hidden"
            )
        },
        onEnter = ::sendBuiltInKeyboardEnter,
        onSpace = { sendBuiltInKeyboardText(" ") },
        onShiftToggle = { builtInKeyboardShiftEnabled = !builtInKeyboardShiftEnabled }
    )
    val runtimeDialogsState = buildExamRuntimeDialogsState(
        showForcedExitAlarm = showForcedExitAlarm,
        forcedExitViolationCount = forcedExitViolationCount,
        appSwitchStatus = appSwitchStatus,
        showKeyboardViolationDialog = showKeyboardViolationDialog,
        keyboardViolationCount = keyboardViolationCount,
        currentKeyboardLabel = currentKeyboardLabel,
        showOverlayViolationDialog = showOverlayViolationDialog,
        overlayViolationCount = overlayViolationCount,
        overlayTrigger = overlayRiskResult.lastTrigger,
        showOfflineWarningDialog = showOfflineWarningDialog,
        offlineDurationMs = offlineWarningDurationMs,
        currentOfflineDurationMs = currentOfflineDurationMs,
        uiLanguage = uiLanguage,
        showNetworkUnstableDialog = showNetworkUnstableDialog,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        showGeofenceViolationDialog = showGeofenceViolationDialog,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        showFakeLocationViolationDialog = showFakeLocationViolationDialog,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        showBluetoothViolationDialog = showBluetoothViolationDialog,
        bluetoothEnabled = bluetoothEnabled,
        bluetoothViolationCount = bluetoothViolationCount,
        showClipboardViolationDialog = showClipboardViolationDialog,
        clipboardViolationCount = clipboardViolationCount,
        clipboardLastConfirmedAt = lastClipboardConfirmedAt,
        clipboardLastDecision = lastClipboardDecision,
        showExitExamDialog = showExitExamDialog,
        exitSessionClearInFlight = exitSessionClearInFlight
    )
    val runtimeDialogsActions = buildExamRuntimeDialogsActions(
        forcedExitViolationCount = forcedExitViolationCount,
        appSwitchStatus = appSwitchStatus,
        keyboardViolationCount = keyboardViolationCount,
        currentKeyboardLabel = currentKeyboardLabel,
        overlayViolationCount = overlayViolationCount,
        overlayRiskResult = overlayRiskResult,
        lastConnectedNetworkLabel = lastConnectedNetworkLabel,
        offlineWarningDurationMs = offlineWarningDurationMs,
        currentOfflineDurationMs = currentOfflineDurationMs,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        geofenceViolationCount = geofenceViolationCount,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationViolationCount = fakeLocationViolationCount,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        bluetoothViolationCount = bluetoothViolationCount,
        bluetoothEnabled = bluetoothEnabled,
        clipboardViolationCount = clipboardViolationCount,
        lastClipboardConfirmedAt = lastClipboardConfirmedAt,
        lastClipboardDecision = lastClipboardDecision,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        alarmSessionIdentity = alarmSessionIdentity,
        appVersionName = appVersionName,
        adminOverridesSummary = adminOverridesSummary,
        examSessionStarted = examSessionStarted,
        examGuardArmed = examGuardArmed,
        acknowledgeRuntimeAlarm = ::acknowledgeRuntimeAlarm,
        recordAction = { code, details, level -> recordAction(code, details, level) },
        currentNetworkEventDetails = ::currentNetworkEventDetails,
        dismissForcedExitAlarm = {
            showForcedExitAlarm = false
            pendingForcedExitViolation = false
            examAlarmController.stop()
        },
        dismissKeyboardViolationDialog = {
            showKeyboardViolationDialog = false
            examAlarmController.stop()
        },
        dismissOverlayViolationDialog = {
            showOverlayViolationDialog = false
            examAlarmController.stop()
        },
        dismissOfflineWarningDialog = { showOfflineWarningDialog = false },
        dismissNetworkUnstableDialog = { showNetworkUnstableDialog = false },
        dismissGeofenceViolationDialog = {
            showGeofenceViolationDialog = false
            examAlarmController.stop()
        },
        dismissFakeLocationViolationDialog = {
            showFakeLocationViolationDialog = false
            examAlarmController.stop()
        },
        openBluetoothSettings = { openBluetoothSettings(context) },
        dismissBluetoothViolationDialog = {
            showBluetoothViolationDialog = false
            examAlarmController.stop()
        },
        refreshBluetoothSecurity = { refreshBluetoothSecurity(triggerViolation = false) },
        dismissClipboardViolationDialog = {
            showClipboardViolationDialog = false
            examAlarmController.stop()
        },
        dismissExitExamDialog = {
            if (!exitSessionClearInFlight) {
                showExitExamDialog = false
            }
        },
        confirmExitExam = {
            if (!exitSessionClearInFlight) {
                componentActivity.lifecycleScope.launch {
                    clearExamSessionOnExit(
                        reason = "footer_home_confirm",
                        waitForResult = true
                    )
                    showExitExamDialog = false
                    onExit()
                }
            }
        }
    )

    val screenPinningFixNeeded = !bypassScreenPinning &&
        screenPinningAvailable &&
        screenPinningEnabledInSystem.equals("Nonaktif", ignoreCase = true)
    val reinstallApkFixNeeded = signatureMismatchDetected && officialApkUrl.isNotBlank()
    val preparationState = PreparationScreenState(
        examName = payload.examName,
        keyboardPackage = currentKeyboardPackage,
        keyboardAllowed = isKeyboardAllowed,
        usingBuiltInExamKeyboard = useBuiltInExamKeyboard || !isKeyboardAllowed,
        bluetoothPermissionGranted = bluetoothPermissionGranted,
        bluetoothEnabled = bluetoothEnabled,
        accessibilityServiceEnabled = accessibilityServiceEnabled,
        adbInspection = adbInspection,
        adbBypassState = adbBypassState,
        rootSecurityStatus = rootSecurityStatus,
        rootBypassState = rootBypassState,
        signatureMismatchDetected = signatureMismatchDetected,
        virtualEnvironmentDetected = virtualEnvironmentDetected,
        tamperDetected = securityTamperDetected,
        sendingSection = sendingSection,
        isStartingExam = lockTaskRequestPending || geofenceStartValidationInFlight,
        webViewSessionResetInFlight = webViewSessionResetInFlight,
        webViewSessionResetError = webViewSessionResetError,
        isRefreshingGeofence = geofenceManualRefreshInFlight,
        isWarmingLocation = locationWarmupInFlight,
        isRefreshingNetwork = networkManualRefreshInFlight,
        lastGeofenceRefreshAt = lastGeofenceRefreshAt,
        networkReadinessStatus = networkReadinessStatus,
        networkUnstableRuntimeStatus = networkUnstableRuntimeStatus,
        networkTimelinePreview = networkTimelinePreview,
        lastNetworkChangeAt = lastNetworkChangeAt,
        lastNetworkChangeSource = lastNetworkChangeSource,
        lastConnectedNetworkLabel = lastConnectedNetworkLabel,
        screenPinningAvailable = screenPinningAvailable,
        isScreenPinningActive = lockTaskBridge.active(),
        screenPinningFixNeeded = screenPinningFixNeeded,
        clipboardViolationCount = clipboardViolationCount,
        clipboardRuntimeStatus = clipboardRuntimeStatus,
        clipboardBypassState = clipboardBypassState,
        deviceTimeSecurityStatus = deviceTimeSecurityStatus,
        deviceTimeBypassState = deviceTimeBypassState,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        fakeLocationRuntimeStatus = fakeLocationRuntimeStatus,
        overlayRiskResult = overlayRiskResult,
        appSwitchStatus = appSwitchStatus,
        reinstallApkFixNeeded = reinstallApkFixNeeded,
        bypassScreenPinning = bypassScreenPinning,
        bypassBluetooth = bypassBluetooth,
        bypassAccessibility = bypassAccessibility,
        bypassAdb = bypassAdb,
        bypassRoot = bypassRoot,
        bypassVirtualEnvironment = bypassVirtualEnvironment,
        bypassKeyboardPolicy = bypassKeyboardPolicy,
        bypassClipboard = bypassClipboard,
        bypassOverlay = bypassOverlay,
        bypassGeofence = bypassGeofence,
        geofenceBypassState = geofenceBypassState,
        bypassFakeLocation = bypassFakeLocation,
        fakeLocationBypassState = fakeLocationBypassState,
        bypassDeviceTime = bypassDeviceTime,
        bypassAppSwitch = bypassAppSwitch,
        showChecklistDetails = adminSettings.showChecklistDetails
    )
    val preparationActions = buildPreparationScreenActions(
        onChooseKeyboard = ::handleChooseKeyboard,
        onOpenKeyboardSettings = ::handleOpenKeyboardSettings,
        onGrantBluetoothPermission = ::handleGrantBluetoothPermission,
        onOpenBluetoothSettings = ::handleOpenBluetoothSettings,
        onOpenAccessibilitySettings = ::handleOpenAccessibilitySettings,
        onOpenOverlayAccessibilitySettings = ::handleOpenOverlayAccessibilitySettings,
        onOpenDeveloperOptionsSettings = ::handleOpenDeveloperOptionsSettings,
        onRequestLocationPermission = ::handleRequestLocationPermission,
        onOpenLocationServicesSettings = ::handleOpenLocationServicesSettings,
        onRefreshGeofenceLocation = ::handleRefreshLocationSecurity,
        onOpenGeofenceMapViewer = ::handleOpenGeofenceMapViewer,
        onOpenInternetSettings = ::handleOpenInternetSettings,
        onOpenWifiSettings = ::handleOpenWifiSettings,
        onOpenCellularSettings = ::handleOpenCellularSettings,
        onOpenAirplaneModeSettings = ::handleOpenAirplaneModeSettings,
        onRefreshNetworkStatus = ::handleRefreshNetworkStatus,
        onOpenDateTimeSettings = ::handleOpenDateTimeSettings,
        onOpenFakeLocationDeveloperOptionsSettings = ::handleOpenFakeLocationDeveloperOptionsSettings,
        onOpenScreenPinningSettings = ::handleOpenScreenPinningSettings,
        onOpenOverlaySettings = ::handleOpenOverlaySettings,
        onReinstallOfficialApk = ::handleReinstallOfficialApk,
        onRefreshStatus = ::handleRefreshPreparationStatus,
        onRefreshAllSecurityChecks = {
            examRuntimeViewModel.dispatch(ExamRuntimeUiAction.RefreshRequested)
        },
        onRequestSectionReport = ::handleRequestSectionReport,
        onStartExam = ::handleStartExam,
        onBackHome = onExit
    )

    ExamRuntimeSessionRenderedUi(
        examSessionStarted = examSessionStarted,
        showGeofenceMapViewer = showGeofenceMapViewer,
        geofenceRuntimeStatus = geofenceRuntimeStatus,
        geofenceManualRefreshInFlight = geofenceManualRefreshInFlight,
        preparationState = preparationState,
        preparationActions = preparationActions,
        runtimeChromeState = runtimeChromeState,
        runtimeChromeActions = runtimeChromeActions,
        payload = payload,
        bypassOverlay = bypassOverlay,
        examAlarmController = examAlarmController,
        participantCaptureBridge = participantCaptureBridge,
        nativeFullscreenBridge = nativeFullscreenBridge,
        keyboardBridge = keyboardBridge,
        useBuiltInExamKeyboard = useBuiltInExamKeyboard,
        effectiveExamUserAgent = effectiveExamUserAgent,
        fullScreenContainer = fullScreenContainer,
        fullScreenCustomView = fullScreenCustomView,
        nativeExamFullscreenActive = nativeExamFullscreenActive,
        runtimeDialogsState = runtimeDialogsState,
        runtimeDialogsActions = runtimeDialogsActions,
        pendingSection = pendingSection,
        uiLanguage = uiLanguage,
        screenPinningMessage = screenPinningMessage,
        securityIssueDialogTitle = securityIssueDialogTitle,
        securityIssueDialogMessage = securityIssueDialogMessage,
        bugReportFeedbackTitle = bugReportFeedbackTitle,
        bugReportFeedbackMessage = bugReportFeedbackMessage,
        onDismissGeofenceMapViewer = { showGeofenceMapViewer = false },
        onRefreshGeofenceMapViewer = {
            launchLocationSecurityManualRefresh(trigger = "geofence_map_viewer_refresh")
        },
        onRefreshMapViewerActionLogged = {
            recordAction(
                code = "GEOFENCE_QUICK_FIX_REFRESH_REQUESTED",
                details = "trigger=map_viewer",
                level = DiagnosticEventLevel.INFO
            )
        },
        onOverlayObscuredTouch = {
            recordOverlayEvent(
                code = "OVERLAY_TOUCH_DETECTED",
                signal = OverlaySignal.ObscuredTouch,
                level = DiagnosticEventLevel.SECURITY,
                extraContext = "source=secure_exam_webview_touch_filter"
            )
            overlayViolationCount += 1
            showOverlayViolationDialog = true
            examAlarmController.start()
        },
        onShowBuiltInExamKeyboardChange = { showBuiltInExamKeyboard = it },
        onWebViewInstanceChange = { webViewInstance = it },
        onHideSystemKeyboard = ::hideSystemKeyboard,
        onWebViewLoadStart = { url ->
            recordAction(
                code = "WEBVIEW_LOAD_START",
                details = url ?: "tanpa URL"
            )
            hasEditableFocus = false
            webViewErrorMessage = null
            loadingProgress = 0.05f
            if (!url.isNullOrBlank() && url != "about:blank") {
                examServerStatus = ExamServerFooterStatus.Checking
            }
            if (useBuiltInExamKeyboard) {
                showBuiltInExamKeyboard = false
            }
        },
        onWebViewLoadFinish = { view, url ->
            recordAction(
                code = "WEBVIEW_LOAD_FINISH",
                details = url ?: "tanpa URL"
            )
            if (!url.isNullOrBlank() && url != "about:blank") {
                webViewErrorMessage = null
                examServerStatus = ExamServerFooterStatus.Online
            }
            view?.evaluateJavascript(
                """
                (function() {
                    if (!document.body) return;
                    document.documentElement.style.userSelect = 'none';
                    document.documentElement.style.webkitUserSelect = 'none';
                    document.documentElement.style.webkitTouchCallout = 'none';
                    document.documentElement.style.webkitTapHighlightColor = 'transparent';
                    document.documentElement.style.caretColor = 'auto';
                    document.addEventListener('copy', function(event) { event.preventDefault(); });
                    document.addEventListener('cut', function(event) { event.preventDefault(); });
                    document.addEventListener('paste', function(event) { event.preventDefault(); });
                    document.addEventListener('contextmenu', function(event) { event.preventDefault(); });
                })();
                """.trimIndent(),
                null
            )
            view?.evaluateJavascript(InstallExamKeyboardScript, null)
            view?.evaluateJavascript(
                if (sideArrowControlsVisible) {
                    InstallExamSideArrowControlsScript
                } else {
                    RemoveExamSideArrowControlsScript
                },
                null
            )
            if (useBuiltInExamKeyboard) {
                hideSystemKeyboard()
            }
            view?.evaluateJavascript(ExamNativeFullscreenBridgeInstallScript, null)
            view?.evaluateJavascript(
                buildExamNativeFullscreenStateSyncScript(nativeExamFullscreenActive),
                null
            )
            view?.evaluateJavascript(ExamFullscreenRequestHookScript, null)
            view?.evaluateJavascript(ExamParticipantCaptureProbeScript, null)
        },
        onWebViewLoadError = { description ->
            recordAction(
                code = "WEBVIEW_LOAD_ERROR",
                details = description,
                level = DiagnosticEventLevel.ERROR
            )
            webViewErrorMessage = description
            examServerStatus = ExamServerFooterStatus.Offline
        },
        onWebViewHttpError = { statusCode ->
            recordAction(
                code = "WEBVIEW_HTTP_ERROR",
                details = "HTTP ${statusCode ?: "-"}",
                level = DiagnosticEventLevel.ERROR
            )
            webViewErrorMessage =
                "Server ujian mengembalikan error ${statusCode ?: "-"}."
            examServerStatus = when {
                statusCode == null -> ExamServerFooterStatus.Offline
                statusCode >= 500 -> ExamServerFooterStatus.Offline
                else -> ExamServerFooterStatus.Warning
            }
        },
        onWebViewRenderProcessGone = { view, didCrash, rendererPriorityAtExit ->
            handleWebViewRendererGone(
                view = view,
                didCrash = didCrash,
                rendererPriorityAtExit = rendererPriorityAtExit
            )
        },
        onLoadingProgressChange = { loadingProgress = it },
        onWebViewErrorMessageChange = { webViewErrorMessage = it },
        onShowCustomView = { view, callback ->
            if (view != null) {
                showCustomView(view, callback)
            }
        },
        onHideCustomView = ::hideCustomView,
        onDismissPendingSection = { pendingSection = null },
        onConfirmPendingSection = { section ->
            pendingSection = null
            launchTelegramSectionReport(section)
        },
        onDismissScreenPinningMessage = { screenPinningMessage = null },
        onDismissSecurityIssueDialog = {
            val shouldExit = exitOnSecurityIssueDialogDismiss
            securityIssueDialogTitle = null
            securityIssueDialogMessage = null
            exitOnSecurityIssueDialogDismiss = false
            examAlarmController.stop()
            if (shouldExit) {
                componentActivity.lifecycleScope.launch {
                    clearExamSessionOnExit(
                        reason = "fatal_security_dialog_dismiss",
                        waitForResult = true
                    )
                    onExit()
                }
            }
        },
        onDismissBugReportFeedback = {
            bugReportFeedbackTitle = null
            bugReportFeedbackMessage = null
        },
        modifier = modifier
    )

}
