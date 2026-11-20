package com.panamatrip.app.features.guides.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.model.LocalGuide

data class GuideDestinationFilterUi(
    val id: String,
    val name: String,
)

sealed interface GuidesUiState {

    data object Loading : GuidesUiState

    data class Content(
        val guides: List<LocalGuide>,
        val filters: List<GuideDestinationFilterUi>,
        val selectedDestinationId: String?,
        val totalCount: Int,
    ) : GuidesUiState

    data object Empty : GuidesUiState

    data class Error(val error: UiError) : GuidesUiState
}
