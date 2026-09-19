package com.jazairsoft.dzic.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.jazairsoft.dzic.data.remote.RadioBrowserRepository
import com.jazairsoft.dzic.domain.model.Station
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pont unique entre l'UI Compose et le MediaSessionService.
 * Toutes les commandes passent par un MediaController : l'etat reste
 * coherent quel que soit le point de controle (app, notification,
 * ecran verrouille, casque Bluetooth).
 */
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RadioBrowserRepository
) {

    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /** File d'attente courante, pour retrouver la Station depuis un MediaItem. */
    private var queue: List<Station> = emptyList()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh(player)
        }
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }.onSuccess { mediaController ->
                controller = mediaController
                mediaController.addListener(listener)
                refresh(mediaController)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _state.value = PlayerUiState()
    }

    /** Lance [station] et charge [stations] comme file d'attente (precedent/suivant). */
    fun play(station: Station, stations: List<Station>) {
        val player = controller ?: run { connect(); return }
        val list = stations.ifEmpty { listOf(station) }
        val index = list.indexOfFirst { it.id == station.id }.coerceAtLeast(0)

        if (queue.map { it.id } == list.map { it.id }) {
            player.seekTo(index, 0L)
        } else {
            queue = list
            player.setMediaItems(list.map { it.toMediaItem() }, index, 0L)
        }
        player.prepare()
        player.play()
        scope.launch { repository.registerClick(station.id) }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun next() {
        val player = controller ?: return
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun previous() {
        val player = controller ?: return
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
    }

    fun stop() {
        val player = controller ?: return
        player.stop()
        player.clearMediaItems()
        queue = emptyList()
        _state.value = PlayerUiState(isConnected = true)
    }

    private fun refresh(player: Player) {
        val currentId = player.currentMediaItem?.mediaId
        val station = queue.firstOrNull { it.id == currentId }
        _state.value = PlayerUiState(
            isConnected = true,
            station = station,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            errorMessage = player.playerError?.errorCodeName
        )
    }

    private fun Station.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(subtitle.ifBlank { country.orEmpty() })
                .setStation(name)
                .setArtworkUri(faviconUrl?.let { Uri.parse(it) })
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build()
        )
        .setLiveConfiguration(
            MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(5_000L)
                .build()
        )
        .build()
}
