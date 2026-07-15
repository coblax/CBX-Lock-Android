package com.coblax.examlock.save

import androidx.compose.runtime.saveable.Saver
import com.coblax.examlock.ExamQrPayload
import com.coblax.examlock.LocationPolicySource
import com.coblax.examlock.model.DiagnosticEvent
import com.coblax.examlock.persistence.deserializeExamLocationPolicy
import com.coblax.examlock.persistence.serializeExamLocationPolicy

internal val DiagnosticEventLogSaver = Saver<List<DiagnosticEvent>, Any>(
    save = { events ->
        events.map { event ->
            listOf(
                event.timestamp,
                event.level,
                event.code,
                event.screen,
                event.appElapsedMs,
                event.sessionElapsedMs,
                event.details
            )
        }
    },
    restore = { restored ->
        (restored as? List<*>)?.mapNotNull { entry ->
            val values = entry as? List<*> ?: return@mapNotNull null
            DiagnosticEvent(
                timestamp = values.getOrNull(0) as? String ?: return@mapNotNull null,
                level = values.getOrNull(1) as? String ?: return@mapNotNull null,
                code = values.getOrNull(2) as? String ?: return@mapNotNull null,
                screen = values.getOrNull(3) as? String ?: return@mapNotNull null,
                appElapsedMs = values.getOrNull(4) as? Long ?: 0L,
                sessionElapsedMs = values.getOrNull(5) as? Long,
                details = values.getOrNull(6) as? String ?: "-"
            )
        } ?: emptyList()
    }
)
internal val ExamQrPayloadSaver = Saver<ExamQrPayload?, Any>(
    save = { payload ->
        payload?.let {
            listOf(
                it.examUrl,
                it.examName,
                it.startDateTime,
                it.endDateTime,
                it.issuedAt,
                it.saveToDirectLink,
                it.locationPolicy?.let(::serializeExamLocationPolicy),
                it.locationPolicySource.name
            )
        }
    },
    restore = { restored ->
        val values = restored as? List<*> ?: return@Saver null
        val savedLocationPolicy = (values.getOrNull(6) as? String)
            ?.let(::deserializeExamLocationPolicy)
        ExamQrPayload(
            examUrl = values.getOrNull(0) as? String ?: return@Saver null,
            examName = values.getOrNull(1) as? String ?: "",
            startDateTime = values.getOrNull(2) as? String ?: "",
            endDateTime = values.getOrNull(3) as? String ?: "",
            issuedAt = values.getOrNull(4) as? Long ?: 0L,
            saveToDirectLink = values.getOrNull(5) as? Boolean ?: false,
            locationPolicy = savedLocationPolicy,
            locationPolicySource = (values.getOrNull(7) as? String)
                ?.let { rawSource ->
                    runCatching { LocationPolicySource.valueOf(rawSource) }.getOrNull()
                }
                ?: if (savedLocationPolicy != null) {
                    LocationPolicySource.CustomQr
                } else {
                    LocationPolicySource.DisabledNoPolicy
                }
        )
    }
)
