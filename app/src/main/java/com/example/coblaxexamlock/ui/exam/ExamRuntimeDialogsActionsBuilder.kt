package com.example.coblaxexamlock.ui.exam

import android.os.Build
import androidx.compose.foundation.layout.size
import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.AlarmSessionIdentity
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.FakeLocationBypassState
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.model.NetworkReadinessStatus
import com.example.coblaxexamlock.model.NetworkUnstableRuntimeStatus
import com.example.coblaxexamlock.openBluetoothSettings
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.ui.dialog.ExamRuntimeDialogsActions
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
import java.util.Locale

internal fun buildExamRuntimeDialogsActions(
    forcedExitViolationCount: Int,
    appSwitchStatus: AppSwitchStatus,
    keyboardViolationCount: Int,
    currentKeyboardLabel: String,
    overlayViolationCount: Int,
    overlayRiskResult: OverlayRiskResult,
    lastConnectedNetworkLabel: String?,
    offlineWarningDurationMs: Long?,
    currentOfflineDurationMs: Long?,
    networkReadinessStatus: NetworkReadinessStatus,
    networkUnstableRuntimeStatus: NetworkUnstableRuntimeStatus,
    geofenceViolationCount: Int,
    geofenceRuntimeStatus: GeofenceRuntimeStatus,
    fakeLocationViolationCount: Int,
    fakeLocationRuntimeStatus: FakeLocationRuntimeStatus,
    bluetoothViolationCount: Int,
    bluetoothEnabled: Boolean,
    clipboardViolationCount: Int,
    lastClipboardConfirmedAt: String?,
    lastClipboardDecision: String,
    clipboardRuntimeStatus: ClipboardRuntimeStatus,
    alarmSessionIdentity: AlarmSessionIdentity,
    appVersionName: String,
    adminOverridesSummary: String,
    examSessionStarted: Boolean,
    examGuardArmed: Boolean,
    acknowledgeRuntimeAlarm: (
        AlarmAcknowledgeType,
        Int,
        (String) -> AlarmAcknowledgePayload,
        () -> Unit
    ) -> Unit,
    recordAction: (String, String, DiagnosticEventLevel) -> Unit,
    currentNetworkEventDetails: (String, NetworkReadinessStatus, String?) -> String,
    dismissForcedExitAlarm: () -> Unit,
    dismissKeyboardViolationDialog: () -> Unit,
    dismissOverlayViolationDialog: () -> Unit,
    dismissOfflineWarningDialog: () -> Unit,
    dismissNetworkUnstableDialog: () -> Unit,
    dismissGeofenceViolationDialog: () -> Unit,
    dismissFakeLocationViolationDialog: () -> Unit,
    openBluetoothSettings: () -> Unit,
    dismissBluetoothViolationDialog: () -> Unit,
    refreshBluetoothSecurity: () -> Unit,
    dismissClipboardViolationDialog: () -> Unit,
    dismissExitExamDialog: () -> Unit,
    confirmExitExam: () -> Unit
): ExamRuntimeDialogsActions {
    return ExamRuntimeDialogsActions(
        onAcknowledgeForcedExit = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.AppSwitch,
                forcedExitViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.AppSwitch,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = forcedExitViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        lastTrigger = appSwitchStatus.lastTrigger,
                        fallbackGuardActive = appSwitchStatus.fallbackGuardActive
                    )
                },
                dismissForcedExitAlarm
            )
        },
        onAcknowledgeKeyboard = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Keyboard,
                keyboardViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Keyboard,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = keyboardViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        keyboardLabel = currentKeyboardLabel.ifBlank { "-" }
                    )
                },
                dismissKeyboardViolationDialog
            )
        },
        onAcknowledgeOverlay = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Overlay,
                overlayViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Overlay,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = overlayViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        overlayHeuristicRisk = overlayRiskResult.heuristicRisk,
                        overlayConfirmed = overlayRiskResult.confirmedInteractionDetected,
                        overlayLastTrigger = overlayRiskResult.lastTrigger,
                        overlayLastDetectedAt = overlayRiskResult.lastDetectedAt,
                        overlayLastContext = overlayRiskResult.lastContext,
                        overlayShieldActive = overlayRiskResult.shieldStatus.active
                    )
                },
                dismissOverlayViolationDialog
            )
        },
        onAcknowledgeOffline = {
            recordAction(
                "NETWORK_OFFLINE_WARNING_ACKNOWLEDGED",
                buildString {
                    append("last_transport=")
                    append(lastConnectedNetworkLabel?.ifBlank { "-" } ?: "-")
                    append(" | duration_ms=")
                    append(offlineWarningDurationMs ?: currentOfflineDurationMs ?: 0L)
                },
                DiagnosticEventLevel.INFO
            )
            dismissOfflineWarningDialog()
        },
        onAcknowledgeNetworkUnstable = {
            recordAction(
                "NETWORK_UNSTABLE_WARNING_ACKNOWLEDGED",
                currentNetworkEventDetails(
                    "unstable_ack",
                    networkReadinessStatus,
                    "flap_count=${networkUnstableRuntimeStatus.flapCount}"
                ),
                DiagnosticEventLevel.INFO
            )
            dismissNetworkUnstableDialog()
        },
        onAcknowledgeGeofence = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Geofence,
                geofenceViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Geofence,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim(),
                        appVersion = BuildConfig.VERSION_NAME,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = geofenceViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        geofencePolicySource = geofenceRuntimeStatus.policySource.diagnosticLabel(),
                        geofenceEnabled = geofenceRuntimeStatus.evaluation.enabled,
                        geofenceShapeType = geofenceRuntimeStatus.evaluation.config?.shapeType?.name?.lowercase(Locale.US),
                        geofencePolygonVertexCount = geofenceRuntimeStatus.evaluation.config?.vertices?.size,
                        geofencePolygonVerticesSummary = summarizePolygonVertices(
                            geofenceRuntimeStatus.evaluation.config?.vertices.orEmpty()
                        ),
                        geofenceCircleCenterCount = effectiveCircleCenters(
                            geofenceRuntimeStatus.evaluation.config
                        ).size,
                        geofenceCircleCentersSummary = summarizeCircleCenters(
                            effectiveCircleCenters(geofenceRuntimeStatus.evaluation.config)
                        ),
                        geofenceVerdict = geofenceRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel(),
                        geofenceCurrentCoordinates = geofenceRuntimeStatus.evaluation.locationSnapshot?.let {
                            formatCoordinates(it.latitude, it.longitude)
                        },
                        geofenceCenterCoordinates = geofenceRuntimeStatus.evaluation.closestCircleCenter?.let {
                            formatCoordinates(it.latitude, it.longitude)
                        } ?: geofenceRuntimeStatus.evaluation.config?.let {
                            formatCoordinates(it.centerLat, it.centerLng)
                        },
                        geofenceRadiusMeters = geofenceRuntimeStatus.evaluation.config?.radiusMeters?.let {
                            String.format(Locale.US, "%.1f", it)
                        },
                        geofenceDistanceMeters = geofenceRuntimeStatus.evaluation.distanceMeters?.let {
                            String.format(Locale.US, "%.1f", it)
                        },
                        geofenceProvider = geofenceRuntimeStatus.evaluation.locationSnapshot?.provider,
                        geofenceAccuracyMeters = geofenceRuntimeStatus.evaluation.locationSnapshot?.accuracyMeters?.let {
                            String.format(Locale.US, "%.1f", it)
                        },
                        geofenceFixQuality = geofenceRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel(),
                        geofenceFixAge = formatLocationFixAge(geofenceRuntimeStatus.securityStatus.fixQualityStatus.ageMs),
                        geofencePermissionGranted = geofenceRuntimeStatus.evaluation.permissionGranted,
                        geofenceServicesEnabled = geofenceRuntimeStatus.evaluation.locationServicesEnabled,
                        geofencePreciseGranted = geofenceRuntimeStatus.securityStatus.preciseLocationGranted
                    )
                },
                dismissGeofenceViolationDialog
            )
        },
        onAcknowledgeFakeLocation = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.FakeLocation,
                fakeLocationViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.FakeLocation,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim(),
                        appVersion = BuildConfig.VERSION_NAME,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = fakeLocationViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        fakeLocationBypassState = fakeLocationRuntimeStatus.securityStatus.bypassState.name.lowercase(Locale.US),
                        fakeLocationVerdict = fakeLocationRuntimeStatus.securityStatus.finalVerdict.diagnosticLabel(),
                        fakeLocationConfidenceTier = fakeLocationRuntimeStatus.securityStatus.confidenceTier.diagnosticLabel(),
                        fakeLocationFixQuality = fakeLocationRuntimeStatus.securityStatus.fixQualityStatus.verdict.diagnosticLabel(),
                        fakeLocationFixQualityEligible = fakeLocationRuntimeStatus.securityStatus.fixQualityEligible,
                        fakeLocationPermissionGranted = fakeLocationRuntimeStatus.securityStatus.permissionGranted,
                        fakeLocationServicesEnabled = fakeLocationRuntimeStatus.securityStatus.locationServicesEnabled,
                        fakeLocationSnapshotAvailable = fakeLocationRuntimeStatus.securityStatus.snapshotAvailable,
                        fakeLocationMockDetected = fakeLocationRuntimeStatus.securityStatus.mockLocationDetected,
                        fakeLocationDeveloperOptionsEnabled = fakeLocationRuntimeStatus.securityStatus.developerOptionsEnabled,
                        fakeLocationSuspiciousPackages = fakeLocationRuntimeStatus.securityStatus.suspiciousFakeLocationPackages.joinToString().ifBlank { "-" },
                        fakeLocationSignals = fakeLocationRuntimeStatus.securityStatus.supportingSignals
                            .map { it.diagnosticLabel() }
                            .joinToString()
                            .ifBlank { "-" }
                    )
                },
                dismissFakeLocationViolationDialog
            )
        },
        onOpenBluetoothSettings = openBluetoothSettings,
        onAcknowledgeBluetooth = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Bluetooth,
                bluetoothViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Bluetooth,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = bluetoothViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        bluetoothEnabled = bluetoothEnabled
                    )
                },
                {
                    dismissBluetoothViolationDialog()
                    refreshBluetoothSecurity()
                }
            )
        },
        onAcknowledgeClipboard = {
            acknowledgeRuntimeAlarm(
                AlarmAcknowledgeType.Clipboard,
                clipboardViolationCount,
                { detailRef ->
                    AlarmAcknowledgePayload(
                        timestamp = diagnosticTimestamp(),
                        alarmType = AlarmAcknowledgeType.Clipboard,
                        examName = alarmSessionIdentity.examName,
                        examUrlHost = alarmSessionIdentity.examUrlHost,
                        examUrlHashShort = alarmSessionIdentity.examUrlHashShort,
                        deviceLabel = "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "-" },
                        appVersion = appVersionName,
                        adminOverridesSummary = adminOverridesSummary,
                        examSessionStarted = examSessionStarted,
                        runtimeGuardsArmed = examGuardArmed,
                        violationCount = clipboardViolationCount,
                        detailRef = detailRef,
                        participantContext = alarmSessionIdentity.participantContext,
                        lastConfirmedAt = lastClipboardConfirmedAt,
                        lastDecision = lastClipboardDecision,
                        clipboardBaselineSemanticSignature = clipboardRuntimeStatus.baselineSemanticSignature,
                        clipboardDetectedSemanticSignature = clipboardRuntimeStatus.detectedSemanticSignature,
                        clipboardCurrentSemanticSignature = clipboardRuntimeStatus.currentSemanticSignature
                    )
                },
                dismissClipboardViolationDialog
            )
        },
        onDismissExitExam = dismissExitExamDialog,
        onConfirmExitExam = confirmExitExam
    )
}
