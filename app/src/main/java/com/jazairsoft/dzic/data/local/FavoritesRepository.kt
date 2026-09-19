package com.jazairsoft.dzic.data.local

import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoriteDao
) {
    val favorites: Flow<List<Station>> = dao.observeAll().map { list -> list.map { it.toStation() } }
    val favoriteIds: Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    suspend fun toggle(station: Station) {
        if (dao.count(station.id) > 0) dao.delete(station.id)
        else dao.insert(FavoriteEntity.from(station))
    }
}
