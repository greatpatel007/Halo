package com.halo.core.designsystem.feedback

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Android phones use very different vibration hardware, from tuned linear actuators to cheap spinning motors. Halo
 * probes the actuator once and picks the richest tier it supports; every pattern then compiles to a plan for that
 * tier. Plans are pure data (this file has no Android dependencies) so the mapping is unit-tested; HapticPlayer turns
 * a plan into a VibrationEffect.
 */

/** Richest way this phone can render a pattern. */
enum class HapticTier {
    /** Envelope effects: intensity *and* sharpness curves (Android 16+ on capable actuators). */
    Envelope,
    /** Composition primitives tuned by the OEM (CLICK, TICK, THUD, rises and falls). */
    Primitives,
    /** Amplitude-controlled waveforms, synthesised in short steps. */
    Amplitude,
    /** On/off motor: only the platform's basic feedback, for strong moments. */
    Basic,
    /** No vibrator. */
    None,
}

/** Composition primitives Halo uses, independent of the platform constants. */
enum class HaloPrimitive { Click, Tick, LowTick, Thud, SlowRise, QuickRise, QuickFall, Spin }

/** What the actuator can do, probed once (see HapticPlayer.probe). */
data class HapticCapabilities(
    val hasVibrator: Boolean,
    val envelope: Boolean = false,
    val minControlPointMs: Long = 0,
    val maxControlPointMs: Long = 0,
    val maxEnvelopePoints: Int = 0,
    /** Supported primitives and their real durations on this actuator (ms). */
    val primitives: Map<HaloPrimitive, Int> = emptyMap(),
    val amplitudeControl: Boolean = false,
    /** Resonance sharpness; high-Q actuators ring longer, so pulses are shortened. NaN when unknown. */
    val qFactor: Float = Float.NaN,
) {
    val tier: HapticTier
        get() = when {
            !hasVibrator -> HapticTier.None
            envelope && minControlPointMs > 0 && maxEnvelopePoints >= MIN_ENVELOPE_POINTS -> HapticTier.Envelope
            primitives.keys.containsAll(CORE_PRIMITIVES) -> HapticTier.Primitives
            amplitudeControl -> HapticTier.Amplitude
            else -> HapticTier.Basic
        }

    private companion object {
        val CORE_PRIMITIVES = setOf(HaloPrimitive.Click, HaloPrimitive.Tick)
        const val MIN_ENVELOPE_POINTS = 8
    }
}

/** A pattern compiled for one tier. */
sealed interface HapticPlan {
    /** Control points from intensity 0: each ramps to (intensity, sharpness) over durationMs. */
    data class Envelope(val initialSharpness: Float, val points: List<EnvelopePoint>) : HapticPlan
    data class Primitives(val steps: List<PrimitiveStep>) : HapticPlan
    data class Waveform(val timingsMs: LongArray, val amplitudes: IntArray) : HapticPlan
    data class Basic(val haptic: SystemHaptic) : HapticPlan
    data object Silent : HapticPlan
}

data class EnvelopePoint(val intensity: Float, val sharpness: Float, val durationMs: Long)

/** A primitive at [scale], after [delayMs] of silence following the previous one. */
data class PrimitiveStep(val primitive: HaloPrimitive, val scale: Float, val delayMs: Int)

/** Compiles [pattern] for this phone. */
fun HapticCapabilities.plan(pattern: HaloPattern): HapticPlan {
    if (pattern.events.isEmpty()) return HapticPlan.Silent
    return when (tier) {
        // Taps are crisper as tuned primitives; anything held needs the envelope's sharpness curves.
        HapticTier.Envelope -> if (pattern.hasContinuous) envelopePlan(pattern) else primitivePlan(pattern)
        HapticTier.Primitives -> primitivePlan(pattern)
        HapticTier.Amplitude -> waveformPlan(pattern)
        HapticTier.Basic -> pattern.basic?.let { HapticPlan.Basic(it) } ?: HapticPlan.Silent
        HapticTier.None -> HapticPlan.Silent
    }
}

// ------------------------------------------------------------------------------------------- envelope (tier 1)

private fun HapticCapabilities.envelopePlan(pattern: HaloPattern): HapticPlan {
    val step = max(minControlPointMs, 1L)
    val points = mutableListOf<EnvelopePoint>()
    var cursor = 0L
    var sharpness = pattern.events.first().let { if (it is Transient) it.level.sharpness else (it as Continuous).from.sharpness }
    fun hold(untilMs: Long) {
        var gap = untilMs - cursor
        if (gap < step) return
        // Stay silent until the event (split into the actuator's longest allowed control point).
        val longest = if (maxControlPointMs > 0) maxControlPointMs else gap
        while (gap > 0) {
            val d = minOf(gap, longest).coerceAtLeast(step)
            points += EnvelopePoint(0f, sharpness, d)
            cursor += d
            gap -= d
        }
    }
    for (event in pattern.events) {
        hold(event.atMs.toLong())
        when (event) {
            is Transient -> {
                sharpness = event.level.sharpness
                // A tap: snap up, fall a little slower when dull (rounder), as fast as allowed when sharp.
                val fall = (step * (1f + 2f * (1f - sharpness))).roundToLong(step)
                points += EnvelopePoint(event.level.intensity, sharpness, step)
                points += EnvelopePoint(0f, sharpness, fall)
                cursor += step + fall
            }
            is Continuous -> {
                points += EnvelopePoint(event.from.intensity, event.from.sharpness, step)
                points += EnvelopePoint(event.to.intensity, event.to.sharpness, max(event.durationMs.toLong(), step))
                points += EnvelopePoint(0f, event.to.sharpness, step)
                sharpness = event.to.sharpness
                cursor += step * 2 + max(event.durationMs.toLong(), step)
            }
        }
    }
    // Too long for this actuator: fall back to primitives rather than truncating mid-ramp.
    if (maxEnvelopePoints > 0 && points.size > maxEnvelopePoints) return primitivePlan(pattern)
    val first = pattern.events.first()
    val initial = if (first is Transient) first.level.sharpness else (first as Continuous).from.sharpness
    return HapticPlan.Envelope(initial, points)
}

private fun Float.roundToLong(min: Long): Long = max(this.roundToInt().toLong(), min)

// ------------------------------------------------------------------------------------------- primitives (tier 2)

private fun HapticCapabilities.primitivePlan(pattern: HaloPattern): HapticPlan {
    val steps = mutableListOf<PrimitiveStep>()
    var endOfLast = 0
    for (event in pattern.events) {
        val (primitive, scale) = when (event) {
            is Transient -> transientPrimitive(event.level) to event.level.intensity
            is Continuous -> continuousPrimitive(event) to max(event.from.intensity, event.to.intensity)
        }
        val chosen = primitive.takeIf { it in primitives } ?: HaloPrimitive.Click
        // Primitives play back to back: an event that overlaps the previous one starts when it ends.
        val start = max(event.atMs, endOfLast)
        steps += PrimitiveStep(chosen, scale.coerceIn(MIN_SCALE, 1f), start - endOfLast)
        endOfLast = start + (primitives[chosen] ?: DEFAULT_PRIMITIVE_MS)
    }
    return HapticPlan.Primitives(steps)
}

/** Sharp taps tick or click; dull ones thud. Very light sharp taps are ticks (the lightest primitive). */
private fun HapticCapabilities.transientPrimitive(level: HapticLevel): HaloPrimitive = when {
    level.sharpness >= 0.6f && level.intensity < 0.5f -> HaloPrimitive.Tick
    level.sharpness >= 0.35f -> HaloPrimitive.Click
    level.intensity >= 0.6f && HaloPrimitive.Thud in primitives -> HaloPrimitive.Thud
    HaloPrimitive.LowTick in primitives -> HaloPrimitive.LowTick
    else -> HaloPrimitive.Tick
}

private fun continuousPrimitive(event: Continuous): HaloPrimitive = when {
    event.to.intensity < event.from.intensity -> HaloPrimitive.QuickFall
    event.durationMs <= QUICK_RISE_MAX_MS -> HaloPrimitive.QuickRise
    else -> HaloPrimitive.SlowRise
}

// ------------------------------------------------------------------------------------------- waveform (tier 3)

private fun HapticCapabilities.waveformPlan(pattern: HaloPattern): HapticPlan {
    val total = pattern.durationMs + TAIL_MS
    val samples = (total / STEP_MS) + 1
    val amp = FloatArray(samples)
    // High-Q actuators keep ringing after the drive stops: shorter pulses keep taps crisp.
    val ringing = if (qFactor.isNaN()) 1f else (10f / qFactor).coerceIn(0.6f, 1f)
    for (event in pattern.events) {
        when (event) {
            is Transient -> {
                // Sharp = one short full-strength pulse; dull = longer and softer-edged.
                val length = ((PULSE_SHARP_MS + (PULSE_DULL_MS - PULSE_SHARP_MS) * (1f - event.level.sharpness)) * ringing)
                val n = max((length / STEP_MS).roundToInt(), 1)
                val start = event.atMs / STEP_MS
                for (i in 0 until n) {
                    val edge = if (event.level.sharpness < 0.5f && (i == 0 || i == n - 1)) 0.6f else 1f
                    val k = start + i
                    if (k < samples) amp[k] = max(amp[k], event.level.intensity * edge)
                }
            }
            is Continuous -> {
                val start = event.atMs / STEP_MS
                val n = max(event.durationMs / STEP_MS, 1)
                for (i in 0..n) {
                    val t = i / n.toFloat()
                    val k = start + i
                    if (k < samples) amp[k] = max(amp[k], event.from.intensity + (event.to.intensity - event.from.intensity) * t)
                }
            }
        }
    }
    // Run-length encode into (duration, amplitude) segments; amplitude is perceptual → motor drive.
    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()
    var i = 0
    while (i < samples) {
        val a = amplitude(amp[i])
        var j = i + 1
        while (j < samples && amplitude(amp[j]) == a) j++
        timings += ((j - i) * STEP_MS).toLong()
        amplitudes += a
        i = j
    }
    return HapticPlan.Waveform(timings.toLongArray(), amplitudes.toIntArray())
}

/**
 * The same plan, [k] times as strong (per-phone calibration: actuators differ in raw strength). Intensities, scales
 * and amplitudes are clamped to what the hardware accepts; timing and sharpness are untouched.
 */
fun HapticPlan.scaled(k: Float): HapticPlan = if (k == 1f) this else when (this) {
    is HapticPlan.Envelope -> copy(points = points.map { it.copy(intensity = (it.intensity * k).coerceIn(0f, 1f)) })
    is HapticPlan.Primitives -> copy(steps = steps.map { it.copy(scale = (it.scale * k).coerceIn(MIN_SCALE, 1f)) })
    is HapticPlan.Waveform -> copy(amplitudes = IntArray(amplitudes.size) { i ->
        if (amplitudes[i] == 0) 0 else (amplitudes[i] * k).toInt().coerceIn(1, MAX_AMPLITUDE)
    })
    is HapticPlan.Basic, HapticPlan.Silent -> this
}

/** Perceived strength grows faster than drive amplitude at the low end: a gamma keeps light taps feelable. */
internal fun amplitude(intensity: Float): Int =
    if (intensity <= 0f) 0 else (MAX_AMPLITUDE * intensity.coerceIn(0f, 1f).pow(AMPLITUDE_GAMMA)).roundToInt().coerceIn(1, MAX_AMPLITUDE)

private const val MIN_SCALE = 0.05f
private const val DEFAULT_PRIMITIVE_MS = 12
private const val QUICK_RISE_MAX_MS = 150
private const val STEP_MS = 4
private const val TAIL_MS = 8
private const val PULSE_SHARP_MS = 8f
private const val PULSE_DULL_MS = 20f
private const val MAX_AMPLITUDE = 255
private const val AMPLITUDE_GAMMA = 0.7f
