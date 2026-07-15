package com.coblax.examlock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.edit

internal const val ACTION_ACCESSIBILITY_GUARD_VIOLATION =
    "com.coblax.examlock.action.ACCESSIBILITY_GUARD_VIOLATION"
internal const val EXTRA_ACCESSIBILITY_GUARD_FOREIGN_PACKAGE = "foreign_package"
internal const val EXTRA_ACCESSIBILITY_GUARD_EVENT_TYPE = "event_type"
internal const val EXTRA_ACCESSIBILITY_GUARD_DETECTED_AT = "detected_at"
internal const val EXTRA_ACCESSIBILITY_GUARD_VIOLATION_COUNT = "violation_count"
internal const val EXTRA_ACCESSIBILITY_GUARD_ALARM_SEVERITY = "alarm_severity"
internal const val EXTRA_ACCESSIBILITY_GUARD_REASON = "reason"

internal const val ACCESSIBILITY_GUARD_REASON_APP_SWITCH = "app_switch"
internal const val ACCESSIBILITY_GUARD_REASON_NOTIFICATION_SHADE = "notification_shade"
internal const val ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED = "service_disabled"
internal const val ACCESSIBILITY_GUARD_REASON_SYSTEM_PANEL = "system_panel"

internal enum class ExamAlarmSeverity(
    val diagnosticLabel: String,
    val targetVolumeFraction: Float
) {
    Warning("warning", 0.60f),
    Escalated("escalated", 0.80f),
    Severe("severe", 1.00f)
}

internal fun alarmSeverityForAppSwitchViolationCount(count: Int): ExamAlarmSeverity {
    return when {
        count >= 3 -> ExamAlarmSeverity.Severe
        count == 2 -> ExamAlarmSeverity.Escalated
        else -> ExamAlarmSeverity.Warning
    }
}

internal data class AccessibilityExamGuardSnapshot(
    val enabled: Boolean,
    val armed: Boolean,
    val fallbackActive: Boolean,
    val violationCount: Int,
    val lastReason: String?,
    val lastForeignPackage: String?,
    val lastEventType: String?,
    val lastDetectedAt: String?,
    val alarmSeverity: ExamAlarmSeverity
)

internal object AccessibilityExamGuardStore {
    private const val PreferencesName = "accessibility_exam_guard"
    private const val KeyArmed = "armed"
    private const val KeyFallbackActive = "fallback_active"
    private const val KeyExamPackage = "exam_package"
    private const val KeyViolationCount = "violation_count"
    private const val KeyLastReason = "last_reason"
    private const val KeyLastForeignPackage = "last_foreign_package"
    private const val KeyLastEventType = "last_event_type"
    private const val KeyLastDetectedAt = "last_detected_at"
    private const val KeyAlarmSeverity = "alarm_severity"

    fun arm(context: Context, fallbackActive: Boolean) {
        prefs(context).edit {
            putBoolean(KeyArmed, true)
            putBoolean(KeyFallbackActive, fallbackActive)
            putString(KeyExamPackage, context.packageName)
        }
    }

    fun disarm(context: Context) {
        prefs(context).edit {
            putBoolean(KeyArmed, false)
            putBoolean(KeyFallbackActive, false)
        }
    }

    fun snapshot(context: Context): AccessibilityExamGuardSnapshot {
        val preferences = prefs(context)
        val count = preferences.getInt(KeyViolationCount, 0).coerceAtLeast(0)
        return AccessibilityExamGuardSnapshot(
            enabled = isExamGuardAccessibilityEnabled(context),
            armed = preferences.getBoolean(KeyArmed, false),
            fallbackActive = preferences.getBoolean(KeyFallbackActive, false),
            violationCount = count,
            lastReason = preferences.getString(KeyLastReason, null),
            lastForeignPackage = preferences.getString(KeyLastForeignPackage, null),
            lastEventType = preferences.getString(KeyLastEventType, null),
            lastDetectedAt = preferences.getString(KeyLastDetectedAt, null),
            alarmSeverity = parseExamAlarmSeverity(
                preferences.getString(KeyAlarmSeverity, null),
                alarmSeverityForAppSwitchViolationCount(count)
            )
        )
    }

    fun recordViolation(
        context: Context,
        reason: String = ACCESSIBILITY_GUARD_REASON_APP_SWITCH,
        foreignPackage: String?,
        eventType: String,
        detectedAt: String = diagnosticNow()
    ): AccessibilityExamGuardSnapshot {
        val preferences = prefs(context)
        val nextCount = (preferences.getInt(KeyViolationCount, 0) + 1).coerceAtLeast(1)
        val severity = alarmSeverityForAppSwitchViolationCount(nextCount)
        preferences.edit {
            putInt(KeyViolationCount, nextCount)
            putString(KeyLastReason, reason.ifBlank { ACCESSIBILITY_GUARD_REASON_APP_SWITCH })
            putString(KeyLastForeignPackage, foreignPackage?.ifBlank { "-" } ?: "-")
            putString(KeyLastEventType, eventType.ifBlank { "-" })
            putString(KeyLastDetectedAt, detectedAt)
            putString(KeyAlarmSeverity, severity.name)
        }
        return snapshot(context)
    }

    fun resetViolations(context: Context) {
        prefs(context).edit {
            putInt(KeyViolationCount, 0)
            remove(KeyLastReason)
            remove(KeyLastForeignPackage)
            remove(KeyLastEventType)
            remove(KeyLastDetectedAt)
            putString(KeyAlarmSeverity, ExamAlarmSeverity.Warning.name)
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
}

internal fun examGuardAccessibilityComponentName(context: Context): ComponentName {
    return ComponentName(context, ExamGuardAccessibilityService::class.java)
}

internal fun examGuardAccessibilityComponentFlattened(context: Context): String {
    return examGuardAccessibilityComponentName(context).flattenToString()
}

internal fun isExamGuardAccessibilityEnabled(context: Context): Boolean {
    val expected = examGuardAccessibilityComponentName(context)
    val enabledServices = runCatching {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
    }.getOrNull().orEmpty()

    if (enabledServices.isBlank()) {
        return false
    }

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        val flattened = splitter.next()
        val component = ComponentName.unflattenFromString(flattened)
        if (component == expected) {
            return true
        }
    }
    return false
}

internal fun isExamGuardAccessibilityAvailable(context: Context): Boolean {
    return runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getServiceInfo(
            examGuardAccessibilityComponentName(context),
            PackageManager.GET_META_DATA
        )
        true
    }.getOrDefault(false)
}

internal fun isExamGuardAccessibilityComponent(serviceComponent: String, context: Context? = null): Boolean {
    val normalized = serviceComponent.trim()
    if (normalized.isBlank()) {
        return false
    }
    val expected = context?.let(::examGuardAccessibilityComponentFlattened)
        ?: "com.coblax.examlock/.ExamGuardAccessibilityService"
    return normalized.equals(expected, ignoreCase = true) ||
        normalized.equals(
            "com.coblax.examlock/com.coblax.examlock.ExamGuardAccessibilityService",
            ignoreCase = true
        ) ||
        normalized.endsWith("/.ExamGuardAccessibilityService", ignoreCase = true) ||
        normalized.endsWith(
            "/com.coblax.examlock.ExamGuardAccessibilityService",
            ignoreCase = true
        )
}

internal fun isAccessibilityExamGuardAllowedPackage(context: Context, packageName: String?): Boolean {
    val normalized = packageName?.trim().orEmpty()
    if (normalized.isBlank()) {
        return true
    }
    if (normalized == context.packageName) {
        return true
    }
    return normalized in setOf(
        "android",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.gms",
        "com.google.android.googlequicksearchbox"
    )
}

internal fun accessibilityEventTypeLabel(eventType: Int): String {
    return when (eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "type_window_state_changed"
        AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "type_windows_changed"
        AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "type_notification_state_changed"
        else -> "type_$eventType"
    }
}

internal fun emitAccessibilityGuardViolationBroadcast(
    context: Context,
    snapshot: AccessibilityExamGuardSnapshot
) {
    context.sendBroadcast(
        Intent(ACTION_ACCESSIBILITY_GUARD_VIOLATION).apply {
            setPackage(context.packageName)
            putExtra(
                EXTRA_ACCESSIBILITY_GUARD_FOREIGN_PACKAGE,
                snapshot.lastForeignPackage?.ifBlank { "-" } ?: "-"
            )
            putExtra(
                EXTRA_ACCESSIBILITY_GUARD_EVENT_TYPE,
                snapshot.lastEventType?.ifBlank { "-" } ?: "-"
            )
            putExtra(
                EXTRA_ACCESSIBILITY_GUARD_DETECTED_AT,
                snapshot.lastDetectedAt?.ifBlank { "-" } ?: "-"
            )
            putExtra(EXTRA_ACCESSIBILITY_GUARD_VIOLATION_COUNT, snapshot.violationCount)
            putExtra(EXTRA_ACCESSIBILITY_GUARD_ALARM_SEVERITY, snapshot.alarmSeverity.name)
            putExtra(
                EXTRA_ACCESSIBILITY_GUARD_REASON,
                snapshot.lastReason?.ifBlank { ACCESSIBILITY_GUARD_REASON_APP_SWITCH }
                    ?: ACCESSIBILITY_GUARD_REASON_APP_SWITCH
            )
        }
    )
}

internal fun buildAccessibilityExamReturnIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        putExtra("accessibility_guard_return", true)
        putExtra("accessibility_guard_return_elapsed_ms", SystemClock.elapsedRealtime())
    }
}

internal fun parseExamAlarmSeverity(
    value: String?,
    default: ExamAlarmSeverity = ExamAlarmSeverity.Escalated
): ExamAlarmSeverity {
    return ExamAlarmSeverity.values().firstOrNull {
        it.name.equals(value, ignoreCase = true) || it.diagnosticLabel.equals(value, ignoreCase = true)
    } ?: default
}

private fun diagnosticNow(): String {
    return java.text.SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        java.util.Locale.US
    ).format(java.util.Date())
}
