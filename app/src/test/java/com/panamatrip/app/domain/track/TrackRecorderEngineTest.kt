package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.TrackSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El cronómetro del grabador debe avanzar con el reloj monotónico, no con la
 * llegada de coordenadas GPS. Estos casos protegen el ticker de un segundo que
 * vive en [com.panamatrip.app.features.trackrecorder.service.LocationTrackingService].
 */
class TrackRecorderEngineTest {

    @Test
    fun `active time advances without new GPS samples`() {
        val engine = newEngine()

        assertEquals(0L, engine.snapshot(1_000L).activeDurationMillis)
        assertEquals(4_000L, engine.snapshot(5_000L).activeDurationMillis)
        assertEquals(0, engine.snapshot(5_000L).pointCount)
    }

    @Test
    fun `paused time is excluded from active time`() {
        val engine = newEngine()

        engine.pause(6_000L)

        assertEquals(5_000L, engine.snapshot(20_000L).activeDurationMillis)
    }

    @Test
    fun `resume continues from the accumulated active time`() {
        val engine = newEngine()

        engine.pause(6_000L)
        engine.resume(20_000L)

        assertEquals(9_000L, engine.snapshot(24_000L).activeDurationMillis)
    }

    @Test
    fun `resume opens a new segment and skips the gap distance`() {
        val engine = newEngine()

        assertTrue(engine.addSample(sample(latitude = 8.9800), 1_000L))
        assertTrue(engine.addSample(sample(latitude = 8.9810), 2_000L))
        engine.pause(3_000L)
        engine.resume(10_000L)
        assertTrue(engine.addSample(sample(latitude = 8.9900), 11_000L))

        assertEquals(listOf(0, 0, 1), engine.recordedPoints().map { it.segmentIndex })
        // Solo cuenta el tramo del primer segmento (~111 m); el salto no suma.
        assertEquals(111.0, engine.snapshot(12_000L).distanceMeters.toDouble(), 2.0)
    }

    @Test
    fun `rejects steps shorter than half the accuracy`() {
        val engine = newEngine()

        assertTrue(engine.addSample(sample(latitude = 8.9800, accuracy = 10f), 1_000L))
        // ~2 m de avance con accuracy de 10 m exige al menos 5 m.
        assertFalse(engine.addSample(sample(latitude = 8.98002, accuracy = 10f), 2_000L))
        assertEquals(1, engine.snapshot(2_000L).pointCount)
    }

    @Test
    fun `rejects samples with poor accuracy`() {
        val engine = newEngine()

        assertFalse(engine.addSample(sample(latitude = 8.9800, accuracy = 80f), 1_000L))
        assertEquals(0, engine.snapshot(1_000L).pointCount)
    }

    @Test
    fun `sequence is fixed at capture and survives pause and finish`() {
        val engine = newEngine()

        assertTrue(engine.addSample(sample(latitude = 8.9800), 1_000L))
        assertTrue(engine.addSample(sample(latitude = 8.9810), 2_000L))
        engine.pause(3_000L)
        engine.resume(10_000L)
        assertTrue(engine.addSample(sample(latitude = 8.9900), 11_000L))

        assertEquals(listOf(0, 1, 2), engine.recordedPoints().map { it.sequence })
        // Persistir dos veces el mismo lote reemplaza las mismas filas.
        assertEquals(listOf(0, 1, 2), engine.finish(12_000L, 1_700_000_100_000L).points.map { it.sequence })
    }

    private fun newEngine() = TrackRecorderEngine(
        trackId = "track-test",
        startedAtEpochMillis = 1_700_000_000_000L,
        startedElapsedMillis = 1_000L,
    )

    private fun sample(latitude: Double, accuracy: Float = 4f) = TrackSample(
        point = GeoPoint(latitude = latitude, longitude = -79.5200),
        elevationMeters = 120.0,
        accuracyMeters = accuracy,
        capturedAtEpochMillis = 1_700_000_000_000L,
    )
}
