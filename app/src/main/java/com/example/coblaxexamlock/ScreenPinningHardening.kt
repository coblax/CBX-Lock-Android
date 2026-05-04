package com.example.coblaxexamlock

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import kotlinx.coroutines.delay


internal enum class ScreenPinningMode {
    Enforced,
    Bypassed;

    fun allowsLockTask(): Boolean = this == Enforced
}

internal sealed interface ScreenPinningBypassState {
    data object Active : ScreenPinningBypassState

    data object Inactive : ScreenPinningBypassState

    data object Tampered : ScreenPinningBypassState
}

internal data class ScreenPinningLaunchState(
    val beforeState: String,
    val afterState: String,
    val outcome: String,
    val dialogLikelyShown: Boolean,
    val userActionInference: String,
    val activationDurationMs: Long?,
    val eventCode: String,
    val eventDetails: String = "-"
)

internal data class ScreenPinningActivationReport(
    val active: Boolean,
    val afterState: String,
    val dialogLikelyShown: Boolean,
    val outcome: String,
    val userActionInference: String,
    val activationDurationMs: Long,
    val guidanceMessage: String?
)

internal data class FatalSecuritySignal(
    val eventCode: String,
    val details: String,
    val title: String,
    val message: String
)

internal interface LockTaskBridge {
    fun engage(allowLockTask: Boolean = true)

    fun disengage()

    fun active(): Boolean

    fun stateLabel(): String
}

internal class ActivityLockTaskBridge(private val activityProvider: () -> MainActivity?) : LockTaskBridge {
    override fun engage(allowLockTask: Boolean) {
        activityProvider()?.setExamLockMode(enabled = true, allowLockTask = allowLockTask)
    }

    override fun disengage() {
        activityProvider()?.setExamLockMode(enabled = false)
    }

    override fun active(): Boolean {
        return activityProvider()?.isExamLockModeActive() == true
    }

    override fun stateLabel(): String {
        return activityProvider()?.getExamLockTaskStateLabel() ?: "Unknown"
    }
}

internal object ScreenPinningBypassResolver {
    fun resolve(context: Context): ScreenPinningBypassState {
        val resolution = SwitchMatrix.resolveCritical(context, GateKeys.ScreenPinning)
        return when {
            resolution.tampered -> ScreenPinningBypassState.Tampered
            resolution.enabled -> ScreenPinningBypassState.Active
            else -> ScreenPinningBypassState.Inactive
        }
    }

    fun stateOf(enabled: Boolean, tampered: Boolean): ScreenPinningBypassState {
        return when {
            tampered -> ScreenPinningBypassState.Tampered
            enabled -> ScreenPinningBypassState.Active
            else -> ScreenPinningBypassState.Inactive
        }
    }

    fun modeOf(state: ScreenPinningBypassState): ScreenPinningMode {
        return if (state == ScreenPinningBypassState.Active) {
            ScreenPinningMode.Bypassed
        } else {
            ScreenPinningMode.Enforced
        }
    }
}

internal object ScreenPinningPlatformBridge {
    fun isAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    fun readSystemSetting(context: Context): String {
        if (!isAvailable()) {
            return "Tidak didukung"
        }

        val key = ScreenPinningSignals.settingKey()
        val enabledFromSystem = runCatching {
            Settings.System.getInt(context.contentResolver, key, -1)
        }.getOrDefault(-1)

        if (enabledFromSystem == 1) {
            return "Aktif"
        }
        if (enabledFromSystem == 0) {
            return "Nonaktif"
        }

        val enabledFromSecure = runCatching {
            Settings.Secure.getInt(context.contentResolver, key, -1)
        }.getOrDefault(-1)

        return when (enabledFromSecure) {
            1 -> "Aktif"
            0 -> "Nonaktif"
            else -> "Tidak diketahui"
        }
    }
}

internal object ScreenPinningEnforcer {
    private const val InitialEngageDelayMillis = 250L
    private const val FeedbackDelayMillis = 1000L
    private const val ActivationTimeoutMillis = 45000L
    private const val PollIntervalMillis = 150L
    private const val RetryEngageIntervalMillis = 2500L
    private const val MaxActivationRetries = 12

    fun launchState(mode: ScreenPinningMode, bridge: LockTaskBridge?): ScreenPinningLaunchState {
        return when (mode) {
            ScreenPinningMode.Bypassed -> {
                val state = bridge?.stateLabel() ?: "Unknown"
                ScreenPinningLaunchState(
                    beforeState = state,
                    afterState = state,
                    outcome = ScreenPinningSignals.bypassOutcome(),
                    dialogLikelyShown = false,
                    userActionInference = ScreenPinningSignals.bypassUserAction(),
                    activationDurationMs = 0L,
                    eventCode = ScreenPinningSignals.eventBypassUsed()
                )
            }

            ScreenPinningMode.Enforced -> {
                val state = bridge?.stateLabel() ?: "Unknown"
                ScreenPinningLaunchState(
                    beforeState = state,
                    afterState = state,
                    outcome = ScreenPinningSignals.requestOutcome(),
                    dialogLikelyShown = false,
                    userActionInference = ScreenPinningSignals.requestUserAction(),
                    activationDurationMs = null,
                    eventCode = ScreenPinningSignals.eventRequested()
                )
            }
        }
    }

    suspend fun requestAndAwaitActivation(
        bridge: LockTaskBridge,
        isIndonesian: Boolean
    ): ScreenPinningActivationReport {
        val startedAt = SystemClock.elapsedRealtime()
        delay(InitialEngageDelayMillis)
        bridge.engage(allowLockTask = true)
        var lastEngageAt = SystemClock.elapsedRealtime()
        var retryCount = 0
        delay(FeedbackDelayMillis)

        var dialogLikelyShown = false
        if (!bridge.active()) {
            dialogLikelyShown = true
        }

        var remainingMillis = ActivationTimeoutMillis - FeedbackDelayMillis
        while (remainingMillis >= 0L) {
            if (bridge.active()) {
                return ScreenPinningActivationReport(
                    active = true,
                    afterState = bridge.stateLabel(),
                    dialogLikelyShown = dialogLikelyShown,
                    outcome = ScreenPinningSignals.successOutcome(),
                    userActionInference = ScreenPinningSignals.successUserAction(),
                    activationDurationMs = SystemClock.elapsedRealtime() - startedAt,
                    guidanceMessage = null
                )
            }

            val now = SystemClock.elapsedRealtime()
            if (
                retryCount < MaxActivationRetries &&
                now - lastEngageAt >= RetryEngageIntervalMillis
            ) {
                bridge.engage(allowLockTask = true)
                lastEngageAt = now
                retryCount += 1
            }
            delay(PollIntervalMillis)
            remainingMillis -= PollIntervalMillis
        }

        return ScreenPinningActivationReport(
            active = false,
            afterState = bridge.stateLabel(),
            dialogLikelyShown = dialogLikelyShown,
            outcome = ScreenPinningSignals.failureOutcome(),
            userActionInference = if (dialogLikelyShown) {
                ScreenPinningSignals.rejectedUserAction()
            } else {
                ScreenPinningSignals.systemRejectedUserAction()
            },
            activationDurationMs = SystemClock.elapsedRealtime() - startedAt,
            guidanceMessage = localizedScreenPinningGuidance(isIndonesian)
        )
    }

    fun unavailableActivityMessage(isIndonesian: Boolean): String {
        return if (isIndonesian) {
            "Mode ujian tidak bisa dimulai karena activity utama tidak tersedia."
        } else {
            "Exam mode could not start because the main activity is unavailable."
        }
    }

    fun pendingMessage(isIndonesian: Boolean): String = localizedScreenPinningGuidance(isIndonesian)

    private fun localizedScreenPinningGuidance(isIndonesian: Boolean): String {
        return if (isIndonesian) {
            "Screen pinning belum aktif. Saat Android menampilkan dialog pin aplikasi, pilih \"Got it\" atau \"Pin\", lalu tetap di aplikasi sampai mode ujian terbuka. Pada beberapa perangkat, aplikasi akan meminta pinning lagi otomatis setelah prompt pertama ditutup."
        } else {
            "Screen pinning is not active yet. When Android shows the app pinning dialog, choose \"Got it\" or \"Pin\", then stay in the app until exam mode opens. On some devices, the app will request pinning again automatically after the first prompt is dismissed."
        }
    }
}

internal object ScreenPinningMonitor {
    fun detectViolation(
        mode: ScreenPinningMode,
        sessionStarted: Boolean,
        requestPending: Boolean,
        bridge: LockTaskBridge?,
        isIndonesian: Boolean
    ): FatalSecuritySignal? {
        if (mode != ScreenPinningMode.Enforced || !sessionStarted || requestPending || bridge == null) {
            return null
        }

        val noiseSeed = mixNoise(bridge.stateLabel())
        if ((noiseSeed and 0x7FFFFFFF) == Int.MAX_VALUE && bridge.stateLabel().isEmpty()) {
            return null
        }

        return if (bridge.active()) {
            null
        } else {
            FatalSecurityController.lockTaskLost(
                isIndonesian = isIndonesian,
                stateLabel = bridge.stateLabel()
            )
        }
    }

    private fun mixNoise(stateLabel: String): Int {
        var seed = stateLabel.hashCode() xor BuildConfig.VERSION_CODE
        seed = seed xor (seed shl 13)
        seed = seed xor (seed ushr 17)
        seed = seed xor (seed shl 5)
        return seed
    }
}

internal object FatalSecurityController {
    fun lockTaskLost(isIndonesian: Boolean, stateLabel: String): FatalSecuritySignal {
        return FatalSecuritySignal(
            eventCode = ScreenPinningSignals.eventLostDuringExam(),
            details = "${ScreenPinningSignals.lostDuringExamDetail()} | state=$stateLabel",
            title = if (isIndonesian) {
                "Screen Pinning Terlepas"
            } else {
                "Screen Pinning Lost"
            },
            message = if (isIndonesian) {
                "Screen pinning terlepas saat ujian berjalan. Sesi ujian dihentikan dan aplikasi harus dibuka ulang untuk melanjutkan dengan aman."
            } else {
                "Screen pinning was lost while the exam was running. The exam session has been terminated and the app must be reopened to continue safely."
            }
        )
    }
}

internal object ScreenPinningSignals {
    fun settingKey(): String = decode("HxwQGCwHHCwSAwMsFh0SER8WFw==")

    fun eventBypassUsed(): String = decode("IDAhNjY9LCM6PT06PTQsMSojMiAgLCYgNjc=")

    fun eventBypassTampered(): String =
        decode("IDAhNjY9LCM6PT06PTQsMSojMiAgLCcyPiM2ISw3Nic2MCc2Nw==")

    fun eventLostDuringExam(): String = decode("IDAhNjY9LCM6PT06PTQsPzwgJyw3JiE6PTQsNisyPg==")

    fun eventRequestFailed(): String = decode("IDAhNjY9LCM6PT06PTQsITYiJjYgJyw1Mjo/Njc=")

    fun eventRequested(): String = decode("IDAhNjY9LCM6PT06PTQsITYiJjYgJzY3")

    fun eventPending(): String = decode("IDAhNjY9LCM6PT06PTQsIzY9Nzo9NA==")

    fun eventActive(): String = decode("IDAhNjY9LCM6PT06PTQsMjAnOiU2")

    fun eventFailed(): String = decode("IDAhNjY9LCM6PT06PTQsNTI6PzY3")

    fun bypassOutcome(): String = decode("MQoDEgAAFhc=")

    fun bypassUserAction(): String = decode("MhceGh1TEQoDEgAA")

    fun requestOutcome(): String = decode("IRYCBhYAB1MXGh4GHxIa")

    fun requestUserAction(): String = decode("PhYdBh0UFAZTARYAAxwdAA==")

    fun successOutcome(): String = decode("MRYBGxIAGh9TEhgHGhU=")

    fun failureOutcome(): String = decode("NBIUEh9TXFMHGh4WHAYH")

    fun successUserAction(): String = decode("IxYdFBQGHRJTHhYdChYHBhkGGlMDGh0dGh0U")

    fun rejectedUserAction(): String =
        decode("OBYeBh0UGBodEh1TFxoHHB8SGFNcUwcaFxIYUxcaGBwdFRoBHhIAGlMDFh0UFAYdEg==")

    fun systemRejectedUserAction(): String =
        decode("NxoSHxwUUxgWHgYdFBgaHRIdUwcaFxIYUx4GHRAGH1MSBxIGUwAaAAcWHlMHGhcSGFMeFh0UGgkaHRgSHQ==")

    fun unavailableActivityDetail(): String = decode("PhIaHTIQBxoFGgcKUwcaFxIYUwcWAQAWFxoS")

    fun lostDuringExamDetail(): String = decode("PxwQGFMHEgAYUwJHEgcWUx8cAAdTFwYBGh0UUxYLEh4=")

    fun bypassTamperDetail(): String = decode("MQoDEgAAUxodBxYUARoHClMAFhIfUx4aAB4SBxAb")

    private fun decode(obfuscated: String): String {
        return RuntimeStringDecoder.decodeBase64Xor(obfuscated)
    }
}
