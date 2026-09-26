package com.huyarev.cinevia

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class DiaryEntry(
    @get:PropertyName("movieId") @set:PropertyName("movieId") var movieId: Int = 0,
    @get:PropertyName("id") @set:PropertyName("id") var firestoreId: String? = null,
    var title: String = "",
    var posterUrl: String = "",
    var status: String = "",
    var rating: Int = 0,
    var timestamp: Long = 0L,
    var myReview: String = ""
)
