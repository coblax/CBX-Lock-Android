package com.coblax.examlock.ui.exam

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.viewinterop.AndroidView
import com.coblax.examlock.config.ExamFullscreenRequestHookScript
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The app injects [ExamFullscreenRequestHookScript] after every exam page load, so CBT
 * sites that insist on fullscreen see it. WebView grants it on the student's first tap
 * and reports it through onShowCustomView, exactly like a fullscreen video. This pins
 * that down: it is why the exam chrome must not hide itself for a custom view (see
 * ExamRuntimeChromeUiTest.pageFullscreenKeepsHeaderFooterKeyboardAndBanner).
 */
class ExamFullscreenHookTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hookPutsTheExamPageIntoFullscreenOnTheFirstTap() {
        val customViewsShown = AtomicInteger()
        val pageFinished = AtomicBoolean(false)
        composeRule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize().testTag(WebViewTag),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customViewsShown.incrementAndGet()
                            }

                            // WebView only enables fullscreen when both are overridden,
                            // as the app's own WebChromeClient does.
                            override fun onHideCustomView() = Unit
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                // The same call the app makes after an exam page load.
                                view.evaluateJavascript(ExamFullscreenRequestHookScript, null)
                                pageFinished.set(true)
                            }
                        }
                        loadDataWithBaseURL(
                            "https://exam.example/",
                            "<html><body style='margin:0'><p>Soal 1</p></body></html>",
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                }
            )
        }
        composeRule.waitUntil(15_000) { pageFinished.get() }
        // Without a user gesture the request is refused, so the page opens normally.
        Thread.sleep(1_000)
        assertEquals(0, customViewsShown.get())

        composeRule.onNodeWithTag(WebViewTag).performClick()
        composeRule.waitUntil(10_000) { customViewsShown.get() == 1 }
    }

    private companion object {
        const val WebViewTag = "exam_fullscreen_hook_webview"
    }
}
