package com.halo.core.designsystem.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One hardware listener per sensor, however many subscribers, and only while the policy allows. */
class HaloSensorsTest {

    private class FakeSource : SensorSource {
        val starts = mutableMapOf<HaloSensor, Int>()
        val running = mutableMapOf<HaloSensor, (FloatArray, Long) -> Unit>()
        override fun start(sensor: HaloSensor, onValues: (FloatArray, Long) -> Unit): Boolean {
            starts[sensor] = (starts[sensor] ?: 0) + 1
            running[sensor] = onValues
            return true
        }
        override fun stop(sensor: HaloSensor) {
            running.remove(sensor)
        }
        fun emit(sensor: HaloSensor, vararg v: Float) = running[sensor]?.invoke(v, 0L)
    }

    @Test
    fun subscribersShareOneListenerAndTheLastCloseTurnsItOff() {
        val source = FakeSource()
        val sensors = HaloSensors(source).apply { setAllowed(true) }
        val a = mutableListOf<Float>()
        val b = mutableListOf<Float>()
        val subA = sensors.subscribe(HaloSensor.Gyroscope) { v, _ -> a += v[2] }
        val subB = sensors.subscribe(HaloSensor.Gyroscope) { v, _ -> b += v[2] }
        assertEquals(1, source.starts[HaloSensor.Gyroscope])
        source.emit(HaloSensor.Gyroscope, 0f, 0f, 1.5f)
        assertEquals(listOf(1.5f), a)
        assertEquals(listOf(1.5f), b)
        subA.close()
        assertTrue(HaloSensor.Gyroscope in sensors.active)
        subB.close()
        assertTrue(sensors.active.isEmpty())
        assertTrue(source.running.isEmpty())
    }

    @Test
    fun thePolicyStopsAndRestartsEverySensorAndSubscribersSettle() {
        val source = FakeSource()
        val sensors = HaloSensors(source)
        var stops = 0
        sensors.subscribe(HaloSensor.Gravity, onStop = { stops++ }) { _, _ -> }
        // Not allowed yet (e.g. background): nothing is on.
        assertTrue(source.running.isEmpty())
        sensors.setAllowed(true)
        assertTrue(HaloSensor.Gravity in source.running)
        sensors.setAllowed(false)
        assertTrue(source.running.isEmpty())
        assertEquals(1, stops)
        sensors.setAllowed(true)
        assertEquals(2, source.starts[HaloSensor.Gravity])
    }

    @Test
    fun missingHardwareIsNotReportedActive() {
        val sensors = HaloSensors(object : SensorSource {
            override fun start(sensor: HaloSensor, onValues: (FloatArray, Long) -> Unit) = false
            override fun stop(sensor: HaloSensor) = Unit
        })
        sensors.setAllowed(true)
        sensors.subscribe(HaloSensor.Gyroscope) { _, _ -> }
        assertTrue(sensors.active.isEmpty())
    }

    @Test
    fun goingToTheBackgroundTurnsSensorsOffImmediatelyWithoutARecomposition() {
        val source = FakeSource()
        val runtime = HaloRuntime(source)
        runtime.sensors.subscribe(HaloSensor.Gravity) { _, _ -> }
        runtime.policy.foreground = true
        assertTrue(HaloSensor.Gravity in source.running)
        // A lifecycle event, straight from the observer: no frame, no composition involved.
        runtime.policy.foreground = false
        assertTrue(source.running.isEmpty())
        runtime.policy.foreground = true
        runtime.policy.hot = true
        assertTrue(source.running.isEmpty())
    }
}
