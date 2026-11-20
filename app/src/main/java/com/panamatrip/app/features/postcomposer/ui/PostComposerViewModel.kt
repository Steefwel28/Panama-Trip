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
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.NewCommunityPost
import com.panamatrip.app.domain.photo.ExifCheck
import com.panamatrip.app.domain.photo.ExifLocationVerifier
import com.panamatrip.app.domain.photo.ExifMetadata
import com.panamatrip.app.domain.photo.ExifVerification
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import java.time.Instant
import java.time.ZoneId
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

enum class PostComposerEvent {
    PUBLISHED,
}

@OptIn(ExperimentalCoroutinesApi::class)
class PostComposerViewModel(
    private val destinationRepository: DestinationRepository,
    private val communityRepository: CommunityRepository,
    private val preferencesRepository: PreferencesRepository,
    private val photoInspector: PhotoInspector,
    private val authorName: String,
    private val authorRole: String,
    private val authorInitials: String,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val selectedDestinationId = MutableStateFlow<String?>(null)
    private val caption = MutableStateFlow("")
    private val photo = MutableStateFlow<SelectedPhotoUi?>(null)
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
            selectedDestinationId,
            caption,
            photo,
            busy,
        ) { destinations, destinationId, draft, selectedPhoto, isBusy ->
            if (destinations.isEmpty()) {
                PostComposerUiState.Empty
            } else {
                val destination = destinations.firstOrNull { it.id == destinationId }
                    ?: destinations.first()
                PostComposerUiState.Content(
                    destinations = destinations,
                    selectedDestination = destination,
                    caption = draft,
                    photo = selectedPhoto,
                    isBusy = isBusy,
                )
            }
        }.catch { emit(PostComposerUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PostComposerUiState.Loading,
    )

    fun selectDestination(destinationId: String) {
        selectedDestinationId.value = destinationId
        val content = state.value as? PostComposerUiState.Content ?: return
        val destination = content.destinations.firstOrNull { it.id == destinationId } ?: return
        photo.update { current ->
            current?.copy(check = verify(current.metadata, destination))
        }
    }

    fun updateCaption(value: String) {
        caption.value = value.take(MAX_CAPTION_LENGTH)
    }

    fun selectPhoto(uri: Uri) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            runCatching { photoInspector.preparePhoto(uri) }
                .onSuccess { prepared ->
                    val destination = (state.value as? PostComposerUiState.Content)
                        ?.selectedDestination
                    photo.value = SelectedPhotoUi(
                        fileUri = prepared.fileUri,
                        metadata = prepared.metadata,
                        check = verify(prepared.metadata, destination),
                    )
                }
                .onFailure { noticeChannel.tryEmit(UiNotice.ACTION_FAILED) }
            busy.value = false
        }
    }

    fun publish() {
        val content = state.value as? PostComposerUiState.Content ?: return
        val destination = content.selectedDestination ?: return
        val selectedPhoto = content.photo ?: return
        if (content.caption.isBlank() || busy.value) return

        busy.value = true
        viewModelScope.launch {
            val preferredVehicle = runCatching {
                preferencesRepository.preferences.first().preferredVehicle
            }.getOrNull()
            val capturedAt = selectedPhoto.metadata.capturedAtEpochMillis
            val visit = Instant.ofEpochMilli(capturedAt ?: clock())
                .atZone(ZoneId.systemDefault())
            val newPost = NewCommunityPost(
                destinationId = destination.id,
                authorName = authorName,
                authorRole = authorRole,
                authorInitials = authorInitials,
                caption = content.caption,
                imageUrl = selectedPhoto.fileUri,
                visitedYear = visit.year,
                visitedMonth = visit.monthValue,
                vehicleUsed = preferredVehicle ?: destination.recommendedVehicle,
                walkDistanceMeters = destination.walkDistanceMeters,
                walkMinutes = destination.walkMinutes,
                photoVerified = selectedPhoto.check.verification ==
                    ExifVerification.VERIFIED_ON_SITE,
                exifCapturedAtEpochMillis = capturedAt,
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

    private fun verify(metadata: ExifMetadata, destination: Destination?): ExifCheck =
        destination?.let { ExifLocationVerifier.verify(metadata, it.attraction) }
            ?: ExifCheck(ExifVerification.NO_GPS)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val NOTICE_BUFFER = 4
        private const val MAX_CAPTION_LENGTH = 600

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PostComposerViewModel(
                    destinationRepository = container.destinationRepository,
                    communityRepository = container.communityRepository,
                    preferencesRepository = container.preferencesRepository,
                    photoInspector = container.photoInspector,
                    authorName = container.guestAuthorName,
                    authorRole = container.guestAuthorRole,
                    authorInitials = container.guestAuthorInitials,
                )
            }
        }
    }
}
