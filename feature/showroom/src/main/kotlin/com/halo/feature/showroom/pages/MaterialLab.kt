package com.halo.feature.showroom.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.halo.core.designsystem.theme.HaloTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/*
 * Material 3 (Expressive) colour roles and type scale, lab version (2026-10-13), derived from Halo's seeds.
 * Tones are exactly Material's: a tone is CIELAB L* (HCT's tone), taken from the androidx material3 token sources
 * (ColorLightTokens / ColorDarkTokens, TypeScaleTokens). Hue and chroma come from Halo's own colours.
 * Halo extensions on top of Material: the brand blue is kept exactly as the primary *container* (Material's
 * "fidelity" scheme), on-chain / Lightning / positive are extended colour families, and numbers stay in Paper Mono.
 */

// ------------------------------------------------------------------------------------------- tones (CIELAB)

private data class Lch(val l: Float, val c: Float, val h: Float)

private fun lchOf(color: Color): Lch {
    fun lin(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    val r = lin(color.red); val g = lin(color.green); val b = lin(color.blue)
    val x = (0.4124f * r + 0.3576f * g + 0.1805f * b) / 0.95047f
    val y = 0.2126f * r + 0.7152f * g + 0.0722f * b
    val z = (0.0193f * r + 0.1192f * g + 0.9505f * b) / 1.08883f
    fun f(t: Float) = if (t > 0.008856f) cbrt(t) else 7.787f * t + 16f / 116f
    val l = 116f * f(y) - 16f
    val a = 500f * (f(x) - f(y))
    val bb = 200f * (f(y) - f(z))
    return Lch(l, hypot(a, bb), atan2(bb, a))
}

/** The colour at Material tone [tone] (L*) with [hue] and up to [chroma]; chroma shrinks until it fits sRGB. */
private fun tone(hue: Float, chroma: Float, tone: Float): Color {
    fun rgb(c: Float): FloatArray? {
        val l = tone.coerceIn(0f, 100f)
        val a = c * cos(hue); val b = c * sin(hue)
        val fy = (l + 16f) / 116f; val fx = fy + a / 500f; val fz = fy - b / 200f
        fun inv(t: Float) = if (t.pow(3) > 0.008856f) t.pow(3) else (t - 16f / 116f) / 7.787f
        val x = inv(fx) * 0.95047f; val y = inv(fy); val z = inv(fz) * 1.08883f
        val lin = floatArrayOf(
            3.2406f * x - 1.5372f * y - 0.4986f * z,
            -0.9689f * x + 1.8758f * y + 0.0415f * z,
            0.0557f * x - 0.2040f * y + 1.0570f * z,
        )
        if (lin.any { it < -1e-3f || it > 1.001f }) return null
        return FloatArray(3) { i ->
            val v = lin[i].coerceIn(0f, 1f)
            if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f
        }
    }
    var lo = 0f
    var hi = chroma
    rgb(hi)?.let { return Color(it[0], it[1], it[2]) }
    repeat(18) { val mid = (lo + hi) / 2f; if (rgb(mid) != null) lo = mid else hi = mid }
    val c = rgb(lo) ?: floatArrayOf(tone / 100f, tone / 100f, tone / 100f)
    return Color(c[0], c[1], c[2])
}

// ------------------------------------------------------------------------------------------- roles

/**
 * A Material colour family: the colour (text/icons), what sits on it, its container and what sits on that.
 * [fill] is the colour a lit control takes (a switch track): Material's colour role, except for Halo's network
 * families, which keep their true seed (fidelity) so on-chain still reads orange and Lightning still reads yellow.
 */
@Immutable
internal class M3Family(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
    val fill: Color = color,
    val onFill: Color = onColor,
)

@Immutable
internal class M3Scheme(
    val dark: Boolean,
    val primary: M3Family,
    val secondary: M3Family,
    val tertiary: M3Family,
    val error: M3Family,
    // Halo's extended families.
    val bitcoin: M3Family,
    val lightning: M3Family,
    val positive: M3Family,
    // Surfaces.
    val surface: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val containerLowest: Color,
    val containerLow: Color,
    val container: Color,
    val containerHigh: Color,
    val containerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val scrim: Color,
)

private fun family(hue: Float, chroma: Float, dark: Boolean) = if (dark) {
    M3Family(tone(hue, chroma, 80f), tone(hue, chroma, 20f), tone(hue, chroma, 30f), tone(hue, chroma, 90f))
} else {
    M3Family(tone(hue, chroma, 40f), tone(hue, chroma, 100f), tone(hue, chroma, 90f), tone(hue, chroma, 10f))
}

/** Builds the scheme from Halo's seeds (brand blue, bitcoin orange, Lightning yellow, positive, negative). */
internal fun m3Scheme(dark: Boolean, brand: Color, bitcoin: Color, lightning: Color, positive: Color, negative: Color): M3Scheme {
    val p = lchOf(brand)
    val neutral = { t: Float -> tone(p.h, NEUTRAL_CHROMA, t) }
    val variant = { t: Float -> tone(p.h, VARIANT_CHROMA, t) }
    val primaryTones = family(p.h, p.c, dark)
    // Fidelity: the brand blue itself is the primary container (the key action), its "on" colour by contrast.
    val primary = M3Family(primaryTones.color, primaryTones.onColor, brand, if (p.l < 60f) Color.White else tone(p.h, p.c, 10f))
    fun ext(seed: Color, chroma: Float = lchOf(seed).c) = lchOf(seed).let { family(it.h, chroma, dark) }
    fun network(seed: Color) = ext(seed).let {
        val s = lchOf(seed)
        M3Family(it.color, it.onColor, it.container, it.onContainer, fill = seed, onFill = if (s.l > 60f) tone(s.h, s.c, 10f) else Color.White)
    }
    return M3Scheme(
        dark = dark,
        primary = primary,
        secondary = family(p.h, SECONDARY_CHROMA, dark),
        tertiary = family(p.h + TERTIARY_SHIFT, TERTIARY_CHROMA, dark),
        error = ext(negative, ERROR_CHROMA),
        bitcoin = network(bitcoin),
        lightning = network(lightning),
        positive = ext(positive),
        surface = neutral(if (dark) 6f else 98f),
        surfaceDim = neutral(if (dark) 6f else 87f),
        surfaceBright = neutral(if (dark) 24f else 98f),
        containerLowest = neutral(if (dark) 4f else 100f),
        containerLow = neutral(if (dark) 10f else 96f),
        container = neutral(if (dark) 12f else 94f),
        containerHigh = neutral(if (dark) 17f else 92f),
        containerHighest = neutral(if (dark) 22f else 90f),
        onSurface = neutral(if (dark) 90f else 10f),
        onSurfaceVariant = variant(if (dark) 80f else 30f),
        outline = variant(if (dark) 60f else 50f),
        outlineVariant = variant(if (dark) 30f else 80f),
        inverseSurface = neutral(if (dark) 90f else 20f),
        inverseOnSurface = neutral(if (dark) 20f else 95f),
        inversePrimary = tone(p.h, p.c, if (dark) 40f else 80f),
        scrim = Color.Black,
    )
}

// ------------------------------------------------------------------------------------------- type

/** Material 3's 15 type roles (+ the Expressive emphasized variants), set in Satoshi. Numbers stay Paper Mono. */
@Immutable
internal class M3Type(
    val displayLarge: TextStyle, val displayMedium: TextStyle, val displaySmall: TextStyle,
    val headlineLarge: TextStyle, val headlineMedium: TextStyle, val headlineSmall: TextStyle,
    val titleLarge: TextStyle, val titleMedium: TextStyle, val titleSmall: TextStyle,
    val bodyLarge: TextStyle, val bodyMedium: TextStyle, val bodySmall: TextStyle,
    val labelLarge: TextStyle, val labelMedium: TextStyle, val labelSmall: TextStyle,
    val titleMediumEmphasized: TextStyle, val labelLargeEmphasized: TextStyle, val labelMediumEmphasized: TextStyle,
    val headlineSmallEmphasized: TextStyle,
)

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

private fun ts(family: FontFamily?, size: Float, line: Float, weight: FontWeight, tracking: Float) = TextStyle(
    fontFamily = family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp,
    lineHeightStyle = Trim,
)

internal fun m3Type(family: FontFamily?): M3Type {
    val r = FontWeight.Normal; val m = FontWeight.Medium; val b = FontWeight.Bold
    return M3Type(
        displayLarge = ts(family, 57f, 64f, r, -0.2f), displayMedium = ts(family, 45f, 52f, r, 0f), displaySmall = ts(family, 36f, 44f, r, 0f),
        headlineLarge = ts(family, 32f, 40f, r, 0f), headlineMedium = ts(family, 28f, 36f, r, 0f), headlineSmall = ts(family, 24f, 32f, r, 0f),
        titleLarge = ts(family, 22f, 28f, r, 0f), titleMedium = ts(family, 16f, 24f, m, 0.2f), titleSmall = ts(family, 14f, 20f, m, 0.1f),
        bodyLarge = ts(family, 16f, 24f, r, 0.5f), bodyMedium = ts(family, 14f, 20f, r, 0.2f), bodySmall = ts(family, 12f, 16f, r, 0.4f),
        labelLarge = ts(family, 14f, 20f, m, 0.1f), labelMedium = ts(family, 12f, 16f, m, 0.5f), labelSmall = ts(family, 11f, 16f, m, 0.5f),
        titleMediumEmphasized = ts(family, 16f, 24f, b, 0.15f),
        labelLargeEmphasized = ts(family, 14f, 20f, b, 0.1f),
        labelMediumEmphasized = ts(family, 12f, 16f, b, 0.5f),
        headlineSmallEmphasized = ts(family, 24f, 32f, m, 0f),
    )
}

internal val LocalM3 = staticCompositionLocalOf<M3Scheme?> { null }
internal val LocalM3Type = staticCompositionLocalOf<M3Type?> { null }

/** The lab scheme for the current Halo theme. */
@Composable
internal fun rememberM3(): M3Scheme {
    val c = HaloTheme.colors
    return remember(c.isDark, c.accent.fill, c.bitcoin.fill, c.lightning.fill) {
        m3Scheme(c.isDark, c.accent.fill, c.bitcoin.fill, c.lightning.fill, c.status.positive, c.status.negative)
    }
}

@Composable
internal fun rememberM3Type(): M3Type {
    val family = HaloTheme.typography.body.fontFamily
    return remember(family) { m3Type(family) }
}

internal val m3: M3Scheme @Composable get() = checkNotNull(LocalM3.current)
internal val m3type: M3Type @Composable get() = checkNotNull(LocalM3Type.current)

private const val NEUTRAL_CHROMA = 4f
private const val VARIANT_CHROMA = 8f
private const val SECONDARY_CHROMA = 16f
private const val TERTIARY_CHROMA = 24f
private const val TERTIARY_SHIFT = (PI / 3).toFloat()
private const val ERROR_CHROMA = 60f
