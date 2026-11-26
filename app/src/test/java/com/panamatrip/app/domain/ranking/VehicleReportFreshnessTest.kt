package com.panamatrip.app.domain.ranking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleReportFreshnessTest {

    private val now = 1_800_000_000_000L

    @Test
    fun `un reporte recien creado es fresco`() {
        assertTrue(VehicleReportFreshness.isFresh(now, now))
    }

    @Test
    fun `a las 48 horas exactas todavia se muestra`() {
        assertTrue(
            VehicleReportFreshness.isFresh(
                now - VehicleReportFreshness.TTL_MILLIS,
                now,
            ),
        )
    }

    @Test
    fun `un milisegundo despues de 48 horas ya expiro`() {
        assertFalse(
            VehicleReportFreshness.isFresh(
                now - VehicleReportFreshness.TTL_MILLIS - 1,
                now,
            ),
        )
    }

    @Test
    fun `reportes futuros o sin fecha no cuentan`() {
        assertFalse(VehicleReportFreshness.isFresh(now + 1, now))
        assertFalse(VehicleReportFreshness.isFresh(0, now))
    }

    @Test
    fun `la expiracion se calcula exactamente a 48 horas`() {
        assertTrue(
            VehicleReportFreshness.expiresAt(now) ==
                now + VehicleReportFreshness.TTL_MILLIS,
        )
    }
}
