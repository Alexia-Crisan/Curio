package com.example.curio.hardware

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {
    private val g = 9.81f

    // un eșantion liniștit, apoi unul puternic: un singur front
    private fun jolt(d: ShakeDetector, t: Long): Boolean {
        d.onSensorSample(0f, 0f, g, t - 10)
        return d.onSensorSample(0f, 0f, 3 * g, t)
    }

    @Test fun twoJoltsInsideTheWindowAreAShake() {
        val d = ShakeDetector()
        assertFalse(jolt(d, 100))
        assertTrue(jolt(d, 400))
    }

    @Test fun oneLongJoltIsNotAShake() {
        val d = ShakeDetector()
        assertFalse((0L..200L step 20).any { d.onSensorSample(0f, 0f, 3 * g, it) })
    }

    @Test fun joltsTooFarApartAreNotAShake() {
        val d = ShakeDetector()
        jolt(d, 100)
        assertFalse(jolt(d, 1000))
    }

    @Test fun gravityAloneNeverShakes() {
        val d = ShakeDetector()
        assertFalse((0L..5000L step 20).any { d.onSensorSample(0f, 0f, g, it) })
    }

    @Test fun cooldownBlocksASecondShake() {
        val d = ShakeDetector()
        jolt(d, 100); assertTrue(jolt(d, 200))
        jolt(d, 700); assertFalse(jolt(d, 800))
        jolt(d, 1800); assertTrue(jolt(d, 1900))
    }
}
