package com.coblax.examlock.runtime

import android.content.Context
import android.os.Build
import com.coblax.examlock.AdbBypassState
import com.coblax.examlock.AdbInspection
import com.coblax.examlock.AlarmAcknowledgePayload
import com.coblax.examlock.AlarmAcknowledgeType
import com.coblax.examlock.AppSwitchStatus
import com.coblax.examlock.BuildConfig
import com.coblax.examlock.ClipboardRuntimeStatus
import com.coblax.examlock.ExamParticipantContext
import com.coblax.examlock.FakeLocationRuntimeStatus
import com.coblax.examlock.GeofenceRuntimeStatus
import com.coblax.examlock.IntegrityCheckResult
import com.coblax.examlock.IntegrityGuard
import com.coblax.examlock.OverlayRiskResult
import com.coblax.examlock.ReverseEngineeringGuard
import com.coblax.examlock.ReverseEngineeringResult
import com.coblax.examlock.RootBypassState
import com.coblax.examlock.RootSecurityStatus
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.SignatureIntegrity
import com.coblax.examlock.diagnosticLabel
import com.coblax.examlock.formatCoordinates
import com.coblax.examlock.config.TelegramMessageChunkLimit
import com.coblax.examlock.format.diagnosticSectionEventCodes
import com.coblax.examlock.format.diagnosticTimestamp
import com.coblax.examlock.format.formatElapsedDuration
import com.coblax.examlock.format.formatGeofenceDistance
import com.coblax.examlock.format.formatLocationFixAge
import com.coblax.examlock.i18n.diagnosticSectionLabel
import com.coblax.examlock.inspectAccessibility
import com.coblax.examlock.model.DiagnosticEvent
import com.coblax.examlock.model.DiagnosticSection
import com.coblax.examlock.model.ExamNetworkStatus
import com.coblax.examlock.model.ExamOfflineRuntimeStatus
import com.coblax.examlock.model.UiLanguage
import com.coblax.examlock.resolveExpectedSigningFingerprints
import com.coblax.examlock.ui.geofence.effectiveCircleCenters
import com.coblax.examlock.ui.geofence.summarizeCircleCenters
import com.coblax.examlock.ui.geofence.summarizePolygonVertices
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun sendTelegramAlarmAcknowledge(
    payload: AlarmAcknowledgePayload
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        require(BuildConfig.REMOTE_DIAGNOSTICS_ENABLED) {
            "Diagnostik jarak jauh dinonaktifkan untuk build ini."
        }

        val token = SecureStrings.telegramBotToken.trim()
        val chatId = SecureStrings.telegramBugChatId.trim()

        require(token.isNotBlank()) { "Token Telegram belum dikonfigurasi." }
        require(chatId.isNotBlank()) { "Chat ID Telegram belum dikonfigurasi." }

        val message = buildString {
            appendLine("[ALARM ACKNOWLEDGED]")
            appendLine("Waktu: ${payload.timestamp}")
            appendLine("Action: ${payload.ackAction}")
            appendLine("Alarm: ${payload.alarmType.telegramLabel}")
            appendLine("Ujian: ${payload.examName.ifBlank { "-" }}")
            appendLine("Session URL host: ${payload.examUrlHost.ifBlank { "-" }}")
            appendLine("Session URL hash: ${payload.examUrlHashShort.ifBlank { "-" }}")
            appendLine("Perangkat: ${payload.deviceLabel.ifBlank { "-" }}")
            appendLine("App version: ${payload.appVersion.ifBlank { "-" }}")
            appendLine("Admin overrides: ${payload.adminOverridesSummary.ifBlank { "-" }}")
            appendLine("Sesi ujian dimulai: ${if (payload.examSessionStarted) "Ya" else "Belum"}")
            appendLine("Runtime guards armed: ${if (payload.runtimeGuardsArmed) "Ya" else "Tidak"}")
            payload.participantContext?.appendTelegramLines(this)
            appendLine("Violation count: ${payload.violationCount}")
            appendLine("Detail ref: ${payload.detailRef.ifBlank { "-" }}")

            when (payload.alarmType) {
                AlarmAcknowledgeType.AppSwitch -> {
                    appendLine("Last trigger: ${payload.lastTrigger?.ifBlank { "-" } ?: "-"}")
                    appendLine(
                        "Fallback guard active: ${
                            payload.fallbackGuardActive?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                }
                AlarmAcknowledgeType.Keyboard -> {
                    appendLine("Detected keyboard: ${payload.keyboardLabel?.ifBlank { "-" } ?: "-"}")
                }
                AlarmAcknowledgeType.Overlay -> {
                    appendLine(
                        "Overlay heuristic risk: ${
                            payload.overlayHeuristicRisk?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Overlay confirmed: ${
                            payload.overlayConfirmed?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine("Overlay last trigger: ${payload.overlayLastTrigger?.ifBlank { "-" } ?: "-"}")
                    appendLine("Overlay last timestamp: ${payload.overlayLastDetectedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine("Overlay last context: ${payload.overlayLastContext?.ifBlank { "-" } ?: "-"}")
                    appendLine(
                        "Overlay shield active: ${
                            payload.overlayShieldActive?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                }
                AlarmAcknowledgeType.Bluetooth -> {
                    appendLine(
                        "Bluetooth enabled: ${
                            payload.bluetoothEnabled?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                }
                AlarmAcknowledgeType.Clipboard -> {
                    appendLine("Last confirmed change: ${payload.lastConfirmedAt?.ifBlank { "-" } ?: "-"}")
                    appendLine("Last decision: ${payload.lastDecision?.ifBlank { "-" } ?: "-"}")
                    appendLine(
                        "Baseline semantic signature: ${
                            payload.clipboardBaselineSemanticSignature?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Detected semantic signature: ${
                            payload.clipboardDetectedSemanticSignature?.ifBlank { "-" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Current semantic signature: ${
                            payload.clipboardCurrentSemanticSignature?.ifBlank { "-" } ?: "-"
                        }"
                    )
                }
                AlarmAcknowledgeType.Geofence -> {
                    appendLine("Location policy source: ${payload.geofencePolicySource?.ifBlank { "-" } ?: "-"}")
                    appendLine(
                        "Geofence enabled: ${
                            payload.geofenceEnabled?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine("Geofence shape: ${payload.geofenceShapeType?.ifBlank { "-" } ?: "-"}")
                    appendLine("Polygon vertex count: ${payload.geofencePolygonVertexCount?.toString() ?: "-"}")
                    appendLine("Polygon vertices: ${payload.geofencePolygonVerticesSummary?.ifBlank { "-" } ?: "-"}")
                    appendLine("Circle center count: ${payload.geofenceCircleCenterCount?.toString() ?: "-"}")
                    appendLine("Circle centers: ${payload.geofenceCircleCentersSummary?.ifBlank { "-" } ?: "-"}")
                    appendLine("Geofence verdict: ${payload.geofenceVerdict?.ifBlank { "-" } ?: "-"}")
                    appendLine("Current coordinates: ${payload.geofenceCurrentCoordinates?.ifBlank { "-" } ?: "-"}")
                    appendLine("Closest / primary center: ${payload.geofenceCenterCoordinates?.ifBlank { "-" } ?: "-"}")
                    appendLine("Shared radius meters: ${payload.geofenceRadiusMeters?.ifBlank { "-" } ?: "-"}")
                    appendLine("Distance from closest center: ${payload.geofenceDistanceMeters?.ifBlank { "-" } ?: "-"}")
                    appendLine("Location provider: ${payload.geofenceProvider?.ifBlank { "-" } ?: "-"}")
                    appendLine("Location accuracy meters: ${payload.geofenceAccuracyMeters?.ifBlank { "-" } ?: "-"}")
                    appendLine("Location fix quality: ${payload.geofenceFixQuality?.ifBlank { "-" } ?: "-"}")
                    appendLine("Location fix age: ${payload.geofenceFixAge?.ifBlank { "-" } ?: "-"}")
                    appendLine(
                        "Location permission granted: ${
                            payload.geofencePermissionGranted?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Precise location granted: ${
                            payload.geofencePreciseGranted?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Location services enabled: ${
                            payload.geofenceServicesEnabled?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                }
                AlarmAcknowledgeType.FakeLocation -> {
                    appendLine("Fake-location bypass state: ${payload.fakeLocationBypassState?.ifBlank { "-" } ?: "-"}")
                    appendLine("Fake-location verdict: ${payload.fakeLocationVerdict?.ifBlank { "-" } ?: "-"}")
                    appendLine("Confidence tier: ${payload.fakeLocationConfidenceTier?.ifBlank { "-" } ?: "-"}")
                    appendLine("Fix quality: ${payload.fakeLocationFixQuality?.ifBlank { "-" } ?: "-"}")
                    appendLine(
                        "Fix-quality eligible: ${
                            payload.fakeLocationFixQualityEligible?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Location permission granted: ${
                            payload.fakeLocationPermissionGranted?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Location services enabled: ${
                            payload.fakeLocationServicesEnabled?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Location snapshot available: ${
                            payload.fakeLocationSnapshotAvailable?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Mock location flag: ${
                            payload.fakeLocationMockDetected?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine(
                        "Developer options enabled: ${
                            payload.fakeLocationDeveloperOptionsEnabled?.let { if (it) "Ya" else "Tidak" } ?: "-"
                        }"
                    )
                    appendLine("Suspicious fake-location packages: ${payload.fakeLocationSuspiciousPackages?.ifBlank { "-" } ?: "-"}")
                    appendLine("Supporting signals: ${payload.fakeLocationSignals?.ifBlank { "-" } ?: "-"}")
                }
            }
        }

        val queue = TelegramMessageQueueHolder.instance
        buildTelegramMessageChunks(message).forEach { chunk ->
            queue.send(
                token = token,
                chatId = chatId,
                message = chunk
            )
        }
    }
}
