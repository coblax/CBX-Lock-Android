package com.example.coblaxexamlock

import android.content.Context
import androidx.webkit.WebViewCompat

private const val LikelyOldWebViewMajorVersion = 90

internal enum class WebViewHealthVerdict {
    Ready,
    NeedsUpdate,
    Unavailable,
    Unknown
}

internal enum class WebViewHealthSeverity {
    Stable,
    Warning,
    Blocking
}

internal data class WebViewCompatibilityStatus(
    val available: Boolean,
    val packageName: String,
    val versionName: String,
    val majorVersion: Int?,
    val outdatedLikely: Boolean,
    val providerSource: String,
    val quickFix: String?
) {
    val verdict: WebViewHealthVerdict
        get() = when {
            !available -> WebViewHealthVerdict.Unavailable
            majorVersion == null -> WebViewHealthVerdict.Unknown
            outdatedLikely -> WebViewHealthVerdict.NeedsUpdate
            else -> WebViewHealthVerdict.Ready
        }

    val severity: WebViewHealthSeverity
        get() = when (verdict) {
            WebViewHealthVerdict.Ready -> WebViewHealthSeverity.Stable
            WebViewHealthVerdict.NeedsUpdate,
            WebViewHealthVerdict.Unknown -> WebViewHealthSeverity.Warning
            WebViewHealthVerdict.Unavailable -> WebViewHealthSeverity.Blocking
        }

    val providerLabel: String
        get() = when (packageName) {
            "com.google.android.webview",
            "com.android.webview" -> "Android System WebView"
            "com.android.chrome" -> "Google Chrome"
            "com.sec.android.app.sbrowser" -> "Samsung Internet"
            "-", "" -> "WebView provider"
            else -> packageName
        }

    val versionLabel: String
        get() = versionName.ifBlank { "unknown" }

    val riskLabel: String
        get() = when (verdict) {
            WebViewHealthVerdict.Ready -> "WebView provider ready"
            WebViewHealthVerdict.NeedsUpdate -> "WebView provider needs update"
            WebViewHealthVerdict.Unavailable -> "WebView provider unavailable"
            WebViewHealthVerdict.Unknown -> "WebView provider version unknown"
        }

    val studentSummary: String
        get() = when (verdict) {
            WebViewHealthVerdict.Ready ->
                "$providerLabel $versionLabel is ready for exam mode."
            WebViewHealthVerdict.NeedsUpdate ->
                "$providerLabel $versionLabel works, but should be updated before long exam sessions."
            WebViewHealthVerdict.Unavailable ->
                "Android WebView or Chrome is not available. Exam mode cannot start safely."
            WebViewHealthVerdict.Unknown ->
                "$providerLabel is available, but its version cannot be verified."
        }

    val adminDetail: String
        get() = "verdict=${verdict.name}" +
            " | severity=${severity.name}" +
            " | provider=$providerLabel" +
            " | package=$packageName" +
            " | version=$versionLabel" +
            " | major=${majorVersion ?: "-"}" +
            " | source=$providerSource"

    val displayLabel: String
        get() = if (available) {
            "$providerLabel $versionLabel".trim()
        } else {
            "Unavailable"
        }

    fun diagnosticSummary(): String {
        return "verdict=${verdict.name}" +
            " | severity=${severity.name}" +
            " | available=$available" +
            " | package=$packageName" +
            " | version=${versionName.ifBlank { "-" }}" +
            " | major=${majorVersion ?: "-"}" +
            " | outdated_likely=$outdatedLikely" +
            " | source=$providerSource"
    }
}

internal fun readWebViewCompatibilityStatus(context: Context): WebViewCompatibilityStatus {
    val packageInfo = runCatching { WebViewCompat.getCurrentWebViewPackage(context) }
        .getOrNull()
    if (packageInfo != null) {
        return resolveWebViewCompatibilityStatus(
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName,
            providerSource = "current_provider"
        )
    }

    val knownPackages = listOf(
        "com.google.android.webview",
        "com.android.webview",
        "com.android.chrome",
        "com.sec.android.app.sbrowser"
    )
    knownPackages.forEach { packageName ->
        val found = runCatching { context.packageManager.getPackageInfo(packageName, 0) }
            .getOrNull()
        if (found != null) {
            return resolveWebViewCompatibilityStatus(
                packageName = found.packageName,
                versionName = found.versionName,
                providerSource = "known_package_fallback"
            )
        }
    }

    return resolveWebViewCompatibilityStatus(
        packageName = null,
        versionName = null,
        providerSource = "not_found"
    )
}

internal fun resolveWebViewCompatibilityStatus(
    packageName: String?,
    versionName: String?,
    providerSource: String = "test"
): WebViewCompatibilityStatus {
    val normalizedPackage = packageName.orEmpty().trim()
    val normalizedVersion = versionName.orEmpty().trim()
    val available = normalizedPackage.isNotBlank()
    val majorVersion = normalizedVersion
        .substringBefore('.')
        .toIntOrNull()
    val outdatedLikely = available &&
        majorVersion != null &&
        majorVersion < LikelyOldWebViewMajorVersion

    return WebViewCompatibilityStatus(
        available = available,
        packageName = normalizedPackage.ifBlank { "-" },
        versionName = normalizedVersion,
        majorVersion = majorVersion,
        outdatedLikely = outdatedLikely,
        providerSource = providerSource.ifBlank { "-" },
        quickFix = when {
            !available -> "Install or enable Android System WebView/Chrome, then reopen CBX Exam Lock."
            majorVersion == null -> "Check Android System WebView or Chrome, then refresh WebView health."
            outdatedLikely -> "Update Android System WebView or Chrome before long exam sessions."
            else -> null
        }
    )
}
