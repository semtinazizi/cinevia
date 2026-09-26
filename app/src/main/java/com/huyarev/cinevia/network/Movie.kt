package com.huyarev.cinevia.network

import com.google.gson.annotations.SerializedName

// TMDB'den gelen toplu film listesi cevabını yakalayan sınıf
data class MovieResponse(
    val results: List<Movie>? = null
)

// Tek bir filmin veya dizinin özelliklerini tanımlayan asıl sınıfımız
data class Movie(
    val id: Int,
    val title: String? = null, // FİLMLER için başlık (Artık çökmemesi için null olabilir)
    val name: String? = null,  // DİZİLER için başlık (TMDB dizilerde name yollar)

    @SerializedName("poster_path")
    val posterPath: String?,

    // Puan değişkeni
    @SerializedName("vote_average")
    val voteAverage: Double? = null,

    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val first_air_date: String? = null,
    val job: String? = null
) {
    // TMDB sadece resmin adını yollar, biz onu tam bir web linkine çeviriyoruz
    val fullImageUrl: String
        get() = "https://image.tmdb.org/t/p/w500$posterPath"
}

// TMDB'den tekil film/dizi detayı çekerken kullanacağımız sınıf
data class MovieDetail(
    val id: Int,
    val title: String? = null, // Film adı
    val name: String? = null,  // Dizi adı
    val overview: String?,
    val tagline: String? = null,
    val runtime: Int? = null,
    val genres: List<GenreItem>? = null,

    @SerializedName("release_date") val releaseDate: String? = null, // Film çıkış tarihi
    @SerializedName("first_air_date") val first_air_date: String? = null, // Dizi çıkış tarihi

    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String? // Yatay geniş arka plan resmi
) {
    val posterUrl: String get() = "https://image.tmdb.org/t/p/w500$posterPath"
    val backdropUrl: String get() = "https://image.tmdb.org/t/p/w1280$backdropPath"
}

data class GenreItem(val id: Int, val name: String)
