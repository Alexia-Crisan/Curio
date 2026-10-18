package com.example.curio.hardware

data class TiltState(
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
)

class TiltIntegrator(
    private val decayFactor: Float = 0.98f,
    private val maxTiltDegrees: Float = 25f,
) {
    private var pitch = 0f
    private var roll = 0f

    fun update(gxRadPerSec: Float, gyRadPerSec: Float, dtSec: Float): TiltState {
        val deltaPitch = Math.toDegrees((gxRadPerSec * dtSec).toDouble()).toFloat()
        val deltaRoll = Math.toDegrees((gyRadPerSec * dtSec).toDouble()).toFloat()

        pitch = (pitch + deltaPitch) * decayFactor
        roll = (roll + deltaRoll) * decayFactor

        pitch = pitch.coerceIn(-maxTiltDegrees, maxTiltDegrees)
        roll = roll.coerceIn(-maxTiltDegrees, maxTiltDegrees)

        return TiltState(pitchDegrees = pitch, rollDegrees = roll)
    }
}
