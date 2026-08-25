package dev.xichen.wodtimer.preset

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PresetEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
}

