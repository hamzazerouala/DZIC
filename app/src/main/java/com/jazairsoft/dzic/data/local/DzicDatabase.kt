package com.jazairsoft.dzic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        HistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DzicDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
}
