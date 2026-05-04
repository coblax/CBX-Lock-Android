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

    connection.outputStream.use { output ->
        output.write(requestBody.toByteArray(StandardCharsets.UTF_8))
    }

    val responseCode = connection.responseCode
    if (responseCode !in 200..299) {
        val errorMessage =
            connection.errorStream?.bufferedReader()?.use { it.readText() }
                ?: "HTTP $responseCode"
        error(errorMessage)
    }
}
