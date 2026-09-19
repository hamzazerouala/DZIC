package com.jazairsoft.dzic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<HistoryEntity>>

    @Query("SELECT playCount FROM history WHERE stationId = :stationId")
    suspend fun playCountOf(stationId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("DELETE FROM history WHERE stationId = :stationId")
    suspend fun remove(stationId: String)

    /** Incremente le compteur d'ecoutes en conservant l'historique existant. */
    @Transaction
    suspend fun record(entry: HistoryEntity) {
        val previous = playCountOf(entry.stationId) ?: 0
        upsert(entry.copy(playCount = previous + 1))
    }
}
