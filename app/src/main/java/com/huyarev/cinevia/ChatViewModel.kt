package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huyarev.cinevia.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Özel Mesaj Şablonu
data class DirectMessage(
    val id: String = "",
    val senderEmail: String = "",
    val senderName: String = "",
    val receiverEmail: String = "",
    val receiverName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: SocialRepository
) : ViewModel() {

    private val _messages = mutableStateOf<List<DirectMessage>>(emptyList())
    val messages: State<List<DirectMessage>> = _messages

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // Mesajları Canlı Dinle
    fun loadMessages(targetUserEmail: String) {
        viewModelScope.launch {
            repository.getMessages(targetUserEmail).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    // SINIRSIZ MESAJ GÖNDER VE BİLDİRİM FIRLAT
    fun sendMessage(targetUserEmail: String, targetUserName: String, content: String) {
        repository.sendMessage(targetUserEmail, targetUserName, content)
    }
}
