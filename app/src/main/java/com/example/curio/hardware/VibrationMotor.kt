package com.example.curio.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class VibrationMotor(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    val isAvailable: Boolean = vibrator?.hasVibrator() == true

    fun play(pattern: VibrationPattern) {
        val v = vibrator ?: return
        if (!isAvailable) return
        runCatching {
            val effect = if (v.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(pattern.timings, -1)
            }
            v.vibrate(effect)
        }
    }
}
