package com.halo.core.designsystem.theme

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.halo.core.designsystem.feedback.HaloFeedbackEngine
import com.halo.core.designsystem.feedback.HapticCapabilities
import com.halo.core.designsystem.feedback.HapticTier
import com.halo.core.designsystem.feedback.Patterns
import com.halo.core.designsystem.feedback.TideTrajectory
import com.halo.core.designsystem.runtime.HaloRuntime

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
    /** Glass struck lightly: the glass halo's tap. */
    Ping,
    /**
     * The theme Tide: a deep release, then a rush of water and sand that thins out as the wave slows. Played live by
     * the theme transition from the wave's own motion; `perform(Tide)` previews a full wave.
     */
    Tide,
}

/**
 * Plays [HaloHaptic] tokens: each is a designed pattern (haptic timeline + synthesised sound, proposal 0002) rendered
 * for the richest tier this phone's actuator supports, with the sound on the same frame. Components only ever pick a
 * token; the platform APIs live in the `feedback` package.
 *
 * The system still has the last word: the touch-feedback setting and intensity (haptics), the ringer and the
 * touch-sounds setting (sound).
 */
@Stable
class HaloHaptics internal constructor(
    private val view: View,
    private val engine: HaloFeedbackEngine,
    private val observer: (HaloHaptic, Float) -> Unit,
) {
    /** How this phone renders haptics (envelope, primitives, amplitude, basic or none). */
    val tier: HapticTier get() = engine.capabilities.tier

    /** What the actuator reported when probed. */
    val capabilities: HapticCapabilities get() = engine.capabilities

    /** In-app switches; the system settings still apply on top. */
    var hapticsEnabled: Boolean
        get() = engine.hapticsEnabled
        set(value) { engine.hapticsEnabled = value }
    var soundEnabled: Boolean
        get() = engine.soundEnabled
        set(value) { engine.soundEnabled = value }

    /** Per-phone haptic strength (0.5 … 1.5): actuators differ in raw strength, tune it on the device. */
    var strength: Float
        get() = engine.hapticStrength
        set(value) { engine.hapticStrength = value }

    /** Plays [haptic]; [pan] places its sound left (−1) to right (+1), e.g. by the control's position. */
    fun perform(haptic: HaloHaptic, pan: Float = 0f) {
        observer(haptic, 1f)
        if (haptic == HaloHaptic.Tide) {
            // A preview: a full wave at 1×, its sound sweeping right to left.
            engine.startTide(toDark = false, trajectory = TideTrajectory.launch(PREVIEW_OMEGA, 1f, PREVIEW_KICK), view).release()
            return
        }
        engine.play(Patterns.of(haptic), view, pan)
    }

    /** Starts the Tide's feedback for a wave following [trajectory] (used by the theme transition). */
    internal fun tide(toDark: Boolean, trajectory: TideTrajectory): HaloFeedbackEngine.TideHandle {
        observer(HaloHaptic.Tide, 1f)
        return engine.startTide(toDark, trajectory, view)
    }

    /** The reduced-motion theme change (crossfade). */
    internal fun themeFade() {
        observer(HaloHaptic.Tide, 0.3f)
        engine.play(Patterns.ThemeFade, view)
    }

    /** A tick with variable [intensity] (0..1): strength, sharpness and pitch rise together (charging progress). */
    fun tick(intensity: Float) {
        val scale = intensity.coerceIn(0.05f, 1f)
        observer(HaloHaptic.Tick, scale)
        engine.play(Patterns.tick(scale), view)
    }

    /** Stops a running haptic (e.g. a hold released before commit). */
    fun cancel() {
        engine.cancel()
    }
}

/** A preview Tide matches the real one at 1×: ω = √(Tide stiffness), launched with the same kick. */
private val PREVIEW_OMEGA = kotlin.math.sqrt(HaloSpring.Tide.stiffness)
private const val PREVIEW_KICK = 0.5f

/**
 * @param observer receives every token played (and its intensity). The Showroom uses it for the
 * on-screen feedback log, since the emulator cannot vibrate.
 */
@Composable
internal fun rememberHaloHaptics(observer: ((HaloHaptic, Float) -> Unit)?, runtime: HaloRuntime): HaloHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    val currentObserver by rememberUpdatedState(observer)
    return remember(view, context, runtime) {
        val engine = HaloFeedbackEngine.get(context).also { it.attach(runtime.policy) }
        HaloHaptics(view, engine) { haptic, intensity -> currentObserver?.invoke(haptic, intensity) }
    }
}
