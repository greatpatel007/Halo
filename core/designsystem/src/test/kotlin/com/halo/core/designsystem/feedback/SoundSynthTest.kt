package com.halo.core.designsystem.feedback

import com.halo.core.designsystem.theme.HaloHaptic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoundSynthTest {

    private val cues = HaloHaptic.entries.mapNotNull { Patterns.of(it).sound }

    @Test
    fun soundsAreShortQuietAndCleanAtTheEdges() {
        for (cue in cues) {
            val pcm = SoundSynth.render(cue)
            val peak = pcm.maxOf { abs(it) }
            assertTrue("$cue too loud: $peak", peak <= 0.31f)
            assertTrue("$cue silent", peak > 0.01f)
            assertEquals(0f, pcm.first(), 1e-4f)
            assertEquals(0f, pcm.last(), 1e-4f)
            val ms = pcm.size * 1000 / SoundSynth.SAMPLE_RATE
            assertTrue("$cue lasts $ms ms", ms <= 2400)
        }
    }

    @Test
    fun rendersAreDeterministic() {
        val cue = Patterns.Tap.sound!!
        assertTrue(SoundSynth.render(cue).contentEquals(SoundSynth.render(cue)))
    }

    @Test
    fun aChargingTickRisesInPitch() {
        fun pitch(intensity: Float): Float = (Patterns.tick(intensity).sound as HaloSoundCue.Click).pitchHz
        assertTrue(pitch(0.9f) > pitch(0.5f))
        assertTrue(pitch(0.5f) > pitch(0.1f))
        // Every pitch is a note of the pentatonic family (an octave up).
        for (i in 0..10) assertTrue(Pentatonic.notes.any { abs(it * 2f - pitch(i / 10f)) < 0.01f })
    }
}
