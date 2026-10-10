package com.project.creativedetour.sensing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/**
 * The hardware step counter reports "steps since boot". We keep a few timestamped readings
 * and subtract the one from ~an hour ago. Steps are counted in low-power hardware and
 * delivered in batches, so this costs almost no battery.
 *
 * The sensor calls us on the main thread while the service reads from a background thread,
 * so both sides are @Synchronized — that's the race condition from the threads lesson.
 */
class StepSource(context: Context) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val counter: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val samples = ArrayDeque<Pair<Long, Float>>() // (ms since boot, steps since boot)

    val available: Boolean get() = counter != null

    fun start() {
        counter?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_US) }
    }

    fun stop() = sensors.unregisterListener(this)

    @Synchronized
    override fun onSensorChanged(event: SensorEvent) {
        samples.addLast(event.timestamp / 1_000_000 to event.values[0])
        // Keep exactly one reading older than an hour: it's the baseline we subtract from.
        val cutoff = SystemClock.elapsedRealtime() - HOUR_MS
        while (samples.size >= 2 && samples[1].first <= cutoff) samples.removeFirst()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** null = no sensor or no reading yet. Under an hour of history gives a partial count. */
    @Synchronized
    fun stepsLastHour(): Int? {
        if (samples.isEmpty()) return null
        val cutoff = SystemClock.elapsedRealtime() - HOUR_MS
        val baseline = samples.lastOrNull { it.first <= cutoff } ?: samples.first()
        return (samples.last().second - baseline.second).toInt()
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val BATCH_LATENCY_US = 5 * 60 * 1_000_000 // let the chip collect 5 min of steps per wake-up
    }
}
