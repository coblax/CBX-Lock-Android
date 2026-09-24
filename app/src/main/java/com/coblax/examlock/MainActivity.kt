package com.coblax.examlock
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
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.coblax.examlock.config.AdminKeyFastExamLabel
import com.coblax.examlock.config.AdminPreferencesName
import com.coblax.examlock.config.FastExamName
import com.coblax.examlock.config.SecretTapWindowMs
import com.coblax.examlock.model.ThemeMode
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.persistence.readSavedThemeMode
import com.coblax.examlock.persistence.readSavedUiLanguage
import com.coblax.examlock.ui.app.AppContent
import com.coblax.examlock.ui.app.applyLowRamRuntimeDetectorBudget
import com.coblax.examlock.ui.theme.LocalWindowSizeClass

class MainActivity : ComponentActivity() {
    private var onUserLeaveExamHandler: (() -> Unit)? = null
    private var onExamWindowFocusChangedHandler: ((Boolean) -> Unit)? = null
    private var onExamMultiWindowModeChangedHandler: ((Boolean) -> Unit)? = null
    private var composeContentStarted = false
    private var edgeToEdgeEnabled = false
    private var initialLowRamProfile: LowRamProfile? = null
    private var pendingNativeHomeAction: String? = null
    private var nativeSecretTapCount = 0
    private var nativeLastSecretTapAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.mark("activity_on_create_start")
        super.onCreate(savedInstanceState)
        com.coblax.examlock.runtime.TelegramMessageQueueHolder.initialize(this)
        applySavedThemeWindowBackground()
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

    /**
     * The launch window picks its background from values/ or values-night/, which follow
     * the *system* night setting. When the user has pinned [ThemeMode.Light] or
     * [ThemeMode.Dark] that can disagree, so repaint the window here before the first
     * frame to avoid a light flash in front of a dark UI (or the reverse).
     */
    private fun applySavedThemeWindowBackground() {
        val background = if (isDarkThemeActive()) {
            getColor(R.color.lock_background_dark)
        } else {
            getColor(R.color.lock_background)
        }
        runCatching {
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(background))
        }
    }

    /** Resolves the effective dark/light mode the same way `COBLAXEXAMLOCKTheme` does. */
    private fun isDarkThemeActive(): Boolean = when (readSavedThemeMode()) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System ->
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Colors for the View-based low-RAM shell. It runs before Compose exists, so it
     * cannot read [com.coblax.examlock.ui.theme.AppColors]; these values mirror the
     * light and dark palettes in `Color.kt`.
     */
    private data class NativeShellPalette(
        val background: Int,
        val cardBackground: Int,
        val cardBorder: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val textStrong: Int,
        val logoTile: Int,
        val accent: Int,
        val onAccent: Int,
        val chipBackground: Int,
        val glyphTint: Int
    )

    private fun nativeShellPalette(): NativeShellPalette = if (isDarkThemeActive()) {
        NativeShellPalette(
            background = Color.rgb(15, 17, 23),
            cardBackground = Color.rgb(30, 33, 48),
            cardBorder = Color.rgb(51, 55, 82),
            textPrimary = Color.rgb(111, 162, 255),
            textSecondary = Color.rgb(155, 163, 181),
            textStrong = Color.rgb(232, 234, 240),
            logoTile = Color.rgb(26, 59, 122),
            // Same contrast-safe pairs as primaryActionColors() in Compose.
            accent = Color.rgb(111, 162, 255),
            onAccent = Color.rgb(15, 17, 23),
            chipBackground = Color.rgb(34, 38, 58),
            glyphTint = Color.argb(40, 111, 162, 255)
        )
    } else {
        NativeShellPalette(
            background = Color.rgb(246, 248, 252),
            cardBackground = Color.WHITE,
            cardBorder = Color.rgb(212, 222, 233),
            textPrimary = Color.rgb(16, 46, 106),
            textSecondary = Color.rgb(86, 96, 107),
            textStrong = Color.rgb(27, 34, 48),
            logoTile = Color.rgb(16, 46, 106),
            accent = Color.rgb(42, 94, 196),
            onAccent = Color.WHITE,
            chipBackground = Color.rgb(244, 247, 251),
            glyphTint = Color.argb(12, 61, 122, 245)
        )
    }

    private fun applyLowRamRuntimeTuning(lowRamProfile: LowRamProfile) {
        applyLowRamRuntimeDetectorBudget(lowRamProfile)
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
            @OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
            val windowSizeClass = calculateWindowSizeClass(this)
            CompositionLocalProvider(
                LocalWindowSizeClass provides windowSizeClass
            ) {
                AppContent(
                    initialHomeActionRaw = pendingNativeHomeAction,
                    initialLowRamProfile = initialLowRamProfile,
                    initialThemeMode = readSavedThemeMode()
                )
            }
        }
    }

    /**
     * View-based twin of the Compose home dashboard for the lowest-memory phones. It
     * mirrors the same layout (header, greeting, scan card, two tiles) so the switch to
     * Compose after the first tap does not look like a different app.
     */
    private fun showNativeLowRamHomeThenCompose() {
        StartupTrace.mark("home_compose_start", "shell=native_survival")
        val lowRamProfile = initialLowRamProfile ?: resolveLowRamProfile(this)
        val shell = nativeShellPalette()
        val english = readSavedUiLanguage() == UiLanguage.English
        fun t(en: String, id: String): String = if (english) en else id
        fun matchWidth() = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(16))
            setBackgroundColor(shell.background)
        }

        // Header: logo tile, product name, profile badge (Secret Admin trigger), gear.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            TextView(this).apply {
                text = "CBX"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = roundedBackground(shell.logoTile, Color.TRANSPARENT, radiusDp = 12)
            },
            LinearLayout.LayoutParams(dp(44), dp(44))
        )
        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                TextView(this@MainActivity).apply {
                    text = "CBX Lock"
                    setTextColor(shell.textPrimary)
                    textSize = 16f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    maxLines = 1
                }
            )
            addView(
                TextView(this@MainActivity).apply {
                    text = "Coblax Exam Lock"
                    setTextColor(shell.textSecondary)
                    textSize = 12f
                    maxLines = 1
                }
            )
        }
        header.addView(
            titleColumn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
                rightMargin = dp(8)
            }
        )
        header.addView(createNativeProfileControls(lowRamProfile))
        root.addView(header, matchWidth())

        root.addView(space(dp(24)))
        root.addView(
            TextView(this).apply {
                text = t("Ready for your exam?", "Siap ujian?")
                setTextColor(shell.textStrong)
                textSize = 24f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            },
            matchWidth()
        )
        root.addView(
            TextView(this).apply {
                text = t(
                    "Scan the QR from your proctor, or open the saved exam link.",
                    "Pindai QR dari pengawas, atau buka link ujian yang tersimpan."
                )
                setTextColor(shell.textSecondary)
                textSize = 14f
            },
            matchWidth().apply { topMargin = dp(4) }
        )
        root.addView(space(dp(16)))

        // Primary action: scan the exam QR.
        val scanCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(96)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBackground(shell.accent, Color.TRANSPARENT, radiusDp = 20)
            isClickable = true
            setOnClickListener { startComposeContent(NativeActionScanExam) }
        }
        scanCard.addView(
            TextView(this).apply {
                text = "QR"
                setTextColor(shell.onAccent)
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = roundedBackground(
                    Color.argb(
                        36,
                        Color.red(shell.onAccent),
                        Color.green(shell.onAccent),
                        Color.blue(shell.onAccent)
                    ),
                    Color.TRANSPARENT,
                    radiusDp = 16
                )
            },
            LinearLayout.LayoutParams(dp(56), dp(56))
        )
        val scanText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                TextView(this@MainActivity).apply {
                    text = t("Scan exam QR", "Pindai QR ujian")
                    setTextColor(shell.onAccent)
                    textSize = 20f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            )
            addView(
                TextView(this@MainActivity).apply {
                    text = t(
                        "Point the camera at the QR from your proctor.",
                        "Arahkan kamera ke QR dari pengawas."
                    )
                    setTextColor(shell.onAccent)
                    textSize = 14f
                }
            )
        }
        scanCard.addView(
            scanText,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(14)
                rightMargin = dp(8)
            }
        )
        scanCard.addView(
            TextView(this).apply {
                text = "→"
                setTextColor(shell.onAccent)
                textSize = 20f
            }
        )
        root.addView(scanCard, matchWidth())
        root.addView(space(dp(12)))

        // Secondary tiles: saved link and admin QR. They stack when the font is large or
        // the screen is narrow, like the Compose dashboard.
        val stackTiles = resources.configuration.fontScale > 1.3f ||
            resources.configuration.screenWidthDp < 340
        fun tile(glyph: String, label: String, value: String, action: String): Pair<View, TextView> {
            val valueView = TextView(this).apply {
                setTextColor(shell.textStrong)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            setNativeTileValue(valueView, value)
            val tileView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                minimumHeight = dp(104)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = roundedBackground(shell.cardBackground, shell.cardBorder, radiusDp = 20)
                isClickable = true
                setOnClickListener { startComposeContent(action) }
                addView(
                    TextView(this@MainActivity).apply {
                        text = glyph
                        setTextColor(shell.textPrimary)
                        textSize = 12f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                        background = pillBackground(shell.glyphTint, Color.TRANSPARENT)
                    },
                    LinearLayout.LayoutParams(dp(40), dp(40))
                )
                addView(space(dp(10)))
                addView(
                    TextView(this@MainActivity).apply {
                        text = label
                        setTextColor(shell.textSecondary)
                        textSize = 12f
                    }
                )
                addView(valueView)
            }
            return tileView to valueView
        }
        val (directLinkTile, directLinkValue) = tile(
            glyph = "GO",
            label = t("Saved exam link", "Link ujian tersimpan"),
            value = "…",
            action = NativeActionDirectLink
        )
        val (adminTile, _) = tile(
            glyph = "AD",
            label = t("For admins", "Untuk admin"),
            value = "Custom QR",
            action = NativeActionCustomQrAdmin
        )
        val tiles = LinearLayout(this).apply {
            orientation = if (stackTiles) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        }
        if (stackTiles) {
            tiles.addView(directLinkTile, matchWidth())
            tiles.addView(adminTile, matchWidth().apply { topMargin = dp(12) })
        } else {
            tiles.addView(
                directLinkTile,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    rightMargin = dp(6)
                }
            )
            tiles.addView(
                adminTile,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    leftMargin = dp(6)
                }
            )
        }
        root.addView(tiles, matchWidth())

        // Scrollable so large fonts and short screens never cut off the tiles.
        setContentView(
            android.widget.ScrollView(this).apply {
                isFillViewport = true
                setBackgroundColor(shell.background)
                addView(root)
            }
        )
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
                    root.postDelayed(
                        { updateNativeDirectLinkLabelAfterIdle(directLinkValue) },
                        NativeLabelLoadDelayMillis
                    )
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
        val shell = nativeShellPalette()
        // lowRamProfileBadgePalette() only defines light tiers; keep its tier dot as the
        // accent but take the pill surface from the shell palette so the badge is not a
        // bright white chip on the dark shell.
        val dark = isDarkThemeActive()
        val containerColor = if (dark) shell.chipBackground else palette.containerColorArgb
        val borderColor = if (dark) shell.cardBorder else palette.borderColorArgb
        val contentColor = if (dark) shell.textSecondary else palette.contentColorArgb
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setMinimumHeight(dp(30))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = pillBackground(containerColor, borderColor)
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
                    setTextColor(contentColor)
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

    private fun createNativePerformanceProfileButton(): View {
        val shell = nativeShellPalette()
        return TextView(this).apply {
            text = NativePerformanceProfileGear
            contentDescription = "Buka pengaturan profil performa"
            setTextColor(shell.textPrimary)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            setMinWidth(dp(32))
            setMinimumHeight(dp(32))
            background = pillBackground(shell.chipBackground, shell.cardBorder)
            setOnClickListener { showNativePerformanceProfileDialog() }
        }
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
        setNativeTileValue(button, label)
        StartupTrace.mark("native_home_direct_link_label_loaded")
    }

    /**
     * Same rule as the Compose tile: a one-word name such as EXAM_SKANSATP has no break
     * point, so it shrinks to fit one line instead of wrapping mid-word.
     */
    private fun setNativeTileValue(view: TextView, value: String) {
        view.text = value
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, NativeTileValueMaxSp)
        if (value.any { it.isWhitespace() }) {
            view.maxLines = 2
            return
        }
        view.maxLines = 1
        view.post {
            val available = view.width - view.paddingLeft - view.paddingRight
            if (available <= 0) return@post
            var size = NativeTileValueMaxSp
            while (size > NativeTileValueMinSp && view.paint.measureText(value) > available) {
                size -= 1f
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size)
            }
        }
    }

    private fun roundedBackground(
        fillColor: Int,
        strokeColor: Int,
        radiusDp: Int = 14
    ): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
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
        onUserLeaveExamHandler = handler
    }

    fun setOnExamWindowFocusChangedHandler(handler: ((Boolean) -> Unit)?) {
        onExamWindowFocusChangedHandler = handler
    }

    fun setOnExamMultiWindowModeChangedHandler(handler: ((Boolean) -> Unit)?) {
        onExamMultiWindowModeChangedHandler = handler
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
        onUserLeaveExamHandler?.invoke()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onExamWindowFocusChangedHandler?.invoke(hasFocus)
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
            window.setHideOverlayWindows(enabled)
            true
        }.getOrElse { false }
    }

    @Suppress("DEPRECATION")
    fun setExamLockMode(enabled: Boolean, allowLockTask: Boolean = true) {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(window, !enabled)
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        if (enabled) {
            runCatching {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            runCatching {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SECURE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
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
            runCatching {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED)
            }
        }
    }

    fun isExamLockModeActive(): Boolean {
        return getExamLockTaskState().isActive()
    }

    fun getExamLockTaskStateLabel(): String {
        return getExamLockTaskState().diagnosticLabel
    }

    internal fun getExamLockTaskState(): ExamLockTaskState {
        return runCatching {
            val activityManager = getSystemService(ActivityManager::class.java)
                ?: return@runCatching ExamLockTaskState.Unknown
            when (activityManager.lockTaskModeState) {
                ActivityManager.LOCK_TASK_MODE_NONE -> ExamLockTaskState.None
                ActivityManager.LOCK_TASK_MODE_LOCKED -> ExamLockTaskState.Locked
                ActivityManager.LOCK_TASK_MODE_PINNED -> ExamLockTaskState.Pinned
                else -> ExamLockTaskState.Unknown
            }
        }.getOrDefault(ExamLockTaskState.Unknown)
    }

    private fun dispatchExamWindowModeChanged() {
        val splitModeActive =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && runCatching { isInMultiWindowMode }.getOrDefault(false)) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && runCatching { isInPictureInPictureMode }.getOrDefault(false))
        onExamMultiWindowModeChangedHandler?.invoke(splitModeActive)
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
        const val NativeTileValueMaxSp = 16f
        const val NativeTileValueMinSp = 11f
    }
}
