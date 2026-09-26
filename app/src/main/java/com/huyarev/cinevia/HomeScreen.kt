package com.huyarev.cinevia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.ui.components.ShimmerMovieItem
import com.huyarev.cinevia.ui.theme.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.huyarev.cinevia.navigation.MovieDetailRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val movies = uiState.movies
    val trendingMovies = uiState.trendingMovies
    val isLoading = uiState.isLoading
    val selectedGenre = uiState.selectedGenre
    val currentType = uiState.selectedContentType
    val categoryList = uiState.categoryList
    val isTvShow = currentType == ContentType.TV_SHOW

    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.loadContent()
        }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            pullToRefreshState.endRefresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground).nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Text(
                text = stringResource(id = R.string.app_name), color = CinemaRed, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
            )

            // --- FİLM / DİZİ GEÇİŞ PANELİ ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(SurfaceDark, RoundedCornerShape(20.dp)),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.weight(1f).clickable { viewModel.setContentType(ContentType.MOVIE) },
                    color = if (currentType == ContentType.MOVIE) CinemaRed else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(stringResource(id = R.string.movies), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                }

                Surface(
                    modifier = Modifier.weight(1f).clickable { viewModel.setContentType(ContentType.TV_SHOW) },
                    color = if (currentType == ContentType.TV_SHOW) CinemaRed else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(stringResource(id = R.string.tv_shows), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // --- KATEGORİ BUTONLARI ---
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                items(categoryList) { genre ->
                    val isSelected = selectedGenre == genre
                    Surface(
                        modifier = Modifier.clickable { viewModel.fetchByGenre(genre) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) CinemaRed else SurfaceDark,
                    ) {
                        Text(genre.name, color = if (isSelected) Color.White else Color.LightGray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }

            if (isLoading) {
                Column {
                    repeat(3) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            repeat(3) { ShimmerMovieItem() }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        val rowTitle = if (selectedGenre == null) stringResource(id = R.string.popular) else "${selectedGenre!!.name}"
                        MovieCategoryRow(categoryTitle = rowTitle, movies = movies, navController = navController, isTvShow = isTvShow)
                    }
                    if (selectedGenre == null && trendingMovies.isNotEmpty()) {
                        item { MovieCategoryRow(categoryTitle = stringResource(id = R.string.trending), movies = trendingMovies, navController = navController, isTvShow = isTvShow) }
                    }
                }
            }
        }
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = SurfaceDark,
            contentColor = CinemaRed
        )
    }
}

@Composable
fun MovieCategoryRow(categoryTitle: String, movies: List<Movie>, navController: NavController, isTvShow: Boolean) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(categoryTitle, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(movies) { movie -> MovieCard(movie = movie, navController = navController, isTvShow = isTvShow) }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, navController: NavController, isTvShow: Boolean) {
    val displayTitle = movie.title ?: movie.name ?: "Bilinmeyen İçerik"
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500), initialOffsetY = { it / 2 })
    ) {
        Column(modifier = Modifier.width(130.dp)) {
            Card(
                modifier = Modifier.width(140.dp).height(210.dp).padding(8.dp)
                    .clickable { navController.navigate(MovieDetailRoute(movie.id, isTvShow)) },
                shape = RoundedCornerShape(12.dp)
            ) {
                AsyncImage(model = movie.fullImageUrl, contentDescription = displayTitle, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayTitle,
                color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
