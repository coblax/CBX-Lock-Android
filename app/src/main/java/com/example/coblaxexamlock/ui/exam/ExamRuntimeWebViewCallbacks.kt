package com.example.coblaxexamlock.ui.exam

import android.webkit.WebView
import com.example.coblaxexamlock.ExamParticipantCaptureProbeScript
import com.example.coblaxexamlock.config.ExamFullscreenRequestHookScript
import com.example.coblaxexamlock.config.ExamNativeFullscreenBridgeInstallScript
import com.example.coblaxexamlock.config.InstallExamKeyboardScript
import com.example.coblaxexamlock.config.InstallExamSideArrowControlsScript
import com.example.coblaxexamlock.config.RemoveExamSideArrowControlsScript
import com.example.coblaxexamlock.format.buildExamNativeFullscreenStateSyncScript
import com.example.coblaxexamlock.model.DiagnosticEventLevel

internal fun handleExamRuntimeWebViewLoadStart(
    url: String?,
    useBuiltInExamKeyboard: Boolean,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    setHasEditableFocus: (Boolean) -> Unit,
    setWebViewErrorMessage: (String?) -> Unit,
    setLoadingProgress: (Float) -> Unit,
    setExamServerStatus: (ExamServerFooterStatus) -> Unit,
    setShowBuiltInExamKeyboard: (Boolean) -> Unit
) {
    recordAction("WEBVIEW_LOAD_START", url ?: "tanpa URL", DiagnosticEventLevel.INFO)
    setHasEditableFocus(false)
    setWebViewErrorMessage(null)
    setLoadingProgress(0.05f)
    if (!url.isNullOrBlank() && url != "about:blank") {
        setExamServerStatus(ExamServerFooterStatus.Checking)
    }
    if (useBuiltInExamKeyboard) {
        setShowBuiltInExamKeyboard(false)
    }
}

internal fun handleExamRuntimeWebViewLoadFinish(
    view: WebView?,
    url: String?,
    sideArrowControlsVisible: Boolean,
    useBuiltInExamKeyboard: Boolean,
    nativeExamFullscreenActive: Boolean,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    setWebViewErrorMessage: (String?) -> Unit,
    setExamServerStatus: (ExamServerFooterStatus) -> Unit,
    hideSystemKeyboard: () -> Unit
) {
    recordAction("WEBVIEW_LOAD_FINISH", url ?: "tanpa URL", DiagnosticEventLevel.INFO)
    if (!url.isNullOrBlank() && url != "about:blank") {
        setWebViewErrorMessage(null)
        setExamServerStatus(ExamServerFooterStatus.Online)
    }
    view?.evaluateExamJavascriptSafely(
        """
        (function() {
            if (!document.body) return;
            document.documentElement.style.userSelect = 'none';
            document.documentElement.style.webkitUserSelect = 'none';
            document.documentElement.style.webkitTouchCallout = 'none';
            document.documentElement.style.webkitTapHighlightColor = 'transparent';
            document.documentElement.style.caretColor = 'auto';
            document.addEventListener('copy', function(event) { event.preventDefault(); });
            document.addEventListener('cut', function(event) { event.preventDefault(); });
            document.addEventListener('paste', function(event) { event.preventDefault(); });
            document.addEventListener('contextmenu', function(event) { event.preventDefault(); });
        })();
        """.trimIndent()
    )
    view?.evaluateExamJavascriptSafely(InstallExamKeyboardScript)
    view?.evaluateExamJavascriptSafely(
        if (sideArrowControlsVisible) {
            InstallExamSideArrowControlsScript
        } else {
            RemoveExamSideArrowControlsScript
        }
    )
    if (useBuiltInExamKeyboard) {
        hideSystemKeyboard()
    }
    view?.evaluateExamJavascriptSafely(ExamNativeFullscreenBridgeInstallScript)
    view?.evaluateExamJavascriptSafely(
        buildExamNativeFullscreenStateSyncScript(nativeExamFullscreenActive)
    )
    view?.evaluateExamJavascriptSafely(ExamFullscreenRequestHookScript)
    view?.evaluateExamJavascriptSafely(ExamParticipantCaptureProbeScript)
}

internal fun handleExamRuntimeWebViewLoadError(
    description: String,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    setWebViewErrorMessage: (String?) -> Unit,
    setExamServerStatus: (ExamServerFooterStatus) -> Unit
) {
    recordAction("WEBVIEW_LOAD_ERROR", description, DiagnosticEventLevel.ERROR)
    setWebViewErrorMessage(description)
    setExamServerStatus(ExamServerFooterStatus.Offline)
}

internal fun handleExamRuntimeWebViewHttpError(
    statusCode: Int?,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    setWebViewErrorMessage: (String?) -> Unit,
    setExamServerStatus: (ExamServerFooterStatus) -> Unit
) {
    recordAction("WEBVIEW_HTTP_ERROR", "HTTP ${statusCode ?: "-"}", DiagnosticEventLevel.ERROR)
    setWebViewErrorMessage("Server ujian mengembalikan error ${statusCode ?: "-"}.")
    setExamServerStatus(
        when {
            statusCode == null -> ExamServerFooterStatus.Offline
            statusCode >= 500 -> ExamServerFooterStatus.Offline
            else -> ExamServerFooterStatus.Warning
        }
    )
}
