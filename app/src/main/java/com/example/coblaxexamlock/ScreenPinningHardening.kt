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

internal enum class PinningActivationState {
    Idle,
    Requested,
    WaitingForSystemDialog,
    WaitingForLockTaskActive,
    ActiveConfirmed,
    TimeoutRetryReady;

    fun isPending(): Boolean {
        return this == Requested ||
            this == WaitingForSystemDialog ||
            this == WaitingForLockTaskActive
    }
}

internal enum class PinningActivationPurpose {
    ExamStart,
    PreparationSetup
}

internal const val PinningActivationGraceWindowMillis = 12_000L
internal const val PinningActivationTimeoutMillis = 20_000L
internal const val PinningActivationPollIntervalMillis = 250L

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
    val guidanceMessage: String?,
    val engageAttemptCount: Int = 0
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

internal fun shouldStartExamLockTask(
    enabled: Boolean,
    allowLockTask: Boolean,
    lockTaskAlreadyActive: Boolean
): Boolean = enabled && allowLockTask && !lockTaskAlreadyActive

internal fun shouldStopExamLockTask(
    enabled: Boolean,
    lockTaskAlreadyActive: Boolean
): Boolean = !enabled && lockTaskAlreadyActive

internal fun shouldIssueScreenPinningEngageAttempt(
    lockTaskAlreadyActive: Boolean,
    engageAttemptCount: Int,
    maxEngageAttempts: Int = 1
): Boolean = !lockTaskAlreadyActive && engageAttemptCount < maxEngageAttempts

internal fun isWithinPinningActivationGrace(
    startedAtElapsedMs: Long?,
    nowElapsedMs: Long,
    graceWindowMillis: Long = PinningActivationGraceWindowMillis
): Boolean {
    val startedAt = startedAtElapsedMs ?: return false
    return (nowElapsedMs - startedAt).coerceAtLeast(0L) <= graceWindowMillis
}

internal fun shouldSuppressPinningTransitionViolation(
    lockTaskRequestPending: Boolean,
    examSessionStarted: Boolean,
    startedAtElapsedMs: Long?,
    nowElapsedMs: Long,
    graceWindowMillis: Long = PinningActivationGraceWindowMillis
): Boolean {
    return lockTaskRequestPending &&
        !examSessionStarted &&
        isWithinPinningActivationGrace(
            startedAtElapsedMs = startedAtElapsedMs,
            nowElapsedMs = nowElapsedMs,
            graceWindowMillis = graceWindowMillis
        )
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
    private const val ActivationTimeoutMillis = PinningActivationTimeoutMillis
    private const val PollIntervalMillis = PinningActivationPollIntervalMillis
    private const val MaxEngageAttempts = 2

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
        if (bridge.active()) {
            return alreadyActiveReport(bridge)
        }
        val startedAt = SystemClock.elapsedRealtime()
        delay(InitialEngageDelayMillis)
        if (bridge.active()) {
            return alreadyActiveReport(bridge)
        }
        var engageAttemptCount = 0
        if (
            shouldIssueScreenPinningEngageAttempt(
                lockTaskAlreadyActive = bridge.active(),
                engageAttemptCount = engageAttemptCount,
                maxEngageAttempts = MaxEngageAttempts
            )
        ) {
            bridge.engage(allowLockTask = true)
            engageAttemptCount += 1
        }
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
                    guidanceMessage = null,
                    engageAttemptCount = engageAttemptCount
                )
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
            guidanceMessage = localizedScreenPinningGuidance(isIndonesian),
            engageAttemptCount = engageAttemptCount
        )
    }

    private fun alreadyActiveReport(bridge: LockTaskBridge): ScreenPinningActivationReport {
        return ScreenPinningActivationReport(
            active = true,
            afterState = bridge.stateLabel(),
            dialogLikelyShown = false,
            outcome = ScreenPinningSignals.successOutcome(),
            userActionInference = ScreenPinningSignals.successUserAction(),
            activationDurationMs = 0L,
            guidanceMessage = null,
            engageAttemptCount = 0
        )
    }

    fun unavailableActivityMessage(isIndonesian: Boolean): String {
        return if (isIndonesian) {
            "Mode ujian tidak bisa dimulai karena activity utama tidak tersedia."
        } else {
            "Exam mode could not start because the main activity is unavailable."
        }
    }

    fun pendingMessage(
        isIndonesian: Boolean,
        purpose: PinningActivationPurpose = PinningActivationPurpose.ExamStart
    ): String = localizedScreenPinningGuidance(isIndonesian, purpose)

    fun activatingMessage(
        isIndonesian: Boolean,
        purpose: PinningActivationPurpose = PinningActivationPurpose.ExamStart
    ): String {
        return when (purpose) {
            PinningActivationPurpose.PreparationSetup -> if (isIndonesian) {
                "Jika Android menampilkan dialog pin aplikasi, pilih Got it atau Pin. Tetap di layar Preparation sampai Screen Pinning aktif."
            } else {
                "If Android shows the app pinning dialog, choose Got it or Pin. Stay on Preparation until Screen Pinning is active."
            }
            PinningActivationPurpose.ExamStart -> if (isIndonesian) {
                "Jika Android menampilkan dialog pin aplikasi, pilih Got it atau Pin. Tetap di layar ini sampai Screen Pinning aktif."
            } else {
                "If Android shows the app pinning dialog, choose Got it or Pin. Stay on this screen until Screen Pinning is active."
            }
        }
    }

    fun retryMessage(
        isIndonesian: Boolean,
        purpose: PinningActivationPurpose = PinningActivationPurpose.ExamStart
    ): String {
        return when (purpose) {
            PinningActivationPurpose.PreparationSetup -> if (isIndonesian) {
                "Tekan Start Screen Pinning lagi, pilih Got it/Pin, lalu jangan buka Home atau Recent sampai status Screen Pinning aktif."
            } else {
                "Press Start Screen Pinning again, choose Got it/Pin, then do not open Home or Recents until Screen Pinning is active."
            }
            PinningActivationPurpose.ExamStart -> if (isIndonesian) {
                "Kembali ke Preparation, tekan Start Screen Pinning, pilih Got it/Pin, lalu mulai ujian setelah status aktif."
            } else {
                "Return to Preparation, press Start Screen Pinning, choose Got it/Pin, then start the exam after it is active."
            }
        }
    }

    fun transitionInterruptedMessage(isIndonesian: Boolean): String {
        return if (isIndonesian) {
            "Screen pinning dibatalkan karena tombol Home/Recent dibuka saat Android masih mengaktifkan pinning. Tetap di layar ini setelah menekan \"Got it\" atau \"Pin\", tunggu sampai Screen Pinning aktif, lalu jangan buka Recent Apps."
        } else {
            "Screen pinning was interrupted because Home/Recent was opened while Android was still activating pinning. Stay on this screen after choosing \"Got it\" or \"Pin\", wait until Screen Pinning is active, and do not open Recent Apps."
        }
    }

    private fun localizedScreenPinningGuidance(
        isIndonesian: Boolean,
        purpose: PinningActivationPurpose = PinningActivationPurpose.ExamStart
    ): String {
        return when (purpose) {
            PinningActivationPurpose.PreparationSetup -> if (isIndonesian) {
                "Screen pinning belum aktif. Saat Android menampilkan dialog pin aplikasi, pilih \"Got it\" atau \"Pin\", lalu tetap di aplikasi sampai status Screen Pinning aktif. Jika dialog tidak muncul, buka Settings > Security > Screen Pinning, aktifkan, lalu tekan Start Screen Pinning lagi."
            } else {
                "Screen pinning is not active yet. When Android shows the app pinning dialog, choose \"Got it\" or \"Pin\", then stay in the app until Screen Pinning is active. If the dialog does not appear, open Settings > Security > Screen Pinning, enable it, then press Start Screen Pinning again."
            }
            PinningActivationPurpose.ExamStart -> if (isIndonesian) {
                "Screen pinning belum aktif. Kembali ke Preparation, tekan Start Screen Pinning, pilih \"Got it\" atau \"Pin\", lalu mulai ujian setelah status aktif. Jika dialog tidak muncul, buka Settings > Security > Screen Pinning dan aktifkan."
            } else {
                "Screen pinning is not active yet. Return to Preparation, press Start Screen Pinning, choose \"Got it\" or \"Pin\", then start the exam after it is active. If the dialog does not appear, open Settings > Security > Screen Pinning and enable it."
            }
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
