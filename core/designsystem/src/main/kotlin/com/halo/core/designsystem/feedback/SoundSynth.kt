package com.halo.core.designsystem.feedback

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Halo's UI sounds are synthesised, not sampled: no audio files, a tiny APK, and every sound can follow a value
 * (a charge's pitch rises with its progress). They are one family, tuned to E-major pentatonic so sounds that overlap
 * never clash, short (most under 120 ms) and quiet, like Apple's UI sounds. Pure Kotlin, so it is unit-tested.
 */

/** A sound in the family. Gains are linear peak levels (0.2 ≈ −14 dBFS). */
@Immutable
sealed interface HaloSoundCue {
    /** A soft click: a tuned body plus a brief airy transient. [brightness] mixes in the transient. */
    data class Click(val pitchHz: Float, val brightness: Float, val decayMs: Int, val gain: Float) : HaloSoundCue

    /** Struck glass: inharmonic glass partials per note, notes [spacingMs] apart (a tink, a chime, an arpeggio). */
    data class Glass(val notesHz: List<Float>, val spacingMs: Int, val decayMs: Int, val gain: Float) : HaloSoundCue

    /** A low, rounded thud with a slight pitch drop, [repeats] times. */
    data class Thud(val pitchHz: Float, val repeats: Int, val spacingMs: Int, val gain: Float) : HaloSoundCue

    /** Air rising in brightness: filtered noise for a charge or swell. */
    data class Breath(val durationMs: Int, val gain: Float) : HaloSoundCue
}

/** E-major pentatonic, E5 upwards (Hz). */
internal object Pentatonic {
    val notes = floatArrayOf(659.26f, 739.99f, 830.61f, 987.77f, 1108.73f, 1318.51f, 1479.98f, 1661.22f)
    fun at(position: Float): Float = notes[(position.coerceIn(0f, 1f) * (notes.size - 1)).toInt()]
}

internal object SoundSynth {
    const val SAMPLE_RATE = 48_000

    /** Renders [cue] to mono float PCM at [SAMPLE_RATE], peak-normalised to its gain, with clean edges. */
    fun render(cue: HaloSoundCue): FloatArray {
        val pcm = when (cue) {
            is HaloSoundCue.Click -> click(cue)
            is HaloSoundCue.Glass -> glass(cue)
            is HaloSoundCue.Thud -> thud(cue)
            is HaloSoundCue.Breath -> breath(cue)
        }
        val gain = when (cue) {
            is HaloSoundCue.Click -> cue.gain
            is HaloSoundCue.Glass -> cue.gain
            is HaloSoundCue.Thud -> cue.gain
            is HaloSoundCue.Breath -> cue.gain
        }
        normalise(pcm, gain)
        fadeEdges(pcm)
        return pcm
    }

    private fun click(c: HaloSoundCue.Click): FloatArray {
        val out = FloatArray(samples(c.decayMs * 4))
        val tau = c.decayMs / 1000f / 3f
        val noise = Noise(7)
        var hp = 0f
        var prev = 0f
        for (i in out.indices) {
            val t = i / SAMPLE_RATE.toFloat()
            val body = sin(TAU * c.pitchHz * t) * exp(-t / tau)
            // Airy transient: high-passed noise over ~1.5 ms.
            val n = noise.next()
            hp = 0.85f * (hp + n - prev)
            prev = n
            val air = hp * exp(-t / AIR_TAU_S)
            out[i] = body * (1f - c.brightness * 0.5f) + air * c.brightness
        }
        return out
    }

    private fun glass(c: HaloSoundCue.Glass): FloatArray {
        val out = FloatArray(samples(c.spacingMs * (c.notesHz.size - 1) + c.decayMs * 3))
        for ((n, hz) in c.notesHz.withIndex()) {
            val offset = samples(c.spacingMs * n)
            for (k in GLASS_RATIOS.indices) {
                val f = hz * GLASS_RATIOS[k]
                if (f > SAMPLE_RATE * 0.45f) continue
                // Higher partials die faster: what makes it read as glass rather than a bell.
                val tau = c.decayMs / 1000f / 3f / (1f + k * 1.5f)
                for (i in 0 until out.size - offset) {
                    val t = i / SAMPLE_RATE.toFloat()
                    out[offset + i] += GLASS_AMPS[k] * sin(TAU * f * t) * exp(-t / tau)
                }
            }
        }
        return out
    }

    private fun thud(c: HaloSoundCue.Thud): FloatArray {
        val out = FloatArray(samples(c.spacingMs * (c.repeats - 1) + THUD_MS * 3))
        for (r in 0 until c.repeats) {
            val offset = samples(c.spacingMs * r)
            val level = 1f - r * 0.3f
            var phase = 0f
            for (i in 0 until out.size - offset) {
                val t = i / SAMPLE_RATE.toFloat()
                val f = c.pitchHz * (1f + 0.5f * exp(-t / 0.01f))
                phase += TAU * f / SAMPLE_RATE
                out[offset + i] += level * sin(phase) * exp(-t / (THUD_MS / 1000f / 3f))
            }
        }
        return out
    }

    private fun breath(c: HaloSoundCue.Breath): FloatArray {
        val out = FloatArray(samples(c.durationMs))
        val noise = Noise(11)
        var lp = 0f
        for (i in out.indices) {
            val p = i / out.size.toFloat()
            // A one-pole low-pass whose cutoff rises from ~300 Hz to ~3 kHz: the air brightens as it swells.
            val cutoff = 300f + 2700f * p * p
            val a = 1f - exp(-TAU * cutoff / SAMPLE_RATE)
            lp += a * (noise.next() - lp)
            val env = sin(PI.toFloat() * min(p / 0.85f, 1f) * 0.5f) * if (p > 0.85f) (1f - (p - 0.85f) / 0.15f) else 1f
            out[i] = lp * env
        }
        return out
    }

    private fun normalise(pcm: FloatArray, gain: Float) {
        var peak = 0f
        for (v in pcm) peak = max(peak, abs(v))
        if (peak <= 0f) return
        val k = gain / peak
        for (i in pcm.indices) pcm[i] *= k
    }

    /** 0.5 ms fade-in and 3 ms fade-out: no clicks at the buffer edges. */
    private fun fadeEdges(pcm: FloatArray) {
        val fadeIn = min(samples(0.5f), pcm.size)
        for (i in 0 until fadeIn) pcm[i] *= i / fadeIn.toFloat()
        val fadeOut = min(samples(3f), pcm.size)
        for (i in 0 until fadeOut) pcm[pcm.size - 1 - i] *= i / fadeOut.toFloat()
    }

    private fun samples(ms: Int) = ms * SAMPLE_RATE / 1000
    private fun samples(ms: Float) = (ms * SAMPLE_RATE / 1000).toInt()

    /** Deterministic noise (xorshift), so every render of a sound is identical. */
    private class Noise(seed: Int) {
        private var s = seed or 1
        fun next(): Float {
            s = s xor (s shl 13); s = s xor (s ushr 17); s = s xor (s shl 5)
            return (s and 0xFFFF) / 32768f - 1f
        }
    }

    private const val TAU = (2.0 * PI).toFloat()
    private const val AIR_TAU_S = 0.0015f
    private const val THUD_MS = 60
    /** Partials of a struck glass (free bar modes) and their levels. */
    private val GLASS_RATIOS = floatArrayOf(1f, 2.756f, 5.404f, 8.933f)
    private val GLASS_AMPS = floatArrayOf(1f, 0.5f, 0.25f, 0.12f)
}
