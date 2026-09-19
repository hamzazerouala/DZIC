package com.jazairsoft.dzic.ui.screens.radios

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.domain.model.ArtistCatalog
import com.jazairsoft.dzic.domain.model.RadioCategory
import com.jazairsoft.dzic.ui.components.AddToPlaylistDialog
import com.jazairsoft.dzic.ui.components.InlineSearchField
import com.jazairsoft.dzic.ui.components.StationRow

@Composable
fun RadiosScreen(
    modifier: Modifier = Modifier,
    viewModel: RadiosViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val pendingStation by viewModel.pendingStation.collectAsStateWithLifecycle()

    var showArtists by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        InlineSearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = stringResource(R.string.search_hint)
        )

        // Filtres compacts en ligne : pays et langue en menus deroulants,
        // puis les categories thematiques et les artistes.
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.filter is RadioFilter.Algeria,
                onClick = { viewModel.apply(RadioFilter.Algeria) },
                label = { Text(stringResource(R.string.section_algeria)) },
                shape = RoundedCornerShape(50),
                colors = selectedChipColors()
            )

            DropdownChip(
                label = (state.filter as? RadioFilter.Country)?.label
                    ?: stringResource(R.string.filter_country),
                selected = state.filter is RadioFilter.Country,
                entries = state.countries.take(60).map { country ->
                    (country.name.orEmpty() + "  (" + (country.stationCount ?: 0) + ")") to {
                        viewModel.apply(RadioFilter.Country(country.code.orEmpty(), country.name.orEmpty()))
                    }
                }
            )

            DropdownChip(
                label = (state.filter as? RadioFilter.Language)?.name?.replaceFirstChar { it.uppercaseChar() }
                    ?: stringResource(R.string.filter_language),
                selected = state.filter is RadioFilter.Language,
                entries = state.languages.take(60).map { language ->
                    (language.name.orEmpty().replaceFirstChar { it.uppercaseChar() } +
                        "  (" + (language.stationCount ?: 0) + ")") to {
                        viewModel.apply(RadioFilter.Language(language.name.orEmpty()))
                    }
                }
            )

            FilterChip(
                selected = showArtists || state.filter is RadioFilter.Artist,
                onClick = { showArtists = !showArtists },
                label = { Text(stringResource(R.string.filter_artists)) },
                shape = RoundedCornerShape(50),
                colors = selectedChipColors()
            )

            RadioCategory.entries.forEach { category ->
                FilterChip(
                    selected = (state.filter as? RadioFilter.Category)?.category == category,
                    onClick = { viewModel.apply(RadioFilter.Category(category)) },
                    label = { Text(stringResource(category.labelRes)) },
                    shape = RoundedCornerShape(50),
                    colors = selectedChipColors()
                )
            }
        }

        if (showArtists) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArtistCatalog.artists.forEach { artist ->
                    FilterChip(
                        selected = (state.filter as? RadioFilter.Artist)?.artist?.query == artist.query,
                        onClick = { viewModel.apply(RadioFilter.Artist(artist)) },
                        label = { Text(artist.display) },
                        shape = RoundedCornerShape(50),
                        colors = selectedChipColors()
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.stations.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.error && state.stations.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.error_network))
                    Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                }

                state.stations.isEmpty() -> Text(
                    text = stringResource(R.string.no_result),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = stringResource(R.string.stations_count, state.stations.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(state.stations, key = { it.id }) { station ->
                        StationRow(
                            station = station,
                            isPlaying = player.station?.id == station.id,
                            isFavorite = favorites.contains(station.id),
                            onClick = { viewModel.play(station, state.stations) },
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

/** Chip qui ouvre un menu : garde les filtres sur une seule ligne compacte. */
@Composable
fun DropdownChip(
    label: String,
    selected: Boolean,
    entries: List<Pair<String, () -> Unit>>
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(50),
            colors = selectedChipColors()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 380.dp)
        ) {
            entries.forEach { (text, action) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
        }
    }
}

@Composable
fun selectedChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
)
