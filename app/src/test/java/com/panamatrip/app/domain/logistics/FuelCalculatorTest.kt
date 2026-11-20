package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelCalculatorTest {

    @Test
    fun `round trip doubles the road distance`() {
        val estimate = FuelCalculator.estimate(
            oneWayRoadMeters = 10_000,
            vehicle = VehicleType.SUV,
            fuelType = FuelType.GASOLINE_95,
            mountainSurcharge = false,
        )

        assertEquals(20.0, estimate.roundTripKilometers, 0.001)
        assertEquals(2.12, estimate.effectiveLiters, 0.01)
        assertTrue("costo inesperado: ${estimate.costCents}", estimate.costCents in 190..210)
    }

    @Test
    fun `mountain surcharge adds fifteen percent to consumption`() {
        val base = FuelCalculator.estimate(12_000, VehicleType.SUV, FuelType.GASOLINE_91, false)
        val surcharged = FuelCalculator.estimate(12_000, VehicleType.SUV, FuelType.GASOLINE_91, true)

        assertEquals(
            1.15,
            surcharged.effectiveLiters / base.effectiveLiters,
            0.001,
        )
        assertEquals(FuelCalculator.MOUNTAIN_SURCHARGE_PERCENT, surcharged.surchargePercent)
        assertEquals(0, base.surchargePercent)
        assertTrue(surcharged.costCents > base.costCents)
    }

    @Test
    fun `diesel is cheaper than premium gasoline for the same trip`() {
        val diesel = FuelCalculator.estimate(15_000, VehicleType.SUV, FuelType.DIESEL, false)
        val premium = FuelCalculator.estimate(15_000, VehicleType.SUV, FuelType.GASOLINE_95, false)

        assertTrue(diesel.costCents < premium.costCents)
    }

    @Test
    fun `four by four consumes more than a sedan`() {
        val sedan = FuelCalculator.estimate(15_000, VehicleType.SEDAN, FuelType.GASOLINE_95, false)
        val fourByFour = FuelCalculator.estimate(
            15_000,
            VehicleType.FOUR_BY_FOUR,
            FuelType.GASOLINE_95,
            false,
        )

        assertTrue(fourByFour.effectiveLiters > sedan.effectiveLiters)
        assertTrue(fourByFour.costCents > sedan.costCents)
    }

    @Test
    fun `negative distances never produce negative costs`() {
        val estimate = FuelCalculator.estimate(-500, VehicleType.SEDAN, FuelType.DIESEL, false)

        assertEquals(0.0, estimate.roundTripKilometers, 0.001)
        assertEquals(0, estimate.costCents)
    }

    @Test
    fun `zero distance has no fuel cost`() {
        val estimate = FuelCalculator.estimate(0, VehicleType.SEDAN, FuelType.GASOLINE_91, true)

        assertEquals(0, estimate.costCents)
        assertEquals(0.0, estimate.effectiveLiters, 0.001)
    }
}
