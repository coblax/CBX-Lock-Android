package com.coblax.examlock.ui.admin

import com.coblax.examlock.ExamQrLocationPolicy
import com.coblax.examlock.GeofenceShapeType
import com.coblax.examlock.model.CustomQrAdminTab
import com.coblax.examlock.validateExamUrl
import com.coblax.examlock.viewmodel.CustomQrDraftState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal enum class CustomQrScheduleProblem {
    Missing,
    EndNotAfterStart,
    AlreadyEnded
}

/**
 * Admins type "cbt.sekolah.sch.id" far more often than the full address; without a
 * scheme it is read as https. An explicit http:// is kept: such an exam opens with a
 * warning to the student.
 */
internal fun normalizeAdminUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || "://" in trimmed) {
        return trimmed
    }
    return "https://$trimmed"
}

internal fun normalizedCustomQrExamUrl(draft: CustomQrDraftState): String? =
    validateExamUrl(normalizeAdminUrl(draft.examUrl)).normalizedUrl

internal fun parseCustomQrDateTimeMillis(value: String, timeZone: TimeZone = TimeZone.getDefault()): Long? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return null
    }
    return CustomQrDateTimePatterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                this.timeZone = timeZone
            }.parse(trimmed)?.time
        }.getOrNull()
    }
}

internal fun customQrScheduleProblem(
    startTime: String,
    endTime: String,
    nowMillis: Long? = null
): CustomQrScheduleProblem? {
    val start = parseCustomQrDateTimeMillis(startTime) ?: return CustomQrScheduleProblem.Missing
    val end = parseCustomQrDateTimeMillis(endTime) ?: return CustomQrScheduleProblem.Missing
    return when {
        end <= start -> CustomQrScheduleProblem.EndNotAfterStart
        nowMillis != null && end <= nowMillis -> CustomQrScheduleProblem.AlreadyEnded
        else -> null
    }
}

internal fun isCustomQrExamStepComplete(draft: CustomQrDraftState): Boolean {
    return draft.examName.isNotBlank() &&
        normalizedCustomQrExamUrl(draft) != null &&
        customQrScheduleProblem(draft.startTime, draft.endTime) == null
}

internal fun canOpenCustomQrStep(
    target: CustomQrAdminTab,
    draft: CustomQrDraftState,
    locationConfigurationValid: Boolean
): Boolean {
    return when (target) {
        CustomQrAdminTab.Exam -> true
        CustomQrAdminTab.Location -> isCustomQrExamStepComplete(draft)
        CustomQrAdminTab.Generate ->
            isCustomQrExamStepComplete(draft) &&
                (!draft.geofenceEnabled || locationConfigurationValid)
    }
}

/**
 * The policy the QR carries. Only the chosen shape's fields go in: a polygon used to
 * carry the last circle's center and radius along, which wasted QR space for nothing.
 */
internal fun buildCustomQrLocationPolicy(draft: CustomQrDraftState): ExamQrLocationPolicy {
    if (!draft.geofenceEnabled) {
        return ExamQrLocationPolicy(shapeType = GeofenceShapeType.Disabled)
    }
    return when (draft.locationShape) {
        GeofenceShapeType.Polygon -> ExamQrLocationPolicy(
            shapeType = GeofenceShapeType.Polygon,
            vertices = draft.polygonVertices.map { it.copy(latitude = it.latitude.trim(), longitude = it.longitude.trim()) }
        )
        else -> {
            val centers = draft.circlePoints.map { it.copy(latitude = it.latitude.trim(), longitude = it.longitude.trim()) }
            ExamQrLocationPolicy(
                shapeType = GeofenceShapeType.Circle,
                centerLat = centers.firstOrNull()?.latitude.orEmpty(),
                centerLng = centers.firstOrNull()?.longitude.orEmpty(),
                radiusMeters = draft.geofenceRadiusMeters.trim(),
                circleCenters = centers
            )
        }
    }
}

private val CustomQrDateTimePatterns = listOf(
    "dd/MM/yyyy HH:mm",
    "yyyy-MM-dd HH:mm",
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm",
    "yyyy-MM-dd'T'HH:mm:ss"
)
