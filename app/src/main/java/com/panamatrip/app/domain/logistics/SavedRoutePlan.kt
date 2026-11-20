package com.panamatrip.app.domain.logistics

import com.panamatrip.app.domain.model.VehicleType

/**
 * Plan guardado en Room para consulta sin señal. Es una fotografía de los
 * cálculos al momento de guardar: si cambian las tarifas, el histórico sigue
 * mostrando lo que el viajero presupuestó.
 */
data class SavedRoutePlan(
    val id: String,
    val destinationId: String,
    val destinationName: String,
    val fuelType: FuelType,
    val vehicle: VehicleType,
    val mountainSurcharge: Boolean,
    val departureMinuteOfDay: Int,
    val distanceMeters: Int,
    val driveMinutes: Int,
    val roundTripKilometers: Double,
    val fuelLiters: Double,
    val fuelCostCents: Int,
    val tollCostCents: Int,
    val cashOnlyCents: Int,
    val totalCents: Int,
    val createdAtEpochMillis: Long,
)
