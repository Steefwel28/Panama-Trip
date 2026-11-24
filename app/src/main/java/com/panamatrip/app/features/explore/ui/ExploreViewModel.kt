package com.panamatrip.app.features.explore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.datastore.UserPreferences
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.core.ui.UiNotice
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.PostComment
import com.panamatrip.app.domain.model.SavedCollection
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.ranking.DestinationWithDistance
import com.panamatrip.app.domain.ranking.DirectoryFilter
import com.panamatrip.app.domain.ranking.DirectoryFilters
import com.panamatrip.app.domain.ranking.FeedRanker
import com.panamatrip.app.domain.repository.CollectionRepository
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(
    private val destinationRepository: DestinationRepository,
    private val communityRepository: CommunityRepository,
    private val collectionRepository: CollectionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(ExploreTab.COMMUNITY)
    private val filters = MutableStateFlow(DirectoryFilters())
    private val commentPostId = MutableStateFlow<String?>(null)
    private val savePostId = MutableStateFlow<String?>(null)
    private val retryTrigger = MutableStateFlow(0)

    /**
     * Orden congelado del feed mientras el usuario interactúa: dar o quitar
     * like actualiza afinidad y likeCount, pero la tarjeta no debe saltar.
     */
    private val frozenFeedOrder = MutableStateFlow<List<String>?>(null)

    private val noticeChannel = MutableSharedFlow<UiNotice>(
        extraBufferCapacity = NOTICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<UiNotice> = noticeChannel.asSharedFlow()

    private val sheetComments = commentPostId.flatMapLatest { postId ->
        if (postId == null) flowOf(emptyList()) else communityRepository.observeComments(postId)
    }

    private data class Bundle(
        val posts: List<CommunityPost>,
        val destinations: List<Destination>,
        val collections: List<SavedCollection>,
        val preferences: UserPreferences,
        val comments: List<PostComment>,
        val tab: ExploreTab = ExploreTab.COMMUNITY,
        val filters: DirectoryFilters = DirectoryFilters(),
        val commentPostId: String? = null,
        val savePostId: String? = null,
    )

    val state: StateFlow<ExploreUiState> = retryTrigger.flatMapLatest {
        combine(
            communityRepository.observePosts(),
            destinationRepository.observeDestinations(),
            collectionRepository.observeCollections(),
            preferencesRepository.preferences,
            sheetComments,
        ) { posts, destinations, collections, preferences, comments ->
            Bundle(posts, destinations, collections, preferences, comments)
        }
            .combine(selectedTab) { bundle, tab -> bundle.copy(tab = tab) }
            .combine(filters) { bundle, activeFilters -> bundle.copy(filters = activeFilters) }
            .combine(commentPostId) { bundle, id -> bundle.copy(commentPostId = id) }
            .combine(savePostId) { bundle, id -> bundle.copy(savePostId = id) }
            .map(::toUiState)
            .catch { emit(ExploreUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ExploreUiState.Loading,
    )

    init {
        viewModelScope.launch {
            val savedVehicle = preferencesRepository.preferences.first().preferredVehicle
            if (savedVehicle != null) {
                filters.update { current ->
                    if (current.vehicle == null) current.copy(vehicle = savedVehicle) else current
                }
            }
        }
    }

    fun selectTab(tab: ExploreTab) {
        selectedTab.value = tab
    }

    fun toggleVehicleFilter(vehicle: VehicleType) {
        filters.update { it.toggleVehicle(vehicle) }
        viewModelScope.launch {
            val active = filters.value.vehicle
            runCatching { preferencesRepository.setPreferredVehicle(active) }
        }
    }

    fun toggleEcosystemFilter(ecosystem: Ecosystem) {
        filters.update { it.toggleEcosystem(ecosystem) }
    }

    fun toggleDistanceFilter(limitMeters: Int) {
        filters.update { it.toggleDistance(limitMeters) }
    }

    fun clearFilters() {
        filters.value = DirectoryFilters()
        viewModelScope.launch {
            runCatching { preferencesRepository.setPreferredVehicle(null) }
        }
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    fun toggleLike(post: CommunityPost) {
        (state.value as? ExploreUiState.Content)?.let { content ->
            frozenFeedOrder.value = content.posts.map { it.post.id }
        }
        viewModelScope.launch {
            runCatching {
                communityRepository.toggleLike(post.id)
                if (!post.isLiked) preferencesRepository.recordLike(post.destination)
            }.onFailure {
                publishNotice(UiNotice.ACTION_FAILED)
            }
        }
    }

    fun openComments(postId: String) {
        commentPostId.value = postId
    }

    fun closeComments() {
        commentPostId.value = null
    }

    fun submitComment(
        postId: String,
        body: String,
        terrainStatus: TerrainStatus?,
        authorName: String,
    ) {
        if (body.isBlank() && terrainStatus == null) return
        viewModelScope.launch {
            runCatching { communityRepository.addComment(postId, body, terrainStatus, authorName) }
                .onSuccess { publishNotice(UiNotice.COMMENT_PUBLISHED) }
                .onFailure { publishNotice(UiNotice.ACTION_FAILED) }
        }
    }

    fun openSaveSheet(postId: String) {
        savePostId.value = postId
    }

    fun closeSheets() {
        savePostId.value = null
        commentPostId.value = null
    }

    fun toggleSave(collectionId: String, post: CommunityPost) {
        val wasSaved = collectionId in post.savedCollectionIds
        viewModelScope.launch {
            runCatching {
                collectionRepository.togglePostInCollection(collectionId, post.id)
                if (!wasSaved) preferencesRepository.recordSave(post.destination)
            }.onSuccess {
                publishNotice(if (wasSaved) UiNotice.REMOVED else UiNotice.SAVED)
            }.onFailure {
                publishNotice(UiNotice.ACTION_FAILED)
            }
        }
    }

    fun createCollectionAndSave(name: String, post: CommunityPost) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val collectionId = collectionRepository.createCollection(name)
                collectionRepository.togglePostInCollection(collectionId, post.id)
                preferencesRepository.recordSave(post.destination)
            }.onSuccess {
                publishNotice(UiNotice.COLLECTION_CREATED)
            }.onFailure {
                publishNotice(UiNotice.ACTION_FAILED)
            }
        }
    }

    fun rememberRoute(destination: Destination) {
        viewModelScope.launch {
            runCatching { preferencesRepository.setActiveRoute(destination.id) }
        }
    }

    private fun publishNotice(notice: UiNotice) {
        noticeChannel.tryEmit(notice)
    }

    private fun toUiState(bundle: Bundle): ExploreUiState {
        if (bundle.posts.isEmpty() && bundle.destinations.isEmpty()) {
            return ExploreUiState.Empty(bundle.tab)
        }
        val affinity = bundle.preferences.affinity
        val rankedPosts = FeedRanker.rank(bundle.posts, affinity, clock())
        val frozenOrder = frozenFeedOrder.value
        val orderedPosts = if (frozenOrder.isNullOrEmpty()) {
            rankedPosts
        } else {
            val position = frozenOrder.withIndex().associate { (index, id) -> id to index }
            rankedPosts.sortedBy { position[it.id] ?: Int.MAX_VALUE }
        }
        val feedPosts = orderedPosts.map { post ->
            FeedPostUi(post = post, affinity = affinity.normalizedFor(post.destination))
        }
        val destinationsWithDistance = bundle.destinations.map { destination ->
            DestinationWithDistance(
                destination = destination,
                distanceMeters = GeoCalculator.distanceMeters(bundle.preferences.origin, destination.parking),
            )
        }
        val directoryEntries = DirectoryFilter.apply(destinationsWithDistance, bundle.filters)
            .map { entry ->
                DirectoryEntryUi(
                    destination = entry.destination,
                    distanceMeters = entry.distanceMeters,
                    driveMinutes = GeoCalculator.estimatedDriveMinutes(entry.distanceMeters),
                    affinity = affinity.normalizedFor(entry.destination),
                )
            }
        val postsById = bundle.posts.associateBy { it.id }
        val activeCommentPost = bundle.commentPostId?.let(postsById::get)
        val activeSavePost = bundle.savePostId?.let(postsById::get)

        return ExploreUiState.Content(
            tab = bundle.tab,
            posts = feedPosts,
            directory = directoryEntries,
            filters = bundle.filters,
            guestMode = bundle.preferences.guestMode,
            commentSheet = activeCommentPost?.let { post ->
                CommentSheetUi(
                    postId = post.id,
                    destinationName = post.destination.name,
                    comments = bundle.comments,
                )
            },
            saveSheet = activeSavePost?.let { post ->
                SaveSheetUi(
                    post = post,
                    collections = bundle.collections.map { collection ->
                        CollectionOptionUi(
                            id = collection.id,
                            name = collection.name,
                            symbol = collection.symbol,
                            postCount = collection.postIds.size,
                            containsPost = collection.id in post.savedCollectionIds,
                        )
                    },
                )
            },
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val NOTICE_BUFFER = 4

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ExploreViewModel(
                    destinationRepository = container.destinationRepository,
                    communityRepository = container.communityRepository,
                    collectionRepository = container.collectionRepository,
                    preferencesRepository = container.preferencesRepository,
                )
            }
        }
    }
}
