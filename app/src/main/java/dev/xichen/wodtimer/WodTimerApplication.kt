package dev.xichen.wodtimer

import android.app.Application
import androidx.room.Room
import dev.xichen.wodtimer.preset.AppDatabase
import dev.xichen.wodtimer.preset.PresetRepository
import dev.xichen.wodtimer.timer.CuePlayer
import dev.xichen.wodtimer.timer.TimerController

class WodTimerApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "wod-timer.db").build()
    }
    val presets by lazy { PresetRepository(database) }
    val timerController by lazy { TimerController(this, CuePlayer(this)) }
}
