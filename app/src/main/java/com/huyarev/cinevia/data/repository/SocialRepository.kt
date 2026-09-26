package com.huyarev.cinevia.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.huyarev.cinevia.DirectMessage
import com.huyarev.cinevia.WatchGroup
import com.huyarev.cinevia.network.Movie
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    fun getGroups(): Flow<List<WatchGroup>> = callbackFlow {
        val listener = db.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val groupList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WatchGroup::class.java)?.copy(id = doc.id)
                    }
                    trySend(groupList)
                }
            }
        awaitClose { listener.remove() }
    }

    fun deleteGroup(groupId: String) {
        db.collection("groups").document(groupId).delete()
    }

    fun createGroup(groupName: String) {
        if (groupName.isBlank()) return
        val currentUserEmail = auth.currentUser?.email?.lowercase() ?: return
        val newGroup = WatchGroup(
            name = groupName,
            currentMovie = "Sürpriz Film",
            memberCount = 1,
            createdBy = currentUserEmail
        )
        db.collection("groups").add(newGroup)
    }

    private fun getChatId(email1: String, email2: String): String {
        val e1 = email1.lowercase()
        val e2 = email2.lowercase()
        return if (e1 < e2) "${e1}_${e2}" else "${e2}_${e1}"
    }

    fun getMessages(targetUserEmail: String): Flow<List<DirectMessage>> = callbackFlow {
        val myEmail = auth.currentUser?.email?.lowercase() ?: return@callbackFlow
        val targetEmail = targetUserEmail.lowercase()
        val chatId = getChatId(myEmail, targetEmail)

        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messageList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DirectMessage::class.java)?.copy(id = doc.id)
                    }
                    trySend(messageList)
                }
            }
        awaitClose { listener.remove() }
    }

    fun sendMessage(targetUserEmail: String, targetUserName: String, content: String) {
        if (content.isBlank()) return
        val user = auth.currentUser ?: return
        val myEmail = user.email?.lowercase() ?: return
        val targetEmail = targetUserEmail.lowercase()
        val myName = myEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val chatId = getChatId(myEmail, targetEmail)
        val currentTime = System.currentTimeMillis()

        val newMessage = DirectMessage(
            "", myEmail, myName, targetEmail, targetUserName, content, currentTime
        )

        val chatRef = db.collection("chats").document(chatId)
        val chatMeta = hashMapOf(
            "users" to listOf(myEmail, targetEmail),
            "userNames" to mapOf(myEmail to myName, targetEmail to targetUserName),
            "lastMessage" to content,
            "timestamp" to currentTime
        )
        chatRef.set(chatMeta, SetOptions.merge())

        chatRef.collection("messages").add(newMessage)
            .addOnSuccessListener {
                val notification = hashMapOf(
                    "toEmail" to targetEmail,
                    "fromEmail" to myEmail,
                    "fromName" to myName,
                    "type" to "message",
                    "message" to "sana yeni bir mesaj gönderdi.",
                    "timestamp" to currentTime,
                    "isRead" to false
                )
                db.collection("notifications").add(notification)
            }
    }

    fun saveLikedMovie(movie: Movie) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        val movieId = movie.id.toString()

        val diaryEntry = hashMapOf(
            "movieId" to movie.id,
            "id" to movieId,
            "title" to (movie.title ?: movie.name ?: "Bilinmeyen İçerik"),
            "posterUrl" to movie.fullImageUrl,
            "status" to "Beğendiklerim",
            "rating" to 0,
            "timestamp" to System.currentTimeMillis(),
            "myReview" to ""
        )

        db.collection("users").document(email).collection("diary")
            .document(movieId)
            .set(diaryEntry, SetOptions.merge())
    }
}
