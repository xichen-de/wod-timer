package dev.xichen.wodtimer.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerController(
    context: Context,
    private val cuePlayer: CuePlayer,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engine: TimerEngine? = null
    private var ticker: Job? = null
    private var previous: TimerSnapshot? = null
    private val _state = MutableStateFlow<TimerSnapshot?>(null)
    val state: StateFlow<TimerSnapshot?> = _state.asStateFlow()

    fun prepare(config: TimerConfig) {
        ticker?.cancel()
        engine = TimerEngine(config)
        previous = null
        publish()
    }

    fun start() {
        val timer = engine ?: return
        timer.start(now())
        publish()
        _state.value?.let { state ->
            if (state.phase == TimerPhase.PREPARING && state.preStartRemainingSeconds in 1..3) {
                cuePlayer.play(Cue.COUNTDOWN, state.config)
            }
        }
        ContextCompat.startForegroundService(appContext, Intent(appContext, TimerService::class.java))
        startTicker()
    }

    fun pause() { engine?.pause(now()); publish() }
    fun resume() { engine?.resume(now()); publish(); startTicker() }
    fun stop() { engine?.stop(now()); previous = null; publish() }

    fun reset() {
        engine?.reset()
        previous = null
        publish()
        ticker?.cancel()
        appContext.stopService(Intent(appContext, TimerService::class.java))
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                publish()
                val status = _state.value?.status
                if (status == TimerStatus.FINISHED || status == TimerStatus.IDLE) break
                delay(TICK_INTERVAL_MILLIS)
            }
        }
    }

    private fun publish() {
        val next = engine?.snapshot(now()) ?: return
        emitCues(previous, next)
        previous = next
        _state.value = next
        if (next.status == TimerStatus.FINISHED) {
            ticker?.cancel()
            appContext.stopService(Intent(appContext, TimerService::class.java))
        }
    }

    private fun emitCues(old: TimerSnapshot?, next: TimerSnapshot) {
        val config = next.config
        transitionCue(old, next)?.let { cue -> cuePlayer.play(cue, config); return }
        if (old == null || old.status == TimerStatus.IDLE || next.status == TimerStatus.PAUSED) return
        val oldSecond = warningSeconds(old)
        val newSecond = warningSeconds(next)
        if (newSecond != oldSecond && newSecond in 1..3 &&
            (next.phase == TimerPhase.PREPARING || config.warningEnabled)
        ) cuePlayer.play(Cue.COUNTDOWN, config)
    }

    private fun warningSeconds(state: TimerSnapshot): Int? {
        state.preStartRemainingSeconds?.let { return it }
        val remaining = state.intervalRemainingMillis ?: state.remainingMillis ?: return null
        return ((remaining + 999L) / 1_000L).toInt()
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 200L
    }
}
