package com.jazairsoft.dzic.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.ui.components.AddToPlaylistDialog
import com.jazairsoft.dzic.ui.components.MediaGrid

@Composable
fun LibraryScreen(
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val section by viewModel.section.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val pendingStation by viewModel.pendingStation.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tab_library),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            if (section == LibrarySection.PLAYLISTS) {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_playlist))
                }
            }
            if (section == LibrarySection.HISTORY && history.isNotEmpty()) {
                IconButton(onClick = viewModel::clearHistory) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.clear_history))
                }
            }
        }

        // Filtres compacts en ligne, au meme gabarit que l'onglet Radios.
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionChip(LibrarySection.FAVORITES, R.string.tab_favorites, section, viewModel::selectSection)
            SectionChip(LibrarySection.PLAYLISTS, R.string.playlists, section, viewModel::selectSection)
            SectionChip(LibrarySection.DOWNLOADS, R.string.downloads, section, viewModel::selectSection)
            SectionChip(LibrarySection.HISTORY, R.string.history, section, viewModel::selectSection)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (section) {
                LibrarySection.FAVORITES -> StationList(
                    stations = favorites,
                    emptyText = stringResource(R.string.favorites_empty),
                    playingId = player.station?.id,
                    favoriteIds = favoriteIds,
                    onPlay = { viewModel.play(it, favorites) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onAddToPlaylist = viewModel::requestAddToPlaylist
                )

                LibrarySection.DOWNLOADS -> StationList(
                    stations = downloads,
                    emptyText = stringResource(R.string.downloads_empty),
                    playingId = player.station?.id,
                    favoriteIds = favoriteIds,
                    downloadStates = downloadStates,
                    onPlay = { viewModel.play(it, downloads) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onAddToPlaylist = viewModel::requestAddToPlaylist,
                    onRemove = { viewModel.removeDownload(it.id) }
                )

                LibrarySection.HISTORY -> StationList(
                    stations = history,
                    emptyText = stringResource(R.string.history_empty),
                    playingId = player.station?.id,
                    favoriteIds = favoriteIds,
                    onPlay = { viewModel.play(it, history) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onAddToPlaylist = viewModel::requestAddToPlaylist,
                    onRemove = { viewModel.removeFromHistory(it.id) }
                )

                LibrarySection.PLAYLISTS -> {
                    if (playlists.isEmpty()) {
                        EmptyState(stringResource(R.string.playlists_empty))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(playlists, key = { it.id }) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenPlaylist(playlist.id) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = stringResource(R.string.stations_count, playlist.itemCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.delete_playlist),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.new_playlist)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(name)
                        showCreateDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionChip(
    value: LibrarySection,
    labelRes: Int,
    current: LibrarySection,
    onSelect: (LibrarySection) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(stringResource(labelRes)) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun StationList(
    stations: List<com.jazairsoft.dzic.domain.model.Station>,
    emptyText: String,
    playingId: String?,
    favoriteIds: Set<String>,
    onPlay: (com.jazairsoft.dzic.domain.model.Station) -> Unit,
    onToggleFavorite: (com.jazairsoft.dzic.domain.model.Station) -> Unit,
    onAddToPlaylist: (com.jazairsoft.dzic.domain.model.Station) -> Unit,
    downloadStates: Map<String, String> = emptyMap(),
    onRemove: ((com.jazairsoft.dzic.domain.model.Station) -> Unit)? = null
) {
    if (stations.isEmpty()) {
        EmptyState(emptyText)
        return
    }
    MediaGrid(
        items = stations,
        playingId = playingId,
        favoriteIds = favoriteIds,
        onPlay = onPlay,
        onToggleFavorite = onToggleFavorite,
        downloadStates = downloadStates,
        onAddToPlaylist = onAddToPlaylist,
        onRemove = onRemove
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp)
        )
    }
}
