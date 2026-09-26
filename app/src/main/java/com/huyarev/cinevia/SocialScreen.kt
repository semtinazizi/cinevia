package com.huyarev.cinevia

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.utils.parseMarkdown
import java.text.SimpleDateFormat
import java.util.*

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SocialScreen(
    onGroupClick: (String, String) -> Unit,
    onUserClick: (String) -> Unit,
    onInboxClick: () -> Unit,
    feedViewModel: FeedViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Film Akışı", "Gruplar")

    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    val notifications by notificationViewModel.notifications
    val unreadCount by notificationViewModel.unreadCount
    var showNotificationsDialog by remember { mutableStateOf(false) }

    if (fullScreenImage != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullScreenImage = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { fullScreenImage = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = fullScreenImage, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                IconButton(onClick = { fullScreenImage = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
            }
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildirimler", color = Color.White, fontWeight = FontWeight.Bold)
                    if (unreadCount > 0) {
                        TextButton(onClick = { notificationViewModel.markAllAsRead() }) { Text("Tümünü Okundu İşaretle", color = Color(0xFFE50914), fontSize = 12.sp) }
                    }
                }
            },
            text = {
                if (notifications.isEmpty()) {
                    Text("Henüz bir bildiriminiz yok.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(notifications) { notif ->
                            val bgColor = if (notif.isRead) Color.Transparent else Color(0xFF2A2A2A)
                            Row(modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).clickable { notificationViewModel.markAsRead(notif.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(20.dp)) }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append(notif.fromName) }
                                        withStyle(style = SpanStyle(color = Color.LightGray)) { append(" ${notif.message}") }
                                    }, fontSize = 14.sp)
                                    Text(text = SimpleDateFormat("dd MMM HH:mm", Locale("tr")).format(Date(notif.timestamp)), color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotificationsDialog = false }) { Text("Kapat", color = Color.Gray) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CineRev Sosyal", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onInboxClick) { Icon(Icons.Default.Email, contentDescription = "Mesajlar", tint = Color.White) }
                Spacer(modifier = Modifier.width(8.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { showNotificationsDialog = true }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Bildirimler", tint = Color.White, modifier = Modifier.size(28.dp))
                    if (unreadCount > 0) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(18.dp).background(Color(0xFFE50914), CircleShape), contentAlignment = Alignment.Center) {
                            Text(text = unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTabIndex, containerColor = Color(0xFF1E1E1E), contentColor = Color(0xFFE50914), divider = { HorizontalDivider(color = Color.DarkGray) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(text = title, color = if (selectedTabIndex == index) Color(0xFFE50914) else Color.Gray, fontWeight = FontWeight.Bold) })
            }
        }

        when (selectedTabIndex) {
            0 -> FeedContent(viewModel = feedViewModel, onAvatarClick = { email -> onUserClick(email) })
            1 -> GroupsContent(viewModel = socialViewModel, onGroupClick = onGroupClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(viewModel: FeedViewModel, onAvatarClick: (String) -> Unit) {
    val posts by viewModel.posts
    var showPostDialog by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            isRefreshing = true
            kotlinx.coroutines.delay(1000)
            isRefreshing = false
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showPostDialog = true }, containerColor = Color(0xFFE50914), contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Gönderi")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) {
                items(posts) { post -> PostCard(post, viewModel, onAvatarClick) }
            }
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFFE50914)
            )
        }
    }

    if (showPostDialog) { CreatePostDialog(viewModel = viewModel, onDismiss = { showPostDialog = false }) }
}

@Composable
fun PostCard(post: SocialPost, viewModel: FeedViewModel, onAvatarClick: (String) -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val myEmail = remember { auth.currentUser?.email?.lowercase() }
    val isMyPost = myEmail == post.senderEmail
    val timeString = SimpleDateFormat("HH:mm - dd MMM", Locale("tr")).format(Date(post.timestamp))

    var expandedMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val dynamicImagesMap by viewModel.dynamicUserImages
    val tazeResimUrl = dynamicImagesMap[post.senderEmail.lowercase().trim()]
    
    val rawAvatarUrl = if (!tazeResimUrl.isNullOrBlank() && tazeResimUrl != "no_image" && tazeResimUrl != "pending") {
        tazeResimUrl
    } else {
        post.senderProfileImageUrl
    }
    val dynamicAvatarUrl = remember(rawAvatarUrl) { viewModel.getSignedUrl(rawAvatarUrl) }

    LaunchedEffect(post.senderEmail) {
        viewModel.fetchDynamicProfileImage(post.senderEmail)
    }

    var showHeart by remember { mutableStateOf(false) }
    var showSpoilerContent by remember { mutableStateOf(!post.isSpoiler) }
    
    val scale by animateFloatAsState(
        targetValue = if (showHeart) 1.5f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        finishedListener = { if (it == 1.5f) showHeart = false }
    )

    Box(contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!post.likedBy.contains(myEmail)) {
                                viewModel.toggleLike(post)
                            }
                            showHeart = true
                        }
                    )
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {

                Box(modifier = Modifier.size(40.dp).background(Color.DarkGray, CircleShape).clickable { onAvatarClick(post.senderEmail) }, contentAlignment = Alignment.Center) {
                    if (dynamicAvatarUrl != null) {
                        AsyncImage(
                            model = dynamicAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            onError = { error ->
                                val errorMessage = error.result.throwable.message
                                val requestUrl = error.result.request.data
                                android.util.Log.e("SocialScreen", "Image Load Failed for ${post.senderEmail}: $errorMessage - URL: $requestUrl")
                            }
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.senderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(timeString, color = Color.Gray, fontSize = 12.sp)
                }

                if (isMyPost) {
                    Box {
                        IconButton(onClick = { expandedMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler", tint = Color.Gray) }
                        DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }, modifier = Modifier.background(Color(0xFF2A2A2A))) {
                            DropdownMenuItem(text = { Text("Düzenle", color = Color.White) }, onClick = { expandedMenu = false; showEditDialog = true })
                            DropdownMenuItem(text = { Text("Sil", color = Color(0xFFE50914)) }, onClick = { expandedMenu = false; showDeleteConfirmDialog = true })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val blurModifier = if (!showSpoilerContent) Modifier.blur(16.dp) else Modifier

            Box(modifier = Modifier.fillMaxWidth().clickable { if (!showSpoilerContent) showSpoilerContent = true }, contentAlignment = Alignment.Center) {
                Column(modifier = blurModifier.fillMaxWidth()) {
                    if (post.content.isNotBlank()) {
                        Text(
                            text = parseMarkdown(post.content),
                            color = Color.White,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }

                    if (post.movieTitle != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (post.moviePosterUrl != null) {
                                AsyncImage(model = post.moviePosterUrl, contentDescription = null, modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Film Önerisi/Alıntısı", color = Color.Gray, fontSize = 12.sp)
                                }
                                Text(text = post.movieTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    if (post.videoUrl != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val signedUrl = remember(post.videoUrl) { viewModel.getSignedUrl(post.videoUrl) }
                        VideoPlayerCard(videoUrl = signedUrl ?: post.videoUrl)
                    }
                }
                
                if (!showSpoilerContent) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(8.dp)).padding(16.dp)) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Spoiler İçerir", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Görmek için tıklayın", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            val isLikedByMe = post.likedBy.contains(myEmail)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.toggleLike(post) }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = if (isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Beğen", tint = if (isLikedByMe) Color(0xFFE50914) else Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("${post.likes}", color = Color.Gray, fontSize = 14.sp)
            }
        }
        }
        
        if (showHeart || scale > 0f) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE50914),
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = if (showHeart) 1f else 0f)
            )
        }
    }

    if (showEditDialog) {
        var editContent by remember { mutableStateOf(post.content) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false }, containerColor = Color(0xFF1E1E1E),
            title = { Text("Gönderiyi Düzenle", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = editContent, onValueChange = { editContent = it }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray), modifier = Modifier.fillMaxWidth().height(120.dp)) },
            confirmButton = { Button(onClick = { viewModel.updatePostContent(post.id, editContent, context); showEditDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Kaydet", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("İptal", color = Color.Gray) } }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false }, containerColor = Color(0xFF1E1E1E),
            title = { Text("Gönderiyi Sil", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Bu gönderiyi kalıcı olarak silmek istediğinize emin misiniz?", color = Color.LightGray) },
            confirmButton = { Button(onClick = { viewModel.deletePost(post, context); showDeleteConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Evet, Sil", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("İptal", color = Color.Gray) } }
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerCard(videoUrl: String) {
    val context = LocalContext.current
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    var isBuffering by remember(videoUrl) { mutableStateOf(true) }
    var errorCode by remember(videoUrl) { mutableStateOf("") }

    val exoPlayer = remember(videoUrl) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x86) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = false
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("VideoPlayerCard", "Player Error: ${error.errorCodeName} (${error.errorCode}) - URL: $videoUrl")
                    hasError = true
                    isBuffering = false
                    errorCode = error.errorCodeName
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                }
            })
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Close, contentDescription = "Hata", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Video yüklenemedi", color = Color.Gray, fontSize = 14.sp)
                    if (errorCode.isNotEmpty()) {
                        Text("Hata Kodu: $errorCode", color = Color.DarkGray, fontSize = 10.sp)
                    }
                }
            } else {
                AndroidView(
                    factory = { PlayerView(context).apply { player = exoPlayer; useController = true; setBackgroundColor(android.graphics.Color.BLACK) } },
                    update = { it.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )
                if (isBuffering) { CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(40.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(viewModel: FeedViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var isSpoiler by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults
    val isUploading by viewModel.isUploading
    val videoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> videoUri = uri }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Gönderi Oluştur", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = content, onValueChange = { content = it }, placeholder = { Text("Bir replik, sahne veya düşünce paylaş...", color = Color.Gray) }, modifier = Modifier.fillMaxWidth().height(100.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isSpoiler = !isSpoiler }) {
                    Checkbox(checked = isSpoiler, onCheckedChange = { isSpoiler = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE50914), uncheckedColor = Color.Gray))
                    Text("Spoiler İçerir", color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedMovie == null) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it; viewModel.searchMovieForQuote(it) }, placeholder = { Text("Bir film veya dizi etiketle (İsteğe bağlı)", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, tint = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE50914), unfocusedBorderColor = Color.DarkGray), singleLine = true)
                    if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(searchResults) { movie ->
                                Column(modifier = Modifier.width(80.dp).clickable { selectedMovie = movie; searchQuery = "" }, horizontalAlignment = Alignment.CenterHorizontally) {
                                    AsyncImage(model = movie.fullImageUrl, contentDescription = null, modifier = Modifier.size(80.dp, 120.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    val displayTitle = movie.title ?: movie.name ?: ""
                                    Text(displayTitle, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = selectedMovie!!.fullImageUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(8.dp))
                        val displayTitle = selectedMovie!!.title ?: selectedMovie!!.name ?: ""
                        Text(text = displayTitle, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { selectedMovie = null }) { Icon(Icons.Default.Close, contentDescription = "İptal", tint = Color.Gray) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { videoPickerLauncher.launch("video/*") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = if (videoUri != null) Color(0xFFE50914) else Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (videoUri != null) "Video Seçildi" else "Video Ekle (İsteğe bağlı)", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalTitle = selectedMovie?.title ?: selectedMovie?.name
                    viewModel.sendPost(
                        content = content, 
                        movieTitle = finalTitle, 
                        moviePosterUrl = selectedMovie?.fullImageUrl, 
                        videoUri = videoUri, 
                        isSpoiler = isSpoiler, 
                        context = context,
                        onComplete = { onDismiss() }
                    ) 
                },
                enabled = !isUploading && (content.isNotBlank() || videoUri != null || selectedMovie != null), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) {
                if (isUploading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } else { Text("Paylaş", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { if (!isUploading) { TextButton(onClick = onDismiss) { Text("İptal", color = Color.Gray) } } }
    )
}

@Composable
fun GroupsContent(viewModel: SocialViewModel, onGroupClick: (String, String) -> Unit) {
    val groups by viewModel.groups
    var showDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }, containerColor = Color(0xFFE50914), shape = CircleShape) { Icon(Icons.Default.Add, contentDescription = "Yeni Grup", tint = Color.White) } },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            items(groups) { group -> WatchGroupCard(group = group, onClick = { onGroupClick(group.id, group.name) }, onDeleteClick = { viewModel.deleteGroup(group.id) }) }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false }, containerColor = Color(0xFF1E1E1E),
                title = { Text("Yeni Grup Kur", color = Color.White) },
                text = { OutlinedTextField(value = newGroupName, onValueChange = { newGroupName = it }, placeholder = { Text("Grup Adı") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) },
                confirmButton = { Button(onClick = { viewModel.createGroup(newGroupName); showDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))) { Text("Oluştur", color = Color.White) } },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("İptal", color = Color.Gray) } }
            )
        }
    }
}

@Composable
fun WatchGroupCard(group: WatchGroup, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
    val isCreator = currentUserEmail == group.createdBy

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).background(Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Group, contentDescription = null, tint = Color.White) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = group.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Sıradaki: ${group.currentMovie}", color = Color.LightGray, fontSize = 13.sp)
            }
            if (isCreator) {
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Close, contentDescription = "Grubu Sil", tint = Color(0xFFE50914)) }
            } else {
                Box(modifier = Modifier.background(Color(0xFFE50914).copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp, 4.dp)) { Text(text = "${group.memberCount} Üye", color = Color(0xFFE50914), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
