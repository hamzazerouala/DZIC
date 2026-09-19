package com.jazairsoft.dzic.domain.model

/** Nature du media : conditionne l'UI (barre de progression, duree, reprise). */
enum class MediaKind { RADIO, TRACK, EPISODE }

/**
 * Element lisible, quelle que soit sa source.
 *
 * Le nom "Station" vient de la V1 qui ne gerait que la radio. Il couvre
 * desormais aussi les morceaux et les episodes de podcast : le renommer
 * touche une quinzaine de fichiers et sera fait dans une passe dediee.
 */
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tags: List<String>,
    val country: String?,
    val countryCode: String?,
    val language: String?,
    val codec: String?,
    val bitrate: Int,
    val votes: Int,
    val clickCount: Int,
    val kind: MediaKind = MediaKind.RADIO,
    val artist: String? = null,
    val durationMs: Long = 0L,
    val sourceLabel: String? = null,
    /** Renseigne quand l'URL de lecture doit etre resolue au moment du clic. */
    val needsResolution: Boolean = false
) {
    val isAlgerian: Boolean get() = countryCode.equals("DZ", ignoreCase = true)

    val isLive: Boolean get() = kind == MediaKind.RADIO

    val subtitle: String
        get() = when (kind) {
            MediaKind.RADIO -> buildList {
                country?.takeIf { it.isNotBlank() }?.let { add(it) }
                tags.take(2).forEach { add(it.replaceFirstChar { c -> c.uppercaseChar() }) }
                if (bitrate > 0) add("${bitrate}k")
            }.joinToString(" · ")

            MediaKind.TRACK -> buildList {
                artist?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (durationMs > 0) add(formatDuration(durationMs))
                sourceLabel?.let { add(it) }
            }.joinToString(" · ")

            MediaKind.EPISODE -> buildList {
                artist?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (durationMs > 0) add(formatDuration(durationMs))
            }.joinToString(" · ")
        }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}
