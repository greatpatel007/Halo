package com.halo.core.designsystem.feedback

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.halo.core.designsystem.runtime.HaloPolicy
import com.halo.core.designsystem.theme.HaloHaptic

/**
 * One per process: the actuator probe, the compiled-effect cache and the audio thread are shared by every screen.
 * Plays a pattern's haptic and its sound on the same frame, so they read as one event.
 */
internal class HaloFeedbackEngine private constructor(context: Context) {

    private val haptics = HapticPlayer(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        },
    )
    private val sound = SoundEngine(context)

    val capabilities: HapticCapabilities get() = haptics.capabilities

    /** Measured audio output latency (ms), −1 until measured. */
    val audioLatencyMs: Float get() = sound.outputLatencyMs

    /** The user's settings below survive restarts (a few bytes, written off the main thread by `apply`). */
    private val settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)

    /** In-app switches (the system settings apply on top of these). */
    var hapticsEnabled: Boolean
        get() = hapticsState
        set(value) {
            hapticsState = value
            settings.edit { putBoolean(KEY_HAPTICS, value) }
        }
    private var hapticsState by mutableStateOf(settings.getBoolean(KEY_HAPTICS, true))

    var soundEnabled: Boolean
        get() = soundState
        set(value) {
            soundState = value
            settings.edit { putBoolean(KEY_SOUND, value) }
        }
    private var soundState by mutableStateOf(settings.getBoolean(KEY_SOUND, true))

    /** Per-phone haptic strength (0.5 … 1.5); 1 = as authored. */
    var hapticStrength: Float
        get() = strengthState
        set(value) {
            strengthState = value.coerceIn(MIN_STRENGTH, MAX_STRENGTH)
            haptics.strength = strengthState
            settings.edit { putFloat(KEY_STRENGTH, strengthState) }
        }
    private var strengthState by mutableStateOf(settings.getFloat(KEY_STRENGTH, 1f).coerceIn(MIN_STRENGTH, MAX_STRENGTH))

    /** Battery saver or heat: long decorative feedback (the Tide's rush and water) shrinks to its launch. */
    @Volatile private var conserving = false
    private var policy: HaloPolicy? = null

    /** Takes sound etiquette and power policy from the runtime (event-driven; the engine never polls the system). */
    fun attach(policy: HaloPolicy) {
        if (this.policy === policy) return
        this.policy = policy
        policy.addListener {
            sound.systemAllowed = policy.soundAllowed
            conserving = policy.conserving
        }
    }

    init {
        haptics.strength = strengthState
        sound.prepare(HaloHaptic.entries.mapNotNull { Patterns.of(it).sound } + (0..10).mapNotNull { Patterns.tick(it / 10f).sound })
    }

    /** Plays [pattern]: haptic first (immediate), sound on the same frame. [pan] places the sound (−1 … +1). */
    fun play(pattern: HaloPattern, view: View, pan: Float = 0f) {
        sound.enabled = soundEnabled
        val heard = pattern.sound?.let { sound.play(it, pan) } == true
        if (!hapticsEnabled) return
        // Land the touch and its sound together: audio leaves the speaker later than the actuator moves, so a paired
        // haptic waits for the measured difference (capped, so touch still feels immediate).
        val wait = if (heard) syncDelayMs(sound.outputLatencyMs) else 0L
        if (wait > 0L) view.postDelayed({ haptics.play(pattern, view) }, wait) else haptics.play(pattern, view)
    }

    fun cancel() = haptics.cancel()

    /**
     * Starts the Tide's feedback: the haptic rush pre-computed from [trajectory] and the live water voice following
     * it. The handle retargets both on a reversal and releases them when the wave is over.
     */
    fun startTide(toDark: Boolean, trajectory: TideTrajectory, view: View): TideHandle {
        if (conserving) {
            // Conserving power: just the release (a short hit and thud), no rush, no live water voice.
            play(Patterns.TideLaunch, view)
            return TideHandle(null, view, toDark, rush = false)
        }
        val voice = TideVoice(toDark, trajectory)
        if (hapticsEnabled) haptics.play(capabilities.tidePlan(trajectory, toDark, launch = true), view)
        sound.enabled = soundEnabled
        val playing = sound.play(voice)
        return TideHandle(if (playing) voice else null, view, toDark, rush = true)
    }

    /** A running Tide's feedback. */
    inner class TideHandle internal constructor(
        private val voice: TideVoice?,
        private val view: View,
        private var toDark: Boolean,
        private val rush: Boolean,
    ) {
        /** Places the water's sound: −1 left … +1 right (the front's position across the screen). */
        fun pan(value: Float) {
            voice?.pan = value.coerceIn(-1f, 1f)
        }

        /** A mid-wave reversal: the water is pulled back, keeping its energy (no new launch hit). */
        fun reverse(trajectory: TideTrajectory, toDark: Boolean) {
            this.toDark = toDark
            if (!rush) return
            haptics.cancel()
            if (hapticsEnabled) haptics.play(capabilities.tidePlan(trajectory, toDark, launch = false), view)
            voice?.retarget(trajectory)
        }

        fun release() {
            voice?.release()
        }
    }

    companion object {
        private const val SETTINGS = "halo_feedback"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_SOUND = "sound"
        private const val KEY_STRENGTH = "strength"
        private const val MIN_STRENGTH = 0.5f
        private const val MAX_STRENGTH = 1.5f

        /** Typical time from vibrate() to the actuator moving (ms). */
        private const val HAPTIC_LATENCY_MS = 15f
        /** Never hold a touch's haptic back longer than this, whatever the audio latency. */
        private const val MAX_SYNC_DELAY_MS = 25f

        internal fun syncDelayMs(audioLatencyMs: Float): Long =
            if (audioLatencyMs < 0f) 0L else (audioLatencyMs - HAPTIC_LATENCY_MS).coerceIn(0f, MAX_SYNC_DELAY_MS).toLong()

        @Volatile private var instance: HaloFeedbackEngine? = null

        fun get(context: Context): HaloFeedbackEngine =
            instance ?: synchronized(this) {
                instance ?: HaloFeedbackEngine(context.applicationContext).also { instance = it }
            }
    }
}
