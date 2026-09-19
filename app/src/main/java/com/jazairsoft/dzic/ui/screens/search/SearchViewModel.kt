package com.jazairsoft.dzic.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.local.PlaylistWithCount
import com.jazairsoft.dzic.data.local.PlaylistsRepository
import com.jazairsoft.dzic.data.remote.RadioBrowserRepository
import com.jazairsoft.dzic.domain.model.Station
import com.jazairsoft.dzic.playback.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Station> = emptyList(),
    val hasSearched: Boolean = false,
    val error: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: RadioBrowserRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _pendingStation = MutableStateFlow<Station?>(null)
    val pendingStation: StateFlow<Station?> = _pendingStation.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<PlaylistWithCount>> = playlistsRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playerState = playerManager.state

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        searchJob?.cancel()
        if (value.trim().length < 2) {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false, isLoading = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce : evite un appel API a chaque frappe
            _uiState.value = _uiState.value.copy(isLoading = true, error = false)
            runCatching { repository.search(value) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(results = it, isLoading = false, hasSearched = true)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, hasSearched = true, error = true)
                }
        }
    }

    fun play(station: Station, queue: List<Station>) = playerManager.play(station, queue)

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }

    fun requestAddToPlaylist(station: Station) {
        _pendingStation.value = station
    }

    fun dismissAddToPlaylist() {
        _pendingStation.value = null
    }

    fun addPendingTo(playlistId: Long) {
        val station = _pendingStation.value ?: return
        viewModelScope.launch {
            playlistsRepository.addStation(playlistId, station)
            _pendingStation.value = null
        }
    }

    fun createPlaylistWithPending(name: String) {
        if (name.isBlank()) return
        val station = _pendingStation.value
        viewModelScope.launch {
            val id = playlistsRepository.create(name)
            if (station != null) playlistsRepository.addStation(id, station)
            _pendingStation.value = null
        }
    }
}
