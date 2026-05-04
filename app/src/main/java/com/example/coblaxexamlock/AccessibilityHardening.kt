package com.example.coblaxexamlock

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.coblaxexamlock.config.RiskyAccessibilityKeywords


private val AllowedAccessibilityServiceComponents = setOf(
    "com.eset.ems2.gp/com.eset.commoncore.core.accessibility.CoreAccessibilityService"
)

private val AllowedAccessibilityPackages = setOf(
    "com.eset.ems2.gp"
)

internal enum class AccessibilityBypassState {
    Active,
    Inactive,
    Tampered
}

internal object AccessibilityBypassResolver {
    fun stateOf(enabled: Boolean, tampered: Boolean): AccessibilityBypassState {
        return when {
            tampered -> AccessibilityBypassState.Tampered
            enabled -> AccessibilityBypassState.Active
            else -> AccessibilityBypassState.Inactive
        }
    }
}

internal data class AccessibilityInspectionResult(
    val managerEnabled: Boolean,
    val touchExplorationEnabled: Boolean,
    val rawEnabledServices: String,
    val activeServiceComponents: List<String>,
    val activePackages: List<String>,
    val allowedServiceComponents: List<String>,
    val allowedPackages: List<String>,
    val effectiveServiceComponents: List<String>,
    val effectivePackages: List<String>,
    val riskyPackages: List<String>
) {
    val blockingServiceActive: Boolean
        get() = managerEnabled && effectiveServiceComponents.isNotEmpty()

    val allowedOnlyActive: Boolean
        get() = managerEnabled && effectiveServiceComponents.isEmpty() && allowedServiceComponents.isNotEmpty()
}

internal fun inspectAccessibility(context: Context): AccessibilityInspectionResult {
    val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
    val managerEnabled = accessibilityManager?.isEnabled == true
    val touchExplorationEnabled = accessibilityManager?.isTouchExplorationEnabled == true
    val rawValue = runCatching {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
    }.getOrDefault("")
    val activeServiceComponents = rawValue.split(':')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val activePackages = activeServiceComponents
        .map { it.substringBefore('/').trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val allowedServiceComponents = activeServiceComponents.filter { serviceComponent ->
        isAllowedAccessibilityService(serviceComponent)
    }
    val allowedPackages = activePackages.filter { packageName ->
        isAllowedAccessibilityPackage(packageName)
    }
    val effectiveServiceComponents = activeServiceComponents.filterNot { serviceComponent ->
        isAllowedAccessibilityService(serviceComponent)
    }
    val effectivePackages = effectiveServiceComponents
        .map { it.substringBefore('/').trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val riskyPackages = effectivePackages.filter { packageName ->
        RiskyAccessibilityKeywords.any { keyword -> packageName.contains(keyword, ignoreCase = true) }
    }

    return AccessibilityInspectionResult(
        managerEnabled = managerEnabled,
        touchExplorationEnabled = touchExplorationEnabled,
        rawEnabledServices = rawValue.ifBlank { "-" },
        activeServiceComponents = activeServiceComponents,
        activePackages = activePackages,
        allowedServiceComponents = allowedServiceComponents,
        allowedPackages = allowedPackages,
        effectiveServiceComponents = effectiveServiceComponents,
        effectivePackages = effectivePackages,
        riskyPackages = riskyPackages
    )
}

internal fun isAllowedAccessibilityService(serviceComponent: String): Boolean {
    if (serviceComponent.isBlank()) {
        return false
    }
    val normalized = serviceComponent.trim()
    return normalized in AllowedAccessibilityServiceComponents ||
        normalized.substringBefore('/').trim() in AllowedAccessibilityPackages ||
        isExamGuardAccessibilityComponent(normalized)
}

internal fun isAllowedAccessibilityPackage(packageName: String): Boolean {
    val normalized = packageName.trim()
    return normalized.isNotBlank() &&
        (normalized in AllowedAccessibilityPackages || normalized == "com.example.coblaxexamlock")
}
