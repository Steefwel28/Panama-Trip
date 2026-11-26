package com.panamatrip.app.domain.model

/** Tipo de medio dentro del carrusel de una publicación. */
enum class PostMediaType { PHOTO, VIDEO }

/** Foto o video adjunto a una publicación, con proporción real para el feed. */
data class PostMediaItem(
    val id: String,
    val uri: String,
    val mimeType: String,
    val type: PostMediaType,
    val width: Int,
    val height: Int,
    val durationMillis: Long? = null,
) {
    /** Relación ancho/alto con respaldo 4:3 si el proveedor no reportó tamaño. */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height else 4f / 3f
}

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
    /** Carrusel completo; si viene vacío se usa [imageUrl] como foto única. */
    val media: List<PostMediaItem> = emptyList(),
    /** Ruta grabada vinculada, si el autor la adjuntó al publicar. */
    val linkedTrackId: String? = null,
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
    val media: List<PostMediaItem> = emptyList(),
    val trackId: String? = null,
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
