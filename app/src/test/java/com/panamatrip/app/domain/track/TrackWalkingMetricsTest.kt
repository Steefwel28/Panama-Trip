package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackWalkingMetricsTest {

    private fun point(
        latitude: Double,
        elapsedMillis: Long,
        segment: Int = 0,
    ) = TrackPoint(
        point = GeoPoint(latitude, -79.5),
        elevationMeters = 0.0,
        accuracyMeters = 5f,
        recordedAtEpochMillis = elapsedMillis,
        activeElapsedMillis = elapsedMillis,
        segmentIndex = segment,
    )

    @Test
    fun `tramo sobre 15 km por hora no suma al odometro`() {
        // ~11 m en 1 s = 11 m/s (39.6 km/h): se dibuja, pero no es caminata.
        val fast = listOf(point(8.0, 0), point(8.0001, 1_000))

        val valid = TrackMetricsCalculator.validWalkingDistanceMeters(fast)

        assertEquals(0, valid)
        assertTrue(TrackMetricsCalculator.distanceMeters(fast) > 0)
    }

    @Test
    fun `tramo bajo 15 km por hora si suma`() {
        // ~11 m en 5 s = 2.2 m/s (7.9 km/h).
        val walk = listOf(point(8.0, 0), point(8.0001, 5_000))

        val valid = TrackMetricsCalculator.validWalkingDistanceMeters(walk)

        assertEquals(TrackMetricsCalculator.distanceMeters(walk), valid)
    }

    @Test
    fun `salto entre segmentos nunca suma`() {
        val pause = listOf(point(8.0, 0), point(8.01, 5_000, segment = 1))

        assertEquals(0, TrackMetricsCalculator.validWalkingDistanceMeters(pause))
        assertEquals(0, TrackMetricsCalculator.distanceMeters(pause))
    }

    @Test
    fun `tramo cercano al limite de 15 km por hora se acepta`() {
        // ~4.5 m en 1.1 s = 4.05 m/s (14.6 km/h).
        val walk = listOf(point(8.0, 0), point(8.00004, 1_100))

        assertEquals(
            TrackMetricsCalculator.distanceMeters(walk),
            TrackMetricsCalculator.validWalkingDistanceMeters(walk),
        )
    }
}
