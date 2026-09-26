package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huyarev.cinevia.data.repository.MovieRepository
import com.huyarev.cinevia.network.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ContentType { MOVIE, TV_SHOW }
data class Genre(val id: Int, val name: String)

data class HomeUiState(
    val movies: List<Movie> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val isLoading: Boolean = true,
    val selectedGenre: Genre? = null,
    val selectedContentType: ContentType = ContentType.MOVIE,
    val errorMessage: String? = null
) {
    val categoryList: List<Genre>
        get() = if (selectedContentType == ContentType.MOVIE) MOVIE_GENRES else TV_GENRES

    companion object {
        val MOVIE_GENRES = listOf(
            Genre(28, "Aksiyon"), Genre(878, "Bilim Kurgu"), Genre(35, "Komedi"),
            Genre(27, "Korku"), Genre(18, "Dram"), Genre(16, "Animasyon"), Genre(10749, "Romantik")
        )
        val TV_GENRES = listOf(
            Genre(10759, "Aksiyon & Macera"), Genre(10765, "Bilim Kurgu & Fantastik"),
            Genre(35, "Komedi"), Genre(18, "Dram"), Genre(99, "Belgesel"), Genre(80, "Suç"), Genre(16, "Animasyon")
        )
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadContent() }

    fun setContentType(type: ContentType) {
        if (_uiState.value.selectedContentType == type) return
        _uiState.update { it.copy(selectedContentType = type, selectedGenre = null) }
        loadContent()
    }

    fun loadContent() {
        if (_uiState.value.selectedGenre != null) {
            fetchByGenre(_uiState.value.selectedGenre!!)
        } else {
            fetchPopular()
        }
    }

    private fun fetchPopular() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val currentType = _uiState.value.selectedContentType
                val response = if (currentType == ContentType.MOVIE) {
                    repository.getPopularMovies()
                } else {
                    repository.getPopularTvShows()
                }
                val popularList = response.results ?: emptyList()

                val trendingResponse = if (currentType == ContentType.MOVIE) {
                    repository.getTrendingContent("movie")
                } else {
                    repository.getTrendingContent("tv")
                }
                val trendingList = trendingResponse.results ?: emptyList()

                _uiState.update {
                    it.copy(
                        movies = popularList,
                        trendingMovies = trendingList,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun fetchByGenre(genre: Genre) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, selectedGenre = genre, errorMessage = null) }
                val currentType = _uiState.value.selectedContentType
                val response = if (currentType == ContentType.MOVIE) {
                    repository.getMoviesByGenre(genre.id)
                } else {
                    repository.getTvShowsByGenre(genre.id)
                }
                _uiState.update {
                    it.copy(
                        movies = response.results ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}
