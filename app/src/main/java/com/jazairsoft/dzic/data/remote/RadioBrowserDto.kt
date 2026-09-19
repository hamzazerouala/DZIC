package com.jazairsoft.dzic.data.remote

import com.google.gson.annotations.SerializedName
import com.jazairsoft.dzic.domain.model.Station

data class StationDto(
    @SerializedName("stationuuid") val stationUuid: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("url_resolved") val urlResolved: String?,
    @SerializedName("favicon") val favicon: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("countrycode") val countryCode: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("codec") val codec: String?,
    @SerializedName("bitrate") val bitrate: Int?,
    @SerializedName("votes") val votes: Int?,
    @SerializedName("clickcount") val clickCount: Int?,
    @SerializedName("lastcheckok") val lastCheckOk: Int?
) {
    fun toStation(): Station? {
        val id = stationUuid?.takeIf { it.isNotBlank() } ?: return null
        val title = name?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val stream = (urlResolved?.takeIf { it.isNotBlank() } ?: url)?.takeIf { it.isNotBlank() } ?: return null
        return Station(
            id = id,
            name = title,
            streamUrl = stream,
            faviconUrl = favicon?.takeIf { it.isNotBlank() && it.startsWith("http") },
            tags = tags.orEmpty().split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() },
            country = country?.takeIf { it.isNotBlank() },
            countryCode = countryCode?.takeIf { it.isNotBlank() },
            language = language?.takeIf { it.isNotBlank() },
            codec = codec,
            bitrate = bitrate ?: 0,
            votes = votes ?: 0,
            clickCount = clickCount ?: 0
        )
    }
}

data class ServerDto(
    @SerializedName("name") val name: String?
)
