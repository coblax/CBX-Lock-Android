package com.example.coblaxexamlock.ui.exam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.coblaxexamlock.ACTION_ACCESSIBILITY_GUARD_VIOLATION
import com.example.coblaxexamlock.ACCESSIBILITY_GUARD_REASON_NOTIFICATION_SHADE
import com.example.coblaxexamlock.ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED
import com.example.coblaxexamlock.ACCESSIBILITY_GUARD_REASON_SYSTEM_PANEL
import com.example.coblaxexamlock.AccessibilityExamGuardSnapshot
import com.example.coblaxexamlock.EXTRA_ACCESSIBILITY_GUARD_ALARM_SEVERITY
import com.example.coblaxexamlock.EXTRA_ACCESSIBILITY_GUARD_DETECTED_AT
import com.example.coblaxexamlock.EXTRA_ACCESSIBILITY_GUARD_EVENT_TYPE
import com.example.coblaxexamlock.EXTRA_ACCESSIBILITY_GUARD_FOREIGN_PACKAGE
import com.example.coblaxexamlock.EXTRA_ACCESSIBILITY_GUARD_REASON
import com.example.coblaxexamlock.EXTRA_ACCESSIBILITY_GUARD_VIOLATION_COUNT
import com.example.coblaxexamlock.ExamAlarmSeverity
import com.example.coblaxexamlock.LocalLowRamProfile
import com.example.coblaxexamlock.LowRamProfile
import com.example.coblaxexamlock.ActivityLockTaskBridge
import com.example.coblaxexamlock.AccessibilityExamGuardStore
import com.example.coblaxexamlock.alarmSeverityForAppSwitchViolationCount
import com.example.coblaxexamlock.buildAccessibilityExamReturnIntent
import com.example.coblaxexamlock.emitAccessibilityGuardViolationBroadcast
import com.example.coblaxexamlock.isExamGuardAccessibilityEnabled
import com.example.coblaxexamlock.launchPlatformIntentSafely
import com.example.coblaxexamlock.model.DiagnosticEventLevel
import com.example.coblaxexamlock.parseExamAlarmSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal data class AccessibilityGuardRuntimeViolation(
    val foreignPackage: String?,
    val eventType: String?,
    val detectedAt: String?,
    val violationCount: Int,
    val severity: ExamAlarmSeverity,
    val reason: String?,
    val source: String
)

@Composable
internal fun AccessibilityExamGuardViolationEffect(
    context: Context,
    examSessionStarted: Boolean,
    accessibilityGuardFallbackActive: Boolean,
    onViolation: (AccessibilityGuardRuntimeViolation) -> Unit
) {
    DisposableEffect(context, examSessionStarted, accessibilityGuardFallbackActive) {
        if (!examSessionStarted || !accessibilityGuardFallbackActive) {
            onDispose { }
        } else {
            val appContext = context.applicationContext
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_ACCESSIBILITY_GUARD_VIOLATION) {
                        onViolation(intent.toAccessibilityGuardViolation(source = "broadcast"))
                    }
                }
            }
            val filter = IntentFilter(ACTION_ACCESSIBILITY_GUARD_VIOLATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                ContextCompat.registerReceiver(
                    appContext,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            onDispose {
                runCatching { appContext.unregisterReceiver(receiver) }
            }
        }
    }
}

@Composable
internal fun AccessibilityExamGuardLivenessEffect(
    context: Context,
    examSessionStarted: Boolean,
    accessibilityGuardFallbackActive: Boolean,
    recordAction: (code: String, details: String, level: DiagnosticEventLevel) -> Unit
) {
    val lowRamProfile = LocalLowRamProfile.current
    val reportedDisabled = remember(examSessionStarted, accessibilityGuardFallbackActive) {
        AtomicBoolean(false)
    }

    fun reportDisabled(appContext: Context, source: String) {
        if (!examSessionStarted || !accessibilityGuardFallbackActive) {
            return
        }
        val enabledNow = isExamGuardAccessibilityEnabled(appContext)
        if (enabledNow) {
            reportedDisabled.set(false)
            return
        }
        if (!reportedDisabled.compareAndSet(false, true)) {
            return
        }
        val updated = AccessibilityExamGuardStore.recordViolation(
            context = appContext,
            reason = ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED,
            foreignPackage = "accessibility_service_disabled",
            eventType = source
        )
        launchPlatformIntentSafely(appContext, buildAccessibilityExamReturnIntent(appContext))
        emitAccessibilityGuardViolationBroadcast(appContext, updated)
    }

    DisposableEffect(context, examSessionStarted, accessibilityGuardFallbackActive) {
        if (!examSessionStarted || !accessibilityGuardFallbackActive) {
            onDispose { }
        } else {
            val appContext = context.applicationContext
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reportDisabled(appContext, "settings_content_observer")
                }
            }
            val resolver = appContext.contentResolver
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                observer
            )
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),
                false,
                observer
            )
            recordAction(
                "ACCESSIBILITY_GUARD_LIVENESS_MONITOR_STARTED",
                "fallback_active=true",
                DiagnosticEventLevel.INFO
            )
            onDispose {
                runCatching { resolver.unregisterContentObserver(observer) }
                recordAction(
                    "ACCESSIBILITY_GUARD_LIVENESS_MONITOR_STOPPED",
                    "fallback_active=false",
                    DiagnosticEventLevel.INFO
                )
            }
        }
    }

    LaunchedEffect(context, examSessionStarted, accessibilityGuardFallbackActive, lowRamProfile.accessibilityLivenessPollMillis) {
        if (!examSessionStarted || !accessibilityGuardFallbackActive) {
            return@LaunchedEffect
        }
        val appContext = context.applicationContext
        while (true) {
            delay(accessibilityGuardLivenessPollMillis(lowRamProfile))
            reportDisabled(appContext, "liveness_poll")
        }
    }
}

internal fun Intent.toAccessibilityGuardViolation(source: String): AccessibilityGuardRuntimeViolation {
    val violationCount = getIntExtra(EXTRA_ACCESSIBILITY_GUARD_VIOLATION_COUNT, 0)
    return AccessibilityGuardRuntimeViolation(
        foreignPackage = getStringExtra(EXTRA_ACCESSIBILITY_GUARD_FOREIGN_PACKAGE),
        eventType = getStringExtra(EXTRA_ACCESSIBILITY_GUARD_EVENT_TYPE),
        detectedAt = getStringExtra(EXTRA_ACCESSIBILITY_GUARD_DETECTED_AT),
        violationCount = violationCount,
        severity = parseExamAlarmSeverity(
            getStringExtra(EXTRA_ACCESSIBILITY_GUARD_ALARM_SEVERITY),
            alarmSeverityForAppSwitchViolationCount(violationCount)
        ),
        reason = getStringExtra(EXTRA_ACCESSIBILITY_GUARD_REASON),
        source = source
    )
}

internal fun AccessibilityExamGuardSnapshot.toRuntimeViolationIfNewer(
    currentViolationCount: Int,
    source: String
): AccessibilityGuardRuntimeViolation? {
    if (violationCount <= currentViolationCount || lastForeignPackage == null) {
        return null
    }
    return AccessibilityGuardRuntimeViolation(
        foreignPackage = lastForeignPackage,
        eventType = lastEventType,
        detectedAt = lastDetectedAt,
        violationCount = violationCount,
        severity = alarmSeverity,
        reason = lastReason,
        source = source
    )
}

internal fun buildAccessibilityGuardViolationDetails(
    baseDetails: String,
    violation: AccessibilityGuardRuntimeViolation
): String {
    return buildString {
        append(baseDetails)
        append(" | source=")
        append(violation.source)
        append(" | foreign_package=")
        append(violation.foreignPackage?.ifBlank { "-" } ?: "-")
        append(" | event_type=")
        append(violation.eventType?.ifBlank { "-" } ?: "-")
        append(" | reason=")
        append(violation.reason?.ifBlank { "-" } ?: "-")
        append(" | alarm_severity=")
        append(violation.severity.diagnosticLabel)
    }
}

internal fun accessibilityGuardEventCodeForReason(reason: String?): String {
    return when (reason) {
        ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED -> "ACCESSIBILITY_GUARD_SERVICE_DISABLED"
        ACCESSIBILITY_GUARD_REASON_NOTIFICATION_SHADE -> "ACCESSIBILITY_GUARD_NOTIFICATION_SHADE_DETECTED"
        ACCESSIBILITY_GUARD_REASON_SYSTEM_PANEL -> "ACCESSIBILITY_GUARD_SYSTEM_PANEL_DETECTED"
        else -> "ACCESSIBILITY_GUARD_APP_SWITCH_DETECTED"
    }
}

private const val AccessibilityGuardLivenessPollMillis = 1_000L

internal fun accessibilityGuardLivenessPollMillis(lowRamProfile: LowRamProfile): Long =
    lowRamProfile.accessibilityLivenessPollMillis

internal fun launchAccessibilityGuardFallbackExamStart(
    context: Context,
    lockTaskBridge: ActivityLockTaskBridge,
    coroutineScope: CoroutineScope,
    examGuardArmed: Boolean,
    updateFallbackUiState: (beforeState: String) -> Unit,
    recordAction: (code: String, details: String, level: DiagnosticEventLevel) -> Unit,
    clearAppSwitchSuppression: () -> Unit,
    resetPreparationSecurityEpisodes: () -> Unit,
    prepareCleanExamWebViewSessionForStart: suspend () -> Boolean,
    armExamRuntimeMonitoring: (String) -> Unit,
    finalizeExamSessionStart: (Boolean) -> Unit,
    onCleanSessionFailed: () -> Unit
) {
    val beforeState = lockTaskBridge.stateLabel()
    AccessibilityExamGuardStore.resetViolations(context)
    AccessibilityExamGuardStore.arm(context, fallbackActive = true)
    updateFallbackUiState(beforeState)
    clearAppSwitchSuppression()
    recordAction(
        "ACCESSIBILITY_GUARD_ARMED",
        "fallback_active=true | screen_pinning_available=false",
        DiagnosticEventLevel.INFO
    )
    resetPreparationSecurityEpisodes()
    coroutineScope.launch {
        if (!prepareCleanExamWebViewSessionForStart()) {
            AccessibilityExamGuardStore.disarm(context)
            onCleanSessionFailed()
            return@launch
        }
        if (!examGuardArmed) {
            armExamRuntimeMonitoring("start_exam_accessibility_guard_fallback")
        }
        finalizeExamSessionStart(false)
    }
}
