package com.jazairsoft.dzic.ui.screens.library

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jazairsoft.dzic.R
import com.jazairsoft.dzic.ui.components.StationRow

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val dragState = rememberDragDropState(
        listState = listState,
        onDragStart = viewModel::onDragStart,
        onMove = viewModel::move,
        onDragEnd = viewModel::onDragEnd
    )

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.stations_count, items.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.playlist_empty),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Column
        }

        Text(
            text = stringResource(R.string.reorder_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = dragState::onDragStart,
                        onDrag = { change, offset ->
                            change.consume()
                            dragState.onDrag(offset.y)
                        },
                        onDragEnd = dragState::onDragEnd,
                        onDragCancel = dragState::onDragEnd
                    )
                }
        ) {
            items(items, key = { it.id }) { item ->
                val index = items.indexOfFirst { it.id == item.id }
                val isDragged = index == dragState.draggingIndex
                val station = item.toStation()

                StationRow(
                    station = station,
                    isPlaying = player.station?.id == station.id,
                    isFavorite = favoriteIds.contains(station.id),
                    onClick = { viewModel.play(station) },
                    onToggleFavorite = { viewModel.toggleFavorite(station) },
                    onRemove = { viewModel.removeItem(item.id) },
                    trailing = {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.reorder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) dragState.draggingOffset else 0f
                        }
                        .then(
                            if (isDragged) Modifier.shadow(8.dp) else Modifier
                        )
                )
            }
        }
    }
}

/**
 * Glisser-deposer sur LazyColumn.
 * On travaille sur les index visibles fournis par le LazyListState : des que le
 * centre de l'element tire franchit un voisin, on declenche l'echange. L'ordre
 * n'est ecrit en base qu'au relachement, pour ne pas marteler la base pendant
 * le geste.
 */
class DragDropState internal constructor(
    private val listState: LazyListState,
    private val onDragStartCallback: () -> Unit,
    private val onMove: (Int, Int) -> Unit,
    private val onDragEndCallback: () -> Unit
) {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    private var initialOffset = 0
    private var draggedDelta by mutableFloatStateOf(0f)

    private val draggingItem: LazyListItemInfo?
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingIndex }

    val draggingOffset: Float
        get() = draggingItem?.let { initialOffset + draggedDelta - it.offset } ?: 0f

    fun onDragStart(offset: androidx.compose.ui.geometry.Offset) {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also {
                draggingIndex = it.index
                initialOffset = it.offset
                draggedDelta = 0f
                onDragStartCallback()
            }
    }

    fun onDrag(deltaY: Float) {
        draggedDelta += deltaY
        val current = draggingItem ?: return
        val startOffset = current.offset + draggingOffset
        val middle = startOffset + current.size / 2f

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            middle.toInt() in candidate.offset..(candidate.offset + candidate.size) &&
                candidate.index != current.index
        } ?: return

        onMove(current.index, target.index)
        draggingIndex = target.index
    }

    fun onDragEnd() {
        if (draggingIndex != null) onDragEndCallback()
        draggingIndex = null
        draggedDelta = 0f
    }
}

@Composable
fun rememberDragDropState(
    listState: LazyListState,
    onDragStart: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit
): DragDropState = remember(listState) {
    DragDropState(listState, onDragStart, onMove, onDragEnd)
}
