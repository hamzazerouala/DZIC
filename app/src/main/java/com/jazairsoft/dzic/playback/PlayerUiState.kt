package com.jazairsoft.dzic.playback

import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.Station

data class PlayerUiState(
    val isConnected: Boolean = false,
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val errorMessage: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val hasContent: Boolean get() = station != null

    /** Une radio n'a ni duree ni position : pas de barre de progression. */
    val isSeekable: Boolean
        get() = station != null && station.kind != MediaKind.RADIO && durationMs > 0

    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
