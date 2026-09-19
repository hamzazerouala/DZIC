package com.jazairsoft.dzic.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.ui.components.AddToPlaylistDialog
import com.jazairsoft.dzic.ui.components.StationRow

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val pendingStation by viewModel.pendingStation.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.error -> Text(
                    text = stringResource(R.string.error_network),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                !state.hasSearched -> Text(
                    text = stringResource(R.string.search_empty),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                state.results.isEmpty() -> Text(
                    text = stringResource(R.string.no_result),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.id }) { station ->
                        StationRow(
                            station = station,
                            isPlaying = player.station?.id == station.id,
                            isFavorite = favorites.contains(station.id),
                            onClick = { viewModel.play(station, state.results) },
                            onToggleFavorite = { viewModel.toggleFavorite(station) },
                            onAddToPlaylist = { viewModel.requestAddToPlaylist(station) }
                        )
                    }
                }
            }
        }
    }

    if (pendingStation != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = viewModel::dismissAddToPlaylist,
            onSelect = viewModel::addPendingTo,
            onCreate = viewModel::createPlaylistWithPending
        )
    }
}
