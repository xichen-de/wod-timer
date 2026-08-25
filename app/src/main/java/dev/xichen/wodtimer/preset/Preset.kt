package dev.xichen.wodtimer.preset

import dev.xichen.wodtimer.timer.TimerConfig
import dev.xichen.wodtimer.timer.TimerMode

enum class PresetMode { FOR_TIME, AMRAP, EVERY_X_MINUTES, INTERVALS }

data class Preset(
    val id: Long = 0,
    val name: String,
    val mode: PresetMode,
    val durationMillis: Long = 10 * 60_000L,
    val intervalMillis: Long = 60_000L,
    val workMillis: Long = 20_000L,
    val restMillis: Long = 10_000L,
    val rounds: Int = 8,
    val preStartSeconds: Int = 3,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val warningEnabled: Boolean = true,
    val forTimeCapEnabled: Boolean = false,
    val position: Int = 0,
) {
    fun normalized() = copy(
        name = name.trim().ifBlank { mode.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) },
        durationMillis = durationMillis.coerceIn(1_000L, 24 * 60 * 60_000L),
        intervalMillis = intervalMillis.coerceIn(1_000L, 60 * 60_000L),
        workMillis = workMillis.coerceIn(1_000L, 60 * 60_000L),
        restMillis = restMillis.coerceIn(0L, 60 * 60_000L),
        rounds = rounds.coerceIn(1, 999),
        preStartSeconds = preStartSeconds.coerceIn(0, 60),
    )

    fun toTimerConfig(): TimerConfig {
        val preset = normalized()
        return TimerConfig(
            mode = when (preset.mode) {
                PresetMode.FOR_TIME -> TimerMode.ForTime(preset.durationMillis.takeIf { preset.forTimeCapEnabled })
                PresetMode.AMRAP -> TimerMode.Amrap(preset.durationMillis)
                PresetMode.EVERY_X_MINUTES -> TimerMode.EveryXMinutes(preset.intervalMillis, preset.rounds)
                PresetMode.INTERVALS -> TimerMode.Intervals(preset.workMillis, preset.restMillis, preset.rounds)
            },
            workoutName = preset.name,
            preStartSeconds = preset.preStartSeconds,
            soundEnabled = preset.soundEnabled,
            vibrationEnabled = preset.vibrationEnabled,
            warningEnabled = preset.warningEnabled,
        )
    }
}

fun defaultPreset(mode: PresetMode) = when (mode) {
    PresetMode.FOR_TIME -> Preset(name = "For Time", mode = mode)
    PresetMode.AMRAP -> Preset(name = "10 min AMRAP", mode = mode)
    PresetMode.EVERY_X_MINUTES -> Preset(name = "EMOM × 10", mode = mode, rounds = 10)
    PresetMode.INTERVALS -> Preset(name = "Tabata Classic", mode = mode)
}
