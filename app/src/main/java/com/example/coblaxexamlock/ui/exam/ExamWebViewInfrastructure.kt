package com.example.coblaxexamlock.ui.exam

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal open class EmptyActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

internal class SecureExamWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onObscuredTouchDetected: () -> Unit = {}
) : WebView(context, attrs, defStyleAttr) {
    var requestedExamUrl: String? = null

    init {
        filterTouchesWhenObscured = true
    }

    override fun onFilterTouchEventForSecurity(event: MotionEvent): Boolean {
        val partiallyObscured =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0
            } else {
                false
            }
        val obscured =
            event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0 ||
                partiallyObscured

        if (obscured) {
            onObscuredTouchDetected()
            return false
        }

        return super.onFilterTouchEventForSecurity(event)
    }

    @Suppress("RedundantOverride")
    override fun performClick(): Boolean {
        return super.performClick()
    }
}

internal class ExamKeyboardBridge(
    private val onEditableFocusChangedCallback: (Boolean) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Suppress("unused")
    @JavascriptInterface
    fun onEditableFocusChanged(focused: Boolean) {
        mainHandler.post {
            onEditableFocusChangedCallback(focused)
        }
    }
}

internal class ExamNativeFullscreenBridge(
    private val onRequestNativeFullscreen: () -> Boolean
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var active: Boolean = false

    fun updateActive(value: Boolean) {
        active = value
    }

    @Suppress("unused")
    @JavascriptInterface
    fun isActive(): Boolean = active

    @Suppress("unused")
    @JavascriptInterface
    fun requestNativeFullscreen(): Boolean {
        var accepted = false
        val latch = CountDownLatch(1)
        mainHandler.post {
            accepted = runCatching { onRequestNativeFullscreen() }.getOrDefault(false)
            latch.countDown()
        }
        runCatching { latch.await(1500L, TimeUnit.MILLISECONDS) }
        return accepted
    }
}

internal object ExamWebViewRecoveryTestHooks {
    @Volatile
    private var simulateRendererGoneHandler: (() -> Unit)? = null

    fun registerRendererGoneSimulation(handler: (() -> Unit)?) {
        simulateRendererGoneHandler = handler
    }

    fun simulateRendererGoneForTesting() {
        simulateRendererGoneHandler?.invoke()
    }
}
