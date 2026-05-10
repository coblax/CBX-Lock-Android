package com.example.coblaxexamlock.model

import com.example.coblaxexamlock.ExamQrLocationPolicy
import com.example.coblaxexamlock.GeofenceShapeType
import com.example.coblaxexamlock.GeofenceVertex
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.config.DefaultExamUserAgent
import com.example.coblaxexamlock.config.FastExamName
import com.example.coblaxexamlock.persistence.deserializeExamLocationPolicy
import com.example.coblaxexamlock.persistence.serializeExamLocationPolicy

internal enum class AppScreen {
    Home,
    CustomQrAdmin,
    ExamWebView,
    SecretAdmin
}

internal enum class UiLanguage(val code: String) {
    English("en"),
    Indonesian("id")
}
internal enum class DateTimeField {
    Start,
    End
}

internal enum class SecretAdminTab {
    Setup,
    Security
}

internal enum class CustomQrAdminTab {
    Exam,
    Location,
    Generate
}
internal data class AdminSettings(
    val fastExamUrl: String = SecureStrings.fastExamUrl,
    val fastExamLabel: String = FastExamName,
    val officialApkUrl: String = "",
    val examUserAgent: String = DefaultExamUserAgent,
    val directLinkLocationPolicySaved: Boolean = false,
    val directLinkLocationPolicySerialized: String = "",
    val directLinkGeofenceEnabled: Boolean = false,
    val directLinkGeofenceCenterLat: String = "",
    val directLinkGeofenceCenterLng: String = "",
    val directLinkGeofenceRadiusMeters: String = "",
    val customQrSaveToDirectLinkEnabled: Boolean = true,
    val bypassScreenPinning: Boolean = false,
    val screenPinningBypassTampered: Boolean = false,
    val bypassBluetooth: Boolean = false,
    val bypassAccessibility: Boolean = false,
    val accessibilityBypassTampered: Boolean = false,
    val bypassAdb: Boolean = false,
    val adbBypassTampered: Boolean = false,
    val bypassRoot: Boolean = false,
    val rootBypassTampered: Boolean = false,
    val bypassVirtualEnvironment: Boolean = false,
    val bypassVpn: Boolean = false,
    val vpnBypassTampered: Boolean = false,
    val bypassKeyboardPolicy: Boolean = false,
    val bypassClipboard: Boolean = false,
    val clipboardBypassTampered: Boolean = false,
    val bypassOverlay: Boolean = false,
    val overlayBypassTampered: Boolean = false,
    val bypassGeofence: Boolean = false,
    val geofenceBypassTampered: Boolean = false,
    val bypassFakeLocation: Boolean = false,
    val fakeLocationBypassTampered: Boolean = false,
    val bypassDeviceTime: Boolean = false,
    val deviceTimeBypassTampered: Boolean = false,
    val bypassLocation: Boolean = false,
    val locationBypassTampered: Boolean = false,
    val bypassAppSwitch: Boolean = false,
    val appSwitchBypassTampered: Boolean = false,
    val bypassScreenRecorder: Boolean = false,
    val screenRecorderBypassTampered: Boolean = false,
    val bypassDisplayMirror: Boolean = false,
    val displayMirrorBypassTampered: Boolean = false,
    val bypassMultiWindow: Boolean = false,
    val multiWindowBypassTampered: Boolean = false,
    val showChecklistDetails: Boolean = false,
    val bypassMigrationResetNotice: Boolean = false
) {
    fun hasAnyBypass(): Boolean {
        return bypassScreenPinning ||
            bypassBluetooth ||
            bypassAccessibility ||
            bypassAdb ||
            bypassRoot ||
            bypassVirtualEnvironment ||
            bypassVpn ||
            bypassKeyboardPolicy ||
            bypassClipboard ||
            bypassOverlay ||
            bypassGeofence ||
            bypassFakeLocation ||
            bypassDeviceTime ||
            bypassAppSwitch
    }

    fun overrideSummary(): String {
        val overrides = mutableListOf<String>()
        if (bypassScreenPinning) overrides.add("screen pinning")
        if (bypassBluetooth) overrides.add("bluetooth")
        if (bypassAccessibility) overrides.add("accessibility")
        if (bypassAdb) overrides.add("adb")
        if (bypassRoot) overrides.add("root")
        if (bypassVirtualEnvironment) overrides.add("virtual env")
        if (bypassVpn) overrides.add("vpn")
        if (bypassKeyboardPolicy) overrides.add("keyboard policy")
        if (bypassClipboard) overrides.add("clipboard")
        if (bypassOverlay) overrides.add("overlay")
        if (bypassGeofence) overrides.add("geofence")
        if (bypassFakeLocation) overrides.add("fake location")
        if (bypassDeviceTime) overrides.add("device time")
        if (bypassAppSwitch) overrides.add("app switch")
        return overrides.joinToString().ifBlank { "-" }
    }
}

internal fun normalizeExamUserAgent(value: String?): String {
    return value.orEmpty().trim().ifBlank { DefaultExamUserAgent }
}

internal fun AdminSettings.effectiveExamUserAgent(): String = normalizeExamUserAgent(examUserAgent)

internal fun AdminSettings.usesDefaultExamUserAgent(): Boolean {
    return effectiveExamUserAgent() == DefaultExamUserAgent
}

internal fun AdminSettings.directLinkLocationPolicy(): ExamQrLocationPolicy? {
    if (!directLinkLocationPolicySaved) {
        return null
    }
    deserializeExamLocationPolicy(directLinkLocationPolicySerialized)?.let { return it }
    val legacyCircleCenters = if (
        directLinkGeofenceEnabled &&
        directLinkGeofenceCenterLat.isNotBlank() &&
        directLinkGeofenceCenterLng.isNotBlank()
    ) {
        listOf(
            GeofenceVertex(
                latitude = directLinkGeofenceCenterLat,
                longitude = directLinkGeofenceCenterLng
            )
        )
    } else {
        emptyList()
    }
    return ExamQrLocationPolicy(
        shapeType = if (directLinkGeofenceEnabled) {
            GeofenceShapeType.Circle
        } else {
            GeofenceShapeType.Disabled
        },
        centerLat = directLinkGeofenceCenterLat,
        centerLng = directLinkGeofenceCenterLng,
        radiusMeters = directLinkGeofenceRadiusMeters,
        circleCenters = legacyCircleCenters
    )
}

internal fun AdminSettings.withDirectLinkLocationPolicy(
    policy: ExamQrLocationPolicy
): AdminSettings {
    return copy(
        directLinkLocationPolicySaved = true,
        directLinkLocationPolicySerialized = serializeExamLocationPolicy(policy),
        directLinkGeofenceEnabled = policy.geofenceEnabled,
        directLinkGeofenceCenterLat = policy.centerLat,
        directLinkGeofenceCenterLng = policy.centerLng,
        directLinkGeofenceRadiusMeters = policy.radiusMeters
    )
}

internal fun AdminSettings.withoutDirectLinkLocationPolicy(): AdminSettings {
    return copy(
        directLinkLocationPolicySaved = false,
        directLinkLocationPolicySerialized = "",
        directLinkGeofenceEnabled = false,
        directLinkGeofenceCenterLat = "",
        directLinkGeofenceCenterLng = "",
        directLinkGeofenceRadiusMeters = ""
    )
}
internal enum class ExamKeyboardPolicy {
    Allow,
    Block
}

internal data class ExamKeyboardRule(
    val packageName: String,
    val policy: ExamKeyboardPolicy,
    val note: String
)
