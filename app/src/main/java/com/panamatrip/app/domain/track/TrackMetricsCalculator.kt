package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.TrackPoint

/** Distancia acumulada, tiempo activo y velocidad media sobre datos puros. */
object TrackMetricsCalculator {

    private const val MILLIS_PER_HOUR = 3_600_000.0
    private const val METERS_PER_KILOMETER = 1_000.0

    fun distanceMeters(points: List<TrackPoint>): Int =
        points.zipWithNext().sumOf { (from, to) ->
            GeoCalculator.distanceMeters(from.point, to.point)
        }

    fun activeDurationMillis(points: List<TrackPoint>): Long =
        points.maxOfOrNull { it.activeElapsedMillis }?.coerceAtLeast(0L) ?: 0L

    fun averageSpeedKmh(distanceMeters: Int, activeDurationMillis: Long): Double {
        if (activeDurationMillis <= 0L) return 0.0
        val kilometers = distanceMeters / METERS_PER_KILOMETER
        val hours = activeDurationMillis / MILLIS_PER_HOUR
        return kilometers / hours
    }
}
