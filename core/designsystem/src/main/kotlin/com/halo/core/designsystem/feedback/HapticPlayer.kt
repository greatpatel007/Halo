package com.halo.core.designsystem.feedback

import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Plays [HaloPattern]s on this phone's actuator. Probes it once, compiles each pattern for the richest tier it
 * supports, and caches the built [VibrationEffect]s, so playing is a lookup on the touch frame. Uses
 * [VibrationAttributes.USAGE_TOUCH], so the system's touch-feedback setting and intensity apply.
 */
internal class HapticPlayer(private val vibrator: Vibrator?) {

    val capabilities: HapticCapabilities = probe(vibrator)
    private val touch = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    private val effects = HashMap<HaloPattern, Built>()

    /** Calibration multiplier (see [HapticPlan.scaled]); changing it rebuilds the cached effects. */
    var strength: Float = 1f
        set(value) {
            if (value != field) effects.clear()
            field = value
        }

    fun play(pattern: HaloPattern, view: View) {
        when (val built = effects.getOrPut(pattern) { build(capabilities.plan(pattern).scaled(strength)) }) {
            is Built.Effect -> vibrator?.vibrate(built.effect, touch)
            is Built.Platform -> view.performHapticFeedback(built.constant)
            Built.Nothing -> Unit
        }
    }

    /** Plays a plan built for one moment (e.g. a Tide trajectory): not cached. */
    fun play(plan: HapticPlan, view: View) {
        when (val built = build(plan.scaled(strength))) {
            is Built.Effect -> vibrator?.vibrate(built.effect, touch)
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
        is HapticPlan.Primitives -> {
            val composition = VibrationEffect.startComposition()
            plan.steps.forEach { composition.addPrimitive(it.primitive.id, it.scale, it.delayMs) }
            Built.Effect(composition.compose())
        }
        is HapticPlan.Waveform -> Built.Effect(VibrationEffect.createWaveform(plan.timingsMs, plan.amplitudes, -1))
        is HapticPlan.Basic -> Built.Platform(plan.haptic.constant)
        HapticPlan.Silent -> Built.Nothing
    }

    companion object {
        /** Android 16: envelope effects. */
        private const val ENVELOPE_SDK = 36

        fun probe(vibrator: Vibrator?): HapticCapabilities {
            if (vibrator == null || !vibrator.hasVibrator()) return HapticCapabilities(hasVibrator = false)
            val ids = HaloPrimitive.entries.map { it.id }.toIntArray()
            val supported = vibrator.arePrimitivesSupported(*ids)
            val durations = vibrator.getPrimitiveDurations(*ids)
            val primitives = HaloPrimitive.entries.withIndex()
                .filter { (i, _) -> supported[i] }
                .associate { (i, p) -> p to durations[i] }
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
                qFactor = vibrator.qFactor,
            )
        }
    }
}

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
        SystemHaptic.Confirm -> HapticFeedbackConstants.CONFIRM
        SystemHaptic.Reject -> HapticFeedbackConstants.REJECT
        SystemHaptic.Threshold -> if (Build.VERSION.SDK_INT >= 34) {
            HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        SystemHaptic.Click -> HapticFeedbackConstants.CONTEXT_CLICK
        SystemHaptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
    }
