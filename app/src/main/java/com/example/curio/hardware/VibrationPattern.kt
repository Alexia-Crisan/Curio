package com.example.curio.hardware

/** Timings in ms (off, on, off, on...), amplitudes 0..255, one per timing. */
enum class VibrationPattern(val timings: LongArray, val amplitudes: IntArray) {
    SHAKE_BONUS(longArrayOf(0, 60), intArrayOf(0, 200)),
    DAY_COMPLETE(longArrayOf(0, 80, 90, 80, 90, 160), intArrayOf(0, 140, 0, 180, 0, 255)),
}
