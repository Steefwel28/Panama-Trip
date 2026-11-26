package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.TrackPoint

/** Distancia acumulada, tiempo activo y velocidad media sobre datos puros. */
object TrackMetricsCalculator {

    private const val MILLIS_PER_HOUR = 3_600_000.0
    private const val METERS_PER_KILOMETER = 1_000.0

    /**
     * Suma la distancia solo entre puntos consecutivos del MISMO segmento.
     * Una pausa abre un segmento nuevo, así que el salto entre el último punto
     * antes de pausar y el primero al reanudar nunca infla el recorrido.
     */
    fun distanceMeters(points: List<TrackPoint>): Int =
        points.zipWithNext().sumOf { (from, to) ->
            if (from.segmentIndex != to.segmentIndex) {
                0
            } else {
                GeoCalculator.distanceMeters(from.point, to.point)
            }
        }

    /**
     * El tiempo activo es acumulativo y ya excluye las pausas desde el motor,
     * por eso basta con el mayor valor registrado.
     */
    fun activeDurationMillis(points: List<TrackPoint>): Long =
        points.maxOfOrNull { it.activeElapsedMillis }?.coerceAtLeast(0L) ?: 0L

    fun averageSpeedKmh(distanceMeters: Int, activeDurationMillis: Long): Double {
        if (activeDurationMillis <= 0L) return 0.0
        val kilometers = distanceMeters / METERS_PER_KILOMETER
        val hours = activeDurationMillis / MILLIS_PER_HOUR
        return kilometers / hours
    }

    /**
     * Distancia válida para caminata: descarta los tramos cuya velocidad
     * supera [MAX_WALKING_SPEED_KMH] (15 km/h). El tramo se sigue dibujando en
     * el mapa, pero no suma al odómetro ni al pasaporte.
     */
    fun validWalkingDistanceMeters(
        points: List<TrackPoint>,
        maxWalkingSpeedKmh: Double = MAX_WALKING_SPEED_KMH,
    ): Int {
        val maxMetersPerSecond = maxWalkingSpeedKmh / SECONDS_PER_HOUR * METERS_PER_KILOMETER
        return points.zipWithNext().sumOf { (from, to) ->
            if (from.segmentIndex != to.segmentIndex) {
                0
            } else {
                val meters = GeoCalculator.distanceMeters(from.point, to.point)
                val elapsedMillis = to.activeElapsedMillis - from.activeElapsedMillis
                val speed = if (elapsedMillis <= 0L) 0.0 else meters / (elapsedMillis / 1000.0)
                if (speed <= maxMetersPerSecond) meters else 0
            }
        }
    }

    const val MAX_WALKING_SPEED_KMH = 15.0
    private const val SECONDS_PER_HOUR = 3_600.0
}
