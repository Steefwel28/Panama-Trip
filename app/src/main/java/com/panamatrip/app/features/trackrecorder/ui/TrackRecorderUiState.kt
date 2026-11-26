package com.panamatrip.app.features.trackrecorder.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.RecorderStatus

/** Recorrido guardado que se muestra como trofeo en el historial. */
data class RecordedTrackUi(
    val id: String,
    val destinationName: String?,
    val startedAtEpochMillis: Long,
    val distanceMeters: Int,
    val activeDurationMillis: Long,
    val averageSpeedKmh: Double,
    val elevationGainMeters: Int,
    val validWalkingDistanceMeters: Int = distanceMeters,
    val stepCount: Int = 0,
)

/** Sesión activa mientras el ForegroundService graba en segundo plano. */
data class ActiveTrackUi(
    val status: RecorderStatus,
    val destinationName: String?,
    val startedAtEpochMillis: Long,
    val distanceMeters: Int,
    val activeDurationMillis: Long,
    val averageSpeedKmh: Double,
    val elevationGainMeters: Int,
    val pointCount: Int,
    val validWalkingDistanceMeters: Int = distanceMeters,
    val stepCount: Int = 0,
    val path: List<GeoPoint> = emptyList(),
)

sealed interface TrackRecorderUiState {

    data object Loading : TrackRecorderUiState

    data class Content(
        val active: ActiveTrackUi?,
        val history: List<RecordedTrackUi>,
        /** Track que quedó IN_PROGRESS tras morir el proceso y puede rescatarse. */
        val resumable: RecordedTrackUi? = null,
    ) : TrackRecorderUiState

    data class Error(val error: UiError) : TrackRecorderUiState
}
