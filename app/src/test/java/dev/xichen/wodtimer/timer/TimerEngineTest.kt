package dev.xichen.wodtimer.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerEngineTest {
    @Test fun `for time cap keeps elapsed primary and finishes at the cap`() {
        val engine = TimerEngine(TimerConfig(TimerMode.ForTime(capMillis = 15 * 60_000L), preStartSeconds = 0))
        engine.start(1_000)

        val running = engine.snapshot(6 * 60_000L + 1_000L)
        assertEquals(6 * 60_000L, running.elapsedMillis)
        assertEquals(9 * 60_000L, running.remainingMillis)

        val capped = engine.snapshot(15 * 60_000L + 1_001L)
        assertEquals(TimerStatus.FINISHED, capped.status)
        assertEquals(FinishReason.TIME_CAP, capped.finishReason)
        assertEquals(15 * 60_000L, capped.elapsedMillis)
        assertEquals(0L, capped.remainingMillis)
    }

    @Test fun `stopping capped for time records a completed workout`() {
        val engine = TimerEngine(TimerConfig(TimerMode.ForTime(capMillis = 10 * 60_000L), preStartSeconds = 0))
        engine.start(0)
        engine.stop(4 * 60_000L)

        val completed = engine.snapshot(9 * 60_000L)
        assertEquals(TimerStatus.FINISHED, completed.status)
        assertEquals(FinishReason.COMPLETED, completed.finishReason)
        assertEquals(4 * 60_000L, completed.elapsedMillis)
        assertEquals(6 * 60_000L, completed.remainingMillis)
    }

    @Test fun `amrap preparation and finish use absolute elapsed time`() {
        val engine = TimerEngine(TimerConfig(TimerMode.Amrap(60_000), preStartSeconds = 3))
        engine.start(10_000)
        assertEquals(3, engine.snapshot(10_000).preStartRemainingSeconds)
        assertEquals(TimerStatus.RUNNING, engine.snapshot(13_000).status)
        assertEquals(30_000L, engine.snapshot(43_000).remainingMillis)
        assertEquals(TimerStatus.FINISHED, engine.snapshot(73_001).status)
        assertEquals(0L, engine.snapshot(73_001).remainingMillis)
    }

    @Test fun `pause does not consume workout time`() {
        val engine = TimerEngine(TimerConfig(TimerMode.Amrap(60_000), preStartSeconds = 0))
        engine.start(1_000)
        engine.pause(11_000)
        assertEquals(10_000L, engine.snapshot(51_000).elapsedMillis)
        engine.resume(51_000)
        assertEquals(15_000L, engine.snapshot(56_000).elapsedMillis)
    }

    @Test fun `every x minutes derives interval and round`() {
        val engine = TimerEngine(TimerConfig(TimerMode.EveryXMinutes(120_000, 3), preStartSeconds = 0))
        engine.start(0)
        val state = engine.snapshot(125_000)
        assertEquals(2, state.currentRound)
        assertEquals(115_000L, state.intervalRemainingMillis)
    }

    @Test fun `intervals alternate work and rest`() {
        val engine = TimerEngine(TimerConfig(TimerMode.Intervals(20_000, 10_000, 2), preStartSeconds = 0))
        engine.start(0)
        assertEquals(TimerPhase.WORK, engine.snapshot(19_000).phase)
        assertEquals(TimerPhase.REST, engine.snapshot(20_000).phase)
        assertEquals(TimerPhase.WORK, engine.snapshot(30_000).phase)
        assertEquals(TimerStatus.FINISHED, engine.snapshot(50_000).status)
    }
}
