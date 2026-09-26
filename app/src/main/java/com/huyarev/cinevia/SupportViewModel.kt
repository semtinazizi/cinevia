package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isSubmitting = mutableStateOf(false)
    val isSubmitting: State<Boolean> = _isSubmitting

    private val _isSuccess = mutableStateOf(false)
    val isSuccess: State<Boolean> = _isSuccess

    // YENİ: Hata veya Uyarı mesajlarını tutacak değişken
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun submitFeedback(category: String, message: String) {
        if (message.isBlank()) return
        _isSubmitting.value = true
        _errorMessage.value = null // Önceki uyarıları temizle

        val user = auth.currentUser
        val email = user?.email ?: "Bilinmeyen Kullanıcı"

        // 24 saat = 24 * 60 * 60 * 1000 milisaniye
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        // Önce kullanıcının attığı eski mesajları Firebase'den çek
        db.collection("feedbacks")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                var canSend = true

                if (!documents.isEmpty) {
                    // Firebase Index hatası almamak için en yeni mesajı Kotlin ile buluyoruz
                    val lastMessage = documents.documents.maxByOrNull { it.getLong("timestamp") ?: 0L }
                    val lastTimestamp = lastMessage?.getLong("timestamp") ?: 0L

                    // Eğer son mesajın üzerinden 24 saat geçmediyse engelle!
                    if (currentTime - lastTimestamp < oneDayInMillis) {
                        canSend = false
                    }
                }

                if (canSend) {
                    val feedback = hashMapOf(
                        "email" to email,
                        "category" to category,
                        "message" to message,
                        "timestamp" to currentTime,
                        "status" to "Bekliyor"
                    )

                    // İzin çıktıysa Firebase'e fırlat!
                    db.collection("feedbacks").add(feedback)
                        .addOnSuccessListener {
                            _isSubmitting.value = false
                            _isSuccess.value = true
                        }
                        .addOnFailureListener { e ->
                            _isSubmitting.value = false
                            _errorMessage.value = "Bir hata oluştu, tekrar dene."
                        }
                } else {
                    // 24 SAAT KURALINA TAKILDI!
                    _isSubmitting.value = false
                    _errorMessage.value = "Günde sadece 1 kez destek mesajı gönderebilirsin. Lütfen yarın tekrar dene!"
                }
            }
            .addOnFailureListener { e ->
                _isSubmitting.value = false
                _errorMessage.value = "Bağlantı hatası: ${e.message}"
            }
    }

    fun resetSuccess() {
        _isSuccess.value = false
        _errorMessage.value = null
    }
}
