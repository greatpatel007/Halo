package com.halo.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentPhysicsTest {

    @Test
    fun detentIsTheNearestSegmentClampedToTheTrack() {
        assertEquals(0, SegmentPhysics.nearest(0.49f, 3))
        assertEquals(1, SegmentPhysics.nearest(0.51f, 3))
        assertEquals(2, SegmentPhysics.nearest(2.2f, 3))
        assertEquals(0, SegmentPhysics.nearest(-0.2f, 3))
        assertEquals(0, SegmentPhysics.nearest(0.7f, 1))
    }

    @Test
    fun slowReleaseStaysAndFlickCarriesOneSegment() {
        // Dropped at 0.4 with no speed: back to 0.
        assertEquals(0, SegmentPhysics.target(0.4f, 0f, 3))
        // Same place, a flick at 5 segments/s carries 0.6 → lands on 1.
        assertEquals(1, SegmentPhysics.target(0.4f, 5f, 3))
        // A hard throw can't leave the track.
        assertEquals(2, SegmentPhysics.target(1f, 40f, 3))
        assertEquals(0, SegmentPhysics.target(1f, -40f, 3))
    }

    @Test
    fun rubberBandFollowsInsideAndResistsPastTheEnds() {
        assertEquals(1.3f, SegmentPhysics.rubberBand(1.3f, 3), 0f)
        val past = SegmentPhysics.rubberBand(3f, 3) - 2f
        assertTrue("resists from the start", past in 0f..1f && past < 1f)
        val far = SegmentPhysics.rubberBand(50f, 3) - 2f
        assertTrue("more pull, more travel", far > past)
        assertTrue("never reaches the limit", far < SegmentPhysics.OVERSHOOT)
        assertTrue("mirrors at the start", SegmentPhysics.rubberBand(-1f, 3) in -SegmentPhysics.OVERSHOOT..0f)
    }

    @Test
    fun thumbCompressesOnlyPastTheWallAndBounded() {
        assertEquals(0f, SegmentPhysics.squash(1.5f, 3), 0f)
        assertEquals(0f, SegmentPhysics.squash(0f, 3), 0f)
        val max = SegmentPhysics.OVERSHOOT * SegmentPhysics.SQUASH
        val pulled = SegmentPhysics.squash(SegmentPhysics.rubberBand(100f, 3), 3)
        assertTrue("compresses", pulled > 0f)
        assertTrue("at most 12% narrower", pulled < max && max <= 0.12f + 1e-6f)
    }

    @Test
    fun labelColourFollowsTheThumbsDistance() {
        assertEquals(1f, SegmentPhysics.weight(1f, 1, 3), 0f)
        assertEquals(0.5f, SegmentPhysics.weight(0.5f, 1, 3), 1e-6f)
        assertEquals(0f, SegmentPhysics.weight(0f, 2, 3), 0f)
        // Overshoot never dims the end segment.
        assertEquals(1f, SegmentPhysics.weight(2.2f, 2, 3), 0f)
    }
}
