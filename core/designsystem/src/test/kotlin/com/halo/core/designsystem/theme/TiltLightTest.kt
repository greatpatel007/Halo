package com.halo.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The key light is world-fixed: it turns with the device's rotation exactly as gravity does. */
class TiltLightTest {

    private fun rotate(rig: LightRig, from: FloatArray, to: FloatArray) {
        val r = lightRotationVector(from, to)
        val a = sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2])
        rig.rotation = if (a == 0f) LightRotation.None else LightRotation(r[0] / a, r[1] / a, r[2] / a, a)
    }

    @Test
    fun stillPhoneKeepsTheDesignedLight() {
        val light = haloKeyLight(dark = true, rig = LightRig())
        val still = haloKeyLight(dark = true)
        assertEquals(still.x, light.x, 1e-6f)
        assertEquals(still.y, light.y, 1e-6f)
        assertEquals(still.z, light.z, 1e-6f)
    }

    @Test
    fun theLightTurnsLikeGravity() {
        // Tilt the phone 15° about the screen's x axis: gravity (screen coords) turns by 15° about x.
        val t = Math.toRadians(15.0).toFloat()
        val g0 = floatArrayOf(0f, 0.7071f, 0.7071f)
        val g1 = floatArrayOf(0f, 0.7071f * cos(t) - 0.7071f * sin(t), 0.7071f * sin(t) + 0.7071f * cos(t))
        val rig = LightRig()
        val light = haloKeyLight(dark = true, rig = rig)
        val before = floatArrayOf(light.x, light.y, light.z)
        rotate(rig, g0, g1)
        // Same rotation applied to the light: x unchanged, (y, z) turned by 15°.
        assertEquals(before[0], light.x, 1e-4f)
        assertEquals(before[1] * cos(t) - before[2] * sin(t), light.y, 1e-4f)
        assertEquals(before[1] * sin(t) + before[2] * cos(t), light.z, 1e-4f)
        assertEquals(1f, sqrt(light.x * light.x + light.y * light.y + light.z * light.z), 1e-4f)
    }

    @Test
    fun theSwingIsCappedAndTheLightNeverSetsBelowTheHorizon() {
        val r = lightRotationVector(floatArrayOf(0f, 0f, 1f), floatArrayOf(1f, 0f, 0f))
        assertEquals(MAX_LIGHT_SWING, sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]), 1e-4f)
        val rig = LightRig()
        val light = haloKeyLight(dark = false, rig = rig)
        rig.rotation = LightRotation(0f, 1f, 0f, 1.4f)
        assertTrue(light.z >= 0.2f)
    }
}
