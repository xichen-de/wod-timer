package dev.xichen.wodtimer

import dev.xichen.wodtimer.timer.TimerStatus
import dev.xichen.wodtimer.ui.AppScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTest {
    @Test fun `screen stays awake only for an active workout on the timer screen`() {
        assertTrue(shouldKeepScreenAwake(AppScreen.TIMER, TimerStatus.PREPARING))
        assertTrue(shouldKeepScreenAwake(AppScreen.TIMER, TimerStatus.RUNNING))
        assertTrue(shouldKeepScreenAwake(AppScreen.TIMER, TimerStatus.PAUSED))

        assertFalse(shouldKeepScreenAwake(AppScreen.TIMER, TimerStatus.IDLE))
        assertFalse(shouldKeepScreenAwake(AppScreen.TIMER, TimerStatus.FINISHED))
        assertFalse(shouldKeepScreenAwake(AppScreen.HOME, TimerStatus.RUNNING))
        assertFalse(shouldKeepScreenAwake(AppScreen.CONFIGURE, TimerStatus.RUNNING))
    }
}
