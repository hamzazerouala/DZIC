package com.jazairsoft.dzic.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.DownloadRepository
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.local.HistoryRepository
import com.jazairsoft.dzic.data.local.PlaylistWithCount
import com.jazairsoft.dzic.data.local.PlaylistsRepository
import com.jazairsoft.dzic.domain.model.Station
import com.jazairsoft.dzic.playback.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibrarySection { FAVORITES, PLAYLISTS, DOWNLOADS, HISTORY }

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val historyRepository: HistoryRepository,
    private val downloadRepository: DownloadRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _section = MutableStateFlow(LibrarySection.FAVORITES)
    val section: StateFlow<LibrarySection> = _section.asStateFlow()

    /** Station en attente d'etre ajoutee a une playlist (dialogue ouvert). */
    private val _pendingStation = MutableStateFlow<Station?>(null)
    val pendingStation: StateFlow<Station?> = _pendingStation.asStateFlow()

    val favorites: StateFlow<List<Station>> = favoritesRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<PlaylistWithCount>> = playlistsRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Station>> = historyRepository.recent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<Station>> = downloadRepository.completed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadStates: StateFlow<Map<String, String>> = downloadRepository.states
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun removeDownload(id: String) {
        viewModelScope.launch { downloadRepository.remove(id) }
    }

    val playerState = playerManager.state

    fun selectSection(value: LibrarySection) {
        _section.value = value
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

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistsRepository.create(name) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { playlistsRepository.delete(playlistId) }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch { playlistsRepository.rename(playlistId, name) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    fun removeFromHistory(stationId: String) {
        viewModelScope.launch { historyRepository.remove(stationId) }
    }
}
