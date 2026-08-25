package dev.xichen.wodtimer

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.xichen.wodtimer.preset.AppDatabase
import dev.xichen.wodtimer.preset.PresetRepository
import dev.xichen.wodtimer.timer.CuePlayer
import dev.xichen.wodtimer.timer.TimerController

class WodTimerApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "wod-timer.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    val presets by lazy { PresetRepository(database) }
    val timerController by lazy { TimerController(this, CuePlayer(this)) }

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE presets ADD COLUMN forTimeCapEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
