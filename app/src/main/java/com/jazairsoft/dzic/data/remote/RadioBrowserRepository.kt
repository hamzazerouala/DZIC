package com.jazairsoft.dzic.data.remote

import com.jazairsoft.dzic.domain.model.RadioCategory
import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioBrowserRepository @Inject constructor(
    private val api: RadioBrowserApi
) {

    /** Cache memoire simple : evite de re-interroger l'API a chaque aller-retour d'onglet. */
    private val categoryCache = mutableMapOf<String, List<Station>>()
    private var algeriaCache: List<Station>? = null

    suspend fun algerianStations(forceRefresh: Boolean = false): List<Station> {
        algeriaCache?.takeIf { !forceRefresh && it.isNotEmpty() }?.let { return it }
        val result = withContext(Dispatchers.IO) {
            api.stationsByCountryCode("DZ").toStations()
        }
        algeriaCache = result
        return result
    }

    /**
     * Stations d'une categorie. Les stations algeriennes correspondantes sont
     * injectees en tete de liste (exigence du cahier des charges).
     */
    suspend fun categoryStations(
        category: RadioCategory,
        forceRefresh: Boolean = false
    ): List<Station> = coroutineScope {
        categoryCache[category.key]?.takeIf { !forceRefresh && it.isNotEmpty() }?.let { return@coroutineScope it }

        val algerian = async { runCatching { algerianStations(forceRefresh) }.getOrDefault(emptyList()) }
        val remote = category.queryTags.map { tag ->
            async(Dispatchers.IO) { runCatching { api.stationsByTag(tag) }.getOrDefault(emptyList()) }
        }

        val generic = remote.awaitAll().flatten().toStations()
        val local = algerian.await().filter { station -> station.matches(category) }

        val merged = (local + generic)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Station> { it.isAlgerian }.thenByDescending { it.clickCount })

        categoryCache[category.key] = merged
        merged
    }

    suspend fun search(query: String): List<Station> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        api.search(query.trim()).toStations()
            .sortedWith(compareByDescending<Station> { it.isAlgerian }.thenByDescending { it.clickCount })
    }

    suspend fun registerClick(stationId: String) {
        runCatching { withContext(Dispatchers.IO) { api.registerClick(stationId) } }
    }

    fun clearCache() {
        categoryCache.clear()
        algeriaCache = null
    }

    private fun List<StationDto>.toStations(): List<Station> =
        mapNotNull { it.toStation() }
            .filter { it.streamUrl.startsWith("http", ignoreCase = true) }
            .distinctBy { it.id }

    private fun Station.matches(category: RadioCategory): Boolean =
        tags.any { tag -> category.matchTags.any { m -> tag == m || tag.contains(m) } }
}

