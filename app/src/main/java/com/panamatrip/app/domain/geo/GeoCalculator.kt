package com.panamatrip.app.domain.geo

import com.panamatrip.app.domain.logistics.PanamanianCorridor
import com.panamatrip.app.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Heurística de proximidad usada por los filtros del directorio. */
enum class DistanceBand {
    NEAR,
    MID,
    FAR;

    companion object {
        const val NEAR_LIMIT_METERS = 15_000
        const val MID_LIMIT_METERS = 60_000

        fun of(meters: Int): DistanceBand = when {
            meters <= NEAR_LIMIT_METERS -> NEAR
            meters <= MID_LIMIT_METERS -> MID
            else -> FAR
        }
    }
}

/** Cálculos geográficos puros, sin dependencias de Android. */
object GeoCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val METERS_PER_DEGREE = 111_320.0

    fun distanceMeters(from: GeoPoint, to: GeoPoint): Int {
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLng = Math.toRadians(to.longitude - from.longitude)
        val originLat = Math.toRadians(from.latitude)
        val targetLat = Math.toRadians(to.latitude)

        val haversine = sin(deltaLat / 2).pow(2) +
            cos(originLat) * cos(targetLat) * sin(deltaLng / 2).pow(2)
        return (2 * EARTH_RADIUS_METERS * asin(sqrt(haversine))).toInt()
    }

    /**
     * Estimación vial gruesa: la distancia en línea recta se corrige por un
     * factor de sinuosidad de carretera y la velocidad media del corredor.
     */
    fun estimatedDriveMinutes(
        distanceMeters: Int,
        corridor: PanamanianCorridor = PanamanianCorridor.METRO,
    ): Int {
        val roadMeters = distanceMeters * ROAD_SINUOSITY
        val metersPerMinute = corridor.averageSpeedKmh * METERS_PER_HOUR / MINUTES_PER_HOUR
        return (roadMeters / metersPerMinute).toInt().coerceAtLeast(1)
    }

    /**
     * Distancia vial aproximada: aplica el mismo factor de sinuosidad que usa
     * la estimación de tiempo, para que el consumo de combustible se calcule
     * sobre kilómetros de carretera y no sobre la línea recta.
     */
    fun roadDistanceMeters(distanceMeters: Int): Int =
        (distanceMeters.coerceAtLeast(0) * ROAD_SINUOSITY).toInt()

    private const val ROAD_SINUOSITY = 1.35
    private const val METERS_PER_HOUR = 1000.0
    private const val MINUTES_PER_HOUR = 60.0

    /** Convertidor de respaldo para entradas manuales de coordenadas. */
    fun fromDegreesMinutes(latitude: String, longitude: String): GeoPoint? = runCatching {
        GeoPoint(latitude.trim().toDouble(), longitude.trim().toDouble())
    }.getOrNull()

    /** Sanity check de coordenadas dentro del istmo panameño ampliado. */
    fun isPlausiblePanamaPoint(point: GeoPoint): Boolean {
        val inLatitude = point.latitude in PANAMA_LATITUDE_RANGE
        val inLongitude = point.longitude in PANAMA_LONGITUDE_RANGE
        return inLatitude && inLongitude
    }

    private val PANAMA_LATITUDE_RANGE = 7.0..9.9
    private val PANAMA_LONGITUDE_RANGE = -83.5..-77.0

    /** Metros por grado, expuesto para pruebas de saneamiento del seed. */
    val metersPerDegree: Double get() = METERS_PER_DEGREE
}
