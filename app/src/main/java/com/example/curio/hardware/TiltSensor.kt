package com.example.curio.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TiltSensor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isAvailable: Boolean = gyroscope != null

    fun tilt(): Flow<TiltState> = callbackFlow {
        if (gyroscope == null) {
            close()
            return@callbackFlow
        }

        val integrator = TiltIntegrator()
        var lastTimestampNs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (lastTimestampNs != 0L) {
                        val dtSec = (it.timestamp - lastTimestampNs) / 1_000_000_000f
                        val gx = it.values[0]
                        val gy = it.values[1]

                        val tiltState = integrator.update(gx, gy, dtSec)
                        trySend(tiltState)
                    }
                    lastTimestampNs = it.timestamp
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
