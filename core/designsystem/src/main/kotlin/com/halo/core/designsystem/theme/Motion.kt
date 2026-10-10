package com.halo.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Spring tokens — the default for anything that moves. Physics-based, interruptible,
 * retargetable mid-flight. Never use raw `spring(...)` outside this file.
 */
enum class HaloSpring(val dampingRatio: Float, val stiffness: Float) {
    /** Press-in, instant feedback. No overshoot. */
    Snap(dampingRatio = 1f, stiffness = 2000f),
    /** Release, small icons, checks. Tiny settle. */
    Quick(dampingRatio = 0.8f, stiffness = 1000f),
    /** Size / position / layout changes. */
    Standard(dampingRatio = 0.9f, stiffness = 500f),
    /** Selection pills, morphs, success pop. Visible overshoot. */
    Expressive(dampingRatio = 0.65f, stiffness = 400f),
    /** Screens, sheets, shared elements, large surfaces. */
    Gentle(dampingRatio = 1f, stiffness = 220f),
    /** Odometer digit roll. */
    Digit(dampingRatio = 0.75f, stiffness = 700f),
    /**
     * Physical mechanisms — switch thumbs, sliders with mass. Measured from the toggle reference:
     * ~330ms to settle, critically damped, never bounces.
     */
    Mechanical(dampingRatio = 1f, stiffness = 400f),
    /**
     * Full-screen water — the theme tide. Apple's "smooth" spring (bounce 0) with a ~2.4s perceptual duration,
     * stiffness = (2π / duration)². Launched with a kick, it bursts from the trigger and decelerates as it runs
     * out, like water up a beach; it can be reversed mid-flight and keeps its momentum.
     */
    Tide(dampingRatio = 1f, stiffness = 7f),
    /**
     * Light travelling round the glass halo. Apple's "smooth" spring (bounce 0) with a ~1.8s perceptual duration,
     * stiffness = (2π / duration)². Launched with a kick like [Tide], so the light leaves at full speed and only
     * decelerates into its resting place.
     */
    Orbit(dampingRatio = 1f, stiffness = 12f),
}

/** Fade durations — color, alpha and progress only. */
enum class HaloFade(val millis: Int) {
    Fast(120),
    Base(200),
    Slow(320),
}

object HaloEasing {
    /** Things entering the screen. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Things leaving the screen. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** On-screen color / alpha changes. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Linear: Easing = LinearEasing
}

/**
 * Motion system. [scale] slows everything down (Showroom slow-mo: 2x / 5x / 10x). Springs are slowed by
 * dividing stiffness by scale^2, which stretches their period by exactly [scale].
 * [reduced] honours "Remove animations": spatial springs snap, fades stay but shorten.
 */
@Immutable
class HaloMotion(
    val scale: Float = 1f,
    val reduced: Boolean = false,
) {
    fun <T> spring(token: HaloSpring, visibilityThreshold: T? = null): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            spring(
                dampingRatio = token.dampingRatio,
                stiffness = token.stiffness / (scale * scale),
                visibilityThreshold = visibilityThreshold,
            )
        }

    fun <T> fade(
        token: HaloFade = HaloFade.Base,
        easing: Easing = HaloEasing.Standard,
        delayMillis: Int = 0,
    ): FiniteAnimationSpec<T> {
        val duration = if (reduced) HaloFade.Fast.millis else token.millis
        return tween(
            durationMillis = (duration * scale).roundToInt(),
            delayMillis = if (reduced) 0 else (delayMillis * scale).roundToInt(),
            easing = easing,
        )
    }

    /**
     * Time-bound progress the user is waiting on (e.g. hold-to-confirm). Scaled by slow-mo, never
     * removed by reduced motion: the duration is part of the interaction contract.
     */
    fun <T> timed(
        durationMillis: Int,
        easing: Easing = HaloEasing.Linear,
        delayMillis: Int = 0,
    ): DurationBasedAnimationSpec<T> = tween(
        durationMillis = (durationMillis * scale).roundToInt(),
        delayMillis = (delayMillis * scale).roundToInt(),
        easing = easing,
    )

    /** Enter stagger: 30ms per item, capped at 6 items. */
    fun staggerDelayMillis(index: Int): Int =
        if (reduced) 0 else (min(index, STAGGER_CAP) * STAGGER_STEP_MS * scale).roundToInt()

    /** Scales a raw duration (e.g. a pause between choreography beats). */
    fun millis(value: Int): Long = (value * scale).roundToInt().toLong()

    /** Distance content rises while entering. */
    val enterRise: Dp get() = if (reduced) 0.dp else 12.dp

    /**
     * Initial velocity (dp/s) for the error shake: a spring from 0 to 0 kicked with this impulse on
     * [HaloSpring.Expressive] gives two decaying oscillations — about 10dp peak.
     */
    val shakeImpulse: Dp get() = if (reduced) 0.dp else 900.dp

    companion object {
        const val STAGGER_STEP_MS = 30
        const val STAGGER_CAP = 6
        /** Exit choreography runs at ~60% of enter. */
        const val EXIT_RATIO = 0.6f
    }
}
