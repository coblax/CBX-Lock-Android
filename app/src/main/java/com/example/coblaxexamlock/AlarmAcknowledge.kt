package com.example.coblaxexamlock

import java.net.URI
import java.security.MessageDigest
import java.util.Locale


internal enum class AlarmAcknowledgeType(
    val wireName: String,
    val telegramLabel: String
) {
    AppSwitch("app_switch", "App Switch"),
    Keyboard("keyboard", "Keyboard"),
    Overlay("overlay", "Overlay"),
    Bluetooth("bluetooth", "Bluetooth"),
    Clipboard("clipboard", "Clipboard"),
    Geofence("geofence", "Geofence"),
    FakeLocation("fake_location", "Anti-Fake-Location")
}

internal data class AlarmSessionIdentity(
    val examName: String,
    val examUrlHost: String,
    val examUrlHashShort: String,
    val participantContext: ExamParticipantContext? = null
)

internal data class AlarmAcknowledgePayload(
    val timestamp: String,
    val alarmType: AlarmAcknowledgeType,
    val ackAction: String = "i_understand",
    val examName: String,
    val examUrlHost: String,
    val examUrlHashShort: String,
    val deviceLabel: String,
    val appVersion: String,
    val adminOverridesSummary: String,
    val examSessionStarted: Boolean,
    val runtimeGuardsArmed: Boolean,
    val violationCount: Int,
    val detailRef: String,
    val participantContext: ExamParticipantContext? = null,
    val lastTrigger: String? = null,
    val fallbackGuardActive: Boolean? = null,
    val lastConfirmedAt: String? = null,
    val lastDecision: String? = null,
    val clipboardBaselineSemanticSignature: String? = null,
    val clipboardDetectedSemanticSignature: String? = null,
    val clipboardCurrentSemanticSignature: String? = null,
    val keyboardLabel: String? = null,
    val overlayHeuristicRisk: Boolean? = null,
    val overlayConfirmed: Boolean? = null,
    val overlayLastTrigger: String? = null,
    val overlayLastDetectedAt: String? = null,
    val overlayLastContext: String? = null,
    val overlayShieldActive: Boolean? = null,
    val bluetoothEnabled: Boolean? = null,
    val geofencePolicySource: String? = null,
    val geofenceEnabled: Boolean? = null,
    val geofenceShapeType: String? = null,
    val geofencePolygonVertexCount: Int? = null,
    val geofencePolygonVerticesSummary: String? = null,
    val geofenceCircleCenterCount: Int? = null,
    val geofenceCircleCentersSummary: String? = null,
    val geofenceVerdict: String? = null,
    val geofenceCurrentCoordinates: String? = null,
    val geofenceCenterCoordinates: String? = null,
    val geofenceRadiusMeters: String? = null,
    val geofenceDistanceMeters: String? = null,
    val geofenceProvider: String? = null,
    val geofenceAccuracyMeters: String? = null,
    val geofenceFixQuality: String? = null,
    val geofenceFixAge: String? = null,
    val geofencePermissionGranted: Boolean? = null,
    val geofenceServicesEnabled: Boolean? = null,
    val geofencePreciseGranted: Boolean? = null,
    val fakeLocationBypassState: String? = null,
    val fakeLocationVerdict: String? = null,
    val fakeLocationConfidenceTier: String? = null,
    val fakeLocationFixQuality: String? = null,
    val fakeLocationFixQualityEligible: Boolean? = null,
    val fakeLocationPermissionGranted: Boolean? = null,
    val fakeLocationServicesEnabled: Boolean? = null,
    val fakeLocationSnapshotAvailable: Boolean? = null,
    val fakeLocationMockDetected: Boolean? = null,
    val fakeLocationDeveloperOptionsEnabled: Boolean? = null,
    val fakeLocationSuspiciousPackages: String? = null,
    val fakeLocationSignals: String? = null
)

internal fun buildAlarmSessionIdentity(
    payload: ExamQrPayload,
    participantContext: ExamParticipantContext? = null
): AlarmSessionIdentity {
    val examUrl = payload.examUrl.trim()
    val host = extractAlarmSessionHost(examUrl)
    return AlarmSessionIdentity(
        examName = payload.examName.trim().ifBlank { "-" },
        examUrlHost = host,
        examUrlHashShort = shortAlarmSessionHash(examUrl),
        participantContext = participantContext
    )
}

private fun extractAlarmSessionHost(examUrl: String): String {
    if (examUrl.isBlank()) {
        return "-"
    }

    val parsedHost = runCatching { URI(examUrl).host?.trim().orEmpty() }
        .getOrDefault("")
        .ifBlank {
            examUrl.substringAfter("://", examUrl)
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .trim()
        }

    return parsedHost.ifBlank { "-" }
}

private fun shortAlarmSessionHash(rawValue: String): String {
    if (rawValue.isBlank()) {
        return "-"
    }

    val digest = MessageDigest.getInstance("SHA-256")
        .digest(rawValue.toByteArray(Charsets.UTF_8))

    return digest.joinToString("") { byte ->
        String.format(Locale.US, "%02X", byte)
    }.take(12)
}
