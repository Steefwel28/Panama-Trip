package com.panamatrip.app.domain.photo

import com.panamatrip.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExifLocationVerifierTest {

    private val attraction = GeoPoint(8.6421, -79.9538)

    @Test
    fun `photo captured at the attraction is verified`() {
        val metadata = ExifMetadata(
            latitude = attraction.latitude,
            longitude = attraction.longitude,
            capturedAtEpochMillis = 1_700_000_000_000L,
        )

        val check = ExifLocationVerifier.verify(metadata, attraction)

        assertEquals(ExifVerification.VERIFIED_ON_SITE, check.verification)
        assertEquals(0, check.distanceMeters)
    }

    @Test
    fun `photo inside the five kilometer radius is verified`() {
        // Aproximadamente 2.2 km al norte del atractivo.
        val metadata = ExifMetadata(
            latitude = attraction.latitude + 0.02,
            longitude = attraction.longitude,
            capturedAtEpochMillis = null,
        )

        val check = ExifLocationVerifier.verify(metadata, attraction)

        assertEquals(ExifVerification.VERIFIED_ON_SITE, check.verification)
    }

    @Test
    fun `photo outside the radius is not verified`() {
        // Ciudad de Panamá, a decenas de kilómetros del atractivo.
        val metadata = ExifMetadata(
            latitude = 8.9824,
            longitude = -79.5199,
            capturedAtEpochMillis = null,
        )

        val check = ExifLocationVerifier.verify(metadata, attraction)

        assertEquals(ExifVerification.OUTSIDE_RADIUS, check.verification)
        assertEquals(true, check.distanceMeters != null && check.distanceMeters!! > 5_000)
    }

    @Test
    fun `missing metadata never blocks publishing`() {
        val check = ExifLocationVerifier.verify(null, attraction)

        assertEquals(ExifVerification.NO_GPS, check.verification)
        assertNull(check.distanceMeters)
    }

    @Test
    fun `metadata without gps coordinates is treated as missing`() {
        val metadata = ExifMetadata(latitude = null, longitude = null, capturedAtEpochMillis = null)

        val check = ExifLocationVerifier.verify(metadata, attraction)

        assertEquals(ExifVerification.NO_GPS, check.verification)
    }

    @Test
    fun `metadata with out of range coordinates is treated as missing`() {
        val metadata = ExifMetadata(latitude = 120.0, longitude = -79.9538, capturedAtEpochMillis = null)

        val check = ExifLocationVerifier.verify(metadata, attraction)

        assertEquals(ExifVerification.NO_GPS, check.verification)
    }

    @Test
    fun `custom radius widens the verification area`() {
        val metadata = ExifMetadata(
            latitude = attraction.latitude + 0.02,
            longitude = attraction.longitude,
            capturedAtEpochMillis = null,
        )

        val check = ExifLocationVerifier.verify(metadata, attraction, radiusMeters = 500)

        assertEquals(ExifVerification.OUTSIDE_RADIUS, check.verification)
    }
}
