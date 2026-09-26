package com.huyarev.cinevia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary")
data class DiaryEntity(
    @PrimaryKey val movieId: Int,
    val firestoreId: String?,
    val title: String,
    val posterUrl: String,
    val status: String,
    val rating: Int,
    val timestamp: Long,
    val myReview: String
)
