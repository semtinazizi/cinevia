package com.huyarev.cinevia

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.huyarev.cinevia.ui.theme.*
import com.huyarev.cinevia.network.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SwipeScreen(
    swipeViewModel: SwipeViewModel = hiltViewModel(),
    calendarViewModel: CalendarViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Keşfet", "30 Günlük Takvim")

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // --- ÜST SEKMELER ---
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SurfaceDark,
            contentColor = CinemaRed,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = CinemaRed
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        // --- İÇERİK ---
        when (selectedTabIndex) {
            0 -> TinderSwipeSection(swipeViewModel)
            1 -> CalendarSection(calendarViewModel)
        }
    }
}

// --- 1. SEKMELİ KISIM: TİNDER SWIPE ---
@Composable
fun TinderSwipeSection(viewModel: SwipeViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val movies by viewModel.movies
    val currentIndex by viewModel.currentIndex
    val isLoading by viewModel.isLoading

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading || movies.isEmpty()) {
            CircularProgressIndicator(color = CinemaRed)
        } else {
            val currentMovie = movies[currentIndex]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SwipeableMovieCard(
                    movie = currentMovie,
                    offsetX = offsetX,
                    coroutineScope = coroutineScope,
                    onSwipeLeft = { viewModel.nextMovie() },
                    onSwipeRight = {
                        viewModel.saveToWatchlist(currentMovie)
                        viewModel.nextMovie()
                    }
                )
                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SwipeActionButton(icon = Icons.Default.Close, color = Color.LightGray, onClick = {
                        coroutineScope.launch {
                            offsetX.animateTo(-1000f, animationSpec = tween(300))
                            viewModel.nextMovie()
                            offsetX.snapTo(0f)
                        }
                    })
                    SwipeActionButton(icon = Icons.Default.Favorite, color = CinemaRed, onClick = {
                        coroutineScope.launch {
                            offsetX.animateTo(1000f, animationSpec = tween(300))
                            viewModel.saveToWatchlist(currentMovie)
                            viewModel.nextMovie()
                            offsetX.snapTo(0f)
                        }
                    })
                }
            }
        }
    }
}

// --- 2. SEKMELİ KISIM: TAKVİM ---
@Composable
fun CalendarSection(viewModel: CalendarViewModel) {
    val movies by viewModel.calendarMovies
    val scratched by viewModel.scratchedDays
    val currentDay by viewModel.currentDayOfCycle
    val isLoading by viewModel.isLoading

    // Hangi pencere açılacak onu tutuyoruz
    var movieToScratch by remember { mutableStateOf<Pair<Int, CalendarMovie>?>(null) }
    var viewedScratchedMovie by remember { mutableStateOf<CalendarMovie?>(null) } // YENİ: Geçmiş filmi görüntüleme

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CinemaRed)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("🎬 Sinema Takvimi", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("Her gün yeni bir kutu açma şansı!", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(movies) { index, movie ->
                    val dayNumber = index + 1
                    val isScratched = scratched.contains(index)
                    val isUnlocked = dayNumber <= currentDay

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isScratched) Color.Transparent else if (isUnlocked) Color(0xFFB0B0B0) else Color(0xFF1E1E1E))
                            .clickable(enabled = isUnlocked) { // YENİ: Sadece kilidi açıksa tıklanabilir (Kazınmış olsa bile)
                                if (!isScratched) {
                                    // Henüz kazınmamışsa KAZIMA PENCERESİNİ aç
                                    movieToScratch = Pair(index, movie)
                                } else {
                                    // Zaten kazınmışsa GEÇMİŞİ GÖRÜNTÜLEME PENCERESİNİ aç
                                    viewedScratchedMovie = movie
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScratched) {
                            AsyncImage(model = movie.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else if (isUnlocked) {
                            Text("$dayNumber", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }

    // 1. DİALOG: Henüz kazınmamış kutuyu kazıma ekranı
    if (movieToScratch != null) {
        Dialog(onDismissRequest = {
            viewModel.scratchDay(movieToScratch!!.first)
            movieToScratch = null
        }) {
            CalendarScratchCard(movie = movieToScratch!!.second, onDismiss = {
                viewModel.scratchDay(movieToScratch!!.first)
                movieToScratch = null
            })
        }
    }

    // 2. DİALOG: Zaten kazınmış kutuya basınca filmi gururla sergileme ekranı
    if (viewedScratchedMovie != null) {
        Dialog(onDismissRequest = { viewedScratchedMovie = null }) {
            ScratchedMovieDetailCard(movie = viewedScratchedMovie!!, onDismiss = {
                viewedScratchedMovie = null
            })
        }
    }
}

// --- YENİ EKLENEN: GEÇMİŞ FİLMİ İNCELEME KARTI (Kazınmış Olanlar İçin) ---
@Composable
fun ScratchedMovieDetailCard(movie: CalendarMovie, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(450.dp), shape = RoundedCornerShape(24.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)))

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("ŞANSINA BU FİLM ÇIKMIŞTI", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(movie.title, color = CinemaRed, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Puan: ${movie.voteAverage}", color = Color.White, fontSize = 18.sp)
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            ) {
                Text("Kapat", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- KAZI KAZAN DİALOGU (Henüz Kazınmamış Olanlar İçin) ---
@Composable
fun CalendarScratchCard(movie: CalendarMovie, onDismiss: () -> Unit) {
    val currentPath = remember { Path() }
    var updateTrigger by remember { mutableIntStateOf(0) }

    Card(modifier = Modifier.fillMaxWidth().height(450.dp), shape = RoundedCornerShape(24.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ALT KATMAN (FİLM BİLGİSİ)
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)))
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("TEBRİKLER!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Light)
                    Text(movie.title, color = CinemaRed, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("Puan: ${movie.voteAverage}", color = Color.White, fontSize = 18.sp)
                }
            }

            // ÜST KATMAN (GÜMÜŞ KAZIMA ALANI)
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPath.moveTo(offset.x, offset.y); updateTrigger++ },
                            onDrag = { change, _ -> currentPath.lineTo(change.position.x, change.position.y); updateTrigger++ }
                        )
                    }
            ) {
                drawRect(color = Color(0xFFC0C0C0))
                updateTrigger.let {
                    drawPath(
                        path = currentPath,
                        color = Color.Transparent,
                        style = Stroke(width = 120f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        blendMode = BlendMode.Clear
                    )
                }
            }

            // KAZIMA UYARISI YAZISI (Kazımaya başlayınca kaybolur)
            if (updateTrigger < 15) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "✨ Günün Sürprizini", color = Color(0xFF333333), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(text = "Görmek İçin Kazı! ✨", color = Color(0xFF333333), fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }

            // BUTON EN ÜSTTE
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            ) {
                Text("Kapat ve Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- YARDIMCI BİLEŞENLER ---
@Composable
fun SwipeActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(70.dp).background(SurfaceDark, CircleShape).border(2.dp, color.copy(alpha = 0.3f), CircleShape)) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun SwipeableMovieCard(movie: Movie, offsetX: Animatable<Float, AnimationVector1D>, coroutineScope: CoroutineScope, onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit) {
    val rotation = offsetX.value / 20f
    Card(
        modifier = Modifier.width(320.dp).height(480.dp).offset { IntOffset(offsetX.value.roundToInt(), 0) }.graphicsLayer(rotationZ = rotation).pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    coroutineScope.launch {
                        if (offsetX.value < -250f) { offsetX.animateTo(-1000f); onSwipeLeft(); offsetX.snapTo(0f) }
                        else if (offsetX.value > 250f) { offsetX.animateTo(1000f); onSwipeRight(); offsetX.snapTo(0f) }
                        else { offsetX.animateTo(0f) }
                    }
                },
                onDrag = { change, dragAmount -> change.consume(); coroutineScope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) } }
            )
        },
        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = movie.fullImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text(text = movie.title ?: movie.name ?: "Bilinmeyen", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = "Puan: ${movie.voteAverage}/10", color = Color.LightGray, fontSize = 16.sp)
            }
        }
    }
}
