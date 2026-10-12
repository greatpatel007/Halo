package com.halo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Material's tone, made concrete: a tone is CIELAB L* (0 black … 100 white), exactly HCT's tone, so a role Material
 * defines as "tone 40" lands on the same lightness here. Hue and chroma are **OKLCH** (Halo's colour space): unlike
 * CIELAB, OKLab keeps blues blue as they lighten (CIELAB drifts them towards purple), which is what Material's CAM16
 * hue achieves too. [at] solves for the OKLab lightness that lands on the requested tone. Proposal 0006.
 */
internal object Tone {

    /** [l] = tone (CIELAB L*), [c] = OKLCH chroma, [h] = OKLCH hue (radians). */
    data class Lch(val l: Float, val c: Float, val h: Float)

    fun of(color: Color): Lch {
        val ok = Oklch.of(color)
        return Lch(lstar(color), ok.c, ok.h)
    }

    /** The colour at [tone] with OKLCH [hue] and up to [chroma] (chroma shrinks to fit sRGB; tone and hue are kept). */
    fun at(hue: Float, chroma: Float, tone: Float): Color {
        val t = tone.coerceIn(0f, 100f)
        if (t <= 0f) return Color(0f, 0f, 0f)
        if (t >= 100f) return Color(1f, 1f, 1f)
        var lo = 0f
        var hi = 1f
        repeat(SOLVE_STEPS) {
            val mid = (lo + hi) / 2f
            if (lstar(Oklch(mid, chroma, hue).toColor()) < t) lo = mid else hi = mid
        }
        return Oklch((lo + hi) / 2f, chroma, hue).toColor()
    }

    /** [color] moved to [tone], keeping its hue and as much chroma as fits. */
    fun shift(color: Color, tone: Float): Color = of(color).let { at(it.h, it.c, tone) }

    /** CIELAB L* of an sRGB colour (D65). */
    fun lstar(color: Color): Float {
        val y = 0.2126729f * decode(color.red) + 0.7151522f * decode(color.green) + 0.0721750f * decode(color.blue)
        val f = if (y > EPSILON) cbrt(y) else (KAPPA * y + 16f) / 116f
        return 116f * f - 16f
    }

    private fun decode(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)

    private const val EPSILON = 216f / 24389f
    private const val KAPPA = 24389f / 27f
    private const val SOLVE_STEPS = 22
}
