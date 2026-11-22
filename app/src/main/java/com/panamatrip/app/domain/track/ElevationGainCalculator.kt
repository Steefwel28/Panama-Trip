package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.model.TrackPoint
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Suma únicamente los desniveles positivos y aplica una histéresis para que el
 * ruido del GPS no infle el trofeo. Una bajada superior al umbral reinicia la
 * base de comparación; las variaciones menores se descartan.
 */
object ElevationGainCalculator {

    const val DEFAULT_NOISE_THRESHOLD_METERS = 1.5

    fun gainMeters(
        elevations: List<Double?>,
        noiseThresholdMeters: Double = DEFAULT_NOISE_THRESHOLD_METERS,
    ): Int {
        require(noiseThresholdMeters >= 0.0) { "El umbral de ruido no puede ser negativo" }
        var baseline: Double? = null
        var gain = 0.0

        elevations.forEach { elevation ->
            if (elevation == null || !elevation.isFinite()) return@forEach
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
                abs(delta) < noiseThresholdMeters -> Unit
            }
        }
        return gain.roundToInt()
    }

    @JvmName("gainMetersForPoints")
    fun gainMeters(
        points: List<TrackPoint>,
        noiseThresholdMeters: Double = DEFAULT_NOISE_THRESHOLD_METERS,
    ): Int = gainMeters(
        elevations = points.map { point -> point.elevationMeters },
        noiseThresholdMeters = noiseThresholdMeters,
    )
}
