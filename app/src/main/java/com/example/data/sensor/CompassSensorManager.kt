package com.example.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

data class CompassData(
    val azimuthDegrees: Float,       // 0..360
    val cardinalDirection: String,   // N, NE, E, etc.
    val mils: Int,                   // 0..6400
    val pitchDegrees: Float,         // Device tilt front/back
    val rollDegrees: Float,          // Device tilt left/right
    val accuracy: Int                // SensorManager.SENSOR_STATUS_*
)

class CompassSensorManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    fun hasSensor(): Boolean {
        return rotationSensor != null || (accelSensor != null && magneticSensor != null)
    }

    fun getCompassDataFlow(): Flow<CompassData> = callbackFlow {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        var lastAzimuth = 0f

        val listener = object : SensorEventListener {
            private var lastAccelerometer = FloatArray(3)
            private var lastMagnetometer = FloatArray(3)
            private var lastAccelerometerSet = false
            private var lastMagnetometerSet = false

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    azimuth = (azimuth % 360f + 360f) % 360f

                    val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    // Low-pass filter for smooth needle movement
                    val smoothedAzimuth = lastAzimuth + 0.15f * (azimuth - lastAzimuth)
                    lastAzimuth = smoothedAzimuth

                    val cardinal = getCardinal(smoothedAzimuth)
                    val mils = ((smoothedAzimuth / 360f) * 6400).roundToInt()

                    trySend(
                        CompassData(
                            azimuthDegrees = smoothedAzimuth,
                            cardinalDirection = cardinal,
                            mils = mils,
                            pitchDegrees = pitch,
                            rollDegrees = roll,
                            accuracy = event.accuracy
                        )
                    )
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    lastAccelerometerSet = true
                    computeOrientation()
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    lastMagnetometerSet = true
                    computeOrientation()
                }
            }

            private fun computeOrientation() {
                if (lastAccelerometerSet && lastMagnetometerSet) {
                    val success = SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)
                    if (success) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        azimuth = (azimuth % 360f + 360f) % 360f

                        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                        val smoothedAzimuth = lastAzimuth + 0.15f * (azimuth - lastAzimuth)
                        lastAzimuth = smoothedAzimuth

                        val cardinal = getCardinal(smoothedAzimuth)
                        val mils = ((smoothedAzimuth / 360f) * 6400).roundToInt()

                        trySend(
                            CompassData(
                                azimuthDegrees = smoothedAzimuth,
                                cardinalDirection = cardinal,
                                mils = mils,
                                pitchDegrees = pitch,
                                rollDegrees = roll,
                                accuracy = 2
                            )
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magneticSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun getCardinal(azimuth: Float): String {
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = (((azimuth + 11.25f) % 360f) / 22.5f).toInt() % 16
        return directions[index]
    }
}
