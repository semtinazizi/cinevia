package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

data class UserSearchResult(
    val email: String = "",
    val name: String = "",
    val profileImageUrl: String? = null
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messages = mutableStateOf<List<ChatMessage>>(emptyList())
    val messages: State<List<ChatMessage>> = _messages

    private val _isAdmin = mutableStateOf(false)
    val isAdmin: State<Boolean> = _isAdmin

    private val _isMember = mutableStateOf(false)
    val isMember: State<Boolean> = _isMember

    private val _searchResults = mutableStateOf<List<UserSearchResult>>(emptyList())
    val searchResults: State<List<UserSearchResult>> = _searchResults

    private var currentGroupId: String = ""
    private var messageListener: ListenerRegistration? = null
    private var groupListener: ListenerRegistration? = null

    fun loadGroup(groupId: String) {
        if (currentGroupId == groupId) return
        currentGroupId = groupId

        groupListener?.remove()
        groupListener = db.collection("groups").document(groupId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener

                val creator = doc.getString("createdBy") ?: ""
                val members = doc.get("members") as? List<String> ?: emptyList()
                val myEmail = auth.currentUser?.email?.lowercase() ?: ""

                _isAdmin.value = (creator == myEmail && myEmail.isNotEmpty())
                _isMember.value = _isAdmin.value || members.contains(myEmail)
            }

        listenForMessages()
    }

    fun deleteGroup(onComplete: () -> Unit) {
        if (_isAdmin.value && currentGroupId.isNotEmpty()) {
            db.collection("groups").document(currentGroupId).delete().addOnSuccessListener {
                onComplete()
            }
        }
    }

    // --- YENİLENEN AKILLI ARAMA MOTORU ---
    fun searchUsers(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        val lowerQuery = query.lowercase()

        db.collection("users")
            .limit(500)
            .get()
            .addOnSuccessListener { snapshot ->
                val results = snapshot.documents.mapNotNull { doc ->
                    val email = doc.id
                    val name = doc.getString("name") ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val profileImageUrl = doc.getString("profileImageUrl")

                    // İsim veya mailde eşleşme varsa al
                    if (name.lowercase().contains(lowerQuery) || email.lowercase().contains(lowerQuery)) {
                        UserSearchResult(email, name, profileImageUrl)
                    } else {
                        null
                    }
                }.take(5) // Grup aramasında 5 kişi göstermek yeterli

                val myEmail = auth.currentUser?.email?.lowercase() ?: ""
                _searchResults.value = results.filter { it.email != myEmail }
            }
            .addOnFailureListener {
                _searchResults.value = emptyList()
            }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun addMember(targetEmail: String, onResult: (String) -> Unit) {
        if (!_isAdmin.value || currentGroupId.isEmpty() || targetEmail.isBlank()) {
            onResult("Sadece grup yöneticisi üye ekleyebilir.")
            return
        }

        val emailTrimmed = targetEmail.trim().lowercase()

        db.collection("users").document(emailTrimmed).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val groupRef = db.collection("groups").document(currentGroupId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(groupRef)
                    val members = snapshot.get("members") as? List<String> ?: emptyList()
                    val groupName = snapshot.getString("name") ?: "bir gruba"

                    if (!members.contains(emailTrimmed)) {
                        transaction.update(groupRef, "members", com.google.firebase.firestore.FieldValue.arrayUnion(emailTrimmed))
                        transaction.update(groupRef, "memberCount", com.google.firebase.firestore.FieldValue.increment(1))

                        val currentUserEmail = auth.currentUser?.email?.lowercase() ?: ""
                        val currentName = currentUserEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val notification = hashMapOf(
                            "toEmail" to emailTrimmed,
                            "fromEmail" to currentUserEmail,
                            "fromName" to currentName,
                            "type" to "group_invite",
                            "message" to "seni '$groupName' grubuna ekledi.",
                            "timestamp" to System.currentTimeMillis(),
                            "isRead" to false
                        )
                        db.collection("notifications").add(notification)
                    }
                    null
                }.addOnSuccessListener {
                    onResult("Kullanıcı başarıyla gruba eklendi!")
                }.addOnFailureListener {
                    onResult("Eklenirken bir hata oluştu.")
                }
            } else {
                onResult("CineRev evreninde böyle bir kullanıcı bulunamadı!")
            }
        }.addOnFailureListener {
            onResult("Bağlantı hatası oluştu.")
        }
    }

    private fun listenForMessages() {
        if (currentGroupId.isEmpty()) return
        messageListener?.remove()

        messageListener = db.collection("groups").document(currentGroupId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    _messages.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentGroupId.isEmpty() || !_isMember.value) return

        val senderEmail = auth.currentUser?.email?.lowercase() ?: "Anonim"
        val senderName = senderEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

        val chatMessage = ChatMessage(
            id = "",
            sender = senderName,
            message = text,
            timestamp = System.currentTimeMillis()
        )

        db.collection("groups").document(currentGroupId).collection("messages").add(chatMessage)
    }

    fun isCurrentUser(senderName: String): Boolean {
        val currentEmail = auth.currentUser?.email?.lowercase() ?: ""
        val currentName = currentEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        return senderName == currentName
    }
}
