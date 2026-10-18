package com.example.curio.hardware

import kotlin.math.sqrt

data class ShakeConfig(
    val spikeThresholdG: Float = 2.5f,
    val requiredSpikes: Int = 2,
    val windowMs: Long = 700L,
    val cooldownMs: Long = 1500L,
)

class ShakeDetector(private val config: ShakeConfig = ShakeConfig()) {
    private val spikeTimestamps = mutableListOf<Long>()
    private var lastShakeTimestamp = 0L

    fun onSensorSample(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        if (timestampMs - lastShakeTimestamp < config.cooldownMs) {
            return false
        }

        val g = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / 9.81f

        if (g > config.spikeThresholdG) {
            spikeTimestamps.add(timestampMs)
            spikeTimestamps.removeAll { timestampMs - it > config.windowMs }

            if (spikeTimestamps.size >= config.requiredSpikes) {
                spikeTimestamps.clear()
                lastShakeTimestamp = timestampMs
                return true
            }
        }
        return false
    }
}
