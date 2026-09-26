package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.huyarev.cinevia.network.Movie
import com.huyarev.cinevia.network.TmdbApi
import com.huyarev.cinevia.network.TmdbApi.Person
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: TmdbApi
) : ViewModel() {
    private val API_KEY = BuildConfig.TMDB_API_KEY

    private val _searchResults = mutableStateOf<List<Person>>(emptyList())
    val searchResults: State<List<Person>> = _searchResults

    private val _trackedPersons = mutableStateOf<List<Person>>(emptyList())
    val trackedPersons: State<List<Person>> = _trackedPersons

    private val _radarMovies = mutableStateOf<List<Movie>>(emptyList())
    val radarMovies: State<List<Movie>> = _radarMovies

    init {
        loadTrackedPersons()
    }

    fun searchPerson(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val response = api.searchPerson(API_KEY, query)
                _searchResults.value = response.results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun trackPerson(person: Person) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        viewModelScope.launch {
            val radarRef = db.collection("users").document(email).collection("radar").document(person.id.toString())
            val personData = mapOf(
                "id" to person.id,
                "name" to person.name,
                "profile_path" to person.profile_path,
                "known_for_department" to person.known_for_department
            )
            radarRef.set(personData, SetOptions.merge()).await()
            loadTrackedPersons()
        }
    }

    fun untrackPerson(personId: Int) {
        val email = auth.currentUser?.email?.lowercase() ?: return
        viewModelScope.launch {
            db.collection("users").document(email).collection("radar").document(personId.toString()).delete().await()
            loadTrackedPersons()
        }
    }

    private fun loadTrackedPersons() {
        val email = auth.currentUser?.email?.lowercase() ?: return
        db.collection("users").document(email).collection("radar").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val persons = snapshot.documents.mapNotNull { doc ->
                val id = doc.getLong("id")?.toInt() ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val profilePath = doc.getString("profile_path")
                val dept = doc.getString("known_for_department")
                Person(id, name, profilePath, dept)
            }
            _trackedPersons.value = persons
            loadRadarMovies(persons)
        }
    }

    private fun loadRadarMovies(persons: List<Person>) {
        viewModelScope.launch {
            val allMovies = mutableListOf<Movie>()
            persons.forEach { person ->
                try {
                    val credits = api.getPersonMovies(person.id, API_KEY)
                    val movies = if (person.known_for_department == "Directing") {
                        credits.crew.filter { it.job == "Director" }
                    } else {
                        credits.cast
                    }
                    allMovies.addAll(movies)
                } catch (e: Exception) {}
            }
            
            // Son çıkanlara göre sırala ve tekrarları çıkar
            val uniqueMovies = allMovies.distinctBy { it.id }.sortedByDescending { it.releaseDate ?: "" }
            _radarMovies.value = uniqueMovies.take(20) // En son çıkan 20 film
        }
    }
}
