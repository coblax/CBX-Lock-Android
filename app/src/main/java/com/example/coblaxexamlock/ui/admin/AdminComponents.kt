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
import com.example.coblaxexamlock.OverlayRiskAnalyzer
import com.example.coblaxexamlock.OverlayShieldStatus
import com.example.coblaxexamlock.R
import com.example.coblaxexamlock.QrCodeGenerator
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ReverseEngineeringResult
import com.example.coblaxexamlock.RootBypassResolver
import com.example.coblaxexamlock.calculateQrExportBitmapSpec
import com.example.coblaxexamlock.buildRootSecurityStatus
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.formatExamScheduleDateTime
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
internal fun BackPillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LockBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = tr("Main menu", "Menu utama"),
                tint = LockOnDark,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = "MENU",
            color = LockTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
internal fun AdminInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = LockTextPrimary,
            fontSize = 16.sp
        ),
        placeholder = {
            Text(
                text = placeholder,
                color = LockTextMuted,
                fontSize = 16.sp
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LockSurfaceSoft,
            unfocusedContainerColor = LockSurfaceSoft,
            focusedBorderColor = LockBlue,
            unfocusedBorderColor = LockOutline,
            focusedTextColor = LockTextPrimary,
            unfocusedTextColor = LockTextPrimary,
            cursorColor = LockBlue,
            focusedPlaceholderColor = LockTextMuted,
            unfocusedPlaceholderColor = LockTextMuted
        )
    )
}

@Composable
internal fun AdminPickerField(
    value: String,
    placeholder: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LockSurfaceSoft)
            .border(
                width = 1.dp,
                color = if (isActive) LockBlue else LockOutline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isBlank = value.isBlank()
        Text(
            text = value.ifBlank { placeholder },
            color = if (isBlank) LockTextMuted else LockTextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = tr("Pick", "Pilih"),
            color = LockBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun AdminToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, LockOutline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color = LockTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = LockTextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LockBlue,
                uncheckedThumbColor = LockBlue.copy(alpha = 0.6f),
                uncheckedTrackColor = LockOutline.copy(alpha = 0.6f)
            )
        )
    }
}


@Composable
internal fun StatusBanner(
    message: String,
    isError: Boolean
) {
    val backgroundColor = if (isError) Color(0xFFFFF4F4) else Color(0xFFEFF6FF)
    val borderColor = if (isError) Color(0xFFE9B4B4) else Color(0xFFB8D2FF)
    val textColor = if (isError) Color(0xFF9A3030) else LockBlueDeep

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
internal fun GeneratedQrCard(
    encryptedPayload: String,
    examName: String,
    startTime: String,
    endTime: String,
    locationPolicy: ExamQrLocationPolicy
) {
    val context = LocalContext.current
    val lowRamProfile = LocalLowRamProfile.current
    val previewBitmapSize = when {
        lowRamProfile.severe -> 384
        lowRamProfile.enabled -> 512
        else -> 640
    }
    val previewDisplaySize = when {
        lowRamProfile.severe -> 180.dp
        lowRamProfile.enabled -> 200.dp
        else -> 220.dp
    }
    val exportBitmapSpec = remember(lowRamProfile) {
        calculateQrExportBitmapSpec(lowRamProfile)
    }
    val shareFailedMessage = tr("Failed to open the share menu.", "Gagal membuka menu bagikan.")
    val saveSuccessPrefix = tr("Saved:", "Tersimpan:")
    val saveFailedMessage = tr("Failed to save the QR.", "Gagal menyimpan QR.")
    val qrBitmap = remember(encryptedPayload, previewBitmapSize) {
        QrCodeGenerator.generateBitmap(encryptedPayload, size = previewBitmapSize)
    }
    DisposableEffect(qrBitmap) {
        onDispose {
            if (!qrBitmap.isRecycled) {
                qrBitmap.recycle()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LockSurfaceSoft)
            .border(1.dp, LockOutline, RoundedCornerShape(22.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tr("Encrypted Exam QR", "QR Ujian Terenkripsi"),
            color = LockTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tr(
                "The CBX Lock app can read and decrypt this data when scanned.",
                "Aplikasi CBX Lock dapat membaca dan mendekripsi data ini saat dipindai."
            ),
            color = LockTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, LockOutline, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = tr("Encrypted exam QR", "QR ujian terenkripsi"),
                modifier = Modifier.size(previewDisplaySize)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        ExamDetailLine(label = tr("Exam Name", "Nama Ujian"), value = examName)
        ExamDetailLine(label = tr("Start", "Mulai"), value = startTime)
        ExamDetailLine(label = tr("End", "Selesai"), value = endTime)
        ExamDetailLine(
            label = tr("Geofence", "Geofence"),
            value = when (locationPolicy.shapeType) {
                GeofenceShapeType.Circle -> tr(
                    "Circle | ${locationPolicy.effectiveCircleCenters.size} centers | ${locationPolicy.radiusMeters} m",
                    "Lingkaran | ${locationPolicy.effectiveCircleCenters.size} center | ${locationPolicy.radiusMeters} m"
                )
                GeofenceShapeType.Polygon -> tr(
                    "Polygon | ${locationPolicy.vertices.size} points",
                    "Polygon | ${locationPolicy.vertices.size} titik"
                )
                GeofenceShapeType.Disabled -> tr("Disabled", "Nonaktif")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    runCatching {
                        val exportBitmap = ExamQrExportHelper.createShareBitmap(
                            encryptedPayload = encryptedPayload,
                            examName = examName,
                            startTime = startTime,
                            endTime = endTime,
                            locationPolicy = locationPolicy,
                            exportSpec = exportBitmapSpec
                        )
                        try {
                            ExamQrExportHelper.shareBitmap(
                                context = context,
                                bitmap = exportBitmap,
                                examName = examName
                            )
                        } finally {
                            if (!exportBitmap.isRecycled) {
                                exportBitmap.recycle()
                            }
                        }
                    }.onFailure {
                        android.widget.Toast.makeText(
                            context,
                            shareFailedMessage,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LockBlue,
                    contentColor = LockOnDark
                )
            ) {
                Text(tr("Share", "Bagikan"), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    runCatching {
                        val exportBitmap = ExamQrExportHelper.createShareBitmap(
                            encryptedPayload = encryptedPayload,
                            examName = examName,
                            startTime = startTime,
                            endTime = endTime,
                            locationPolicy = locationPolicy,
                            exportSpec = exportBitmapSpec
                        )
                        try {
                            ExamQrExportHelper.saveToGallery(
                                context = context,
                                bitmap = exportBitmap,
                                examName = examName
                            )
                        } finally {
                            if (!exportBitmap.isRecycled) {
                                exportBitmap.recycle()
                            }
                        }
                    }.onSuccess { fileName ->
                        android.widget.Toast.makeText(
                            context,
                            "$saveSuccessPrefix $fileName",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure {
                        android.widget.Toast.makeText(
                            context,
                            saveFailedMessage,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LockBackground,
                    contentColor = LockBlueDeep
                ),
                border = BorderStroke(1.dp, LockBlue.copy(alpha = 0.45f))
            ) {
                Text(tr("Download", "Download"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun ExamDetailLine(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = label,
            color = LockTextSecondary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = LockTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start
        )
    }
}

internal fun formatDateTime(calendar: Calendar): String {
    return formatExamScheduleDateTime(calendar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeDatePickerDialog(
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    PickerDialogTheme {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { onConfirm(datePickerState.selectedDateMillis) }) {
                    Text(tr("Next", "Lanjut"), color = LockBlueSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "Batal"), color = LockTextMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    PickerDialogTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = LockSurface,
            titleContentColor = LockOnDark,
            textContentColor = LockOnDark,
            title = {
                Text(
                    text = tr("Select Time", "Pilih Jam"),
                    color = LockOnDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(timePickerState.hour, timePickerState.minute)
                    }
                ) {
                    Text(tr("Save", "Simpan"), color = LockBlueSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "Batal"), color = LockTextMuted)
                }
            }
        )
    }
}

@Composable
internal fun PickerDialogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PickerDialogColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
internal fun ActionButton(
    text: String,
    subtitle: String? = null,
    badgeText: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    iconContainerColor: Color = contentColor.copy(alpha = 0.12f),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle.isNullOrBlank()) 72.dp else 90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .border(1.dp, borderColor.copy(alpha = 0.70f), RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (contentColor == LockOnDark) {
                                Color.White.copy(alpha = 0.14f)
                            } else {
                                LockBlue.copy(alpha = 0.07f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = contentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Text(
                text = text,
                color = contentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                lineHeight = 22.sp
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = if (contentColor == LockOnDark) {
                        Color.White.copy(alpha = 0.80f)
                    } else {
                        LockTextSecondary
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconContent != null -> iconContent()
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

