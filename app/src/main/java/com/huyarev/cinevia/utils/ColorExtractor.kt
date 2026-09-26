package com.huyarev.cinevia.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getDominantColorFromUrl(context: Context, url: String?): Color {
    if (url == null) return Color(0xFF121212) // Koyu varsayılan
    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Palette için hardware bitmap kullanılamaz
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = result?.toBitmap() ?: return@withContext Color(0xFF121212)
            
            val palette = Palette.from(bitmap).generate()
            // Koyu veya canlı bir renk seç, bulamazsa siyah ver
            val dominantColor = palette.darkMutedSwatch?.rgb ?: palette.darkVibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
            
            if (dominantColor != null) {
                Color(dominantColor)
            } else {
                Color(0xFF121212)
            }
        } catch (e: Exception) {
            Color(0xFF121212)
        }
    }
}
