package com.halo.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

/** The glass halo's liquid follows real first-order viscous dynamics (lab frame, relative motion shown). */
class FluidFlowTest {

    private fun FluidFlow.run(seconds: Float, dt: Float = 1f / 60f) {
        repeat((seconds / dt).toInt()) { step(dt) }
    }

    @Test
    fun spinsUpFromRestToTheDrive() {
        val flow = FluidFlow()
        flow.run(VISCOUS_S)
        // One time constant: 1 − 1/e of the way there.
        assertEquals(DRIVE * (1 - exp(-1f)), flow.rate, 0.01f)
        flow.run(VISCOUS_S * 6)
        assertEquals(DRIVE, flow.rate, 0.001f)
    }

    @Test
    fun aTapAddsMomentumThatViscosityBleedsOff() {
        val flow = FluidFlow()
        flow.run(VISCOUS_S * 8)
        flow.push(TAP_IMPULSE)
        flow.step(1f / 60f)
        assertTrue(flow.rate > DRIVE + TAP_IMPULSE * 0.95f)
        val start = flow.phase
        flow.run(VISCOUS_S)
        assertEquals(DRIVE + TAP_IMPULSE * exp(-1f), flow.rate, 0.05f)
        flow.run(VISCOUS_S * 9)
        // The extra distance travelled is impulse × τ (≈ 8.4 rad, about one and a third laps) on top of the drive.
        // (Within one frame of travel: the first frame after the tap is counted before `start`.)
        assertEquals(TAP_IMPULSE * VISCOUS_S + DRIVE * VISCOUS_S * 10, flow.phase - start, 0.25f)
    }

    @Test
    fun turningThePhoneMakesTheLiquidLagThenCatchUp() {
        val flow = FluidFlow()
        flow.run(VISCOUS_S * 8)
        // The phone starts turning counter-clockwise (glass clockwise rate −3 rad/s).
        flow.glassSpin = -3f
        flow.step(1f / 60f)
        // Relative to the glass the liquid now runs ~3 rad/s the other way: its inertia keeps it still.
        assertEquals(DRIVE + 3f, flow.rate, 0.1f)
        flow.run(VISCOUS_S * 8)
        // Viscosity drags it along with the glass: only the drive is left.
        assertEquals(DRIVE, flow.rate, 0.01f)
    }
}
