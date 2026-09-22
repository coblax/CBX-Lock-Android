package com.coblax.examlock.ui.exam

import android.webkit.WebView
import com.coblax.examlock.config.ExamFullscreenRequestHookScript
import com.coblax.examlock.config.ExamNativeFullscreenBridgeInstallScript
import com.coblax.examlock.config.InstallExamKeyboardScript
import com.coblax.examlock.config.InstallExamSideArrowControlsScript
import com.coblax.examlock.config.RemoveExamSideArrowControlsScript
import com.coblax.examlock.ExamParticipantCaptureProbeScript
import com.coblax.examlock.format.buildExamNativeFullscreenStateSyncScript
import com.coblax.examlock.model.DiagnosticEventLevel

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
    if (!url.isNullOrBlank() && url != "about:blank" && !url.startsWith("data:")) {
        // The client forwards finishes only for successful main-frame navigations.
        setExamServerStatus(ExamServerFooterStatus.Online)
        setWebViewErrorMessage(null)
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
    val userMessage = resolveWebViewLoadErrorMessage(description)
    setWebViewErrorMessage(userMessage)
    setExamServerStatus(ExamServerFooterStatus.Offline)
}

/**
 * Maps raw WebView error descriptions (e.g. `net::ERR_NAME_NOT_RESOLVED`)
 * to clear, actionable messages that a student can understand and act on.
 * The raw [description] is always preserved in diagnostics via [recordAction].
 */
private fun resolveWebViewLoadErrorMessage(description: String): String {
    val desc = description.trim()
    return when {
        desc.contains("ERR_NAME_NOT_RESOLVED", ignoreCase = true) ->
            "Server ujian tidak ditemukan. Periksa koneksi internet Anda, lalu tekan Refresh."
        desc.contains("ERR_INTERNET_DISCONNECTED", ignoreCase = true) ->
            "Tidak ada koneksi internet. Sambungkan ke Wi-Fi atau data seluler, lalu tekan Refresh."
        desc.contains("ERR_CONNECTION_TIMED_OUT", ignoreCase = true) ||
            desc.contains("ERR_TIMED_OUT", ignoreCase = true) ->
            "Koneksi ke server ujian timeout. Periksa kestabilan internet, lalu tekan Refresh."
        desc.contains("ERR_CONNECTION_REFUSED", ignoreCase = true) ->
            "Server ujian menolak koneksi. Mungkin server sedang tidak aktif. Tunggu lalu tekan Refresh."
        desc.contains("ERR_CONNECTION_RESET", ignoreCase = true) ->
            "Koneksi ke server ujian terputus. Periksa jaringan Anda, lalu tekan Refresh."
        desc.contains("ERR_CONNECTION_CLOSED", ignoreCase = true) ->
            "Koneksi ke server ujian ditutup sebelum halaman selesai dimuat. Tekan Refresh untuk mencoba lagi."
        desc.contains("ERR_NETWORK_CHANGED", ignoreCase = true) ->
            "Jaringan berubah saat memuat halaman ujian. Pastikan koneksi stabil, lalu tekan Refresh."
        desc.contains("ERR_SSL", ignoreCase = true) ||
            desc.contains("ERR_CERT", ignoreCase = true) ||
            desc.contains("SSL:", ignoreCase = true) ->
            "Koneksi aman ke server ujian gagal (masalah sertifikat). Hubungi admin/pengawas ujian."
        desc.contains("ERR_CACHE_MISS", ignoreCase = true) ->
            "Data cache halaman ujian tidak tersedia. Tekan Refresh untuk memuat ulang."
        desc.contains("ERR_TOO_MANY_REDIRECTS", ignoreCase = true) ->
            "Halaman ujian melakukan terlalu banyak redirect. Hubungi admin/pengawas ujian."
        desc.contains("ERR_CLEARTEXT_NOT_PERMITTED", ignoreCase = true) ->
            "Server ujian tidak menggunakan koneksi aman (HTTPS). Hubungi admin untuk memperbarui URL ujian."
        desc.contains("ERR_ADDRESS_UNREACHABLE", ignoreCase = true) ->
            "Alamat server ujian tidak bisa dijangkau. Periksa koneksi internet, lalu tekan Refresh."
        desc.contains("ERR_SOCKET_", ignoreCase = true) ||
            desc.contains("ERR_EMPTY_RESPONSE", ignoreCase = true) ||
            desc.contains("ERR_CONTENT_LENGTH_MISMATCH", ignoreCase = true) ||
            desc.contains("ERR_INCOMPLETE_CHUNKED_ENCODING", ignoreCase = true) ||
            desc.contains("ERR_RESPONSE_HEADERS_TRUNCATED", ignoreCase = true) ||
            desc.contains("ERR_HTTP2_", ignoreCase = true) ||
            desc.contains("ERR_QUIC_", ignoreCase = true) ->
            "Koneksi ke server ujian sempat terputus saat memuat. Menyambung ulang otomatis; jika tetap gagal tekan Refresh."
        desc.contains("ERR_FAILED", ignoreCase = true) ->
            "Gagal memuat halaman ujian. Periksa koneksi internet, lalu tekan Refresh."
        else ->
            "Halaman ujian belum dapat dimuat. Periksa koneksi, lalu tekan Muat Ulang. Jika tetap gagal, hubungi admin/pengawas."
    }
}

internal fun handleExamRuntimeWebViewHttpError(
    statusCode: Int?,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    setWebViewErrorMessage: (String?) -> Unit,
    setExamServerStatus: (ExamServerFooterStatus) -> Unit
) {
    recordAction("WEBVIEW_HTTP_ERROR", "HTTP ${statusCode ?: "-"}", DiagnosticEventLevel.ERROR)
    // Distinguish between 4xx (access/URL issue — server IS responding) and
    // 5xx (server failure). The old generic message "Server ujian mengembalikan
    // error 403" misled students into thinking they had a network problem.
    val errorMessage = when {
        statusCode == null -> "Tidak bisa terhubung ke server ujian."
        statusCode == 401 || statusCode == 403 ->
            "Akses ke halaman ujian ditolak (HTTP $statusCode). Kemungkinan penyebab: sesi ujian kedaluwarsa, atau IP perangkat tidak diizinkan server. Hubungi admin/pengawas."
        statusCode == 404 ->
            "Halaman ujian tidak ditemukan (HTTP 404). Pastikan URL ujian benar."
        statusCode in 400..499 ->
            "Server ujian menolak permintaan (HTTP $statusCode). Coba muat ulang."
        statusCode >= 500 ->
            "Server ujian mengalami gangguan (HTTP $statusCode). Tunggu lalu coba muat ulang."
        else ->
            "Server ujian mengembalikan status $statusCode."
    }
    setWebViewErrorMessage(errorMessage)
    setExamServerStatus(
        when {
            statusCode == null -> ExamServerFooterStatus.Offline
            statusCode >= 500 -> ExamServerFooterStatus.Offline
            // 4xx is NOT offline — the server IS responding, it's an access issue.
            // Setting Warning instead of Offline avoids triggering auto-reload loops.
            else -> ExamServerFooterStatus.Warning
        }
    )
}
