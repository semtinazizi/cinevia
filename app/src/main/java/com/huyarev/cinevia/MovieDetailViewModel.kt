package com.huyarev.cinevia

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.huyarev.cinevia.data.repository.DiaryRepository
import com.huyarev.cinevia.data.repository.MovieRepository
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.network.MovieDetail
import com.huyarev.cinevia.network.TmdbApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val movieDetail: MovieDetail? = null,
    val isLoading: Boolean = true,
    val trailerKey: String? = null,
    val cast: List<TmdbApi.CastMember> = emptyList(),
    val similarMovies: List<Movie> = emptyList(),
    val translatedTitle: String? = null,
    val isTranslating: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val diaryRepository: DiaryRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    fun fetchDetails(movieId: Int, isTvShow: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, translatedTitle = null, errorMessage = null) }

            try {
                val detailResponse = if (isTvShow) {
                    movieRepository.getTvDetails(movieId)
                } else {
                    movieRepository.getMovieDetails(movieId)
                }

                val videoResponse = if (isTvShow) {
                    movieRepository.getTvVideos(movieId)
                } else {
                    movieRepository.getMovieVideos(movieId)
                }

                val youtubeVideos = videoResponse.results.filter { it.site == "YouTube" }
                val trTrailer = youtubeVideos.find { it.type == "Trailer" && it.iso_639_1 == "tr" }?.key
                val enTrailer = youtubeVideos.find { it.type == "Trailer" && it.iso_639_1 == "en" }?.key
                val trTeaser = youtubeVideos.find { it.type == "Teaser" && it.iso_639_1 == "tr" }?.key
                val enTeaser = youtubeVideos.find { it.type == "Teaser" && it.iso_639_1 == "en" }?.key

                val key = trTrailer ?: enTrailer ?: trTeaser ?: enTeaser ?: youtubeVideos.firstOrNull()?.key

                val creditsResponse = try {
                    if (isTvShow) {
                        movieRepository.getTvCredits(movieId)
                    } else {
                        movieRepository.getMovieCredits(movieId)
                    }
                } catch (e: Exception) {
                    TmdbApi.CreditsResponse(emptyList(), emptyList())
                }

                val similarResponse = try {
                    if (isTvShow) {
                        movieRepository.getSimilarTvShows(movieId)
                    } else {
                        movieRepository.getSimilarMovies(movieId)
                    }
                } catch (e: Exception) {
                    null
                }

                _uiState.update {
                    it.copy(
                        movieDetail = detailResponse,
                        trailerKey = key,
                        cast = creditsResponse.cast,
                        similarMovies = similarResponse?.results ?: emptyList(),
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // --- GÜNLÜĞE / LİSTEYE FİLM EKLEME MOTORU ---
    fun addToDiary(
        status: String, 
        context: Context, 
        rating: Int = 0, 
        review: String = "", 
        shareToSocial: Boolean = false,
        feedViewModel: FeedViewModel? = null
    ) {
        val user = auth.currentUser
        val movie = _uiState.value.movieDetail

        if (user == null || movie == null) {
            Toast.makeText(context, "Oturum veya film bilgisi bulunamadı!", Toast.LENGTH_SHORT).show()
            return
        }

        val title = movie.title ?: movie.name ?: "İsimsiz Film"
        val posterUrl = movie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""

        diaryRepository.addToDiary(
            movieId = movie.id,
            title = title,
            posterUrl = posterUrl,
            status = status,
            rating = rating,
            review = review,
            onSuccess = {
                Toast.makeText(context, "'$title', $status listesine eklendi!", Toast.LENGTH_SHORT).show()
                if (shareToSocial && review.isNotBlank() && feedViewModel != null) {
                    feedViewModel.sendPost(
                        content = review,
                        movieTitle = title,
                        moviePosterUrl = posterUrl,
                        videoUri = null,
                        isSpoiler = false,
                        movieId = movie.id,
                        rating = rating,
                        context = context,
                        onComplete = {}
                    )
                }
            },
            onFailure = {
                Toast.makeText(context, "Listeye eklenirken hata oluştu.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- YAPAY ZEKA ÇEVİRİ MOTORU ---
    fun translateTitle(originalText: String) {
        _uiState.update { it.copy(isTranslating = true) }
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(originalText)
            .addOnSuccessListener { languageCode ->
                if (languageCode != "und" && languageCode != "tr") {
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(languageCode)
                        .setTargetLanguage(TranslateLanguage.TURKISH)
                        .build()
                    val translator = Translation.getClient(options)
                    translator.downloadModelIfNeeded().addOnSuccessListener {
                        translator.translate(originalText).addOnSuccessListener { translatedText ->
                            _uiState.update { it.copy(translatedTitle = translatedText, isTranslating = false) }
                            translator.close()
                        }.addOnFailureListener {
                            _uiState.update { it.copy(isTranslating = false) }
                            translator.close()
                        }
                    }.addOnFailureListener {
                        _uiState.update { it.copy(isTranslating = false) }
                        translator.close()
                    }
                } else {
                    _uiState.update { it.copy(isTranslating = false) }
                }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(isTranslating = false) }
            }
    }
}
