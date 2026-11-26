package com.panamatrip.app.core.preview

import com.panamatrip.app.core.database.seed.SeedCatalog
import com.panamatrip.app.core.designsystem.labels.monthYearLabel
import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.logistics.FuelType
import com.panamatrip.app.domain.logistics.LogisticsEngine
import com.panamatrip.app.domain.logistics.LogisticsInput
import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.GuideLanguage
import com.panamatrip.app.domain.model.LocalGuide
import com.panamatrip.app.domain.model.SavedCollection
import com.panamatrip.app.domain.model.VehicleAccessSummary
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.ranking.RatingCalculator
import com.panamatrip.app.domain.ranking.VehicleTrafficLight
import com.panamatrip.app.features.destinationdetail.ui.DestinationDetailUiState
import com.panamatrip.app.features.explore.ui.DirectoryEntryUi
import com.panamatrip.app.features.explore.ui.ExploreTab
import com.panamatrip.app.features.explore.ui.ExploreUiState
import com.panamatrip.app.features.explore.ui.FeedPostUi
import com.panamatrip.app.features.guides.ui.GuideDestinationFilterUi
import com.panamatrip.app.features.guides.ui.GuidesUiState
import com.panamatrip.app.features.routeplan.ui.DepartureWindow
import com.panamatrip.app.features.routeplan.ui.OriginMode
import com.panamatrip.app.features.routeplan.ui.RoutePlanUiState
import com.panamatrip.app.features.routeplan.ui.SavedPlanUi
import com.panamatrip.app.features.saved.ui.SavedCollectionUi
import com.panamatrip.app.features.saved.ui.SavedUiState
import com.panamatrip.app.features.trackrecorder.ui.ActiveTrackUi
import com.panamatrip.app.features.trackrecorder.ui.RecordedTrackUi
import com.panamatrip.app.features.trackrecorder.ui.TrackRecorderUiState
import com.panamatrip.app.features.postcomposer.ui.PostComposerUiState
import com.panamatrip.app.features.postcomposer.ui.SelectedPhotoUi
import com.panamatrip.app.domain.photo.ExifCheck
import com.panamatrip.app.domain.photo.ExifMetadata
import com.panamatrip.app.domain.photo.ExifVerification
import com.panamatrip.app.domain.model.RecorderStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Datos deterministas para los @Preview de tema claro y oscuro. Reutiliza la
 * semilla editorial real para que el diseño se evalúe con contenido auténtico.
 */
object PreviewFixtures {

    private val zone = ZoneId.systemDefault()

    private val reviews: List<DestinationReview> = SeedCatalog.reviews.map { seed ->
        val visit = Instant.ofEpochMilli(System.currentTimeMillis() - seed.daysAgo * DAY_MILLIS)
            .atZone(zone)
        DestinationReview(
            id = seed.id,
            authorName = seed.authorName,
            authorRole = seed.authorRole,
            rating = seed.rating,
            body = seed.body,
            visitLabel = monthYearLabel(visit.monthValue, visit.year),
        )
    }

    private val accessByDestination: Map<String, Map<VehicleType, VehicleAccessSummary>> =
        SeedCatalog.vehicleReports
            .groupBy { it.destinationId }
            .mapValues { (_, reports) ->
                VehicleTrafficLight.aggregate(
                    reports.map { VehicleTrafficLight.Report(it.vehicle, it.verdict) },
                )
            }

    val destinations: List<Destination> = SeedCatalog.destinations.map { seed ->
        val destinationReviews = reviews.filter { review ->
            SeedCatalog.reviews.first { it.id == review.id }.destinationId == seed.id
        }
        Destination(
            id = seed.id,
            name = seed.name,
            province = seed.province,
            ecosystem = seed.ecosystem,
            summary = seed.summary,
            description = seed.description,
            accessNotes = seed.accessNotes,
            parking = GeoPoint(seed.parkingLatitude, seed.parkingLongitude),
            attraction = GeoPoint(seed.attractionLatitude, seed.attractionLongitude),
            heroImageUrl = seed.heroImageUrl,
            walkDistanceMeters = seed.walkDistanceMeters,
            walkMinutes = seed.walkMinutes,
            elevationGainMeters = seed.elevationGainMeters,
            recommendedVehicle = seed.recommendedVehicle,
            vehicleAccess = accessByDestination[seed.id] ?: emptyMap(),
            averageRating = RatingCalculator.average(destinationReviews),
            reviewCount = destinationReviews.size,
            featuredOrder = seed.featuredOrder,
        )
    }

    private val collections: List<SavedCollection> = SeedCatalog.collections.map { seed ->
        SavedCollection(
            id = seed.id,
            name = seed.name,
            symbol = seed.symbol,
            postIds = seed.postIds,
            previewImageUrl = SeedCatalog.posts.firstOrNull { it.id == seed.postIds.firstOrNull() }?.imageUrl,
            sortOrder = seed.sortOrder,
        )
    }

    val posts: List<CommunityPost> = SeedCatalog.posts.map { seed ->
        val destination = destinations.first { it.id == seed.destinationId }
        val savedIn = collections.filter { seed.id in it.postIds }.map { it.id }.toSet()
        val visit = Instant.ofEpochMilli(System.currentTimeMillis() - seed.visitedDaysAgo * DAY_MILLIS)
            .atZone(zone)
        CommunityPost(
            id = seed.id,
            destination = destination,
            authorName = seed.authorName,
            authorRole = seed.authorRole,
            authorInitials = seed.authorInitials,
            caption = seed.caption,
            imageUrl = seed.imageUrl,
            visitedYear = visit.year,
            visitedMonth = visit.monthValue,
            vehicleUsed = seed.vehicleUsed,
            walkDistanceMeters = seed.walkDistanceMeters,
            walkMinutes = seed.walkMinutes,
            activityTag = seed.activityTag,
            likeCount = seed.baseLikeCount + if (seed.id.hashCode() % 2 == 0) 1 else 0,
            isLiked = seed.id.hashCode() % 3 == 0,
            commentCount = SeedCatalog.comments.count { it.postId == seed.id },
            savedCollectionIds = savedIn,
            createdAt = System.currentTimeMillis() - seed.visitedDaysAgo * DAY_MILLIS,
        )
    }

    fun exploreState(tab: ExploreTab): ExploreUiState.Content {
        val origin = GeoPoint(8.9824, -79.5199)
        return ExploreUiState.Content(
            tab = tab,
            posts = posts.map { FeedPostUi(post = it, affinity = 0.35f, distanceMeters = 42_000) },
            directory = destinations.map { destination ->
                val distance = GeoCalculator.distanceMeters(origin, destination.parking)
                DirectoryEntryUi(
                    destination = destination,
                    distanceMeters = distance,
                    driveMinutes = GeoCalculator.estimatedDriveMinutes(distance),
                    affinity = 0.35f,
                )
            }.sortedBy { it.distanceMeters },
            filters = com.panamatrip.app.domain.ranking.DirectoryFilters(),
            guestMode = true,
        )
    }

    fun detailState(): DestinationDetailUiState.Content = DestinationDetailUiState.Content(
        destination = destinations.first(),
        reviews = reviews.filter {
            SeedCatalog.reviews.first { seed -> seed.id == it.id }.destinationId == destinations.first().id
        },
    )

    fun savedState(): SavedUiState.Content = SavedUiState.Content(
        collections = collections.map { collection ->
            SavedCollectionUi(
                id = collection.id,
                name = collection.name,
                symbol = collection.symbol,
                postCount = collection.postIds.size,
                previewImageUrl = collection.previewImageUrl,
                selected = false,
            )
        },
        posts = posts.filter { it.savedCollectionIds.isNotEmpty() },
        selectedCollectionId = null,
    )

    fun routeState(): RoutePlanUiState.Content {
        val origin = GeoPoint(8.9824, -79.5199)
        val destination = destinations.first { it.province == "Colón" }
        val plan = LogisticsEngine.plan(
            LogisticsInput(
                origin = origin,
                destination = destination,
                vehicle = VehicleType.SUV,
                fuelType = FuelType.GASOLINE_95,
                mountainSurcharge = true,
                departureMinuteOfDay = DepartureWindow.DAWN.minuteOfDay,
                month = 11,
            ),
        )
        val savedPlan = SavedPlanUi(
            id = "preview-plan",
            fuelType = FuelType.GASOLINE_95,
            mountainSurcharge = true,
            departureMinuteOfDay = DepartureWindow.DAWN.minuteOfDay,
            totalCents = plan.totalRoundTripCents,
            cashOnlyCents = plan.cashOnlyRoundTripCents,
            createdAtEpochMillis = System.currentTimeMillis() - DAY_MILLIS,
        )
        return RoutePlanUiState.Content(
            destination = destination,
            origin = origin,
            originMode = OriginMode.CUSTOM,
            vehicle = VehicleType.SUV,
            fuelType = FuelType.GASOLINE_95,
            surchargeEnabled = true,
            departureWindow = DepartureWindow.DAWN,
            cashAlertsEnabled = true,
            plan = plan,
            savedPlans = listOf(savedPlan),
        )
    }

    fun guidesState(): GuidesUiState.Content {
        val guides = SeedCatalog.guides.map { seed ->
            val destination = destinations.first { it.id == seed.destinationId }
            LocalGuide(
                id = seed.id,
                destinationId = seed.destinationId,
                destinationName = destination.name,
                province = destination.province,
                name = seed.name,
                headline = seed.headline,
                languages = seed.languages.map { GuideLanguage.valueOf(it) },
                rateUsdCents = seed.rateCents,
                verified = seed.verified,
                whatsappNumber = seed.whatsappNumber,
            )
        }
        return GuidesUiState.Content(
            guides = guides,
            filters = guides
                .map { GuideDestinationFilterUi(id = it.destinationId, name = it.destinationName) }
                .distinctBy { it.id }
                .sortedBy { it.name },
            selectedDestinationId = null,
            totalCount = guides.size,
        )
    }

    val ecosystems: List<Ecosystem> = Ecosystem.entries
    val accessStatuses: List<AccessStatus> = AccessStatus.entries

    fun recorderState(): TrackRecorderUiState.Content = TrackRecorderUiState.Content(
        active = ActiveTrackUi(
            status = RecorderStatus.PAUSED,
            destinationName = destinations.first().name,
            startedAtEpochMillis = System.currentTimeMillis() - 42 * MINUTE_MILLIS,
            distanceMeters = 3_240,
            activeDurationMillis = 38 * MINUTE_MILLIS,
            averageSpeedKmh = 5.1,
            elevationGainMeters = 184,
            pointCount = 412,
        ),
        history = listOf(
            RecordedTrackUi(
                id = "preview-track-1",
                destinationName = destinations.first().name,
                startedAtEpochMillis = System.currentTimeMillis() - DAY_MILLIS,
                distanceMeters = 5_180,
                activeDurationMillis = 74 * MINUTE_MILLIS,
                averageSpeedKmh = 4.2,
                elevationGainMeters = 312,
            ),
            RecordedTrackUi(
                id = "preview-track-2",
                destinationName = destinations.getOrNull(1)?.name,
                startedAtEpochMillis = System.currentTimeMillis() - 3 * DAY_MILLIS,
                distanceMeters = 2_400,
                activeDurationMillis = 31 * MINUTE_MILLIS,
                averageSpeedKmh = 4.6,
                elevationGainMeters = 96,
            ),
        ),
    )

    fun composerState(): PostComposerUiState.Content {
        val destination = destinations.first()
        return PostComposerUiState.Content(
            destinations = destinations,
            selectedDestination = destination,
            caption = "Subida con neblina y el sendero mojado, pero la vista desde la cima compensa cada paso.",
            photo = SelectedPhotoUi(
                fileUri = destination.heroImageUrl,
                metadata = ExifMetadata(
                    latitude = destination.attraction.latitude,
                    longitude = destination.attraction.longitude,
                    capturedAtEpochMillis = System.currentTimeMillis() - 2 * DAY_MILLIS,
                ),
                check = ExifCheck(
                    verification = ExifVerification.VERIFIED_ON_SITE,
                    distanceMeters = 420,
                ),
            ),
            isBusy = false,
        )
    }

    private const val MINUTE_MILLIS = 60_000L
    private const val DAY_MILLIS = 86_400_000L
}
