package com.huyarev.cinevia.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.huyarev.cinevia.DiaryEntry
import com.huyarev.cinevia.data.local.dao.DiaryDao
import com.huyarev.cinevia.data.local.entity.DiaryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getDiaryMovies(): Flow<List<DiaryEntry>> = callbackFlow {
        val currentUser = auth.currentUser
        val email = currentUser?.email
        if (email == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(email)
            .collection("diary")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val movies = snapshot.documents.mapNotNull { it.toObject(DiaryEntry::class.java) }
                    scope.launch {
                        diaryDao.insertDiaryEntries(movies.map { it.toEntity() })
                    }
                    trySend(movies)
                }
            }
        
        awaitClose { listener.remove() }
    }

    fun getLocalDiaryMovies(): Flow<List<DiaryEntry>> {
        return diaryDao.getAllDiaryEntries().map { entities ->
            entities.map { it.toEntry() }
        }
    }

    fun updateReview(movieId: Int, newReview: String) {
        val currentUser = auth.currentUser
        val email = currentUser?.email ?: return
        db.collection("users").document(email)
            .collection("diary").document(movieId.toString())
            .update("myReview", newReview)
    }

    fun addToDiary(
        movieId: Int,
        title: String,
        posterUrl: String,
        status: String,
        rating: Int,
        review: String = "",
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val entry = hashMapOf(
            "movieId" to movieId,
            "id" to movieId.toString(),
            "title" to title,
            "posterUrl" to posterUrl,
            "status" to status,
            "rating" to rating,
            "myReview" to review,
            "timestamp" to System.currentTimeMillis(),
            "id" to movieId.toString()
        )

        db.collection("users").document(email).collection("diary").document(movieId.toString())
            .set(entry, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun DiaryEntry.toEntity(): DiaryEntity = DiaryEntity(
        movieId = movieId,
        firestoreId = firestoreId,
        title = title,
        posterUrl = posterUrl,
        status = status,
        rating = rating,
        timestamp = timestamp,
        myReview = myReview
    )

    private fun DiaryEntity.toEntry(): DiaryEntry = DiaryEntry(
        movieId = movieId,
        firestoreId = firestoreId,
        title = title,
        posterUrl = posterUrl,
        status = status,
        rating = rating,
        timestamp = timestamp,
        myReview = myReview
    )
}
