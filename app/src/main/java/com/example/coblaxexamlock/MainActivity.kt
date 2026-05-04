package com.example.coblaxexamlock
import android.app.ActivityManager
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.coblaxexamlock.ui.app.AppContent
import com.example.coblaxexamlock.ui.theme.COBLAXEXAMLOCKTheme
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    private var onUserLeaveExamHandler: WeakReference<(() -> Unit)>? = null
    private var onExamWindowFocusChangedHandler: WeakReference<((Boolean) -> Unit)>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            COBLAXEXAMLOCKTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent()
                }
            }
        }
    }

    fun setOnUserLeaveExamHandler(handler: (() -> Unit)?) {
        onUserLeaveExamHandler = handler?.let { WeakReference(it) }
    }

    fun setOnExamWindowFocusChangedHandler(handler: ((Boolean) -> Unit)?) {
        onExamWindowFocusChangedHandler = handler?.let { WeakReference(it) }
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
            if (allowLockTask) {
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
            runCatching { stopLockTask() }
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
}

