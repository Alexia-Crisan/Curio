package com.example.curio.hardware

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TiltIntegratorTest {
    @Test fun angleRisesWithRotation() {
        val t = TiltIntegrator()
        var s = TiltState()
        repeat(10) { s = t.update(0f, 1f, 0.02f) }   // 1 rad/s for 0.2 s
        assertTrue(s.rollDegrees > 5f)
    }

    @Test fun theAngleNeverPasses25Degrees() {
        val t = TiltIntegrator()
        var s = TiltState()
        repeat(500) { s = t.update(5f, -5f, 0.02f) }
        assertTrue(abs(s.pitchDegrees) <= 25f && abs(s.rollDegrees) <= 25f)
    }

    @Test fun easesBackToFlatWhenStill() {
        val t = TiltIntegrator()
        repeat(20) { t.update(0f, 2f, 0.02f) }
        var s = TiltState()
        repeat(300) { s = t.update(0f, 0f, 0.02f) }
        assertTrue(abs(s.rollDegrees) < 1f)
    }
}
