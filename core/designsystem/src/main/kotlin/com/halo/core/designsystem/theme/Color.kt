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
) {
    /** These colours with other accent families (see [haloBrand]); [focus] is the focus ring's colour. */
    fun copy(
        accent: HaloAccentColors = this.accent,
        bitcoin: HaloAccentColors = this.bitcoin,
        lightning: HaloAccentColors = this.lightning,
        focus: Color = line.focus,
    ): HaloColors = HaloColors(
        bg = bg,
        content = content,
        line = HaloLineColors(line.subtle, line.default, line.strong, focus, line.highlight),
        accent = accent,
        bitcoin = bitcoin,
        lightning = lightning,
        status = status,
        isDark = isDark,
    )
}

fun haloDarkColors(): HaloColors = with(HaloPalette) {
    HaloColors(
        bg = HaloBackgroundColors(
            canvas = Ink950,
            surface = Ink850,
            raised = Ink800,
            glass = Ink700.copy(alpha = 0.72f),
            scrim = Black.copy(alpha = 0.60f),
        ),
        content = HaloContentColors(
            primary = Ink100,
            secondary = Ink300,
            tertiary = Ink400,
            disabled = Ink500,
            inverse = Ink950,
        ),
        line = HaloLineColors(
            subtle = White.copy(alpha = 0.06f),
            default = White.copy(alpha = 0.10f),
            strong = White.copy(alpha = 0.16f),
            focus = Blue400,
            highlight = White.copy(alpha = 0.08f),
        ),
        accent = HaloAccentColors(
            fill = Blue500,
            fillPressed = Blue600,
            onFill = White,
            text = Blue400,
            tint = Blue900,
            glow = Blue500.copy(alpha = 0.30f),
        ),
        bitcoin = HaloAccentColors(
            fill = Btc500,
            fillPressed = Btc600,
            onFill = Ink950,
            text = Btc400,
            tint = Btc900,
            glow = Btc500.copy(alpha = 0.18f),
        ),
        lightning = HaloAccentColors(
            fill = Yellow500,
            fillPressed = Yellow550,
            onFill = Ink950,
            text = Yellow400,
            tint = Yellow900,
            glow = Yellow500.copy(alpha = 0.16f),
        ),
        status = HaloStatusColors(
            positive = Green500,
            positiveTint = Green900,
            negative = Red500,
            negativeTint = Red900,
            warning = Coral500,
            warningTint = Coral900,
        ),
        isDark = true,
    )
}

/**
 * Light theme — matte ceramic. Contrast against `bg.canvas` (#EBEEF2): primary 16:1, secondary 6.7:1,
 * tertiary 3.6:1; accent/bitcoin/lightning `text` roles use deep stops (>= 4.5:1). Fills keep their brand
 * hue with ink on top.
 */
fun haloLightColors(): HaloColors = with(HaloPalette) {
    HaloColors(
        bg = HaloBackgroundColors(
            canvas = Ceramic100,
            surface = Ceramic050,
            raised = Ceramic000,
            glass = Ceramic000.copy(alpha = 0.70f),
            scrim = Slate950.copy(alpha = 0.32f),
        ),
        content = HaloContentColors(
            primary = Slate950,
            secondary = Slate700,
            tertiary = Slate500,
            disabled = Slate400,
            inverse = Ceramic000,
        ),
        line = HaloLineColors(
            subtle = Slate950.copy(alpha = 0.06f),
            default = Slate950.copy(alpha = 0.10f),
            strong = Slate950.copy(alpha = 0.16f),
            focus = Blue600,
            highlight = White.copy(alpha = 0.90f),
        ),
        accent = HaloAccentColors(
            fill = Blue500,
            fillPressed = Blue600,
            onFill = White,
            text = Blue600,
            tint = Blue050,
            glow = Blue500.copy(alpha = 0.28f),
        ),
        bitcoin = HaloAccentColors(
            fill = Btc500,
            fillPressed = Btc550,
            onFill = Slate950,
            text = Btc800,
            tint = Btc050,
            glow = Btc500.copy(alpha = 0.22f),
        ),
        lightning = HaloAccentColors(
            fill = Yellow500,
            fillPressed = Yellow600,
            onFill = Slate950,
            text = Yellow800,
            tint = Yellow050,
            glow = Yellow550.copy(alpha = 0.26f),
        ),
        status = HaloStatusColors(
            positive = Green700,
            positiveTint = Green050,
            negative = Red700,
            negativeTint = Red050,
            warning = Coral700,
            warningTint = Coral050,
        ),
        isDark = false,
    )
}
