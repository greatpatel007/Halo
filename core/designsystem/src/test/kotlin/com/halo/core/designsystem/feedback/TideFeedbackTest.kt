package com.halo.core.designsystem.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/** The Tide's feedback follows the wave's own spring (research 0002). */
class TideFeedbackTest {

    private val omega = sqrt(7f)
    private val launch = TideTrajectory.launch(omega, distance = 3000f, kick = 0.5f)

    @Test
    fun theLaunchLeavesAtPeakSpeedAndOnlyDecelerates() {
        assertEquals(1f, launch.speed(0f), 1e-4f)
        // Analytic: v(t)/v0 = (1 + ωt)·e^(−ωt).
        for (t in listOf(0.2f, 0.63f, 1f, 1.5f)) assertEquals((1 + omega * t) * exp(-omega * t), launch.speed(t), 1e-4f)
        var last = 1f
        var t = 0f
        while (t < 3f) {
            val s = launch.speed(t)
            assertTrue(s <= last + 1e-6f)
            last = s
            t += 0.01f
        }
        // Half speed near 0.63 s; the wash ends around when the front stops (~1.8 s at 1×).
        assertEquals(0.5f, launch.speed(0.63f), 0.02f)
        assertTrue(launch.durationS in 1.4f..2.2f)
    }

    @Test
    fun aReversalStartsFromTheCurrentMotionAndIsNormalisedToTheLaunch() {
        // Mid-wave: heading out at 40% of launch speed, pulled back to the start 1500 px behind.
        val v = 0.4f * launch.reference
        val back = TideTrajectory(omega, error0 = 1500f, velocity0 = v, reference = launch.reference)
        assertEquals(0.4f, back.speed(0f), 1e-4f)
        // It slows, turns round (speed through zero) and comes back.
        val speeds = (0..200).map { back.speed(it / 100f) }
        assertTrue(speeds.min() < 0.05f)
        assertTrue(speeds.drop(20).max() > 0.1f)
    }

    private val envelopePhone = HapticCapabilities(
        hasVibrator = true, envelope = true, minControlPointMs = 20, maxControlPointMs = 1000, maxEnvelopePoints = 128,
        primitives = HaloPrimitive.entries.associateWith { 12 }, amplitudeControl = true,
    )
    private val primitivePhone = HapticCapabilities(hasVibrator = true, primitives = HaloPrimitive.entries.associateWith { 12 })
    private val amplitudePhone = HapticCapabilities(hasVibrator = true, amplitudeControl = true)

    @Test
    fun envelopeRushLaunchesThenThinsOutToSilence() {
        val plan = envelopePhone.tidePlan(launch, toDark = true, launch = true) as HapticPlan.Envelope
        assertEquals(0.55f, plan.points.first().intensity, 1e-4f)
        assertEquals(0f, plan.points.last().intensity)
        assertTrue(plan.points.all { it.durationMs >= 20 })
        assertTrue(plan.points.size <= 128)
        // Early water is stronger than late water.
        val body = plan.points.drop(1).dropLast(1)
        assertTrue(body.take(5).map { it.intensity }.average() > body.takeLast(5).map { it.intensity }.average() * 3)
        // Water is round: low sharpness throughout.
        assertTrue(body.all { it.sharpness < 0.6f })
    }

    @Test
    fun primitiveRushIsAThinningStreamOfGrainsNotABuzz() {
        val plan = primitivePhone.tidePlan(launch, toDark = false, launch = true) as HapticPlan.Primitives
        assertEquals(HaloPrimitive.Thud, plan.steps.first().primitive)
        val grains = plan.steps.drop(1)
        assertTrue(grains.size in 8..60)
        assertTrue(grains.all { it.primitive == HaloPrimitive.LowTick })
        // Spacing widens and grains weaken as the water slows.
        assertTrue(grains.last().delayMs > grains[1].delayMs)
        assertTrue(grains.last().scale < grains.first().scale)
    }

    @Test
    fun reversalsHaveNoLaunchHit() {
        val plan = primitivePhone.tidePlan(launch, toDark = false, launch = false) as HapticPlan.Primitives
        assertFalse(plan.steps.any { it.primitive == HaloPrimitive.Thud })
    }

    @Test
    fun amplitudeRushEndsSilent() {
        val plan = amplitudePhone.tidePlan(launch, toDark = true, launch = true) as HapticPlan.Waveform
        assertEquals(0, plan.amplitudes.last())
        assertTrue(plan.amplitudes.first() > plan.amplitudes[plan.amplitudes.size / 2])
    }

    @Test
    fun basicMotorsGetOnlyTheLaunch() {
        assertEquals(HapticPlan.Basic(SystemHaptic.Click), HapticCapabilities(hasVibrator = true).tidePlan(launch, true, launch = true))
        assertEquals(HapticPlan.Silent, HapticCapabilities(hasVibrator = true).tidePlan(launch, true, launch = false))
    }

    @Test
    fun theWaterVoiceIsQuietCleanAndStopsWhenReleased() {
        val voice = TideVoice(toDark = false, trajectory = launch)
        val block = FloatArray(192 * 2)
        var peak = 0f
        var early = 0.0
        var late = 0.0
        var blocks = 0
        while (blocks < 500) { // 2 s
            block.fill(0f)
            voice.mix(block, 192)
            for (v in block) {
                assertFalse(v.isNaN())
                peak = maxOf(peak, abs(v))
            }
            val energy = block.sumOf { (it * it).toDouble() }
            if (blocks in 30..80) early += energy
            if (blocks in 400..450) late += energy
            blocks++
        }
        assertTrue("peak $peak", peak < 0.5f)
        assertTrue("water rushes then thins", early > late * 5)
        voice.release()
        var alive = true
        var n = 0
        while (alive && n < 2000) {
            block.fill(0f)
            alive = voice.mix(block, 192)
            n++
        }
        assertFalse("voice stops after release", alive)
    }
}
