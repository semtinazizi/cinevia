package com.huyarev.cinevia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CineRevMessagingService : FirebaseMessagingService() {

    // Arka planda yeni bir bildirim geldiğinde tetiklenir
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Firebase'den gelen başlık ve mesajı al
        val title = remoteMessage.notification?.title ?: "CineRev"
        val body = remoteMessage.notification?.body ?: "Yeni bir mesajınız var."

        showNotification(title, body)
    }

    // Kullanıcının cihaza özel bildirim kimliği (Token) değiştiğinde tetiklenir
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // İleride kişiye özel bildirim atmak istersen bu token'ı Firestore'daki kullanıcı profiline kaydedebilirsin.
    }

    // Bildirimi Ekranda Çizme Algoritması
    private fun showNotification(title: String, message: String) {
        val channelId = "CineRev_general_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (Oreo) ve üzeri için Kanal (Channel) oluşturmak zorunludur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CineRev Genel Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Bildirime tıklanınca uygulamayı aç
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // Geçici ikon, sonra kendi logonu koyabilirsin
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true) // Tıklanınca kaybolsun
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Benzersiz bir ID ile bildirimi fırlat
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
