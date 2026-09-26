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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit, // YENİ: Yönlendirme komutu
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bildirimler", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(Icons.Default.Check, contentDescription = "Tümünü Okundu İşaretle", tint = Color(0xFFE50914))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Henüz bir bildirim yok.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notif ->
                    NotificationItemRow(notif = notif, onClick = {
                        viewModel.markAsRead(notif.id) // Önce okundu yap
                        onNotificationClick(notif)     // Sonra ilgili sayfaya yönlendir!
                    })
                }
            }
        }
    }
}

@Composable
fun NotificationItemRow(notif: NotificationItem, onClick: () -> Unit) {
    val backgroundColor = if (notif.isRead) Color(0xFF1E1E1E) else Color(0xFF2A1A1A)

    val icon = when (notif.type) {
        "follow" -> Icons.Default.PersonAdd
        "group_invite" -> Icons.Default.GroupAdd
        "like" -> Icons.Default.Favorite
        else -> Icons.Default.Notifications
    }

    val iconTint = if (notif.isRead) Color.Gray else Color(0xFFE50914)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(Color(0xFF121212), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${notif.fromName} ${notif.message}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            val sdf = SimpleDateFormat("dd MMM HH:mm", Locale("tr"))
            val date = Date(notif.timestamp)
            Text(text = sdf.format(date), color = Color.Gray, fontSize = 12.sp)
        }

        if (!notif.isRead) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFE50914), CircleShape))
        }
    }
}
