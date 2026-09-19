package com.jazairsoft.dzic.ui.screens.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jazairsoft.dzic.data.remote.PodcastRepository
import com.jazairsoft.dzic.domain.model.PodcastCategory
import com.jazairsoft.dzic.domain.model.PodcastShow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastsUiState(
    val category: PodcastCategory = PodcastCategory.AUDIOBOOKS,
    val shows: List<PodcastShow> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val query: String = "",
    val isSearching: Boolean = false
)

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    private val repository: PodcastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastsUiState())
    val uiState: StateFlow<PodcastsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        selectCategory(PodcastCategory.AUDIOBOOKS)
    }

    fun selectCategory(category: PodcastCategory, forceRefresh: Boolean = false) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            category = category,
            isLoading = true,
            error = false,
            isSearching = false
        )
        viewModelScope.launch {
            runCatching { repository.shows(category, forceRefresh) }
                .onSuccess { _uiState.value = _uiState.value.copy(shows = it, isLoading = false) }
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
            val searchingBooks = _uiState.value.category.isAudiobook
            runCatching {
                if (searchingBooks) repository.searchBooks(value) else repository.search(value)
            }
                .onSuccess { _uiState.value = _uiState.value.copy(shows = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = true) }
        }
    }

    fun retry() = if (_uiState.value.isSearching) onQueryChange(_uiState.value.query)
    else selectCategory(_uiState.value.category, forceRefresh = true)

    /** Le detail d'une emission a besoin de l'objet complet, pas seulement d'un id. */
    fun showById(id: String): PodcastShow? = _uiState.value.shows.firstOrNull { it.id == id }
}
