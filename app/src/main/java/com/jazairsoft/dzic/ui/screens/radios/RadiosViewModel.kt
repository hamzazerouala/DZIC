package com.jazairsoft.dzic.ui.screens.radios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.local.FavoritesRepository
import com.jazairsoft.dzic.data.local.PlaylistWithCount
import com.jazairsoft.dzic.data.local.PlaylistsRepository
import com.jazairsoft.dzic.data.remote.CountryDto
import com.jazairsoft.dzic.data.remote.LanguageDto
import com.jazairsoft.dzic.data.remote.RadioBrowserRepository
import com.jazairsoft.dzic.domain.model.ArtistRadio
import com.jazairsoft.dzic.domain.model.RadioCategory
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

/** Ce que la liste affiche actuellement. */
sealed interface RadioFilter {
    data object Algeria : RadioFilter
    data class Category(val category: RadioCategory) : RadioFilter
    data class Artist(val artist: ArtistRadio) : RadioFilter
    data class Country(val code: String, val label: String) : RadioFilter
    data class Language(val name: String) : RadioFilter
    data class Search(val query: String) : RadioFilter
}

data class RadiosUiState(
    val filter: RadioFilter = RadioFilter.Algeria,
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val query: String = "",
    val countries: List<CountryDto> = emptyList(),
    val languages: List<LanguageDto> = emptyList()
)

@HiltViewModel
class RadiosViewModel @Inject constructor(
    private val repository: RadioBrowserRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playlistsRepository: PlaylistsRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadiosUiState())
    val uiState: StateFlow<RadiosUiState> = _uiState.asStateFlow()

    private val _pendingStation = MutableStateFlow<Station?>(null)
    val pendingStation: StateFlow<Station?> = _pendingStation.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<PlaylistWithCount>> = playlistsRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playerState = playerManager.state

    private var searchJob: Job? = null

    init {
        apply(RadioFilter.Algeria)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                countries = repository.countries(),
                languages = repository.languages()
            )
        }
    }

    fun apply(filter: RadioFilter, forceRefresh: Boolean = false) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(filter = filter, isLoading = true, error = false)
        viewModelScope.launch {
            runCatching {
                when (filter) {
                    is RadioFilter.Algeria -> repository.algerianStations(forceRefresh)
                    is RadioFilter.Category -> repository.categoryStations(filter.category, forceRefresh)
                    is RadioFilter.Artist -> repository.search(filter.artist.query)
                    is RadioFilter.Country -> repository.stationsOfCountry(filter.code, forceRefresh)
                    is RadioFilter.Language -> repository.stationsOfLanguage(filter.name, forceRefresh)
                    is RadioFilter.Search -> repository.search(filter.query)
                }
            }
                .onSuccess { stations ->
                    _uiState.value = _uiState.value.copy(
                        stations = stations,
                        isLoading = false,
                        error = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = true)
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        searchJob?.cancel()
        if (value.trim().length < 2) {
            if (_uiState.value.filter is RadioFilter.Search) apply(RadioFilter.Algeria)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce : evite un appel API a chaque frappe
            apply(RadioFilter.Search(value.trim()))
        }
    }

    fun retry() = apply(_uiState.value.filter, forceRefresh = true)

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
