package com.pocketrocket.nearmiss.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

interface MotionListener {
    fun onBrakeDetected(gForce: Float)
    fun onSwerveDetected(yawRate: Float)
}

class MotionDetector(context: Context, private val listener: MotionListener) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var brakeThresholdG = 0.4f
    var swerveThresholdDegS = 60f
    
    private val sustainTimeMs = 150L
    private var brakeStartTimeMs = 0L
    private var isBraking = false
    private var maxGForce = 0f

    fun start() {
        linearAccel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val currentTimeMs = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // Assuming phone is mounted upright. y-axis is forward/backward
                // A hard brake results in forward deceleration.
                // Depending on orientation, we might need absolute or specific axis.
                // For simplicity in the prototype, we assume y-axis is the braking axis (longitudinal).
                val accelY = event.values[1] // m/s^2
                val gForce = accelY / 9.81f

                if (abs(gForce) > brakeThresholdG) {
                    if (!isBraking) {
                        isBraking = true
                        brakeStartTimeMs = currentTimeMs
                        maxGForce = abs(gForce)
                    } else {
                        maxGForce = maxOf(maxGForce, abs(gForce))
                        if (currentTimeMs - brakeStartTimeMs >= sustainTimeMs) {
                            listener.onBrakeDetected(maxGForce)
                            // Reset to avoid continuous spamming
                            isBraking = false
                            brakeStartTimeMs = currentTimeMs + 1000 // 1s cooldown
                        }
                    }
                } else {
                    isBraking = false
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                // z-axis is yaw (rotation around vertical axis)
                val yawRateRad = event.values[2]
                val yawRateDeg = Math.toDegrees(yawRateRad.toDouble()).toFloat()

                if (abs(yawRateDeg) > swerveThresholdDegS) {
                    listener.onSwerveDetected(abs(yawRateDeg))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
