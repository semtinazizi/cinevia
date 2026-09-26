package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    // Ekranda göstereceğimiz hata veya başarı mesajları
    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    private fun getErrorMessage(exception: Exception?): String {
        return when (exception) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Kullanıcı bulunamadı."
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "E-posta veya şifre hatalı."
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Bu e-posta adresi zaten kullanılıyor."
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Şifre çok zayıf (en az 6 karakter olmalı)."
            else -> exception?.message ?: "Bilinmeyen bir hata oluştu."
        }
    }

    // Kayıt Olma Fonksiyonu
    fun signUp(email: String, password: String, onAuthSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                    onAuthSuccess() // Başarılıysa yönlendirme yap
                } else {
                    _authState.value = AuthState.Error(getErrorMessage(task.exception))
                }
            }
    }

    // Giriş Yapma Fonksiyonu
    fun signIn(email: String, password: String, onAuthSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                    onAuthSuccess() // Başarılıysa yönlendirme yap
                } else {
                    _authState.value = AuthState.Error(getErrorMessage(task.exception))
                }
            }
    }

    // Şifre Sıfırlama
    fun resetPassword(email: String, onResult: (String) -> Unit) {
        if (email.isBlank()) {
            onResult("Lütfen e-posta adresinizi girin.")
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult("Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.")
                } else {
                    onResult(getErrorMessage(task.exception))
                }
            }
    }
}

// Durumları takip etmek için basit bir yapı
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
