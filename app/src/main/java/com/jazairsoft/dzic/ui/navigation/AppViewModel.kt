package com.jazairsoft.dzic.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.remote.PodcastSelection
import com.jazairsoft.dzic.domain.model.PodcastShow
import com.jazairsoft.dzic.playback.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val favoritesRepository: FavoritesRepository,
    private val podcastSelection: PodcastSelection
) : ViewModel() {

    val playerState = playerManager.state

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun stop() = playerManager.stop()
    fun clearError() = playerManager.clearError()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)

    fun selectShow(show: PodcastShow) {
        podcastSelection.current = show
    }

    fun toggleFavoriteCurrent() {
        val station = playerManager.state.value.station ?: return
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }
}
