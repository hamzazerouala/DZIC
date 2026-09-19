package com.jazairsoft.dzic.ui.screens.radios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.remote.RadioBrowserRepository
import com.jazairsoft.dzic.domain.model.RadioCategory
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

data class RadiosUiState(
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val algerianStations: List<Station> = emptyList(),
    val selectedCategory: RadioCategory? = null,
    val categoryStations: List<Station> = emptyList(),
    val isCategoryLoading: Boolean = false
)

@HiltViewModel
class RadiosViewModel @Inject constructor(
    private val repository: RadioBrowserRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadiosUiState())
    val uiState: StateFlow<RadiosUiState> = _uiState.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playerState = playerManager.state

    init {
        loadAlgeria()
    }

    fun loadAlgeria(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = false)
            runCatching { repository.algerianStations(forceRefresh) }
                .onSuccess { stations ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = false,
                        algerianStations = stations
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = true)
                }
        }
    }

    fun selectCategory(category: RadioCategory?) {
        if (category == null) {
            _uiState.value = _uiState.value.copy(selectedCategory = null, categoryStations = emptyList())
            return
        }
        _uiState.value = _uiState.value.copy(selectedCategory = category, isCategoryLoading = true)
        viewModelScope.launch {
            runCatching { repository.categoryStations(category) }
                .onSuccess { stations ->
                    _uiState.value = _uiState.value.copy(
                        categoryStations = stations,
                        isCategoryLoading = false,
                        error = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isCategoryLoading = false, error = true)
                }
        }
    }

    fun play(station: Station, queue: List<Station>) = playerManager.play(station, queue)

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoritesRepository.toggle(station) }
    }
}
