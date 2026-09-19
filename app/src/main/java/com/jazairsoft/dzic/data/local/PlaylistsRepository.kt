package com.jazairsoft.dzic.data.local

import com.jazairsoft.dzic.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistsRepository @Inject constructor(
    private val dao: PlaylistDao
) {
    val playlists: Flow<List<PlaylistWithCount>> = dao.observePlaylists()

    fun items(playlistId: Long): Flow<List<PlaylistItemEntity>> = dao.observeItems(playlistId)

    fun stations(playlistId: Long): Flow<List<Station>> =
        dao.observeItems(playlistId).map { list -> list.map { it.toStation() } }

    fun playlist(playlistId: Long): Flow<PlaylistEntity?> = dao.observePlaylist(playlistId)

    suspend fun create(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun rename(playlistId: Long, name: String) = dao.rename(playlistId, name.trim())

    suspend fun delete(playlistId: Long) = dao.deletePlaylistWithItems(playlistId)

    /** Renvoie false si la station est deja dans la playlist. */
    suspend fun addStation(playlistId: Long, station: Station): Boolean {
        if (dao.contains(playlistId, station.id) > 0) return false
        val position = dao.nextPosition(playlistId)
        dao.insertItem(PlaylistItemEntity.from(playlistId, position, station))
        return true
    }

    suspend fun removeItem(itemId: Long) = dao.deleteItem(itemId)

    suspend fun persistOrder(itemIds: List<Long>) = dao.persistOrder(itemIds)
}
