package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * A host app's identity: its palettes for both themes and its two type families. Everything else (spacing, motion,
 * light, materials, haptics, sound) is the design system's and stays the same in every app.
 *
 * Use [HaloBrand.Halo] for the Halo look, or [haloBrand] to derive a brand from one accent colour.
 */
@Immutable
class HaloBrand(
    val dark: HaloColors,
    val light: HaloColors,
    val fonts: HaloFonts = HaloFonts.Halo,
) {
    fun colors(isDark: Boolean): HaloColors = if (isDark) dark else light

    companion object {
        /** Halo's own brand: blue #2A67FF, Bitcoin orange, Lightning yellow, Satoshi and Paper Mono. */
        val Halo: HaloBrand by lazy { HaloBrand(haloDarkColors(), haloLightColors()) }
    }
}

/**
 * The two type families every style is built from: [sans] for UI and body text, [mono] for numbers, data and
 * labels. Sizes, weights, line heights and tracking stay the design system's.
 */
@Immutable
class HaloFonts(val sans: FontFamily, val mono: FontFamily) {
    companion object {
        val Halo: HaloFonts by lazy { HaloFonts(sans = Satoshi, mono = PaperMono) }
    }
}

/**
 * A brand from seed colours. Each seed is the colour of a filled control in the dark theme; the other roles (pressed,
 * text, tint, glow, the light theme's deeper stops) are derived in OKLCH, keeping the seed's hue and checking contrast:
 * accent text reaches 4.5:1 on the canvas and the label on a fill reaches 4.5:1 too, in both themes. Neutrals, lines
 * and status colours stay Halo's.
 *
 * @param accent every interactive UI accent (Halo: blue #2A67FF).
 * @param bitcoin the first signal family (Halo: on-chain Bitcoin). Null keeps Halo's.
 * @param lightning the second signal family (Halo: Lightning). Null keeps Halo's.
 */
fun haloBrand(
    accent: Color,
    bitcoin: Color? = null,
    lightning: Color? = null,
    fonts: HaloFonts = HaloFonts.Halo,
): HaloBrand {
    fun HaloColors.rebrand(): HaloColors {
        val a = haloAccent(accent, this)
        return copy(
            accent = a,
            bitcoin = bitcoin?.let { haloAccent(it, this) } ?: this.bitcoin,
            lightning = lightning?.let { haloAccent(it, this) } ?: this.lightning,
            focus = if (isDark) a.fill else a.text,
        )
    }
    return HaloBrand(haloDarkColors().rebrand(), haloLightColors().rebrand(), fonts)
}

/**
 * One accent family from a [seed], for the theme of [base] (its canvas and ink decide the contrast targets).
 * Public so an app can add families of its own with the same rules.
 */
fun haloAccent(seed: Color, base: HaloColors): HaloAccentColors {
    val s = Oklch.of(seed)
    val canvas = base.bg.canvas
    val inks = listOf(base.content.inverse, base.content.primary)
    return if (base.isDark) {
        val fill = seed
        HaloAccentColors(
            fill = fill,
            fillPressed = s.withL(s.l - PRESSED_STEP).toColor(),
            onFill = inks.maxBy { contrast(it, fill) },
            text = s.reach(canvas, TEXT_CONTRAST, lighter = true),
            tint = Oklch(DARK_TINT_L, min(s.c, TINT_CHROMA), s.h).toColor(),
            glow = fill.copy(alpha = DARK_GLOW),
        )
    } else {
        // On ceramic a fill reads a touch deeper than on black, as Halo's own light ramps do.
        val fill = s.withL(s.l - LIGHT_FILL_STEP).toColor()
        HaloAccentColors(
            fill = fill,
            fillPressed = s.withL(s.l - LIGHT_FILL_STEP - PRESSED_STEP).toColor(),
            onFill = inks.maxBy { contrast(it, fill) },
            text = s.reach(canvas, TEXT_CONTRAST, lighter = false),
            tint = Oklch(LIGHT_TINT_L, min(s.c, TINT_CHROMA), s.h).toColor(),
            glow = fill.copy(alpha = LIGHT_GLOW),
        )
    }
}

/** WCAG 2 contrast ratio of two opaque colours (1 … 21). */
fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

private const val TEXT_CONTRAST = 4.6f
private const val PRESSED_STEP = 0.06f
private const val LIGHT_FILL_STEP = 0.03f
private const val DARK_TINT_L = 0.24f
private const val LIGHT_TINT_L = 0.95f
private const val TINT_CHROMA = 0.06f
private const val DARK_GLOW = 0.24f
private const val LIGHT_GLOW = 0.30f

/** OKLCH (Björn Ottosson's OKLab in polar form): lightness 0…1, chroma, hue in radians. */
internal data class Oklch(val l: Float, val c: Float, val h: Float) {

    fun withL(value: Float) = copy(l = value.coerceIn(0f, 1f))

    /** The closest sRGB colour: chroma is reduced (hue and lightness kept) until the colour fits the gamut. */
    fun toColor(): Color {
        var lo = 0f
        var hi = c
        if (inGamut(c)) return rgb(c)
        repeat(GAMUT_STEPS) {
            val mid = (lo + hi) / 2f
            if (inGamut(mid)) lo = mid else hi = mid
        }
        return rgb(lo)
    }

    /**
     * The seed's hue at the lightness nearest its own that reaches [ratio] against [background], moving [lighter]
     * or darker. Falls back to the extreme if the hue can't reach it.
     */
    fun reach(background: Color, ratio: Float, lighter: Boolean): Color {
        if (contrast(toColor(), background) >= ratio) return toColor()
        var lo = l
        var hi = if (lighter) 1f else 0f
        repeat(GAMUT_STEPS) {
            val mid = (lo + hi) / 2f
            if (contrast(withL(mid).toColor(), background) >= ratio) hi = mid else lo = mid
        }
        return withL(hi).toColor()
    }

    private fun lab(chroma: Float) = floatArrayOf(l, chroma * cos(h), chroma * sin(h))

    private fun inGamut(chroma: Float): Boolean = linear(lab(chroma)).all { it in -GAMUT_EPS..1f + GAMUT_EPS }

    private fun rgb(chroma: Float): Color {
        val (r, g, b) = linear(lab(chroma)).map { encode(it.coerceIn(0f, 1f)) }
        return Color(r, g, b)
    }

    companion object {
        fun of(color: Color): Oklch {
            val r = decode(color.red)
            val g = decode(color.green)
            val b = decode(color.blue)
            val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
            val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
            val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
            val okL = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
            val okA = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
            val okB = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
            return Oklch(okL, hypot(okA, okB), atan2(okB, okA))
        }

        private fun linear(lab: FloatArray): List<Float> {
            val (okL, okA, okB) = lab.toList()
            val l = (okL + 0.3963377774f * okA + 0.2158037573f * okB).pow(3)
            val m = (okL - 0.1055613458f * okA - 0.0638541728f * okB).pow(3)
            val s = (okL - 0.0894841775f * okA - 1.2914855480f * okB).pow(3)
            return listOf(
                4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
                -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
                -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
            )
        }

        private fun decode(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
        private fun encode(v: Float) = if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f

        private const val GAMUT_STEPS = 20
        private const val GAMUT_EPS = 1e-4f
    }
}
