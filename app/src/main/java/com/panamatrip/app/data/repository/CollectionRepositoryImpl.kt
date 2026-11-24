package com.panamatrip.app.data.repository

import androidx.room.withTransaction
import com.panamatrip.app.core.database.PanamaTripDatabase
import com.panamatrip.app.core.database.dao.CollectionDao
import com.panamatrip.app.core.database.dao.PostDao
import com.panamatrip.app.core.database.entity.CollectionEntity
import com.panamatrip.app.core.database.entity.CollectionPostEntity
import com.panamatrip.app.data.mapper.toDomain
import com.panamatrip.app.domain.model.SavedCollection
import com.panamatrip.app.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class CollectionRepositoryImpl(
    private val database: PanamaTripDatabase,
    private val collectionDao: CollectionDao,
    private val postDao: PostDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : CollectionRepository {

    override fun observeCollections(): Flow<List<SavedCollection>> = combine(
        collectionDao.observeCollections(),
        collectionDao.observeEntries(),
        postDao.observeAll(),
    ) { collections, entries, posts ->
        val entriesByCollection = entries.groupBy { it.collectionId }
        val imagesByPost = posts.associate { it.id to it.imageUrl }
        val postsById = posts.associateBy { it.id }

        collections.map { entity ->
            val postIds = entriesByCollection[entity.id]
                .orEmpty()
                .sortedByDescending { it.postId }
                .map { it.postId }
                .filter { postsById.containsKey(it) }
            entity.toDomain(
                postIds = postIds,
                previewImageUrl = postIds.firstNotNullOfOrNull { imagesByPost[it] },
            )
        }
    }

    override fun observeSavedPostIds(): Flow<Set<String>> =
        collectionDao.observeSavedPostIds().map { ids -> ids.toSet() }

    override suspend fun togglePostInCollection(collectionId: String, postId: String) {
        database.withTransaction {
            val saved = collectionDao.hasEntry(collectionId, postId) > 0
            setPostInCollection(collectionId, postId, !saved)
        }
    }

    /** Idempotente: guardar o quitar dos veces el mismo post deja el mismo estado. */
    private suspend fun setPostInCollection(collectionId: String, postId: String, saved: Boolean) {
        if (saved) {
            collectionDao.insertEntry(
                CollectionPostEntity(collectionId = collectionId, postId = postId, savedAt = clock()),
            )
        } else {
            collectionDao.deleteEntry(collectionId, postId)
        }
    }

    override suspend fun createCollection(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "La colección necesita un nombre" }
        val existing = collectionDao.count()
        val id = "collection-${UUID.randomUUID()}"
        collectionDao.insertCollections(
            listOf(
                CollectionEntity(
                    id = id,
                    name = trimmed,
                    symbol = DEFAULT_SYMBOL,
                    sortOrder = existing,
                    createdAt = clock(),
                ),
            ),
        )
        return id
    }

    private companion object {
        const val DEFAULT_SYMBOL = "📌"
    }
}
