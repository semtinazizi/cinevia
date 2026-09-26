package com.huyarev.cinevia

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ChatInboxItem(
    val chatId: String = "",
    val targetEmail: String = "",
    val targetName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _inboxItems = mutableStateOf<List<ChatInboxItem>>(emptyList())
    val inboxItems: State<List<ChatInboxItem>> = _inboxItems

    init {
        loadInbox()
    }

    private fun loadInbox() {
        val myEmail = auth.currentUser?.email?.lowercase() ?: return

        // İçinde benim olduğum tüm sohbet odalarını getir
        db.collection("chats")
            .whereArrayContains("users", myEmail)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CineRev", "Gelen Kutusu Çekilirken Hata: ", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val users = doc.get("users") as? List<String> ?: return@mapNotNull null
                        val userNames = doc.get("userNames") as? Map<String, String> ?: return@mapNotNull null

                        val targetEmail = users.firstOrNull { it != myEmail } ?: myEmail
                        val targetName = userNames[targetEmail] ?: "Kullanıcı"
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        ChatInboxItem(doc.id, targetEmail, targetName, lastMessage, timestamp)
                    }

                    // Telefonun gücüyle tarihe göre sıralama yapıyoruz
                    _inboxItems.value = items.sortedByDescending { it.timestamp }
                }
            }
    }

    // YENİ EKLENEN KISIM: SOHBETİ SİL (Kendini odadan çıkar)
    fun deleteChat(chatId: String) {
        val myEmail = auth.currentUser?.email?.lowercase() ?: return

        // FieldValue.arrayRemove kullanarak sadece kendimizi siliyoruz
        db.collection("chats").document(chatId)
            .update("users", com.google.firebase.firestore.FieldValue.arrayRemove(myEmail))
    }
}
