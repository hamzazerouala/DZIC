package com.jazairsoft.dzic.data.remote

import com.jazairsoft.dzic.domain.model.MusicCategory
import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val archive: InternetArchiveApi,
    private val openverse: OpenverseApi
) {
    private val cache = mutableMapOf<String, List<Station>>()
    private val resolved = mutableMapOf<String, String>()

    /**
     * Internet Archive d'abord : c'est la seule source qui repond de maniere
     * fiable sans cle. Openverse vient en complement et son echec est normal,
     * donc jamais bloquant.
     */
    suspend fun category(category: MusicCategory, forceRefresh: Boolean = false): List<Station> {
        cache[category.key]?.takeIf { !forceRefresh && it.isNotEmpty() }?.let { return it }

        val fromArchive = withContext(Dispatchers.IO) {
            runCatching { archive.search(query = archiveQuery(category.archiveSubject)) }
                .getOrNull()?.response?.docs.orEmpty()
                .mapNotNull { it.toStation() }
        }

        val fromOpenverse = withContext(Dispatchers.IO) {
            runCatching { openverse.search(query = category.openverseQuery).results.orEmpty() }
                .getOrDefault(emptyList())
                .mapNotNull { it.toStation() }
                .filter { it.durationMs == 0L || it.durationMs >= MIN_TRACK_MS }
        }

        val merged = (fromArchive + fromOpenverse).distinctBy { it.id }
        if (merged.isNotEmpty()) cache[category.key] = merged
        return merged
    }

    suspend fun search(query: String): List<Station> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val terms = query.trim().replace("\"", "")
        val fromArchive = runCatching {
            archive.search(query = "mediatype:(audio) AND ($terms)")
        }.getOrNull()?.response?.docs.orEmpty().mapNotNull { it.toStation() }

        val fromOpenverse = runCatching { openverse.search(query = terms).results.orEmpty() }
            .getOrDefault(emptyList())
            .mapNotNull { it.toStation() }

        (fromArchive + fromOpenverse).distinctBy { it.id }
    }

    /**
     * Resout l'URL de lecture reelle d'un item Internet Archive.
     * Renvoie null si l'item ne contient aucun mp3 exploitable.
     */
    suspend fun resolveStream(station: Station): String? {
        if (!station.needsResolution) return station.streamUrl
        resolved[station.id]?.let { return it }

        val identifier = station.id.removePrefix("ia:")
        return withContext(Dispatchers.IO) {
            val files = runCatching { archive.metadata(identifier).files }
                .getOrNull().orEmpty()
            val audio = files.firstOrNull { file ->
                val name = file.name.orEmpty().lowercase()
                name.endsWith(".mp3") || name.endsWith(".ogg")
            } ?: return@withContext null

            val encoded = URLEncoder.encode(audio.name, "UTF-8")
                .replace("+", "%20")
            val url = "https://archive.org/download/$identifier/$encoded"
            resolved[station.id] = url
            url
        }
    }

    fun clearCache() {
        cache.clear()
        resolved.clear()
    }

    private fun archiveQuery(subject: String): String =
        "collection:(opensource_audio) AND mediatype:(audio) AND subject:(\"$subject\")"

    private companion object {
        const val MIN_TRACK_MS = 45_000L
    }
}
