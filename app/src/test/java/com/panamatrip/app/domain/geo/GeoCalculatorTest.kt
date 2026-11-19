package com.panamatrip.app.domain.geo

import com.panamatrip.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoCalculatorTest {

    @Test
    fun `distance between Panama City and Colon is plausible`() {
        val panamaCity = GeoPoint(8.9824, -79.5199)
        val colon = GeoPoint(9.3592, -79.9014)

        val distance = GeoCalculator.distanceMeters(panamaCity, colon)

        assertTrue("distancia fue $distance", distance in 45_000..70_000)
    }

    @Test
    fun `same point yields zero distance`() {
        val point = GeoPoint(8.6421, -79.9538)
        assertEquals(0, GeoCalculator.distanceMeters(point, point))
    }

    @Test
    fun `drive estimate is never below one minute`() {
        val point = GeoPoint(8.6421, -79.9538)
        assertEquals(1, GeoCalculator.estimatedDriveMinutes(GeoCalculator.distanceMeters(point, point)))
    }

    @Test
    fun `distance bands classify thresholds`() {
        assertEquals(DistanceBand.NEAR, DistanceBand.of(14_000))
        assertEquals(DistanceBand.NEAR, DistanceBand.of(DistanceBand.NEAR_LIMIT_METERS))
        assertEquals(DistanceBand.MID, DistanceBand.of(DistanceBand.NEAR_LIMIT_METERS + 1))
        assertEquals(DistanceBand.FAR, DistanceBand.of(DistanceBand.MID_LIMIT_METERS + 1))
    }

    @Test
    fun `plausibility check rejects points outside the isthmus`() {
        assertTrue(GeoCalculator.isPlausiblePanamaPoint(GeoPoint(8.6421, -79.9538)))
        assertFalse(GeoCalculator.isPlausiblePanamaPoint(GeoPoint(40.7128, -74.0060)))
    }

    @Test
    fun `manual coordinate parsing rejects garbage`() {
        assertEquals(GeoPoint(8.5, -80.1), GeoCalculator.fromDegreesMinutes(" 8.5 ", "-80.1"))
        assertEquals(null, GeoCalculator.fromDegreesMinutes("ocho", "-80.1"))
    }
}
