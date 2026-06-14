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
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.coblaxexamlock.config.DefaultExamUserAgent
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
    private val onObscuredTouchDetected: (ExamOverlayTouchSignal) -> Boolean = { true }
) : WebView(context, attrs, defStyleAttr) {
    var requestedExamUrl: String? = null
    private val pendingConnectionRetryCallbacks = mutableSetOf<Runnable>()

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
            val shouldBlock = onObscuredTouchDetected(
                ExamOverlayTouchSignal(
                    fullyObscured = event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0,
                    partiallyObscured = partiallyObscured,
                    actionMasked = event.actionMasked
                )
            )
            return !shouldBlock
        }

        return super.onFilterTouchEventForSecurity(event)
    }

    @Suppress("RedundantOverride")
    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun postConnectionRetry(delayMillis: Long, retryUrl: String) {
        cancelPendingConnectionRetries()
        var callback: Runnable? = null
        callback = Runnable {
            pendingConnectionRetryCallbacks.remove(callback)
            if (!isAttachedToWindow) {
                return@Runnable
            }
            runCatching {
                loadExamUrlSafely(retryUrl)
                requestedExamUrl = retryUrl
            }
        }
        pendingConnectionRetryCallbacks += callback
        postDelayed(callback, delayMillis)
    }

    fun cancelPendingConnectionRetries() {
        pendingConnectionRetryCallbacks.forEach(::removeCallbacks)
        pendingConnectionRetryCallbacks.clear()
    }

    override fun onDetachedFromWindow() {
        cancelPendingConnectionRetries()
        super.onDetachedFromWindow()
    }

    override fun destroy() {
        cancelPendingConnectionRetries()
        super.destroy()
    }
}

internal fun resolveExamWebViewRetryUrl(
    requestedExamUrl: String?,
    fallbackExamUrl: String
): String = requestedExamUrl?.takeIf { it.isNotBlank() } ?: fallbackExamUrl

internal class ExamKeyboardBridge(
    private val onEditableFocusChangedCallback: (Boolean) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastFocused: Boolean? = null

    @Suppress("unused")
    @JavascriptInterface
    fun onEditableFocusChanged(focused: Boolean) {
        mainHandler.post {
            if (lastFocused == focused) {
                return@post
            }
            lastFocused = focused
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

internal fun WebView.evaluateExamJavascriptSafely(
    script: String,
    callback: ValueCallback<String>? = null
): Boolean {
    return runCatching {
        evaluateJavascript(script, callback)
        true
    }.getOrDefault(false)
}

internal fun WebView.loadExamUrlSafely(url: String): Boolean {
    return runCatching {
        loadUrl(url)
        true
    }.getOrDefault(false)
}

private val BrowserLikeRefreshHeaders = mapOf(
    "Cache-Control" to "no-cache",
    "Pragma" to "no-cache"
)

internal fun WebView.reloadExamUrlLikeBrowserSafely(fallbackUrl: String): Boolean {
    val currentUrl = url?.takeIf { it.isNotBlank() && it != "about:blank" }
    val requestedUrl = (this as? SecureExamWebView)?.requestedExamUrl
        ?.takeIf { it.isNotBlank() && it != "about:blank" }
    val targetUrl = currentUrl ?: requestedUrl ?: fallbackUrl
    return runCatching {
        stopLoading()
        clearCache(false)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        loadUrl(targetUrl, BrowserLikeRefreshHeaders)
        if (this is SecureExamWebView) {
            requestedExamUrl = targetUrl
        }
        true
    }.getOrDefault(false)
}

internal fun WebView.updateExamUserAgentSafely(userAgent: String): Boolean {
    return runCatching {
        settings.userAgentString = if (userAgent == DefaultExamUserAgent) {
            WebSettings.getDefaultUserAgent(context)
        } else {
            userAgent
        }
        true
    }.getOrDefault(false)
}
