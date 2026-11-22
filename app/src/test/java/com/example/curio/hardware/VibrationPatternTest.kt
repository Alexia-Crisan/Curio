package com.example.curio.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationPatternTest {
    @Test
    fun everyPatternIsWellFormed() {
        VibrationPattern.entries.forEach { p ->
            assertEquals(p.name, p.timings.size, p.amplitudes.size)
            assertEquals(p.name, 0L, p.timings.first())
            assertTrue(p.name, p.amplitudes.all { it in 0..255 })
            assertTrue(p.name, p.timings.sum() < 700)
        }
    }

    @Test
    fun theDayPatternIsLongerAndDifferent() {
        assertNotEquals(VibrationPattern.SHAKE_BONUS.timings.toList(), VibrationPattern.DAY_COMPLETE.timings.toList())
        assertTrue(VibrationPattern.DAY_COMPLETE.timings.sum() > VibrationPattern.SHAKE_BONUS.timings.sum())
    }
}
