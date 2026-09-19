package com.jazairsoft.dzic.data.local

import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao
) {
    val recent: Flow<List<Station>> = dao.observeRecent().map { list -> list.map { it.toStation() } }

    suspend fun record(station: Station) = dao.record(HistoryEntity.from(station, playCount = 0))

    suspend fun clear() = dao.clear()

    suspend fun remove(stationId: String) = dao.remove(stationId)
}
