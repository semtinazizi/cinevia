package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

import com.huyarev.cinevia.navigation.ChatRoute
import com.huyarev.cinevia.navigation.FollowListRoute

@Composable
fun OtherProfileScreen(
    navController: NavController,
    targetEmail: String,
    profileViewModel: ProfileViewModel = viewModel(),
    feedViewModel: FeedViewModel = viewModel()
) {
    val targetName by profileViewModel.targetName
    val targetBio by profileViewModel.targetBio

    val followerCount by profileViewModel.targetFollowerCount
    val followingCount by profileViewModel.targetFollowingCount
    val isFollowing by profileViewModel.isFollowing
    val profileImageUrlRaw by profileViewModel.targetProfileImage
    val profileImageUrl = remember(profileImageUrlRaw) { feedViewModel.getSignedUrl(profileImageUrlRaw) }
    val targetPosts by feedViewModel.userPosts

    var selectedPost by remember { mutableStateOf<SocialPost?>(null) }

    // YENİ: Başkasının profil resmini büyütmek için hafıza
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetEmail) {
        profileViewModel.loadTargetUserProfile(targetEmail)
        feedViewModel.fetchUserPosts(targetEmail)
    }

    fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
            count >= 1000 -> String.format("%.1fK", count / 1000.0)
            else -> count.toString()
        }
    }

    // YENİ: Başkasının Profil Resmine Tıklanınca Açılan Tam Ekran Penceresi
    if (showFullScreenImage != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullScreenImage = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { showFullScreenImage = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = showFullScreenImage, contentDescription = null,
                    modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit
                )
                IconButton(onClick = { showFullScreenImage = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = targetName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {

            // YENİ: Tıklanabilir (Clickable) Başkası Profil Resmi
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1E1E1E), CircleShape)
                    .clickable { if (profileImageUrl != null) showFullScreenImage = profileImageUrl },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl != null) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = { error ->
                            android.util.Log.e("OtherProfileScreen", "Image Load Failed for $targetEmail: ${error.result.throwable.message} - URL: ${error.result.request.data}")
                        }
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(modifier = Modifier.width(24.dp))

            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatColumn(label = "Gönderi", count = formatCount(targetPosts.size))

                StatColumn(
                    label = "Takipçi",
                    count = formatCount(followerCount),
                    onClick = { navController.navigate(FollowListRoute("Takipçiler", targetEmail)) }
                )
                StatColumn(
                    label = "Takip",
                    count = formatCount(followingCount),
                    onClick = { navController.navigate(FollowListRoute("Takip Edilenler", targetEmail)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = targetBio,
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { profileViewModel.toggleFollow(targetEmail) },
                colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) Color(0xFF2A2A2A) else Color(0xFFE50914)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFollowing) "Takipten Çıkar" else "Takip Et", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { navController.navigate(ChatRoute(targetEmail, targetName)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Message, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mesaj", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.DarkGray)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(targetPosts) { post ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(Color(0xFF1E1E1E))
                        .clickable { selectedPost = post }
                ) {
                    if (post.moviePosterUrl != null) {
                        AsyncImage(model = post.moviePosterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    if (post.videoUrl != null) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.Center).size(32.dp))
                    }
                }
            }
        }
    }

    if (selectedPost != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedPost = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { selectedPost = null },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.clickable(enabled = false) {}) {
                    PostCard(
                        post = selectedPost!!,
                        viewModel = feedViewModel,
                        onAvatarClick = { }
                    )
                }
                IconButton(
                    onClick = { selectedPost = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatColumn(label: String, count: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(text = count, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}
