package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Bulunan kullanıcıların veri yapısı
data class CineRevUser(
    val email: String,
    val profileImageUrl: String?,
    val name: String? = null
)

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _searchResults = mutableStateOf<List<CineRevUser>>(emptyList())
    val searchResults: State<List<CineRevUser>> = _searchResults

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun searchUser(query: String) {
        val safeQuery = query.trim().lowercase() // Boşlukları sil ve harfleri küçült

        if (safeQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").get().await()
                val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

                val results = snapshot.documents.mapNotNull { doc ->
                    val email = doc.id
                    // Eşleşmeyi kontrol et ve Kullanıcının kendisini gizle
                    if (email.lowercase().contains(safeQuery) && email != currentUserEmail) {
                        CineRevUser(
                            email = email,
                            profileImageUrl = doc.getString("profileImageUrl")
                        )
                    } else null
                }
                _searchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
} // <-- İşte silinen o meşhur son parantez!
