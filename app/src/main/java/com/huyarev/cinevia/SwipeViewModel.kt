package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huyarev.cinevia.data.repository.SocialRepository
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.network.TmdbApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val api: TmdbApi
) : ViewModel() {
    private val _movies = mutableStateOf<List<Movie>>(emptyList())
    val movies: State<List<Movie>> = _movies

    private val _currentIndex = mutableStateOf(0)
    val currentIndex: State<Int> = _currentIndex

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val API_KEY = BuildConfig.TMDB_API_KEY

    init {
        fetchSwipeMovies()
    }

    private fun fetchSwipeMovies() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = api.getPopularMovies(apiKey = API_KEY)
                _movies.value = response.results?.shuffled() ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun nextMovie() {
        if (_currentIndex.value < _movies.value.size - 1) {
            _currentIndex.value++
        } else {
            fetchSwipeMovies()
            _currentIndex.value = 0
        }
    }

    fun saveToWatchlist(movie: Movie) {
        repository.saveLikedMovie(movie)
    }
}
