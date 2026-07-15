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

internal fun formatDiagnosticEvent(event: DiagnosticEvent): String {
    val sessionPart = event.sessionElapsedMs?.let { " | session=${it}ms" } ?: ""
    val detailsPart = event.details.takeIf { it.isNotBlank() && it != "-" }?.let { " | $it" } ?: ""
    return "${event.timestamp} | ${event.level} | ${event.code} | screen=${event.screen} | app=${event.appElapsedMs}ms$sessionPart$detailsPart"
}

internal fun ExamParticipantContext.appendTelegramLines(builder: StringBuilder) {
    builder.appendLine("Peserta: ${displayName.orEmpty().ifBlank { "-" }}")
    builder.appendLine("User ID: ${userId.orEmpty().ifBlank { "-" }}")
    builder.appendLine("Username: ${username.orEmpty().ifBlank { "-" }}")
    builder.appendLine("Role: ${role.orEmpty().ifBlank { "-" }}")
    builder.appendLine("Kelas: ${kodeKelas.orEmpty().ifBlank { "-" }}")
    builder.appendLine("Ruang: ${kodeRuang.orEmpty().ifBlank { "-" }}")
}
