package com.panamatrip.app.domain.model

/** Estado del grabador, independiente de la capa Android que lo alimenta. */
enum class RecorderStatus {
    RECORDING,
    PAUSED,
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
)
