package com.halo.core.designsystem.theme

import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** The haptic vocabulary. Components pick a token; never call the platform APIs directly. */
enum class HaloHaptic {
    /** Buttons and taps. */
    Tap,
    /** Keypad digits, slider steps, digit roll. */
    Tick,
    ToggleOn,
    ToggleOff,
    /** A swipe / hold reaches its commit point. */
    Threshold,
    /** Success. */
    Confirm,
    /** Invalid input (pair with a shake). */
    Reject,
    /** Rising charge — hold-to-send. */
    Crescendo,
    /** Payment sent. */
    Celebrate,
}

/**
 * Plays [HaloHaptic] tokens. Simple tokens go through [View.performHapticFeedback] so they honour the
 * user's touch-feedback setting; signature moments use [VibrationEffect.Composition] primitives with
 * [VibrationAttributes.USAGE_TOUCH], falling back when the actuator lacks a primitive.
 */
@Stable
class HaloHaptics internal constructor(
    private val view: View,
    private val vibrator: Vibrator?,
    private val observer: (HaloHaptic, Float) -> Unit,
) {
    private val touch = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)

    fun perform(haptic: HaloHaptic) {
        observer(haptic, 1f)
        when (haptic) {
            HaloHaptic.Tap -> view(HapticFeedbackConstants.CONTEXT_CLICK)
            HaloHaptic.Tick -> view(
                if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                else HapticFeedbackConstants.CLOCK_TICK,
            )
            HaloHaptic.ToggleOn -> view(
                if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.TOGGLE_ON
                else HapticFeedbackConstants.CONTEXT_CLICK,
            )
            HaloHaptic.ToggleOff -> view(
                if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.TOGGLE_OFF
                else HapticFeedbackConstants.CLOCK_TICK,
            )
            HaloHaptic.Threshold -> view(
                if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                else HapticFeedbackConstants.LONG_PRESS,
            )
            HaloHaptic.Confirm -> view(HapticFeedbackConstants.CONFIRM)
            HaloHaptic.Reject -> view(HapticFeedbackConstants.REJECT)
            HaloHaptic.Crescendo -> compose(
                HapticFeedbackConstants.LONG_PRESS,
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE to 0.5f,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE to 0.9f,
            )
            HaloHaptic.Celebrate -> compose(
                HapticFeedbackConstants.CONFIRM,
                VibrationEffect.Composition.PRIMITIVE_THUD to 1f,
                VibrationEffect.Composition.PRIMITIVE_SPIN to 0.6f,
                VibrationEffect.Composition.PRIMITIVE_TICK to 0.4f,
            )
        }
    }

    /** A tick with variable [intensity] (0..1) — for progress that "charges up". */
    fun tick(intensity: Float) {
        val scale = intensity.coerceIn(0.05f, 1f)
        observer(HaloHaptic.Tick, scale)
        val v = vibrator
        if (v != null && v.hasVibrator() &&
            v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
        ) {
            v.vibrate(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale)
                    .compose(),
                touch,
            )
        } else {
            view(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /** Stops a running composition (e.g. a hold released before commit). */
    fun cancel() {
        vibrator?.cancel()
    }

    private fun view(constant: Int) {
        view.performHapticFeedback(constant)
    }

    private fun compose(fallback: Int, vararg primitives: Pair<Int, Float>) {
        val v = vibrator
        val ids = primitives.map { it.first }.toIntArray()
        if (v == null || !v.hasVibrator() || !v.areAllPrimitivesSupported(*ids)) {
            view(fallback)
            return
        }
        val composition = VibrationEffect.startComposition()
        primitives.forEach { (id, scale) -> composition.addPrimitive(id, scale) }
        v.vibrate(composition.compose(), touch)
    }
}

/**
 * @param observer receives every token played (and its intensity). The Showroom uses it for the
 * on-screen haptic log, since the emulator cannot vibrate.
 */
@Composable
fun rememberHaloHaptics(observer: ((HaloHaptic, Float) -> Unit)? = null): HaloHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    val currentObserver by rememberUpdatedState(observer)
    return remember(view, context) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        HaloHaptics(view, vibrator) { haptic, intensity -> currentObserver?.invoke(haptic, intensity) }
    }
}
