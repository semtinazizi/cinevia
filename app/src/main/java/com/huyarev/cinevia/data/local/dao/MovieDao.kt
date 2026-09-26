package com.huyarev.cinevia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.huyarev.cinevia.data.local.entity.MovieEntity

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE type = :type")
    suspend fun getMoviesByType(type: String): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("DELETE FROM movies WHERE type = :type")
    suspend fun deleteMoviesByType(type: String)
}
