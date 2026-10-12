package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The user's contrast preference (Android 14+ "Contrast level", or a host override). Material ships a scheme per
 * level; [Medium] and [High] push text and outlines further from their backgrounds and make containers bolder.
 */
enum class HaloContrast { Standard, Medium, High }

/**
 * A Material colour family: [color] (text, icons, a filled control's ink-free fill), [onColor] on it, [container]
 * and [onContainer]. [fill] / [onFill] are what a *lit control* takes (a switch track, a filled button): Material's
 * colour role for UI families; for Halo's meaning families (on-chain, Lightning) the true seed, so orange and yellow
 * still read as themselves (Material's tone 40 turns yellow olive).
 */
@Immutable
class HaloColorFamily(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
    val fill: Color = color,
    val onFill: Color = onColor,
)

/** The colours a scheme is generated from. Halo's own: brand blue, Bitcoin orange, Lightning yellow, status hues. */
@Immutable
class HaloSeeds(
    /** The brand: the key action keeps it exactly (Material's "fidelity"); primary roles take its hue and chroma. */
    val brand: Color,
    val bitcoin: Color,
    val lightning: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
) {
    companion object {
        val Halo: HaloSeeds = with(HaloPalette) {
            HaloSeeds(brand = Blue500, bitcoin = Btc500, lightning = Yellow500, positive = Green500, negative = Red500, warning = Coral500)
        }
    }
}

/**
 * Material 3 colour roles (androidx `ColorLight/DarkTokens`, at Material's exact tones) plus Halo's extensions.
 * Generate with [haloScheme]. Proposal 0006.
 */
@Immutable
class HaloScheme(
    val isDark: Boolean,
    val contrast: HaloContrast,
    /** UI actions. [HaloColorFamily.container] is the brand seed itself at standard contrast: the key action. */
    val primary: HaloColorFamily,
    /** Quiet UI: selected segments, tonal buttons, navigation indicators. */
    val secondary: HaloColorFamily,
    val tertiary: HaloColorFamily,
    val error: HaloColorFamily,
    /** On-chain Bitcoin only. */
    val bitcoin: HaloColorFamily,
    /** Lightning only. */
    val lightning: HaloColorFamily,
    /** Incoming / success. */
    val positive: HaloColorFamily,
    /** Cautions (coral, never yellow). */
    val warning: HaloColorFamily,
    val surface: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val scrim: Color,
)

/** Tones of a family's four roles at one contrast level, light theme then dark. */
private class FamilyTones(val color: Float, val on: Float, val container: Float, val onContainer: Float)

private fun familyTones(dark: Boolean, contrast: HaloContrast): FamilyTones = when (contrast) {
    HaloContrast.Standard -> if (dark) FamilyTones(80f, 20f, 30f, 90f) else FamilyTones(40f, 100f, 90f, 10f)
    HaloContrast.Medium -> if (dark) FamilyTones(85f, 10f, 40f, 100f) else FamilyTones(30f, 100f, 70f, 0f)
    HaloContrast.High -> if (dark) FamilyTones(92f, 0f, 85f, 0f) else FamilyTones(22f, 100f, 30f, 100f)
}

/** Neutral roles' tones: onSurfaceVariant, outline, outlineVariant per contrast level (surfaces stay Material's). */
private class NeutralTones(val onVariant: Float, val outline: Float, val outlineVariant: Float)

private fun neutralTones(dark: Boolean, contrast: HaloContrast): NeutralTones = when (contrast) {
    HaloContrast.Standard -> if (dark) NeutralTones(80f, 60f, 30f) else NeutralTones(30f, 50f, 80f)
    HaloContrast.Medium -> if (dark) NeutralTones(88f, 72f, 50f) else NeutralTones(20f, 38f, 60f)
    HaloContrast.High -> if (dark) NeutralTones(96f, 88f, 72f) else NeutralTones(8f, 22f, 36f)
}

private fun family(hue: Float, chroma: Float, t: FamilyTones) = HaloColorFamily(
    color = Tone.at(hue, chroma, t.color),
    onColor = Tone.at(hue, chroma, t.on),
    container = Tone.at(hue, chroma, t.container),
    onContainer = Tone.at(hue, chroma, t.onContainer),
)

/** The best of black-ish or white-ish ink on [fill], in [fill]'s hue. */
private fun inkOn(fill: Color): Color {
    val s = Tone.of(fill)
    val dark = Tone.at(s.h, minOf(s.c, INK_DARK_CHROMA), INK_DARK)
    val light = Tone.at(s.h, s.c * INK_LIGHT_CHROMA, INK_LIGHT)
    return if (contrast(dark, fill) >= contrast(light, fill)) dark else light
}

/**
 * The full scheme for one theme and contrast level, from [seeds].
 * - Neutrals take the brand hue at Material's neutral chroma (a faint brand cast, like Halo's inks).
 * - Primary takes the brand's hue and chroma; at standard contrast its container *is* the brand seed (fidelity).
 * - Meaning families (Bitcoin, Lightning) keep their seed as the lit-control [HaloColorFamily.fill].
 */
fun haloScheme(dark: Boolean, contrast: HaloContrast = HaloContrast.Standard, seeds: HaloSeeds = HaloSeeds.Halo): HaloScheme {
    val t = familyTones(dark, contrast)
    val n = neutralTones(dark, contrast)
    val p = Tone.of(seeds.brand)
    fun neutral(tone: Float) = Tone.at(p.h, NEUTRAL_CHROMA, tone)
    fun variant(tone: Float) = Tone.at(p.h, VARIANT_CHROMA, tone)
    fun ext(seed: Color, chroma: Float = Tone.of(seed).c) = Tone.of(seed).let { family(it.h, chroma, t) }

    val primaryTones = family(p.h, p.c, t)
    val primary = if (contrast == HaloContrast.Standard) {
        HaloColorFamily(primaryTones.color, primaryTones.onColor, seeds.brand, inkOn(seeds.brand), fill = seeds.brand, onFill = inkOn(seeds.brand))
    } else {
        primaryTones
    }
    fun meaning(seed: Color) = ext(seed).let {
        HaloColorFamily(it.color, it.onColor, it.container, it.onContainer, fill = seed, onFill = inkOn(seed))
    }
    return HaloScheme(
        isDark = dark,
        contrast = contrast,
        primary = primary,
        secondary = family(p.h, SECONDARY_CHROMA, t),
        tertiary = family(p.h + TERTIARY_SHIFT, TERTIARY_CHROMA, t),
        error = ext(seeds.negative, ERROR_CHROMA),
        bitcoin = meaning(seeds.bitcoin),
        lightning = meaning(seeds.lightning),
        positive = ext(seeds.positive),
        warning = ext(seeds.warning),
        surface = neutral(if (dark) 6f else 98f),
        surfaceDim = neutral(if (dark) 6f else 87f),
        surfaceBright = neutral(if (dark) 24f else 98f),
        surfaceContainerLowest = neutral(if (dark) 4f else 100f),
        surfaceContainerLow = neutral(if (dark) 10f else 96f),
        surfaceContainer = neutral(if (dark) 12f else 94f),
        surfaceContainerHigh = neutral(if (dark) 17f else 92f),
        surfaceContainerHighest = neutral(if (dark) 22f else 90f),
        onSurface = neutral(if (dark) 90f else 10f).let { if (contrast == HaloContrast.Standard) it else neutral(if (dark) 100f else 0f) },
        onSurfaceVariant = variant(n.onVariant),
        outline = variant(n.outline),
        outlineVariant = variant(n.outlineVariant),
        inverseSurface = neutral(if (dark) 90f else 20f),
        inverseOnSurface = neutral(if (dark) 20f else 95f),
        inversePrimary = Tone.at(p.h, p.c, if (dark) 40f else 80f),
        scrim = HaloPalette.Black,
    )
}

// OKLCH chroma (Material's CAM16 chroma 4 / 8 / 16 / 24 / 60 ≈ OKLCH 0.012 / 0.024 / 0.05 / 0.075 / 0.2).
private const val NEUTRAL_CHROMA = 0.012f
private const val VARIANT_CHROMA = 0.024f
private const val SECONDARY_CHROMA = 0.05f
private const val TERTIARY_CHROMA = 0.075f
private const val TERTIARY_SHIFT = 1.0472f // 60°
private const val ERROR_CHROMA = 0.2f
private const val INK_DARK = 2f
private const val INK_DARK_CHROMA = 0.02f
private const val INK_LIGHT = 100f
private const val INK_LIGHT_CHROMA = 0f
