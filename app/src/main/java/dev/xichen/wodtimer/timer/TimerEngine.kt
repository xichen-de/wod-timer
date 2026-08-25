package dev.xichen.wodtimer.timer

import kotlin.math.ceil

/** Mutable command state with presentation derived exclusively from a monotonic timestamp. */
class TimerEngine(
    val config: TimerConfig,
) {
    private var status = TimerStatus.IDLE
    private var startedAtMillis = 0L
    private var pausedAtMillis = 0L
    private var pausedDurationMillis = 0L
    private var statusBeforePause = TimerStatus.RUNNING
    private var stoppedElapsedMillis: Long? = null
    private var finishReason: FinishReason? = null

    fun start(nowMillis: Long) {
        if (status != TimerStatus.IDLE && status != TimerStatus.FINISHED) return
        startedAtMillis = nowMillis
        pausedDurationMillis = 0L
        stoppedElapsedMillis = null
        finishReason = null
        status = if (config.preStartSeconds > 0) TimerStatus.PREPARING else TimerStatus.RUNNING
    }

    fun pause(nowMillis: Long) {
        refreshStatus(nowMillis)
        if (status != TimerStatus.RUNNING && status != TimerStatus.PREPARING) return
        statusBeforePause = status
        pausedAtMillis = nowMillis
        status = TimerStatus.PAUSED
    }

    fun resume(nowMillis: Long) {
        if (status != TimerStatus.PAUSED) return
        pausedDurationMillis += (nowMillis - pausedAtMillis).coerceAtLeast(0L)
        status = statusBeforePause
        refreshStatus(nowMillis)
    }

    fun stop(nowMillis: Long) {
        refreshStatus(nowMillis)
        if (status == TimerStatus.RUNNING || status == TimerStatus.PREPARING || status == TimerStatus.PAUSED) {
            stoppedElapsedMillis = workoutElapsed(nowMillis)
            finishReason = FinishReason.COMPLETED
            status = TimerStatus.FINISHED
        }
    }

    fun reset() {
        status = TimerStatus.IDLE
        startedAtMillis = 0L
        pausedAtMillis = 0L
        pausedDurationMillis = 0L
        stoppedElapsedMillis = null
        finishReason = null
    }

    fun snapshot(nowMillis: Long): TimerSnapshot {
        refreshStatus(nowMillis)
        val effectiveNow = if (status == TimerStatus.PAUSED) pausedAtMillis else nowMillis
        val activeSinceStart = if (status == TimerStatus.IDLE) 0L else
            (effectiveNow - startedAtMillis - pausedDurationMillis).coerceAtLeast(0L)
        val prepMillis = config.preStartSeconds * 1_000L

        if (status == TimerStatus.IDLE) return idleSnapshot()
        if (status == TimerStatus.PREPARING || (status == TimerStatus.PAUSED && statusBeforePause == TimerStatus.PREPARING)) {
            val remaining = (prepMillis - activeSinceStart).coerceAtLeast(0L)
            return TimerSnapshot(
                config = config,
                status = status,
                phase = TimerPhase.PREPARING,
                elapsedMillis = 0L,
                remainingMillis = null,
                currentRound = null,
                totalRounds = totalRounds(),
                preStartRemainingSeconds = ceil(remaining / 1_000.0).toInt(),
            )
        }

        val elapsed = stoppedElapsedMillis ?: (activeSinceStart - prepMillis).coerceAtLeast(0L)
        return runningSnapshot(elapsed, status)
    }

    private fun refreshStatus(nowMillis: Long) {
        if (status == TimerStatus.IDLE || status == TimerStatus.PAUSED || status == TimerStatus.FINISHED) return
        val active = (nowMillis - startedAtMillis - pausedDurationMillis).coerceAtLeast(0L)
        val prep = config.preStartSeconds * 1_000L
        if (active >= prep && status == TimerStatus.PREPARING) status = TimerStatus.RUNNING
        val total = config.mode.totalDurationMillis()
        if (status == TimerStatus.RUNNING && total != null && active - prep >= total) {
            stoppedElapsedMillis = total
            finishReason = if (config.mode is TimerMode.ForTime) FinishReason.TIME_CAP else FinishReason.DURATION_COMPLETE
            status = TimerStatus.FINISHED
        }
    }

    private fun workoutElapsed(nowMillis: Long): Long {
        val effectiveNow = if (status == TimerStatus.PAUSED) pausedAtMillis else nowMillis
        return (effectiveNow - startedAtMillis - pausedDurationMillis - config.preStartSeconds * 1_000L)
            .coerceAtLeast(0L)
    }

    private fun idleSnapshot() = TimerSnapshot(
        config = config,
        status = TimerStatus.IDLE,
        phase = when (config.mode) {
            is TimerMode.Intervals -> TimerPhase.WORK
            else -> TimerPhase.RUNNING
        },
        elapsedMillis = 0L,
        remainingMillis = config.mode.totalDurationMillis(),
        currentRound = totalRounds()?.let { 1 },
        totalRounds = totalRounds(),
        intervalRemainingMillis = when (val mode = config.mode) {
            is TimerMode.EveryXMinutes -> mode.intervalMillis
            is TimerMode.Intervals -> mode.workMillis
            else -> null
        },
    )

    private fun runningSnapshot(elapsedMillis: Long, currentStatus: TimerStatus): TimerSnapshot {
        val mode = config.mode
        val total = mode.totalDurationMillis()
        val clampedElapsed = total?.let { elapsedMillis.coerceAtMost(it) } ?: elapsedMillis
        val finished = currentStatus == TimerStatus.FINISHED
        return when (mode) {
            is TimerMode.ForTime -> TimerSnapshot(
                config, currentStatus, if (finished) TimerPhase.FINISHED else TimerPhase.RUNNING,
                clampedElapsed, mode.capMillis?.let { (it - clampedElapsed).coerceAtLeast(0L) }, null, null,
                finishReason = finishReason,
            )
            is TimerMode.Amrap -> TimerSnapshot(
                config, currentStatus, if (finished) TimerPhase.FINISHED else TimerPhase.RUNNING,
                clampedElapsed, (mode.durationMillis - clampedElapsed).coerceAtLeast(0L), null, null,
            )
            is TimerMode.EveryXMinutes -> {
                val round = if (finished) mode.rounds else (clampedElapsed / mode.intervalMillis).toInt() + 1
                val within = if (finished) mode.intervalMillis else clampedElapsed % mode.intervalMillis
                TimerSnapshot(
                    config, currentStatus, if (finished) TimerPhase.FINISHED else TimerPhase.RUNNING,
                    clampedElapsed, (total!! - clampedElapsed).coerceAtLeast(0L), round, mode.rounds,
                    intervalRemainingMillis = if (finished) 0L else mode.intervalMillis - within,
                )
            }
            is TimerMode.Intervals -> intervalSnapshot(mode, clampedElapsed, currentStatus, finished)
        }
    }

    private fun intervalSnapshot(
        mode: TimerMode.Intervals,
        elapsed: Long,
        currentStatus: TimerStatus,
        finished: Boolean,
    ): TimerSnapshot {
        val cycle = mode.workMillis + mode.restMillis
        val round = if (finished) mode.rounds else (elapsed / cycle).toInt() + 1
        val within = elapsed % cycle
        val working = within < mode.workMillis
        val phaseDuration = if (working) mode.workMillis else mode.restMillis
        val phaseElapsed = if (working) within else within - mode.workMillis
        return TimerSnapshot(
            config = config,
            status = currentStatus,
            phase = if (finished) TimerPhase.FINISHED else if (working) TimerPhase.WORK else TimerPhase.REST,
            elapsedMillis = elapsed,
            remainingMillis = (mode.totalDurationMillis()!! - elapsed).coerceAtLeast(0L),
            currentRound = round,
            totalRounds = mode.rounds,
            intervalRemainingMillis = if (finished) 0L else phaseDuration - phaseElapsed,
        )
    }

    private fun totalRounds(): Int? = when (val mode = config.mode) {
        is TimerMode.EveryXMinutes -> mode.rounds
        is TimerMode.Intervals -> mode.rounds
        else -> null
    }
}
