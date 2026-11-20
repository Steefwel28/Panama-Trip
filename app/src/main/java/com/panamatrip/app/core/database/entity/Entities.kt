package com.panamatrip.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val province: String,
    val ecosystem: String,
    val summary: String,
    val description: String,
    val accessNotes: String,
    val parkingLatitude: Double,
    val parkingLongitude: Double,
    val attractionLatitude: Double,
    val attractionLongitude: Double,
    val heroImageUrl: String,
    val walkDistanceMeters: Int,
    val walkMinutes: Int,
    val elevationGainMeters: Int,
    val recommendedVehicle: String,
    val featuredOrder: Int,
)

@Entity(
    tableName = "vehicle_reports",
    indices = [Index("destinationId")],
)
data class VehicleReportEntity(
    @PrimaryKey val id: String,
    val destinationId: String,
    val vehicleType: String,
    val verdict: String,
    val authorName: String,
    val createdAt: Long,
)

@Entity(
    tableName = "posts",
    indices = [Index("destinationId"), Index("createdAt")],
)
data class PostEntity(
    @PrimaryKey val id: String,
    val destinationId: String,
    val authorName: String,
    val authorRole: String,
    val authorInitials: String,
    val caption: String,
    val imageUrl: String,
    val visitedYear: Int,
    val visitedMonth: Int,
    val vehicleUsed: String,
    val walkDistanceMeters: Int,
    val walkMinutes: Int,
    val activityTag: String?,
    val baseLikeCount: Int,
    val createdAt: Long,
)

@Entity(tableName = "post_likes")
data class PostLikeEntity(
    @PrimaryKey val postId: String,
    val createdAt: Long,
)

@Entity(
    tableName = "comments",
    indices = [Index("postId")],
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorName: String,
    val body: String,
    val terrainStatus: String?,
    val createdAt: Long,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val symbol: String,
    val sortOrder: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "collection_posts",
    primaryKeys = ["collectionId", "postId"],
    indices = [Index("postId")],
)
data class CollectionPostEntity(
    val collectionId: String,
    val postId: String,
    val savedAt: Long,
)

@Entity(
    tableName = "reviews",
    indices = [Index("destinationId")],
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val destinationId: String,
    val authorName: String,
    val authorRole: String,
    val rating: Int,
    val body: String,
    val visitLabel: String,
    val createdAt: Long,
)

@Entity(
    tableName = "route_plans",
    indices = [Index("destinationId"), Index("createdAt")],
)
data class RoutePlanEntity(
    @PrimaryKey val id: String,
    val destinationId: String,
    val destinationName: String,
    val fuelType: String,
    val vehicleType: String,
    val mountainSurcharge: Boolean,
    val departureMinuteOfDay: Int,
    val distanceMeters: Int,
    val driveMinutes: Int,
    val roundTripKilometers: Double,
    val fuelLiters: Double,
    val fuelCostCents: Int,
    val tollCostCents: Int,
    val cashOnlyCents: Int,
    val totalCents: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "guides",
    indices = [Index("destinationId")],
)
data class GuideEntity(
    @PrimaryKey val id: String,
    val destinationId: String,
    val name: String,
    val headline: String,
    val languages: String,
    val rateCents: Int,
    val verified: Boolean,
    val whatsappNumber: String,
)
