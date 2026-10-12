package com.halo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.halo.core.designsystem.R

/** Paper Mono — every number, amount, address, label and data field. 8 static weights (OFL 1.1). */
val PaperMono = FontFamily(
    Font(R.font.halo_paper_mono_thin, FontWeight.Thin),
    Font(R.font.halo_paper_mono_extralight, FontWeight.ExtraLight),
    Font(R.font.halo_paper_mono_light, FontWeight.Light),
    Font(R.font.halo_paper_mono_regular, FontWeight.Normal),
    Font(R.font.halo_paper_mono_medium, FontWeight.Medium),
    Font(R.font.halo_paper_mono_semibold, FontWeight.SemiBold),
    Font(R.font.halo_paper_mono_bold, FontWeight.Bold),
    Font(R.font.halo_paper_mono_extrabold, FontWeight.ExtraBold),
)

/**
 * Satoshi — UI and body text. One variable file (wght 300–900); each weight is pinned explicitly
 * because the file's default instance is 900.
 */
val Satoshi = FontFamily(
    satoshi(FontWeight.Light),
    satoshi(FontWeight.Normal),
    satoshi(FontWeight.Medium),
    satoshi(FontWeight.Bold),
    satoshi(FontWeight.Black),
)

private fun satoshi(weight: FontWeight) = Font(
    resId = R.font.halo_satoshi_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Trims the first ascent / last descent so a text box is exactly its line height — pixel-exact alignment. */
private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    trackingEm: Float = 0f,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = trackingEm.em,
    lineHeightStyle = TrimmedLineHeight,
)

/**
 * Material 3's type roles (androidx `TypeScaleTokens`, display → label, plus the Expressive emphasized variants) set
 * in the brand's sans, and Halo's numeric roles in its mono (numbers that are values never use the sans). Proposal 0006.
 *
 * The old Halo names at the bottom are **migration aliases** onto the new roles; they go away in 0006 phase 7.
 */
@Immutable
class HaloTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    // Material 3 Expressive emphasized variants.
    val displayLargeEmphasized: TextStyle,
    val displayMediumEmphasized: TextStyle,
    val displaySmallEmphasized: TextStyle,
    val headlineLargeEmphasized: TextStyle,
    val headlineMediumEmphasized: TextStyle,
    val headlineSmallEmphasized: TextStyle,
    val titleLargeEmphasized: TextStyle,
    val titleMediumEmphasized: TextStyle,
    val titleSmallEmphasized: TextStyle,
    val bodyLargeEmphasized: TextStyle,
    val bodyMediumEmphasized: TextStyle,
    val bodySmallEmphasized: TextStyle,
    val labelLargeEmphasized: TextStyle,
    val labelMediumEmphasized: TextStyle,
    val labelSmallEmphasized: TextStyle,
    // Halo numeric roles (mono).
    /** Balance hero. Mono Light 48/52, −3%. */
    val amountDisplay: TextStyle,
    /** Amount entry. Mono Regular 32/38, −2%. */
    val amountHeadline: TextStyle,
    /** Card amounts. Mono Regular 20/24, −1%. */
    val amountTitle: TextStyle,
    /** List-row amounts. Mono Medium 15/20. */
    val amountBody: TextStyle,
    /** Addresses, txids, timestamps. Mono Regular 13/18. */
    val data: TextStyle,
    /** Section labels, UPPERCASE. Mono Medium 11/14, +8%. Retires in 0006 (sentence-case titles replace it). */
    val overline: TextStyle,
    /** Badges, chips. Mono SemiBold 10/12, +6%. Retires in 0006 (Label Small replaces it). */
    val micro: TextStyle,
) {
    // ---- Migration aliases (0006): old Halo names → Material roles.
    val amountHero: TextStyle get() = amountDisplay
    val amountLarge: TextStyle get() = amountHeadline
    val amountMedium: TextStyle get() = amountTitle
    val amountSmall: TextStyle get() = amountBody
    val headline: TextStyle get() = headlineSmall
    val title: TextStyle get() = titleLarge
    val subtitle: TextStyle get() = titleMedium
    val body: TextStyle get() = bodyLarge
    val label: TextStyle get() = labelLarge
}

/** The type scale set in [fonts] (Halo's own by default). Sizes/weights/tracking from Material 3's tokens. */
fun haloTypography(fonts: HaloFonts = HaloFonts.Halo): HaloTypography = with(fonts) {
    val r = FontWeight.Normal
    val m = FontWeight.Medium
    val b = FontWeight.Bold
    fun t(weight: FontWeight, size: Float, line: Float, trackingSp: Float) = styleSp(sans, weight, size, line, trackingSp)
    HaloTypography(
        displayLarge = t(r, 57f, 64f, -0.25f),
        displayMedium = t(r, 45f, 52f, 0f),
        displaySmall = t(r, 36f, 44f, 0f),
        headlineLarge = t(r, 32f, 40f, 0f),
        headlineMedium = t(r, 28f, 36f, 0f),
        headlineSmall = t(r, 24f, 32f, 0f),
        titleLarge = t(r, 22f, 28f, 0f),
        titleMedium = t(m, 16f, 24f, 0.15f),
        titleSmall = t(m, 14f, 20f, 0.1f),
        bodyLarge = t(r, 16f, 24f, 0.5f),
        bodyMedium = t(r, 14f, 20f, 0.25f),
        bodySmall = t(r, 12f, 16f, 0.4f),
        labelLarge = t(m, 14f, 20f, 0.1f),
        labelMedium = t(m, 12f, 16f, 0.5f),
        labelSmall = t(m, 11f, 16f, 0.5f),
        displayLargeEmphasized = t(m, 57f, 64f, 0f),
        displayMediumEmphasized = t(m, 45f, 52f, 0f),
        displaySmallEmphasized = t(m, 36f, 44f, 0f),
        headlineLargeEmphasized = t(m, 32f, 40f, 0f),
        headlineMediumEmphasized = t(m, 28f, 36f, 0f),
        headlineSmallEmphasized = t(m, 24f, 32f, 0f),
        titleLargeEmphasized = t(m, 22f, 28f, 0f),
        titleMediumEmphasized = t(b, 16f, 24f, 0.15f),
        titleSmallEmphasized = t(b, 14f, 20f, 0.1f),
        bodyLargeEmphasized = t(m, 16f, 24f, 0.15f),
        bodyMediumEmphasized = t(m, 14f, 20f, 0.25f),
        bodySmallEmphasized = t(m, 12f, 16f, 0.4f),
        labelLargeEmphasized = t(b, 14f, 20f, 0.1f),
        labelMediumEmphasized = t(b, 12f, 16f, 0.5f),
        labelSmallEmphasized = t(b, 11f, 16f, 0.5f),
        amountDisplay = style(mono, FontWeight.Light, 48, 52, -0.03f),
        amountHeadline = style(mono, FontWeight.Normal, 32, 38, -0.02f),
        amountTitle = style(mono, FontWeight.Normal, 20, 24, -0.01f),
        amountBody = style(mono, FontWeight.Medium, 15, 20),
        data = style(mono, FontWeight.Normal, 13, 18),
        overline = style(mono, FontWeight.Medium, 11, 14, 0.08f),
        micro = style(mono, FontWeight.SemiBold, 10, 12, 0.06f),
    )
}

/** A style with Material's absolute tracking (sp), trimmed line box like every Halo style. */
private fun styleSp(family: FontFamily, weight: FontWeight, size: Float, lineHeight: Float, trackingSp: Float) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = trackingSp.sp,
    lineHeightStyle = TrimmedLineHeight,
)
