package com.coblax.examlock.ui.exam

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import com.coblax.examlock.ExamAlarmSeverity
import kotlin.math.roundToInt

internal class ExamAlarmController(private val appContext: Context) {
    private var ringtone: Ringtone? = null
    private var previousAlarmVolume: Int? = null
    private val autoStopHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stop() }

    companion object {
        private const val AUTO_STOP_DURATION_MS = 30_000L
    }

    private val audioManager: AudioManager?
        get() = appContext.getSystemService(AudioManager::class.java)

    fun start(severity: ExamAlarmSeverity = ExamAlarmSeverity.Escalated) {
        if (ringtone?.isPlaying == true) {
            boostAlarmVolume(severity)
            rescheduleAutoStop()
            return
        }

        boostAlarmVolume(severity)

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        ringtone = alarmUri?.let { uri ->
            RingtoneManager.getRingtone(appContext, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        }
        rescheduleAutoStop()
    }

    fun stop() {
        autoStopHandler.removeCallbacks(autoStopRunnable)
        ringtone?.stop()
        ringtone = null
        restoreAlarmVolume()
    }

    private fun rescheduleAutoStop() {
        autoStopHandler.removeCallbacks(autoStopRunnable)
        autoStopHandler.postDelayed(autoStopRunnable, AUTO_STOP_DURATION_MS)
    }

    private fun boostAlarmVolume(severity: ExamAlarmSeverity) {
        val manager = audioManager ?: return
        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (maxVolume <= 0) {
            return
        }

        val targetVolume = (maxVolume * severity.targetVolumeFraction).roundToInt().coerceIn(1, maxVolume)
        if (previousAlarmVolume == null) {
            previousAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        }

        runCatching {
            manager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
        }
    }

    private fun restoreAlarmVolume() {
        val manager = audioManager ?: return
        val originalVolume = previousAlarmVolume ?: return
        previousAlarmVolume = null
        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        runCatching {
            manager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                originalVolume.coerceIn(0, maxVolume),
                0
            )
        }
    }
}
