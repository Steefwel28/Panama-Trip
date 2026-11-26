package com.panamatrip.app.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.panamatrip.app.core.database.entity.CollectionEntity
import com.panamatrip.app.core.database.entity.CollectionPostEntity
import com.panamatrip.app.core.database.entity.CommentEntity
import com.panamatrip.app.core.database.entity.DestinationEntity
import com.panamatrip.app.core.database.entity.GuideEntity
import com.panamatrip.app.core.database.entity.PostEntity
import com.panamatrip.app.core.database.entity.PostLikeEntity
import com.panamatrip.app.core.database.entity.PostMediaEntity
import com.panamatrip.app.core.database.entity.ReviewEntity
import com.panamatrip.app.core.database.entity.RoutePlanEntity
import com.panamatrip.app.core.database.entity.TrackEntity
import com.panamatrip.app.core.database.entity.TrackPointEntity
import com.panamatrip.app.core.database.entity.VehicleReportEntity
import kotlinx.coroutines.flow.Flow

data class CommentCountProjection(
    val postId: String,
    val total: Int,
)

data class CollectionEntryProjection(
    val collectionId: String,
    val postId: String,
)

/** Publicación con su carrusel ordenado; la cabecera sigue en `posts`. */
data class PostWithMedia(
    @Embedded val post: PostEntity,
    @Relation(parentColumn = "id", entityColumn = "postId")
    val media: List<PostMediaEntity>,
)

@Dao
interface DestinationDao {
    @Query("SELECT * FROM destinations ORDER BY featuredOrder ASC")
    fun observeAll(): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE id = :id")
    fun observeById(id: String): Flow<DestinationEntity?>

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getById(id: String): DestinationEntity?

    @Query("SELECT COUNT(*) FROM destinations")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DestinationEntity>)
}

@Dao
interface VehicleReportDao {
    @Query("SELECT * FROM vehicle_reports")
    fun observeAll(): Flow<List<VehicleReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VehicleReportEntity>)

    @Query("SELECT COUNT(*) FROM vehicle_reports")
    suspend fun count(): Int
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAllWithMedia(): Flow<List<PostWithMedia>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: String): PostEntity?

    @Query("SELECT postId FROM post_likes")
    fun observeLikedPostIds(): Flow<List<String>>

    @Query("SELECT postId, COUNT(*) AS total FROM comments GROUP BY postId")
    fun observeCommentCounts(): Flow<List<CommentCountProjection>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeComments(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(items: List<PostMediaEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: PostLikeEntity)

    /** Inserta o reemplaza el like: repetir la misma operación no altera el estado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLike(like: PostLikeEntity)

    @Query("SELECT COUNT(*) FROM post_likes WHERE postId = :postId")
    suspend fun isLiked(postId: String): Int

    @Query("DELETE FROM post_likes WHERE postId = :postId")
    suspend fun deleteLike(postId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(items: List<CommentEntity>)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM comments")
    suspend fun commentCount(): Int
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY sortOrder ASC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT collectionId, postId FROM collection_posts")
    fun observeEntries(): Flow<List<CollectionEntryProjection>>

    @Query("SELECT postId FROM collection_posts")
    fun observeSavedPostIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(items: List<CollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: CollectionPostEntity)

    @Query("SELECT COUNT(*) FROM collection_posts WHERE collectionId = :collectionId AND postId = :postId")
    suspend fun hasEntry(collectionId: String, postId: String): Int

    @Query("DELETE FROM collection_posts WHERE collectionId = :collectionId AND postId = :postId")
    suspend fun deleteEntry(collectionId: String, postId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(items: List<CollectionPostEntity>)

    @Query("SELECT COUNT(*) FROM collections")
    suspend fun count(): Int
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE destinationId = :destinationId ORDER BY createdAt DESC")
    fun observeForDestination(destinationId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReviewEntity>)

    @Query("SELECT COUNT(*) FROM reviews")
    suspend fun count(): Int
}

@Dao
interface RoutePlanDao {
    @Query(
        "SELECT * FROM route_plans WHERE destinationId = :destinationId " +
            "ORDER BY createdAt DESC LIMIT :limit",
    )
    fun observeRecentForDestination(
        destinationId: String,
        limit: Int,
    ): Flow<List<RoutePlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: RoutePlanEntity)

    @Query("SELECT COUNT(*) FROM route_plans")
    suspend fun count(): Int
}

data class GuideWithDestinationProjection(
    val id: String,
    val destinationId: String,
    val name: String,
    val headline: String,
    val languages: String,
    val rateCents: Int,
    val verified: Boolean,
    val whatsappNumber: String,
    val destinationName: String,
    val province: String,
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY sequence ASC")
    fun observePoints(trackId: String): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY sequence ASC")
    suspend fun getPoints(trackId: String): List<TrackPointEntity>

    /** Tracks que quedaron a medias y se pueden retomar al reabrir la app. */
    @Query("SELECT * FROM tracks WHERE status IN ('IN_PROGRESS','PAUSED') ORDER BY startedAt DESC")
    suspend fun findResumable(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query(
        "UPDATE tracks SET status = :status, finishedAt = :finishedAt, " +
            "distanceMeters = :distanceMeters, activeDurationMillis = :activeDurationMillis, " +
            "averageSpeedKmh = :averageSpeedKmh, elevationGainMeters = :elevationGainMeters, " +
            "pointCount = :pointCount WHERE id = :trackId " +
            "AND status NOT IN ('COMPLETED', 'DISCARDED')",
    )
    suspend fun updateProgress(
        trackId: String,
        status: String,
        finishedAt: Long,
        distanceMeters: Int,
        activeDurationMillis: Long,
        averageSpeedKmh: Double,
        elevationGainMeters: Int,
        pointCount: Int,
    )

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deletePoints(trackId: String)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int
}

@Dao
interface GuideDao {
    @Query(
        "SELECT g.id, g.destinationId, g.name, g.headline, g.languages, g.rateCents, " +
            "g.verified, g.whatsappNumber, d.name AS destinationName, d.province AS province " +
            "FROM guides g INNER JOIN destinations d ON d.id = g.destinationId " +
            "ORDER BY d.featuredOrder ASC, g.verified DESC, g.name ASC",
    )
    fun observeAll(): Flow<List<GuideWithDestinationProjection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GuideEntity>)

    @Query("SELECT COUNT(*) FROM guides")
    suspend fun count(): Int
}
