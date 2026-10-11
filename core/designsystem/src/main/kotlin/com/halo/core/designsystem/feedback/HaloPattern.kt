package com.halo.core.designsystem.feedback

import androidx.compose.runtime.Immutable

/*
 * Halo Sensory (proposal 0002): one authoring model for touch feedback, in the spirit of Apple's Core Haptics.
 * A pattern is a short timeline of haptic events (transient taps and held, ramping vibrations, each with an intensity
 * and a sharpness) plus the sound that plays with it. The renderer turns the same pattern into the best effect this
 * phone's actuator can play (see HapticPlan.kt), so a token feels as close to the same as the hardware allows.
 */

/** Perceptual strength and sharpness, both 0..1. Sharp = crisp and high, dull = round and low. */
@Immutable
data class HapticLevel(val intensity: Float, val sharpness: Float)

/** One event on a pattern's timeline. */
sealed interface HapticEvent {
    /** Start, ms from the beginning of the pattern. */
    val atMs: Int
}

/** A single tap of the actuator. */
@Immutable
data class Transient(override val atMs: Int, val level: HapticLevel) : HapticEvent

/** A held vibration that ramps linearly from [from] to [to] over [durationMs]. */
@Immutable
data class Continuous(
    override val atMs: Int,
    val durationMs: Int,
    val from: HapticLevel,
    val to: HapticLevel,
) : HapticEvent

/** Basic platform feedback for actuators that can only buzz (tier 4). Only strong moments get one. */
enum class SystemHaptic { Confirm, Reject, Threshold, Click, Tick }

/**
 * A designed piece of touch feedback.
 *
 * @param events the haptic timeline, in start order.
 * @param sound the sound that plays with it (null = silent).
 * @param basic what to play on a basic vibration motor; null drops it there (no haptic beats a mushy one).
 */
@Immutable
class HaloPattern(
    val events: List<HapticEvent>,
    val sound: HaloSoundCue?,
    val basic: SystemHaptic?,
) {
    val durationMs: Int
        get() = events.maxOfOrNull {
            when (it) {
                is Transient -> it.atMs
                is Continuous -> it.atMs + it.durationMs
            }
        } ?: 0

    val hasContinuous: Boolean get() = events.any { it is Continuous }
}
