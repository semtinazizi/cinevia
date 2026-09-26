package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    targetUserEmail: String,
    targetUserName: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages
    val errorMessage by viewModel.errorMessage
    var messageText by remember { mutableStateOf("") }

    val myEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    // Sayfa açıldığında mesajları yükle
    LaunchedEffect(targetUserEmail) {
        viewModel.loadMessages(targetUserEmail)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(targetUserName, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- SOHBET AKIŞI ---
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                reverseLayout = false // En eski mesaj üstte, yeniler altta
            ) {
                items(messages) { msg ->
                    val isMe = msg.senderEmail == myEmail
                    val timeString = SimpleDateFormat("HH:mm", Locale("tr")).format(Date(msg.timestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isMe) Color(0xFFE50914) else Color(0xFF1E1E1E), // Benim mesajım kırmızı, onunki gri
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.content, color = Color.White, fontSize = 15.sp)
                                Text(timeString, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            // --- 24 SAAT KURALI UYARISI ---
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFE50914),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }

            // --- MESAJ YAZMA ALANI ---
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    // YENİ METİN BURADA:
                    placeholder = { Text("Bir mesaj yaz...", color = Color.Gray, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                IconButton(
                    onClick = {
                        viewModel.sendMessage(targetUserEmail, targetUserName, messageText)
                        messageText = ""
                    },
                    modifier = Modifier.background(Color(0xFFE50914), CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Gönder", tint = Color.White)
                }
            }
        }
    }
}
