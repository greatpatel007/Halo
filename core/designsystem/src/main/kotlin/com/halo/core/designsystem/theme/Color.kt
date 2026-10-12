package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Primitive ramps. Components never reference these directly — use [HaloColors] semantic roles.
 * The only file allowed to declare raw color literals is this one.
 */
object HaloPalette {
    // Ink — dark theme neutrals (soft-touch black).
    val Ink950 = Color(0xFF0A0A0B)
    val Ink900 = Color(0xFF101012)
    val Ink850 = Color(0xFF141416)
    val Ink800 = Color(0xFF1A1A1D)
    val Ink700 = Color(0xFF232327)
    val Ink600 = Color(0xFF2E2E34)
    val Ink500 = Color(0xFF46464D)
    val Ink400 = Color(0xFF6E6E76)
    val Ink300 = Color(0xFF9C9CA3)
    val Ink200 = Color(0xFFCFCFD2)
    val Ink100 = Color(0xFFF4F4F2)

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

    // Volt — the UI accent.
    val Volt300 = Color(0xFFE6FF8A)
    val Volt400 = Color(0xFFDCFF5E)
    val Volt500 = Color(0xFFD4FF3A)
    val Volt550 = Color(0xFFC6F432)
    val Volt600 = Color(0xFFB6DD22)
    val Volt650 = Color(0xFFB4E21D)
    val Volt800 = Color(0xFF4D6600)
    val Volt850 = Color(0xFF5B7A00)
    val Volt900 = Color(0xFF1C2209)
    val Volt050 = Color(0xFFEEF9CC)

    // Bitcoin — on-chain only.
    val Btc300 = Color(0xFFFFB977)
    val Btc400 = Color(0xFFFFA04A)
    val Btc500 = Color(0xFFFF8A1F)
    val Btc550 = Color(0xFFEE7A0E)
    val Btc600 = Color(0xFFE0700A)
    val Btc800 = Color(0xFFB4500A)
    val Btc900 = Color(0xFF2B1707)
    val Btc050 = Color(0xFFFFEBD8)

    // Lightning — electric cyan, Lightning only.
    val Cyan300 = Color(0xFF7AE8FF)
    val Cyan500 = Color(0xFF2EDBFF)
    val Cyan550 = Color(0xFF12C2E8)
    val Cyan600 = Color(0xFF00B8E6)
    val Cyan650 = Color(0xFF00A3CC)
    val Cyan800 = Color(0xFF00708C)
    val Cyan900 = Color(0xFF072530)
    val Cyan050 = Color(0xFFD9F6FF)

    // Status.
    val Green500 = Color(0xFF3DDC97)
    val Green700 = Color(0xFF0B7A4F)
    val Green900 = Color(0xFF0F2A1F)
    val Green050 = Color(0xFFDDF5EA)
    val Red500 = Color(0xFFFF5A5F)
    val Red700 = Color(0xFFC42D33)
    val Red900 = Color(0xFF2E1213)
    val Red050 = Color(0xFFFCE3E4)
    val Amber500 = Color(0xFFFFC857)
    val Amber700 = Color(0xFF8A5C00)
    val Amber900 = Color(0xFF2C2410)
    val Amber050 = Color(0xFFFFF1D1)

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

/** An accent family: lime (UI), Bitcoin orange (on-chain) or Lightning cyan. */
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
    /** Electric lime — every interactive UI accent. */
    val accent: HaloAccentColors,
    /** Bitcoin orange — on-chain Bitcoin only. */
    val bitcoin: HaloAccentColors,
    /** Electric cyan — Lightning only. */
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
            focus = Volt500,
            highlight = White.copy(alpha = 0.08f),
        ),
        accent = HaloAccentColors(
            fill = Volt500,
            fillPressed = Volt600,
            onFill = Ink950,
            text = Volt400,
            tint = Volt900,
            glow = Volt500.copy(alpha = 0.24f),
        ),
        bitcoin = HaloAccentColors(
            fill = Btc500,
            fillPressed = Btc600,
            onFill = Ink950,
            text = Btc400,
            tint = Btc900,
            glow = Btc500.copy(alpha = 0.24f),
        ),
        lightning = HaloAccentColors(
            fill = Cyan500,
            fillPressed = Cyan550,
            onFill = Ink950,
            text = Cyan300,
            tint = Cyan900,
            glow = Cyan500.copy(alpha = 0.26f),
        ),
        status = HaloStatusColors(
            positive = Green500,
            positiveTint = Green900,
            negative = Red500,
            negativeTint = Red900,
            warning = Amber500,
            warningTint = Amber900,
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
            focus = Volt850,
            highlight = White.copy(alpha = 0.90f),
        ),
        accent = HaloAccentColors(
            fill = Volt550,
            fillPressed = Volt650,
            onFill = Slate950,
            text = Volt800,
            tint = Volt050,
            glow = Volt550.copy(alpha = 0.30f),
        ),
        bitcoin = HaloAccentColors(
            fill = Btc500,
            fillPressed = Btc550,
            onFill = Slate950,
            text = Btc800,
            tint = Btc050,
            glow = Btc500.copy(alpha = 0.30f),
        ),
        lightning = HaloAccentColors(
            fill = Cyan600,
            fillPressed = Cyan650,
            onFill = Slate950,
            text = Cyan800,
            tint = Cyan050,
            glow = Cyan600.copy(alpha = 0.30f),
        ),
        status = HaloStatusColors(
            positive = Green700,
            positiveTint = Green050,
            negative = Red700,
            negativeTint = Red050,
            warning = Amber700,
            warningTint = Amber050,
        ),
        isDark = false,
    )
}
