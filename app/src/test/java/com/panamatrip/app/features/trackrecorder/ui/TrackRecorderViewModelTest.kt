package com.panamatrip.app.features.trackrecorder.ui

import androidx.lifecycle.SavedStateHandle
import com.panamatrip.app.core.testing.MainDispatcherRule
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.RecorderStatus
import com.panamatrip.app.domain.model.TrackPoint
import com.panamatrip.app.domain.model.TrackProgress
import com.panamatrip.app.domain.model.TrackSnapshot
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.TrackRepository
import com.panamatrip.app.features.trackrecorder.data.TrackRecorderController
import com.panamatrip.app.features.trackrecorder.service.RecorderEvent
import com.panamatrip.app.features.trackrecorder.service.RecorderSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackRecorderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `el track activo nunca se ofrece como recuperable`() = runTest {
        val active = track(id = "track-active", status = TrackStatus.IN_PROGRESS)
        val viewModel = createViewModel(
            controller = FakeTrackRecorderController(activeSession(active.id)),
            repository = FakeTrackRepository(resumable = listOf(active)),
        )
        startCollecting(viewModel)
        advanceUntilIdle()

        val content = viewModel.state.value as TrackRecorderUiState.Content
        assertNull(content.resumable)
    }

    @Test
    fun `al volver a Idle recarga huerfanos aunque el track activo haya desaparecido`() = runTest {
        val repository = FakeTrackRepository()
        val controller = FakeTrackRecorderController(activeSession("track-active"))
        val viewModel = createViewModel(controller, repository)
        startCollecting(viewModel)
        advanceUntilIdle()

        assertNull((viewModel.state.value as TrackRecorderUiState.Content).resumable)

        repository.resumable = listOf(track(id = "track-orphan", status = TrackStatus.IN_PROGRESS))
        controller.session.value = RecorderSession.Idle
        advanceUntilIdle()

        val content = viewModel.state.value as TrackRecorderUiState.Content
        assertEquals("track-orphan", content.resumable?.id)
    }

    @Test
    fun `finalizar y reiniciar rapido no expone el track anterior`() = runTest {
        val previous = track(id = "track-previous", status = TrackStatus.IN_PROGRESS)
        val repository = FakeTrackRepository(resumable = listOf(previous))
        val controller = FakeTrackRecorderController(activeSession(previous.id))
        val viewModel = createViewModel(controller, repository)
        startCollecting(viewModel)
        advanceUntilIdle()

        assertNull((viewModel.state.value as TrackRecorderUiState.Content).resumable)

        // El servicio cierra el anterior y arranca otro antes de que la UI
        // alcance a recomponer: la tarjeta de recuperación no debe aparecer.
        repository.resumable = emptyList()
        controller.session.value = RecorderSession.Idle
        controller.session.value = activeSession("track-next")
        advanceUntilIdle()

        val content = viewModel.state.value as TrackRecorderUiState.Content
        assertEquals(RecorderStatus.RECORDING, content.active?.status)
        assertNull(content.resumable)
    }

    private fun TestScope.startCollecting(viewModel: TrackRecorderViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
    }

    private fun createViewModel(
        controller: TrackRecorderController,
        repository: TrackRepository,
    ): TrackRecorderViewModel = TrackRecorderViewModel(
        savedStateHandle = SavedStateHandle(),
        controller = controller,
        trackRepository = repository,
        destinationRepository = FakeDestinationRepository(),
    )

    private fun activeSession(trackId: String): RecorderSession.Active = RecorderSession.Active(
        trackId = trackId,
        status = RecorderStatus.RECORDING,
        snapshot = TrackSnapshot(
            status = RecorderStatus.RECORDING,
            distanceMeters = 0,
            activeDurationMillis = 0L,
            averageSpeedKmh = 0.0,
            elevationGainMeters = 0,
            pointCount = 0,
        ),
        startedAtEpochMillis = 1_700_000_000_000L,
        destinationId = null,
        destinationName = null,
    )

    private fun track(id: String, status: TrackStatus): RecordedTrack = RecordedTrack(
        id = id,
        destinationId = null,
        destinationName = null,
        startedAtEpochMillis = 1_700_000_000_000L,
        finishedAtEpochMillis = 0L,
        distanceMeters = 0,
        activeDurationMillis = 0L,
        averageSpeedKmh = 0.0,
        elevationGainMeters = 0,
        pointCount = 0,
        status = status,
    )

    private class FakeTrackRecorderController(
        initial: RecorderSession,
    ) : TrackRecorderController {
        override val session = MutableStateFlow(initial)
        override val events = MutableSharedFlow<RecorderEvent>(extraBufferCapacity = 4)
        override val path = MutableStateFlow<List<com.panamatrip.app.domain.model.GeoPoint>>(emptyList())

        override fun start(destinationId: String?, destinationName: String?) = Unit

        override fun pause() = Unit

        override fun resume() = Unit

        override fun finish() = Unit

        override fun discard() = Unit
    }

    private class FakeTrackRepository(
        var resumable: List<RecordedTrack> = emptyList(),
    ) : TrackRepository {
        override fun observeTracks(): Flow<List<RecordedTrack>> = flowOf(emptyList())

        override fun observeTrackPoints(trackId: String): Flow<List<TrackPoint>> =
            flowOf(emptyList())

        override suspend fun save(track: RecordedTrack) = Unit

        override suspend fun startTrack(track: RecordedTrack) = Unit

        override suspend fun persistBatch(
            trackId: String,
            points: List<TrackPoint>,
            progress: TrackProgress,
        ) = Unit

        override suspend fun updateProgress(trackId: String, progress: TrackProgress) = Unit

        override suspend fun findResumable(): List<RecordedTrack> = resumable

        override suspend fun discard(trackId: String) = Unit
    }

    private class FakeDestinationRepository : DestinationRepository {
        override fun observeDestinations(): Flow<List<Destination>> = flowOf(emptyList())

        override fun observeDestination(id: String): Flow<Destination?> = flowOf(null)

        override fun observeReviews(destinationId: String): Flow<List<DestinationReview>> =
            flowOf(emptyList())
    }
}
