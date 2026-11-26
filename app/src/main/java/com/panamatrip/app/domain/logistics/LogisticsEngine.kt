package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.VehicleType

/** Entradas seleccionables por el viajero para recalcular el plan al instante. */
data class LogisticsInput(
    val origin: GeoPoint,
    val destination: Destination,
    val vehicle: VehicleType,
    val fuelType: FuelType,
    val mountainSurcharge: Boolean,
    val departureMinuteOfDay: Int,
    val month: Int,
)

/** Plan logístico completo, listo para pintar y para guardar en Room. */
data class LogisticsPlan(
    val oneWayDistanceMeters: Int,
    val oneWayRoadMeters: Int,
    val driveMinutes: Int,
    val roundTripMinutes: Int,
    val fuel: FuelEstimate,
    val tollSegments: List<TollSegment>,
    val tollRoundTripCents: Int,
    val cashOnlyRoundTripCents: Int,
    val weather: WeatherOutlook,
    val foodStops: List<FoodStopSuggestion>,
    val departureMinuteOfDay: Int,
    val arrivalMinuteOfDay: Int,
    val returnMinuteOfDay: Int,
) {
    val cashOnlyTollSegments: List<TollSegment> get() = tollSegments.filter { it.isCashOnly }
    val totalRoundTripCents: Int get() = fuel.costCents + tollRoundTripCents
}

/**
 * Coordina los cuatro motores del bloque: distancia, combustible, peajes,
 * clima y paradas. No conoce Android ni Compose, así que puede probarse en JVM.
 */
object LogisticsEngine {

    private const val MINUTES_PER_DAY = 24 * 60
    private const val METERS_PER_KILOMETER = 1_000.0

    fun plan(input: LogisticsInput): LogisticsPlan {
        val oneWayDistance = GeoCalculator.distanceMeters(input.origin, input.destination.parking)
        val oneWayRoadMeters = GeoCalculator.roadDistanceMeters(oneWayDistance)
        val corridor = CorridorCatalog.of(input.destination.province, input.destination.parking)
        val driveMinutes = GeoCalculator.estimatedDriveMinutes(oneWayDistance, corridor)
        val fuel = FuelCalculator.estimate(
            oneWayRoadMeters = oneWayRoadMeters,
            vehicle = input.vehicle,
            fuelType = input.fuelType,
            mountainSurcharge = input.mountainSurcharge,
        )
        val tollSegments = TollCatalog.segmentsFor(
            province = input.destination.province,
            parking = input.destination.parking,
        )
        val weather = WeatherAdvisor.outlookFor(input.destination.ecosystem, input.month)
        val foodCorridor = FoodStopCatalog.corridorFor(input.destination.province)
        val foodStops = FoodStopAdvisor.suggest(
            departureMinuteOfDay = input.departureMinuteOfDay,
            driveMinutes = driveMinutes,
            walkMinutes = input.destination.walkMinutes,
            corridor = foodCorridor,
            routeKilometers = oneWayRoadMeters / METERS_PER_KILOMETER,
        )

        return LogisticsPlan(
            oneWayDistanceMeters = oneWayDistance,
            oneWayRoadMeters = oneWayRoadMeters,
            driveMinutes = driveMinutes,
            roundTripMinutes = driveMinutes * 2 + input.destination.walkMinutes,
            fuel = fuel,
            tollSegments = tollSegments,
            tollRoundTripCents = TollCatalog.roundTripCents(tollSegments),
            cashOnlyRoundTripCents = TollCatalog.cashOnlyRoundTripCents(tollSegments),
            weather = weather,
            foodStops = foodStops,
            departureMinuteOfDay = input.departureMinuteOfDay,
            arrivalMinuteOfDay = (input.departureMinuteOfDay + driveMinutes) % MINUTES_PER_DAY,
            returnMinuteOfDay = (
                input.departureMinuteOfDay +
                    driveMinutes * 2 +
                    input.destination.walkMinutes
                ) % MINUTES_PER_DAY,
        )
    }

    /**
     * El recargo de montaña se sugiere cuando el destino exige 4x4 o el
     * ecosistema castiga el consumo, pero el viajero siempre puede desactivarlo.
     */
    fun suggestsMountainSurcharge(destination: Destination): Boolean =
        destination.recommendedVehicle == VehicleType.FOUR_BY_FOUR ||
            destination.ecosystem == Ecosystem.MOUNTAIN ||
            destination.ecosystem == Ecosystem.CLOUD_FOREST
}
