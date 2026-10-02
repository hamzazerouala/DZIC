package com.jazairsoft.dzic.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.jazairsoft.dzic.domain.model.MediaKind
import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.flow.Flow

enum class DownloadState { RUNNING, DONE, FAILED }

/**
 * Un media rapatrie sur l'appareil. On ne telecharge jamais une radio :
 * un flux live n'a pas de fin, le mettre en cache remplirait le disque
 * sans rien rendre reecoutable.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String?,
    val faviconUrl: String?,
    val sourceUrl: String,
    val localPath: String,
    val durationMs: Long,
    val kindName: String,
    val sourceLabel: String?,
    val bytes: Long,
    val state: String,
    val addedAt: Long
) {
    val isComplete: Boolean get() = state == DownloadState.DONE.name

    fun toStation(): Station = Station(
        id = id,
        name = name,
        // Lecture depuis le fichier local : aucune donnee mobile consommee.
        streamUrl = if (isComplete) "file://$localPath" else sourceUrl,
        faviconUrl = faviconUrl,
        tags = emptyList(),
        country = null,
        countryCode = null,
        language = null,
        codec = "MP3",
        bitrate = 0,
        votes = 0,
        clickCount = 0,
        kind = runCatching { MediaKind.valueOf(kindName) }.getOrDefault(MediaKind.TRACK),
        artist = artist,
        durationMs = durationMs,
        sourceLabel = sourceLabel
    )

    companion object {
        fun from(station: Station, localPath: String) = DownloadEntity(
            id = station.id,
            name = station.name,
            artist = station.artist,
            faviconUrl = station.faviconUrl,
            sourceUrl = station.streamUrl,
            localPath = localPath,
            durationMs = station.durationMs,
            kindName = station.kind.name,
            sourceLabel = station.sourceLabel,
            bytes = 0,
            state = DownloadState.RUNNING.name,
            addedAt = System.currentTimeMillis()
        )
    }
}

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun get(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("UPDATE downloads SET state = :state, bytes = :bytes WHERE id = :id")
    suspend fun updateState(id: String, state: String, bytes: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT SUM(bytes) FROM downloads WHERE state = 'DONE'")
    fun observeTotalBytes(): Flow<Long?>
}
