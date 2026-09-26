package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    groupName: String,
    onBackClick: () -> Unit,
    viewModel: GroupDetailViewModel = viewModel()
) {
    val messages by viewModel.messages
    var messageText by remember { mutableStateOf("") }

    val isAdmin by viewModel.isAdmin
    val isMember by viewModel.isMember

    var showDeleteDialog by remember { mutableStateOf(false) }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberEmail by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val searchResults by viewModel.searchResults
    val context = LocalContext.current

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showAddMemberDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Üye Ekle", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Grubu Sil", tint = Color(0xFFE50914))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        bottomBar = {
            if (isMember) {
                // SADECE ÜYELER İÇİN MESAJ KUTUSU
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Filme dair bir şeyler yaz...", color = Color.Gray) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier.background(Color(0xFFE50914), CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Gönder", tint = Color.White)
                    }
                }
            } else {
                // ÜYE OLMAYANLAR İÇİN UYARI MESAJI (Kendi kendine katılma kaldırıldı)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒 Bu grup sadece üyelere özeldir. Mesaj yazmak için yönetici tarafından eklenmeniz gerekir.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            items(messages) { msg ->
                val isMe = viewModel.isCurrentUser(msg.sender)
                MessageBubble(sender = if(isMe) "Sen" else msg.sender, message = msg.message, isMe = isMe)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddMemberDialog = false
                viewModel.clearSearchResults()
            },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Yeni Üye Ekle", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Kullanıcı adı yazarak arayın veya direkt e-posta girin.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newMemberEmail,
                        onValueChange = {
                            newMemberEmail = it
                            viewModel.searchUsers(it)
                        },
                        label = { Text("İsim veya E-posta", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                            items(searchResults) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.addMember(user.email) { sonucMesaji ->
                                                toastMessage = sonucMesaji
                                                showAddMemberDialog = false
                                                newMemberEmail = ""
                                                viewModel.clearSearchResults()
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(40.dp).background(Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) {
                                        if (user.profileImageUrl != null) {
                                            AsyncImage(model = user.profileImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMember(newMemberEmail) { sonucMesaji ->
                            toastMessage = sonucMesaji
                            showAddMemberDialog = false
                            newMemberEmail = ""
                            viewModel.clearSearchResults()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Davet Et", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddMemberDialog = false
                    newMemberEmail = ""
                    viewModel.clearSearchResults()
                }) {
                    Text("İptal", color = Color.Gray)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Grubu Sil", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Bu grubu kalıcı olarak silmek istediğine emin misin? Tüm mesajlar silinecektir.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup {
                            showDeleteDialog = false
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Evet, Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun MessageBubble(sender: String, message: String, isMe: Boolean) {
    val align = if (isMe) Alignment.End else Alignment.Start
    val bgColor = if (isMe) Color(0xFFE50914) else Color(0xFF1E1E1E)
    val textColor = Color.White

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = align) {
        Text(text = sender, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp))
        Box(
            modifier = Modifier.background(bgColor, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text = message, color = textColor, fontSize = 14.sp)
        }
    }
}
