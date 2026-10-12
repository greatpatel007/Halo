package com.halo.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetPhysicsTest {

    private val screen = 2800f

    @Test
    fun projectionIsScrollViewDeceleration() {
        // A release at 1000 px/s travels v/1000 · r/(1 − r) = 499 px under the 0.998 rate.
        assertEquals(1499f, SheetPhysics.project(1000f, 1000f), 0.5f)
        assertEquals(1000f, SheetPhysics.project(1000f, 0f), 0.001f)
    }

    @Test
    fun slowDragGoesToNearestPointAndFastFlickCarries() {
        val anchors = listOf(400f, 1400f, screen)
        // Released just below the partial point with no speed: stays at partial.
        assertEquals(1400f, SheetPhysics.settle(1500f, 0f, anchors))
        // Same position, a firm downward flick (2400 px/s → ~1200 px): dismissed.
        assertEquals(screen, SheetPhysics.settle(1500f, 2400f, anchors))
        // A short upward flick from partial carries it all the way up.
        assertEquals(400f, SheetPhysics.settle(1380f, -2200f, anchors))
    }

    @Test
    fun rubberBandResistsAndNeverReachesTheDimension() {
        val d = 1000f
        assertEquals(500f, SheetPhysics.rubberBand(500f, 400f, screen, d), 0f)
        val small = SheetPhysics.resist(50f, d)
        val big = SheetPhysics.resist(5000f, d)
        assertTrue("resists from the start", small < 50f)
        assertTrue("more pull, more travel", big > small)
        assertTrue("never reaches the dimension", big < d)
        // Past the top the sheet sits above the top anchor, by the resisted distance.
        assertEquals(400f - SheetPhysics.resist(100f, d), SheetPhysics.rubberBand(300f, 400f, screen, d), 0.001f)
    }

    @Test
    fun partialRestingPointOnlyForTallContent() {
        val short = SheetPhysics.anchors(screen, 900f)
        assertNull(short.partial)
        assertEquals(screen - 900f, short.expanded, 0f)
        assertEquals(screen - 900f, short.lowest, 0f)

        val tall = SheetPhysics.anchors(screen, 2400f)
        assertEquals(screen / 2f, tall.partial!!, 0f)
        assertEquals(screen - 2400f, tall.expanded, 0f)
        assertTrue(tall.expanded < tall.partial!!)
    }
}
