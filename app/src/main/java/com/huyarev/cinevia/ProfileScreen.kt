package com.huyarev.cinevia

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.MoreVert // YENİ EKLENDİ
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.huyarev.cinevia.ui.theme.*
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.huyarev.cinevia.navigation.FollowListRoute
import com.huyarev.cinevia.navigation.SocialRoute
import com.huyarev.cinevia.navigation.SupportRoute

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    feedViewModel: FeedViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val userEmail = user?.email ?: "Bilinmeyen Kullanıcı"

    val myPosts by feedViewModel.userPosts
    val diaryMovies by viewModel.diaryMovies

    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val currentUserName by viewModel.currentUserName
    val currentUserBio by viewModel.currentUserBio
    val myFollowers by viewModel.currentUserFollowers
    val myFollowing by viewModel.currentUserFollowing

    var showEditDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) } // YENİ: Üç Nokta Menüsü için

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var localImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserProfile()
        viewModel.listenToMyFollowStats()
    }

    val profileImageUrlRaw by viewModel.currentUserProfileImage
    val profileImageUrl = remember(profileImageUrlRaw) { feedViewModel.getSignedUrl(profileImageUrlRaw) }
    val isUploading by feedViewModel.isUploading

    var showImageMenu by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profileImageUrlRaw) {
        localImageUri = null
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            localImageUri = it
            feedViewModel.uploadProfileImage(it, context)
        }
    }

    LaunchedEffect(userEmail) {
        feedViewModel.fetchUserPosts(userEmail)
    }

    // --- SİNEMA KARNESİ HESAPLAMALARI ---
    val watchedCount = diaryMovies.count { it.status == "İzledim" }
    val toWatchCount = diaryMovies.count { it.status == "İzleyeceğim" }
    val likedCount = diaryMovies.count { it.status == "Beğendiklerim" || (it.status == "İzledim" && it.rating >= 4) }

    if (showFullScreenImage != null) {
        Dialog(
            onDismissRequest = { showFullScreenImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { showFullScreenImage = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = showFullScreenImage, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                IconButton(onClick = { showFullScreenImage = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
            }
        }
    }

    if (showImageMenu) {
        AlertDialog(
            onDismissRequest = { showImageMenu = false },
            containerColor = SurfaceDark,
            title = { Text("Profil Fotoğrafı", color = Color.White) },
            text = {
                Column {
                    if (profileImageUrl != null) {
                        TextButton(onClick = { showImageMenu = false; showFullScreenImage = profileImageUrl }) { Text("Fotoğrafı Görüntüle", color = Color.White, fontSize = 16.sp) }
                        TextButton(onClick = { showImageMenu = false; feedViewModel.deleteProfileImage(context) }) { Text("Fotoğrafı Sil", color = CinemaRed, fontSize = 16.sp) }
                    }
                    TextButton(onClick = { showImageMenu = false; imagePickerLauncher.launch("image/*") }) { Text("Yeni Fotoğraf Yükle", color = Color.White, fontSize = 16.sp) }
                }
            },
            confirmButton = { TextButton(onClick = { showImageMenu = false }) { Text("İptal", color = Color.Gray) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DarkBackground),
            contentPadding = PaddingValues(bottom = 100.dp) // Alt menünün altında kalmasın diye
        ) {
            // --- 1. ÜST KISIM (KULLANICI KİMLİĞİ VE AYARLAR MENÜSÜ) ---
            item {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).padding(top = 16.dp, bottom = 24.dp)) {

                // YENİ: SAĞ ÜST KÖŞE ÜÇ NOKTA (AYARLAR) MENÜSÜ
                Box(modifier = Modifier.fillMaxWidth().padding(end = 16.dp), contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = { showSettingsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Ayarlar", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false },
                        modifier = Modifier.background(Color(0xFF2A2A2A))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Destek ve Bildirim Formu", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.White) },
                            onClick = { showSettingsMenu = false; navController.navigate(SupportRoute) }
                        )
                        DropdownMenuItem(
                            text = { Text("Oturumu Kapat", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Gray) },
                            onClick = { showSettingsMenu = false; FirebaseAuth.getInstance().signOut() }
                        )
                        DropdownMenuItem(
                            text = { Text("Hesabı Sil", color = CinemaRed) },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CinemaRed) },
                            onClick = { showSettingsMenu = false; showDeleteAccountDialog = true }
                        )
                    }
                }

                // Profil Resmi ve Bilgiler
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(100.dp).background(SurfaceDark, CircleShape).clickable { showImageMenu = true }, contentAlignment = Alignment.Center) {
                        if (localImageUri != null) {
                            AsyncImage(
                                model = localImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!profileImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                                onError = { error ->
                                    val errorMessage = error.result.throwable.message
                                    val requestUrl = error.result.request.data
                                    android.util.Log.e("ProfileScreen", "Profile Image Load Failed: $errorMessage - URL: $requestUrl")
                                }
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
                        }
                        if (isUploading) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CinemaRed, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = currentUserName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = userEmail, color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = currentUserBio, color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    
                    // --- YENİ: ROZETLER SİSTEMİ ---
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val reviewCount = diaryMovies.count { it.myReview.isNotBlank() }
                        if (watchedCount >= 50) BadgeChip(icon = "🎬", text = "Sinefil")
                        if (likedCount >= 20) BadgeChip(icon = "🌟", text = "Seçici")
                        if (reviewCount >= 10) BadgeChip(icon = "✍️", text = "Eleştirmen")
                        if (watchedCount >= 100) BadgeChip(icon = "🏆", text = "Efsane")
                        if (watchedCount < 50 && likedCount < 20 && reviewCount < 10) {
                             BadgeChip(icon = "🍿", text = "Çaylak")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { navController.navigate(FollowListRoute("Takipçiler", userEmail)) }) {
                            Text(text = myFollowers.size.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Takipçi", color = Color.Gray, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { navController.navigate(FollowListRoute("Takip Edilenler", userEmail)) }) {
                            Text(text = myFollowing.size.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Takip", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showEditDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)), shape = RoundedCornerShape(16.dp)) {
                        Text("Profili Düzenle", color = Color.White)
                    }
                }
            }
        }

        // --- 2. SİNEMA İSTATİSTİKLERİ KARTLARI ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Sinema Karnem", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // İzlenenler
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalMovies, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = watchedCount.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "İzlenen", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                // Beğenilenler
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = likedCount.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Beğenilen", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                // Sırada
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MovieFilter, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = toWatchCount.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Sırada", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- 3. PAYLAŞIMLARIM (AŞAĞI DOĞRU KAYDIRMALI GALERİ) ---
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Gönderilerim (${myPosts.size})", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(12.dp))

            if (myPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Henüz bir video veya film alıntısı paylaşmadınız.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    myPosts.chunked(3).forEach { rowPosts ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPosts.forEach { post ->
                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E1E1E)).clickable { navController.navigate(SocialRoute) }
                                ) {
                                    if (post.moviePosterUrl != null) {
                                        AsyncImage(model = post.moviePosterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                    if (post.videoUrl != null) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.Center).size(36.dp))
                                    }
                                }
                            }
                            repeat(3 - rowPosts.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        // NOT: Eski kocaman "Hesap ve Ayarlar" kısmı buradan tamamen silindi! Menüye taşındı.
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF2A2A2A),
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = currentUserName, currentBio = currentUserBio, onDismiss = { showEditDialog = false },
            onSave = { newName, newBio -> viewModel.updateProfileInfo(newName, newBio) { showEditDialog = false } }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false }, containerColor = SurfaceDark,
            title = { Text("Hesabı Sil", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Bu işlem geri alınamaz. Profiliniz, günlüğünüz ve veritabanı kaydınız kalıcı olarak silinecektir. Emin misiniz?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        val email = user?.email
                        showDeleteAccountDialog = false
                        if (email != null) com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(email).delete()
                        user?.delete()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) { FirebaseAuth.getInstance().signOut() }
                            else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Güvenlik önlemi: Hesabınızı kalıcı olarak silebilmek için oturumu kapatıp tekrar giriş yapmalısınız.") }
                                FirebaseAuth.getInstance().signOut()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
                ) { Text("Evet, Sil", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountDialog = false }) { Text("İptal", color = Color.Gray) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(currentName: String, currentBio: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF1E1E1E),
        title = { Text("Profili Düzenle", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Görünür İsim", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914)), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Kısa Biyografi", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914)), maxLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(name, bio) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Kaydet", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = Color.Gray) } }
    )
}

@Composable
fun BadgeChip(icon: String, text: String) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .background(Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = Color(0xFFFFC107), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
