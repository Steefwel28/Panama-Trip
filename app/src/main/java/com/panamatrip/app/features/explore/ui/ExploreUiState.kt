package com.panamatrip.app.features.explore.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.PostComment
import com.panamatrip.app.domain.ranking.DirectoryFilters

enum class ExploreTab {
    COMMUNITY,
    DIRECTORY,
}

data class FeedPostUi(
    val post: CommunityPost,
    val affinity: Float,
)

data class DirectoryEntryUi(
    val destination: Destination,
    val distanceMeters: Int,
    val driveMinutes: Int,
    val affinity: Float,
)

data class CollectionOptionUi(
    val id: String,
    val name: String,
    val symbol: String,
    val postCount: Int,
    val containsPost: Boolean,
)

data class CommentSheetUi(
    val postId: String,
    val destinationName: String,
    val comments: List<PostComment>,
)

data class SaveSheetUi(
    val post: CommunityPost,
    val collections: List<CollectionOptionUi>,
)

sealed interface ExploreUiState {

    data object Loading : ExploreUiState

    data class Content(
        val tab: ExploreTab,
        val posts: List<FeedPostUi>,
        val directory: List<DirectoryEntryUi>,
        val filters: DirectoryFilters,
        val guestMode: Boolean,
        val commentSheet: CommentSheetUi? = null,
        val saveSheet: SaveSheetUi? = null,
    ) : ExploreUiState

    data class Empty(val tab: ExploreTab) : ExploreUiState

    data class Error(val error: UiError) : ExploreUiState
}
