package com.panamatrip.app.features.trackrecorder.ui

import com.panamatrip.app.core.ui.UiError
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
)

sealed interface TrackRecorderUiState {

    data object Loading : TrackRecorderUiState

    data class Content(
        val active: ActiveTrackUi?,
        val history: List<RecordedTrackUi>,
    ) : TrackRecorderUiState

    data class Error(val error: UiError) : TrackRecorderUiState
}
