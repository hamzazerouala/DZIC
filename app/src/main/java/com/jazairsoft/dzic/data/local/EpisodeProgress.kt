package com.jazairsoft.dzic.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Position de lecture d'un episode ou d'un chapitre, pour reprendre ou on s'est arrete. */
@Entity(tableName = "episode_progress")
data class EpisodeProgressEntity(
    @PrimaryKey val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
) {
    /** Termine : on ne propose plus de reprendre. */
    val isFinished: Boolean
        get() = durationMs > 0 && positionMs >= durationMs - FINISHED_MARGIN_MS

    private companion object {
        const val FINISHED_MARGIN_MS = 30_000L
    }
}

@Dao
interface EpisodeProgressDao {

    @Query("SELECT * FROM episode_progress WHERE episodeId = :episodeId")
    suspend fun get(episodeId: String): EpisodeProgressEntity?

    @Query("SELECT * FROM episode_progress")
    fun observeAll(): Flow<List<EpisodeProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: EpisodeProgressEntity)

    @Query("DELETE FROM episode_progress WHERE episodeId = :episodeId")
    suspend fun remove(episodeId: String)

    @Query("DELETE FROM episode_progress")
    suspend fun clear()
}

@Singleton
class EpisodeProgressRepository @Inject constructor(
    private val dao: EpisodeProgressDao
) {
    val all: Flow<List<EpisodeProgressEntity>> = dao.observeAll()

    suspend fun positionOf(episodeId: String): Long {
        val entry = dao.get(episodeId) ?: return 0L
        return if (entry.isFinished) 0L else entry.positionMs
    }

    suspend fun save(episodeId: String, positionMs: Long, durationMs: Long) {
        // En deca de ce seuil il n'y a rien a reprendre : inutile de polluer la base.
        if (positionMs < MIN_SAVE_MS) return
        dao.upsert(
            EpisodeProgressEntity(
                episodeId = episodeId,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clear() = dao.clear()

    private companion object {
        const val MIN_SAVE_MS = 15_000L
    }
}
