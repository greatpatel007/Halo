package com.halo.core.designsystem.feedback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A sound the mixer renders block by block (stereo, interleaved). Returns false once it has finished. */
internal fun interface MixVoice {
    fun mix(block: FloatArray, frames: Int): Boolean
}

/**
 * The Tide's water, synthesised live (research 0002). It drives itself from the wave's [TideTrajectory], so it slows
 * exactly as the front does; a reversal [retarget]s it. A stereo scene, each layer a physical source:
 * - **whoomp**: the body of water released (a low sine with a pitch drop and a noise burst), centred;
 * - **body**: the mass of moving water, broadband noise low-passed by speed (faster = brighter), centred;
 * - **splash**: the crisp top of the surf, high-passed noise that gusts with turbulence, independent left and right
 *   (decorrelated, so it surrounds you instead of sitting in one point);
 * - **sand**: very short crackles, more of them when faster, each on its own side;
 * - **foam**: bubbles collapsing, as rising sine chirps (Minnaert resonance) of varied size and position.
 * Direction colours it: to dark is deeper, to light brighter. The whole scene leans towards the front's position.
 */
internal class TideVoice(private val toDark: Boolean, trajectory: TideTrajectory, seed: Int = 1) : MixVoice {

    @Volatile private var trajectory = trajectory
    @Volatile private var restartAt = 0L
    @Volatile var pan = AUTO_PAN
    @Volatile private var released = false

    private var sample = 0L
    private var speed = 0f
    private var currentPan = 0.5f
    private val tone = if (toDark) 0.8f else 1.2f
    private var rng = seed or 1

    private val bodyL = OnePole()
    private val bodyR = OnePole()
    private val bodyHpL = OnePole()
    private val bodyHpR = OnePole()
    private val splashL = HighPass()
    private val splashR = HighPass()
    private var gust = 0f
    private var gustTarget = 0f
    private val sandL = Crackle()
    private val sandR = Crackle()
    private val bubbleFreq = FloatArray(BUBBLES)
    private val bubbleLife = IntArray(BUBBLES) { 1 }
    private val bubbleAge = IntArray(BUBBLES) { Int.MAX_VALUE }
    private val bubblePhase = FloatArray(BUBBLES)
    private val bubbleSide = FloatArray(BUBBLES)
    private val bubbleLevel = FloatArray(BUBBLES)
    private var whoompPhase = 0f

    /** A reversal: follow the new trajectory from now, without a new launch. */
    fun retarget(next: TideTrajectory) {
        trajectory = next
        restartAt = sample
    }

    /** The wave is over: fade out with the speed and stop. */
    fun release() {
        released = true
    }

    override fun mix(block: FloatArray, frames: Int): Boolean {
        val t = (sample - restartAt) / SR
        val target = if (released) 0f else trajectory.speed(t)
        val fromSpeed = speed
        val toSpeed = speed + (target - speed) * SMOOTH
        val panTarget = if (pan == AUTO_PAN) 0.6f - 1.2f * min(sample / SR / max(trajectory.durationS, 0.1f), 1f) else pan
        val fromPan = currentPan
        currentPan += (panTarget - currentPan) * SMOOTH
        // Turbulence: the splash gusts, a new random level every block (~4 ms), smoothed.
        gustTarget = 0.55f + 0.9f * nextUnit()
        for (f in 0 until frames) {
            val k = f / frames.toFloat()
            val s = fromSpeed + (toSpeed - fromSpeed) * k
            val p = fromPan + (currentPan - fromPan) * k
            gust += (gustTarget - gust) * 0.02f
            val n = noise()

            // Body: low-passed noise, brighter when faster, with the lowest rumble trimmed (it only muddies). Mostly
            // shared between the sides, partly each its own: water arrives from many points, not one.
            val bodyCut = (350f + 1400f * s) * tone
            val nl = n * 0.6f + noise() * 0.4f
            val nr = n * 0.6f + noise() * 0.4f
            val bl = bodyL.low(nl, bodyCut) - bodyHpL.low(bodyL.y, 120f)
            val br = bodyR.low(nr, bodyCut) - bodyHpR.low(bodyR.y, 120f)
            val bodyLevel = s.pow(0.9f) * 0.9f

            // Splash: high-passed noise, independent per side, level and brightness rising with speed.
            val splashCut = (3000f + 2500f * s) * tone
            val splashLevel = s.pow(1.3f) * gust * 0.7f
            val sl = splashL.high(noise(), splashCut) * splashLevel
            val sr = splashR.high(noise(), splashCut) * splashLevel

            // Sand: crisp crackles, rate following speed, each side its own.
            val rate = SAND_RATE * s / SR
            if (nextUnit() < rate) sandL.trigger(0.5f + 0.5f * nextUnit())
            if (nextUnit() < rate) sandR.trigger(0.5f + 0.5f * nextUnit())
            val cl = sandL.next(noise()) * 0.35f
            val cr = sandR.next(noise()) * 0.35f

            // Foam: bubbles of varied size, only while the water is fast; small ones ring higher and shorter.
            if (s > 0.2f && nextUnit() < BUBBLE_RATE * (s - 0.2f) / 0.8f / SR) spawnBubble()
            var fl = 0f
            var fr = 0f
            for (i in 0 until BUBBLES) {
                val age = bubbleAge[i]
                if (age > bubbleLife[i]) continue
                val life = age / bubbleLife[i].toFloat()
                // Minnaert: a collapsing bubble's pitch rises as it shrinks.
                bubblePhase[i] += TAU * bubbleFreq[i] * (1f + 0.35f * life) / SR
                val v = sin(bubblePhase[i]) * exp(-life * 5f) * min(age / 24f, 1f) * bubbleLevel[i]
                fl += v * (1f - bubbleSide[i])
                fr += v * (1f + bubbleSide[i])
                bubbleAge[i] = age + 1
            }

            // Whoomp: the release, in the first ~120 ms of a launch.
            var whoomp = 0f
            if (restartAt == 0L && sample < WHOOMP_SAMPLES) {
                val ts = sample / SR
                val freq = 82f * sqrt(tone) * (1f + 0.6f * exp(-ts / 0.02f))
                whoompPhase += TAU * freq / SR
                val env = min(ts / 0.003f, 1f) * exp(-ts / 0.035f)
                whoomp = (sin(whoompPhase) * 0.8f + n * 0.25f * exp(-ts / 0.012f)) * env
            }

            // The scene leans towards the front (balance, not a hard pan, so it stays wide).
            val angle = (p + 1f) * QUARTER_PI
            val gl = cos(angle) * SQRT2
            val gr = sin(angle) * SQRT2
            block[f * 2] += (bl * bodyLevel + whoomp + sl + cl + fl * 0.3f) * gl * MASTER
            block[f * 2 + 1] += (br * bodyLevel + whoomp + sr + cr + fr * 0.3f) * gr * MASTER
            sample++
        }
        speed = toSpeed
        val bubblesAlive = (0 until BUBBLES).any { bubbleAge[it] <= bubbleLife[it] }
        // The water is over once its trajectory has died out, even if the front is still settling its last pixels.
        val overdue = t > trajectory.durationS + OVERDUE_S
        val faded = speed < 0.002f && !bubblesAlive && sample > WHOOMP_SAMPLES
        val done = (released || overdue) && faded
        return !done
    }

    private fun spawnBubble() {
        val slot = (0 until BUBBLES).maxByOrNull { bubbleAge[it] - bubbleLife[it] } ?: return
        val size = nextUnit()
        bubbleAge[slot] = 0
        bubbleFreq[slot] = (1500f + 4500f * size * size) * sqrt(tone)
        bubbleLife[slot] = (SR * (0.012f - 0.007f * size)).toInt()
        bubblePhase[slot] = 0f
        bubbleSide[slot] = nextUnit() * 1.6f - 0.8f
        bubbleLevel[slot] = 0.5f + 0.5f * nextUnit()
    }

    private fun nextUnit(): Float {
        rng = rng xor (rng shl 13); rng = rng xor (rng ushr 17); rng = rng xor (rng shl 5)
        return (rng and 0xFFFFFF) / 16777216f
    }

    private fun noise(): Float = nextUnit() * 2f - 1f

    /** One-pole low-pass. */
    private class OnePole {
        var y = 0f
        fun low(x: Float, cutoffHz: Float): Float {
            y += (1f - exp(-TAU * cutoffHz / SR)) * (x - y)
            return y
        }
    }

    /** Two cascaded one-pole high-passes (12 dB/oct): a clean, airy top. */
    private class HighPass {
        private val a = OnePole()
        private val b = OnePole()
        fun high(x: Float, cutoffHz: Float): Float {
            val h1 = x - a.low(x, cutoffHz)
            return h1 - b.low(h1, cutoffHz)
        }
    }

    /** A sand grain: a ~0.6 ms burst of high-passed noise. */
    private class Crackle {
        private var env = 0f
        private var prev = 0f
        private var hp = 0f
        fun trigger(level: Float) {
            env = level
        }
        fun next(n: Float): Float {
            hp = 0.6f * (hp + n - prev)
            prev = n
            val out = hp * env
            env *= 0.965f
            return out
        }
    }

    private companion object {
        const val SR = SoundSynth.SAMPLE_RATE.toFloat()
        const val TAU = (2.0 * PI).toFloat()
        const val QUARTER_PI = 0.7853982f
        const val SQRT2 = 1.4142135f
        /** Per-block smoothing of speed and pan (≈16 ms time constant at 4 ms blocks): no zipper noise. */
        const val SMOOTH = 0.25f
        const val SAND_RATE = 60f
        const val BUBBLE_RATE = 40f
        const val BUBBLES = 10
        const val WHOOMP_SAMPLES = 5760L
        const val MASTER = 0.2f
        const val OVERDUE_S = 0.3f
        /** Sentinel: no pan from the screen yet, so sweep right → left with the wave's progress. */
        const val AUTO_PAN = -99f
    }
}
