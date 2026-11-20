package com.panamatrip.app.domain.logistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TollCatalogTest {

    @Test
    fun `colon corridor is cash only both ways`() {
        val segments = TollCatalog.segmentsFor("Colón")

        assertEquals(1, segments.size)
        val segment = segments.single()
        assertTrue(segment.isCashOnly)
        assertEquals(TollPayment.CASH_ONLY, segment.payment)
        assertEquals(500, TollCatalog.roundTripCents(segments))
        assertEquals(500, TollCatalog.cashOnlyRoundTripCents(segments))
    }

    @Test
    fun `western highway accepts panapass and requires no cash`() {
        val segments = TollCatalog.segmentsFor("Panamá Oeste")

        assertEquals(1, segments.size)
        assertEquals(TollPayment.PANAPASS, segments.single().payment)
        assertEquals(270, TollCatalog.roundTripCents(segments))
        assertEquals(0, TollCatalog.cashOnlyRoundTripCents(segments))
    }

    @Test
    fun `routes without tolls return an empty plan`() {
        assertTrue(TollCatalog.segmentsFor("Panamá").isEmpty())
        assertTrue(TollCatalog.segmentsFor("Darién").isEmpty())
        assertEquals(0, TollCatalog.roundTripCents(emptyList()))
    }

    @Test
    fun `every catalogued toll charges a positive amount`() {
        val provinces = listOf(
            "Colón",
            "Panamá Oeste",
            "Coclé",
            "Veraguas",
            "Chiriquí",
            "Herrera",
            "Los Santos",
        )

        provinces.flatMap(TollCatalog::segmentsFor).forEach { segment ->
            assertTrue("${segment.id} sin tarifa", segment.oneWayCents > 0)
        }
    }
}
