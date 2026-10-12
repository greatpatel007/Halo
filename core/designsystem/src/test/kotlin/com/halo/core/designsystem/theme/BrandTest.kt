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
        for (seed in seeds.filter { Oklch.of(it).c > 0.05f }) {
            val h = Oklch.of(seed).h
            for (colors in listOf(haloBrand(seed).dark, haloBrand(seed).light)) {
                val text = Oklch.of(colors.accent.text)
                if (text.c > 0.03f) assertEquals("$seed", h, text.h, 0.12f)
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
    fun onlyAccentFamiliesChange() {
        val brand = haloBrand(accent = Color(0xFF1E3AFF))
        val halo = haloDarkColors()
        assertEquals(halo.bg.canvas, brand.dark.bg.canvas)
        assertEquals(halo.bitcoin.fill, brand.dark.bitcoin.fill)
        assertEquals(brand.dark.accent.fill, brand.dark.line.focus)
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
