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
import androidx.compose.ui.draw.drawWithContent
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
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.lowRamProfileBadgeLabel
import com.example.coblaxexamlock.lowRamProfileBadgePalette
import com.example.coblaxexamlock.OverlayRiskAnalyzer
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.R
import com.example.coblaxexamlock.QrCodeGenerator
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ReverseEngineeringResult
import com.example.coblaxexamlock.RootBypassResolver
import com.example.coblaxexamlock.StartupTrace
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.config.DefaultExamUserAgent
import com.example.coblaxexamlock.config.DeveloperGithubUrl
import com.example.coblaxexamlock.config.PickerDialogColorScheme
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.evaluateFakeLocationSecurity
import com.example.coblaxexamlock.evaluateGeofence
import com.example.coblaxexamlock.evaluateGeofenceSecurity
import com.example.coblaxexamlock.evaluateLocationFixQuality
import com.example.coblaxexamlock.format.buildIntegrityPublicSummary
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.i18n.LocalUiLanguage
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.i18n.localized
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.inspectAdb
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
import com.example.coblaxexamlock.runtime.readExamNetworkStatus
import com.example.coblaxexamlock.runtime.sendTelegramSectionReport
import com.example.coblaxexamlock.ui.geofence.CircleGeofenceEditorScreen
import com.example.coblaxexamlock.ui.geofence.PolygonGeofenceEditor
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleVertexList
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertexList
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
import com.example.coblaxexamlock.ui.theme.LockCardBg
import com.example.coblaxexamlock.ui.theme.flatCard
import com.example.coblaxexamlock.ui.theme.flatCardElevated
import com.example.coblaxexamlock.ui.theme.flatPill
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
import java.util.concurrent.atomic.AtomicBoolean
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
internal fun ExamLockHomeScreen(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onScanExam: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenFastExam: () -> Unit,
    directLinkLabel: String,
    onSecretTap: () -> Unit,
    onOpenPerformanceProfile: () -> Unit,
    showDeferredChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    val versionLabel = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi
    val firstDrawMarked = remember { AtomicBoolean(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (firstDrawMarked.compareAndSet(false, true)) {
                    StartupTrace.mark("home_first_frame", "severe=${lowRamProfile.severe}")
                }
            }
            .background(LockBackground)
    ) {
        if (!compactHome) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            0f to LockBlue.copy(alpha = 0.10f),
                            0.5f to LockBlueSoft.copy(alpha = 0.05f),
                            1f to Color.Transparent
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compactHome) 16.dp else 20.dp,
                    vertical = if (compactHome) 12.dp else 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeHeroCard(
                uiLanguage = uiLanguage,
                onUiLanguageChange = onUiLanguageChange,
                onSecretTap = onSecretTap,
                onOpenPerformanceProfile = onOpenPerformanceProfile
            )

            Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 18.dp))

            HomeActionButton(
                text = tr("SCAN EXAM QR", "SCAN QR UJIAN"),
                subtitle = tr(
                    "Scan the exam QR to start. Your settings are already verified.",
                    "Pindai QR ujian untuk mulai. Pengaturan sudah diverifikasi."
                ),
                badgeText = tr("RECOMMENDED", "REKOMENDASI"),
                icon = { Icons.Rounded.QrCodeScanner },
                severeGlyph = "QR",
                containerColor = LockBlue,
                contentColor = LockOnDark,
                borderColor = LockBlue,
                iconContainerColor = Color.White.copy(alpha = 0.16f),
                onClick = onScanExam
            )

            Spacer(modifier = Modifier.height(if (compactHome) 10.dp else 14.dp))

            HomeActionButton(
                text = tr("CUSTOM QR (ADMIN)", "CUSTOM QR (ADMIN)"),
                subtitle = tr(
                    "Create a new exam QR for admin tasks like scheduling or trial checks.",
                    "Buat QR ujian baru untuk kebutuhan admin seperti jadwal atau uji coba."
                ),
                badgeText = "ADMIN",
                icon = { Icons.Rounded.AdminPanelSettings },
                severeGlyph = "AD",
                containerColor = Color.White,
                contentColor = LockBlue,
                borderColor = LockOutline,
                iconContainerColor = LockBlue.copy(alpha = 0.10f),
                onClick = onOpenAdmin
            )

            Spacer(modifier = Modifier.height(if (compactHome) 10.dp else 14.dp))

            HomeActionButton(
                text = directLinkLabel,
                subtitle = tr(
                    "Open the exam quickly when you already have the link.",
                    "Buka ujian cepat saat sudah punya link."
                ),
                badgeText = tr("DIRECT LINK", "LINK LANGSUNG"),
                icon = { Icons.Rounded.Language },
                severeGlyph = "GO",
                containerColor = LockGold.copy(alpha = 0.22f),
                contentColor = LockBlueDeep,
                borderColor = LockGold.copy(alpha = 0.55f),
                iconContainerColor = LockBlueDeep.copy(alpha = 0.08f),
                onClick = onOpenFastExam
            )

            if (showDeferredChrome) {
                Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 18.dp))

                DeveloperInfo()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr(
                        "Production build - Version $versionLabel",
                        "Build produksi - Versi $versionLabel"
                    ),
                    color = LockTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HomeActionButton(
    text: String,
    subtitle: String,
    badgeText: String,
    icon: () -> androidx.compose.ui.graphics.vector.ImageVector,
    severeGlyph: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    iconContainerColor: Color,
    onClick: () -> Unit
) {
    if (LocalLowRamProfile.current.severe) {
        ActionButton(
            text = text,
            subtitle = subtitle,
            badgeText = badgeText,
            iconContent = {
                LightweightHomeGlyph(
                    text = severeGlyph,
                    color = contentColor
                )
            },
            containerColor = containerColor,
            contentColor = contentColor,
            borderColor = borderColor,
            iconContainerColor = iconContainerColor,
            onClick = onClick
        )
    } else {
        ActionButton(
            text = text,
            subtitle = subtitle,
            badgeText = badgeText,
            icon = icon(),
            containerColor = containerColor,
            contentColor = contentColor,
            borderColor = borderColor,
            iconContainerColor = iconContainerColor,
            onClick = onClick
        )
    }
}

@Composable
private fun LightweightHomeGlyph(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .padding(12.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun HomeHeroCard(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onSecretTap: () -> Unit,
    onOpenPerformanceProfile: () -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (compactHome) Modifier.flatCard(radius = 22.dp)
                else Modifier.flatCardElevated(radius = 26.dp)
            )
            .padding(
                horizontal = if (compactHome) 16.dp else 20.dp,
                vertical = if (compactHome) 14.dp else 18.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProductionBuildBadge(
                    uiLanguage = uiLanguage,
                    onSecretTap = onSecretTap
                )
                PerformanceProfileGearButton(onClick = onOpenPerformanceProfile)
            }

            LanguageTogglePill(
                currentLanguage = uiLanguage,
                onLanguageChange = onUiLanguageChange
            )
        }

        Spacer(modifier = Modifier.height(if (compactHome) 12.dp else 18.dp))

        CoblaxFrontBrand(uiLanguage = uiLanguage)
    }
}

@Composable
internal fun ProductionBuildBadge(
    uiLanguage: UiLanguage,
    onSecretTap: () -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val badgePalette = lowRamProfileBadgePalette(lowRamProfile)
    val containerColor = Color(badgePalette.containerColorArgb)
    val contentColor = Color(badgePalette.contentColorArgb)
    val borderColor = Color(badgePalette.borderColorArgb)
    val dotColor = Color(badgePalette.dotColorArgb)
    val label = lowRamProfileBadgeLabel(lowRamProfile)

    Row(
        modifier = Modifier
            .flatPill(
                containerColor = containerColor,
                borderColor = borderColor,
                borderAlpha = 1f
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onSecretTap
            )
            .heightIn(min = 30.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = localized(uiLanguage, label, label),
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
internal fun LanguageTogglePill(
    currentLanguage: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi

    Row(
        modifier = Modifier
            .flatPill(
                containerColor = LockCardBg.copy(alpha = 0.98f),
                borderColor = LockOutline,
                borderAlpha = 0.70f
            )
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(LockBlue.copy(alpha = 0.08f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (!compactHome) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = tr("Change language", "Ubah bahasa"),
                    tint = LockBlueDeep,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = "LANG",
                color = LockBlueDeep,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageOptionChip(
                label = "EN",
                selected = currentLanguage == UiLanguage.English,
                onClick = { onLanguageChange(UiLanguage.English) }
            )
            LanguageOptionChip(
                label = "ID",
                selected = currentLanguage == UiLanguage.Indonesian,
                onClick = { onLanguageChange(UiLanguage.Indonesian) }
            )
        }
    }
}

@Composable
internal fun LanguageOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) LockBlue else LockSurfaceSoft)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) LockOnDark else LockTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun CoblaxFrontBrand(uiLanguage: UiLanguage) {
    val lowRamProfile = LocalLowRamProfile.current
    val compactHome = lowRamProfile.deferHeavyUi
    val logoSize = if (compactHome) 112.dp else 188.dp
    val titleSize = if (compactHome) 28.sp else 34.sp
    val subtitleSize = if (compactHome) 12.sp else 14.sp
    val bodySize = if (compactHome) 12.sp else 13.sp
    val bodyLineHeight = if (compactHome) 16.sp else 18.sp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoblaxLogoMark(
            modifier = Modifier.size(logoSize)
        )

        Spacer(modifier = Modifier.height(if (compactHome) 4.dp else 8.dp))

        Text(
            text = "CBX Lock",
            color = LockBlueDeep,
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "COBLAX EXAM LOCK",
            color = LockBlueMid,
            fontSize = subtitleSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )

        Spacer(modifier = Modifier.height(if (compactHome) 8.dp else 12.dp))

        Text(
            text = localized(
                uiLanguage,
                "Keeps online exams focused and safer from cheating by locking the device and guiding students to the official exam page.",
                "Menjaga ujian online tetap fokus dan lebih aman dari kecurangan dengan mengunci perangkat serta mengarahkan siswa ke halaman ujian resmi."
            ),
            color = LockTextSecondary,
            fontSize = bodySize,
            lineHeight = bodyLineHeight,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun CoblaxLogoMark(modifier: Modifier = Modifier) {
    if (LocalLowRamProfile.current.severe) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(26.dp))
                .background(LockBlueDeep),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CBX",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            )
        }
    } else {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = modifier
        )
    }
}

@Composable
internal fun DeveloperInfo() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LockCardBg)
            .border(1.dp, LockOutline.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
            .clickable { openExternalUrl(context, DeveloperGithubUrl) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = tr("Developer", "Pengembang"),
                color = LockTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "github.com/coblax",
                color = LockBlueDeep,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(LockBlue.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = tr("OPEN", "BUKA"),
                color = LockBlueDeep,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
