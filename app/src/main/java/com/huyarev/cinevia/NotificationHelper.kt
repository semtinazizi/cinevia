package com.huyarev.cinevia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "CineRev_notifications"

    fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 ve üzeri için Kanal (Channel) oluşturmak zorunludur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CineRev Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mesaj ve Takipçi Bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Bildirime tıklayınca CineRev'yı açması için bir niyet (Intent) oluşturuyoruz
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Bildirimin Görsel Tasarımı
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Eğer kendi logon varsa R.drawable.ic_launcher_foreground yapabilirsin
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Tıklayınca bildirim silinsin
            .setContentIntent(pendingIntent)

        // Benzersiz bir ID ile bildirimi fırlat!
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
