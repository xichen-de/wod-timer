package dev.xichen.wodtimer.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class CueTransitionTest {
    @Test fun `all workout modes use work cue when workout begins`() {
        val modes = listOf(
            TimerMode.ForTime,
            TimerMode.Amrap(60_000),
            TimerMode.EveryXMinutes(60_000, 3),
            TimerMode.Intervals(20_000, 10_000, 3),
        )

        modes.forEach { mode ->
            val engine = TimerEngine(TimerConfig(mode, preStartSeconds = 0))
            val ready = engine.snapshot(0)
            engine.start(0)
            assertEquals(mode.toString(), Cue.WORK, transitionCue(ready, engine.snapshot(0)))
        }
    }

    @Test fun `work and rest cues are reused at interval phase boundaries`() {
        val engine = TimerEngine(
            TimerConfig(TimerMode.Intervals(workMillis = 20_000, restMillis = 10_000, rounds = 2), preStartSeconds = 0),
        )
        engine.start(0)

        assertEquals(Cue.REST, transitionCue(engine.snapshot(19_999), engine.snapshot(20_000)))
        assertEquals(Cue.WORK, transitionCue(engine.snapshot(29_999), engine.snapshot(30_000)))
    }

    @Test fun `every x minute rounds reuse the work cue`() {
        val engine = TimerEngine(
            TimerConfig(TimerMode.EveryXMinutes(intervalMillis = 60_000, rounds = 3), preStartSeconds = 0),
        )
        engine.start(0)

        assertEquals(Cue.WORK, transitionCue(engine.snapshot(59_999), engine.snapshot(60_000)))
    }

    @Test fun `preparation ending starts work and pause resume does not replay it`() {
        val engine = TimerEngine(TimerConfig(TimerMode.Amrap(60_000), preStartSeconds = 3))
        engine.start(0)
        val preparing = engine.snapshot(2_999)
        val running = engine.snapshot(3_000)
        assertEquals(Cue.WORK, transitionCue(preparing, running))

        engine.pause(4_000)
        val paused = engine.snapshot(4_000)
        engine.resume(5_000)
        assertEquals(null, transitionCue(paused, engine.snapshot(5_000)))
    }
}
