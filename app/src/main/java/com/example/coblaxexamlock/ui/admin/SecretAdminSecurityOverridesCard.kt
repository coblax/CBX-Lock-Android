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
import com.example.coblaxexamlock.LowRamProfile
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


@Composable
internal fun SecretAdminSecurityOverridesCard(
    settings: AdminSettings,
    overridesActive: Boolean,
    onSettingsChange: (AdminSettings) -> Unit
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
                    title = tr(
                        "Bypass Reverse Engineering Checks",
                        "Bypass Cek Reverse Engineering"
                    ),
                    description = if (settings.reverseEngineeringBypassTampered) {
                        tr(
                            "Bypass storage was tampered. Enforcement stays active until the admin saves this setting again.",
                            "Storage bypass terdeteksi tampered. Enforcement tetap aktif sampai admin menyimpan ulang pengaturan ini."
                        )
                    } else {
                        tr(
                            "Skip debugger, tracer, hooking memory, class, and package enforcement for official troubleshooting only. Detection remains logged.",
                            "Lewati enforcement debugger, tracer, memory hooking, class, dan package hanya untuk troubleshooting resmi. Deteksi tetap dicatat."
                        )
                    },
                    checked = settings.bypassReverseEngineering && !settings.reverseEngineeringBypassTampered,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(bypassReverseEngineering = it))
                    }
                )
                AdminToggleRow(
                    title = tr("Bypass APK Integrity Checks", "Bypass Cek Integritas APK"),
                    description = if (settings.apkIntegrityBypassTampered) {
                        tr(
                            "Bypass storage was tampered. Enforcement stays active until the admin saves this setting again.",
                            "Storage bypass terdeteksi tampered. Enforcement tetap aktif sampai admin menyimpan ulang pengaturan ini."
                        )
                    } else {
                        tr(
                            "Skip signature/hash integrity enforcement for official troubleshooting only. Detection remains logged.",
                            "Lewati enforcement signature/hash integrity hanya untuk troubleshooting resmi. Deteksi tetap dicatat."
                        )
                    },
                    checked = settings.bypassApkIntegrity && !settings.apkIntegrityBypassTampered,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassApkIntegrity = it)) }
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
                    title = tr("Bypass VPN Detection", "Bypass Deteksi VPN"),
                    description = tr(
                        "Allow exam start while VPN is active for approved troubleshooting only.",
                        "Izinkan mulai ujian saat VPN aktif hanya untuk troubleshooting resmi."
                    ),
                    checked = settings.bypassVpn,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassVpn = it)) }
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
                AdminToggleRow(
                    title = tr("Bypass Screen Recorder Detection", "Bypass Deteksi Screen Recorder"),
                    description = tr(
                        "Skip screen recorder app detection.",
                        "Lewati deteksi aplikasi screen recorder."
                    ),
                    checked = settings.bypassScreenRecorder,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassScreenRecorder = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Display Mirror Detection", "Bypass Deteksi Display Mirror"),
                    description = tr(
                        "Skip external display / screen casting detection.",
                        "Lewati deteksi display eksternal / screen casting."
                    ),
                    checked = settings.bypassDisplayMirror,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassDisplayMirror = it)) }
                )
                AdminToggleRow(
                    title = tr("Bypass Multi-Window Detection", "Bypass Deteksi Multi-Window"),
                    description = tr(
                        "Skip split-screen and picture-in-picture detection.",
                        "Lewati deteksi split-screen dan picture-in-picture."
                    ),
                    checked = settings.bypassMultiWindow,
                    onCheckedChange = { onSettingsChange(settings.copy(bypassMultiWindow = it)) }
                )
            }
        }
}
