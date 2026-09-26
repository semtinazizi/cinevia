package com.huyarev.cinevia

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.huyarev.cinevia.utils.getDominantColorFromUrl

import com.huyarev.cinevia.navigation.MovieDetailRoute

@Composable
fun PersonDetailScreen(
    personId: Int,
    navController: NavController,
    viewModel: PersonDetailViewModel = viewModel()
) {
    val personDetail by viewModel.personDetail
    val personMovies by viewModel.personMovies
    val isLoading by viewModel.isLoading

    var isBioExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var backgroundColor by remember { mutableStateOf(Color(0xFF050505)) }

    LaunchedEffect(personId) {
        viewModel.loadPerson(personId)
    }

    LaunchedEffect(personDetail?.profile_path) {
        personDetail?.profile_path?.let { path ->
            val url = "https://image.tmdb.org/t/p/w500$path"
            backgroundColor = getDominantColorFromUrl(context, url)
        }
    }

    Scaffold(
        containerColor = Color(0xFF050505),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(personDetail?.name ?: "", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505))
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else if (personDetail != null) {
            val person = personDetail!!
            val profileUrl = person.profile_path?.let { "https://image.tmdb.org/t/p/w500$it" }

            Column(modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(backgroundColor.copy(alpha = 0.7f), Color(0xFF050505))))
                .padding(paddingValues)) {
                
                // Üst Kısım: Fotoğraf ve Kısa Bilgiler
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = profileUrl,
                        contentDescription = person.name,
                        modifier = Modifier.size(100.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(person.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(person.known_for_department ?: "Bilinmiyor", color = Color(0xFFE50914), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!person.birthday.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cake, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(person.birthday, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        if (!person.place_of_birth.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(person.place_of_birth, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Biyografi
                if (!person.biography.isNullOrBlank()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).animateContentSize()) {
                        Text("Biyografi", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = person.biography,
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = if (isBioExpanded) "Daha Az Göster" else "Devamını Oku",
                            color = Color(0xFFE50914),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { isBioExpanded = !isBioExpanded }.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Filmografi (Grid)
                Text(
                    text = "Filmografi (${personMovies.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(personMovies) { movie ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navController.navigate(MovieDetailRoute(movie.id, isTvShow = false))
                            },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.aspectRatio(0.7f).fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box {
                                    AsyncImage(
                                        model = movie.fullImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (movie.voteAverage != null && movie.voteAverage > 0.0) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(bottomStart = 8.dp),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(text = String.format("%.1f", movie.voteAverage), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = movie.title ?: movie.name ?: "Bilinmiyor",
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
