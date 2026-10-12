package com.halo.core.designsystem.feedback

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Plays [HaloPattern]s on this phone's actuator. Probes it once, compiles each pattern for the richest tier it
 * supports, and caches the built [VibrationEffect]s, so playing is a lookup on the touch frame. Plays as touch feedback
 * ([VibrationAttributes.USAGE_TOUCH] on Android 13+, sonification audio attributes before), so the system's
 * touch-feedback setting and intensity apply.
 *
 * Tiers by Android version: envelopes 16+, primitives 12+ (where the actuator has them), amplitude waveforms and the
 * platform's own effects on every version.
 */
internal class HapticPlayer(private val vibrator: Vibrator?) {

    val capabilities: HapticCapabilities = probe(vibrator)
    private val touch: Any? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH) else null
    @Suppress("DEPRECATION") // The touch attributes of Android 8–12.
    private val legacyTouch: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val effects = HashMap<HaloPattern, Built>()

    /** Calibration multiplier (see [HapticPlan.scaled]); changing it rebuilds the cached effects. */
    var strength: Float = 1f
        set(value) {
            if (value != field) effects.clear()
            field = value
        }

    fun play(pattern: HaloPattern, view: View) {
        when (val built = effects.getOrPut(pattern) { build(capabilities.plan(pattern).scaled(strength)) }) {
            is Built.Effect -> vibrate(built.effect)
            is Built.Platform -> view.performHapticFeedback(built.constant)
            Built.Nothing -> Unit
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && touch is VibrationAttributes) {
            v.vibrate(effect, touch)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, legacyTouch)
        }
    }

    /** Plays a plan built for one moment (e.g. a Tide trajectory): not cached. */
    fun play(plan: HapticPlan, view: View) {
        when (val built = build(plan.scaled(strength))) {
            is Built.Effect -> vibrate(built.effect)
            is Built.Platform -> view.performHapticFeedback(built.constant)
            Built.Nothing -> Unit
        }
    }

    fun cancel() {
        vibrator?.cancel()
    }

    private sealed interface Built {
        class Effect(val effect: VibrationEffect) : Built
        class Platform(val constant: Int) : Built
        data object Nothing : Built
    }

    private fun build(plan: HapticPlan): Built = when (plan) {
        is HapticPlan.Envelope -> if (Build.VERSION.SDK_INT >= ENVELOPE_SDK) {
            val builder = VibrationEffect.BasicEnvelopeBuilder().setInitialSharpness(plan.initialSharpness)
            plan.points.forEach { builder.addControlPoint(it.intensity, it.sharpness, it.durationMs) }
            Built.Effect(builder.build())
        } else {
            Built.Nothing
        }
        is HapticPlan.Primitives -> if (Build.VERSION.SDK_INT >= PRIMITIVES_SDK) {
            val composition = VibrationEffect.startComposition()
            plan.steps.forEach { composition.addPrimitive(it.primitive.id, it.scale, it.delayMs) }
            Built.Effect(composition.compose())
        } else {
            Built.Nothing
        }
        is HapticPlan.Waveform -> Built.Effect(VibrationEffect.createWaveform(plan.timingsMs, plan.amplitudes, -1))
        is HapticPlan.Basic -> Built.Platform(plan.haptic.constant)
        HapticPlan.Silent -> Built.Nothing
    }

    companion object {
        /** Android 16: envelope effects. */
        private const val ENVELOPE_SDK = 36
        /** Android 12: primitive durations (and the low tick and spin primitives) are known. */
        private const val PRIMITIVES_SDK = Build.VERSION_CODES.S

        @SuppressLint("WrongConstant") // ids come from HaloPrimitive.id, which maps to the platform's primitive constants.
        fun probe(vibrator: Vibrator?): HapticCapabilities {
            if (vibrator == null || !vibrator.hasVibrator()) return HapticCapabilities(hasVibrator = false)
            val primitives = if (Build.VERSION.SDK_INT >= PRIMITIVES_SDK) {
                val ids = HaloPrimitive.entries.map { it.id }.toIntArray()
                val supported = vibrator.arePrimitivesSupported(*ids)
                val durations = vibrator.getPrimitiveDurations(*ids)
                HaloPrimitive.entries.withIndex()
                    .filter { (i, _) -> supported[i] }
                    .associate { (i, p) -> p to durations[i] }
            } else {
                emptyMap()
            }
            var envelope = false
            var minPoint = 0L
            var maxPoint = 0L
            var maxPoints = 0
            if (Build.VERSION.SDK_INT >= ENVELOPE_SDK && vibrator.areEnvelopeEffectsSupported()) {
                val info = vibrator.envelopeEffectInfo
                envelope = true
                minPoint = info.minControlPointDurationMillis
                maxPoint = info.maxControlPointDurationMillis
                maxPoints = info.maxSize
            }
            return HapticCapabilities(
                hasVibrator = true,
                envelope = envelope,
                minControlPointMs = minPoint,
                maxControlPointMs = maxPoint,
                maxEnvelopePoints = maxPoints,
                primitives = primitives,
                amplitudeControl = vibrator.hasAmplitudeControl(),
                qFactor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) vibrator.qFactor else Float.NaN,
            )
        }
    }
}

@get:RequiresApi(Build.VERSION_CODES.S)
private val HaloPrimitive.id: Int
    get() = when (this) {
        HaloPrimitive.Click -> VibrationEffect.Composition.PRIMITIVE_CLICK
        HaloPrimitive.Tick -> VibrationEffect.Composition.PRIMITIVE_TICK
        HaloPrimitive.LowTick -> VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        HaloPrimitive.Thud -> VibrationEffect.Composition.PRIMITIVE_THUD
        HaloPrimitive.SlowRise -> VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
        HaloPrimitive.QuickRise -> VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
        HaloPrimitive.QuickFall -> VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
        HaloPrimitive.Spin -> VibrationEffect.Composition.PRIMITIVE_SPIN
    }

private val SystemHaptic.constant: Int
    get() = when (this) {
        // CONFIRM and REJECT arrived in Android 11; older phones ignore unknown constants, so use the nearest.
        SystemHaptic.Confirm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        SystemHaptic.Reject -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        SystemHaptic.Threshold -> if (Build.VERSION.SDK_INT >= 34) {
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        SystemHaptic.Click -> HapticFeedbackConstants.CONTEXT_CLICK
        SystemHaptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
    }
