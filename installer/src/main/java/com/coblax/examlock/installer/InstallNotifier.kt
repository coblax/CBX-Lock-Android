package com.coblax.examlock.installer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Tells the student how the install ended when the installer is not on screen at that moment,
 * for example after switching away while Android was still installing.
 */
internal object InstallNotifier {
    private const val ChannelId = "install_result"
    private const val NotificationId = 1

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return Build.VERSION.SDK_INT < 24 || manager.areNotificationsEnabled()
    }

    fun show(context: Context, success: Boolean, title: String, body: String) {
        if (!canNotify(context)) return
        runCatching {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val texts = Texts.forLocale()
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        ChannelId,
                        texts.t("Installation result", "Hasil pemasangan"),
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, InstallerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            @Suppress("DEPRECATION")
            val builder = if (Build.VERSION.SDK_INT >= 26) {
                Notification.Builder(context, ChannelId)
            } else {
                Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
            }
            val notification = builder
                .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText(body))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            manager.notify(NotificationId, notification)
        }
    }
}
