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

internal fun buildTelegramMessageChunks(
    message: String,
    maxChunkLength: Int = TelegramMessageChunkLimit
): List<String> {
    if (message.length <= maxChunkLength) {
        return listOf(message)
    }

    val chunks = mutableListOf<String>()
    var currentChunk = StringBuilder()

    fun flushChunk() {
        if (currentChunk.isNotEmpty()) {
            chunks += currentChunk.toString().trimEnd()
            currentChunk = StringBuilder()
        }
    }

    fun appendLineToChunk(line: String) {
        val prefix = if (currentChunk.isEmpty()) "" else "\n"
        if (currentChunk.length + prefix.length + line.length <= maxChunkLength) {
            currentChunk.append(prefix).append(line)
        } else {
            flushChunk()
            if (line.length <= maxChunkLength) {
                currentChunk.append(line)
            } else {
                line.chunked(maxChunkLength).forEach { part ->
                    chunks += part
                }
            }
        }
    }

    message.lineSequence().forEach(::appendLineToChunk)
    flushChunk()

    if (chunks.size <= 1) {
        return chunks
    }

    return chunks.mapIndexed { index, chunk ->
        val partHeader = "DIAGNOSTIK CBX LOCK (${index + 1}/${chunks.size})\n"
        partHeader + chunk
    }
}
