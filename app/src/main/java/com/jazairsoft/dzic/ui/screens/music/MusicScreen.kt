package com.jazairsoft.dzic.ui.screens.music

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.domain.model.MusicCategory
import com.jazairsoft.dzic.ui.components.AddToPlaylistDialog
import com.jazairsoft.dzic.ui.components.InlineSearchField
import com.jazairsoft.dzic.ui.components.MediaGrid
import com.jazairsoft.dzic.ui.screens.radios.selectedChipColors

@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val unavailable by viewModel.unavailableIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val pendingStation by viewModel.pendingStation.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(notice) {
        val message = notice ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, context.getString(message), android.widget.Toast.LENGTH_LONG).show()
        viewModel.clearNotice()
    }

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

                else -> MediaGrid(
                    items = state.tracks,
                    playingId = player.station?.id,
                    favoriteIds = favorites,
                    onPlay = { viewModel.play(it, state.tracks) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    unavailableIds = unavailable,
                    downloadStates = downloadStates,
                    onDownload = viewModel::download,
                    onAddToPlaylist = viewModel::requestAddToPlaylist
                )
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
