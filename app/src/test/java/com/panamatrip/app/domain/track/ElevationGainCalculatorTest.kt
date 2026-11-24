package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationGainCalculatorTest {

    @Test
    fun `medians out a single GPS spike`() {
        val elevations = listOf(100.0, 100.0, 150.0, 100.0, 100.0)

        val gain = ElevationGainCalculator.gainMeters(elevations)

        // La mediana móvil de 5 aplana el pico de 150 m.
        assertEquals(0, gain)
    }

    @Test
    fun `sums only positive deltas above six meters`() {
        val elevations = listOf(100.0, 100.0, 100.0, 130.0, 130.0, 130.0)

        val gain = ElevationGainCalculator.gainMeters(elevations)

        // Meseta de 100 m → meseta de 130 m: 30 m reales, sin sumar el ruido.
        assertEquals(30, gain)
    }

    @Test
    fun `ignores variations below the noise threshold`() {
        val elevations = listOf(50.0, 50.8, 50.2, 51.0, 50.4, 51.1)

        val gain = ElevationGainCalculator.gainMeters(elevations)

        assertEquals(0, gain)
    }

    @Test
    fun `higher threshold filters small climbs`() {
        val elevations = listOf(10.0, 10.0, 10.0, 50.0, 50.0, 50.0)

        assertEquals(40, ElevationGainCalculator.gainMeters(elevations))
        assertEquals(0, ElevationGainCalculator.gainMeters(elevations, noiseThresholdMeters = 50.0))
    }

    @Test
    fun `skips missing and non finite altitudes`() {
        val elevations = listOf(
            null, 100.0, 100.0, 110.0, 110.0, Double.NaN, 120.0, 120.0,
        )

        val gain = ElevationGainCalculator.gainMeters(elevations)

        // Los nulos y NaN se descartan; la rampa limpia suma 10 m + 10 m.
        assertEquals(20, gain)
    }

    @Test
    fun `does not add gain across different segments`() {
        val points = listOf(
            point(elevation = 100.0, segment = 0),
            point(elevation = 120.0, segment = 0),
            point(elevation = 100.0, segment = 1),
            point(elevation = 120.0, segment = 1),
        )

        // Cada segmento sube 20 m; si se cruzaran, la bajada anularía una subida.
        assertEquals(40, ElevationGainCalculator.gainMeters(points))
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

    private fun point(elevation: Double, segment: Int) = TrackPoint(
        point = GeoPoint(latitude = 8.98, longitude = -79.52),
        elevationMeters = elevation,
        accuracyMeters = 5f,
        recordedAtEpochMillis = 0L,
        activeElapsedMillis = 0L,
        segmentIndex = segment,
    )
}
