package com.jazairsoft.dzic.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jazairsoft.dzic.domain.model.Station

/**
 * Grille de tuiles. Deux colonnes sur telephone : au-dela, la pochette
 * devient trop petite pour servir de reperage.
 */
@Composable
fun MediaGrid(
    items: List<Station>,
    playingId: String?,
    favoriteIds: Set<String>,
    onPlay: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
    downloadStates: Map<String, String> = emptyMap(),
    onDownload: ((Station) -> Unit)? = null,
    onAddToPlaylist: ((Station) -> Unit)? = null,
    onRemove: ((Station) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { station ->
            MediaCard(
                station = station,
                isPlaying = playingId == station.id,
                isFavorite = favoriteIds.contains(station.id),
                onClick = { onPlay(station) },
                onToggleFavorite = { onToggleFavorite(station) },
                downloadState = downloadStates[station.id],
                onDownload = onDownload?.let { action -> { action(station) } },
                onAddToPlaylist = onAddToPlaylist?.let { action -> { action(station) } },
                onRemove = onRemove?.let { action -> { action(station) } }
            )
        }
    }
}
