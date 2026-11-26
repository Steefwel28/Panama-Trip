package com.panamatrip.app.features.postcomposer.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.PostMediaType
import com.panamatrip.app.domain.photo.ExifCheck
import com.panamatrip.app.domain.photo.ExifMetadata

/** Medio ya copiado al almacenamiento interno y listo para publicar. */
data class ComposerMediaUi(
    val id: String,
    val fileUri: String,
    val mimeType: String,
    val type: PostMediaType,
    val width: Int,
    val height: Int,
    val durationMillis: Long? = null,
    val check: ExifCheck? = null,
    val metadata: ExifMetadata? = null,
    val capturedAtEpochMillis: Long? = null,
)

/** Destino más cercano al GPS actual; nunca se autoselecciona. */
data class NearbyDestinationUi(
    val destination: Destination,
    val distanceMeters: Int,
)

/** Ruta grabada que se puede vincular (máximo una por publicación). */
data class TrackOptionUi(
    val id: String,
    val name: String,
    val distanceMeters: Int,
    val finishedAtEpochMillis: Long,
)

sealed interface PostComposerUiState {

    data object Loading : PostComposerUiState

    data class Content(
        val destinations: List<Destination>,
        val selectedDestination: Destination?,
        val caption: String,
        val media: List<ComposerMediaUi>,
        val searchQuery: String,
        val nearby: NearbyDestinationUi?,
        val tracks: List<TrackOptionUi>,
        val linkedTrackId: String?,
        val isBusy: Boolean,
    ) : PostComposerUiState {
        val canPublish: Boolean
            get() = !isBusy &&
                selectedDestination != null &&
                media.isNotEmpty() &&
                caption.isNotBlank()
    }

    data object Empty : PostComposerUiState

    data class Error(val error: UiError) : PostComposerUiState
}
