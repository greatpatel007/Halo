package com.halo.core.designsystem.runtime

import com.halo.core.designsystem.theme.LightRotation
import com.halo.core.designsystem.theme.haloKeyLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The scene and feedback etiquette follow the runtime's policy (proposal 0003, step 3). */
class HaloSceneTest {

    private class FakeSource : SensorSource {
        val running = mutableMapOf<HaloSensor, (FloatArray, Long) -> Unit>()
        override fun start(sensor: HaloSensor, onValues: (FloatArray, Long) -> Unit): Boolean {
            running[sensor] = onValues
            return true
        }
        override fun stop(sensor: HaloSensor) {
            running.remove(sensor)
        }
    }

    @Test
    fun theLightFollowsGravityFromTheSharedHubAndSettlesWhenItStops() {
        val source = FakeSource()
        val runtime = HaloRuntime(source)
        runtime.policy.foreground = true
        val light = haloKeyLight(dark = true, rig = runtime.scene.rig)
        val still = light.x
        val gravity = source.running.getValue(HaloSensor.Gravity)
        gravity(floatArrayOf(0f, 9.81f, 0f), 1_000_000_000L)
        gravity(floatArrayOf(5f, 8.4f, 0f), 1_500_000_000L)
        assertNotEquals(LightRotation.None, runtime.scene.rig.rotation)
        assertNotEquals(still, light.x, 1e-4f)
        // Background: the hub stops gravity and the light returns to its designed direction.
        runtime.policy.foreground = false
        assertEquals(LightRotation.None, runtime.scene.rig.rotation)
        assertEquals(still, light.x, 1e-6f)
    }

    @Test
    fun hdrBacksOffWhileConservingPower() {
        val runtime = HaloRuntime()
        val hdr = runtime.scene.hdr
        assertFalse(hdr.limited)
        runtime.policy.powerSave = true
        assertTrue(hdr.limited)
        assertEquals(1f, hdr.emission)
        runtime.policy.powerSave = false
        runtime.policy.hot = true
        assertTrue(hdr.limited)
        runtime.policy.hot = false
        assertFalse(hdr.limited)
    }

    @Test
    fun soundEtiquetteFollowsTheRingerAndTouchSounds() {
        val policy = HaloRuntime().policy
        var heard = 0
        policy.addListener { heard++ }
        assertTrue(policy.soundAllowed)
        policy.ringerNormal = false
        assertFalse(policy.soundAllowed)
        policy.ringerNormal = true
        policy.touchSounds = false
        assertFalse(policy.soundAllowed)
        // addListener calls once immediately, then once per real change (3 changes).
        assertEquals(4, heard)
    }
}
