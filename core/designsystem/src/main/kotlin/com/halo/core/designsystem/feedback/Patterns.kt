package com.halo.core.designsystem.feedback

import com.halo.core.designsystem.theme.HaloHaptic

/*
 * The authored feedback for every token: haptic timeline + sound, designed together so they read as one event.
 * Intensity/sharpness follow Apple's vocabulary: light, crisp taps for frequent actions; firmer, rounder hits for
 * commits; a two-step rise for success; three falling dull hits for rejection. The basic-motor fallback keeps only
 * moments worth a buzz.
 */
internal object Patterns {

    private fun tap(at: Int, intensity: Float, sharpness: Float) = Transient(at, HapticLevel(intensity, sharpness))

    val Tap = HaloPattern(
        events = listOf(tap(0, 0.55f, 0.5f)),
        sound = HaloSoundCue.Click(pitchHz = 1318.51f, brightness = 0.5f, decayMs = 28, gain = 0.18f),
        basic = SystemHaptic.Click,
    )

    val Tick: HaloPattern get() = tick(0.35f)

    val ToggleOn = HaloPattern(
        // A crisp click with a lighter settle as the thumb lands.
        events = listOf(tap(0, 0.6f, 0.7f), tap(38, 0.28f, 0.9f)),
        sound = HaloSoundCue.Click(pitchHz = 1318.51f, brightness = 0.6f, decayMs = 30, gain = 0.2f),
        basic = SystemHaptic.Click,
    )

    val ToggleOff = HaloPattern(
        events = listOf(tap(0, 0.5f, 0.4f)),
        sound = HaloSoundCue.Click(pitchHz = 659.26f, brightness = 0.35f, decayMs = 34, gain = 0.18f),
        basic = SystemHaptic.Tick,
    )

    val Threshold = HaloPattern(
        events = listOf(tap(0, 0.9f, 0.9f)),
        sound = HaloSoundCue.Click(pitchHz = 1661.22f, brightness = 0.7f, decayMs = 22, gain = 0.22f),
        basic = SystemHaptic.Threshold,
    )

    val Confirm = HaloPattern(
        // Two steps up: a light tap, then a fuller one, with a rising glass chime.
        events = listOf(tap(0, 0.55f, 0.6f), tap(95, 0.85f, 0.45f)),
        sound = HaloSoundCue.Glass(notesHz = listOf(987.77f, 1318.51f), spacingMs = 95, decayMs = 380, gain = 0.2f),
        basic = SystemHaptic.Confirm,
    )

    val Reject = HaloPattern(
        // Three dull hits, each weaker: a headshake. Always paired with the visual shake.
        events = listOf(tap(0, 0.8f, 0.3f), tap(75, 0.65f, 0.3f), tap(150, 0.45f, 0.3f)),
        sound = HaloSoundCue.Thud(pitchHz = 164.81f, repeats = 2, spacingMs = 90, gain = 0.3f),
        basic = SystemHaptic.Reject,
    )

    val Crescendo = HaloPattern(
        // The start of a charge: a held vibration that swells and sharpens, with rising air.
        events = listOf(Continuous(0, 300, HapticLevel(0.08f, 0.3f), HapticLevel(0.55f, 0.7f))),
        sound = HaloSoundCue.Breath(durationMs = 320, gain = 0.12f),
        basic = null,
    )

    val Celebrate = HaloPattern(
        // A full hit, a bright decaying shimmer, two sparkles; an E-major arpeggio in glass.
        events = listOf(
            tap(0, 1f, 0.5f),
            Continuous(30, 260, HapticLevel(0.6f, 0.8f), HapticLevel(0.05f, 0.4f)),
            tap(320, 0.4f, 1f),
            tap(400, 0.3f, 1f),
        ),
        sound = HaloSoundCue.Glass(
            notesHz = listOf(659.26f, 830.61f, 987.77f, 1318.51f),
            spacingMs = 70,
            decayMs = 700,
            gain = 0.22f,
        ),
        basic = SystemHaptic.Confirm,
    )

    /**
     * A light, very sharp tap and a glass tink. Glass rings at its own resonance (fixed pitch); the liquid inside damps
     * it, so the ring is short.
     */
    val Ping = HaloPattern(
        events = listOf(tap(0, 0.4f, 0.95f)),
        sound = HaloSoundCue.Glass(notesHz = listOf(2637.02f), spacingMs = 0, decayMs = 260, gain = 0.14f),
        basic = null,
    )

    /** Reduced motion's theme change (a crossfade, no water): one soft, round tap and a short breath. */
    val ThemeFade = HaloPattern(
        events = listOf(tap(0, 0.35f, 0.3f)),
        sound = HaloSoundCue.Breath(durationMs = 200, gain = 0.08f),
        basic = null,
    )

    /**
     * A tick whose strength, sharpness and pitch follow [intensity] (charging progress, steps). Quantised to
     * [TICK_LEVELS] cached patterns (finer than anyone can feel or hear), so effects and sounds are built once.
     */
    fun tick(intensity: Float): HaloPattern = ticks[(intensity.coerceIn(0f, 1f) * (TICK_LEVELS - 1)).toInt()]

    private const val TICK_LEVELS = 20
    private val ticks = Array(TICK_LEVELS) { buildTick(it / (TICK_LEVELS - 1f)) }

    private fun buildTick(intensity: Float): HaloPattern {
        val i = intensity.coerceIn(0.05f, 1f)
        return HaloPattern(
            events = listOf(tap(0, i, 0.6f + 0.4f * i)),
            sound = HaloSoundCue.Click(pitchHz = Pentatonic.at(i) * 2f, brightness = 0.75f, decayMs = 12, gain = 0.05f + 0.07f * i),
            basic = null,
        )
    }

    fun of(token: HaloHaptic): HaloPattern = when (token) {
        HaloHaptic.Tap -> Tap
        HaloHaptic.Tick -> Tick
        HaloHaptic.ToggleOn -> ToggleOn
        HaloHaptic.ToggleOff -> ToggleOff
        HaloHaptic.Threshold -> Threshold
        HaloHaptic.Confirm -> Confirm
        HaloHaptic.Reject -> Reject
        HaloHaptic.Crescendo -> Crescendo
        HaloHaptic.Celebrate -> Celebrate
        HaloHaptic.Ping -> Ping
        // The Tide is played live from its trajectory (HaloHaptics.tide); as a plain token it is the launch.
        HaloHaptic.Tide -> TideLaunch
    }

    /** The Tide's release on its own (a deep, soft hit with the whoomp's low thud). */
    val TideLaunch = HaloPattern(
        events = listOf(tap(0, 0.55f, 0.2f)),
        sound = HaloSoundCue.Thud(pitchHz = 82.41f, repeats = 1, spacingMs = 0, gain = 0.25f),
        basic = SystemHaptic.Click,
    )
}
