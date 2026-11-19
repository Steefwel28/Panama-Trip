package com.panamatrip.app.domain.model

/**
 * Destino natural con doble coordenada: el estacionamiento (acceso vehicular)
 * y el atractivo (cerro, cascada, laguna).
 */
data class Destination(
    val id: String,
    val name: String,
    val province: String,
    val ecosystem: Ecosystem,
    val summary: String,
    val description: String,
    val accessNotes: String,
    val parking: GeoPoint,
    val attraction: GeoPoint,
    val heroImageUrl: String,
    val walkDistanceMeters: Int,
    val walkMinutes: Int,
    val elevationGainMeters: Int,
    val recommendedVehicle: VehicleType,
    val vehicleAccess: Map<VehicleType, VehicleAccessSummary>,
    val averageRating: Double,
    val reviewCount: Int,
    val featuredOrder: Int,
)

/** Semáforo comunitario agregado para un tipo de vehículo. */
data class VehicleAccessSummary(
    val vehicle: VehicleType,
    val status: AccessStatus,
    val reports: Int,
) {
    val hasCommunitySignal: Boolean get() = reports > 0
}
