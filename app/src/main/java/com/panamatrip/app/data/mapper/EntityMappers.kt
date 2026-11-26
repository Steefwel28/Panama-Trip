package com.panamatrip.app.data.mapper

import com.panamatrip.app.core.database.entity.CollectionEntity
import com.panamatrip.app.core.database.entity.CommentEntity
import com.panamatrip.app.core.database.entity.DestinationEntity
import com.panamatrip.app.core.database.entity.PostEntity
import com.panamatrip.app.core.database.entity.PostMediaEntity
import com.panamatrip.app.core.database.entity.ReviewEntity
import com.panamatrip.app.core.database.entity.RoutePlanEntity
import com.panamatrip.app.core.database.entity.TrackEntity
import com.panamatrip.app.core.database.entity.TrackPointEntity
import com.panamatrip.app.core.database.entity.VehicleReportEntity
import com.panamatrip.app.core.database.dao.GuideWithDestinationProjection
import com.panamatrip.app.domain.logistics.FuelType
import com.panamatrip.app.domain.logistics.SavedRoutePlan
import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.Carrier
import com.panamatrip.app.domain.model.SignalLevel
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.GuideLanguage
import com.panamatrip.app.domain.model.LocalGuide
import com.panamatrip.app.domain.model.PostComment
import com.panamatrip.app.domain.model.PostMediaItem
import com.panamatrip.app.domain.model.PostMediaType
import com.panamatrip.app.domain.model.RecordedTrack
import com.panamatrip.app.domain.model.SavedCollection
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.TrackPoint
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.model.VehicleAccessSummary
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.ranking.RatingCalculator
import com.panamatrip.app.domain.ranking.VehicleTrafficLight

fun VehicleReportEntity.toTrafficLightReport(): VehicleTrafficLight.Report =
    VehicleTrafficLight.Report(
        vehicle = enumOrDefault(vehicleType, VehicleType.SUV),
        verdict = enumOrDefault(verdict, AccessStatus.CAUTION),
    )

fun DestinationEntity.toDomain(
    access: Map<VehicleType, VehicleAccessSummary>,
    reviews: List<DestinationReview>,
): Destination = Destination(
    id = id,
    name = name,
    province = province,
    ecosystem = enumOrDefault(ecosystem, Ecosystem.MOUNTAIN),
    summary = summary,
    description = description,
    accessNotes = accessNotes,
    parking = GeoPoint(parkingLatitude, parkingLongitude),
    attraction = GeoPoint(attractionLatitude, attractionLongitude),
    heroImageUrl = heroImageUrl,
    walkDistanceMeters = walkDistanceMeters,
    walkMinutes = walkMinutes,
    elevationGainMeters = elevationGainMeters,
    recommendedVehicle = enumOrDefault(recommendedVehicle, VehicleType.SUV),
    vehicleAccess = completeAccessMap(access),
    carrierSignal = mapOf(
        Carrier.MOVIL to enumOrDefault(movilSignal, SignalLevel.MEDIUM),
        Carrier.TIGO to enumOrDefault(tigoSignal, SignalLevel.MEDIUM),
    ),
    averageRating = RatingCalculator.average(reviews),
    reviewCount = reviews.size,
    featuredOrder = featuredOrder,
)

fun ReviewEntity.toDomain(): DestinationReview = DestinationReview(
    id = id,
    authorName = authorName,
    authorRole = authorRole,
    rating = rating.coerceIn(MIN_RATING, MAX_RATING),
    body = body,
    visitLabel = visitLabel,
)

fun CommentEntity.toDomain(): PostComment = PostComment(
    id = id,
    authorName = authorName,
    body = body,
    terrainStatus = terrainStatus?.let { runCatching { TerrainStatus.valueOf(it) }.getOrNull() },
    createdAt = createdAt,
)

fun PostEntity.toDomain(
    destination: Destination,
    likeCount: Int,
    isLiked: Boolean,
    commentCount: Int,
    savedCollectionIds: Set<String>,
    media: List<PostMediaEntity> = emptyList(),
): CommunityPost = CommunityPost(
    id = id,
    destination = destination,
    authorName = authorName,
    authorRole = authorRole,
    authorInitials = authorInitials,
    caption = caption,
    imageUrl = imageUrl,
    visitedYear = visitedYear,
    visitedMonth = visitedMonth,
    vehicleUsed = enumOrDefault(vehicleUsed, VehicleType.SUV),
    walkDistanceMeters = walkDistanceMeters,
    walkMinutes = walkMinutes,
    activityTag = activityTag,
    likeCount = (baseLikeCount + if (isLiked) 1 else 0).coerceAtLeast(0),
    isLiked = isLiked,
    commentCount = commentCount,
    savedCollectionIds = savedCollectionIds,
    createdAt = createdAt,
    photoVerified = photoVerified,
    exifCapturedAtEpochMillis = exifCapturedAt,
    media = media
        .sortedBy { it.position }
        .map { item -> item.toDomain() },
    linkedTrackId = trackId,
)

fun PostMediaEntity.toDomain(): PostMediaItem = PostMediaItem(
    id = id,
    uri = uri,
    mimeType = mimeType,
    type = if (mimeType.startsWith("video/")) PostMediaType.VIDEO else PostMediaType.PHOTO,
    width = width,
    height = height,
    durationMillis = durationMillis,
)

fun PostMediaItem.toEntity(postId: String, position: Int): PostMediaEntity = PostMediaEntity(
    id = id,
    postId = postId,
    uri = uri,
    mimeType = mimeType,
    position = position,
    width = width,
    height = height,
    durationMillis = durationMillis,
)

fun CollectionEntity.toDomain(postIds: List<String>, previewImageUrl: String?): SavedCollection =
    SavedCollection(
        id = id,
        name = name,
        symbol = symbol,
        postIds = postIds,
        previewImageUrl = previewImageUrl,
        sortOrder = sortOrder,
    )

fun GuideWithDestinationProjection.toDomain(): LocalGuide = LocalGuide(
    id = id,
    destinationId = destinationId,
    destinationName = destinationName,
    province = province,
    name = name,
    headline = headline,
    languages = languages
        .split(LANGUAGE_SEPARATOR)
        .map { token -> token.trim() }
        .filter { token -> token.isNotEmpty() }
        .mapNotNull { token ->
            runCatching { GuideLanguage.valueOf(token) }.getOrNull()
        },
    rateUsdCents = rateCents,
    verified = verified,
    whatsappNumber = whatsappNumber,
)

fun RoutePlanEntity.toDomain(): SavedRoutePlan = SavedRoutePlan(
    id = id,
    destinationId = destinationId,
    destinationName = destinationName,
    fuelType = enumOrDefault(fuelType, FuelType.GASOLINE_95),
    vehicle = enumOrDefault(vehicleType, com.panamatrip.app.domain.model.VehicleType.SUV),
    mountainSurcharge = mountainSurcharge,
    departureMinuteOfDay = departureMinuteOfDay,
    distanceMeters = distanceMeters,
    driveMinutes = driveMinutes,
    roundTripKilometers = roundTripKilometers,
    fuelLiters = fuelLiters,
    fuelCostCents = fuelCostCents,
    tollCostCents = tollCostCents,
    cashOnlyCents = cashOnlyCents,
    totalCents = totalCents,
    createdAtEpochMillis = createdAt,
)

fun SavedRoutePlan.toEntity(): RoutePlanEntity = RoutePlanEntity(
    id = id,
    destinationId = destinationId,
    destinationName = destinationName,
    fuelType = fuelType.name,
    vehicleType = vehicle.name,
    mountainSurcharge = mountainSurcharge,
    departureMinuteOfDay = departureMinuteOfDay,
    distanceMeters = distanceMeters,
    driveMinutes = driveMinutes,
    roundTripKilometers = roundTripKilometers,
    fuelLiters = fuelLiters,
    fuelCostCents = fuelCostCents,
    tollCostCents = tollCostCents,
    cashOnlyCents = cashOnlyCents,
    totalCents = totalCents,
    createdAt = createdAtEpochMillis,
)

fun TrackEntity.toDomain(): RecordedTrack = RecordedTrack(
    id = id,
    destinationId = destinationId,
    destinationName = destinationName,
    startedAtEpochMillis = startedAt,
    finishedAtEpochMillis = finishedAt,
    distanceMeters = distanceMeters,
    activeDurationMillis = activeDurationMillis,
    averageSpeedKmh = averageSpeedKmh,
    elevationGainMeters = elevationGainMeters,
    pointCount = pointCount,
    status = enumOrDefault(status, TrackStatus.COMPLETED),
    validWalkingDistanceMeters = validWalkingDistanceMeters,
    stepCount = stepCount,
    evidenceAvailable = evidenceAvailable,
)

/** Arma el recorrido completo: cabecera de [TrackEntity] más sus puntos ordenados. */
fun TrackEntity.toDomain(points: List<TrackPointEntity>): RecordedTrack =
    toDomain().copy(points = points.map { point -> point.toDomain() })

fun RecordedTrack.toEntity(): TrackEntity = TrackEntity(
    id = id,
    destinationId = destinationId,
    destinationName = destinationName,
    startedAt = startedAtEpochMillis,
    finishedAt = finishedAtEpochMillis,
    distanceMeters = distanceMeters,
    activeDurationMillis = activeDurationMillis,
    averageSpeedKmh = averageSpeedKmh,
    elevationGainMeters = elevationGainMeters,
    pointCount = pointCount,
    status = status.name,
    validWalkingDistanceMeters = validWalkingDistanceMeters,
    stepCount = stepCount,
    evidenceAvailable = evidenceAvailable,
)

fun TrackPoint.toEntity(trackId: String): TrackPointEntity = TrackPointEntity(
    trackId = trackId,
    sequence = sequence,
    latitude = point.latitude,
    longitude = point.longitude,
    elevationMeters = elevationMeters,
    accuracyMeters = accuracyMeters,
    recordedAt = recordedAtEpochMillis,
    activeElapsedMillis = activeElapsedMillis,
    segmentIndex = segmentIndex,
)

fun TrackPointEntity.toDomain(): TrackPoint = TrackPoint(
    point = GeoPoint(latitude, longitude),
    elevationMeters = elevationMeters,
    accuracyMeters = accuracyMeters,
    recordedAtEpochMillis = recordedAt,
    activeElapsedMillis = activeElapsedMillis,
    segmentIndex = segmentIndex,
    sequence = sequence,
)

/** Completa los tres vehículos para que la UI nunca tenga huecos. */
private fun completeAccessMap(
    access: Map<VehicleType, VehicleAccessSummary>,
): Map<VehicleType, VehicleAccessSummary> = VehicleType.entries.associateWith { vehicle ->
    access[vehicle] ?: VehicleAccessSummary(vehicle, AccessStatus.CAUTION, reports = 0)
}

private const val MIN_RATING = 1
private const val MAX_RATING = 5
private const val LANGUAGE_SEPARATOR = ","

private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T =
    raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
