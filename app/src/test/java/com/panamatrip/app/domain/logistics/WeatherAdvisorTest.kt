package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.model.Ecosystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAdvisorTest {

    @Test
    fun `cloud forest in rainy season is misty and warns about layers`() {
        val outlook = WeatherAdvisor.outlookFor(Ecosystem.CLOUD_FOREST, month = 11)

        assertEquals(WeatherCondition.MISTY, outlook.condition)
        assertEquals(75, outlook.rainChancePercent)
        assertTrue(outlook.advice.contains(WeatherAdvice.RAIN_GEAR))
        assertTrue(outlook.advice.contains(WeatherAdvice.LAYERS))
        assertTrue(outlook.advice.size <= 3)
    }

    @Test
    fun `dry season reduces rain chance across every ecosystem`() {
        Ecosystem.entries.forEach { ecosystem ->
            val dry = WeatherAdvisor.outlookFor(ecosystem, month = 2)
            val rainy = WeatherAdvisor.outlookFor(ecosystem, month = 10)

            assertTrue(
                "$ecosystem debería llover más en temporada húmeda",
                rainy.rainChancePercent > dry.rainChancePercent,
            )
        }
    }

    @Test
    fun `lagoon in dry season is clear and recommends insect repellent`() {
        val outlook = WeatherAdvisor.outlookFor(Ecosystem.LAGOON, month = 2)

        assertEquals(WeatherCondition.CLEAR, outlook.condition)
        assertTrue(outlook.advice.contains(WeatherAdvice.INSECT_REPELLENT))
        assertTrue(outlook.maxTemperatureC >= outlook.minTemperatureC)
    }

    @Test
    fun `percentages and temperatures stay in sane ranges`() {
        Ecosystem.entries.forEach { ecosystem ->
            (1..12).forEach { month ->
                val outlook = WeatherAdvisor.outlookFor(ecosystem, month)

                assertTrue(outlook.rainChancePercent in 0..100)
                assertTrue(outlook.minTemperatureC in 10..30)
                assertTrue(outlook.maxTemperatureC in outlook.minTemperatureC..34)
                assertTrue(outlook.advice.isNotEmpty())
            }
        }
    }

    @Test
    fun `months outside the calendar are clamped`() {
        val january = WeatherAdvisor.outlookFor(Ecosystem.MOUNTAIN, month = 1)
        val weird = WeatherAdvisor.outlookFor(Ecosystem.MOUNTAIN, month = 0)

        assertEquals(january, weird)
    }
}
