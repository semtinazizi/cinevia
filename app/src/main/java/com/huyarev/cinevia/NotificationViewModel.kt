package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class NotificationItem(
    val id: String = "",
    val toEmail: String = "",
    val fromEmail: String = "",
    val fromName: String = "",
    val type: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    // SADECE GENEL BİLDİRİMLERİ (Takip, Beğeni, Davet) TUTACAK LİSTE
    private val _notifications = mutableStateOf<List<NotificationItem>>(emptyList())
    val notifications: State<List<NotificationItem>> = _notifications

    // ZİL İKONU İÇİN (Okunmamış Genel Bildirim Sayısı)
    private val _unreadCount = mutableStateOf(0)
    val unreadCount: State<Int> = _unreadCount

    // YENİ: ZARF İKONU İÇİN (Okunmamış Mesaj Sayısı)
    private val _unreadMessageCount = mutableStateOf(0)
    val unreadMessageCount: State<Int> = _unreadMessageCount

    init {
        listenForNotifications()
    }

    private fun listenForNotifications() {
        val userEmail = auth.currentUser?.email?.lowercase() ?: return

        db.collection("notifications")
            .whereEqualTo("toEmail", userEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        NotificationItem(
                            id = doc.id,
                            toEmail = doc.getString("toEmail") ?: "",
                            fromEmail = doc.getString("fromEmail") ?: "",
                            fromName = doc.getString("fromName") ?: "",
                            type = doc.getString("type") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    }

                    // 1. Gelen verileri ikiye bölüyoruz!
                    val generalNotifs = items.filter { it.type != "message" }.sortedByDescending { it.timestamp }
                    val messageNotifs = items.filter { it.type == "message" }

                    // 2. Ekrana sadece genel bildirimleri yolla (Mesajlar bildirim ekranını kirletmesin)
                    _notifications.value = generalNotifs

                    // 3. Zil ve Zarf ikonlarındaki kırmızı nokta (Badge) sayılarını güncelle
                    _unreadCount.value = generalNotifs.count { !it.isRead }
                    _unreadMessageCount.value = messageNotifs.count { !it.isRead }
                }
            }
    }

    // Tıklanılan genel bildirimi okundu yap
    fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId).update("isRead", true)
    }

    // Tüm genel bildirimleri okundu yap
    fun markAllAsRead() {
        val unreadNotifs = _notifications.value.filter { !it.isRead }
        if (unreadNotifs.isEmpty()) return

        db.runBatch { batch ->
            unreadNotifs.forEach { notif ->
                val ref = db.collection("notifications").document(notif.id)
                batch.update(ref, "isRead", true)
            }
        }
    }

    // YENİ: Kullanıcı Mesajlar Kutusu'na (Inbox) girdiğinde arka planda tüm mesaj bildirimlerini temizle
    fun markAllMessagesAsRead() {
        val userEmail = auth.currentUser?.email?.lowercase() ?: return
        db.collection("notifications")
            .whereEqualTo("toEmail", userEmail)
            .whereEqualTo("type", "message")
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener
                db.runBatch { batch ->
                    snapshot.documents.forEach { doc ->
                        batch.update(doc.reference, "isRead", true)
                    }
                }
            }
    }
}
