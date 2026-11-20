package com.panamatrip.app.data.repository

import com.panamatrip.app.core.database.dao.CollectionDao
import com.panamatrip.app.core.database.dao.PostDao
import com.panamatrip.app.core.database.entity.PostLikeEntity
import com.panamatrip.app.core.database.entity.CommentEntity
import com.panamatrip.app.core.database.entity.PostEntity
import com.panamatrip.app.data.mapper.toDomain
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.NewCommunityPost
import com.panamatrip.app.domain.model.PostComment
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class CommunityRepositoryImpl(
    private val postDao: PostDao,
    private val collectionDao: CollectionDao,
    private val destinationRepository: DestinationRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : CommunityRepository {

    override fun observePosts(): Flow<List<CommunityPost>> = combine(
        postDao.observeAll(),
        postDao.observeLikedPostIds(),
        postDao.observeCommentCounts(),
        collectionDao.observeEntries(),
        destinationRepository.observeDestinations(),
    ) { posts, likedIds, commentCounts, collectionEntries, destinations ->
        val liked = likedIds.toSet()
        val countsByPost = commentCounts.associate { it.postId to it.total }
        val savedByPost = collectionEntries
            .groupBy { it.postId }
            .mapValues { (_, entries) -> entries.map { it.collectionId }.toSet() }
        val destinationsById = destinations.associateBy { it.id }

        posts.mapNotNull { post ->
            val destination = destinationsById[post.destinationId] ?: return@mapNotNull null
            post.toDomain(
                destination = destination,
                likeCount = post.baseLikeCount,
                isLiked = post.id in liked,
                commentCount = countsByPost[post.id] ?: 0,
                savedCollectionIds = savedByPost[post.id].orEmpty(),
            )
        }
    }

    override fun observeComments(postId: String): Flow<List<PostComment>> =
        postDao.observeComments(postId).map { comments -> comments.map { it.toDomain() } }

    override suspend fun toggleLike(postId: String) {
        if (postDao.isLiked(postId) > 0) {
            postDao.deleteLike(postId)
        } else {
            postDao.insertLike(PostLikeEntity(postId = postId, createdAt = clock()))
        }
    }

    override suspend fun addComment(postId: String, body: String, terrainStatus: TerrainStatus?, authorName: String) {
        val trimmed = body.trim()
        require(trimmed.isNotEmpty() || terrainStatus != null) { "El comentario no puede estar vacío" }
        postDao.insertComment(
            CommentEntity(
                id = "comment-${UUID.randomUUID()}",
                postId = postId,
                authorName = authorName,
                body = trimmed,
                terrainStatus = terrainStatus?.name,
                createdAt = clock(),
            ),
        )
    }

    override suspend fun publishPost(post: NewCommunityPost) {
        val trimmed = post.caption.trim()
        require(trimmed.isNotEmpty()) { "La publicación necesita una descripción" }
        require(post.imageUrl.isNotBlank()) { "La publicación necesita una foto" }
        postDao.insertPost(
            PostEntity(
                id = "post-${UUID.randomUUID()}",
                destinationId = post.destinationId,
                authorName = post.authorName,
                authorRole = post.authorRole,
                authorInitials = post.authorInitials,
                caption = trimmed,
                imageUrl = post.imageUrl,
                visitedYear = post.visitedYear,
                visitedMonth = post.visitedMonth,
                vehicleUsed = post.vehicleUsed.name,
                walkDistanceMeters = post.walkDistanceMeters,
                walkMinutes = post.walkMinutes,
                activityTag = null,
                baseLikeCount = 0,
                createdAt = clock(),
                photoVerified = post.photoVerified,
                exifCapturedAt = post.exifCapturedAtEpochMillis,
            ),
        )
    }
}
