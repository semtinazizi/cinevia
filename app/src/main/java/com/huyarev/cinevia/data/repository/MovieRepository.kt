package com.huyarev.cinevia.data.repository

import com.huyarev.cinevia.data.local.dao.MovieDao
import com.huyarev.cinevia.data.local.entity.MovieEntity
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.network.MovieDetail
import com.huyarev.cinevia.network.MovieResponse
import com.huyarev.cinevia.network.TmdbApi
import com.huyarev.cinevia.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val movieDao: MovieDao,
    private val api: TmdbApi
) {
    private val API_KEY = BuildConfig.TMDB_API_KEY

    suspend fun getPopularMovies(): MovieResponse {
        return try {
            val response = api.getPopularMovies(API_KEY)
            val results = response.results ?: emptyList()
            val entities = results.map { it.toEntity("MOVIE") }
            movieDao.insertMovies(entities)
            response.copy(results = results)
        } catch (e: Exception) {
            val entities = movieDao.getMoviesByType("MOVIE")
            MovieResponse(results = entities.map { it.toMovie() })
        }
    }

    suspend fun getPopularTvShows(): MovieResponse {
        return try {
            val response = api.getPopularTvShows(API_KEY)
            val results = response.results ?: emptyList()
            val entities = results.map { it.toEntity("TV_SHOW") }
            movieDao.insertMovies(entities)
            response.copy(results = results)
        } catch (e: Exception) {
            val entities = movieDao.getMoviesByType("TV_SHOW")
            MovieResponse(results = entities.map { it.toMovie() })
        }
    }

    suspend fun getMoviesByGenre(genreId: Int): MovieResponse = api.getMoviesByGenre(API_KEY, genreId)
    suspend fun getTvShowsByGenre(genreId: Int): MovieResponse = api.getTvShowsByGenre(API_KEY, genreId)
    suspend fun getMovieDetails(movieId: Int): MovieDetail = api.getMovieDetails(movieId, API_KEY)
    suspend fun getTvDetails(movieId: Int): MovieDetail = api.getTvDetails(movieId, API_KEY)
    suspend fun getMovieVideos(movieId: Int): TmdbApi.VideoResponse = api.getMovieVideos(movieId, API_KEY)
    suspend fun getTvVideos(movieId: Int): TmdbApi.VideoResponse = api.getTvVideos(movieId, API_KEY)
    suspend fun getMovieCredits(movieId: Int): TmdbApi.CreditsResponse = api.getMovieCredits(movieId, API_KEY)
    suspend fun getTvCredits(seriesId: Int): TmdbApi.CreditsResponse = api.getTvCredits(seriesId, API_KEY)
    suspend fun getSimilarMovies(movieId: Int): MovieResponse = api.getSimilarMovies(movieId, API_KEY)
    suspend fun getSimilarTvShows(seriesId: Int): MovieResponse = api.getSimilarTvShows(seriesId, API_KEY)
    suspend fun searchMulti(query: String): MovieResponse = api.searchMulti(API_KEY, query)
    suspend fun getTrendingContent(mediaType: String): MovieResponse = api.getTrendingContent(mediaType, API_KEY)

    private fun Movie.toEntity(type: String): MovieEntity = MovieEntity(
        id = id,
        title = title,
        name = name,
        posterPath = posterPath,
        releaseDate = null, // Movie class doesn't have it
        firstAirDate = null, // Movie class doesn't have it
        voteAverage = voteAverage,
        type = type
    )

    private fun MovieEntity.toMovie(): Movie = Movie(
        id = id,
        title = title,
        name = name,
        posterPath = posterPath,
        voteAverage = voteAverage
    )
}
