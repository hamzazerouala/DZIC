package com.jazairsoft.dzic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt,
               (SELECT COUNT(*) FROM playlist_items i WHERE i.playlistId = p.id) AS itemCount
        FROM playlists p
        ORDER BY p.createdAt DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylist(playlistId: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun rename(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteItemsOf(playlistId: Long)

    /** Pas de cle etrangere en base : la suppression en cascade est faite ici. */
    @Transaction
    suspend fun deletePlaylistWithItems(playlistId: Long) {
        deleteItemsOf(playlistId)
        deletePlaylist(playlistId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId AND stationId = :stationId")
    suspend fun contains(playlistId: Long, stationId: String): Int

    @Query("UPDATE playlist_items SET position = :position WHERE id = :itemId")
    suspend fun updatePosition(itemId: Long, position: Int)

    /** Reecrit les positions dans l'ordre fourni, apres un glisser-deposer. */
    @Transaction
    suspend fun persistOrder(itemIds: List<Long>) {
        itemIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
