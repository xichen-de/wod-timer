package dev.xichen.wodtimer.timer

sealed interface TimerMode {
    data object ForTime : TimerMode
    data class Amrap(val durationMillis: Long) : TimerMode
    data class EveryXMinutes(val intervalMillis: Long, val rounds: Int) : TimerMode
    data class Intervals(val workMillis: Long, val restMillis: Long, val rounds: Int) : TimerMode
}

data class TimerConfig(
    val mode: TimerMode,
    val preStartSeconds: Int = 3,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val warningEnabled: Boolean = true,
)

enum class TimerStatus { IDLE, PREPARING, RUNNING, PAUSED, FINISHED }
enum class TimerPhase { PREPARING, RUNNING, WORK, REST, FINISHED }

data class TimerSnapshot(
    val config: TimerConfig,
    val status: TimerStatus,
    val phase: TimerPhase,
    val elapsedMillis: Long,
    val remainingMillis: Long?,
    val currentRound: Int?,
    val totalRounds: Int?,
    val preStartRemainingSeconds: Int? = null,
    val intervalRemainingMillis: Long? = null,
)

fun TimerMode.title(): String = when (this) {
    TimerMode.ForTime -> "For Time"
    is TimerMode.Amrap -> "AMRAP"
    is TimerMode.EveryXMinutes -> "Every X Minutes"
    is TimerMode.Intervals -> "Intervals"
}

fun TimerMode.totalDurationMillis(): Long? = when (this) {
    TimerMode.ForTime -> null
    is TimerMode.Amrap -> durationMillis
    is TimerMode.EveryXMinutes -> intervalMillis * rounds
    is TimerMode.Intervals -> (workMillis + restMillis) * rounds - restMillis
}

private fun formatHms(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

/** Remaining time, rounded up so a countdown never shows 0:00 before it actually elapses. */
fun formatClock(millis: Long): String = formatHms(if (millis <= 0) 0 else (millis + 999L) / 1_000L)

/** Elapsed time, truncated to whole seconds. */
fun formatElapsed(millis: Long): String = formatHms(millis.coerceAtLeast(0L) / 1_000L)

