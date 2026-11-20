package com.panamatrip.app.domain.logistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodStopAdvisorTest {

    @Test
    fun `early departure on the north corridor suggests a breakfast stop`() {
        val suggestions = FoodStopAdvisor.suggest(
            departureMinuteOfDay = 5 * 60 + 30,
            driveMinutes = 90,
            walkMinutes = 30,
            corridor = FoodCorridor.NORTH,
        )

        assertTrue(suggestions.isNotEmpty())
        assertTrue(
            "esperaba la fonda de Sabanitas: ${suggestions.map { it.stop.id }}",
            suggestions.any { it.stop.id == "fonda-portobolena" },
        )
        assertEquals(RouteLeg.OUTBOUND, suggestions.first().leg)
    }

    @Test
    fun `late night departure produces no food stop`() {
        val suggestions = FoodStopAdvisor.suggest(
            departureMinuteOfDay = 22 * 60,
            driveMinutes = 30,
            walkMinutes = 10,
            corridor = FoodCorridor.CITY,
        )

        assertTrue("no debería sugerir paradas a las 22:00", suggestions.isEmpty())
    }

    @Test
    fun `every suggestion respects the requested corridor`() {
        FoodCorridor.entries.forEach { corridor ->
            (1..12).forEach { hour ->
                val suggestions = FoodStopAdvisor.suggest(
                    departureMinuteOfDay = hour * 60,
                    driveMinutes = 240,
                    walkMinutes = 60,
                    corridor = corridor,
                )

                suggestions.forEach { suggestion ->
                    assertEquals(corridor, suggestion.stop.corridor)
                    assertTrue(suggestion.clockMinuteOfDay in 0 until MINUTES_PER_DAY)
                    assertTrue(suggestion.elapsedMinutes >= 0)
                }
            }
        }
    }

    @Test
    fun `a stop never repeats across outbound and return`() {
        val suggestions = FoodStopAdvisor.suggest(
            departureMinuteOfDay = 9 * 60,
            driveMinutes = 300,
            walkMinutes = 45,
            corridor = FoodCorridor.WEST,
        )

        val ids = suggestions.map { it.stop.id }
        assertEquals(ids.size, ids.toSet().size)
        assertNotEquals(0, ids.size)
    }

    @Test
    fun `suggestions are capped and sorted by elapsed time`() {
        val suggestions = FoodStopAdvisor.suggest(
            departureMinuteOfDay = 10 * 60,
            driveMinutes = 300,
            walkMinutes = 30,
            corridor = FoodCorridor.WEST,
        )

        assertTrue("máximo tres paradas", suggestions.size <= 3)
        assertEquals(
            suggestions.map { it.elapsedMinutes }.sorted(),
            suggestions.map { it.elapsedMinutes },
        )
    }

    private companion object {
        private const val MINUTES_PER_DAY = 24 * 60
    }
}
