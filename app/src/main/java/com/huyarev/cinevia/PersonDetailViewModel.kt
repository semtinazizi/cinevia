package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.network.TmdbApi
import com.huyarev.cinevia.network.TmdbApi.PersonDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val api: TmdbApi
) : ViewModel() {
    private val API_KEY = BuildConfig.TMDB_API_KEY

    private val _personDetail = mutableStateOf<PersonDetail?>(null)
    val personDetail: State<PersonDetail?> = _personDetail

    private val _personMovies = mutableStateOf<List<Movie>>(emptyList())
    val personMovies: State<List<Movie>> = _personMovies

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun loadPerson(personId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detail = api.getPersonDetails(personId, API_KEY)
                _personDetail.value = detail

                val credits = api.getPersonMovies(personId, API_KEY)
                
                // Oyuncu mu yönetmen mi kontrol et
                val movies = if (detail.known_for_department == "Directing") {
                    credits.crew.filter { it.job == "Director" }
                } else {
                    credits.cast
                }
                
                // Tekrarları çıkar ve sırala
                val uniqueMovies = movies.distinctBy { it.id }.sortedByDescending { it.releaseDate ?: "" }
                _personMovies.value = uniqueMovies
                
            } catch (e: Exception) {
                // Hata durumu
            } finally {
                _isLoading.value = false
            }
        }
    }
}
