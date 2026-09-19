package com.jazairsoft.dzic.playback

import com.jazairsoft.dzic.domain.model.Station

data class PlayerUiState(
    val isConnected: Boolean = false,
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val errorMessage: String? = null
) {
    val hasContent: Boolean get() = station != null
}
