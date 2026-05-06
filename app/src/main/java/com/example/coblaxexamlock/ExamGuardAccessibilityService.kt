package com.example.coblaxexamlock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import java.util.Locale

class ExamGuardAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSystemUiCandidateAtElapsedMs: Long = 0L
    private var lastSystemUiViolationAtElapsedMs: Long = 0L
    private var lastServiceDisabledSignalAtElapsedMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !MonitoredEventTypes.contains(event.eventType)) {
            return
        }

        val snapshot = AccessibilityExamGuardStore.snapshot(this)
        if (!snapshot.armed) {
            return
        }

        val foregroundPackage = event.packageName?.toString().orEmpty()
        if (foregroundPackage == SystemUiPackage) {
            handleSystemUiEvent(event)
            return
        }

        if (isAccessibilityExamGuardAllowedPackage(this, foregroundPackage)) {
            return
        }

        recordAndReturnToExam(
            reason = ACCESSIBILITY_GUARD_REASON_APP_SWITCH,
            foreignPackage = foregroundPackage,
            eventType = accessibilityEventTypeLabel(event.eventType)
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        recordServiceDisabledSignal("service_unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        recordServiceDisabledSignal("service_destroyed")
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    private fun handleSystemUiEvent(event: AccessibilityEvent) {
        val explicitReason = systemUiPanelReason(event)
        if (explicitReason != null) {
            recordSystemUiViolationIfAllowed(
                reason = explicitReason,
                eventType = accessibilityEventTypeLabel(event.eventType)
            )
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return
        }

        pendingSystemUiCandidateAtElapsedMs = SystemClock.elapsedRealtime()
        mainHandler.postDelayed(
            {
                val now = SystemClock.elapsedRealtime()
                val candidateAge = now - pendingSystemUiCandidateAtElapsedMs
                if (candidateAge >= SystemUiDwellThresholdMs) {
                    recordSystemUiViolationIfAllowed(
                        reason = ACCESSIBILITY_GUARD_REASON_SYSTEM_PANEL,
                        eventType = "system_ui_dwell"
                    )
                }
            },
            SystemUiDwellThresholdMs
        )
    }

    private fun recordSystemUiViolationIfAllowed(reason: String, eventType: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSystemUiViolationAtElapsedMs < SystemUiViolationCooldownMs) {
            return
        }
        lastSystemUiViolationAtElapsedMs = now
        recordAndReturnToExam(
            reason = reason,
            foreignPackage = SystemUiPackage,
            eventType = eventType
        )
    }

    private fun recordServiceDisabledSignal(eventType: String) {
        val snapshot = AccessibilityExamGuardStore.snapshot(this)
        if (!snapshot.armed || !snapshot.fallbackActive) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastServiceDisabledSignalAtElapsedMs < ServiceDisabledCooldownMs) {
            return
        }
        lastServiceDisabledSignalAtElapsedMs = now
        recordAndReturnToExam(
            reason = ACCESSIBILITY_GUARD_REASON_SERVICE_DISABLED,
            foreignPackage = "accessibility_service_disabled",
            eventType = eventType
        )
    }

    private fun recordAndReturnToExam(
        reason: String,
        foreignPackage: String,
        eventType: String
    ) {
        val updated = AccessibilityExamGuardStore.recordViolation(
            context = this,
            reason = reason,
            foreignPackage = foreignPackage,
            eventType = eventType
        )

        launchPlatformIntentSafely(this, buildAccessibilityExamReturnIntent(this))

        emitAccessibilityGuardViolationBroadcast(this, updated)
    }

    private fun systemUiPanelReason(event: AccessibilityEvent): String? {
        val className = event.className?.toString()
            ?.lowercase(Locale.US)
            .orEmpty()

        return when {
            className.contains("notificationshade") ||
                className.contains("notificationpanel") ||
                className.contains("notificationstack") ||
                className.contains("shade") ->
                ACCESSIBILITY_GUARD_REASON_NOTIFICATION_SHADE

            className.contains("quicksettings") ||
                className.contains("qspanel") ||
                className.contains("qscontainer") ||
                className.contains("statusbarwindow") ->
                ACCESSIBILITY_GUARD_REASON_SYSTEM_PANEL

            else -> null
        }
    }

    companion object {
        private const val SystemUiPackage = "com.android.systemui"
        private const val SystemUiDwellThresholdMs = 650L
        private const val SystemUiViolationCooldownMs = 2_500L
        private const val ServiceDisabledCooldownMs = 2_000L

        private val MonitoredEventTypes = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        )
    }
}
