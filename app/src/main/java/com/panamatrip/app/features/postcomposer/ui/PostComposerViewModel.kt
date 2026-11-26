package com.panamatrip.app.features.postcomposer.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.core.ui.UiNotice
import com.panamatrip.app.data.photo.PhotoInspector
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.NewCommunityPost
import com.panamatrip.app.domain.model.PostMediaItem
import com.panamatrip.app.domain.model.PostMediaType
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.photo.ExifCheck
import com.panamatrip.app.domain.photo.ExifLocationVerifier
import com.panamatrip.app.domain.photo.ExifMetadata
import com.panamatrip.app.domain.photo.ExifVerification
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import com.panamatrip.app.domain.repository.TrackRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PostComposerEvent {
    PUBLISHED,
}

@OptIn(ExperimentalCoroutinesApi::class)
class PostComposerViewModel(
    private val destinationRepository: DestinationRepository,
    private val communityRepository: CommunityRepository,
    private val preferencesRepository: PreferencesRepository,
    private val trackRepository: TrackRepository,
    private val photoInspector: PhotoInspector,
    private val initialTrackId: String? = null,
    private val recentMediaSource: suspend (startMillis: Long, endMillis: Long) -> List<Uri> =
        { _, _ -> emptyList() },
    private val locationSource: suspend () -> GeoPoint? = { null },
    private val authorName: String,
    private val authorRole: String,
    private val authorInitials: String,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val selectedDestinationId = MutableStateFlow<String?>(null)
    private val caption = MutableStateFlow("")
    private val media = MutableStateFlow<List<ComposerMediaUi>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val linkedTrackId = MutableStateFlow<String?>(null)
    private val userLocation = MutableStateFlow<GeoPoint?>(null)
    private val busy = MutableStateFlow(false)
    private val retryTrigger = MutableStateFlow(0)

    private val noticeChannel = MutableSharedFlow<UiNotice>(
        extraBufferCapacity = NOTICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<UiNotice> = noticeChannel.asSharedFlow()

    private val eventChannel = MutableSharedFlow<PostComposerEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<PostComposerEvent> = eventChannel.asSharedFlow()

    val state: StateFlow<PostComposerUiState> = retryTrigger.flatMapLatest {
        combine(
            destinationRepository.observeDestinations(),
            trackRepository.observeTracks(),
            selectedDestinationId,
            caption,
            media,
        ) { destinations, tracks, destinationId, draft, selectedMedia ->
            Bundle(destinations, tracks, destinationId, draft, selectedMedia)
        }
            .combine(searchQuery) { bundle, query -> bundle.copy(query = query) }
            .combine(linkedTrackId) { bundle, trackId -> bundle.copy(trackId = trackId) }
            .combine(userLocation) { bundle, location -> bundle.copy(location = location) }
            .combine(busy) { bundle, isBusy -> bundle.copy(isBusy = isBusy) }
            .map(::toUiState)
            .catch { emit(PostComposerUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PostComposerUiState.Loading,
    )

    init {
        viewModelScope.launch {
            userLocation.value = runCatching { locationSource() }.getOrNull()
        }
        val requestedTrackId = initialTrackId
        if (requestedTrackId != null) {
            viewModelScope.launch {
                val track = runCatching {
                    trackRepository.observeTracks().first()
                        .firstOrNull { it.id == requestedTrackId }
                }.getOrNull()
                linkedTrackId.value = requestedTrackId
                // Sin ventana del track no se adivina: se abre el compositor vacío.
                val startedAt = track?.startedAtEpochMillis ?: return@launch
                val finishedAt = track.finishedAtEpochMillis.takeIf { it > startedAt } ?: clock()
                val captured = runCatching { recentMediaSource(startedAt, finishedAt) }
                    .getOrDefault(emptyList())
                if (captured.isNotEmpty()) addMedia(captured)
            }
        }
    }

    fun selectDestination(destinationId: String) {
        selectedDestinationId.value = destinationId
        val content = state.value as? PostComposerUiState.Content ?: return
        val destination = content.destinations.firstOrNull { it.id == destinationId } ?: return
        media.update { items ->
            items.map { item ->
                val metadata = item.metadata
                if (metadata == null) item else item.copy(check = verify(metadata, destination))
            }
        }
    }

    fun updateCaption(value: String) {
        caption.value = value.take(MAX_CAPTION_LENGTH)
    }

    fun updateSearch(value: String) {
        searchQuery.value = value
    }

    /** Añade fotos y videos; cada uno se copia al almacenamiento interno. */
    fun addMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (!busy.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val destination = (state.value as? PostComposerUiState.Content)
                    ?.selectedDestination
                val prepared = uris.take(MAX_MEDIA_ITEMS).mapNotNull { uri ->
                    runCatching { prepareItem(uri, destination) }
                        .onFailure { noticeChannel.tryEmit(UiNotice.ACTION_FAILED) }
                        .getOrNull()
                }
                media.update { current ->
                    (current + prepared).take(MAX_MEDIA_ITEMS)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                busy.value = false
            }
        }
    }

    fun removeMedia(id: String) {
        media.update { items -> items.filterNot { it.id == id } }
    }

    /** Máximo una ruta: elegir otra reemplaza la anterior; repetir la quita. */
    fun selectTrack(trackId: String) {
        linkedTrackId.value = if (linkedTrackId.value == trackId) null else trackId
    }

    fun publish() {
        val content = state.value as? PostComposerUiState.Content ?: return
        val destination = content.selectedDestination ?: return
        if (!content.canPublish || busy.value) return

        busy.value = true
        viewModelScope.launch {
            val preferredVehicle = runCatching {
                preferencesRepository.preferences.first().preferredVehicle
            }.getOrNull()
            val firstPhoto = content.media.firstOrNull { it.type == PostMediaType.PHOTO }
            val capturedAt = firstPhoto?.capturedAtEpochMillis
            val visit = Instant.ofEpochMilli(capturedAt ?: clock())
                .atZone(ZoneId.systemDefault())
            val newPost = NewCommunityPost(
                destinationId = destination.id,
                authorName = authorName,
                authorRole = authorRole,
                authorInitials = authorInitials,
                caption = content.caption,
                imageUrl = content.media.first().fileUri,
                visitedYear = visit.year,
                visitedMonth = visit.monthValue,
                vehicleUsed = preferredVehicle ?: destination.recommendedVehicle,
                walkDistanceMeters = destination.walkDistanceMeters,
                walkMinutes = destination.walkMinutes,
                photoVerified = firstPhoto?.check?.verification ==
                    ExifVerification.VERIFIED_ON_SITE,
                exifCapturedAtEpochMillis = capturedAt,
                media = content.media.map { item ->
                    PostMediaItem(
                        id = item.id,
                        uri = item.fileUri,
                        mimeType = item.mimeType,
                        type = item.type,
                        width = item.width,
                        height = item.height,
                        durationMillis = item.durationMillis,
                    )
                },
                trackId = content.linkedTrackId,
            )
            runCatching { communityRepository.publishPost(newPost) }
                .onSuccess { eventChannel.tryEmit(PostComposerEvent.PUBLISHED) }
                .onFailure { noticeChannel.tryEmit(UiNotice.ACTION_FAILED) }
            busy.value = false
        }
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    private suspend fun prepareItem(
        uri: Uri,
        destination: Destination?,
    ): ComposerMediaUi {
        val mime = photoInspector.mimeTypeOf(uri)
        return if (mime.startsWith("video/")) {
            val video = photoInspector.prepareVideo(uri)
            ComposerMediaUi(
                id = "media-${UUID.randomUUID()}",
                fileUri = video.fileUri,
                mimeType = video.mimeType,
                type = PostMediaType.VIDEO,
                width = video.width,
                height = video.height,
                durationMillis = video.durationMillis,
                capturedAtEpochMillis = video.capturedAtEpochMillis,
            )
        } else {
            val photo = photoInspector.preparePhoto(uri)
            ComposerMediaUi(
                id = "media-${UUID.randomUUID()}",
                fileUri = photo.fileUri,
                mimeType = mime,
                type = PostMediaType.PHOTO,
                width = photo.width,
                height = photo.height,
                check = verify(
                    metadata = photo.metadata,
                    destination = destination,
                ),
                metadata = photo.metadata,
                capturedAtEpochMillis = photo.metadata.capturedAtEpochMillis,
            )
        }
    }

    private fun verify(metadata: ExifMetadata, destination: Destination?): ExifCheck =
        destination?.let { ExifLocationVerifier.verify(metadata, it.attraction) }
            ?: ExifCheck(ExifVerification.NO_GPS)

    private fun toUiState(bundle: Bundle): PostComposerUiState {
        if (bundle.destinations.isEmpty()) return PostComposerUiState.Empty
        val selected = bundle.destinations.firstOrNull { it.id == bundle.destinationId }
        val query = bundle.query.trim()
        val visibleDestinations = if (query.isEmpty()) {
            bundle.destinations
        } else {
            bundle.destinations.filter { destination ->
                destination.name.contains(query, ignoreCase = true) ||
                    destination.province.contains(query, ignoreCase = true)
            }
        }
        val nearby = bundle.location?.let { point ->
            bundle.destinations
                .minByOrNull { destination ->
                    GeoCalculator.distanceMeters(point, destination.parking)
                }
                ?.let { destination ->
                    NearbyDestinationUi(
                        destination = destination,
                        distanceMeters = GeoCalculator.distanceMeters(point, destination.parking),
                    )
                }
        }
        val tracks = bundle.tracks
            .filter { track -> track.status == TrackStatus.COMPLETED }
            .take(MAX_TRACK_OPTIONS)
            .map { track ->
                TrackOptionUi(
                    id = track.id,
                    name = track.destinationName.orEmpty(),
                    distanceMeters = track.distanceMeters,
                    finishedAtEpochMillis = track.finishedAtEpochMillis,
                )
            }
        return PostComposerUiState.Content(
            destinations = visibleDestinations,
            selectedDestination = selected,
            caption = bundle.caption,
            media = bundle.media,
            searchQuery = bundle.query,
            nearby = nearby,
            tracks = tracks,
            linkedTrackId = bundle.trackId,
            isBusy = bundle.isBusy,
        )
    }

    private data class Bundle(
        val destinations: List<Destination>,
        val tracks: List<com.panamatrip.app.domain.model.RecordedTrack>,
        val destinationId: String?,
        val caption: String,
        val media: List<ComposerMediaUi>,
        val query: String = "",
        val trackId: String? = null,
        val location: GeoPoint? = null,
        val isBusy: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val NOTICE_BUFFER = 4
        private const val MAX_CAPTION_LENGTH = 600
        private const val MAX_MEDIA_ITEMS = 10
        private const val MAX_TRACK_OPTIONS = 20

        fun factory(
            container: AppContainer,
            initialTrackId: String? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PostComposerViewModel(
                    destinationRepository = container.destinationRepository,
                    communityRepository = container.communityRepository,
                    preferencesRepository = container.preferencesRepository,
                    trackRepository = container.trackRepository,
                    photoInspector = container.photoInspector,
                    initialTrackId = initialTrackId,
                    recentMediaSource = { start, end ->
                        container.recentMediaScanner.scan(start, end)
                    },
                    locationSource = { container.locationProvider.lastKnownLocation() },
                    authorName = container.guestAuthorName,
                    authorRole = container.guestAuthorRole,
                    authorInitials = container.guestAuthorInitials,
                )
            }
        }
    }
}
