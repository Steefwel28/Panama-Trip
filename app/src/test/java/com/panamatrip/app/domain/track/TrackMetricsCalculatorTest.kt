package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La distancia solo puede sumar puntos consecutivos del mismo segmento: una
 * pausa no es un desplazamiento real del senderista.
 */
class TrackMetricsCalculatorTest {

    @Test
    fun `sums distance inside the same segment`() {
        val points = listOf(
            point(latitude = 8.9800, segment = 0),
            point(latitude = 8.9810, segment = 0),
        )

        val distance = TrackMetricsCalculator.distanceMeters(points)

        // ~111 m por milésima de grado; tolerancia amplia por la esfera.
        assertEquals(111.0, distance.toDouble(), 2.0)
    }

    @Test
    fun `never adds distance across a pause`() {
        val points = listOf(
            point(latitude = 8.9800, segment = 0),
            point(latitude = 8.9810, segment = 0),
            // Reanudación lejos del último punto: no debe contarse el salto.
            point(latitude = 8.9900, segment = 1),
            point(latitude = 8.9910, segment = 1),
        )

        val distance = TrackMetricsCalculator.distanceMeters(points)

        assertEquals(222.0, distance.toDouble(), 4.0)
    }

    @Test
    fun `speed is zero without active time`() {
        assertEquals(0.0, TrackMetricsCalculator.averageSpeedKmh(500, 0L), 0.0)
    }

    @Test
    fun `average speed uses active milliseconds`() {
        // 1 km en 10 minutos = 6 km/h.
        val speed = TrackMetricsCalculator.averageSpeedKmh(
            distanceMeters = 1_000,
            activeDurationMillis = 600_000L,
        )

        assertEquals(6.0, speed, 0.01)
    }

    private fun point(latitude: Double, segment: Int) = TrackPoint(
        point = GeoPoint(latitude = latitude, longitude = -79.5200),
        elevationMeters = 100.0,
        accuracyMeters = 5f,
        recordedAtEpochMillis = 0L,
        activeElapsedMillis = 0L,
        segmentIndex = segment,
    )
}
