package com.halo.core.designsystem.feedback

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Clarity of the Tide's water (fast phase, 0.1–0.27 s): broadband with a crisp top, not one honking band, and wide,
 * not mono. Before the stereo rework: 64% in 1–3 kHz, 7% above 6 kHz, L/R correlation 1.00.
 */
class TideClarityTest {
    @Test
    fun waterIsBroadbandCrispAndWide() {
        val voice = TideVoice(false, TideTrajectory.launch(sqrt(7f), 3000f, 0.5f))
        val block = FloatArray(384)
        val left = FloatArray(48000)
        val right = FloatArray(48000)
        var i = 0
        while (i < 48000) { block.fill(0f); voice.mix(block, 192); for (f in 0 until 192) { if (i + f < 48000) { left[i + f] = block[f * 2]; right[i + f] = block[f * 2 + 1] } }; i += 192 }
        val bands = listOf(50 to 250, 250 to 1000, 1000 to 3000, 3000 to 6000, 6000 to 12000)
        // Window 0.1–0.4 s (fast water): single-bin DFT sums over band.
        val start = 4800; val n = 8192
        val e = DoubleArray(bands.size)
        for ((b, band) in bands.withIndex()) {
            var f = band.first
            while (f < band.second) {
                var re = 0.0; var im = 0.0
                for (k in 0 until n) { val w = 0.5 - 0.5 * cos(2 * PI * k / n); val x = left[start + k] * w; re += x * cos(2 * PI * f * k / 48000); im -= x * sin(2 * PI * f * k / 48000) }
                e[b] += re * re + im * im
                f += 50
            }
        }
        val total = e.sum()
        var corr = 0.0; var ll = 0.0; var rr = 0.0
        for (k in start until start + n) { corr += left[k] * right[k]; ll += left[k] * left[k]; rr += right[k] * right[k] }
        val share = e.map { it / total }
        assertTrue("1–3 kHz dominates: ${share[2]}", share[2] < 0.45)
        assertTrue("too little crisp top: ${share[3] + share[4]}", share[3] + share[4] > 0.35)
        assertTrue("too narrow: ${corr / sqrt(ll * rr)}", corr / sqrt(ll * rr) < 0.6)
    }
}
