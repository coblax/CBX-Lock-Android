package com.coblax.examlock.ui.exam

import android.webkit.WebView
import com.coblax.examlock.buildExamKeyboardInsertScript
import com.coblax.examlock.config.ExamKeyboardBackspaceScript
import com.coblax.examlock.config.ExamKeyboardEnterScript
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
