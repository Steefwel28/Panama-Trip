package com.panamatrip.app.features.postcomposer.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.photo.ExifCheck
import com.panamatrip.app.domain.photo.ExifMetadata

/** Foto ya comprimida y verificada contra el atractivo seleccionado. */
data class SelectedPhotoUi(
    val fileUri: String,
    val metadata: ExifMetadata,
    val check: ExifCheck,
)

sealed interface PostComposerUiState {

    data object Loading : PostComposerUiState

    data class Content(
        val destinations: List<Destination>,
        val selectedDestination: Destination?,
        val caption: String,
        val photo: SelectedPhotoUi?,
        val isBusy: Boolean,
    ) : PostComposerUiState {
        val canPublish: Boolean
            get() = !isBusy &&
                selectedDestination != null &&
                photo != null &&
                caption.isNotBlank()
    }

    data object Empty : PostComposerUiState

    data class Error(val error: UiError) : PostComposerUiState
}
