package com.halo.core.designsystem.feedback

import com.halo.core.designsystem.theme.HaloHaptic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One pattern, four kinds of hardware: each gets the richest plan it can play. */
class HapticPlanTest {

    private val allPrimitives = HaloPrimitive.entries.associateWith { 20 }
    private val envelopePhone = HapticCapabilities(
        hasVibrator = true, envelope = true, minControlPointMs = 4, maxControlPointMs = 1000, maxEnvelopePoints = 32,
        primitives = allPrimitives, amplitudeControl = true, qFactor = 8f,
    )
    private val primitivePhone = HapticCapabilities(hasVibrator = true, primitives = allPrimitives, amplitudeControl = true)
    private val amplitudePhone = HapticCapabilities(hasVibrator = true, amplitudeControl = true, qFactor = 20f)
    private val basicPhone = HapticCapabilities(hasVibrator = true)

    @Test
    fun tiersFollowTheHardware() {
        assertEquals(HapticTier.Envelope, envelopePhone.tier)
        assertEquals(HapticTier.Primitives, primitivePhone.tier)
        assertEquals(HapticTier.Amplitude, amplitudePhone.tier)
        assertEquals(HapticTier.Basic, basicPhone.tier)
        assertEquals(HapticTier.None, HapticCapabilities(hasVibrator = false).tier)
    }

    @Test
    fun envelopePhonesUseSharpnessCurvesForHeldPatternsAndPrimitivesForTaps() {
        val held = envelopePhone.plan(Patterns.Crescendo) as HapticPlan.Envelope
        assertTrue(held.points.all { it.durationMs >= 4 })
        assertEquals(0f, held.points.last().intensity)
        // The swell sharpens as it grows.
        assertTrue(held.points.maxOf { it.sharpness } > held.initialSharpness)
        assertTrue(envelopePhone.plan(Patterns.Tap) is HapticPlan.Primitives)
    }

    @Test
    fun primitivesMatchSharpnessAndRespectRealDurations() {
        val reject = primitivePhone.plan(Patterns.Reject) as HapticPlan.Primitives
        // Dull, firm hits thud.
        assertEquals(HaloPrimitive.Thud, reject.steps.first().primitive)
        // 75 ms apart with 20 ms primitives → 55 ms of silence between.
        assertEquals(55, reject.steps[1].delayMs)
        val light = primitivePhone.plan(Patterns.tick(0.2f)) as HapticPlan.Primitives
        assertEquals(HaloPrimitive.Tick, light.steps.single().primitive)
    }

    @Test
    fun amplitudePhonesGetASynthesisedWaveformThatStartsAndEndsSilent() {
        val wave = amplitudePhone.plan(Patterns.Confirm) as HapticPlan.Waveform
        assertEquals(wave.timingsMs.size, wave.amplitudes.size)
        assertTrue(wave.amplitudes.max() > 0)
        assertEquals(0, wave.amplitudes.last())
        // The two taps are separate pulses.
        assertTrue(wave.amplitudes.count { it > 0 } >= 2)
    }

    @Test
    fun basicMotorsOnlyBuzzForMomentsWorthIt() {
        assertEquals(HapticPlan.Basic(SystemHaptic.Confirm), basicPhone.plan(Patterns.Confirm))
        assertEquals(HapticPlan.Silent, basicPhone.plan(Patterns.Tick))
        assertEquals(HapticPlan.Silent, basicPhone.plan(Patterns.Crescendo))
    }

    @Test
    fun everyTokenPlansOnEveryTier() {
        for (phone in listOf(envelopePhone, primitivePhone, amplitudePhone, basicPhone)) {
            for (token in HaloHaptic.entries) phone.plan(Patterns.of(token))
        }
    }

    @Test
    fun lightTapsStayFeelableOnAmplitudeMotors() {
        assertTrue(amplitude(0.1f) > 255 * 0.1f)
        assertEquals(255, amplitude(1f))
        assertEquals(0, amplitude(0f))
    }

    @Test
    fun calibrationScalesStrengthButNotTimingAndStaysInRange() {
        val plan = primitivePhone.plan(Patterns.Confirm) as HapticPlan.Primitives
        val weak = plan.scaled(0.5f) as HapticPlan.Primitives
        val strong = plan.scaled(1.5f) as HapticPlan.Primitives
        assertEquals(plan.steps.map { it.delayMs }, weak.steps.map { it.delayMs })
        assertEquals(plan.steps[0].scale * 0.5f, weak.steps[0].scale, 1e-4f)
        assertTrue(strong.steps.all { it.scale <= 1f })
        val wave = amplitudePhone.plan(Patterns.Confirm) as HapticPlan.Waveform
        val scaled = wave.scaled(1.4f) as HapticPlan.Waveform
        assertTrue(scaled.amplitudes.all { it in 0..255 })
        assertEquals(wave.amplitudes.count { it == 0 }, scaled.amplitudes.count { it == 0 })
    }
}
