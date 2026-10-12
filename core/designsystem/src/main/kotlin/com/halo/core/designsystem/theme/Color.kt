package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Primitive ramps. Components never reference these directly — use [HaloColors] semantic roles.
 * The only file allowed to declare raw color literals is this one.
 */
object HaloPalette {
    // Ink — dark theme neutrals (soft-touch black), with a faint cast of the brand blue (OKLCH hue 264).
    val Ink950 = Color(0xFF090A0C)
    val Ink900 = Color(0xFF0E0F12)
    val Ink850 = Color(0xFF131417)
    val Ink800 = Color(0xFF181A1D)
    val Ink700 = Color(0xFF212327)
    val Ink600 = Color(0xFF2B2E32)
    val Ink500 = Color(0xFF45484D)
    val Ink400 = Color(0xFF6F737A)
    val Ink300 = Color(0xFF999DA5)
    val Ink200 = Color(0xFFCDCFD5)
    val Ink100 = Color(0xFFF3F4F7)

    // Ceramic — light theme neutrals (cool matte white, as in the toggle reference).
    val Ceramic000 = Color(0xFFFFFFFF)
    /** Raised ceramic parts: a hair under white so lit slopes keep headroom before clipping. */
    val Ceramic025 = Color(0xFFF9FAFC)
    val Ceramic050 = Color(0xFFF5F7FA)
    val Ceramic100 = Color(0xFFEBEEF2)
    val Ceramic150 = Color(0xFFE4E8EE)
    val Ceramic200 = Color(0xFFDCE1E8)
    val Ceramic300 = Color(0xFFC3CAD4)
    val Slate400 = Color(0xFFA7AFBB)
    val Slate500 = Color(0xFF737C8A)
    val Slate700 = Color(0xFF4A5261)
    val Slate950 = Color(0xFF0D1014)
    /** Cool shadow tone of the light theme (shadows on ceramic are blue-grey, never black). */
    val Slate900Shadow = Color(0xFF2B3A52)

    // Blue — the UI accent (brand primary #2A67FF). Text stops reach 4.5:1 on each theme's canvas.
    val Blue400 = Color(0xFF6E9BFF)
    val Blue500 = Color(0xFF2A67FF)
    val Blue600 = Color(0xFF1F55E0)
    val Blue700 = Color(0xFF1A4CD1)
    /** The brand navy: the accent's tint on soft-touch black. */
    val Blue900 = Color(0xFF0D1A3C)
    val Blue050 = Color(0xFFE3ECFF)

    // Colour hierarchy (OKLCH chroma): brand blue is the one loud colour (0.24). Network families sit below it
    // (~0.14) and status colours below those (0.10–0.15), so nothing out-shouts the brand.

    // Bitcoin — on-chain only.
    val Btc300 = Color(0xFFF9C696)
    val Btc400 = Color(0xFFF3AD6D)
    val Btc500 = Color(0xFFED9449)
    val Btc550 = Color(0xFFE28842)
    val Btc600 = Color(0xFFD77F3D)
    val Btc800 = Color(0xFFA2511A)
    val Btc900 = Color(0xFF321B0B)
    val Btc050 = Color(0xFFFEEBDB)

    // Lightning — yellow, Lightning only. The UI uses a softened yellow at the network families' chroma (~0.13, like
    // orange); the exact brand yellow is kept for brand moments, where it may be loud.
    val YellowBrand = Color(0xFFF5DE4B)
    val Yellow400 = Color(0xFFE5D587)
    val Yellow500 = Color(0xFFE6D06A)
    val Yellow550 = Color(0xFFD3BA59)
    val Yellow600 = Color(0xFFCDB352)
    val Yellow800 = Color(0xFF665800)
    val Yellow900 = Color(0xFF262107)
    val Yellow050 = Color(0xFFFFF8D6)

    // Status.
    val Green500 = Color(0xFF6ECF9A)
    val Green700 = Color(0xFF25764E)
    val Green900 = Color(0xFF0F271A)
    val Green050 = Color(0xFFDFF5E7)
    val Red500 = Color(0xFFED7473)
    val Red700 = Color(0xFFB63638)
    val Red900 = Color(0xFF361717)
    val Red050 = Color(0xFFFFE8E7)
    // Warning is coral, kept apart from Lightning's yellow and Bitcoin's orange.
    val Coral500 = Color(0xFFF2AA8C)
    val Coral700 = Color(0xFFA25235)
    val Coral900 = Color(0xFF321E16)
    val Coral050 = Color(0xFFFFECE3)

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
}

@Immutable
class HaloBackgroundColors(
    /** L0 — the app canvas. */
    val canvas: Color,
    /** L1 — cards, rows, inputs. */
    val surface: Color,
    /** L2 — raised elements: sheets, popovers, selected cards. */
    val raised: Color,
    /** L3 — glass tint laid over a backdrop blur. */
    val glass: Color,
    /** Dim layer behind modal surfaces. */
    val scrim: Color,
)

@Immutable
class HaloContentColors(
    val primary: Color,
    val secondary: Color,
    /** Non-essential text only (>= 3:1). */
    val tertiary: Color,
    val disabled: Color,
    val inverse: Color,
)

@Immutable
class HaloLineColors(
    val subtle: Color,
    val default: Color,
    val strong: Color,
    val focus: Color,
    /** Top-edge light catch used by depth L2+. */
    val highlight: Color,
)

/** An accent family: blue (UI), Bitcoin orange (on-chain) or Lightning yellow. */
@Immutable
class HaloAccentColors(
    val fill: Color,
    val fillPressed: Color,
    val onFill: Color,
    /** Accent used as text/icon on the theme's backgrounds. */
    val text: Color,
    /** Low-intensity accent background (chips, selected rows). */
    val tint: Color,
    /** Light emitted around active accent elements. */
    val glow: Color,
)

@Immutable
class HaloStatusColors(
    val positive: Color,
    val positiveTint: Color,
    val negative: Color,
    val negativeTint: Color,
    val warning: Color,
    val warningTint: Color,
)

/**
 * Halo's colour roles, now a **migration alias layer** over Material 3's roles (proposal 0006, phase 1): every role
 * below is derived from [scheme], which components should read directly as they migrate (`HaloTheme.scheme`).
 * Mapping: bg.canvas → surface · bg.surface → surfaceContainer · bg.raised → surfaceContainerHigh ·
 * content.primary/secondary/tertiary → onSurface / onSurfaceVariant / outline · line.subtle/default/strong →
 * outlineVariant (half) / outlineVariant / outline · line.focus → primary · accent.fill → primaryContainer (the brand) ·
 * accent.text → primary · networks: fill → family.fill (the seed), text → family.color, tint → family.container.
 */
@Immutable
class HaloColors(
    val bg: HaloBackgroundColors,
    val content: HaloContentColors,
    val line: HaloLineColors,
    /** Brand blue — every interactive UI accent. */
    val accent: HaloAccentColors,
    /** Bitcoin orange — on-chain Bitcoin only. */
    val bitcoin: HaloAccentColors,
    /** Yellow — Lightning only. */
    val lightning: HaloAccentColors,
    val status: HaloStatusColors,
    val isDark: Boolean,
    /** The Material 3 roles these colours are derived from. */
    val scheme: HaloScheme,
)

/** The alias layer for [scheme] (see [HaloColors]). */
fun HaloScheme.toHaloColors(): HaloColors {
    val dark = isDark
    fun meaning(f: HaloColorFamily, glow: Float) = HaloAccentColors(
        fill = f.fill,
        fillPressed = Tone.shift(f.fill, Tone.of(f.fill).l - PRESSED_TONE_STEP),
        onFill = f.onFill,
        text = f.color,
        tint = f.container,
        glow = f.fill.copy(alpha = glow),
    )
    val brand = primary.fill
    val brandTone = Tone.of(brand)
    return HaloColors(
        bg = HaloBackgroundColors(
            canvas = surface,
            surface = surfaceContainer,
            raised = surfaceContainerHigh,
            glass = (if (dark) surfaceContainer else surfaceContainerLowest).copy(alpha = if (dark) GLASS_DARK else GLASS_LIGHT),
            scrim = scrim.copy(alpha = if (dark) SCRIM_DARK else SCRIM_LIGHT),
        ),
        content = HaloContentColors(
            primary = onSurface,
            secondary = onSurfaceVariant,
            tertiary = outline,
            disabled = onSurface.copy(alpha = DISABLED_ALPHA),
            inverse = inverseOnSurface,
        ),
        line = HaloLineColors(
            subtle = outlineVariant.copy(alpha = SUBTLE_LINE),
            default = outlineVariant,
            strong = outline,
            focus = primary.color,
            highlight = HaloPalette.White.copy(alpha = if (dark) HIGHLIGHT_DARK else HIGHLIGHT_LIGHT),
        ),
        accent = HaloAccentColors(
            fill = brand,
            fillPressed = Tone.at(brandTone.h, brandTone.c, brandTone.l - PRESSED_TONE_STEP),
            onFill = primary.onFill,
            text = primary.color,
            // A quiet selected background in the brand's hue (Material's tonal primary container).
            tint = Tone.at(brandTone.h, minOf(brandTone.c, TINT_CHROMA), if (dark) TINT_TONE_DARK else TINT_TONE_LIGHT),
            glow = brand.copy(alpha = if (dark) BRAND_GLOW_DARK else BRAND_GLOW_LIGHT),
        ),
        bitcoin = meaning(bitcoin, if (dark) NETWORK_GLOW_DARK else NETWORK_GLOW_LIGHT),
        lightning = meaning(lightning, if (dark) NETWORK_GLOW_DARK else NETWORK_GLOW_LIGHT),
        status = HaloStatusColors(
            positive = positive.color,
            positiveTint = positive.container,
            negative = error.color,
            negativeTint = error.container,
            warning = warning.color,
            warningTint = warning.container,
        ),
        isDark = dark,
        scheme = this,
    )
}

/** Dark theme at [contrast], from [seeds] (Halo's own by default). */
fun haloDarkColors(contrast: HaloContrast = HaloContrast.Standard, seeds: HaloSeeds = HaloSeeds.Halo): HaloColors =
    haloScheme(dark = true, contrast = contrast, seeds = seeds).toHaloColors()

/** Light theme at [contrast], from [seeds] (Halo's own by default). */
fun haloLightColors(contrast: HaloContrast = HaloContrast.Standard, seeds: HaloSeeds = HaloSeeds.Halo): HaloColors =
    haloScheme(dark = false, contrast = contrast, seeds = seeds).toHaloColors()

private const val PRESSED_TONE_STEP = 8f
private const val GLASS_DARK = 0.72f
private const val GLASS_LIGHT = 0.70f
private const val SCRIM_DARK = 0.60f
private const val SCRIM_LIGHT = 0.32f
private const val DISABLED_ALPHA = 0.38f
private const val SUBTLE_LINE = 0.5f
private const val HIGHLIGHT_DARK = 0.08f
private const val HIGHLIGHT_LIGHT = 0.90f
private const val TINT_TONE_DARK = 20f
private const val TINT_TONE_LIGHT = 92f
private const val TINT_CHROMA = 0.06f
private const val BRAND_GLOW_DARK = 0.30f
private const val BRAND_GLOW_LIGHT = 0.28f
private const val NETWORK_GLOW_DARK = 0.18f
private const val NETWORK_GLOW_LIGHT = 0.22f
