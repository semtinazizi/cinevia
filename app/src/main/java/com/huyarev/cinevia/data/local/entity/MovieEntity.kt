package com.huyarev.cinevia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String?,
    val name: String?,
    val posterPath: String?,
    val releaseDate: String?,
    val firstAirDate: String?,
    val voteAverage: Double?,
    val type: String // "MOVIE" or "TV_SHOW"
)
