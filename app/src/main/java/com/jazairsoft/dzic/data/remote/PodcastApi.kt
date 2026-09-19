package com.jazairsoft.dzic.data.remote

import com.google.gson.annotations.SerializedName
import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.PodcastShow
import com.jazairsoft.dzic.domain.model.Station
import retrofit2.http.GET
import retrofit2.http.Query

// ---------- iTunes Search : catalogue de podcasts, sans cle ----------

interface ItunesApi {
    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 50,
        @Query("country") country: String = "FR"
    ): ItunesResponse
}

data class ItunesResponse(
    @SerializedName("resultCount") val resultCount: Int?,
    @SerializedName("results") val results: List<ItunesPodcastDto>?
)

data class ItunesPodcastDto(
    @SerializedName("collectionId") val collectionId: Long?,
    @SerializedName("collectionName") val collectionName: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("artworkUrl600") val artworkUrl600: String?,
    @SerializedName("artworkUrl100") val artworkUrl100: String?,
    @SerializedName("feedUrl") val feedUrl: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("collectionExplicitness") val explicitness: String?
) {
    val isExplicit: Boolean get() = explicitness.equals("explicit", ignoreCase = true)

    fun toShow(): PodcastShow? {
        val id = collectionId ?: return null
        val title = collectionName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val feed = feedUrl?.takeIf { it.startsWith("http") } ?: return null
        return PodcastShow(
            id = "it:$id",
            title = title,
            author = artistName?.trim(),
            artworkUrl = artworkUrl600 ?: artworkUrl100,
            feedUrl = feed,
            genres = genres.orEmpty()
        )
    }
}

// ---------- LibriVox : livres audio du domaine public ----------

interface LibriVoxApi {
    @GET("api/feed/audiobooks/")
    suspend fun books(
        @Query("format") format: String = "json",
        @Query("extended") extended: Int = 1,
        @Query("limit") limit: Int = 40,
        @Query("offset") offset: Int = 0,
        @Query("title") title: String? = null,
        @Query("author") author: String? = null
    ): LibriVoxResponse
}

data class LibriVoxResponse(
    @SerializedName("books") val books: List<LibriVoxBookDto>?
)

data class LibriVoxBookDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("totaltime") val totalTime: String?,
    @SerializedName("url_rss") val urlRss: String?,
    @SerializedName("authors") val authors: List<LibriVoxAuthorDto>?,
    @SerializedName("sections") val sections: List<LibriVoxSectionDto>?
) {
    val authorName: String?
        get() = authors?.firstOrNull()?.let { author ->
            listOfNotNull(author.firstName?.trim(), author.lastName?.trim())
                .joinToString(" ")
                .takeIf { it.isNotBlank() }
        }

    fun toShow(): PodcastShow? {
        val identifier = id?.takeIf { it.isNotBlank() } ?: return null
        val label = title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (sections.orEmpty().none { !it.listenUrl.isNullOrBlank() }) return null
        return PodcastShow(
            id = "lv:$identifier",
            title = label,
            author = authorName,
            artworkUrl = null,
            feedUrl = urlRss,
            genres = listOfNotNull(language),
            isAudiobook = true,
            description = description?.replace(Regex("<[^>]*>"), "")?.trim()
        )
    }

    fun chapters(): List<Station> = sections.orEmpty().mapNotNull { it.toStation(title.orEmpty()) }
}

data class LibriVoxAuthorDto(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?
)

data class LibriVoxSectionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("section_number") val sectionNumber: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("listen_url") val listenUrl: String?,
    @SerializedName("playtime") val playtime: String?
) {
    fun toStation(bookTitle: String): Station? {
        val identifier = id?.takeIf { it.isNotBlank() } ?: return null
        val stream = listenUrl?.takeIf { it.startsWith("http") } ?: return null
        val label = title?.trim()?.takeIf { it.isNotBlank() }
            ?: sectionNumber?.let { "Chapitre $it" }
            ?: return null
        return Station(
            id = "lvs:$identifier",
            name = label,
            streamUrl = stream,
            faviconUrl = null,
            tags = emptyList(),
            country = null,
            countryCode = null,
            language = null,
            codec = "MP3",
            bitrate = 0,
            votes = 0,
            clickCount = 0,
            kind = MediaKind.EPISODE,
            artist = bookTitle,
            durationMs = (playtime?.toLongOrNull() ?: 0L) * 1000L
        )
    }
}
