package com.example.coblaxexamlock

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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
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
import com.example.coblaxexamlock.config.ExamNativeFullscreenBridgeInstallScript
import com.example.coblaxexamlock.model.normalizeExamUserAgent
import com.example.coblaxexamlock.ui.exam.ExamKeyboardBridge
import com.example.coblaxexamlock.ui.exam.ExamNativeFullscreenBridge
import com.example.coblaxexamlock.ui.exam.SecureExamWebView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull


@Suppress("AssignedValueIsNeverRead")
@SuppressLint("SetJavaScriptEnabled")
internal fun WebView.applyExamWebViewSettings(examUserAgent: String) {
    settings.apply {
        // The exam site requires JavaScript and DOM storage; surrounding
        // hardening stays in place, so this lint warning is intentionally suppressed.
        javaScriptEnabled = true
        domStorageEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = false
        displayZoomControls = false
        userAgentString = normalizeExamUserAgent(examUserAgent)
    }
}

private const val ExamWebViewSessionResetTimeoutMillis = 8_000L

@Suppress("DEPRECATION")
internal suspend fun clearExamWebViewSessionData(
    context: Context,
    existingWebView: WebView?
): Result<Unit> = withContext(Dispatchers.Main.immediate) {
    withTimeoutOrNull(ExamWebViewSessionResetTimeoutMillis) {
        runCatching {
            val cookieManager = CookieManager.getInstance()
            awaitWebViewCookieClear(cookieManager::removeSessionCookies)
            awaitWebViewCookieClear(cookieManager::removeAllCookies)
            cookieManager.flush()

            WebStorage.getInstance().deleteAllData()

            val webViewDatabase = WebViewDatabase.getInstance(context.applicationContext)
            webViewDatabase.clearFormData()
            webViewDatabase.clearHttpAuthUsernamePassword()
            clearLegacyWebViewUsernamePassword(webViewDatabase)

            existingWebView?.prepareForFreshExamSession()
            Unit
        }
    } ?: Result.failure(
        IllegalStateException("Timed out while clearing the exam WebView session.")
    )
}

private suspend fun awaitWebViewCookieClear(
    clearAction: (android.webkit.ValueCallback<Boolean>) -> Unit
) {
    suspendCancellableCoroutine<Unit> { continuation ->
        clearAction {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun clearLegacyWebViewUsernamePassword(webViewDatabase: WebViewDatabase) {
    runCatching { webViewDatabase.clearUsernamePassword() }
}

@Suppress("DEPRECATION")
internal fun WebView.prepareForFreshExamSession() {
    stopLoading()
    runCatching { loadUrl("about:blank") }
    runCatching { clearHistory() }
    runCatching { clearFormData() }
    runCatching { clearSslPreferences() }
    runCatching { clearCache(true) }
    if (this is SecureExamWebView) {
        requestedExamUrl = null
    }
}

internal fun WebView.attachExamParticipantCaptureBridge(
    bridge: ExamParticipantCaptureBridge
) {
    removeJavascriptInterface("ExamParticipantCaptureBridge")
    addJavascriptInterface(bridge, "ExamParticipantCaptureBridge")
}

internal fun WebView.attachExamNativeFullscreenBridge(
    bridge: ExamNativeFullscreenBridge
) {
    removeJavascriptInterface("CBTNativeFullscreenHostBridge")
    addJavascriptInterface(bridge, "CBTNativeFullscreenHostBridge")
}

internal fun WebView.installExamNativeFullscreenDocumentStartScriptIfSupported() {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
        runCatching {
            WebViewCompat.addDocumentStartJavaScript(
                this,
                ExamNativeFullscreenBridgeInstallScript,
                setOf("*")
            )
        }
    }
}

internal fun WebView.detachExamParticipantCaptureBridge() {
    removeJavascriptInterface("ExamParticipantCaptureBridge")
}

internal fun WebView.detachExamNativeFullscreenBridge() {
    removeJavascriptInterface("CBTNativeFullscreenHostBridge")
}

internal fun SecureExamWebView.attachExamKeyboardBridge(
    bridge: ExamKeyboardBridge,
    onHideSystemKeyboard: (() -> Unit)? = null
) {
    removeJavascriptInterface("ExamKeyboardBridge")
    addJavascriptInterface(bridge, "ExamKeyboardBridge")
    if (onHideSystemKeyboard == null) {
        setOnTouchListener(null)
    } else {
        setOnTouchListener { view, event ->
            onHideSystemKeyboard()
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            false
        }
    }
}

internal fun SecureExamWebView.detachExamKeyboardBridge() {
    removeJavascriptInterface("ExamKeyboardBridge")
    setOnTouchListener(null)
}

internal fun WebView.sendExamArrowKeyFallback(keyCode: Int) {
    val downTime = SystemClock.uptimeMillis()
    dispatchKeyEvent(
        KeyEvent(
            downTime,
            downTime,
            KeyEvent.ACTION_DOWN,
            keyCode,
            0
        )
    )
    dispatchKeyEvent(
        KeyEvent(
            downTime,
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            keyCode,
            0
        )
    )
}

internal fun showKeyboardPicker(activity: Activity?): Boolean {
    val inputMethodManager = activity?.getSystemService(InputMethodManager::class.java)
        ?: return false
    inputMethodManager.showInputMethodPicker()
    return true
}

internal fun openKeyboardSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    )
}

@SuppressLint("InlinedApi")

internal fun openBluetoothSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    )
}

internal fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    )
}

internal fun openDeveloperOptionsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            )
        }
}

internal fun openScreenPinningSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }
}

internal fun openOverlaySettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            ).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }
}

internal fun openLocationServicesSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }
}

internal fun openDateTimeSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_DATE_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }
}

internal fun openInternetConnectivitySettings(context: Context) {
    val fallbackIntents = listOf(
        Intent(Settings.ACTION_WIFI_SETTINGS),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
    runCatching {
        context.startActivity(
            createInternetConnectivityIntent().apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }.onFailure {
        fallbackIntents.firstNotNullOfOrNull { intent ->
            runCatching {
                context.startActivity(
                    intent.apply {
                        if (context !is Activity) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                )
                true
            }.getOrNull()
        }
    }
}

private fun createInternetConnectivityIntent(): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    } else {
        Intent(Settings.ACTION_WIFI_SETTINGS)
    }
}

internal fun openWifiSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    )
}

internal fun openCellularSettings(context: Context) {
    val primaryIntent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(primaryIntent) }
        .onFailure {
            context.startActivity(
                Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            )
        }
}

internal fun openAirplaneModeSettings(context: Context) {
    val primaryIntent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(primaryIntent) }
        .onFailure {
            context.startActivity(
                Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            )
        }
}



internal fun buildExamKeyboardInsertScript(text: String): String {
    return "window.__coblaxExamKeyboard && window.__coblaxExamKeyboard.insertText('${escapeForJavascript(text)}');"
}

internal fun escapeForJavascript(text: String): String {
    return text
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "")
}

internal fun parseStoredDateTime(value: String): Calendar {
    val calendar = Calendar.getInstance()
    if (value.isBlank()) {
        return calendar
    }

    val formats = listOf(
        "dd/MM/yyyy HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm"
    )

    for (pattern in formats) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getDefault()
            }.parse(value)
        }.getOrNull()
        if (parsed != null) {
            calendar.time = parsed
            return calendar
        }
    }

    return calendar
}
