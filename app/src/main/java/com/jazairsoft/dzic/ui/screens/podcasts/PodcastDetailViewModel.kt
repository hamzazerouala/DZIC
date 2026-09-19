package com.jazairsoft.dzic.ui.screens.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.EpisodeProgressRepository
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.remote.PodcastRepository
import com.jazairsoft.dzic.data.remote.PodcastSelection
import com.jazairsoft.dzic.domain.model.PodcastShow
import com.jazairsoft.dzic.domain.model.Station
import com.jazairsoft.dzic.playback.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastDetailUiState(
    val show: PodcastShow? = null,
    val episodes: List<Station> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false
)

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    private val repository: PodcastRepository,
    private val selection: PodcastSelection,
    private val favoritesRepository: FavoritesRepository,
    progressRepository: EpisodeProgressRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastDetailUiState())
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Episodes deja entames, pour afficher une pastille de reprise. */
    val startedEpisodes: StateFlow<Map<String, Float>> = progressRepository.all
        .map { list ->
            list.filterNot { it.isFinished }
                .associate { it.episodeId to if (it.durationMs > 0) it.positionMs.toFloat() / it.durationMs else 0f }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val playerState = playerManager.state

    init {
        load()
    }

    fun load() {
        val show = selection.current
        _uiState.value = _uiState.value.copy(show = show, isLoading = true, error = false)
        if (show == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = true)
            return
        }
        viewModelScope.launch {
            runCatching { repository.episodes(show) }
                .onSuccess { _uiState.value = _uiState.value.copy(episodes = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = true) }
        }
    }

    fun play(episode: Station) = playerManager.play(episode, _uiState.value.episodes)

    fun toggleFavorite(episode: Station) {
        viewModelScope.launch { favoritesRepository.toggle(episode) }
    }
}
