package com.halo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proposal 0006 phase 1: the generated Material 3 scheme keeps Material's tones, contrast and Halo's fidelity rules. */
class SchemeTest {

    private val schemes = listOf(true, false).flatMap { dark -> HaloContrast.entries.map { haloScheme(dark, it) } }

    private fun tone(c: Color) = Tone.of(c).l

    @Test
    fun standardTonesAreMaterials() {
        val dark = haloScheme(dark = true)
        val light = haloScheme(dark = false)
        // androidx material3 ColorDarkTokens / ColorLightTokens.
        val expected = listOf(
            dark.surface to 6f, dark.surfaceContainerLowest to 4f, dark.surfaceContainerLow to 10f,
            dark.surfaceContainer to 12f, dark.surfaceContainerHigh to 17f, dark.surfaceContainerHighest to 22f,
            dark.onSurface to 90f, dark.onSurfaceVariant to 80f, dark.outline to 60f, dark.outlineVariant to 30f,
            dark.primary.color to 80f, dark.secondary.container to 30f, dark.secondary.onContainer to 90f,
            light.surface to 98f, light.surfaceContainerLowest to 100f, light.surfaceContainerLow to 96f,
            light.surfaceContainer to 94f, light.surfaceContainerHigh to 92f, light.surfaceContainerHighest to 90f,
            light.onSurface to 10f, light.onSurfaceVariant to 30f, light.outline to 50f, light.outlineVariant to 80f,
            light.primary.color to 40f, light.secondary.container to 90f, light.secondary.onContainer to 10f,
        )
        for ((color, t) in expected) assertEquals("tone of $color", t, tone(color), 0.6f)
    }

    @Test
    fun textReachesAaOnEverySurfaceAtEveryContrast() {
        for (s in schemes) {
            val surfaces = listOf(s.surface, s.surfaceContainerLowest, s.surfaceContainerLow, s.surfaceContainer, s.surfaceContainerHigh, s.surfaceContainerHighest)
            for (bg in surfaces) {
                assertTrue("${s.isDark} ${s.contrast} onSurface", contrast(s.onSurface, bg) >= 4.5f)
                assertTrue("${s.isDark} ${s.contrast} onSurfaceVariant", contrast(s.onSurfaceVariant, bg) >= 4.5f)
            }
        }
    }

    @Test
    fun familyPairsReachAa() {
        for (s in schemes) {
            for ((name, f) in listOf("secondary" to s.secondary, "error" to s.error, "bitcoin" to s.bitcoin, "lightning" to s.lightning, "positive" to s.positive)) {
                assertTrue("${s.isDark} ${s.contrast} $name color/on", contrast(f.color, f.onColor) >= 4.5f)
                assertTrue("${s.isDark} ${s.contrast} $name container/on", contrast(f.container, f.onContainer) >= 4.5f)
                assertTrue("${s.isDark} ${s.contrast} $name text on surface", contrast(f.color, s.surface) >= 3f)
            }
            // A lit control's ink: AA, or the large-text / UI minimum on a mid-tone seed.
            for (f in listOf(s.primary, s.bitcoin, s.lightning)) assertTrue(contrast(f.fill, f.onFill) >= 3f)
        }
    }

    @Test
    fun fidelityKeepsTheSeeds() {
        val seeds = HaloSeeds.Halo
        for (dark in listOf(true, false)) {
            val s = haloScheme(dark)
            assertEquals(seeds.brand, s.primary.container)
            assertEquals(seeds.brand, s.primary.fill)
            assertEquals(seeds.bitcoin, s.bitcoin.fill)
            assertEquals(seeds.lightning, s.lightning.fill)
        }
    }

    @Test
    fun higherContrastLevelsPullTextAndOutlinesApart() {
        for (dark in listOf(true, false)) {
            val levels = HaloContrast.entries.map { haloScheme(dark, it) }
            val variant = levels.map { contrast(it.onSurfaceVariant, it.surface) }
            val outline = levels.map { contrast(it.outline, it.surface) }
            assertTrue("$dark variant $variant", variant[0] < variant[1] && variant[1] < variant[2])
            assertTrue("$dark outline $outline", outline[0] < outline[1] && outline[1] < outline[2])
        }
    }

    @Test
    fun neutralsCarryTheBrandHue() {
        val brandHue = Tone.of(HaloSeeds.Halo.brand).h
        val s = haloScheme(dark = true)
        assertEquals(brandHue, Tone.of(s.surfaceContainerHighest).h, 0.15f)
        assertTrue("neutral chroma stays faint", Tone.of(s.surfaceContainerHighest).c < 0.02f)
    }

    @Test
    fun toneRoundTrips() {
        for (c in listOf(Color(0xFF2A67FF), Color(0xFFED9449), Color(0xFFE6D06A), Color(0xFF7A7F87))) {
            val lch = Tone.of(c)
            val back = Tone.at(lch.h, lch.c, lch.l)
            assertEquals(c.red, back.red, 0.004f)
            assertEquals(c.green, back.green, 0.004f)
            assertEquals(c.blue, back.blue, 0.004f)
        }
    }
}
