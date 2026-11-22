package com.panamatrip.app.domain.track

import org.junit.Assert.assertEquals
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

    private fun newEngine() = TrackRecorderEngine(
        trackId = "track-test",
        startedAtEpochMillis = 1_700_000_000_000L,
        startedElapsedMillis = 1_000L,
    )
}
