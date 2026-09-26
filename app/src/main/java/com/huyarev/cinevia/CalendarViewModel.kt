package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.huyarev.cinevia.network.RetrofitInstance
import com.huyarev.cinevia.network.TmdbApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Takvime Özel Veri Modeli
data class CalendarMovie(
    val id: Int = 0,
    val title: String = "",
    val posterUrl: String = "",
    val voteAverage: Double = 0.0
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: TmdbApi
) : ViewModel() {

    private val _calendarMovies = mutableStateOf<List<CalendarMovie>>(emptyList())
    val calendarMovies: State<List<CalendarMovie>> = _calendarMovies

    private val _scratchedDays = mutableStateOf<List<Int>>(emptyList())
    val scratchedDays: State<List<Int>> = _scratchedDays

    private val _currentDayOfCycle = mutableStateOf(1)
    val currentDayOfCycle: State<Int> = _currentDayOfCycle

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        loadOrGenerateCalendar()
    }

    private fun loadOrGenerateCalendar() {
        val email = auth.currentUser?.email?.lowercase() ?: return
        val docRef = db.collection("users").document(email).collection("calendar").document("data")

        docRef.get().addOnSuccessListener { snapshot ->
            val currentTime = System.currentTimeMillis()
            val oneDayMillis = 24L * 60 * 60 * 1000

            if (snapshot.exists()) {
                val startMillis = snapshot.getLong("cycleStartMillis") ?: currentTime
                val daysPassed = ((currentTime - startMillis) / oneDayMillis).toInt() + 1

                if (daysPassed > 30) {
                    // 30 Gün doldu! Geçmiş filmleri al ve yepyeni bir 30 günlük döngü başlat.
                    val pastIds = snapshot.get("pastMovieIds") as? List<Long> ?: emptyList()
                    generateNewCycle(pastIds.map { it.toInt() }, currentTime, docRef)
                } else {
                    // Devam eden döngü içindeyiz
                    val moviesData = snapshot.get("movies") as? List<Map<String, Any>> ?: emptyList()
                    _calendarMovies.value = moviesData.map {
                        CalendarMovie(
                            id = (it["id"] as? Long)?.toInt() ?: 0,
                            title = it["title"] as? String ?: "",
                            posterUrl = it["posterUrl"] as? String ?: "",
                            voteAverage = (it["voteAverage"] as? Double) ?: 0.0
                        )
                    }
                    _scratchedDays.value = (snapshot.get("scratchedDays") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                    _currentDayOfCycle.value = daysPassed
                    _isLoading.value = false
                }
            } else {
                // Kullanıcı takvimi ilk defa açıyor
                generateNewCycle(emptyList(), currentTime, docRef)
            }
        }
    }

    private fun generateNewCycle(pastIds: List<Int>, currentTime: Long, docRef: com.google.firebase.firestore.DocumentReference) {
        viewModelScope.launch {
            try {
                val API_KEY = "9ddddd5c53fe1be229c68fad9fdc46b2"
                // Geçmişte çıkanların tekrar çıkmaması için büyük bir havuz çekiyoruz
                val response = api.getPopularMovies(apiKey = API_KEY)

                // Daha önce çıkmamış filmleri bul ve karıştır
                var freshMovies = (response.results ?: emptyList()).filter { !pastIds.contains(it.id) }.shuffled()

                // Eğer yeterli film yoksa (çok uzun aylardır kullanıyorsa) listeyi döngüyle uzatıyoruz ki sistem çökmesin
                while (freshMovies.size < 30) {
                    freshMovies = freshMovies + freshMovies.shuffled()
                }

                // 30 Tanesini seç!
                val selectedMovies = freshMovies.take(30).map {
                    CalendarMovie(it.id ?: 0, it.title ?: it.name ?: "", it.fullImageUrl ?: "", it.voteAverage ?: 0.0)
                }

                val newPastIds = pastIds + selectedMovies.map { it.id }
                val data = hashMapOf(
                    "cycleStartMillis" to currentTime,
                    "movies" to selectedMovies,
                    "scratchedDays" to emptyList<Int>(),
                    "pastMovieIds" to newPastIds
                )

                docRef.set(data).addOnSuccessListener {
                    _calendarMovies.value = selectedMovies
                    _scratchedDays.value = emptyList()
                    _currentDayOfCycle.value = 1
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    // Kullanıcı bir kutuyu kazıdıktan sonra veritabanına kaydeder
    fun scratchDay(dayIndex: Int) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        val currentList = _scratchedDays.value.toMutableList()
        if (!currentList.contains(dayIndex)) {
            currentList.add(dayIndex)
            _scratchedDays.value = currentList
            db.collection("users").document(email).collection("calendar").document("data")
                .update("scratchedDays", currentList)
        }
    }
}
