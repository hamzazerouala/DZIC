package com.jazairsoft.dzic.ui.screens.podcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.domain.model.PodcastCategory
import com.jazairsoft.dzic.domain.model.PodcastShow
import com.jazairsoft.dzic.ui.components.InlineSearchField
import com.jazairsoft.dzic.ui.components.ShowCard
import com.jazairsoft.dzic.ui.screens.radios.selectedChipColors

@Composable
fun PodcastsScreen(
    onOpenShow: (PodcastShow) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodcastsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {

        InlineSearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = stringResource(
                if (state.category.isAudiobook) R.string.books_search_hint
                else R.string.pod_search_hint
            )
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PodcastCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(stringResource(category.labelRes)) },
                    shape = RoundedCornerShape(50),
                    colors = selectedChipColors()
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.shows.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.error && state.shows.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.error_network))
                    Button(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                }

                state.shows.isEmpty() -> Text(
                    text = stringResource(R.string.no_result),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.shows, key = { it.id }) { show ->
                        ShowCard(
                            title = show.title,
                            author = show.author,
                            artworkUrl = show.artworkUrl,
                            identifier = show.id,
                            onClick = { onOpenShow(show) }
                        )
                    }
                }
            }
        }
    }
}
