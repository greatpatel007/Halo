package com.halo.core.designsystem.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shared clock's pacing: hidden effects never step, busy ones get every frame, calm ones ~30 fps. */
class AmbientSchedulerTest {

    private val frame = 1f / 60f

    @Test
    fun hiddenEffectsNeverStep() {
        val s = AmbientScheduler()
        var steps = 0
        s.add { steps++; true }
        repeat(60) { s.frame(frame) }
        assertEquals(0, steps)
    }

    @Test
    fun busyEffectsStepEveryFrameCalmOnesAtThirtyFps() {
        val s = AmbientScheduler()
        var busy = true
        var steps = 0
        var time = 0f
        val h = s.add { dt -> steps++; time += dt; busy }
        s.setVisible(h, true)
        repeat(60) { s.frame(frame) }
        assertEquals(60, steps)
        busy = false
        steps = 0
        repeat(60) { s.frame(frame) }
        // One step to learn it's calm, then every other 60 Hz frame.
        assertTrue("calm steps $steps", steps in 30..31)
        // No time is lost or invented: physics time equals real time.
        assertEquals(2f, time, 0.02f)
        // At exactly 60 Hz the calm rate is 30/s (every second frame), never 20/s from float rounding.
        steps = 0
        repeat(600) { s.frame(1f / 60f) }
        assertTrue("calm steps over 10 s: $steps", steps in 299..301)
    }

    @Test
    fun aPauseIsNotHandedOverAsElapsedTime() {
        val s = AmbientScheduler()
        var last = 0f
        val h = s.add { dt -> last = dt; false }
        s.setVisible(h, true)
        s.frame(frame)
        s.reset()
        s.frame(5f)
        assertTrue(last <= AmbientScheduler.MAX_STEP_S)
    }

    @Test
    fun anyBusyEffectKeepsEveryoneAtFullRate() {
        val s = AmbientScheduler()
        var calmSteps = 0
        val a = s.add { calmSteps++; false }
        val b = s.add { true }
        s.setVisible(a, true)
        s.setVisible(b, true)
        repeat(60) { s.frame(frame) }
        assertEquals(60, calmSteps)
        s.remove(b)
        assertEquals(1, s.visibleCount)
    }
}
