package com.panamatrip.app.domain.logistics

import com.panamatrip.app.core.database.seed.SeedCatalog
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogisticsEngineTest {

    @Test
    fun `plan aggregates fuel, tolls, weather and food stop data`() {
        val destination = destinationOf("bahia-portobelo")
        val plan = LogisticsEngine.plan(input(destination))

        assertTrue(plan.oneWayDistanceMeters > 0)
        assertTrue(plan.oneWayRoadMeters >= plan.oneWayDistanceMeters)
        assertTrue(plan.driveMinutes > 0)
        assertEquals(plan.driveMinutes * 2 + destination.walkMinutes, plan.roundTripMinutes)
        assertTrue(plan.fuel.costCents > 0)
        assertEquals(500, plan.tollRoundTripCents)
        assertEquals(500, plan.cashOnlyRoundTripCents)
        assertEquals(1, plan.cashOnlyTollSegments.size)
        assertEquals(plan.fuel.costCents + plan.tollRoundTripCents, plan.totalRoundTripCents)
        assertTrue(plan.weather.rainChancePercent in 0..100)
        assertTrue(plan.weather.advice.isNotEmpty())
    }

    @Test
    fun `western destinations use panapass and need no cash`() {
        val destination = destinationOf("laguna-san-carlos")
        val plan = LogisticsEngine.plan(input(destination, fuelType = FuelType.GASOLINE_91))

        assertEquals(270, plan.tollRoundTripCents)
        assertEquals(0, plan.cashOnlyRoundTripCents)
        assertTrue(plan.cashOnlyTollSegments.isEmpty())
    }

    @Test
    fun `arrival and return follow the departure schedule`() {
        val destination = destinationOf("cerro-azul")
        val departure = 6 * 60 + 15
        val plan = LogisticsEngine.plan(input(destination, departureMinuteOfDay = departure))

        assertEquals(
            (departure + plan.driveMinutes) % MINUTES_PER_DAY,
            plan.arrivalMinuteOfDay,
        )
        assertEquals(
            (departure + plan.driveMinutes * 2 + destination.walkMinutes) % MINUTES_PER_DAY,
            plan.returnMinuteOfDay,
        )
    }

    @Test
    fun `departure after nightfall wraps the clock around midnight`() {
        val destination = destinationOf("bahia-portobelo")
        val departure = 23 * 60 + 30
        val plan = LogisticsEngine.plan(input(destination, departureMinuteOfDay = departure))

        assertTrue(plan.arrivalMinuteOfDay in 0 until MINUTES_PER_DAY)
        assertTrue(plan.returnMinuteOfDay in 0 until MINUTES_PER_DAY)
        assertTrue(plan.arrivalMinuteOfDay < departure)
    }

    @Test
    fun `mountain surcharge is suggested for 4x4 and cloud forest destinations`() {
        assertTrue(LogisticsEngine.suggestsMountainSurcharge(destinationOf("los-picachos-de-ola")))
        assertTrue(LogisticsEngine.suggestsMountainSurcharge(destinationOf("cerro-azul")))
        assertFalse(LogisticsEngine.suggestsMountainSurcharge(destinationOf("bahia-portobelo")))
    }

    @Test
    fun `panama este access charges no tolls`() {
        val plan = LogisticsEngine.plan(input(destinationOf("cerro-azul")))

        assertEquals(0, plan.tollRoundTripCents)
        assertEquals(0, plan.cashOnlyRoundTripCents)
        assertTrue(plan.tollSegments.isEmpty())
    }

    @Test
    fun `surcharge changes the fuel estimate but not the tolls`() {
        val destination = destinationOf("los-picachos-de-ola")
        val base = LogisticsEngine.plan(input(destination, mountainSurcharge = false))
        val surcharged = LogisticsEngine.plan(input(destination, mountainSurcharge = true))

        assertTrue(surcharged.fuel.effectiveLiters > base.fuel.effectiveLiters)
        assertEquals(base.tollRoundTripCents, surcharged.tollRoundTripCents)
    }

    private fun input(
        destination: Destination,
        fuelType: FuelType = FuelType.GASOLINE_95,
        mountainSurcharge: Boolean = false,
        departureMinuteOfDay: Int = 7 * 60,
    ): LogisticsInput = LogisticsInput(
        origin = ORIGIN,
        destination = destination,
        vehicle = VehicleType.SUV,
        fuelType = fuelType,
        mountainSurcharge = mountainSurcharge,
        departureMinuteOfDay = departureMinuteOfDay,
        month = 11,
    )

    private fun destinationOf(id: String): Destination {
        val seed = SeedCatalog.destinations.first { it.id == id }
        return Destination(
            id = seed.id,
            name = seed.name,
            province = seed.province,
            ecosystem = seed.ecosystem,
            summary = seed.summary,
            description = seed.description,
            accessNotes = seed.accessNotes,
            parking = GeoPoint(seed.parkingLatitude, seed.parkingLongitude),
            attraction = GeoPoint(seed.attractionLatitude, seed.attractionLongitude),
            heroImageUrl = seed.heroImageUrl,
            walkDistanceMeters = seed.walkDistanceMeters,
            walkMinutes = seed.walkMinutes,
            elevationGainMeters = seed.elevationGainMeters,
            recommendedVehicle = seed.recommendedVehicle,
            vehicleAccess = emptyMap(),
            averageRating = 0.0,
            reviewCount = 0,
            featuredOrder = seed.featuredOrder,
        )
    }

    private companion object {
        private val ORIGIN = GeoPoint(8.9824, -79.5199)
        private const val MINUTES_PER_DAY = 24 * 60
    }
}
