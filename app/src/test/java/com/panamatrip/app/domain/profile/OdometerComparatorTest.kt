package com.panamatrip.app.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OdometerComparatorTest {

    @Test
    fun `cada banda empieza exactamente en su limite`() {
        assertEquals(OdometerTier.PRIMER_PASO, OdometerComparator.tierFor(0.0))
        assertEquals(OdometerTier.TROCHA_DE_BARRIO, OdometerComparator.tierFor(10.0))
        assertEquals(OdometerTier.SENDERO_DE_SEMANA, OdometerComparator.tierFor(50.0))
        assertEquals(OdometerTier.GUIA_DE_TROCHA, OdometerComparator.tierFor(150.0))
        assertEquals(OdometerTier.VETERANO_DEL_ISTMO, OdometerComparator.tierFor(500.0))
        assertEquals(OdometerTier.A_PURO_PULMON, OdometerComparator.tierFor(1_000.0))
    }

    @Test
    fun `justo debajo del limite se queda en la banda anterior`() {
        assertEquals(OdometerTier.PRIMER_PASO, OdometerComparator.tierFor(9.999))
        assertEquals(OdometerTier.VETERANO_DEL_ISTMO, OdometerComparator.tierFor(999.999))
    }

    @Test
    fun `metros restantes apuntan a la siguiente banda`() {
        assertEquals(2_500, OdometerComparator.metersToNextTier(47.5))
        assertEquals(0, OdometerComparator.metersToNextTier(1_500.0))
    }

    @Test
    fun `valores negativos caen en la primera banda`() {
        assertTrue(OdometerComparator.tierFor(-10.0) == OdometerTier.PRIMER_PASO)
    }
}
