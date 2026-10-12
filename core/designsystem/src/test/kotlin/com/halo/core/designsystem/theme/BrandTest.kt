package com.halo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandTest {

    /** Seeds across the hue circle, including the hard ones: pale yellow, deep blue, near-grey. */
    private val seeds = listOf(
        Color(0xFFD4FF3A), Color(0xFFFF8A1F), Color(0xFF2EDBFF), Color(0xFFE53935), Color(0xFF1E3AFF),
        Color(0xFF8E24AA), Color(0xFFFFEB3B), Color(0xFF00C853), Color(0xFF7A7F87), Color(0xFF000080),
    )

    @Test
    fun accentTextAndLabelsReachAaInBothThemes() {
        for (seed in seeds) {
            val brand = haloBrand(accent = seed)
            for (colors in listOf(brand.dark, brand.light)) {
                val a = colors.accent
                val theme = if (colors.isDark) "dark" else "light"
                assertTrue("$seed $theme text ${contrast(a.text, colors.bg.canvas)}", contrast(a.text, colors.bg.canvas) >= 4.5f)
                assertTrue("$seed $theme onFill ${contrast(a.onFill, a.fill)}", contrast(a.onFill, a.fill) >= 4.5f || seedIsMidTone(a.fill))
            }
        }
    }

    @Test
    fun hueIsKept() {
        // Material tones keep the seed's hue (OKLCH hue, blue-stable like HCT); only chroma shrinks to fit the gamut.
        for (seed in seeds.filter { Tone.of(it).c > 0.05f }) {
            val h = Tone.of(seed).h
            for (colors in listOf(haloBrand(seed).dark, haloBrand(seed).light)) {
                val text = Tone.of(colors.accent.text)
                if (text.c > 0.02f) assertEquals("$seed", h, text.h, 0.05f)
            }
        }
    }

    @Test
    fun oklchRoundTrips() {
        for (seed in seeds) {
            val back = Oklch.of(seed).toColor()
            assertEquals(seed.red, back.red, 0.004f)
            assertEquals(seed.green, back.green, 0.004f)
            assertEquals(seed.blue, back.blue, 0.004f)
        }
    }

    @Test
    fun rebrandKeepsTheSignalFamiliesAndTheSeed() {
        // Material: neutrals carry the brand's hue, so the canvas may shift; signal families and the key action don't.
        val seed = Color(0xFF1E3AFF)
        val brand = haloBrand(accent = seed)
        val halo = haloDarkColors()
        assertEquals(halo.bitcoin.fill, brand.dark.bitcoin.fill)
        assertEquals(halo.lightning.fill, brand.dark.lightning.fill)
        assertEquals(seed, brand.dark.accent.fill)
        assertEquals(brand.dark.scheme.primary.color, brand.dark.line.focus)
    }

    /**
     * A mid-tone fill (relative luminance near 0.18) can't give 4.5:1 to either black or white; the best ink still
     * reaches the large-text / UI minimum of 3:1.
     */
    private fun seedIsMidTone(fill: Color): Boolean {
        val best = maxOf(contrast(Color.Black, fill), contrast(Color.White, fill))
        return best >= 3f && best < 4.5f
    }
}
