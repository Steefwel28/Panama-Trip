package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.model.VehicleType
import kotlin.math.roundToInt

/** Combustibles disponibles en las estaciones panameñas para este bloque. */
enum class FuelType {
    GASOLINE_91,
    GASOLINE_95,
    DIESEL,
}

/**
 * Resultado del cálculo de combustible para el viaje completo. Todos los
 * importes se guardan en centavos de dólar para evitar errores de redondeo.
 */
data class FuelEstimate(
    val fuelType: FuelType,
    val vehicle: VehicleType,
    val roundTripKilometers: Double,
    val effectiveLiters: Double,
    val pricePerGallonCents: Int,
    val costCents: Int,
    val surchargeApplied: Boolean,
) {
    val surchargePercent: Int
        get() = if (surchargeApplied) FuelCalculator.MOUNTAIN_SURCHARGE_PERCENT else 0
}

/**
 * Motor de combustible ida y vuelta. El precio se publica por galón en
 * Panamá, pero el consumo se modela en litros cada 100 km: se convierte en un
 * único punto para que la UI nunca repita la fórmula.
 */
object FuelCalculator {

    const val MOUNTAIN_SURCHARGE_PERCENT = 15

    private const val SURCHARGE_FACTOR = 1.15
    private const val LITERS_PER_GALLON = 3.78541

    // Tarifas referenciales por galón (USD). Se muestran siempre como estimado.
    private const val GASOLINE_91_CENTS_PER_GALLON = 330
    private const val GASOLINE_95_CENTS_PER_GALLON = 355
    private const val DIESEL_CENTS_PER_GALLON = 320

    private const val SEDAN_LITERS_PER_100_KM = 7.8
    private const val SUV_LITERS_PER_100_KM = 10.6
    private const val FOUR_BY_FOUR_LITERS_PER_100_KM = 13.4

    fun pricePerGallonCents(fuelType: FuelType): Int = when (fuelType) {
        FuelType.GASOLINE_91 -> GASOLINE_91_CENTS_PER_GALLON
        FuelType.GASOLINE_95 -> GASOLINE_95_CENTS_PER_GALLON
        FuelType.DIESEL -> DIESEL_CENTS_PER_GALLON
    }

    fun consumptionLitersPer100Km(vehicle: VehicleType): Double = when (vehicle) {
        VehicleType.SEDAN -> SEDAN_LITERS_PER_100_KM
        VehicleType.SUV -> SUV_LITERS_PER_100_KM
        VehicleType.FOUR_BY_FOUR -> FOUR_BY_FOUR_LITERS_PER_100_KM
    }

    /**
     * @param oneWayRoadMeters distancia vial de ida, ya corregida por la
     * sinuosidad de la carretera.
     */
    fun estimate(
        oneWayRoadMeters: Int,
        vehicle: VehicleType,
        fuelType: FuelType,
        mountainSurcharge: Boolean,
    ): FuelEstimate {
        val roundTripMeters = oneWayRoadMeters.coerceAtLeast(0) * ROUND_TRIP_FACTOR
        val roundTripKilometers = roundTripMeters / METERS_PER_KILOMETER
        val baseLiters = roundTripKilometers / 100.0 * consumptionLitersPer100Km(vehicle)
        val factor = if (mountainSurcharge) SURCHARGE_FACTOR else 1.0
        val effectiveLiters = baseLiters * factor
        val gallons = effectiveLiters / LITERS_PER_GALLON
        val priceCents = pricePerGallonCents(fuelType)
        val costCents = (gallons * priceCents).roundToInt()

        return FuelEstimate(
            fuelType = fuelType,
            vehicle = vehicle,
            roundTripKilometers = roundTripKilometers,
            effectiveLiters = effectiveLiters,
            pricePerGallonCents = priceCents,
            costCents = costCents,
            surchargeApplied = mountainSurcharge,
        )
    }

    private const val ROUND_TRIP_FACTOR = 2
    private const val METERS_PER_KILOMETER = 1_000.0
}
