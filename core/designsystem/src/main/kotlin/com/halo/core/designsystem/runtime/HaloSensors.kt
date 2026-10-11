package com.halo.core.designsystem.runtime

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.EnumMap

/** Motion sensors the runtime shares. Values are in the platform's device axes (x right, y up, z out of the screen). */
enum class HaloSensor {
    /** Gravity (m/s²): which way is down, for the world-fixed light. Falls back to the accelerometer. */
    Gravity,
    /** Angular velocity (rad/s): how fast the phone turns, for inertia (z = counter-clockwise about the screen). */
    Gyroscope,
}

/** A subscription; close it to stop listening (the last close turns the hardware off). */
fun interface SensorSubscription : AutoCloseable {
    override fun close()
}

/** The platform behind the hub (a fake in tests). */
internal interface SensorSource {
    /** Starts [sensor] at ~30 Hz; returns false if the phone doesn't have it. */
    fun start(sensor: HaloSensor, onValues: (values: FloatArray, timestampNanos: Long) -> Unit): Boolean
    fun stop(sensor: HaloSensor)
}

/**
 * One listener per sensor, however many subscribers: the hardware is on only while someone subscribes **and** the
 * policy allows ambient effects (foreground, no reduced motion, battery saver or heat). When it stops for any reason,
 * subscribers get `onStop` so they can settle (e.g. the light returns to its designed direction).
 */
@Stable
class HaloSensors internal constructor(private val source: SensorSource) {

    private class Subscriber(val onValues: (FloatArray, Long) -> Unit, val onStop: () -> Unit)

    private val subscribers = EnumMap<HaloSensor, MutableList<Subscriber>>(HaloSensor::class.java)
    private val running = HashSet<HaloSensor>()
    private var allowed = false

    /** Sensors whose hardware is on right now (the Showroom lab shows it). */
    var active: Set<HaloSensor> by mutableStateOf(emptySet())
        private set

    /**
     * Listens to [sensor] while the policy allows. [onValues] runs on the main thread with the platform's values;
     * [onStop] runs whenever the hardware stops (policy, last subscriber, or this subscription closing).
     */
    fun subscribe(
        sensor: HaloSensor,
        onStop: () -> Unit = {},
        onValues: (values: FloatArray, timestampNanos: Long) -> Unit,
    ): SensorSubscription {
        val sub = Subscriber(onValues, onStop)
        subscribers.getOrPut(sensor) { mutableListOf() } += sub
        reconcile(sensor)
        return SensorSubscription {
            if (subscribers[sensor]?.remove(sub) == true) {
                sub.onStop()
                reconcile(sensor)
            }
        }
    }

    internal fun setAllowed(value: Boolean) {
        if (value == allowed) return
        allowed = value
        HaloSensor.entries.forEach(::reconcile)
    }

    private fun reconcile(sensor: HaloSensor) {
        val subs = subscribers[sensor].orEmpty()
        val want = allowed && subs.isNotEmpty()
        if (want && sensor !in running) {
            val started = source.start(sensor) { values, time -> subscribers[sensor]?.toList()?.forEach { it.onValues(values, time) } }
            if (started) running += sensor
        } else if (!want && sensor in running) {
            source.stop(sensor)
            running -= sensor
            subs.forEach { it.onStop() }
        }
        active = running.toSet()
    }
}

/** The real sensors, at ~30 Hz, callbacks on the main thread. */
internal class AndroidSensorSource(context: Context) : SensorSource {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val listeners = EnumMap<HaloSensor, SensorEventListener>(HaloSensor::class.java)

    override fun start(sensor: HaloSensor, onValues: (FloatArray, Long) -> Unit): Boolean {
        val m = manager ?: return false
        val hardware = when (sensor) {
            HaloSensor.Gravity -> m.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            HaloSensor.Gyroscope -> m.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        } ?: return false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = onValues(event.values, event.timestamp)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listeners[sensor] = listener
        return m.registerListener(listener, hardware, SAMPLE_PERIOD_US)
    }

    override fun stop(sensor: HaloSensor) {
        listeners.remove(sensor)?.let { manager?.unregisterListener(it) }
    }

    private companion object {
        /** ~30 Hz. */
        const val SAMPLE_PERIOD_US = 33_333
    }
}
