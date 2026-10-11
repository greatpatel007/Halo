package com.halo.core.designsystem.feedback

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Feedback for the Tide, the theme transition (research 0002). The wave's front runs on a critically damped spring,
 * so its speed is known exactly; the haptic rush is pre-computed from it (a running vibration can't be modulated
 * live) and the sound voice follows the same curve. Everything here is pure, so it is unit-tested.
 */

/**
 * The front's motion. With error e = position − target, a critically damped spring gives
 * e(t) = (e₀ + (v₀ + ωe₀)t)·e^(−ωt), so v(t) = e^(−ωt)·(v₀ − ωt(v₀ + ωe₀)).
 *
 * @param omega angular frequency (rad/s), already slowed by slow-mo.
 * @param reference the launch's peak speed (½ωD for a full sweep): speeds are normalised to it, so a reversal that
 * moves slower also feels and sounds weaker.
 */
internal class TideTrajectory(
    val omega: Float,
    val error0: Float,
    val velocity0: Float,
    val reference: Float,
) {
    /** Normalised speed (0 … ~1) at [t] seconds. */
    fun speed(t: Float): Float {
        if (reference <= 0f) return 0f
        val v = exp(-omega * t) * (velocity0 - omega * t * (velocity0 + omega * error0))
        return abs(v) / reference
    }

    /** Seconds until the front has effectively stopped (speed and remaining distance both negligible). */
    val durationS: Float by lazy {
            var t = 0f
            val step = 0.01f
            val limit = 10f / omega
            var lastAlive = 0f
            while (t < limit) {
                if (speed(t) >= SILENT_SPEED) lastAlive = t
                t += step
            }
            lastAlive
        }

    companion object {
        /** Below this normalised speed the wash is over. */
        const val SILENT_SPEED = 0.04f

        /** A full launch: the front leaves at its peak speed and only decelerates. */
        fun launch(omega: Float, distance: Float, kick: Float): TideTrajectory {
            val v0 = kick * omega * distance
            return TideTrajectory(omega, -distance, v0, v0)
        }
    }
}

/** Haptic character of the wave: to-dark is deeper, to-light a little brighter. */
private fun baseSharpness(toDark: Boolean) = if (toDark) 0.18f else 0.3f

/** The rush, compiled for this phone's tier. [launch] adds the release hit at the start (not on a reversal). */
internal fun HapticCapabilities.tidePlan(trajectory: TideTrajectory, toDark: Boolean, launch: Boolean): HapticPlan {
    val duration = trajectory.durationS
    if (duration <= 0f) return HapticPlan.Silent
    return when (tier) {
        HapticTier.Envelope -> tideEnvelope(trajectory, duration, toDark, launch)
        HapticTier.Primitives -> tideGrains(trajectory, duration, toDark, launch)
        HapticTier.Amplitude -> tideWaveform(trajectory, duration, toDark, launch)
        HapticTier.Basic -> if (launch) HapticPlan.Basic(SystemHaptic.Click) else HapticPlan.Silent
        HapticTier.None -> HapticPlan.Silent
    }
}

/** Water body: intensity follows speed (slightly compressed, so the tail stays feelable). */
private fun body(s: Float) = 0.32f * s.coerceIn(0f, 1f).pow(0.8f)

/** Deterministic grain jitter in −1 … 1. */
private fun grain(i: Int): Float {
    var h = i * 374761393 + 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFF) / 32767.5f - 1f
}

private fun HapticCapabilities.tideEnvelope(
    trajectory: TideTrajectory,
    duration: Float,
    toDark: Boolean,
    launch: Boolean,
): HapticPlan {
    val minStep = max(minControlPointMs, 1L)
    var step = max(minStep, ENVELOPE_STEP_MS)
    // Stay inside the actuator's point budget by widening the step for long (slow-mo) waves.
    val budget = if (maxEnvelopePoints > 0) maxEnvelopePoints - 3 else Int.MAX_VALUE
    while ((duration * 1000f / step) > budget) step += minStep
    val base = baseSharpness(toDark)
    val points = mutableListOf<EnvelopePoint>()
    if (launch) points += EnvelopePoint(LAUNCH_INTENSITY, base, minStep)
    var t = 0f
    var i = 0
    while (t <= duration) {
        val s = trajectory.speed(t)
        // Sand: each point jitters intensity and sharpness a little, more so when fast.
        val j = grain(i) * s
        points += EnvelopePoint(
            intensity = (body(s) * (1f + 0.25f * j)).coerceIn(0f, 1f),
            sharpness = (base + 0.2f * s + 0.15f * j).coerceIn(0f, 1f),
            durationMs = step,
        )
        t += step / 1000f
        i++
    }
    points += EnvelopePoint(0f, base, minStep)
    return HapticPlan.Envelope(initialSharpness = base, points = points)
}

private fun HapticCapabilities.tideGrains(
    trajectory: TideTrajectory,
    duration: Float,
    toDark: Boolean,
    launch: Boolean,
): HapticPlan {
    // A granular stream instead of a motor buzz: sand rushing, thinning out as the water slows.
    val grainPrimitive = if (HaloPrimitive.LowTick in primitives) HaloPrimitive.LowTick else HaloPrimitive.Tick
    val grainMs = primitives[grainPrimitive] ?: DEFAULT_GRAIN_MS
    val steps = mutableListOf<PrimitiveStep>()
    var clockMs = 0
    if (launch) {
        val hit = if (HaloPrimitive.Thud in primitives) HaloPrimitive.Thud else HaloPrimitive.Click
        steps += PrimitiveStep(hit, if (toDark) 0.6f else 0.5f, 0)
        clockMs = primitives[hit] ?: DEFAULT_GRAIN_MS
    }
    var t = clockMs / 1000f
    while (t <= duration) {
        val s = trajectory.speed(t)
        if (s < TideTrajectory.SILENT_SPEED) break
        val interval = (GRAIN_FAST_MS + (GRAIN_SLOW_MS - GRAIN_FAST_MS) * (1f - s.coerceIn(0f, 1f))).roundToInt()
        val at = (t * 1000f).roundToInt()
        steps += PrimitiveStep(grainPrimitive, (0.12f + 0.5f * s).coerceIn(0.05f, 1f), max(at - clockMs, 0))
        clockMs = at + grainMs
        t = (at + interval) / 1000f
    }
    return if (steps.isEmpty()) HapticPlan.Silent else HapticPlan.Primitives(steps)
}

private fun tideWaveform(trajectory: TideTrajectory, duration: Float, toDark: Boolean, launch: Boolean): HapticPlan {
    val samples = (duration * 1000f / WAVE_STEP_MS).roundToInt() + 2
    val amp = FloatArray(samples)
    var nextGrainMs = 0f
    for (k in 0 until samples) {
        val tMs = k * WAVE_STEP_MS.toFloat()
        val s = trajectory.speed(tMs / 1000f)
        amp[k] = body(s)
        if (tMs >= nextGrainMs && s >= TideTrajectory.SILENT_SPEED) {
            amp[k] = min(1f, amp[k] + 0.3f * s)
            nextGrainMs = tMs + GRAIN_FAST_MS + (GRAIN_SLOW_MS - GRAIN_FAST_MS) * (1f - s)
        }
    }
    if (launch) for (k in 0 until min(3, samples)) amp[k] = max(amp[k], if (toDark) 0.6f else 0.5f)
    amp[samples - 1] = 0f
    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()
    var i = 0
    while (i < samples) {
        val a = amplitude(amp[i])
        var j = i + 1
        while (j < samples && amplitude(amp[j]) == a) j++
        timings += ((j - i) * WAVE_STEP_MS).toLong()
        amplitudes += a
        i = j
    }
    return HapticPlan.Waveform(timings.toLongArray(), amplitudes.toIntArray())
}

private const val LAUNCH_INTENSITY = 0.55f
private const val ENVELOPE_STEP_MS = 24L
private const val GRAIN_FAST_MS = 35f
private const val GRAIN_SLOW_MS = 140f
private const val DEFAULT_GRAIN_MS = 12
private const val WAVE_STEP_MS = 4
