package com.panamatrip.app.features.trackrecorder.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.core.ui.UiNotice
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.TrackProgress
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.TrackRepository
import com.panamatrip.app.features.trackrecorder.data.TrackRecorderController
import com.panamatrip.app.features.trackrecorder.service.RecorderEvent
import com.panamatrip.app.features.trackrecorder.service.RecorderSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TrackRecorderViewModel(
    savedStateHandle: SavedStateHandle,
    private val controller: TrackRecorderController,
    private val trackRepository: TrackRepository,
    private val destinationRepository: DestinationRepository,
) : ViewModel() {

    private val destinationId: String? =
        savedStateHandle.get<String>(ARG_DESTINATION_ID)?.takeIf { it.isNotBlank() }
    private val retryTrigger = MutableStateFlow(0)
    private val resumableTrack = MutableStateFlow<RecordedTrack?>(null)
    private val _savedTrackId = MutableStateFlow<String?>(null)

    /** Track recién cerrado, para abrir el compositor con la ruta enlazada. */
    val savedTrackId: StateFlow<String?> = _savedTrackId.asStateFlow()

    private val noticeChannel = MutableSharedFlow<UiNotice>(
        extraBufferCapacity = NOTICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<UiNotice> = noticeChannel.asSharedFlow()

    val state: StateFlow<TrackRecorderUiState> = retryTrigger.flatMapLatest {
        combine<List<RecordedTrack>, RecorderSession, RecordedTrack?, List<GeoPoint>, TrackRecorderUiState>(
            trackRepository.observeTracks(),
            controller.session,
            resumableTrack,
            controller.path,
        ) { tracks, session, resumable, path ->
            TrackRecorderUiState.Content(
                active = session.toActiveUi(path),
                history = tracks
                    .filter { track -> track.status == TrackStatus.COMPLETED }
                    .map { track -> track.toUi() },
                resumable = resumable
                    ?.takeIf { session is RecorderSession.Idle }
                    ?.toUi(),
            )
        }.catch { emit(TrackRecorderUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TrackRecorderUiState.Loading,
    )

    init {
        // Observa cada transición de sesión: al volver a Idle recarga huérfanos
        // y nunca ofrece como recuperable el track que el servicio está grabando.
        viewModelScope.launch {
            controller.session
                .map { session -> (session as? RecorderSession.Active)?.trackId }
                .distinctUntilChanged()
                .collect { activeTrackId -> reloadResumable(activeTrackId) }
        }
        viewModelScope.launch {
            controller.events.collect { event ->
                when (event) {
                    is RecorderEvent.Saved -> {
                        noticeChannel.tryEmit(UiNotice.TRACK_SAVED)
                        _savedTrackId.value = event.trackId
                    }
                    RecorderEvent.Discarded -> noticeChannel.tryEmit(UiNotice.TRACK_DISCARDED)
                    RecorderEvent.Failed -> noticeChannel.tryEmit(UiNotice.ACTION_FAILED)
                    RecorderEvent.PermissionDenied -> {
                        noticeChannel.tryEmit(UiNotice.LOCATION_DENIED)
                    }
                }
            }
        }
    }

    private suspend fun reloadResumable(activeTrackId: String?) {
        resumableTrack.value = runCatching {
            trackRepository.findResumable()
                .firstOrNull { track -> track.id != activeTrackId }
        }.getOrNull()
    }

    fun start() {
        viewModelScope.launch {
            val destinationName = destinationId?.let { id ->
                runCatching { destinationRepository.observeDestination(id).first()?.name }
                    .getOrNull()
            }
            controller.start(destinationId, destinationName)
        }
    }

    fun pause() = controller.pause()

    fun resume() = controller.resume()

    fun finish() = controller.finish()

    fun discard() = controller.discard()

    /** Cierra un track huérfano con las métricas que alcanzaron a persistirse. */
    fun keepRecovered() {
        val track = resumableTrack.value ?: return
        viewModelScope.launch {
            val result = runCatching {
                trackRepository.updateProgress(
                    trackId = track.id,
                    progress = TrackProgress(
                        status = TrackStatus.COMPLETED,
                        distanceMeters = track.distanceMeters,
                        activeDurationMillis = track.activeDurationMillis,
                        averageSpeedKmh = track.averageSpeedKmh,
                        elevationGainMeters = track.elevationGainMeters,
                        pointCount = track.pointCount,
                        finishedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
            if (result.isSuccess) {
                resumableTrack.value = null
                noticeChannel.tryEmit(UiNotice.TRACK_SAVED)
            } else {
                noticeChannel.tryEmit(UiNotice.ACTION_FAILED)
            }
        }
    }

    fun discardRecovered() {
        val track = resumableTrack.value ?: return
        viewModelScope.launch {
            val result = runCatching { trackRepository.discard(track.id) }
            if (result.isSuccess) {
                resumableTrack.value = null
                noticeChannel.tryEmit(UiNotice.TRACK_DISCARDED)
            } else {
                noticeChannel.tryEmit(UiNotice.ACTION_FAILED)
            }
        }
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    /** Marca el track ya enlazado para no reabrir el compositor en bucle. */
    fun consumeSavedTrack() {
        _savedTrackId.value = null
    }

    private fun RecorderSession.toActiveUi(path: List<GeoPoint>): ActiveTrackUi? = when (this) {
        RecorderSession.Idle -> null
        is RecorderSession.Active -> ActiveTrackUi(
            status = status,
            destinationName = destinationName,
            startedAtEpochMillis = startedAtEpochMillis,
            distanceMeters = snapshot.distanceMeters,
            activeDurationMillis = snapshot.activeDurationMillis,
            averageSpeedKmh = snapshot.averageSpeedKmh,
            elevationGainMeters = snapshot.elevationGainMeters,
            pointCount = snapshot.pointCount,
            validWalkingDistanceMeters = snapshot.validWalkingDistanceMeters,
            stepCount = snapshot.stepCount,
            path = path,
        )
    }

    private fun RecordedTrack.toUi(): RecordedTrackUi = RecordedTrackUi(
        id = id,
        destinationName = destinationName,
        startedAtEpochMillis = startedAtEpochMillis,
        distanceMeters = distanceMeters,
        activeDurationMillis = activeDurationMillis,
        averageSpeedKmh = averageSpeedKmh,
        elevationGainMeters = elevationGainMeters,
        validWalkingDistanceMeters = validWalkingDistanceMeters,
        stepCount = stepCount,
    )

    companion object {
        const val ARG_DESTINATION_ID = "destinationId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val NOTICE_BUFFER = 4

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TrackRecorderViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    controller = container.trackRecorderController,
                    trackRepository = container.trackRepository,
                    destinationRepository = container.destinationRepository,
                )
            }
        }
    }
}
