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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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

    private val noticeChannel = MutableSharedFlow<UiNotice>(
        extraBufferCapacity = NOTICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<UiNotice> = noticeChannel.asSharedFlow()

    val state: StateFlow<TrackRecorderUiState> = retryTrigger.flatMapLatest {
        combine<List<RecordedTrack>, RecorderSession, TrackRecorderUiState>(
            trackRepository.observeTracks(),
            controller.session,
        ) { tracks, session ->
            TrackRecorderUiState.Content(
                active = session.toActiveUi(),
                history = tracks.map { track -> track.toUi() },
            )
        }.catch { emit(TrackRecorderUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TrackRecorderUiState.Loading,
    )

    init {
        viewModelScope.launch {
            controller.events.collect { event ->
                when (event) {
                    RecorderEvent.SAVED -> noticeChannel.tryEmit(UiNotice.TRACK_SAVED)
                    RecorderEvent.DISCARDED -> noticeChannel.tryEmit(UiNotice.TRACK_DISCARDED)
                    RecorderEvent.FAILED -> noticeChannel.tryEmit(UiNotice.ACTION_FAILED)
                    RecorderEvent.PERMISSION_DENIED -> {
                        noticeChannel.tryEmit(UiNotice.LOCATION_DENIED)
                    }
                }
            }
        }
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

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    private fun RecorderSession.toActiveUi(): ActiveTrackUi? = when (this) {
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
