package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.AlarmAcknowledgePayload
import com.example.coblaxexamlock.AlarmAcknowledgeType
import com.example.coblaxexamlock.format.diagnosticSectionEventCodes
import com.example.coblaxexamlock.model.DiagnosticEvent
import com.example.coblaxexamlock.model.DiagnosticSection

internal fun alarmDiagnosticSection(type: AlarmAcknowledgeType): DiagnosticSection {
    return when (type) {
        AlarmAcknowledgeType.AppSwitch -> DiagnosticSection.AppSwitch
        AlarmAcknowledgeType.Keyboard -> DiagnosticSection.Keyboard
        AlarmAcknowledgeType.Overlay -> DiagnosticSection.Overlay
        AlarmAcknowledgeType.Bluetooth -> DiagnosticSection.Bluetooth
        AlarmAcknowledgeType.Clipboard -> DiagnosticSection.Clipboard
        AlarmAcknowledgeType.Geofence -> DiagnosticSection.Geofence
        AlarmAcknowledgeType.FakeLocation -> DiagnosticSection.FakeLocation
    }
}

internal fun latestAlarmDetailRef(
    diagnosticEvents: List<DiagnosticEvent>,
    type: AlarmAcknowledgeType
): String {
    val latestEventCode = diagnosticEvents
        .firstOrNull { it.code in diagnosticSectionEventCodes(alarmDiagnosticSection(type)) }
        ?.code
        .orEmpty()
        .ifBlank { "-" }
    return "${type.wireName}:$latestEventCode"
}

internal fun buildAlarmAckEventDetails(
    payload: AlarmAcknowledgePayload,
    result: String,
    extra: String? = null
): String {
    return buildString {
        append("alarm_type=").append(payload.alarmType.wireName)
        append(" | exam=").append(payload.examName)
        append(" | url_host=").append(payload.examUrlHost)
        append(" | url_hash=").append(payload.examUrlHashShort)
        append(" | detail_ref=").append(payload.detailRef)
        append(" | result=").append(result)
        if (!extra.isNullOrBlank()) {
            append(" | ").append(extra)
        }
    }
}
