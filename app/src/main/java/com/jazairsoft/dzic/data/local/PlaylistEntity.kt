package com.jazairsoft.dzic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jazairsoft.dzic.domain.model.Station

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

/**
 * Un element de playlist embarque une copie de la station.
 * Ainsi une playlist reste lisible hors connexion a l'API Radio Browser,
 * et survit a la disparition d'une station de la base communautaire.
 */
@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val position: Int,
    val stationId: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tags: String,
    val country: String?,
    val countryCode: String?,
    val language: String?,
    val codec: String?,
    val bitrate: Int
) {
    fun toStation(): Station = Station(
        id = stationId,
        name = name,
        streamUrl = streamUrl,
        faviconUrl = faviconUrl,
        tags = tags.split(",").filter { it.isNotBlank() },
        country = country,
        countryCode = countryCode,
        language = language,
        codec = codec,
        bitrate = bitrate,
        votes = 0,
        clickCount = 0
    )

    companion object {
        fun from(playlistId: Long, position: Int, station: Station) = PlaylistItemEntity(
            playlistId = playlistId,
            position = position,
            stationId = station.id,
            name = station.name,
            streamUrl = station.streamUrl,
            faviconUrl = station.faviconUrl,
            tags = station.tags.joinToString(","),
            country = station.country,
            countryCode = station.countryCode,
            language = station.language,
            codec = station.codec,
            bitrate = station.bitrate
        )
    }
}

/** Playlist + nombre d'elements, pour l'affichage de la liste. */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val itemCount: Int
)
