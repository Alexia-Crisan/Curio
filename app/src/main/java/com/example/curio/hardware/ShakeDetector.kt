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
    private var wasAbove = false
    private var lastShakeTimestamp: Long? = null

    fun onSensorSample(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        val g = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / 9.81f
        val isAbove = g > config.spikeThresholdG
        val risingEdge = isAbove && !wasAbove
        wasAbove = isAbove
        val last = lastShakeTimestamp
        if (last != null && timestampMs - last < config.cooldownMs) return false
        if (!risingEdge) return false
        spikeTimestamps.add(timestampMs)
        spikeTimestamps.removeAll { timestampMs - it > config.windowMs }
        if (spikeTimestamps.size >= config.requiredSpikes) {
            spikeTimestamps.clear()
            lastShakeTimestamp = timestampMs
            return true
        }
        return false
    }
}
