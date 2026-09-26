package com.huyarev.cinevia.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): MovieResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): MovieDetail
    // Kategoriye (Türe) göre film çeken komut
    @GET("discover/movie")
    suspend fun getMoviesByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: Int, // Tür ID'si (Örn: Aksiyon=28)
        @Query("language") language: String = "tr-TR"
    ): MovieResponse
    // Kullanıcının yazdığı kelimeye göre film arama komutu
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String, // Aranacak kelime
        @Query("language") language: String = "tr-TR"
    ): MovieResponse

    // YENİ: Hem dizi hem film arayan motor
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "tr-TR"
    ): MovieResponse // (Senin projendeki Response sınıfının adı neyse onu yaz, örn: MovieResponse)

    // YENİ: Haftalık trend olan içerikleri çekme (Film veya Dizi)
    @GET("trending/{media_type}/week")
    suspend fun getTrendingContent(
        @Path("media_type") mediaType: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): MovieResponse

    // 1. Popüler Dizileri Çekme
    @GET("tv/popular")
    suspend fun getPopularTvShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): MovieResponse

    // 2. Kategoriye Göre Dizi Çekme
    @GET("discover/tv")
    suspend fun getTvShowsByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: Int,
        @Query("language") language: String = "tr-TR"
    ): MovieResponse

    // 3. Dizi Detaylarını Çekme
    @GET("tv/{series_id}")
    suspend fun getTvDetails(
        @Path("series_id") seriesId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): MovieDetail

    // 4. Dizi Fragmanlarını Çekme
    @GET("tv/{series_id}/videos")
    suspend fun getTvVideos(
        @Path("series_id") seriesId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("include_video_language") includeVideoLangs: String = "tr,en,null"
    ): VideoResponse

    // Fragman verilerini karşılayan sınıflar (iso_639_1 eklendi!)
    data class VideoResponse(val results: List<VideoResult>)
    data class VideoResult(val key: String, val site: String, val type: String, val iso_639_1: String?)

    // Fragmanı çeken API komutu (Sihirli parametre eklendi)
    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR",
        @Query("include_video_language") includeVideoLangs: String = "tr,en,null"
    ): VideoResponse

    // Kadro (Cast & Crew) API'ları
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): CreditsResponse

    @GET("tv/{series_id}/credits")
    suspend fun getTvCredits(
        @Path("series_id") seriesId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): CreditsResponse

    // Benzer İçerikler (Similar Movies & TV)
    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): MovieResponse

    @GET("tv/{series_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("series_id") seriesId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): MovieResponse

    data class CreditsResponse(val cast: List<CastMember>, val crew: List<CrewMember>)
    data class CastMember(val id: Int, val name: String, val character: String?, val profile_path: String?)
    data class CrewMember(val id: Int, val name: String, val job: String?, val profile_path: String?)

    // --- YENİ: RADAR (YÖNETMEN VE OYUNCU TAKİBİ) API'LARI ---
    @GET("search/person")
    suspend fun searchPerson(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "tr-TR"
    ): PersonResponse

    @GET("person/{person_id}/movie_credits")
    suspend fun getPersonMovies(
        @Path("person_id") personId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): PersonMovieCreditsResponse

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "tr-TR"
    ): PersonDetail

    // Radar modelleri
    data class PersonResponse(val results: List<Person>)
    data class Person(val id: Int, val name: String, val profile_path: String?, val known_for_department: String?)
    data class PersonDetail(val id: Int, val name: String, val biography: String?, val birthday: String?, val place_of_birth: String?, val known_for_department: String?, val profile_path: String?)
    data class PersonMovieCreditsResponse(val cast: List<Movie>, val crew: List<Movie>)
}

object RetrofitInstance {
    val api: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }
}
