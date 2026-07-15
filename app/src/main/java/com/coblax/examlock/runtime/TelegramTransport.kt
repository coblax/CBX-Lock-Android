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

internal class TelegramHttpException(
    val statusCode: Int,
    message: String
) : java.io.IOException(message)

internal fun sendTelegramTextMessage(
    token: String,
    chatId: String,
    message: String
) {
    val requestBody = buildString {
        append("chat_id=")
        append(URLEncoder.encode(chatId, StandardCharsets.UTF_8.name()))
        append("&text=")
        append(URLEncoder.encode(message, StandardCharsets.UTF_8.name()))
        append("&disable_web_page_preview=true")
    }

    val connection =
        (URL("https://api.telegram.org/bot$token/sendMessage").openConnection()
            as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
                )
            }

    try {
        connection.outputStream.use { output ->
            output.write(requestBody.toByteArray(StandardCharsets.UTF_8))
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorMessage =
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "HTTP $responseCode"
            throw TelegramHttpException(responseCode, errorMessage)
        }

        // Validate Telegram API response body — HTTP 200 doesn't guarantee delivery
        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
        if (!responseBody.contains("\"ok\":true") && !responseBody.contains("\"ok\": true")) {
            throw TelegramHttpException(
                responseCode,
                "Telegram API returned ok=false: $responseBody"
            )
        }
    } finally {
        connection.disconnect()
    }
}
