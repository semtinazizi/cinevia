package com.huyarev.cinevia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (Int) -> Unit,
    onUserClick: (String) -> Unit, // YENİ: Bulunan kişiye tıklayınca profiline gitmesi için eklendi
    onBackClick: () -> Unit,
    viewModel: SearchViewModel = viewModel(),
    feedViewModel: FeedViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val movieResults by viewModel.searchResults
    val userResults by viewModel.userSearchResults
    val isLoading by viewModel.isLoading

    // SEKME DEĞİŞKENLERİ
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Filmler", "Kişiler")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF121212))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.search(it) // Çift motorlu aramayı tetikler
                        },
                        placeholder = { Text("Film veya kişi ara...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // YENİ: ŞIK SEKME MENÜSÜ
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF121212),
                    contentColor = Color(0xFFE50914),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFFE50914)
                        )
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
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                if (selectedTabIndex == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(15) {
                            com.huyarev.cinevia.ui.components.ShimmerMovieItem()
                        }
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFE50914))
                }
            } else {
                if (selectedTabIndex == 0) {
                    // --- 1. FİLM SEKRESİ ---
                    if (movieResults.isEmpty() && query.isNotBlank()) {
                        Text("Film bulunamadı.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(movieResults) { movie ->
                                SearchMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                            }
                        }
                    }
                } else {
                    // --- 2. KİŞİLER SEKRESİ ---
                    if (userResults.isEmpty() && query.isNotBlank()) {
                        Text("CineRev evreninde böyle birisi yok.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(userResults) { user ->
                                SearchUserRow(user = user, onClick = { onUserClick(user.email) }, feedViewModel = feedViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchMovieCard(movie: com.huyarev.cinevia.network.Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Card(shape = RoundedCornerShape(8.dp)) {
            AsyncImage(
                model = movie.fullImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = movie.title ?: movie.name ?: "İsimsiz",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SearchUserRow(user: UserSearchResult, onClick: () -> Unit, feedViewModel: FeedViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp).background(Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val profileImageUrl = remember(user.profileImageUrl) { feedViewModel.getSignedUrl(user.profileImageUrl) }
            if (profileImageUrl != null) {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = { error ->
                        android.util.Log.e("SearchScreen", "Image Load Failed for ${user.email}: ${error.result.throwable.message} - URL: ${error.result.request.data}")
                    }
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            // SADECE İSİM GÖRÜNECEK, E-POSTA SATIRINI TAMAMEN SİLDİK! 🛡️
            Text(text = user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
