package dev.xichen.wodtimer.preset

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetTest {
    @Test fun `saved and duplicated names respect the backup reader limit`() {
        val preset = defaultPreset(PresetMode.AMRAP).copy(name = "a".repeat(201)).normalized()
        assertEquals(200, preset.name.length)
        assertEquals(200, preset.copy(name = "${preset.name} copy").normalized().name.length)
    }
}
