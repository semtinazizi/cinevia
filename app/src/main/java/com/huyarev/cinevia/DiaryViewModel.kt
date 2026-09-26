package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huyarev.cinevia.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _diaryMovies = mutableStateOf<List<DiaryEntry>>(emptyList())
    val diaryMovies: State<List<DiaryEntry>> = _diaryMovies

    init {
        fetchDiaryMovies()
    }

    private fun fetchDiaryMovies() {
        viewModelScope.launch {
            // We listen to local cache which is synced by getDiaryMovies()
            repository.getLocalDiaryMovies().collect { movies ->
                _diaryMovies.value = movies
            }
        }
        // Also trigger sync
        viewModelScope.launch {
            repository.getDiaryMovies().collect { }
        }
    }

    // Kullanıcı notunu yazıp kaydettiğinde Firestore'u güncelleyecek fonksiyon
    fun updateReview(movieId: Int, newReview: String) {
        repository.updateReview(movieId, newReview)
    }
}
