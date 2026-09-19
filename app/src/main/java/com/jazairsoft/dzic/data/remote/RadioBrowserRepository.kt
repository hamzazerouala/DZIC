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

    private val categoryCache = mutableMapOf<String, List<Station>>()
    private val countryCache = mutableMapOf<String, List<Station>>()
    private val languageCache = mutableMapOf<String, List<Station>>()
    private var algeriaCache: List<Station>? = null
    private var countriesCache: List<CountryDto>? = null
    private var languagesCache: List<LanguageDto>? = null

    suspend fun algerianStations(forceRefresh: Boolean = false): List<Station> =
        stationsOfCountry("DZ", forceRefresh).also { algeriaCache = it }

    suspend fun stationsOfCountry(code: String, forceRefresh: Boolean = false): List<Station> {
        countryCache[code]?.takeIf { !forceRefresh && it.isNotEmpty() }?.let { return it }
        val result = withContext(Dispatchers.IO) {
            api.stationsByCountryCode(code).toStations()
        }
        countryCache[code] = result
        return result
    }

    suspend fun stationsOfLanguage(language: String, forceRefresh: Boolean = false): List<Station> {
        languageCache[language]?.takeIf { !forceRefresh && it.isNotEmpty() }?.let { return it }
        val result = withContext(Dispatchers.IO) {
            api.stationsByLanguage(language).toStations()
        }
        languageCache[language] = result
        return result
    }

    /** Pays tries par nombre de stations : les plus fournis en premier. */
    suspend fun countries(): List<CountryDto> {
        countriesCache?.let { return it }
        val result = withContext(Dispatchers.IO) {
            runCatching { api.countries() }.getOrDefault(emptyList())
                .filter { !it.name.isNullOrBlank() && !it.code.isNullOrBlank() && (it.stationCount ?: 0) > 0 }
                .sortedByDescending { it.stationCount ?: 0 }
        }
        countriesCache = result
        return result
    }

    suspend fun languages(): List<LanguageDto> {
        languagesCache?.let { return it }
        val result = withContext(Dispatchers.IO) {
            runCatching { api.languages() }.getOrDefault(emptyList())
                .filter { !it.name.isNullOrBlank() && (it.stationCount ?: 0) > 0 }
                .sortedByDescending { it.stationCount ?: 0 }
        }
        languagesCache = result
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
        countryCache.clear()
        languageCache.clear()
        algeriaCache = null
    }

    private fun List<StationDto>.toStations(): List<Station> =
        mapNotNull { it.toStation() }
            .filter { it.streamUrl.startsWith("http", ignoreCase = true) }
            .distinctBy { it.id }

    private fun Station.matches(category: RadioCategory): Boolean =
        tags.any { tag -> category.matchTags.any { m -> tag == m || tag.contains(m) } }
}
