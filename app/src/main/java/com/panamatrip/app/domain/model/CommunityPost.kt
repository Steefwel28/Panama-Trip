package com.panamatrip.app.domain.model

/** Publicación del feed comunitario, ya agregada con sus interacciones. */
data class CommunityPost(
    val id: String,
    val destination: Destination,
    val authorName: String,
    val authorRole: String,
    val authorInitials: String,
    val caption: String,
    val imageUrl: String,
    val visitedYear: Int,
    val visitedMonth: Int,
    val vehicleUsed: VehicleType,
    val walkDistanceMeters: Int,
    val walkMinutes: Int,
    val activityTag: String?,
    val likeCount: Int,
    val isLiked: Boolean,
    val commentCount: Int,
    val savedCollectionIds: Set<String>,
    val createdAt: Long,
    val photoVerified: Boolean = false,
    val exifCapturedAtEpochMillis: Long? = null,
)

/** Datos mínimos para publicar una expedición desde el teléfono. */
data class NewCommunityPost(
    val destinationId: String,
    val authorName: String,
    val authorRole: String,
    val authorInitials: String,
    val caption: String,
    val imageUrl: String,
    val visitedYear: Int,
    val visitedMonth: Int,
    val vehicleUsed: VehicleType,
    val walkDistanceMeters: Int,
    val walkMinutes: Int,
    val photoVerified: Boolean,
    val exifCapturedAtEpochMillis: Long?,
)

/** Comentario con chip rápido de terreno opcional. */
data class PostComment(
    val id: String,
    val authorName: String,
    val body: String,
    val terrainStatus: TerrainStatus?,
    val createdAt: Long,
)

/** Reseña verificada de un destino. */
data class DestinationReview(
    val id: String,
    val authorName: String,
    val authorRole: String,
    val rating: Int,
    val body: String,
    val visitLabel: String,
)

/** Colección local de publicaciones guardadas. */
data class SavedCollection(
    val id: String,
    val name: String,
    val symbol: String,
    val postIds: List<String>,
    val previewImageUrl: String?,
    val sortOrder: Int,
)
