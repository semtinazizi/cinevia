package com.huyarev.cinevia

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.huyarev.cinevia.utils.getDominantColorFromUrl

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.huyarev.cinevia.navigation.MovieDetailRoute
import com.huyarev.cinevia.navigation.PersonDetailRoute
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    isTvShow: Boolean,
    onBackClick: () -> Unit,
    navController: NavController = rememberNavController(),
    viewModel: MovieDetailViewModel = hiltViewModel(),
    feedViewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detail = uiState.movieDetail
    val isLoading = uiState.isLoading
    val trailerKey = uiState.trailerKey
    val cast = uiState.cast
    val similarMovies = uiState.similarMovies
    val translatedTitle = uiState.translatedTitle
    val isTranslating = uiState.isTranslating
    val context = LocalContext.current

    var showRatingDialog by remember { mutableStateOf(false) }
    var showTrailerDialog by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var shareToSocial by remember { mutableStateOf(false) }
    var backgroundColor by remember { mutableStateOf(Color(0xFF050505)) }
    val scrollState = rememberScrollState()
    
    val communityReviews by feedViewModel.moviePosts

    LaunchedEffect(movieId) {
        viewModel.fetchDetails(movieId, isTvShow)
        feedViewModel.fetchPostsByMovieId(movieId)
    }

    LaunchedEffect(detail?.posterPath) {
        detail?.posterPath?.let { path ->
            val url = "https://image.tmdb.org/t/p/w500$path"
            backgroundColor = getDominantColorFromUrl(context, url)
        }
    }

    Scaffold(containerColor = Color(0xFF050505)) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundColor.copy(alpha = 0.5f), Color(0xFF050505))))
        ) {
            if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else if (detail != null) {
            val movie = detail!!

            val originalTitle = movie.title ?: movie.name ?: "İsimsiz İçerik"
            val displayTitle = translatedTitle ?: originalTitle
            val displayDate = (movie.releaseDate ?: movie.first_air_date)?.take(4) ?: "Bilinmiyor"
            val overviewText = if (movie.overview.isNullOrBlank()) "Bu içerik için henüz Türkçe bir özet bulunmuyor." else movie.overview

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).graphicsLayer {
                    translationY = scrollState.value * 0.5f
                    alpha = 1f - (scrollState.value.toFloat() / 800f).coerceIn(0f, 1f)
                }) {
                    AsyncImage(model = movie.backdropUrl, contentDescription = "Arka Plan", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF050505)), startY = 300f)))

                    if (trailerKey != null) {
                        IconButton(
                            onClick = { showTrailerDialog = true },
                            modifier = Modifier.align(Alignment.Center).size(64.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Fragman", tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                    }

                    IconButton(onClick = onBackClick, modifier = Modifier.padding(top = 40.dp, start = 16.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                }


                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayTitle, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
                            modifier = Modifier.weight(1f)
                        )

                        if (isTranslating) {
                            CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else if (translatedTitle == null) {
                            IconButton(onClick = { viewModel.translateTitle(originalTitle) }) {
                                Icon(Icons.Default.Language, contentDescription = stringResource(id = R.string.translate), tint = Color(0xFFE50914))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoPill(icon = Icons.Default.Star, text = "${movie.voteAverage ?: "N/A"}/10", color = Color(0xFFFFC107))
                        InfoPill(icon = Icons.Default.CalendarToday, text = displayDate, color = Color.LightGray)
                        if (movie.runtime != null && movie.runtime > 0) {
                            val hours = movie.runtime / 60
                            val mins = movie.runtime % 60
                            InfoPill(icon = Icons.Default.AccessTime, text = "${hours}sa ${mins}dk", color = Color.LightGray)
                        }
                    }

                    if (!movie.genres.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(movie.genres) { genre ->
                                Surface(
                                    color = Color(0xFF1E1E1E),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = genre.name,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!movie.tagline.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"${movie.tagline}\"",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = stringResource(id = R.string.summary), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = overviewText, color = Color.LightGray, fontSize = 16.sp, lineHeight = 24.sp)

                    // --- OYUNCU KADROSU (CAST CAROUSEL) ---
                    if (cast.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "Oyuncu Kadrosu",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cast.take(15)) { member ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(90.dp)
                                        .clickable { navController.navigate(PersonDetailRoute(member.id)) }
                                ) {
                                    val imageUrl = member.profile_path?.let { "https://image.tmdb.org/t/p/w200$it" }
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = member.name,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(CircleShape)
                                            .background(Color.DarkGray, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = member.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    if (!member.character.isNullOrBlank()) {
                                        Text(
                                            text = member.character,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- YENİ EKLENEN LİSTEYE EKLEME BUTONLARI ---
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = stringResource(id = R.string.my_movie_diary), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // İzledim Butonu
                        Button(
                            onClick = { showRatingDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(stringResource(id = R.string.watched), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // İzleyeceğim Butonu
                        Button(
                            onClick = { viewModel.addToDiary("İzleyeceğim", context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(stringResource(id = R.string.will_watch), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Yarım Bıraktım Butonu
                        Button(
                            onClick = { viewModel.addToDiary("Yarım Bıraktım", context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(stringResource(id = R.string.dropped), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- TOPLULUK İNCELEMELERİ ---
                    Text(
                        text = "Topluluk İncelemeleri",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    if (communityReviews.isNotEmpty()) {
                        communityReviews.forEach { post ->
                            CommunityReviewItem(post)
                        }
                    } else {
                        Text(
                            text = "Bu film için henüz inceleme yazılmamış. İlk yazan sen ol!",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // --- BENZER İÇERİKLER (SIMILAR MOVIES) ---
                    if (similarMovies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "Benzer İçerikler",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(similarMovies) { simMovie ->
                                Column(
                                    modifier = Modifier.width(110.dp)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(160.dp)
                                            .clickable { navController.navigate(MovieDetailRoute(simMovie.id, isTvShow)) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = simMovie.fullImageUrl,
                                            contentDescription = simMovie.title ?: simMovie.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = simMovie.title ?: simMovie.name ?: "İsimsiz",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp)) // Alttan boşluk
                }
            }
            if (showRatingDialog) {
                AlertDialog(
                    onDismissRequest = { showRatingDialog = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Puan Ver", color = Color.White) },
                    text = {
                        Column {
                            Text("Bu içeriğe kaç yıldız veriyorsun?", color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Star",
                                        tint = if (i <= selectedRating) Color(0xFFFFC107) else Color.DarkGray,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable { selectedRating = i }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = reviewText,
                                onValueChange = { reviewText = it },
                                label = { Text("İnceleme yaz (isteğe bağlı)") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE50914),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray
                                ),
                                maxLines = 5
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = shareToSocial,
                                    onCheckedChange = { shareToSocial = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE50914), checkmarkColor = Color.White)
                                )
                                Text("Bunu Sosyal Akış'ta da paylaş", color = Color.LightGray, fontSize = 14.sp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addToDiary("İzledim", context, selectedRating, reviewText, shareToSocial, feedViewModel)
                            showRatingDialog = false
                            reviewText = "" // reset
                            shareToSocial = false
                        }) {
                            Text("Kaydet", color = Color(0xFFE50914), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRatingDialog = false }) {
                            Text("İptal", color = Color.Gray)
                        }
                    }
                )
            }

            if (showTrailerDialog && trailerKey != null) {
                TrailerPlayerDialog(
                    trailerKey = trailerKey,
                    onDismiss = { showTrailerDialog = false }
                )
            }
        }
        }
    }
}

@Composable
fun TrailerPlayerDialog(trailerKey: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            enableAutomaticInitialization = false
                            initialize(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.loadVideo(trailerKey, 0f)
                                }
                                override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                    // Youtube gömülü oynatma kısıtlaması (Hata 150/152) olduğunda doğrudan Youtube uygulamasında aç
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$trailerKey"))
                                    ctx.startActivity(intent)
                                    onDismiss()
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$trailerKey"))
                        context.startActivity(intent)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("YouTube Uygulamasında İzle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
            }
        }
    }
}

@Composable
fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CommunityReviewItem(post: SocialPost) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.senderProfileImageUrl ?: "https://ui-avatars.com/api/?name=${post.senderName}&background=random",
                    contentDescription = "Profil",
                    modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(post.senderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (post.rating != null && post.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.rating}/5", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(post.content, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
