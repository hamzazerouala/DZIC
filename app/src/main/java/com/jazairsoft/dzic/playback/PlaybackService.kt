package com.jazairsoft.dzic.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.jazairsoft.dzic.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Foreground service dedie a la lecture.
 * C'est le seul mecanisme fiable pour survivre au mode Doze, a l'ecran
 * verrouille et au retrait de l'app des recentes.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var okHttpClient: OkHttpClient

    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(USER_AGENT)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        val player = ExoPlayer.Builder(this)
            // RadioExtractorsFactory choisit l'extracteur d'apres le Content-Type
            // renvoye par le serveur, pas d'apres l'extension de l'URL.
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory, RadioExtractorsFactory())
            )
            // true => ExoPlayer gere l'AudioFocus : pause sur appel entrant,
            // duck sur notification sonore, reprise ensuite.
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Explicite : elimine toute ambiguite sur un volume interne a zero.
        player.volume = 1f

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Erreur ExoPlayer: ${error.errorCodeName}", error)
                // On ne retente que sur incident reseau. Un format non lisible
                // ne deviendra pas lisible en reessayant : le relancer masquerait
                // l'erreur au lieu de la remonter a l'utilisateur.
                if (error.isRetriable() && retryCount < MAX_RETRIES) {
                    retryCount++
                    handler.postDelayed({
                        player.prepare()
                        player.play()
                    }, RETRY_DELAY_MS * retryCount)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) retryCount = 0
            }

            override fun onMediaItemTransition(
                mediaItem: androidx.media3.common.MediaItem?,
                reason: Int
            ) {
                retryCount = 0
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun PlaybackException.isRetriable(): Boolean = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> true
        else -> false
    }

    private companion object {
        const val TAG = "DzicService"
        const val USER_AGENT = "DZIC/0.1 (Android; Jazairsoft)"
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 2000L
    }
}
