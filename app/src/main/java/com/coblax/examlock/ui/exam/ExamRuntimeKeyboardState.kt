package com.coblax.examlock.ui.exam

import android.view.KeyEvent
import android.webkit.WebView
import com.coblax.examlock.buildExamKeyboardInsertScript
import com.coblax.examlock.config.ExamKeyboardArrowLeftScript
import com.coblax.examlock.config.ExamKeyboardArrowRightScript
import com.coblax.examlock.config.ExamKeyboardBackspaceScript
import com.coblax.examlock.config.ExamKeyboardEnterScript
import com.coblax.examlock.sendExamArrowKeyFallback
import java.util.Locale

internal fun sendBuiltInExamKeyboardText(
    webView: WebView?,
    rawText: String,
    shiftEnabled: Boolean,
    updateShiftEnabled: (Boolean) -> Unit,
    hideSystemKeyboard: () -> Unit
) {
    val text = if (shiftEnabled) {
        rawText.uppercase(Locale.US)
    } else {
        rawText
    }
    webView?.evaluateExamJavascriptSafely(buildExamKeyboardInsertScript(text))
    hideSystemKeyboard()
    if (shiftEnabled && rawText.any { it.isLetter() }) {
        updateShiftEnabled(false)
    }
}

internal fun sendBuiltInExamKeyboardBackspace(
    webView: WebView?,
    hideSystemKeyboard: () -> Unit
) {
    webView?.evaluateExamJavascriptSafely(ExamKeyboardBackspaceScript)
    hideSystemKeyboard()
}

internal fun sendBuiltInExamKeyboardEnter(
    webView: WebView?,
    hideSystemKeyboard: () -> Unit
) {
    webView?.evaluateExamJavascriptSafely(ExamKeyboardEnterScript)
    hideSystemKeyboard()
}

internal fun sendExamKeyboardArrowLeft(webView: WebView?) {
    sendExamKeyboardArrow(webView, ExamKeyboardArrowLeftScript, KeyEvent.KEYCODE_DPAD_LEFT)
}

internal fun sendExamKeyboardArrowRight(webView: WebView?) {
    sendExamKeyboardArrow(webView, ExamKeyboardArrowRightScript, KeyEvent.KEYCODE_DPAD_RIGHT)
}

private fun sendExamKeyboardArrow(
    webView: WebView?,
    script: String,
    fallbackKeyCode: Int
) {
    val activeWebView = webView ?: return
    activeWebView.requestFocus()
    val handled = activeWebView.evaluateExamJavascriptSafely(script) { result ->
        if (result?.trim() != "true") {
            activeWebView.sendExamArrowKeyFallback(fallbackKeyCode)
        }
    }
    if (!handled) {
        activeWebView.sendExamArrowKeyFallback(fallbackKeyCode)
    }
}
