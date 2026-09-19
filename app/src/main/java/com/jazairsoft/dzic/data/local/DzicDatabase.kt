package com.jazairsoft.dzic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class DzicDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
