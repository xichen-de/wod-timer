package dev.xichen.wodtimer.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerNotificationTest {
    @Test fun `capped for time notification shows truncated elapsed time`() {
        val engine = TimerEngine(TimerConfig(TimerMode.ForTime(60_000), preStartSeconds = 0))
        engine.start(0)
        assertEquals("RUNNING · 00:01", notificationText(engine.snapshot(1_200)))
        engine.pause(1_200)
        assertEquals("PAUSED · 00:01", notificationText(engine.snapshot(5_000)))
    }

    @Test fun `notification shows preparation countdown then remaining workout time`() {
        val engine = TimerEngine(TimerConfig(TimerMode.Amrap(60_000), preStartSeconds = 3))
        engine.start(0)
        assertEquals("PREPARING · 00:03", notificationText(engine.snapshot(0)))
        assertEquals("PREPARING · 00:02", notificationText(engine.snapshot(1_000)))
        assertEquals("RUNNING · 00:59", notificationText(engine.snapshot(4_200)))
        assertEquals(notificationText(engine.snapshot(4_200)), notificationText(engine.snapshot(4_400)))
    }
}
