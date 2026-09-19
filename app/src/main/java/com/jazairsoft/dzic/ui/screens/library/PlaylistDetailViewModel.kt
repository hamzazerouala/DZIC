package com.jazairsoft.dzic.ui.screens.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.local.PlaylistItemEntity
import com.jazairsoft.dzic.data.local.PlaylistsRepository
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

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistsRepository: PlaylistsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<String>("playlistId")?.toLongOrNull() ?: 0L

    val title: StateFlow<String> = playlistsRepository.playlist(playlistId)
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /**
     * Copie locale reordonnable : le glisser-deposer doit reagir a chaque
     * franchissement, sans attendre un aller-retour en base. L'ordre definitif
     * est ecrit quand le doigt est relache.
     */
    private val _items = MutableStateFlow<List<PlaylistItemEntity>>(emptyList())
    val items: StateFlow<List<PlaylistItemEntity>> = _items.asStateFlow()

    private var dragging = false

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playerState = playerManager.state

    init {
        viewModelScope.launch {
            playlistsRepository.items(playlistId).collect { fromDb ->
                if (!dragging) _items.value = fromDb
            }
        }
    }

    fun onDragStart() {
        dragging = true
    }

    fun move(from: Int, to: Int) {
        val current = _items.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        current.add(to, current.removeAt(from))
        _items.value = current
    }

    fun onDragEnd() {
        dragging = false
        val ids = _items.value.map { it.id }
        viewModelScope.launch { playlistsRepository.persistOrder(ids) }
    }

    fun play(station: Station) = playerManager.play(station, _items.value.map { it.toStation() })

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }

    fun removeItem(itemId: Long) {
        viewModelScope.launch { playlistsRepository.removeItem(itemId) }
    }
}
