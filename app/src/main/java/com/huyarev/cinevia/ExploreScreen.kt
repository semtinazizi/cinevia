package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

import com.huyarev.cinevia.navigation.MovieDetailRoute
import com.huyarev.cinevia.navigation.PersonDetailRoute

@Composable
fun ExploreScreen(
    navController: NavController,
    radarViewModel: RadarViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Radar", "Meydan Okumalar")

    val diaryMovies by profileViewModel.diaryMovies

    LaunchedEffect(Unit) {
        profileViewModel.loadCurrentUserProfile()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> RadarTabContent(radarViewModel, navController)
            1 -> ChallengesTabContent(diaryMovies)
        }
    }
}

@Composable
fun RadarTabContent(viewModel: RadarViewModel, navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults
    val trackedPersons by viewModel.trackedPersons
    val radarMovies by viewModel.radarMovies

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Text("Yönetmen ve Oyuncu Takibi", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Favori oyuncularını veya yönetmenlerini takip et, yeni çıkan filmlerini anında burada gör.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.searchPerson(it) },
                placeholder = { Text("Oyuncu veya yönetmen ara...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFE50914),
                    unfocusedBorderColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
            item {
                Text("Arama Sonuçları", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults) { person ->
                        val isTracked = trackedPersons.any { it.id == person.id }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                .clickable { navController.navigate(PersonDetailRoute(person.id)) }
                                .padding(8.dp)
                        ) {
                            val imageUrl = person.profile_path?.let { "https://image.tmdb.org/t/p/w200$it" }
                            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(person.name, color = Color.White, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                            Text(person.known_for_department ?: "", color = Color.Gray, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { if (isTracked) viewModel.untrackPerson(person.id) else viewModel.trackPerson(person) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isTracked) Color.DarkGray else Color(0xFFE50914)),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(30.dp).fillMaxWidth()
                            ) {
                                Icon(if (isTracked) Icons.Default.PersonRemove else Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (trackedPersons.isNotEmpty()) {
            item {
                Text("Takip Edilen Kişiler", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trackedPersons) { person ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                .clickable { navController.navigate(PersonDetailRoute(person.id)) }
                                .padding(8.dp)
                        ) {
                            val imageUrl = person.profile_path?.let { "https://image.tmdb.org/t/p/w200$it" }
                            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(person.name, color = Color.White, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.untrackPerson(person.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(30.dp).fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            Text("Radardaki Filmler", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            if (radarMovies.isEmpty()) {
                Text("Şu an radarınızda gösterilecek bir film yok. Yeni birilerini takip etmeyi deneyin.", color = Color.Gray, fontSize = 14.sp)
            }
        }

        items(radarMovies) { movie ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .clickable { navController.navigate(MovieDetailRoute(movie.id, isTvShow = false)) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(model = movie.fullImageUrl, contentDescription = null, modifier = Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(movie.title ?: movie.name ?: "Bilinmeyen", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Çıkış: ${movie.releaseDate ?: movie.first_air_date ?: "Belirsiz"}", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(movie.voteAverage?.toString() ?: "N/A", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengesTabContent(diaryMovies: List<DiaryEntry>) {
    val watchedCount = diaryMovies.count { it.status == "İzledim" }
    val likedCount = diaryMovies.count { it.status == "Beğendiklerim" || (it.status == "İzledim" && it.rating >= 4) }
    val reviewCount = diaryMovies.count { it.myReview.isNotBlank() }

    val challenges = listOf(
        ChallengeItem("Sinemaya Giriş", "10 Film İzle", watchedCount, 10),
        ChallengeItem("Sinefil Olma Yolunda", "50 Film İzle", watchedCount, 50),
        ChallengeItem("Efsane", "100 Film İzle", watchedCount, 100),
        ChallengeItem("Seçici Zevkler", "20 Filmi Beğen (4+ Puan)", likedCount, 20),
        ChallengeItem("Eleştirmen", "10 Filme İnceleme/Yorum Yaz", reviewCount, 10)
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            Text("Sinema Meydan Okumaları", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Aşağıdaki hedefleri tamamlayarak profilindeki rozetleri açabilirsin!", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        items(challenges) { challenge ->
            val progress = (challenge.current.toFloat() / challenge.target.toFloat()).coerceIn(0f, 1f)
            val isCompleted = challenge.current >= challenge.target

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(challenge.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(challenge.description, color = Color.Gray, fontSize = 12.sp)
                        }
                        if (isCompleted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Tamamlandı", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                        } else {
                            Text("${challenge.current} / ${challenge.target}", color = Color(0xFFE50914), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFE50914),
                        trackColor = Color(0xFF2A2A2A)
                    )
                }
            }
        }
    }
}

data class ChallengeItem(
    val title: String,
    val description: String,
    val current: Int,
    val target: Int
)
