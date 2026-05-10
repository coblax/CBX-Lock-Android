package com.example.coblaxexamlock

import com.example.coblaxexamlock.model.RootDetectionDetails
import com.example.coblaxexamlock.model.RootIndicatorType
import com.example.coblaxexamlock.runtime.buildRootIndicatorLabel
import com.example.coblaxexamlock.runtime.getAdbRawValue
import com.example.coblaxexamlock.runtime.getDeveloperOptionsRawValue
import com.example.coblaxexamlock.runtime.isDeviceRooted
import com.example.coblaxexamlock.runtime.isSelinuxPermissive
import com.example.coblaxexamlock.runtime.resolvePrimaryRootIndicator


internal enum class AdbBypassState {
    Active,
    Inactive,
    Tampered
}

internal object AdbBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): AdbBypassState {
        return when {
            tampered -> AdbBypassState.Tampered
            enabled -> AdbBypassState.Active
            else -> AdbBypassState.Inactive
        }
    }
}

internal data class AdbInspection(
    val developerOptionsEnabled: Boolean,
    val adbEnabled: Boolean,
    val developerOptionsRawValue: String,
    val adbRawValue: String,
    val adbSecureProperty: String
) {
    val blocking: Boolean
        get() = developerOptionsEnabled || adbEnabled

    val insecureSystemProperty: Boolean
        get() = adbSecureProperty == "0"

    val integrityHintSummary: String
        get() = if (insecureSystemProperty) "sysprop_adb_secure" else "-"
}

internal fun inspectAdb(context: android.content.Context): AdbInspection {
    val developerOptionsRawValue = getDeveloperOptionsRawValue(context)
    val adbRawValue = getAdbRawValue(context)
    val adbSecureProperty = readSystemProperty("ro.adb.secure").ifBlank { "-" }
    return AdbInspection(
        developerOptionsEnabled = developerOptionsRawValue == "1",
        adbEnabled = adbRawValue == "1",
        developerOptionsRawValue = developerOptionsRawValue,
        adbRawValue = adbRawValue,
        adbSecureProperty = adbSecureProperty
    )
}

internal enum class RootBypassState {
    Active,
    Inactive,
    Tampered
}

internal object RootBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): RootBypassState {
        return when {
            tampered -> RootBypassState.Tampered
            enabled -> RootBypassState.Active
            else -> RootBypassState.Inactive
        }
    }
}

internal data class RootSecurityStatus(
    val details: RootDetectionDetails,
    val detected: Boolean,
    val selinuxPermissive: Boolean,
    val primaryIndicator: RootIndicatorType?,
    val primaryIndicatorLabel: String,
    val severityLabel: String,
    val blocking: Boolean,
    val evidenceSummary: String
)

internal fun buildRootSecurityStatus(details: RootDetectionDetails): RootSecurityStatus {
    val detected = isDeviceRooted(details)
    val selinuxPermissive = isSelinuxPermissive(details)
    val primaryIndicator = resolvePrimaryRootIndicator(details)
    val primaryIndicatorLabel = when {
        primaryIndicator != null -> buildRootIndicatorLabel(details, primaryIndicator)
        selinuxPermissive -> "SELinux permissive"
        else -> "Not detected"
    }
    val severityLabel = when {
        detected -> "danger"
        selinuxPermissive -> "warning"
        else -> "safe"
    }
    val evidenceSummary = buildList {
        if (details.zygiskDetected) add("zygisk")
        if (details.xposedBridgeDetected) add("xposed_bridge")
        if (details.magiskPaths.isNotEmpty()) add("magisk")
        if (details.rootBinaryPaths.isNotEmpty() || details.hasSuBinary) add("su_binary")
        if (details.foundRootPackages.isNotEmpty()) add("root_package")
        if (details.bootloaderUnlocked) add("bootloader")
        if (details.dangerousSystemProperties.isNotEmpty()) add("dangerous_props")
        if (details.hasTestKeys) add("test_keys")
        if (details.selinuxEnabled == false) add("selinux_disabled")
        if (selinuxPermissive) add("selinux_permissive")
        // informational only — excluded from root detection to avoid false positives
        if (details.roAdbSecure == "0") add("adb_secure_prop=0")
        if (details.roBuildType.isNotBlank() && !details.roBuildType.equals("user", ignoreCase = true)) {
            add("build_type=${details.roBuildType}")
        }
    }.joinToString().ifBlank { "-" }

    return RootSecurityStatus(
        details = details,
        detected = detected,
        selinuxPermissive = selinuxPermissive,
        primaryIndicator = primaryIndicator,
        primaryIndicatorLabel = primaryIndicatorLabel,
        severityLabel = severityLabel,
        blocking = detected,
        evidenceSummary = evidenceSummary
    )
}

@android.annotation.SuppressLint("PrivateApi")
private fun readSystemProperty(key: String): String {
    return runCatching {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val getMethod = systemProperties.getMethod("get", String::class.java, String::class.java)
        (getMethod.invoke(null, key, "") as? String).orEmpty()
    }.getOrDefault("")
}
