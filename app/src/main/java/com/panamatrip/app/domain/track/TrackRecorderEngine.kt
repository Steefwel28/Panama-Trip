package com.panamatrip.app.domain.track

import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.RecorderStatus
import com.panamatrip.app.domain.model.TrackPoint
import com.panamatrip.app.domain.model.TrackSample
import com.panamatrip.app.domain.model.TrackSnapshot

/**
 * Máquina de estados pura del grabador: recibe muestras GPS y tiempos
 * monotónicos, filtra ruido y entrega métricas listas para la UI y Room.
 * No conoce Android, por lo que se puede probar por completo en JVM.
 */
class TrackRecorderEngine(
    private val trackId: String,
    private val startedAtEpochMillis: Long,
    private val startedElapsedMillis: Long,
    private val destinationId: String? = null,
    private val destinationName: String? = null,
    private val maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS,
    private val minDistanceBetweenPointsMeters: Int = DEFAULT_MIN_POINT_DISTANCE_METERS,
) {

    private val points = mutableListOf<TrackPoint>()
    private var status = RecorderStatus.RECORDING
    private var pausedAtElapsedMillis: Long? = null
    private var accumulatedPauseMillis = 0L
    private var currentSegmentIndex = 0
    private var stepCount = 0

    fun addSample(sample: TrackSample, nowElapsedMillis: Long): Boolean {
        if (status != RecorderStatus.RECORDING) return false
        val accuracy = sample.accuracyMeters
        if (accuracy != null && accuracy > maxAccuracyMeters) return false

        // Solo se compara contra el último punto del segmento activo: el salto
        // entre pausa y reanudación no es un desplazamiento real.
        val previous = points.lastOrNull()?.takeIf { it.segmentIndex == currentSegmentIndex }
        if (previous != null) {
            val step = GeoCalculator.distanceMeters(previous.point, sample.point)
            val minStep = maxOf(
                minDistanceBetweenPointsMeters.toFloat(),
                (accuracy ?: 0f) / 2f,
                MIN_STEP_METERS,
            )
            if (step < minStep) return false
        }

        points += TrackPoint(
            point = sample.point,
            elevationMeters = sample.elevationMeters,
            accuracyMeters = sample.accuracyMeters,
            recordedAtEpochMillis = sample.capturedAtEpochMillis,
            activeElapsedMillis = activeElapsedMillis(nowElapsedMillis),
            segmentIndex = currentSegmentIndex,
            // El índice se fija aquí: es el orden real de captura y sobrevive a
            // reintentos de persistencia sin volver a consultar Room.
            sequence = points.size,
        )
        return true
    }

    fun pause(nowElapsedMillis: Long): Boolean {
        if (status != RecorderStatus.RECORDING) return false
        pausedAtElapsedMillis = nowElapsedMillis
        status = RecorderStatus.PAUSED
        return true
    }

    fun resume(nowElapsedMillis: Long): Boolean {
        val pausedAt = pausedAtElapsedMillis ?: return false
        accumulatedPauseMillis += (nowElapsedMillis - pausedAt).coerceAtLeast(0L)
        pausedAtElapsedMillis = null
        status = RecorderStatus.RECORDING
        currentSegmentIndex += 1
        return true
    }

    fun snapshot(nowElapsedMillis: Long): TrackSnapshot {
        val distance = TrackMetricsCalculator.distanceMeters(points)
        val activeMillis = activeElapsedMillis(nowElapsedMillis)
        return TrackSnapshot(
            status = status,
            distanceMeters = distance,
            activeDurationMillis = activeMillis,
            averageSpeedKmh = TrackMetricsCalculator.averageSpeedKmh(distance, activeMillis),
            elevationGainMeters = ElevationGainCalculator.gainMeters(points),
            pointCount = points.size,
            validWalkingDistanceMeters =
                TrackMetricsCalculator.validWalkingDistanceMeters(points),
            stepCount = stepCount,
        )
    }

    /** Pasos acumulados del podómetro desde el inicio de esta grabación. */
    fun updateStepCount(steps: Int) {
        stepCount = steps.coerceAtLeast(0)
    }

    fun finish(nowElapsedMillis: Long, finishedAtEpochMillis: Long): RecordedTrack {
        val finalSnapshot = snapshot(nowElapsedMillis)
        return RecordedTrack(
            id = trackId,
            destinationId = destinationId,
            destinationName = destinationName,
            startedAtEpochMillis = startedAtEpochMillis,
            finishedAtEpochMillis = finishedAtEpochMillis,
            distanceMeters = finalSnapshot.distanceMeters,
            activeDurationMillis = finalSnapshot.activeDurationMillis,
            averageSpeedKmh = finalSnapshot.averageSpeedKmh,
            elevationGainMeters = finalSnapshot.elevationGainMeters,
            pointCount = finalSnapshot.pointCount,
            points = points.toList(),
            validWalkingDistanceMeters = finalSnapshot.validWalkingDistanceMeters,
            stepCount = finalSnapshot.stepCount,
            evidenceAvailable = true,
        )
    }

    fun recordedPoints(): List<TrackPoint> = points.toList()

    /** Último punto aceptado; evita copiar toda la ruta al persistir un lote. */
    fun lastRecordedPoint(): TrackPoint? = points.lastOrNull()

    private fun activeElapsedMillis(nowElapsedMillis: Long): Long {
        val pausedFor = pausedAtElapsedMillis
            ?.let { pausedAt -> (nowElapsedMillis - pausedAt).coerceAtLeast(0L) }
            ?: 0L
        return (nowElapsedMillis - startedElapsedMillis - accumulatedPauseMillis - pausedFor)
            .coerceAtLeast(0L)
    }

    companion object {
        const val DEFAULT_MAX_ACCURACY_METERS = 50f
        const val DEFAULT_MIN_POINT_DISTANCE_METERS = 2

        /** Paso mínimo absoluto: por debajo de 5 m la deriva GPS domina la señal. */
        const val MIN_STEP_METERS = 5f
    }
}
