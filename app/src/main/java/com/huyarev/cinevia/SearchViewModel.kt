package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.network.TmdbApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: TmdbApi
) : ViewModel() {
    private val _searchResults = mutableStateOf<List<Movie>>(emptyList())
    val searchResults: State<List<Movie>> = _searchResults

    private val _userSearchResults = mutableStateOf<List<UserSearchResult>>(emptyList())
    val userSearchResults: State<List<UserSearchResult>> = _userSearchResults

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val API_KEY = BuildConfig.TMDB_API_KEY
    private var searchJob: Job? = null

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _userSearchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isLoading.value = true

            // 1. FİLM ARAMASI
            try {
                val response = api.searchMovies(API_KEY, query)
                _searchResults.value = response.results ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. KİŞİ ARAMASI (Yenilendi)
            searchUsers(query)

            _isLoading.value = false
        }
    }

    private fun searchUsers(query: String) {
        // Aranacak kelimeyi tamamen küçük harfe çeviriyoruz (Örn: "AhMeT" -> "ahmet")
        val lowerQuery = query.lowercase()

        // Firebase'deki strict (katı) kuralı kaldırıp, veriyi Kotlin'in esnek gücüyle süzüyoruz
        db.collection("users")
            .limit(500) // Sistemi yormamak için aktif kullanıcıları çeker
            .get()
            .addOnSuccessListener { snapshot ->
                val results = snapshot.documents.mapNotNull { doc ->
                    val email = doc.id
                    val name = doc.getString("name") ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val profileImageUrl = doc.getString("profileImageUrl")

                    // EĞER isimde VEYA e-postada aranılan kelime geçiyorsa listeye ekle!
                    if (name.lowercase().contains(lowerQuery) || email.lowercase().contains(lowerQuery)) {
                        UserSearchResult(email, name, profileImageUrl)
                    } else {
                        null
                    }
                }.take(20) // Ekrana en fazla 20 kişi dök

                val myEmail = auth.currentUser?.email?.lowercase() ?: ""
                _userSearchResults.value = results.filter { it.email != myEmail }
            }
            .addOnFailureListener {
                _userSearchResults.value = emptyList()
            }
    }
}
