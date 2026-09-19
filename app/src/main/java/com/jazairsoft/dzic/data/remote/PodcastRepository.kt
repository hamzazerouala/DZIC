package com.jazairsoft.dzic.data.remote

import android.util.Xml
import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.PodcastCategory
import com.jazairsoft.dzic.domain.model.PodcastShow
import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    private val itunes: ItunesApi,
    private val libriVox: LibriVoxApi,
    private val okHttpClient: OkHttpClient
) {
    private val showsCache = mutableMapOf<String, List<PodcastShow>>()
    private val booksCache = mutableMapOf<String, LibriVoxBookDto>()

    suspend fun shows(category: PodcastCategory, forceRefresh: Boolean = false): List<PodcastShow> {
        showsCache[category.key]?.takeIf { !forceRefresh && it.isNotEmpty() }?.let { return it }

        val result = if (category.isAudiobook) audiobooks() else podcasts(category)
        showsCache[category.key] = result
        return result
    }

    private suspend fun podcasts(category: PodcastCategory): List<PodcastShow> =
        withContext(Dispatchers.IO) {
            runCatching { itunes.searchPodcasts(term = category.query).results.orEmpty() }
                .getOrDefault(emptyList())
                // Le flag explicit est un champ standard des flux podcast :
                // on l'applique strictement, independamment de la categorie.
                .filterNot { it.isExplicit }
                .mapNotNull { it.toShow() }
                .filter { it.isAllowed(category) }
                .distinctBy { it.id }
        }

    private suspend fun audiobooks(): List<PodcastShow> = withContext(Dispatchers.IO) {
        runCatching { libriVox.books(limit = 60).books.orEmpty() }
            .getOrDefault(emptyList())
            .onEach { book -> book.id?.let { booksCache["lv:$it"] = book } }
            .mapNotNull { it.toShow() }
            .filter { it.isAllowed(null) }
    }

    suspend fun search(query: String): List<PodcastShow> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching { itunes.searchPodcasts(term = query.trim()).results.orEmpty() }
            .getOrDefault(emptyList())
            .filterNot { it.isExplicit }
            .mapNotNull { it.toShow() }
            .filter { it.isAllowed(null) }
            .distinctBy { it.id }
    }

    suspend fun searchBooks(query: String): List<PodcastShow> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching { libriVox.books(limit = 40, title = query.trim()).books.orEmpty() }
            .getOrDefault(emptyList())
            .onEach { book -> book.id?.let { booksCache["lv:$it"] = book } }
            .mapNotNull { it.toShow() }
            .filter { it.isAllowed(null) }
    }

    /** Episodes d'une emission, ou chapitres d'un livre. */
    suspend fun episodes(show: PodcastShow): List<Station> = withContext(Dispatchers.IO) {
        if (show.isAudiobook) {
            val cached = booksCache[show.id]
            if (cached != null) return@withContext cached.chapters()
            val numericId = show.id.removePrefix("lv:")
            val book = runCatching { libriVox.books(limit = 1, title = show.title).books.orEmpty() }
                .getOrDefault(emptyList())
                .firstOrNull { it.id == numericId }
            return@withContext book?.chapters().orEmpty()
        }

        val feed = show.feedUrl ?: return@withContext emptyList()
        runCatching { parseFeed(feed, show) }.getOrDefault(emptyList())
    }

    /**
     * Lecture du flux RSS avec XmlPullParser : disponible nativement sur
     * Android, aucune bibliotheque supplementaire a embarquer.
     */
    private fun parseFeed(feedUrl: String, show: PodcastShow): List<Station> {
        val request = Request.Builder().url(feedUrl).build()
        val xml = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.string() ?: return emptyList()
        }

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val episodes = mutableListOf<Station>()
        var inItem = false
        var title: String? = null
        var enclosure: String? = null
        var duration = 0L
        var guid: String? = null
        var explicit = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && episodes.size < MAX_EPISODES) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "item" -> {
                        inItem = true
                        title = null; enclosure = null; duration = 0L; guid = null; explicit = false
                    }
                    "title" -> if (inItem) title = parser.nextTextSafe()
                    "guid" -> if (inItem) guid = parser.nextTextSafe()
                    "enclosure" -> if (inItem) {
                        val type = parser.getAttributeValue(null, "type").orEmpty()
                        val url = parser.getAttributeValue(null, "url")
                        if (!url.isNullOrBlank() && (type.startsWith("audio") || type.isBlank())) {
                            enclosure = url
                        }
                    }
                    "itunes:duration" -> if (inItem) duration = parseDuration(parser.nextTextSafe())
                    "itunes:explicit" -> if (inItem) {
                        val value = parser.nextTextSafe()?.lowercase()
                        explicit = value == "yes" || value == "true"
                    }
                }

                XmlPullParser.END_TAG -> if (parser.name.equals("item", ignoreCase = true)) {
                    inItem = false
                    val name = title?.trim()
                    val stream = enclosure
                    if (!name.isNullOrBlank() && !stream.isNullOrBlank() && !explicit) {
                        episodes += Station(
                            id = "ep:" + (guid?.takeIf { it.isNotBlank() } ?: stream),
                            name = name,
                            streamUrl = stream,
                            faviconUrl = show.artworkUrl,
                            tags = emptyList(),
                            country = null,
                            countryCode = null,
                            language = null,
                            codec = null,
                            bitrate = 0,
                            votes = 0,
                            clickCount = 0,
                            kind = MediaKind.EPISODE,
                            artist = show.title,
                            durationMs = duration
                        )
                    }
                }
            }
            event = parser.next()
        }
        return episodes
    }

    private fun XmlPullParser.nextTextSafe(): String? = runCatching { nextText() }.getOrNull()

    /** "1:02:03", "62:03" ou "3723" selon les flux. */
    private fun parseDuration(raw: String?): Long {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return 0L
        return if (value.contains(':')) {
            val parts = value.split(':').mapNotNull { it.trim().toLongOrNull() }
            when (parts.size) {
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                2 -> (parts[0] * 60 + parts[1]) * 1000
                else -> 0L
            }
        } else {
            (value.toLongOrNull() ?: 0L) * 1000
        }
    }

    fun clearCache() {
        showsCache.clear()
        booksCache.clear()
    }

    private companion object {
        const val MAX_EPISODES = 150
    }
}

/**
 * Double filtrage exige par le cahier des charges : whitelist de genres, puis
 * filtre mot-cle sur le titre, car les genres declares ne sont pas fiables.
 */
private fun PodcastShow.isAllowed(category: PodcastCategory?): Boolean {
    val haystack = (title + " " + author.orEmpty()).lowercase()
    if (PodcastCategory.blockedKeywords.any { haystack.contains(it) }) return false

    val genresLower = genres.map { it.lowercase() }
    if (genresLower.any { genre -> PodcastCategory.blockedGenres.any { genre.contains(it) } }) return false

    if (category == null || category.allowedGenres.isEmpty()) return true
    return genresLower.any { genre -> category.allowedGenres.any { genre.contains(it) } }
}
