package com.coblax.examlock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager

internal enum class DpcProtectionTier {
    None,
    NormalApk,
    LegacyDpcAndroid7,
    DpcOverlayRestricted,
    DpcOverlayRestrictedWithShield
}

internal fun DpcProtectionTier.diagnosticLabel(): String {
    return when (this) {
        DpcProtectionTier.None -> "none"
        DpcProtectionTier.NormalApk -> "normal_apk"
        DpcProtectionTier.LegacyDpcAndroid7 -> "legacy_dpc_android7"
        DpcProtectionTier.DpcOverlayRestricted -> "dpc_overlay_restricted"
        DpcProtectionTier.DpcOverlayRestrictedWithShield -> "dpc_overlay_restricted_with_shield"
    }
}

internal data class DpcRuntimeStatus(
    val deviceOwner: Boolean,
    val adminActive: Boolean,
    val lockTaskPermitted: Boolean,
    val createWindowsRestrictionSupported: Boolean,
    val createWindowsRestrictionActive: Boolean,
    val protectionTier: DpcProtectionTier
) {
    fun diagnosticSummary(): String =
        "device_owner=${yesNo(deviceOwner)}" +
            " | admin_active=${yesNo(adminActive)}" +
            " | lock_task_permitted=${yesNo(lockTaskPermitted)}" +
            " | create_windows_supported=${yesNo(createWindowsRestrictionSupported)}" +
            " | create_windows_active=${yesNo(createWindowsRestrictionActive)}" +
            " | tier=${protectionTier.diagnosticLabel()}"

    fun enrollmentLabel(): String {
        return when {
            deviceOwner -> "Device owner active"
            adminActive -> "Device admin only"
            else -> "Not enrolled"
        }
    }

    private fun yesNo(value: Boolean): String = if (value) "yes" else "no"
}

internal fun defaultDpcRuntimeStatus(
    sdkInt: Int = Build.VERSION_CODES.S,
    overlayShieldSupported: Boolean = sdkInt >= Build.VERSION_CODES.S
): DpcRuntimeStatus =
    DpcRuntimeStatus(
        deviceOwner = false,
        adminActive = false,
        lockTaskPermitted = false,
        createWindowsRestrictionSupported = sdkInt >= Build.VERSION_CODES.O,
        createWindowsRestrictionActive = false,
        protectionTier = resolveDpcProtectionTier(
            sdkInt = sdkInt,
            deviceOwner = false,
            overlayShieldSupported = overlayShieldSupported
        )
    )

internal data class DpcExamPolicyApplyResult(
    val before: DpcRuntimeStatus,
    val after: DpcRuntimeStatus,
    val lockTaskAllowlistApplied: Boolean,
    val createWindowsRestrictionApplied: Boolean,
    val createWindowsRestrictionUnsupported: Boolean,
    val error: String?
)

internal data class DpcExamPolicyClearResult(
    val before: DpcRuntimeStatus,
    val after: DpcRuntimeStatus,
    val createWindowsRestrictionCleared: Boolean,
    val skipped: Boolean,
    val error: String?
)

internal fun resolveDpcProtectionTier(
    sdkInt: Int,
    deviceOwner: Boolean,
    overlayShieldSupported: Boolean
): DpcProtectionTier {
    if (deviceOwner) {
        return when {
            sdkInt < Build.VERSION_CODES.O -> DpcProtectionTier.LegacyDpcAndroid7
            overlayShieldSupported -> DpcProtectionTier.DpcOverlayRestrictedWithShield
            else -> DpcProtectionTier.DpcOverlayRestricted
        }
    }
    return if (overlayShieldSupported) {
        DpcProtectionTier.NormalApk
    } else {
        DpcProtectionTier.None
    }
}

internal fun shouldApplyCreateWindowsRestriction(sdkInt: Int, deviceOwner: Boolean): Boolean =
    deviceOwner && sdkInt >= Build.VERSION_CODES.O

internal fun shouldClearCreateWindowsRestriction(sessionAppliedRestriction: Boolean): Boolean =
    sessionAppliedRestriction

internal object ExamDeviceOwnerController {
    fun adminComponent(context: Context): ComponentName =
        ComponentName(context.applicationContext, CbxDeviceAdminReceiver::class.java)

    fun enrollmentCommand(context: Context): String =
        "adb shell dpm set-device-owner ${context.packageName}/.CbxDeviceAdminReceiver"

    fun readStatus(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
        overlayShieldSupported: Boolean = sdkInt >= Build.VERSION_CODES.S
    ): DpcRuntimeStatus {
        val appContext = context.applicationContext
        val dpm = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val userManager = appContext.getSystemService(Context.USER_SERVICE) as? UserManager
        val admin = adminComponent(appContext)
        val deviceOwner = runCatching {
            dpm?.isDeviceOwnerApp(appContext.packageName) == true
        }.getOrDefault(false)
        val adminActive = runCatching {
            dpm?.isAdminActive(admin) == true
        }.getOrDefault(false)
        val lockTaskPermitted = runCatching {
            dpm?.isLockTaskPermitted(appContext.packageName) == true
        }.getOrDefault(false)
        val restrictionSupported = sdkInt >= Build.VERSION_CODES.O
        val restrictionActive = if (restrictionSupported) {
            runCatching {
                userManager?.hasUserRestriction(UserManager.DISALLOW_CREATE_WINDOWS) == true
            }.getOrDefault(false)
        } else {
            false
        }
        return DpcRuntimeStatus(
            deviceOwner = deviceOwner,
            adminActive = adminActive,
            lockTaskPermitted = lockTaskPermitted,
            createWindowsRestrictionSupported = restrictionSupported,
            createWindowsRestrictionActive = restrictionActive,
            protectionTier = resolveDpcProtectionTier(
                sdkInt = sdkInt,
                deviceOwner = deviceOwner,
                overlayShieldSupported = overlayShieldSupported
            )
        )
    }

    fun applyExamPolicies(context: Context): DpcExamPolicyApplyResult {
        val before = readStatus(context)
        if (!before.deviceOwner) {
            return DpcExamPolicyApplyResult(
                before = before,
                after = before,
                lockTaskAllowlistApplied = false,
                createWindowsRestrictionApplied = false,
                createWindowsRestrictionUnsupported = false,
                error = null
            )
        }

        val appContext = context.applicationContext
        val dpm = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val admin = adminComponent(appContext)
        var lockTaskAllowlistApplied = false
        var createWindowsRestrictionApplied = false
        var createWindowsRestrictionUnsupported = false
        var error: String? = null

        runCatching {
            dpm?.setLockTaskPackages(admin, arrayOf(appContext.packageName))
            lockTaskAllowlistApplied = true
        }.onFailure { throwable ->
            error = throwable.shortDiagnostic()
        }

        if (shouldApplyCreateWindowsRestriction(Build.VERSION.SDK_INT, before.deviceOwner)) {
            runCatching {
                dpm?.addUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS)
                createWindowsRestrictionApplied = true
            }.onFailure { throwable ->
                error = listOfNotNull(error, throwable.shortDiagnostic()).joinToString(" | ")
            }
        } else {
            createWindowsRestrictionUnsupported = true
        }

        return DpcExamPolicyApplyResult(
            before = before,
            after = readStatus(context),
            lockTaskAllowlistApplied = lockTaskAllowlistApplied,
            createWindowsRestrictionApplied = createWindowsRestrictionApplied,
            createWindowsRestrictionUnsupported = createWindowsRestrictionUnsupported,
            error = error
        )
    }

    fun clearCreateWindowsRestrictionIfSessionApplied(
        context: Context,
        sessionAppliedRestriction: Boolean
    ): DpcExamPolicyClearResult {
        val before = readStatus(context)
        if (!shouldClearCreateWindowsRestriction(sessionAppliedRestriction)) {
            return DpcExamPolicyClearResult(
                before = before,
                after = before,
                createWindowsRestrictionCleared = false,
                skipped = true,
                error = null
            )
        }

        val appContext = context.applicationContext
        val dpm = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val admin = adminComponent(appContext)
        var cleared = false
        var error: String? = null
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dpm?.clearUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS)
                cleared = true
            }
        }.onFailure { throwable ->
            error = throwable.shortDiagnostic()
        }

        return DpcExamPolicyClearResult(
            before = before,
            after = readStatus(context),
            createWindowsRestrictionCleared = cleared,
            skipped = false,
            error = error
        )
    }
}

private fun Throwable.shortDiagnostic(): String =
    message?.take(160) ?: javaClass.simpleName.take(160)
