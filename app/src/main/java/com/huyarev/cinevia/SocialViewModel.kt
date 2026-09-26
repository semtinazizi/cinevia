package com.huyarev.cinevia

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huyarev.cinevia.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchGroup(
    val id: String = "",
    val name: String = "",
    val currentMovie: String = "",
    val memberCount: Int = 0,
    val createdBy: String = ""
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository
) : ViewModel() {

    private val _groups = mutableStateOf<List<WatchGroup>>(emptyList())
    val groups: State<List<WatchGroup>> = _groups

    init {
        fetchGroups()
    }

    private fun fetchGroups() {
        viewModelScope.launch {
            repository.getGroups().collect { groupList ->
                _groups.value = groupList
            }
        }
    }

    fun deleteGroup(groupId: String) {
        repository.deleteGroup(groupId)
    }

    fun createGroup(groupName: String) {
        repository.createGroup(groupName)
    }
}
