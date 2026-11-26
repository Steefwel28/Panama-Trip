package com.panamatrip.app.domain.model

/** Estado del grabador, independiente de la capa Android que lo alimenta. */
enum class RecorderStatus {
    RECORDING,
    PAUSED,
}

/**
 * Estado persistido de un recorrido. Se distingue de [RecorderStatus] porque
 * aquí interesa saber si el track quedó huérfano y se puede retomar.
 */
enum class TrackStatus {
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    DISCARDED,
}

/** Muestra GPS cruda que entra al motor de grabación. */
data class TrackSample(
    val point: GeoPoint,
    val elevationMeters: Double?,
    val accuracyMeters: Float?,
    val capturedAtEpochMillis: Long,
)

/** Punto ya filtrado y con su tiempo activo acumulado (sin pausas). */
data class TrackPoint(
    val point: GeoPoint,
    val elevationMeters: Double?,
    val accuracyMeters: Float?,
    val recordedAtEpochMillis: Long,
    val activeElapsedMillis: Long,
    /** Tramo del recorrido: cada reanudación tras una pausa abre un segmento nuevo. */
    val segmentIndex: Int = 0,
    /** Índice definitivo asignado al capturar; nunca se recalcula al persistir. */
    val sequence: Int = 0,
)

/** Métricas instantáneas que alimentan la pantalla durante la grabación. */
data class TrackSnapshot(
    val status: RecorderStatus,
    val distanceMeters: Int,
    val activeDurationMillis: Long,
    val averageSpeedKmh: Double,
    val elevationGainMeters: Int,
    val pointCount: Int,
)

/** Recorrido terminado y listo para persistirse como trofeo. */
data class RecordedTrack(
    val id: String,
    val destinationId: String?,
    val destinationName: String?,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val distanceMeters: Int,
    val activeDurationMillis: Long,
    val averageSpeedKmh: Double,
    val elevationGainMeters: Int,
    val pointCount: Int,
    val points: List<TrackPoint> = emptyList(),
    val status: TrackStatus = TrackStatus.COMPLETED,
)

/** Progreso que se persiste por lote mientras el GPS sigue capturando. */
data class TrackProgress(
    val status: TrackStatus,
    val distanceMeters: Int,
    val activeDurationMillis: Long,
    val averageSpeedKmh: Double,
    val elevationGainMeters: Int,
    val pointCount: Int,
    val finishedAtEpochMillis: Long,
)
