package com.jazairsoft.dzic.ui.screens.music

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.domain.model.MusicCategory
import com.jazairsoft.dzic.ui.components.AddToPlaylistDialog
import com.jazairsoft.dzic.ui.components.InlineSearchField
import com.jazairsoft.dzic.ui.components.StationRow
import com.jazairsoft.dzic.ui.screens.radios.selectedChipColors

@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val pendingStation by viewModel.pendingStation.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {

        InlineSearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = stringResource(R.string.music_search_hint)
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusicCategory.entries.forEach { category ->
                FilterChip(
                    selected = !state.isSearching && state.category == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(stringResource(category.labelRes)) },
                    shape = RoundedCornerShape(50),
                    colors = selectedChipColors()
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.tracks.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.error && state.tracks.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.error_network))
                    Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                }

                state.tracks.isEmpty() -> Text(
                    text = stringResource(R.string.no_result),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = stringResource(R.string.tracks_count, state.tracks.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(state.tracks, key = { it.id }) { track ->
                        StationRow(
                            station = track,
                            isPlaying = player.station?.id == track.id,
                            isFavorite = favorites.contains(track.id),
                            onClick = { viewModel.play(track, state.tracks) },
                            onToggleFavorite = { viewModel.toggleFavorite(track) },
                            onAddToPlaylist = { viewModel.requestAddToPlaylist(track) }
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
