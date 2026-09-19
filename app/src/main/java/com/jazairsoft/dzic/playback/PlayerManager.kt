package com.jazairsoft.dzic.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val repository: RadioBrowserRepository,
    private val okHttpClient: OkHttpClient
) {

    private var controller: MediaController? = null
    private var connecting = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /** File d'attente courante, pour retrouver la Station depuis un MediaItem. */
    private var queue: List<Station> = emptyList()

    /**
     * Lecture demandee avant que le MediaController ne soit pret.
     * Sans ca, le premier appui sur une station ne declenche rien du tout.
     */
    private var pendingRequest: Pair<Station, List<Station>>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Erreur de lecture: ${error.errorCodeName}", error)
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = error.readableMessage()
            )
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            connecting = false
            runCatching { future.get() }
                .onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    refresh(mediaController)
                    // Rejoue la demande faite pendant la connexion.
                    pendingRequest?.let { (station, list) ->
                        pendingRequest = null
                        play(station, list)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Connexion au service de lecture impossible", error)
                    pendingRequest = null
                    _state.value = _state.value.copy(
                        isConnected = false,
                        isBuffering = false,
                        errorMessage = "Service de lecture indisponible : ${error.message ?: error::class.java.simpleName}"
                    )
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
        val player = controller
        if (player == null) {
            // On memorise la demande et on la rejoue des la connexion etablie.
            pendingRequest = station to stations
            _state.value = _state.value.copy(station = station, isBuffering = true, errorMessage = null)
            connect()
            return
        }

        val list = stations.ifEmpty { listOf(station) }
        _state.value = _state.value.copy(station = station, isBuffering = true, errorMessage = null)

        scope.launch {
            // Radio Browser renvoie parfois un .pls ou un .m3u : ExoPlayer ne sait
            // pas lire un fichier de playlist, il faut en extraire le flux reel.
            val resolved = list.map { candidate ->
                if (candidate.id == station.id) candidate.copy(streamUrl = resolveStreamUrl(candidate.streamUrl))
                else candidate
            }
            val index = resolved.indexOfFirst { it.id == station.id }.coerceAtLeast(0)

            queue = resolved
            player.setMediaItems(resolved.map { it.toMediaItem() }, index, 0L)
            player.prepare()
            player.play()
            repository.registerClick(station.id)
        }
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

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    /**
     * Suit un .pls / .m3u jusqu'au premier flux http reel.
     * Renvoie l'URL d'origine si ce n'est pas une playlist ou si la lecture echoue.
     */
    private suspend fun resolveStreamUrl(url: String): String {
        val lower = url.substringBefore('?').lowercase()
        val isPlaylist = lower.endsWith(".pls") || lower.endsWith(".m3u")
        if (!isPlaylist) return url

        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use url
                    val body = response.body?.string().orEmpty()
                    val stream = body.lineSequence()
                        .map { it.trim() }
                        .mapNotNull { line ->
                            when {
                                line.startsWith("http", ignoreCase = true) -> line
                                line.startsWith("File", ignoreCase = true) && line.contains('=') ->
                                    line.substringAfter('=').trim()
                                else -> null
                            }
                        }
                        .firstOrNull { it.startsWith("http", ignoreCase = true) }
                    stream ?: url
                }
            }.getOrDefault(url)
        }
    }

    private fun refresh(player: Player) {
        val currentId = player.currentMediaItem?.mediaId
        val station = queue.firstOrNull { it.id == currentId } ?: _state.value.station
        val error = player.playerError
        _state.value = PlayerUiState(
            isConnected = true,
            station = if (player.mediaItemCount == 0 && error == null) null else station,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            errorMessage = error?.readableMessage() ?: _state.value.errorMessage
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
        .build()

    private companion object {
        const val TAG = "DzicPlayer"
        const val USER_AGENT = "DZIC/0.1 (Android; Jazairsoft)"
    }
}

/** Message lisible a afficher : le code brut ne dit rien a l'utilisateur. */
private fun PlaybackException.readableMessage(): String = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Connexion au flux impossible (reseau)"
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "Le serveur de la station a refuse la connexion"
    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
        "Flux HTTP bloque par le systeme"
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "Flux introuvable (station hors service)"
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
        "Format de flux non pris en charge"
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
        "Codec audio non pris en charge"
    else -> "Lecture impossible ($errorCodeName)"
}
