package com.jazairsoft.dzic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jazairsoft.dzic.domain.model.Station

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tags: String,
    val country: String?,
    val countryCode: String?,
    val language: String?,
    val codec: String?,
    val bitrate: Int,
    val addedAt: Long
) {
    fun toStation(): Station = Station(
        id = id,
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
        fun from(station: Station): FavoriteEntity = FavoriteEntity(
            id = station.id,
            name = station.name,
            streamUrl = station.streamUrl,
            faviconUrl = station.faviconUrl,
            tags = station.tags.joinToString(","),
            country = station.country,
            countryCode = station.countryCode,
            language = station.language,
            codec = station.codec,
            bitrate = station.bitrate,
            addedAt = System.currentTimeMillis()
        )
    }
}
