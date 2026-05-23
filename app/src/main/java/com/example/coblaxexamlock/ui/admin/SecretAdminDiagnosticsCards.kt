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
internal fun AdminReadinessSummaryCard(
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
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
internal fun AdminAdvancedDiagnosticsCard(
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

internal fun readSecretAdminLockTaskStateLabel(context: Context): String {
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

