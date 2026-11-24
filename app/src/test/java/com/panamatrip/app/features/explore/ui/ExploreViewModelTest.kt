package com.panamatrip.app.features.explore.ui

import com.panamatrip.app.core.datastore.UserPreferences
import com.panamatrip.app.core.testing.MainDispatcherRule
import com.panamatrip.app.core.ui.UiNotice
import com.panamatrip.app.domain.affinity.AffinitySnapshot
import com.panamatrip.app.domain.affinity.AffinityWeights
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.NewCommunityPost
import com.panamatrip.app.domain.model.PostComment
import com.panamatrip.app.domain.model.SavedCollection
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.repository.CollectionRepository
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = 1_800_000_000_000L

    @Test
    fun `toggleLike actualiza el contador sin alterar el orden del feed`() = runTest {
        val viewModel = createViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        val before = viewModel.state.value as ExploreUiState.Content
        val orderBefore = before.posts.map { it.post.id }
        val target = before.posts.last().post

        viewModel.toggleLike(target)
        advanceUntilIdle()

        val after = viewModel.state.value as ExploreUiState.Content
        assertEquals(orderBefore, after.posts.map { it.post.id })

        val updated = after.posts.first { it.post.id == target.id }.post
        assertTrue(updated.isLiked)
        assertEquals(target.likeCount + 1, updated.likeCount)
    }

    @Test
    fun `toggleLike no emite aviso de like`() = runTest {
        val viewModel = createViewModel()
        startCollecting(viewModel)
        val notices = mutableListOf<UiNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notices.collect { notices += it }
        }
        advanceUntilIdle()

        val content = viewModel.state.value as ExploreUiState.Content
        viewModel.toggleLike(content.posts.first().post)
        advanceUntilIdle()

        assertTrue(notices.isEmpty())
    }

    @Test
    fun `quitar like mantiene el orden congelado y baja el contador`() = runTest {
        val viewModel = createViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        val first = viewModel.state.value as ExploreUiState.Content
        val orderBefore = first.posts.map { it.post.id }
        val target = first.posts.first().post

        viewModel.toggleLike(target)
        advanceUntilIdle()
        viewModel.toggleLike(target)
        advanceUntilIdle()

        val after = viewModel.state.value as ExploreUiState.Content
        assertEquals(orderBefore, after.posts.map { it.post.id })
        val updated = after.posts.first { it.post.id == target.id }.post
        assertFalse(updated.isLiked)
        assertEquals(target.likeCount, updated.likeCount)
    }

    private fun TestScope.startCollecting(viewModel: ExploreViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
    }

    private fun createViewModel(): ExploreViewModel {
        val oldest = destination(id = "oldest", ecosystem = Ecosystem.MOUNTAIN)
        val newest = destination(id = "newest", ecosystem = Ecosystem.WATERFALL)
        val posts = listOf(
            post(
                id = "oldest-post",
                destination = oldest,
                createdAt = now - TEN_DAYS_MILLIS,
            ),
            post(
                id = "newest-post",
                destination = newest,
                createdAt = now,
            ),
        )
        return ExploreViewModel(
            destinationRepository = FakeDestinationRepository(listOf(oldest, newest)),
            communityRepository = FakeCommunityRepository(posts),
            collectionRepository = FakeCollectionRepository(),
            preferencesRepository = FakePreferencesRepository(),
            clock = { now },
        )
    }

    private fun destination(id: String, ecosystem: Ecosystem): Destination = Destination(
        id = id,
        name = "Destino $id",
        province = "Chiriquí",
        ecosystem = ecosystem,
        summary = "Resumen",
        description = "Descripción",
        accessNotes = "Notas",
        parking = GeoPoint(latitude = 8.5, longitude = -82.5),
        attraction = GeoPoint(latitude = 8.51, longitude = -82.51),
        heroImageUrl = "",
        walkDistanceMeters = 1200,
        walkMinutes = 35,
        elevationGainMeters = 300,
        recommendedVehicle = VehicleType.SUV,
        vehicleAccess = emptyMap(),
        averageRating = 4.5,
        reviewCount = 12,
        featuredOrder = 0,
    )

    private fun post(
        id: String,
        destination: Destination,
        createdAt: Long,
        likeCount: Int = 0,
    ): CommunityPost = CommunityPost(
        id = id,
        destination = destination,
        authorName = "Ana",
        authorRole = "Guía local",
        authorInitials = "AN",
        caption = "Ruta verificada",
        imageUrl = "",
        visitedYear = 2026,
        visitedMonth = 1,
        vehicleUsed = VehicleType.SUV,
        walkDistanceMeters = 1200,
        walkMinutes = 35,
        activityTag = null,
        likeCount = likeCount,
        isLiked = false,
        commentCount = 0,
        savedCollectionIds = emptySet(),
        createdAt = createdAt,
    )

    private class FakeDestinationRepository(
        destinations: List<Destination>,
    ) : DestinationRepository {
        private val state = MutableStateFlow(destinations)

        override fun observeDestinations(): Flow<List<Destination>> = state

        override fun observeDestination(id: String): Flow<Destination?> =
            state.map { list -> list.firstOrNull { it.id == id } }

        override fun observeReviews(destinationId: String): Flow<List<DestinationReview>> =
            flowOf(emptyList())
    }

    private class FakeCommunityRepository(
        initial: List<CommunityPost>,
    ) : CommunityRepository {
        private val state = MutableStateFlow(initial)

        override fun observePosts(): Flow<List<CommunityPost>> = state

        override fun observeComments(postId: String): Flow<List<PostComment>> = flowOf(emptyList())

        override suspend fun toggleLike(postId: String) {
            state.update { posts ->
                posts.map { post ->
                    if (post.id != postId) {
                        post
                    } else {
                        post.copy(
                            isLiked = !post.isLiked,
                            likeCount = post.likeCount + if (post.isLiked) -1 else 1,
                        )
                    }
                }
            }
        }

        override suspend fun addComment(
            postId: String,
            body: String,
            terrainStatus: TerrainStatus?,
            authorName: String,
        ) = Unit

        override suspend fun publishPost(post: NewCommunityPost) = Unit
    }

    private class FakeCollectionRepository : CollectionRepository {
        private val state = MutableStateFlow(emptyList<SavedCollection>())

        override fun observeCollections(): Flow<List<SavedCollection>> = state

        override fun observeSavedPostIds(): Flow<Set<String>> = flowOf(emptySet())

        override suspend fun togglePostInCollection(collectionId: String, postId: String) = Unit

        override suspend fun createCollection(name: String): String = "collection"
    }

    private class FakePreferencesRepository : PreferencesRepository {
        private val state = MutableStateFlow(UserPreferences())

        override val preferences: Flow<UserPreferences> = state

        override val affinity: Flow<AffinitySnapshot> = state.map { it.affinity }

        override suspend fun setPreferredVehicle(vehicle: VehicleType?) {
            state.update { it.copy(preferredVehicle = vehicle) }
        }

        override suspend fun setActiveRoute(destinationId: String?) {
            state.update { it.copy(activeRouteDestinationId = destinationId) }
        }

        override suspend fun recordLike(destination: Destination) {
            record(destination, AffinityWeights.LIKE)
        }

        override suspend fun recordSave(destination: Destination) {
            record(destination, AffinityWeights.SAVE)
        }

        override suspend fun recordView(destination: Destination) {
            record(destination, AffinityWeights.VIEW)
        }

        private fun record(destination: Destination, weight: Double) {
            state.update { preferences ->
                preferences.copy(
                    affinity = preferences.affinity.withInteraction(
                        destinationId = destination.id,
                        ecosystem = destination.ecosystem,
                        weight = weight,
                    ),
                )
            }
        }
    }

    private companion object {
        const val TEN_DAYS_MILLIS = 10L * 24 * 60 * 60 * 1000
    }
}
