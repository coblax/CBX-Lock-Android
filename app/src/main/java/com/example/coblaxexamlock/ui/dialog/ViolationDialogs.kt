package com.example.coblaxexamlock.ui.dialog

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
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.GeofenceSecurityStatus
import com.example.coblaxexamlock.GeofenceSecurityVerdict
import com.example.coblaxexamlock.LocationSpoofConfidenceTier
import com.example.coblaxexamlock.LocationSpoofSecurityStatus
import com.example.coblaxexamlock.LocationSpoofSecurityVerdict
import com.example.coblaxexamlock.OverlaySignal
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.i18n.tr
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
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


@Composable
internal fun KeyboardViolationDialog(
    violationCount: Int,
    keyboardLabel: String,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFDF1F1),
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFB42318).copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFFB42318),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = tr("Keyboard Not Allowed", "Keyboard Tidak Diizinkan"),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = tr(
                        "The app detected a non-standard keyboard during the exam session.",
                        "Aplikasi mendeteksi keyboard non-standar saat sesi ujian berjalan."
                    ),
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Detected keyboard: ${keyboardLabel.ifBlank { "Unknown" }}",
                        "Keyboard terdeteksi: ${keyboardLabel.ifBlank { "Tidak diketahui" }}"
                    ),
                    color = LockTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr("Keyboard violations: $violationCount", "Jumlah pelanggaran keyboard: $violationCount"),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Please switch back to the device's default keyboard so the exam can continue.",
                        "Silakan kembali ke keyboard bawaan perangkat agar ujian bisa dilanjutkan."
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}

@Composable
internal fun ExitExamDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isClearingSession: Boolean
) {
    AlertDialog(
        onDismissRequest = {
            if (!isClearingSession) {
                onDismiss()
            }
        },
        containerColor = LockBackground,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = LockTextPrimary.copy(alpha = 0.08f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = LockTextPrimary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
                Text(
                    text = tr("Exit Exam Mode", "Keluar Dari Mode Ujian"),
                    color = LockTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = if (isClearingSession) {
                    tr(
                        "Clearing exam session data before returning to Home.",
                        "Membersihkan data sesi ujian sebelum kembali ke Home."
                    )
                } else {
                    tr(
                        "You will leave the exam screen and app lock mode will be turned off.",
                        "Anda akan keluar dari layar ujian dan mode kunci aplikasi akan dimatikan."
                    )
                },
                color = LockTextSecondary
            )
        },
        confirmButton = {
            if (isClearingSession) {
                TextButton(onClick = {}, enabled = false) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LockBlueDeep
                        )
                        Text(tr("Clearing", "Membersihkan"), color = LockTextSecondary)
                    }
                }
            } else {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB42318),
                        contentColor = Color.White
                    )
                ) {
                    Text(tr("Exit", "Keluar"), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClearingSession
            ) {
                Text(tr("Cancel", "Batal"), color = LockBlueDeep)
            }
        }
    )
}

@Composable
internal fun OverlayViolationDialog(
    violationCount: Int,
    trigger: String?,
    onAcknowledge: () -> Unit
) {
    val reasonText = when (trigger) {
        OverlaySignal.WindowFocusLoss.diagnosticLabel() -> tr(
            "Reason: the exam window lost focus in a suspicious way, which often indicates a floating app captured focus.",
            "Alasan: jendela ujian kehilangan fokus secara mencurigakan, yang sering menandakan floating app mengambil fokus."
        )
        else -> tr(
            "Reason: touch input on the exam screen was obscured by another window.",
            "Alasan: input sentuh pada layar ujian tertutup oleh jendela lain."
        )
    }
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFDF1F1),
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFB42318).copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFFB42318),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = tr("Floating App Detected", "Floating App Terdeteksi"),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = tr(
                        "The app detected a floating window or overlay above the exam screen.",
                        "Aplikasi mendeteksi ada jendela melayang atau overlay di atas layar ujian."
                    ),
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = reasonText, color = LockTextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr("Overlay violations: $violationCount", "Jumlah pelanggaran overlay: $violationCount"),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Close the floating app, then continue the exam carefully.",
                        "Tutup floating app lalu lanjutkan ujian dengan hati-hati."
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}

@Composable
internal fun OfflineTooLongDialog(
    durationText: String,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFFFBF0),
        title = {
            Text(
                text = tr("Connection Lost Too Long", "Koneksi Terputus Terlalu Lama"),
                color = LockGoldDark,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = tr(
                        "The exam device has been offline for too long.",
                        "Perangkat ujian sudah offline terlalu lama."
                    ),
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Offline duration: $durationText",
                        "Durasi offline: $durationText"
                    ),
                    color = LockGoldDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Check Wi-Fi or cellular data, then continue the exam once the connection is stable.",
                        "Periksa Wi-Fi atau data seluler, lalu lanjutkan ujian setelah koneksi stabil."
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}

@Composable
internal fun NetworkUnstableDialog(
    transportLabel: String,
    flapCount: Int,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFFFBF0),
        title = {
            Text(
                text = tr("Connection Unstable", "Koneksi Tidak Stabil"),
                color = LockGoldDark,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = tr(
                        "The exam connection changed several times in a short period.",
                        "Koneksi ujian berubah beberapa kali dalam waktu singkat."
                    ),
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Last transport: $transportLabel",
                        "Transport terakhir: $transportLabel"
                    ),
                    color = LockGoldDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tr(
                        "Detected changes: $flapCount",
                        "Jumlah perubahan terdeteksi: $flapCount"
                    ),
                    color = LockTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Move to a more stable Wi-Fi or cellular connection if the exam needs internet access.",
                        "Pindah ke koneksi Wi-Fi atau seluler yang lebih stabil jika ujian membutuhkan akses internet."
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}

@Composable
internal fun GeofenceViolationDialog(
    locationStatus: GeofenceSecurityStatus,
    violationCount: Int,
    onAcknowledge: () -> Unit
) {
    val evaluation = locationStatus.geofenceEvaluation
    val titleText = when (locationStatus.finalVerdict) {
        GeofenceSecurityVerdict.Outside -> tr("Outside Allowed Exam Area", "Di Luar Area Ujian")
        GeofenceSecurityVerdict.PreciseRequired -> tr("Precise Location Required", "Lokasi Presisi Diperlukan")
        GeofenceSecurityVerdict.StaleFix -> tr("Location Fix Too Old", "Fix Lokasi Terlalu Lama")
        GeofenceSecurityVerdict.LowAccuracy -> tr("Location Accuracy Too Low", "Akurasi Lokasi Terlalu Rendah")
        GeofenceSecurityVerdict.MissingAccuracy -> tr("Location Accuracy Missing", "Akurasi Lokasi Belum Ada")
        else -> tr("Location Validation Failed", "Validasi Lokasi Gagal")
    }
    val primaryMessage = when (locationStatus.finalVerdict) {
        GeofenceSecurityVerdict.Outside -> tr(
            "The device location moved outside the configured exam radius.",
            "Lokasi perangkat keluar dari radius ujian yang dikonfigurasi."
        )
        GeofenceSecurityVerdict.PreciseRequired -> tr(
            "The exam requires precise location access, but only approximate location is available.",
            "Ujian membutuhkan akses lokasi presisi, tetapi yang tersedia hanya lokasi perkiraan."
        )
        GeofenceSecurityVerdict.StaleFix -> tr(
            "The latest location fix is too old, so the exam area cannot be validated reliably.",
            "Fix lokasi terbaru sudah terlalu lama sehingga area ujian tidak bisa divalidasi dengan andal."
        )
        GeofenceSecurityVerdict.LowAccuracy -> tr(
            "The latest location fix is too inaccurate for strict geofence validation.",
            "Fix lokasi terbaru terlalu tidak akurat untuk validasi geofence ketat."
        )
        GeofenceSecurityVerdict.MissingAccuracy -> tr(
            "The latest location fix has no usable accuracy value yet.",
            "Fix lokasi terbaru belum memiliki nilai akurasi yang bisa dipakai."
        )
        else -> tr(
            "The app could not validate the exam location while the session was running.",
            "Aplikasi tidak dapat memvalidasi lokasi ujian saat sesi sedang berjalan."
        )
    }
    val locationText = evaluation.locationSnapshot?.let {
        formatCoordinates(it.latitude, it.longitude)
    } ?: "-"
    val circleCenters = evaluation.config?.circleCenters.orEmpty()
    val centerText = evaluation.closestCircleCenter?.let {
        formatCoordinates(it.latitude, it.longitude)
    } ?: evaluation.config?.let {
        formatCoordinates(it.centerLat, it.centerLng)
    } ?: "-"
    val radiusText = evaluation.config?.radiusMeters?.let {
        String.format(Locale.US, "%.1f m", it)
    } ?: "-"
    val distanceText = evaluation.distanceMeters?.let {
        String.format(Locale.US, "%.1f m", it)
    } ?: "-"
    val providerText = evaluation.locationSnapshot?.provider?.ifBlank { "-" } ?: "-"
    val accuracyText = evaluation.locationSnapshot?.accuracyMeters?.let {
        String.format(Locale.US, "%.1f m", it)
    } ?: "-"
    val fixQualityText = locationStatus.fixQualityStatus.verdict.diagnosticLabel()
    val fixAgeText = formatLocationFixAge(locationStatus.fixQualityStatus.ageMs)
    val preciseText = if (locationStatus.preciseLocationGranted) {
        tr("granted", "diberikan")
    } else {
        tr("required", "wajib")
    }
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFDF1F1),
        title = {
            Text(
                text = titleText,
                color = Color(0xFFB42318),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = primaryMessage,
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Location security violations: $violationCount",
                        "Jumlah pelanggaran keamanan lokasi: $violationCount"
                    ),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tr(
                        "Verdict: ${locationStatus.finalVerdict.diagnosticLabel()}",
                        "Verdict: ${locationStatus.finalVerdict.diagnosticLabel()}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr("Current coordinates: $locationText", "Koordinat saat ini: $locationText"),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Closest / primary center: $centerText",
                        "Center terdekat / utama: $centerText"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Circle centers: ${circleCenters.size} | ${summarizeCircleCenters(circleCenters)}",
                        "Center circle: ${circleCenters.size} | ${summarizeCircleCenters(circleCenters)}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr("Shared radius: $radiusText", "Radius bersama: $radiusText"),
                    color = LockTextSecondary
                )
                Text(
                    text = tr("Distance from closest center: $distanceText", "Jarak dari center terdekat: $distanceText"),
                    color = LockTextSecondary
                )
                Text(
                    text = tr("Provider / accuracy: $providerText / $accuracyText", "Provider / akurasi: $providerText / $accuracyText"),
                    color = LockTextSecondary
                )
                Text(
                    text = tr("Fix quality / age: $fixQualityText / $fixAgeText", "Kualitas fix / umur: $fixQualityText / $fixAgeText"),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Location permission: ${if (evaluation.permissionGranted) "granted" else "missing"} | Precise: $preciseText | Services: ${if (evaluation.locationServicesEnabled) "enabled" else "disabled"}",
                        "Izin lokasi: ${if (evaluation.permissionGranted) "diberikan" else "belum"} | Presisi: $preciseText | Layanan: ${if (evaluation.locationServicesEnabled) "aktif" else "nonaktif"}"
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}

@Composable
internal fun FakeLocationViolationDialog(
    fakeLocationStatus: LocationSpoofSecurityStatus,
    violationCount: Int,
    onAcknowledge: () -> Unit
) {
    val suspiciousPackages = fakeLocationStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" }
    val titleText = when (fakeLocationStatus.finalVerdict) {
        LocationSpoofSecurityVerdict.PermissionRequired ->
            tr("Location Permission Required", "Izin Lokasi Diperlukan")
        LocationSpoofSecurityVerdict.LocationServicesDisabled ->
            tr("Location Services Disabled", "Layanan Lokasi Nonaktif")
        LocationSpoofSecurityVerdict.LocationUnavailable ->
            tr("Location Snapshot Unavailable", "Snapshot Lokasi Belum Tersedia")
        else -> when (fakeLocationStatus.confidenceTier) {
            LocationSpoofConfidenceTier.Critical -> tr("Critical Fake Location Detected", "Fake Location Kritis Terdeteksi")
            else -> tr("Mock Location Detected", "Lokasi Palsu Terdeteksi")
        }
    }
    val primaryMessage = when (fakeLocationStatus.finalVerdict) {
        LocationSpoofSecurityVerdict.PermissionRequired -> tr(
            "Location permission is no longer available while the exam is running. Anti-fake-location cannot continue safely without it.",
            "Izin lokasi tidak lagi tersedia saat ujian berlangsung. Anti-fake-location tidak bisa lanjut dengan aman tanpanya."
        )
        LocationSpoofSecurityVerdict.LocationServicesDisabled -> tr(
            "Location services were turned off while the exam is running. Anti-fake-location cannot continue safely without them.",
            "Layanan lokasi dimatikan saat ujian berlangsung. Anti-fake-location tidak bisa lanjut dengan aman tanpanya."
        )
        LocationSpoofSecurityVerdict.LocationUnavailable -> tr(
            "The app could not obtain a usable location snapshot while the exam is running.",
            "Aplikasi tidak bisa mendapatkan snapshot lokasi yang bisa dipakai saat ujian berlangsung."
        )
        else -> when (fakeLocationStatus.confidenceTier) {
            LocationSpoofConfidenceTier.Critical -> tr(
                "The app detected critical combined fake-location signals while the exam was running.",
                "Aplikasi mendeteksi kombinasi sinyal fake-location kritis saat ujian berlangsung."
            )
            else -> tr(
                "The app detected strong fake-location or mock-location signals while the exam was running.",
                "Aplikasi mendeteksi sinyal fake-location atau mock-location kuat saat ujian berlangsung."
            )
        }
    }
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFDF1F1),
        title = {
            Text(
                text = titleText,
                color = Color(0xFFB42318),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = primaryMessage,
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr(
                        "Fake-location violations: $violationCount",
                        "Jumlah pelanggaran fake-location: $violationCount"
                    ),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tr(
                        "Verdict: ${fakeLocationStatus.finalVerdict.diagnosticLabel()}",
                        "Verdict: ${fakeLocationStatus.finalVerdict.diagnosticLabel()}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Confidence tier: ${fakeLocationStatus.confidenceTier.diagnosticLabel()}",
                        "Confidence tier: ${fakeLocationStatus.confidenceTier.diagnosticLabel()}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Fix quality eligible: ${if (fakeLocationStatus.fixQualityEligible) "yes" else "no"} (${fakeLocationStatus.fixQualityStatus.verdict.diagnosticLabel()})",
                        "Fix layak dinilai: ${if (fakeLocationStatus.fixQualityEligible) "ya" else "tidak"} (${fakeLocationStatus.fixQualityStatus.verdict.diagnosticLabel()})"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Location permission: ${if (fakeLocationStatus.permissionGranted) "yes" else "no"}",
                        "Izin lokasi: ${if (fakeLocationStatus.permissionGranted) "ya" else "tidak"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Location services: ${if (fakeLocationStatus.locationServicesEnabled) "enabled" else "disabled"}",
                        "Layanan lokasi: ${if (fakeLocationStatus.locationServicesEnabled) "aktif" else "nonaktif"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Snapshot available: ${if (fakeLocationStatus.snapshotAvailable) "yes" else "no"}",
                        "Snapshot tersedia: ${if (fakeLocationStatus.snapshotAvailable) "ya" else "tidak"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Mock flag: ${if (fakeLocationStatus.mockLocationDetected) "yes" else "no"}",
                        "Flag mock: ${if (fakeLocationStatus.mockLocationDetected) "ya" else "tidak"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Developer options: ${if (fakeLocationStatus.developerOptionsEnabled) "enabled" else "disabled"}",
                        "Developer options: ${if (fakeLocationStatus.developerOptionsEnabled) "aktif" else "nonaktif"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Suspicious packages: $suspiciousPackages",
                        "Paket mencurigakan: $suspiciousPackages"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Supporting signals: ${fakeLocationStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}",
                        "Sinyal pendukung: ${fakeLocationStatus.supportingSignals.map { it.diagnosticLabel() }.joinToString().ifBlank { "-" }}"
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}

@Composable
internal fun BluetoothViolationDialog(
    bluetoothEnabled: Boolean,
    violationCount: Int,
    onOpenBluetoothSettings: () -> Unit,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFDF1F1),
        title = {
            Text(
                text = tr("Bluetooth Is Not Safe", "Bluetooth Tidak Aman"),
                color = Color(0xFFB42318),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (bluetoothEnabled) {
                        tr("Bluetooth was detected as enabled during the exam.", "Bluetooth terdeteksi aktif saat mode ujian berjalan.")
                    } else {
                        tr("Bluetooth must be confirmed off before the exam continues.", "Bluetooth perlu dipastikan nonaktif sebelum ujian dilanjutkan.")
                    },
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr("Bluetooth violations: $violationCount", "Jumlah pelanggaran Bluetooth: $violationCount"),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenBluetoothSettings) {
                Text(tr("Open Bluetooth", "Buka Bluetooth"), color = LockBlueDeep)
            }
        },
        dismissButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockTextMuted)
            }
        }
    )
}

@Composable
internal fun ClipboardViolationDialog(
    violationCount: Int,
    lastConfirmedAt: String?,
    lastDecision: String,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFDF1F1),
        title = {
            Text(
                text = tr("Clipboard Changed", "Clipboard Berubah"),
                color = Color(0xFFB42318),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = tr(
                        "The app detected that the device clipboard changed while the exam was running, including after returning to the app.",
                        "Aplikasi mendeteksi clipboard perangkat berubah saat ujian sedang berjalan, termasuk setelah kembali ke aplikasi."
                    ),
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr("Clipboard violations: $violationCount", "Jumlah pelanggaran clipboard: $violationCount"),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tr(
                        "Last confirmed change: ${lastConfirmedAt?.ifBlank { "-" } ?: "-"}",
                        "Perubahan terkonfirmasi terakhir: ${lastConfirmedAt?.ifBlank { "-" } ?: "-"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Listener decision: $lastDecision",
                        "Keputusan listener: $lastDecision"
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}

internal data class ExamRuntimeDialogsState(
    val showForcedExitAlarm: Boolean,
    val forcedExitViolationCount: Int,
    val appSwitchStatus: AppSwitchStatus,
    val showKeyboardViolationDialog: Boolean,
    val keyboardViolationCount: Int,
    val currentKeyboardLabel: String,
    val showOverlayViolationDialog: Boolean,
    val overlayViolationCount: Int,
    val overlayTrigger: String?,
    val showOfflineWarningDialog: Boolean,
    val offlineDurationText: String,
    val showNetworkUnstableDialog: Boolean,
    val networkTransportLabel: String,
    val networkUnstableFlapCount: Int,
    val showGeofenceViolationDialog: Boolean,
    val geofenceStatus: GeofenceSecurityStatus,
    val geofenceViolationCount: Int,
    val showFakeLocationViolationDialog: Boolean,
    val fakeLocationStatus: LocationSpoofSecurityStatus,
    val fakeLocationViolationCount: Int,
    val showBluetoothViolationDialog: Boolean,
    val bluetoothEnabled: Boolean,
    val bluetoothViolationCount: Int,
    val showClipboardViolationDialog: Boolean,
    val clipboardViolationCount: Int,
    val clipboardLastConfirmedAt: String?,
    val clipboardLastDecision: String,
    val showExitExamDialog: Boolean,
    val exitSessionClearInFlight: Boolean
)

internal data class ExamRuntimeDialogsActions(
    val onAcknowledgeForcedExit: () -> Unit,
    val onAcknowledgeKeyboard: () -> Unit,
    val onAcknowledgeOverlay: () -> Unit,
    val onAcknowledgeOffline: () -> Unit,
    val onAcknowledgeNetworkUnstable: () -> Unit,
    val onAcknowledgeGeofence: () -> Unit,
    val onAcknowledgeFakeLocation: () -> Unit,
    val onOpenBluetoothSettings: () -> Unit,
    val onAcknowledgeBluetooth: () -> Unit,
    val onAcknowledgeClipboard: () -> Unit,
    val onDismissExitExam: () -> Unit,
    val onConfirmExitExam: () -> Unit
)

@Composable
internal fun ExamRuntimeDialogsHost(
    state: ExamRuntimeDialogsState,
    actions: ExamRuntimeDialogsActions
) {
    if (state.showForcedExitAlarm) {
        SecurityViolationDialog(
            violationCount = state.forcedExitViolationCount,
            fallbackGuardActive = state.appSwitchStatus.fallbackGuardActive,
            lastTrigger = state.appSwitchStatus.lastTrigger,
            lastDetectedAt = state.appSwitchStatus.lastDetectedAt,
            lastContext = state.appSwitchStatus.lastContext,
            onAcknowledge = actions.onAcknowledgeForcedExit
        )
    }

    if (state.showKeyboardViolationDialog) {
        KeyboardViolationDialog(
            violationCount = state.keyboardViolationCount,
            keyboardLabel = state.currentKeyboardLabel,
            onAcknowledge = actions.onAcknowledgeKeyboard
        )
    }

    if (state.showOverlayViolationDialog) {
        OverlayViolationDialog(
            violationCount = state.overlayViolationCount,
            trigger = state.overlayTrigger,
            onAcknowledge = actions.onAcknowledgeOverlay
        )
    }

    if (state.showOfflineWarningDialog) {
        OfflineTooLongDialog(
            durationText = state.offlineDurationText,
            onAcknowledge = actions.onAcknowledgeOffline
        )
    }

    if (state.showNetworkUnstableDialog && !state.showOfflineWarningDialog) {
        NetworkUnstableDialog(
            transportLabel = state.networkTransportLabel,
            flapCount = state.networkUnstableFlapCount,
            onAcknowledge = actions.onAcknowledgeNetworkUnstable
        )
    }

    if (state.showGeofenceViolationDialog) {
        GeofenceViolationDialog(
            locationStatus = state.geofenceStatus,
            violationCount = state.geofenceViolationCount,
            onAcknowledge = actions.onAcknowledgeGeofence
        )
    }

    if (state.showFakeLocationViolationDialog) {
        FakeLocationViolationDialog(
            fakeLocationStatus = state.fakeLocationStatus,
            violationCount = state.fakeLocationViolationCount,
            onAcknowledge = actions.onAcknowledgeFakeLocation
        )
    }

    if (state.showBluetoothViolationDialog) {
        BluetoothViolationDialog(
            bluetoothEnabled = state.bluetoothEnabled,
            violationCount = state.bluetoothViolationCount,
            onOpenBluetoothSettings = actions.onOpenBluetoothSettings,
            onAcknowledge = actions.onAcknowledgeBluetooth
        )
    }

    if (state.showClipboardViolationDialog) {
        ClipboardViolationDialog(
            violationCount = state.clipboardViolationCount,
            lastConfirmedAt = state.clipboardLastConfirmedAt,
            lastDecision = state.clipboardLastDecision,
            onAcknowledge = actions.onAcknowledgeClipboard
        )
    }

    if (state.showExitExamDialog) {
        ExitExamDialog(
            onDismiss = actions.onDismissExitExam,
            onConfirm = actions.onConfirmExitExam,
            isClearingSession = state.exitSessionClearInFlight
        )
    }
}

@Composable
internal fun SecurityViolationDialog(
    violationCount: Int,
    fallbackGuardActive: Boolean,
    lastTrigger: String?,
    lastDetectedAt: String?,
    lastContext: String?,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = Color(0xFFFDF1F1),
        title = {
            Text(
                text = tr("Violation Detected", "Pelanggaran Terdeteksi"),
                color = Color(0xFFB42318),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = tr(
                        "The app detected that you left the exam screen or forced the app to lose focus.",
                        "Aplikasi mendeteksi Anda meninggalkan layar ujian atau memaksa aplikasi kehilangan fokus."
                    ),
                    color = LockTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tr("Violations: $violationCount", "Jumlah pelanggaran: $violationCount"),
                    color = Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tr(
                        "Fallback guard active: ${if (fallbackGuardActive) "Yes" else "No"}",
                        "Fallback guard aktif: ${if (fallbackGuardActive) "Ya" else "Tidak"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Last trigger: ${lastTrigger?.ifBlank { "-" } ?: "-"}",
                        "Pemicu terakhir: ${lastTrigger?.ifBlank { "-" } ?: "-"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Last detected at: ${lastDetectedAt?.ifBlank { "-" } ?: "-"}",
                        "Terdeteksi terakhir: ${lastDetectedAt?.ifBlank { "-" } ?: "-"}"
                    ),
                    color = LockTextSecondary
                )
                Text(
                    text = tr(
                        "Context: ${lastContext?.ifBlank { "-" } ?: "-"}",
                        "Konteks: ${lastContext?.ifBlank { "-" } ?: "-"}"
                    ),
                    color = LockTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(tr("I Understand", "Saya Mengerti"), color = LockBlueDeep)
            }
        }
    )
}
