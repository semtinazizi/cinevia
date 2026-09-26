package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete // YENİ EKLENDİ
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onBackClick: () -> Unit,
    onChatClick: (String, String) -> Unit,
    viewModel: InboxViewModel = viewModel()
) {
    val inboxItems by viewModel.inboxItems

    // YENİ: Silinecek sohbeti hafızada tutan değişken
    var chatToDelete by remember { mutableStateOf<ChatInboxItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesajlar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (inboxItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Henüz bir mesajın yok.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(inboxItems) { item ->
                    val timeString = SimpleDateFormat("HH:mm", Locale("tr")).format(Date(item.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatClick(item.targetEmail, item.targetName) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profil İkonu
                        Box(modifier = Modifier.size(50.dp).background(Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(16.dp))

                        // İsim ve Son Mesaj
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.targetName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = item.lastMessage, color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Zaman
                        Text(text = timeString, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))

                        // YENİ: ÇÖP KUTUSU BUTONU
                        IconButton(
                            onClick = { chatToDelete = item },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.DarkGray)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1E1E1E))
                }
            }
        }
    }

    // YENİ: SİLME ONAY DİALOGU
    if (chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Sohbeti Sil", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("${chatToDelete?.targetName} ile olan sohbeti listeden kaldırmak istediğine emin misin?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChat(chatToDelete!!.chatId)
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) { Text("İptal", color = Color.Gray) }
            }
        )
    }
}
