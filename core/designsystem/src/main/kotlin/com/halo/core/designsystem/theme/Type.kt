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
    Font(R.font.paper_mono_thin, FontWeight.Thin),
    Font(R.font.paper_mono_extralight, FontWeight.ExtraLight),
    Font(R.font.paper_mono_light, FontWeight.Light),
    Font(R.font.paper_mono_regular, FontWeight.Normal),
    Font(R.font.paper_mono_medium, FontWeight.Medium),
    Font(R.font.paper_mono_semibold, FontWeight.SemiBold),
    Font(R.font.paper_mono_bold, FontWeight.Bold),
    Font(R.font.paper_mono_extrabold, FontWeight.ExtraBold),
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
    resId = R.font.satoshi_variable,
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

@Immutable
class HaloTypography(
    /** Balance hero. Paper Mono Light 48/52, −3%. */
    val amountHero: TextStyle,
    /** Amount entry. Paper Mono Regular 32/38, −2%. */
    val amountLarge: TextStyle,
    /** Card amounts. Paper Mono Regular 20/24, −1%. */
    val amountMedium: TextStyle,
    /** List-row amounts. Paper Mono Medium 15/20. */
    val amountSmall: TextStyle,
    /** Screen titles. Satoshi Bold 26/32, −2%. */
    val headline: TextStyle,
    /** Sheet titles. Satoshi Bold 20/26, −1%. */
    val title: TextStyle,
    /** Row titles. Satoshi Medium 17/22, −0.5%. */
    val subtitle: TextStyle,
    /** Paragraphs. Satoshi Regular 15/22. */
    val body: TextStyle,
    /** Helper text. Satoshi Regular 13/18. */
    val bodySmall: TextStyle,
    /** Buttons. Satoshi Bold 15/20. */
    val label: TextStyle,
    /** Section labels — always UPPERCASE. Paper Mono Medium 11/14, +8%. */
    val overline: TextStyle,
    /** Addresses, txids, timestamps. Paper Mono Regular 13/18. */
    val data: TextStyle,
    /** Badges, chips. Paper Mono SemiBold 10/12, +6%. */
    val micro: TextStyle,
)

fun haloTypography(): HaloTypography = HaloTypography(
    amountHero = style(PaperMono, FontWeight.Light, 48, 52, -0.03f),
    amountLarge = style(PaperMono, FontWeight.Normal, 32, 38, -0.02f),
    amountMedium = style(PaperMono, FontWeight.Normal, 20, 24, -0.01f),
    amountSmall = style(PaperMono, FontWeight.Medium, 15, 20),
    headline = style(Satoshi, FontWeight.Bold, 26, 32, -0.02f),
    title = style(Satoshi, FontWeight.Bold, 20, 26, -0.01f),
    subtitle = style(Satoshi, FontWeight.Medium, 17, 22, -0.005f),
    body = style(Satoshi, FontWeight.Normal, 15, 22),
    bodySmall = style(Satoshi, FontWeight.Normal, 13, 18),
    label = style(Satoshi, FontWeight.Bold, 15, 20),
    overline = style(PaperMono, FontWeight.Medium, 11, 14, 0.08f),
    data = style(PaperMono, FontWeight.Normal, 13, 18),
    micro = style(PaperMono, FontWeight.SemiBold, 10, 12, 0.06f),
)
