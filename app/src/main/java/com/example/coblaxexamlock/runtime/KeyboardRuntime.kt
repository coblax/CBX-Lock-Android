package com.example.coblaxexamlock.runtime

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.example.coblaxexamlock.config.AllowedExamKeyboardPackages
import com.example.coblaxexamlock.config.AllowedSystemKeyboardPackagePrefixes
import com.example.coblaxexamlock.config.BlockedExamKeyboardPackages
import com.example.coblaxexamlock.config.SuspiciousKeyboardPackageTokens
import com.example.coblaxexamlock.config.TrustedOemKeyboardManufacturers
import java.util.Locale


internal fun getCurrentInputMethodPackage(context: Context): String? {
    return Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )?.substringBefore('/')
}

internal fun getCurrentInputMethodRawValue(context: Context): String {
    return Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    ).orEmpty().ifBlank { "-" }
}

internal fun resolveKeyboardAppLabel(
    context: Context,
    packageName: String
): String {
    if (packageName.isBlank()) {
        return "Keyboard tidak terdeteksi"
    }

    return runCatching {
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(appInfo).toString()
    }.getOrElse {
        packageName
    }
}

internal fun getAppVersionName(context: Context, packageName: String): String {
    if (packageName.isBlank()) {
        return "-"
    }
    return runCatching {
        context.packageManager.getPackageInfo(packageName, 0).versionName ?: "-"
    }.getOrDefault("-")
}

internal fun getEnabledInputMethodPackages(context: Context): List<String> {
    val inputMethodManager = context.getSystemService(InputMethodManager::class.java) ?: return emptyList()
    return inputMethodManager.enabledInputMethodList
        .mapNotNull { it.packageName }
        .distinct()
}

internal fun isSystemAppPackage(context: Context, packageName: String): Boolean {
    if (packageName.isBlank()) {
        return false
    }
    val appInfo = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
    }.getOrNull() ?: return false

    return appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
        appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
}

internal fun normalizeDeviceVendor(value: String?): String {
    return value
        ?.trim()
        ?.lowercase(Locale.US)
        .orEmpty()
}

internal fun isTrustedOemKeyboardDevice(): Boolean {
    val brand = normalizeDeviceVendor(Build.BRAND)
    val manufacturer = normalizeDeviceVendor(Build.MANUFACTURER)
    return brand in TrustedOemKeyboardManufacturers || manufacturer in TrustedOemKeyboardManufacturers
}

internal fun hasSuspiciousKeyboardToken(packageName: String): Boolean {
    val normalizedPackage = packageName.lowercase(Locale.US)
    return SuspiciousKeyboardPackageTokens.any { token -> token in normalizedPackage }
}

internal fun matchesAllowedSystemKeyboardPrefix(packageName: String): Boolean {
    val normalizedPackage = packageName.lowercase(Locale.US)
    return AllowedSystemKeyboardPackagePrefixes.any { prefix ->
        normalizedPackage.startsWith(prefix.lowercase(Locale.US))
    }
}

internal fun isAllowedExamKeyboard(
    context: Context,
    packageName: String
): Boolean {
    if (packageName.isBlank()) {
        return false
    }

    if (packageName in BlockedExamKeyboardPackages) {
        return false
    }

    if (packageName in AllowedExamKeyboardPackages) {
        return true
    }

    if (hasSuspiciousKeyboardToken(packageName)) {
        return false
    }

    val isSystemKeyboard = isSystemAppPackage(context, packageName)

    if (!isSystemKeyboard) {
        return false
    }

    if (matchesAllowedSystemKeyboardPrefix(packageName)) {
        return true
    }

    return isTrustedOemKeyboardDevice()
}
