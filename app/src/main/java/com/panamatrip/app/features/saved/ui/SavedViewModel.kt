package com.panamatrip.app.features.saved.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.core.ui.UiNotice
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.SavedCollection
import com.panamatrip.app.domain.repository.CollectionRepository
import com.panamatrip.app.domain.repository.CommunityRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedCollectionUi(
    val id: String,
    val name: String,
    val symbol: String,
    val postCount: Int,
    val previewImageUrl: String?,
    val selected: Boolean,
)

sealed interface SavedUiState {

    data object Loading : SavedUiState

    data class Content(
        val collections: List<SavedCollectionUi>,
        val posts: List<CommunityPost>,
        val selectedCollectionId: String?,
    ) : SavedUiState {
        val hasSavedPosts: Boolean get() = posts.isNotEmpty()
    }

    data object Empty : SavedUiState

    data class Error(val error: UiError) : SavedUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModel(
    private val collectionRepository: CollectionRepository,
    private val communityRepository: CommunityRepository,
) : ViewModel() {

    private val selectedCollectionId = MutableStateFlow<String?>(null)
    private val retryTrigger = MutableStateFlow(0)
    private val noticeChannel = MutableSharedFlow<UiNotice>(
        extraBufferCapacity = NOTICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<UiNotice> = noticeChannel.asSharedFlow()

    val state: StateFlow<SavedUiState> = retryTrigger.flatMapLatest {
        combine(
            collectionRepository.observeCollections(),
            communityRepository.observePosts(),
            selectedCollectionId,
        ) { collections, posts, selectedId ->
            buildState(collections, posts, selectedId)
        }.catch { emit(SavedUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SavedUiState.Loading,
    )

    fun selectCollection(collectionId: String?) {
        selectedCollectionId.update { current ->
            if (collectionId == null || current == collectionId) null else collectionId
        }
    }

    fun unsave(post: CommunityPost, collectionId: String) {
        viewModelScope.launch {
            runCatching { collectionRepository.togglePostInCollection(collectionId, post.id) }
                .onSuccess { noticeChannel.tryEmit(UiNotice.REMOVED) }
                .onFailure { noticeChannel.tryEmit(UiNotice.ACTION_FAILED) }
        }
    }

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { collectionRepository.createCollection(name) }
                .onSuccess { noticeChannel.tryEmit(UiNotice.COLLECTION_CREATED) }
                .onFailure { noticeChannel.tryEmit(UiNotice.ACTION_FAILED) }
        }
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    private fun buildState(
        collections: List<SavedCollection>,
        posts: List<CommunityPost>,
        selectedId: String?,
    ): SavedUiState {
        val savedPosts = posts.filter { it.savedCollectionIds.isNotEmpty() }
        if (savedPosts.isEmpty() && collections.isEmpty()) return SavedUiState.Empty

        val visiblePosts = if (selectedId == null) {
            savedPosts
        } else {
            savedPosts.filter { selectedId in it.savedCollectionIds }
        }
        return SavedUiState.Content(
            collections = collections.map { collection ->
                SavedCollectionUi(
                    id = collection.id,
                    name = collection.name,
                    symbol = collection.symbol,
                    postCount = collection.postIds.size,
                    previewImageUrl = collection.previewImageUrl,
                    selected = collection.id == selectedId,
                )
            },
            posts = visiblePosts,
            selectedCollectionId = selectedId,
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val NOTICE_BUFFER = 4

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SavedViewModel(
                    collectionRepository = container.collectionRepository,
                    communityRepository = container.communityRepository,
                )
            }
        }
    }
}
