package org.umn.jalankita.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import org.umn.jalankita.MainActivity
import org.umn.jalankita.R

/**
 * System notifications for report lifecycle events.
 *
 * Posting is best-effort: on Android 13+ it silently does nothing until the user
 * grants POST_NOTIFICATIONS, and the in-app Notifikasi list is always the
 * authoritative record either way.
 */
class NotificationPresenter(private val context: Context) : StatusNotifier {

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.status_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Perubahan status laporan kerusakan jalan Anda."
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.sync_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Konfirmasi laporan yang berhasil disinkronkan."
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROXIMITY,
                context.getString(R.string.proximity_channel_name),
                // Higher importance than the rest: this one is about the road
                // the user is on right now, not about paperwork.
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Peringatan kerusakan jalan di dekat lokasi Anda."
                enableVibration(true)
            },
        )
    }

    override fun show(
        title: String,
        message: String,
        reportId: String?,
        kind: NotificationKind,
    ) {
        if (!context.canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (reportId != null) putExtra(EXTRA_REPORT_ID, reportId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, kind.channelId())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(kind.priority())
            .setCategory(
                if (kind == NotificationKind.PROXIMITY) {
                    NotificationCompat.CATEGORY_NAVIGATION
                } else {
                    NotificationCompat.CATEGORY_STATUS
                },
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(reportId?.hashCode() ?: title.hashCode(), notification)
        }
    }

    private fun Context.canPostNotifications(): Boolean =
        NotificationManagerCompat.from(this).areNotificationsEnabled()

    private fun NotificationKind.channelId(): String = when (this) {
        NotificationKind.SYNC -> CHANNEL_SYNC
        NotificationKind.STATUS -> CHANNEL_STATUS
        NotificationKind.PROXIMITY -> CHANNEL_PROXIMITY
    }

    private fun NotificationKind.priority(): Int = when (this) {
        NotificationKind.SYNC -> NotificationCompat.PRIORITY_LOW
        NotificationKind.STATUS -> NotificationCompat.PRIORITY_DEFAULT
        NotificationKind.PROXIMITY -> NotificationCompat.PRIORITY_HIGH
    }

    companion object {
        const val EXTRA_REPORT_ID = "jalankita.extra.REPORT_ID"
        private const val CHANNEL_STATUS = "report_status"
        private const val CHANNEL_SYNC = "report_sync"
        private const val CHANNEL_PROXIMITY = "road_proximity"
    }
}
