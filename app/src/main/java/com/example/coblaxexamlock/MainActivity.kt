package com.example.coblaxexamlock
import android.app.AlertDialog
import android.app.ActivityManager
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.coblaxexamlock.config.AdminKeyFastExamLabel
import com.example.coblaxexamlock.config.AdminPreferencesName
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.config.SecretTapWindowMs
import com.example.coblaxexamlock.runtime.SecurityDetectorCache
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
    private var nativeSecretTapCount = 0
    private var nativeLastSecretTapAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.mark("activity_on_create_start")
        super.onCreate(savedInstanceState)
        com.example.coblaxexamlock.runtime.TelegramMessageQueueHolder.initialize(this)
        val lowRamProfile = resolveLowRamProfile(this)
        initialLowRamProfile = lowRamProfile
        applyLowRamRuntimeTuning(lowRamProfile)
        val nativePreflightStarted = shouldUseNativePreflightShell(lowRamProfile)
        if (nativePreflightStarted) {
            StartupTrace.mark("set_content_start", "native_preflight")
            showNativeLowRamHomeThenCompose()
        }
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

    private fun applyLowRamRuntimeTuning(lowRamProfile: LowRamProfile) {
        SecurityDetectorCache.cacheTtlMultiplier = when {
            lowRamProfile.ultra -> 3
            lowRamProfile.enabled -> 2
            else -> 1
        }
        SecurityDetectorCache.metadataCacheMaxEntries = lowRamProfile.detectorMetadataCacheMaxEntries
        SecurityDetectorCache.skipDisplayMetadataDefault = lowRamProfile.skipDisplayMetadataInScan
        com.example.coblaxexamlock.runtime.LowRamDispatchers.detectorParallelism = lowRamProfile.detectorParallelism
    }

    private fun shouldUseNativePreflightShell(lowRamProfile: LowRamProfile): Boolean {
        if (lowRamProfile.ultra) {
            return true
        }
        val activityManager = getSystemService(ActivityManager::class.java) ?: return false
        return runCatching {
            val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
            activityManager.isLowRamDevice ||
                activityManager.memoryClass <= NativePreflightMemoryClassMb ||
                memoryInfo.lowMemory ||
                memoryInfo.availMem <= NativePreflightAvailableMemoryBytes
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
        val lowRamProfile = initialLowRamProfile ?: resolveLowRamProfile(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(32), dp(20), dp(16))
            setBackgroundColor(Color.rgb(246, 248, 252))
        }

        // Brand container
        val brandCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = roundedBackground(Color.WHITE, Color.rgb(212, 222, 233))
        }

        // Lightweight profile badge and hidden Secret Admin trigger
        brandCard.addView(
            createNativeProfileControls(lowRamProfile),
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
            background = roundedBackground(Color.rgb(16, 46, 106), Color.TRANSPARENT)
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
                setTextColor(Color.rgb(16, 46, 106))
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
                    roundedBackground(Color.rgb(61, 122, 245), Color.TRANSPARENT)
                } else {
                    roundedBackground(Color.WHITE, Color.rgb(212, 222, 233))
                }
                setOnClickListener { startComposeContent(action) }
            }

            val glyphView = TextView(this).apply {
                text = glyph
                setTextColor(
                    if (action == NativeActionScanExam) Color.WHITE
                    else Color.rgb(16, 46, 106)
                )
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = roundedBackground(
                    if (action == NativeActionScanExam) Color.argb(35, 255, 255, 255)
                    else Color.argb(12, 61, 122, 245),
                    Color.TRANSPARENT
                )
            }
            buttonRow.addView(glyphView)

            val labelView = TextView(this).apply {
                text = label
                setTextColor(
                    if (action == NativeActionScanExam) Color.WHITE
                    else Color.rgb(16, 46, 106)
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

    private fun createNativeProfileControls(lowRamProfile: LowRamProfile): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(createNativeProfileBadge(lowRamProfile))
            addView(horizontalSpace(dp(8)))
            addView(createNativePerformanceProfileButton())
        }

    private fun createNativeProfileBadge(lowRamProfile: LowRamProfile): View {
        val palette = lowRamProfileBadgePalette(lowRamProfile)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setMinimumHeight(dp(30))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = pillBackground(palette.containerColorArgb, palette.borderColorArgb)
            setOnClickListener { registerNativeSecretTap() }

            addView(
                View(this@MainActivity).apply {
                    background = pillBackground(palette.dotColorArgb, Color.TRANSPARENT)
                },
                LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                    rightMargin = dp(6)
                    gravity = Gravity.CENTER_VERTICAL
                }
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = lowRamProfileBadgeLabel(lowRamProfile)
                    setTextColor(palette.contentColorArgb)
                    textSize = 10f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER_VERTICAL }
            )
        }
    }

    private fun createNativePerformanceProfileButton(): View =
        TextView(this).apply {
            text = NativePerformanceProfileGear
            contentDescription = "Buka pengaturan profil performa"
            setTextColor(Color.rgb(16, 46, 106))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            setMinWidth(dp(32))
            setMinimumHeight(dp(32))
            background = pillBackground(Color.rgb(244, 247, 251), Color.rgb(212, 222, 233))
            setOnClickListener { showNativePerformanceProfileDialog() }
        }

    private fun showNativePerformanceProfileDialog() {
        val detectedProfile = resolveDetectedLowRamProfile(this)
        val effectiveProfile = initialLowRamProfile ?: resolveLowRamProfile(this)
        val overrideOptions = lowRamProfileOverrideOptions()
        val checkedIndex = overrideOptions.indexOf(effectiveProfile.lowRamOverride).coerceAtLeast(0)
        val labels = overrideOptions
            .map { option -> nativePerformanceProfileOptionLabel(option) }
            .toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Profil Performa")
            .setMessage(nativePerformanceProfileSummary(detectedProfile, effectiveProfile))
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selectedOverride = overrideOptions[which]
                saveLowRamProfileOverride(this, selectedOverride)
                initialLowRamProfile = applyLowRamProfileOverride(
                    detectedProfile = detectedProfile,
                    override = selectedOverride
                )
                applyLowRamRuntimeTuning(initialLowRamProfile ?: detectedProfile)
                when {
                    isLowRamProfileOverrideRisky(detectedProfile, selectedOverride) -> {
                        Toast.makeText(
                            this,
                            "Mode lebih ringan dari deteksi. HP kecil bisa lebih lag.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    selectedOverride == LowRamProfileOverride.Ultra -> {
                        Toast.makeText(
                            this,
                            "Ultra mengurangi beban UI dan memperjarang polling berkala.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                dialog.dismiss()
                showNativeLowRamHomeThenCompose()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun nativePerformanceProfileOptionLabel(override: LowRamProfileOverride): String =
        when (override) {
            LowRamProfileOverride.Auto -> "Auto - Deteksi perangkat"
            LowRamProfileOverride.Normal -> "Normal - Performa penuh"
            LowRamProfileOverride.Low -> "Low - Lebih ringan"
            LowRamProfileOverride.Ultra -> "Ultra - Paling ringan"
        }

    private fun nativePerformanceProfileSummary(
        detectedProfile: LowRamProfile,
        effectiveProfile: LowRamProfile
    ): String {
        return "Ini hanya mengatur performa/UI, bukan bypass proteksi ujian.\n\n" +
            "Terdeteksi: ${lowRamProfileBadgeLabel(detectedProfile)}\n" +
            "Aktif: ${lowRamProfileBadgeLabel(effectiveProfile)}\n" +
            "RAM: avail=${effectiveProfile.availableMemoryMb ?: "-"}MB total=${effectiveProfile.totalMemoryMb ?: "-"}MB\n" +
            "Polling: ${effectiveProfile.slowPollingMultiplier}x"
    }

    private fun registerNativeSecretTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - nativeLastSecretTapAt > SecretTapWindowMs) {
            nativeSecretTapCount = 0
        }
        nativeLastSecretTapAt = now
        nativeSecretTapCount += 1
        if (nativeSecretTapCount >= NativeSecretTapRequiredCount) {
            nativeSecretTapCount = 0
            startComposeContent(NativeActionSecretAdmin)
        }
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

    private fun pillBackground(fillColor: Int, strokeColor: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(999).toFloat()
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

    private fun horizontalSpace(widthPx: Int): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, 1)
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
        const val NativeActionSecretAdmin = "SecretAdmin"
        const val NativeLabelLoadDelayMillis = 1_200L
        const val NativePreflightMemoryClassMb = 96
        const val NativePreflightAvailableMemoryBytes = 512L * 1024L * 1024L
        const val NativeSecretTapRequiredCount = 4
        const val NativePerformanceProfileGear = "\u2699"
    }
}
