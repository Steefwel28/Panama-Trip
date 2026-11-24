package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.model.TrackPoint
import kotlin.jvm.JvmName
import kotlin.math.roundToInt

/**
 * Suma únicamente los desniveles positivos. Antes de comparar aplica una
 * mediana móvil de ventana 5 para matar picos aislados del GPS y exige un
 * umbral de 6 m: variaciones menores son ruido, no cuesta.
 */
object ElevationGainCalculator {

    const val DEFAULT_NOISE_THRESHOLD_METERS = 6.0
    private const val MEDIAN_WINDOW = 5

    fun gainMeters(
        elevations: List<Double?>,
        noiseThresholdMeters: Double = DEFAULT_NOISE_THRESHOLD_METERS,
    ): Int {
        require(noiseThresholdMeters >= 0.0) { "El umbral de ruido no puede ser negativo" }
        val valid = elevations.filter { elevation ->
            elevation != null && elevation.isFinite()
        }.map { elevation -> elevation!! }

        var baseline: Double? = null
        var gain = 0.0

        medianSmooth(valid, MEDIAN_WINDOW).forEach { elevation ->
            val previous = baseline
            if (previous == null) {
                baseline = elevation
                return@forEach
            }
            val delta = elevation - previous
            when {
                delta >= noiseThresholdMeters -> {
                    gain += delta
                    baseline = elevation
                }
                delta <= -noiseThresholdMeters -> baseline = elevation
                else -> Unit
            }
        }
        return gain.roundToInt()
    }

    /**
     * Variante para puntos crudos: no acumula entre segmentos distintos, porque
     * la pausa no debe convertir dos laderas separadas en una sola subida.
     */
    @JvmName("gainMetersForPoints")
    fun gainMeters(
        points: List<TrackPoint>,
        noiseThresholdMeters: Double = DEFAULT_NOISE_THRESHOLD_METERS,
    ): Int {
        if (points.isEmpty()) return 0
        var gain = 0
        val segment = mutableListOf<Double?>()
        var segmentIndex = points.first().segmentIndex

        fun flush() {
            if (segment.isNotEmpty()) {
                gain += gainMeters(segment, noiseThresholdMeters)
                segment.clear()
            }
        }

        points.forEach { point ->
            if (point.segmentIndex != segmentIndex) {
                flush()
                segmentIndex = point.segmentIndex
            }
            segment += point.elevationMeters
        }
        flush()
        return gain
    }

    /** Mediana móvil centrada; con menos de 3 muestras no hay nada que suavizar. */
    private fun medianSmooth(values: List<Double>, window: Int): List<Double> {
        if (values.size < 3 || window < 3) return values
        val half = window / 2
        return values.mapIndexed { index, _ ->
            val from = (index - half).coerceAtLeast(0)
            val to = (index + half).coerceAtMost(values.lastIndex)
            val slice = values.subList(from, to + 1).sorted()
            slice[slice.size / 2]
        }
    }
}
