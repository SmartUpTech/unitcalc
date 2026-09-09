package net.smartlogic.unitconverter.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerDurationFormatterTest {
    @Test
    fun formatDuration_withHours() {
        assertEquals("2:00:00", TimerDurationFormatter.formatDuration(7_200_000L))
    }

    @Test
    fun formatDuration_withoutHours() {
        assertEquals("5:00", TimerDurationFormatter.formatDuration(300_000L))
    }

    @Test
    fun normalizeLabel_limitsLength() {
        val label = "A".repeat(40)
        assertEquals(25, TimerDurationFormatter.normalizeLabel(label).length)
    }
}
