package com.jazairsoft.dzic.data.remote

import com.jazairsoft.dzic.domain.model.MusicCategory
import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val openverse: OpenverseApi,
    private val ccMixter: CcMixterApi
) {
    private val cache = mutableMapOf<String, List<Station>>()

    /**
     * Une categorie interroge les deux sources en parallele.
     * Openverse fournit le gros du catalogue (Jamendo), ccMixter apporte des
     * instrumentaux et remixes curatels que Jamendo couvre mal.
     */
    suspend fun category(category: MusicCategory, forceRefresh: Boolean = false): List<Station> =
        coroutineScope {
            cache[category.key]?.takeIf { !forceRefresh && it.isNotEmpty() }?.let { return@coroutineScope it }

            val fromOpenverse = async(Dispatchers.IO) {
                runCatching { openverse.search(query = category.query).results.orEmpty() }
                    .getOrDefault(emptyList())
                    .mapNotNull { it.toStation() }
            }
            val fromCcMixter = async(Dispatchers.IO) {
                val tag = category.ccTag ?: return@async emptyList()
                runCatching { ccMixter.query(tags = tag) }
                    .getOrDefault(emptyList())
                    .mapNotNull { it.toStation() }
            }

            val merged = (fromOpenverse.await() + fromCcMixter.await())
                // Ecarte les fragments trop courts : Openverse remonte aussi des
                // extraits de quelques secondes qui n'ont rien de musical.
                .filter { it.durationMs == 0L || it.durationMs >= MIN_TRACK_MS }
                .distinctBy { it.id }

            cache[category.key] = merged
            merged
        }

    suspend fun search(query: String): List<Station> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching { openverse.search(query = query.trim()).results.orEmpty() }
            .getOrDefault(emptyList())
            .mapNotNull { it.toStation() }
            .filter { it.durationMs == 0L || it.durationMs >= MIN_TRACK_MS }
    }

    fun clearCache() = cache.clear()

    private companion object {
        const val MIN_TRACK_MS = 45_000L
    }
}
