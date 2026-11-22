package com.panamatrip.app.domain.track

import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationGainCalculatorTest {

    @Test
    fun `sums only positive deltas above the noise threshold`() {
        val elevations = listOf(100.0, 112.0, 108.0, 125.0, 121.0, 140.0)

        val gain = ElevationGainCalculator.gainMeters(elevations)

        // +12, +17 y +19; las bajadas no restan.
        assertEquals(48, gain)
    }

    @Test
    fun `ignores variations below the noise threshold`() {
        val elevations = listOf(50.0, 50.8, 50.2, 51.0, 50.4, 51.1)

        val gain = ElevationGainCalculator.gainMeters(elevations)

        assertEquals(0, gain)
    }

    @Test
    fun `higher threshold filters small climbs`() {
        val elevations = listOf(0.0, 10.0, 12.0)

        assertEquals(12, ElevationGainCalculator.gainMeters(elevations))
        assertEquals(10, ElevationGainCalculator.gainMeters(elevations, noiseThresholdMeters = 5.0))
    }

    @Test
    fun `skips missing and non finite altitudes`() {
        val elevations = listOf(null, 100.0, Double.NaN, 110.0, null, 120.0)

        val gain = ElevationGainCalculator.gainMeters(elevations)

        assertEquals(20, gain)
    }

    @Test
    fun `empty and single point inputs yield zero`() {
        assertEquals(0, ElevationGainCalculator.gainMeters(emptyList<Double?>()))
        assertEquals(0, ElevationGainCalculator.gainMeters(listOf(340.0)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative threshold is rejected`() {
        ElevationGainCalculator.gainMeters(listOf(1.0, 2.0), noiseThresholdMeters = -1.0)
    }
}
