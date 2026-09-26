package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

import com.huyarev.cinevia.navigation.MovieDetailRoute

@Composable
fun DiaryScreen(
    navController: NavController,
    viewModel: DiaryViewModel
) {
    val diaryMovies by viewModel.diaryMovies
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("İzlediklerim", "İzleyeceklerim", "Beğendiklerim")

    // --- FİLTRELEME MOTORU GÜNCELLENDİ ---
    val watchedMovies = diaryMovies.filter { it.status == "İzledim" }
    val willWatchMovies = diaryMovies.filter { it.status == "İzleyeceğim" }

    // YENİ: Hem Tinder'dan sağa kaydırılanlar (Beğendiklerim) hem de yüksek puan verilenler (İzledim + 4 Yıldız) burada çıksın!
    val likedMovies = diaryMovies.filter { it.status == "Beğendiklerim" || (it.status == "İzledim" && it.rating >= 4) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        // ÜST BAŞLIK
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Film Günlüğüm", color = Color(0xFFE50914), fontSize = 24.sp, fontWeight = FontWeight.Black)
        }

        // --- SEKMELER (TABS) ALANI ---
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color(0xFFE50914),
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFFE50914)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        // --- SEKME İÇERİKLERİ (IZGARA GÖRÜNÜMÜ) ---
        // Seçilen sekmeye göre ilgili listeyi ekrana basıyoruz
        when (selectedTabIndex) {
            0 -> MovieGridList(movies = watchedMovies, navController = navController)
            1 -> MovieGridList(movies = willWatchMovies, navController = navController)
            2 -> MovieGridList(movies = likedMovies, navController = navController)
        }
    }
}

// Filmleri yan yana 3'lü ızgara (Grid) şeklinde dizen fonksiyon
@Composable
fun MovieGridList(movies: List<DiaryEntry>, navController: NavController) {
    if (movies.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bu liste henüz boş.", color = Color.Gray, fontSize = 16.sp)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // Yan yana 3 afiş
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp) // Alt menünün altında kalmasın diye
        ) {
            items(movies) { movie ->
                DiaryMovieGridCard(movie, navController)
            }
        }
    }
}

// Tekil Afiş Tasarımı
@Composable
fun DiaryMovieGridCard(movie: DiaryEntry, navController: NavController) {
    Column(modifier = Modifier.fillMaxWidth().clickable {
        navController.navigate(MovieDetailRoute(movie.movieId, isTvShow = false))
    }, horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.aspectRatio(0.7f).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Puan Yıldızı
                if (movie.rating > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = movie.rating.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
