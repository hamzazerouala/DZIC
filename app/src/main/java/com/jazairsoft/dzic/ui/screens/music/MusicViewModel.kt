package com.jazairsoft.dzic.ui.screens.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.DownloadOutcome
import com.jazairsoft.dzic.data.local.DownloadRepository
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.local.PlaylistWithCount
import com.jazairsoft.dzic.data.local.PlaylistsRepository
import com.jazairsoft.dzic.data.remote.MusicRepository
import com.jazairsoft.dzic.domain.model.MusicCategory
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

data class MusicUiState(
    val category: MusicCategory = MusicCategory.SPORT,
    val tracks: List<Station> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val query: String = "",
    val isSearching: Boolean = false
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val downloadRepository: DownloadRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val _pendingStation = MutableStateFlow<Station?>(null)
    val pendingStation: StateFlow<Station?> = _pendingStation.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<PlaylistWithCount>> = playlistsRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playerState = playerManager.state

    val downloadStates: StateFlow<Map<String, String>> = downloadRepository.states
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _notice = MutableStateFlow<Int?>(null)
    val notice: StateFlow<Int?> = _notice.asStateFlow()

    fun download(track: Station) {
        _notice.value = when (downloadRepository.enqueue(track)) {
            DownloadOutcome.Started -> com.jazairsoft.dzic.R.string.download_started
            DownloadOutcome.NeedsWifi -> com.jazairsoft.dzic.R.string.download_needs_wifi
            DownloadOutcome.NotDownloadable -> com.jazairsoft.dzic.R.string.download_not_possible
            DownloadOutcome.AlreadyPresent -> com.jazairsoft.dzic.R.string.download_done
        }
    }

    fun clearNotice() { _notice.value = null }

    private var searchJob: Job? = null

    init {
        selectCategory(MusicCategory.SPORT)
    }

    fun selectCategory(category: MusicCategory, forceRefresh: Boolean = false) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            category = category,
            isLoading = true,
            error = false,
            isSearching = false
        )
        viewModelScope.launch {
            runCatching { repository.category(category, forceRefresh) }
                .onSuccess { _uiState.value = _uiState.value.copy(tracks = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = true) }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        searchJob?.cancel()
        if (value.trim().length < 2) {
            if (_uiState.value.isSearching) selectCategory(_uiState.value.category)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.value = _uiState.value.copy(isLoading = true, error = false, isSearching = true)
            runCatching { repository.search(value) }
                .onSuccess { _uiState.value = _uiState.value.copy(tracks = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = true) }
        }
    }

    fun retry() = if (_uiState.value.isSearching) onQueryChange(_uiState.value.query)
    else selectCategory(_uiState.value.category, forceRefresh = true)

    fun play(track: Station, queue: List<Station>) = playerManager.play(track, queue)

    fun toggleFavorite(track: Station) {
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }

    fun requestAddToPlaylist(track: Station) {
        _pendingStation.value = track
    }

    fun dismissAddToPlaylist() {
        _pendingStation.value = null
    }

    fun addPendingTo(playlistId: Long) {
        val track = _pendingStation.value ?: return
        viewModelScope.launch {
            playlistsRepository.addStation(playlistId, track)
            _pendingStation.value = null
        }
    }

    fun createPlaylistWithPending(name: String) {
        if (name.isBlank()) return
        val track = _pendingStation.value
        viewModelScope.launch {
            val id = playlistsRepository.create(name)
            if (track != null) playlistsRepository.addStation(id, track)
            _pendingStation.value = null
        }
    }
}
