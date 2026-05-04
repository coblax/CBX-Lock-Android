package com.example.coblaxexamlock.runtime

import android.content.Context
import android.os.Build
import com.example.coblaxexamlock.AdbBypassState
import com.example.coblaxexamlock.AdbInspection
import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.AppSwitchStatus
import com.example.coblaxexamlock.BuildConfig
import com.example.coblaxexamlock.ClipboardRuntimeStatus
import com.example.coblaxexamlock.ExamParticipantContext
import com.example.coblaxexamlock.FakeLocationRuntimeStatus
import com.example.coblaxexamlock.GeofenceRuntimeStatus
import com.example.coblaxexamlock.IntegrityCheckResult
import com.example.coblaxexamlock.IntegrityGuard
import com.example.coblaxexamlock.OverlayRiskResult
import com.example.coblaxexamlock.ReverseEngineeringGuard
import com.example.coblaxexamlock.ReverseEngineeringResult
import com.example.coblaxexamlock.RootBypassState
import com.example.coblaxexamlock.RootSecurityStatus
import com.example.coblaxexamlock.SecureStrings
import com.example.coblaxexamlock.SignatureIntegrity
import com.example.coblaxexamlock.diagnosticLabel
import com.example.coblaxexamlock.formatCoordinates
import com.example.coblaxexamlock.config.TelegramMessageChunkLimit
import com.example.coblaxexamlock.format.diagnosticSectionEventCodes
import com.example.coblaxexamlock.format.diagnosticTimestamp
import com.example.coblaxexamlock.format.formatElapsedDuration
import com.example.coblaxexamlock.format.formatGeofenceDistance
import com.example.coblaxexamlock.format.formatLocationFixAge
import com.example.coblaxexamlock.i18n.diagnosticSectionLabel
import com.example.coblaxexamlock.inspectAccessibility
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticSection
import com.example.coblaxexamlock.model.ExamNetworkStatus
import com.example.coblaxexamlock.model.ExamOfflineRuntimeStatus
import com.example.coblaxexamlock.model.UiLanguage
import com.example.coblaxexamlock.resolveExpectedSigningFingerprints
import com.example.coblaxexamlock.ui.geofence.effectiveCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizeCircleCenters
import com.example.coblaxexamlock.ui.geofence.summarizePolygonVertices
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
