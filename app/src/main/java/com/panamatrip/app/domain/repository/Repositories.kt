package com.panamatrip.app.domain.repository

import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.PostComment
import com.panamatrip.app.domain.model.SavedCollection
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.model.LocalGuide
import com.panamatrip.app.domain.model.NewCommunityPost
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.TrackPoint
import com.panamatrip.app.domain.model.TrackProgress
import com.panamatrip.app.domain.logistics.SavedRoutePlan
import com.panamatrip.app.domain.affinity.AffinitySnapshot
import com.panamatrip.app.core.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow

interface DestinationRepository {
    fun observeDestinations(): Flow<List<Destination>>
    fun observeDestination(id: String): Flow<Destination?>
    fun observeReviews(destinationId: String): Flow<List<DestinationReview>>
}

interface CommunityRepository {
    fun observePosts(): Flow<List<CommunityPost>>
    fun observeComments(postId: String): Flow<List<PostComment>>
    suspend fun toggleLike(postId: String)
    suspend fun addComment(
        postId: String,
        body: String,
        terrainStatus: TerrainStatus?,
        authorName: String,
    )

    suspend fun publishPost(post: NewCommunityPost)
}

interface CollectionRepository {
    fun observeCollections(): Flow<List<SavedCollection>>
    fun observeSavedPostIds(): Flow<Set<String>>
    suspend fun togglePostInCollection(collectionId: String, postId: String)
    suspend fun createCollection(name: String): String
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    val affinity: Flow<AffinitySnapshot>
    suspend fun setPreferredVehicle(vehicle: VehicleType?)
    suspend fun setActiveRoute(destinationId: String?)
    suspend fun recordLike(destination: com.panamatrip.app.domain.model.Destination)
    suspend fun recordSave(destination: com.panamatrip.app.domain.model.Destination)
    suspend fun recordView(destination: com.panamatrip.app.domain.model.Destination)
}

interface GuideRepository {
    fun observeGuides(): Flow<List<LocalGuide>>
}

interface RoutePlanRepository {
    fun observeSavedPlans(destinationId: String, limit: Int): Flow<List<SavedRoutePlan>>
    suspend fun save(plan: SavedRoutePlan)
}

interface TrackRepository {
    fun observeTracks(): Flow<List<RecordedTrack>>
    fun observeTrackPoints(trackId: String): Flow<List<TrackPoint>>

    /** Atómico: cabecera (upsert) + todos sus puntos. */
    suspend fun save(track: RecordedTrack)

    /** Inserta la cabecera como IN_PROGRESS para poder retomarla si la app muere. */
    suspend fun startTrack(track: RecordedTrack)

    /** Atómico: agrega puntos al lote ya guardado y actualiza el progreso. */
    suspend fun persistBatch(trackId: String, points: List<TrackPoint>, progress: TrackProgress)

    suspend fun updateProgress(trackId: String, progress: TrackProgress)

    /** Tracks IN_PROGRESS/PAUSED con sus puntos, para ofrecer recuperación. */
    suspend fun findResumable(): List<RecordedTrack>

    /** Atómico: borra puntos y cabecera. */
    suspend fun discard(trackId: String)
}
