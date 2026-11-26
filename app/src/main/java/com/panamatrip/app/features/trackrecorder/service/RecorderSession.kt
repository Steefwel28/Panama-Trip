package com.panamatrip.app.features.trackrecorder.service

import com.panamatrip.app.domain.model.RecorderStatus
import com.panamatrip.app.domain.model.TrackSnapshot

/** Contrato observable del servicio, seguro de leer desde cualquier pantalla. */
sealed interface RecorderSession {

    data object Idle : RecorderSession

    data class Active(
        val trackId: String,
        val status: RecorderStatus,
        val snapshot: TrackSnapshot,
        val startedAtEpochMillis: Long,
        val destinationId: String?,
        val destinationName: String?,
    ) : RecorderSession
}

/** Eventos efímeros del servicio hacia la UI. */
sealed interface RecorderEvent {

    /** El track quedó persistido; el id permite enlazarlo en el compositor. */
    data class Saved(val trackId: String) : RecorderEvent

    data object Discarded : RecorderEvent

    data object Failed : RecorderEvent

    data object PermissionDenied : RecorderEvent
}
