package com.panamatrip.app.core.database

import com.panamatrip.app.core.database.entity.CollectionEntity
import com.panamatrip.app.core.database.entity.CollectionPostEntity
import com.panamatrip.app.core.database.entity.CommentEntity
import com.panamatrip.app.core.database.entity.DestinationEntity
import com.panamatrip.app.core.database.entity.PostEntity
import com.panamatrip.app.core.database.entity.ReviewEntity
import com.panamatrip.app.core.database.entity.VehicleReportEntity
import com.panamatrip.app.core.database.seed.SeedCatalog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Precarga idempotente de la base editorial. Se ejecuta en el arranque desde
 * un scope de aplicación, nunca desde la UI.
 */
class DatabaseSeeder(private val database: PanamaTripDatabase) {

    suspend fun seedIfEmpty(now: Long = System.currentTimeMillis()) {
        seedDestinations()
        seedVehicleReports(now)
        seedPosts(now)
        seedComments(now)
        seedCollections(now)
        seedReviews(now)
    }

    private suspend fun seedDestinations() {
        if (database.destinationDao().count() > 0) return
        database.destinationDao().insertAll(
            SeedCatalog.destinations.map { seed ->
                DestinationEntity(
                    id = seed.id,
                    name = seed.name,
                    province = seed.province,
                    ecosystem = seed.ecosystem.name,
                    summary = seed.summary,
                    description = seed.description,
                    accessNotes = seed.accessNotes,
                    parkingLatitude = seed.parkingLatitude,
                    parkingLongitude = seed.parkingLongitude,
                    attractionLatitude = seed.attractionLatitude,
                    attractionLongitude = seed.attractionLongitude,
                    heroImageUrl = seed.heroImageUrl,
                    walkDistanceMeters = seed.walkDistanceMeters,
                    walkMinutes = seed.walkMinutes,
                    elevationGainMeters = seed.elevationGainMeters,
                    recommendedVehicle = seed.recommendedVehicle.name,
                    featuredOrder = seed.featuredOrder,
                )
            },
        )
    }

    private suspend fun seedVehicleReports(now: Long) {
        if (database.vehicleReportDao().count() > 0) return
        database.vehicleReportDao().insertAll(
            SeedCatalog.vehicleReports.mapIndexed { index, report ->
                VehicleReportEntity(
                    id = report.id,
                    destinationId = report.destinationId,
                    vehicleType = report.vehicle.name,
                    verdict = report.verdict.name,
                    authorName = report.authorName,
                    createdAt = now - index * MILLIS_PER_HOUR,
                )
            },
        )
    }

    private suspend fun seedPosts(now: Long) {
        if (database.postDao().count() > 0) return
        val zone = ZoneId.systemDefault()
        val posts = SeedCatalog.posts.mapIndexed { index, seed ->
            val createdAt = now - seed.visitedDaysAgo * MILLIS_PER_DAY
            val visit = Instant.ofEpochMilli(createdAt).atZone(zone)
            PostEntity(
                id = seed.id,
                destinationId = seed.destinationId,
                authorName = seed.authorName,
                authorRole = seed.authorRole,
                authorInitials = seed.authorInitials,
                caption = seed.caption,
                imageUrl = seed.imageUrl,
                visitedYear = visit.year,
                visitedMonth = visit.monthValue,
                vehicleUsed = seed.vehicleUsed.name,
                walkDistanceMeters = seed.walkDistanceMeters,
                walkMinutes = seed.walkMinutes,
                activityTag = seed.activityTag,
                baseLikeCount = seed.baseLikeCount,
                createdAt = createdAt - index * MILLIS_PER_MINUTE,
            )
        }
        database.postDao().insertAll(posts)
    }

    private suspend fun seedComments(now: Long) {
        if (database.postDao().commentCount() > 0) return
        database.postDao().insertComments(
            SeedCatalog.comments.map { seed ->
                CommentEntity(
                    id = seed.id,
                    postId = seed.postId,
                    authorName = seed.authorName,
                    body = seed.body,
                    terrainStatus = seed.terrainStatus?.name,
                    createdAt = now - seed.daysAgo * MILLIS_PER_DAY,
                )
            },
        )
    }

    private suspend fun seedCollections(now: Long) {
        if (database.collectionDao().count() > 0) return
        database.collectionDao().insertCollections(
            SeedCatalog.collections.map { seed ->
                CollectionEntity(
                    id = seed.id,
                    name = seed.name,
                    symbol = seed.symbol,
                    sortOrder = seed.sortOrder,
                    createdAt = now,
                )
            },
        )
        database.collectionDao().insertEntries(
            SeedCatalog.collections.flatMap { collection ->
                collection.postIds.map { postId ->
                    CollectionPostEntity(
                        collectionId = collection.id,
                        postId = postId,
                        savedAt = now,
                    )
                }
            },
        )
    }

    private suspend fun seedReviews(now: Long) {
        if (database.reviewDao().count() > 0) return
        val formatter = DateTimeFormatter.ofPattern("MM/yyyy", Locale.forLanguageTag("es-PA"))
        val zone = ZoneId.systemDefault()
        database.reviewDao().insertAll(
            SeedCatalog.reviews.map { seed ->
                val visitEpoch = now - seed.daysAgo * MILLIS_PER_DAY
                ReviewEntity(
                    id = seed.id,
                    destinationId = seed.destinationId,
                    authorName = seed.authorName,
                    authorRole = seed.authorRole,
                    rating = seed.rating,
                    body = seed.body,
                    visitLabel = formatter.format(Instant.ofEpochMilli(visitEpoch).atZone(zone)),
                    createdAt = visitEpoch,
                )
            },
        )
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val MILLIS_PER_HOUR = 3_600_000L
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
