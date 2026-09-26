package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _diaryMovies = mutableStateOf<List<DiaryEntry>>(emptyList())
    val diaryMovies: State<List<DiaryEntry>> = _diaryMovies

    // --- KENDİ PROFİLİM İÇİN CANLI VERİLER ---
    private val _currentUserName = mutableStateOf("")
    val currentUserName: State<String> = _currentUserName

    private val _currentUserBio = mutableStateOf("")
    val currentUserBio: State<String> = _currentUserBio

    // KRİTİK DEĞİŞKEN: Hata almamak için bu ismin tam olması şart!
    private val _currentUserProfileImage = mutableStateOf<String?>(null)
    val currentUserProfileImage: State<String?> = _currentUserProfileImage

    private val _currentUserFollowers = mutableStateOf<List<String>>(emptyList())
    val currentUserFollowers: State<List<String>> = _currentUserFollowers

    private val _currentUserFollowing = mutableStateOf<List<String>>(emptyList())
    val currentUserFollowing: State<List<String>> = _currentUserFollowing

    // --- HEDEF KULLANICI (BAŞKASININ PROFİLİ) ---
    private val _targetFollowerCount = mutableStateOf(0)
    val targetFollowerCount: State<Int> = _targetFollowerCount

    private val _targetFollowingCount = mutableStateOf(0)
    val targetFollowingCount: State<Int> = _targetFollowingCount

    private val _isFollowing = mutableStateOf(false)
    val isFollowing: State<Boolean> = _isFollowing

    private val _targetProfileImage = mutableStateOf<String?>(null)
    val targetProfileImage: State<String?> = _targetProfileImage

    private val _targetName = mutableStateOf("")
    val targetName: State<String> = _targetName

    private val _targetBio = mutableStateOf("")
    val targetBio: State<String> = _targetBio

    private val _followListData = mutableStateOf<List<CineRevUser>>(emptyList())
    val followListData: State<List<CineRevUser>> = _followListData

    init {
        fetchDiaryMovies()
        loadCurrentUserProfile() // Başlar başlamaz kendi bilgilerini dinle
    }

    private fun fetchDiaryMovies() {
        val email = auth.currentUser?.email?.lowercase() ?: return
        db.collection("users").document(email).collection("diary")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _diaryMovies.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DiaryEntry::class.java)
                    }
                }
            }
    }

    // Kendi profil bilgilerini ve RESMİNİ Firestore'dan canlı dinle
    fun loadCurrentUserProfile() {
        val email = auth.currentUser?.email?.lowercase() ?: return
        db.collection("users").document(email).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                _currentUserName.value = snapshot.getString("name") ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                _currentUserBio.value = snapshot.getString("bio") ?: "CineRev evreninde yeni bir gezgin..."

                // Resim URL'si güncellendiği an burası tetiklenir ve arayüze haber verir
                val url = snapshot.getString("profileImageUrl")
                _currentUserProfileImage.value = if (url.isNullOrBlank()) null else url
            }
        }
    }

    fun updateProfileInfo(newName: String, newBio: String, onComplete: () -> Unit) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        val data = mapOf(
            "name" to newName.trim(),
            "bio" to newBio.trim()
        )
        db.collection("users").document(email).set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onComplete() }
    }

    fun loadTargetUserProfile(targetEmail: String) {
        val currentUserEmail = auth.currentUser?.email?.lowercase() ?: return
        val normalizedTargetEmail = targetEmail.lowercase()
        db.collection("users").document(normalizedTargetEmail).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val followers = snapshot.get("followers") as? List<String> ?: emptyList()
                val following = snapshot.get("following") as? List<String> ?: emptyList()

                _targetFollowerCount.value = followers.size
                _targetFollowingCount.value = following.size
                _isFollowing.value = followers.contains(currentUserEmail)
                _targetProfileImage.value = snapshot.getString("profileImageUrl")
                _targetName.value = snapshot.getString("name") ?: normalizedTargetEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                _targetBio.value = snapshot.getString("bio") ?: "CineRev evreninde yeni bir gezgin..."
            }
        }
    }

    fun toggleFollow(targetEmail: String) {
        val currentUserEmail = auth.currentUser?.email?.lowercase() ?: return
        val normalizedTargetEmail = targetEmail.lowercase()
        if (currentUserEmail == normalizedTargetEmail) return
        val targetRef = db.collection("users").document(normalizedTargetEmail)
        val currentRef = db.collection("users").document(currentUserEmail)

        db.runTransaction { transaction ->
            val targetSnapshot = transaction.get(targetRef)
            val followers = targetSnapshot.get("followers") as? List<String> ?: emptyList()

            if (followers.contains(currentUserEmail)) {
                transaction.update(targetRef, "followers", FieldValue.arrayRemove(currentUserEmail))
                transaction.update(currentRef, "following", FieldValue.arrayRemove(normalizedTargetEmail))
            } else {
                transaction.update(targetRef, "followers", FieldValue.arrayUnion(currentUserEmail))
                transaction.update(currentRef, "following", FieldValue.arrayUnion(normalizedTargetEmail))
            }
            null
        }
    }

    fun listenToMyFollowStats() {
        val email = auth.currentUser?.email?.lowercase() ?: return
        db.collection("users").document(email).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                _currentUserFollowers.value = snapshot.get("followers") as? List<String> ?: emptyList()
                _currentUserFollowing.value = snapshot.get("following") as? List<String> ?: emptyList()
            }
        }
    }

    fun fetchFollowList(ownerEmail: String, isFollowers: Boolean) {
        val normalizedOwnerEmail = ownerEmail.lowercase()
        _followListData.value = emptyList()
        db.collection("users").document(normalizedOwnerEmail).get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                val fieldName = if (isFollowers) "followers" else "following"
                val emails = snapshot.get(fieldName) as? List<String> ?: emptyList()
                if (emails.isNotEmpty()) {
                    db.collection("users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), emails).get().addOnSuccessListener { userDocs ->
                        _followListData.value = userDocs.documents.mapNotNull { doc ->
                            CineRevUser(email = doc.id, profileImageUrl = doc.getString("profileImageUrl"), name = doc.getString("name") ?: doc.id)
                        }
                    }
                }
            }
        }
    }
}
