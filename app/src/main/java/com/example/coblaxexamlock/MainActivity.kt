package com.example.coblaxexamlock
import android.app.ActivityManager
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.coblaxexamlock.config.AdminKeyFastExamLabel
import com.example.coblaxexamlock.config.AdminPreferencesName
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.ui.app.AppContent
import com.example.coblaxexamlock.ui.theme.COBLAXEXAMLOCKTheme
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    private var onUserLeaveExamHandler: WeakReference<(() -> Unit)>? = null
    private var onExamWindowFocusChangedHandler: WeakReference<((Boolean) -> Unit)>? = null
    private var onExamMultiWindowModeChangedHandler: WeakReference<((Boolean) -> Unit)>? = null
    private var composeContentStarted = false
    private var edgeToEdgeEnabled = false
    private var initialLowRamProfile: LowRamProfile? = null
    private var pendingNativeHomeAction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.mark("activity_on_create_start")
        super.onCreate(savedInstanceState)
        com.example.coblaxexamlock.runtime.TelegramMessageQueueHolder.initialize(this)
        val nativePreflightStarted = shouldUseNativePreflightShell()
        if (nativePreflightStarted) {
            StartupTrace.mark("set_content_start", "native_preflight")
            showNativeLowRamHomeThenCompose()
        }
        val lowRamProfile = resolveLowRamProfile(this)
        initialLowRamProfile = lowRamProfile
        if (lowRamProfile.severe) {
            if (!nativePreflightStarted) {
                StartupTrace.mark("set_content_start", "native_survival")
                showNativeLowRamHomeThenCompose()
            }
        } else {
            ensureEdgeToEdge()
            StartupTrace.mark("set_content_start", "compose")
            startComposeContent()
        }
    }

    private fun shouldUseNativePreflightShell(): Boolean {
        val activityManager = getSystemService(ActivityManager::class.java) ?: return false
        return runCatching {
            activityManager.isLowRamDevice || activityManager.memoryClass <= NativePreflightMemoryClassMb
        }.getOrDefault(false)
    }

    private fun ensureEdgeToEdge() {
        if (edgeToEdgeEnabled) {
            return
        }
        enableEdgeToEdge()
        edgeToEdgeEnabled = true
    }

    private fun startComposeContent(initialHomeAction: String? = null) {
        if (composeContentStarted) {
            return
        }
        ensureEdgeToEdge()
        if (initialHomeAction != null) {
            pendingNativeHomeAction = initialHomeAction
            StartupTrace.mark("native_home_action", "action=$initialHomeAction")
        }
        composeContentStarted = true
        StartupTrace.mark("compose_set_content_start")
        setContent {
            COBLAXEXAMLOCKTheme {
                AppContent(
                    initialHomeActionRaw = pendingNativeHomeAction,
                    initialLowRamProfile = initialLowRamProfile
                )
            }
        }
    }

    private fun showNativeLowRamHomeThenCompose() {
        StartupTrace.mark("home_compose_start", "shell=native_survival")
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(32), dp(20), dp(16))
            setBackgroundColor(Color.WHITE)
        }

        // Brand container
        val brandCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = roundedBackground(Color.WHITE, Color.rgb(216, 225, 236))
        }

        // Production badge
        brandCard.addView(
            TextView(this).apply {
                text = "PROD"
                setTextColor(Color.WHITE)
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = roundedBackground(Color.rgb(12, 32, 72), Color.TRANSPARENT)
                setOnClickListener { startComposeContent(NativeActionRuntimeHome) }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        brandCard.addView(space(dp(14)))

        // Logo mark
        val logoMark = TextView(this).apply {
            text = "CBX"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(14), dp(20), dp(14))
            background = roundedBackground(Color.rgb(12, 32, 72), Color.TRANSPARENT)
        }
        brandCard.addView(
            logoMark,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
        )

        brandCard.addView(space(dp(10)))

        brandCard.addView(
            TextView(this).apply {
                text = "EXAM LOCK"
                setTextColor(Color.rgb(12, 32, 72))
                textSize = 18f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        brandCard.addView(
            TextView(this).apply {
                text = "Secure exam browser"
                setTextColor(Color.rgb(86, 96, 107))
                textSize = 12f
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        )

        root.addView(
            brandCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(space(dp(16)))

        val nativeActions = listOf(
            Triple("QR", "SCAN QR UJIAN", NativeActionScanExam),
            Triple("AD", "CUSTOM QR (ADMIN)", NativeActionCustomQrAdmin),
            Triple("GO", "DIRECT LINK", NativeActionDirectLink)
        )
        var directLinkButton: TextView? = null
        nativeActions.forEach { (glyph, label, action) ->
            val buttonRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = if (action == NativeActionScanExam) {
                    roundedBackground(Color.rgb(68, 129, 243), Color.TRANSPARENT)
                } else {
                    roundedBackground(Color.WHITE, Color.rgb(216, 225, 236))
                }
                setOnClickListener { startComposeContent(action) }
            }

            val glyphView = TextView(this).apply {
                text = glyph
                setTextColor(
                    if (action == NativeActionScanExam) Color.WHITE
                    else Color.rgb(12, 32, 72)
                )
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = roundedBackground(
                    if (action == NativeActionScanExam) Color.argb(40, 255, 255, 255)
                    else Color.argb(15, 68, 129, 243),
                    Color.TRANSPARENT
                )
            }
            buttonRow.addView(glyphView)

            val labelView = TextView(this).apply {
                text = label
                setTextColor(
                    if (action == NativeActionScanExam) Color.WHITE
                    else Color.rgb(12, 32, 72)
                )
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(dp(12), 0, 0, 0)
            }
            if (action == NativeActionDirectLink) {
                directLinkButton = labelView
            }
            buttonRow.addView(labelView)

            root.addView(
                buttonRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10)
                }
            )
        }
        setContentView(root)
        StartupTrace.mark("native_home_view_ready")
        root.post {
            StartupTrace.mark("native_home_main_idle")
        }
        root.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (root.viewTreeObserver.isAlive) {
                        root.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    StartupTrace.mark("home_first_frame", "shell=native_survival")
                    StartupTrace.mark("native_survival_idle_ready")
                    directLinkButton?.let { button ->
                        root.postDelayed({ updateNativeDirectLinkLabelAfterIdle(button) }, NativeLabelLoadDelayMillis)
                    }
                    return true
                }
            }
        )
    }

    private fun updateNativeDirectLinkLabelAfterIdle(button: TextView) {
        val label = runCatching {
            getSharedPreferences(AdminPreferencesName, MODE_PRIVATE)
                .getString(AdminKeyFastExamLabel, FastExamName)
                ?.trim()
                ?.ifBlank { FastExamName }
                ?: FastExamName
        }.getOrDefault(FastExamName)
        button.text = label
        StartupTrace.mark("native_home_direct_link_label_loaded")
    }

    private fun roundedBackground(fillColor: Int, strokeColor: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
            setColor(fillColor)
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(1), strokeColor)
            }
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt().coerceAtLeast(value)

    private fun space(heightPx: Int): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, heightPx)
        }

    fun setOnUserLeaveExamHandler(handler: (() -> Unit)?) {
        onUserLeaveExamHandler = handler?.let { WeakReference(it) }
    }

    fun setOnExamWindowFocusChangedHandler(handler: ((Boolean) -> Unit)?) {
        onExamWindowFocusChangedHandler = handler?.let { WeakReference(it) }
    }

    fun setOnExamMultiWindowModeChangedHandler(handler: ((Boolean) -> Unit)?) {
        onExamMultiWindowModeChangedHandler = handler?.let { WeakReference(it) }
    }

    fun setExamPortraitMode(enabled: Boolean) {
        runCatching {
            requestedOrientation =
                if (enabled) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        onUserLeaveExamHandler?.get()?.invoke()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onExamWindowFocusChangedHandler?.get()?.invoke(hasFocus)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        dispatchExamWindowModeChanged()
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        dispatchExamWindowModeChanged()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        dispatchExamWindowModeChanged()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        dispatchExamWindowModeChanged()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryPressureCoordinator.dispatchTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        MemoryPressureCoordinator.dispatchLowMemory()
    }

    fun setOverlayShieldMode(enabled: Boolean): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null
        }
        return runCatching {
            javaClass.getMethod(
                "setHideOverlayWindows",
                Boolean::class.javaPrimitiveType
            ).invoke(this, enabled)
            true
        }.getOrElse { false }
    }

    fun setExamLockMode(enabled: Boolean, allowLockTask: Boolean = true) {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        if (enabled) {
            runCatching {
                val secureFlag = if (BuildConfig.DEBUG) {
                    0
                } else {
                    WindowManager.LayoutParams.FLAG_SECURE
                }
                window.addFlags(secureFlag or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            runCatching {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            if (
                shouldStartExamLockTask(
                    enabled = true,
                    allowLockTask = allowLockTask,
                    lockTaskAlreadyActive = isExamLockModeActive()
                )
            ) {
                runCatching { startLockTask() }
            }
        } else {
            runCatching {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SECURE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
            runCatching {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            if (shouldStopExamLockTask(enabled = false, lockTaskAlreadyActive = isExamLockModeActive())) {
                runCatching { stopLockTask() }
            }
        }
    }

    fun isExamLockModeActive(): Boolean {
        return runCatching {
            val activityManager = getSystemService(ActivityManager::class.java) ?: return false
            activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        }.getOrDefault(false)
    }

    fun getExamLockTaskStateLabel(): String {
        return runCatching {
            val activityManager = getSystemService(ActivityManager::class.java) ?: return "Unknown"
            when (activityManager.lockTaskModeState) {
                ActivityManager.LOCK_TASK_MODE_NONE -> "NONE"
                ActivityManager.LOCK_TASK_MODE_LOCKED -> "LOCKED"
                ActivityManager.LOCK_TASK_MODE_PINNED -> "PINNED"
                else -> "UNKNOWN"
            }
        }.getOrDefault("Unknown")
    }

    private fun dispatchExamWindowModeChanged() {
        val splitModeActive =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && runCatching { isInMultiWindowMode }.getOrDefault(false)) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && runCatching { isInPictureInPictureMode }.getOrDefault(false))
        onExamMultiWindowModeChangedHandler?.get()?.invoke(splitModeActive)
    }

    private companion object {
        const val NativeActionRuntimeHome = "RuntimeHome"
        const val NativeActionScanExam = "ScanExam"
        const val NativeActionCustomQrAdmin = "CustomQrAdmin"
        const val NativeActionDirectLink = "DirectLink"
        const val NativeLabelLoadDelayMillis = 1_200L
        const val NativePreflightMemoryClassMb = 96
    }
}

