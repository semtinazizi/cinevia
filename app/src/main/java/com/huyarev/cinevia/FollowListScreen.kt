package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    title: String,
    ownerEmail: String, // DÜZELTME: Artık sadece sayfa sahibinin emailini alıyoruz
    onUserClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    feedViewModel: FeedViewModel = viewModel()
) {
    val users by viewModel.followListData

    // Sayfa açıldığında ViewModel'e "Verileri Çek" emrini veriyoruz
    LaunchedEffect(ownerEmail, title) {
        viewModel.fetchFollowList(ownerEmail, title == "Takipçiler")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Liste boş veya yükleniyor...", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(users) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick(user.email) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(50.dp).background(Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) {
                            val profileImageUrl = remember(user.profileImageUrl) { feedViewModel.getSignedUrl(user.profileImageUrl) }
                            if (profileImageUrl != null) {
                                AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onError = { error ->
                                        android.util.Log.e("FollowListScreen", "Image Load Failed for ${user.email}: ${error.result.throwable.message} - URL: ${error.result.request.data}")
                                    }
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = user.name ?: user.email.substringBefore("@").replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1E1E1E), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
