package com.panamatrip.app.domain.ranking

import com.panamatrip.app.domain.affinity.AffinitySnapshot
import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.VehicleAccessSummary
import com.panamatrip.app.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleTrafficLightTest {

    @Test
    fun `unanimous clear reports turn green`() {
        val summary = VehicleTrafficLight.summaryFor(
            VehicleType.SEDAN,
            listOf(AccessStatus.CLEAR, AccessStatus.CLEAR),
        )

        assertEquals(AccessStatus.CLEAR, summary.status)
        assertEquals(2, summary.reports)
    }

    @Test
    fun `blocked majority turns red`() {
        val summary = VehicleTrafficLight.summaryFor(
            VehicleType.SEDAN,
            listOf(AccessStatus.BLOCKED, AccessStatus.BLOCKED, AccessStatus.CLEAR),
        )

        assertEquals(AccessStatus.BLOCKED, summary.status)
    }

    @Test
    fun `mixed reports stay caution`() {
        val summary = VehicleTrafficLight.summaryFor(
            VehicleType.SUV,
            listOf(AccessStatus.CLEAR, AccessStatus.BLOCKED),
        )

        assertEquals(AccessStatus.CAUTION, summary.status)
    }

    @Test
    fun `no reports default to caution without community signal`() {
        val summary = VehicleTrafficLight.summaryFor(VehicleType.FOUR_BY_FOUR, emptyList())

        assertEquals(AccessStatus.CAUTION, summary.status)
        assertEquals(0, summary.reports)
        assertFalse(summary.hasCommunitySignal)
    }

    @Test
    fun `aggregate groups by vehicle`() {
        val aggregation = VehicleTrafficLight.aggregate(
            listOf(
                VehicleTrafficLight.Report(VehicleType.SEDAN, AccessStatus.BLOCKED),
                VehicleTrafficLight.Report(VehicleType.SUV, AccessStatus.CLEAR),
                VehicleTrafficLight.Report(VehicleType.SUV, AccessStatus.CLEAR),
            ),
        )

        assertEquals(AccessStatus.BLOCKED, aggregation.getValue(VehicleType.SEDAN).status)
        assertEquals(AccessStatus.CLEAR, aggregation.getValue(VehicleType.SUV).status)
    }
}

class FeedRankerTest {

    @Test
    fun `affinity orders the feed before recency`() {
        val now = 1_000_000L
        val preferred = post(id = "post-b", destinationId = "dest-b", createdAt = now - 10_000)
        val recent = post(id = "post-a", destinationId = "dest-a", createdAt = now)
        val affinity = AffinitySnapshot(
            destinationScores = mapOf("dest-b" to 5.0),
            totalInteractions = 1,
        )

        val ranked = FeedRanker.rank(listOf(recent, preferred), affinity, now)

        assertEquals("post-b", ranked.first().id)
    }

    @Test
    fun `equal affinity keeps the newest first`() {
        val now = 1_000_000L
        val older = post(id = "post-a", destinationId = "dest-a", createdAt = now - 90_000)
        val newest = post(id = "post-b", destinationId = "dest-b", createdAt = now)

        val ranked = FeedRanker.rank(listOf(older, newest), AffinitySnapshot(), now)

        assertEquals("post-b", ranked.first().id)
    }

    private fun post(id: String, destinationId: String, createdAt: Long): CommunityPost =
        CommunityPost(
            id = id,
            destination = destination(destinationId),
            authorName = "Editor",
            authorRole = "Editor/Curador",
            authorInitials = "EC",
            caption = "",
            imageUrl = "",
            visitedYear = 2026,
            visitedMonth = 10,
            vehicleUsed = VehicleType.SUV,
            walkDistanceMeters = 1000,
            walkMinutes = 20,
            activityTag = null,
            likeCount = 0,
            isLiked = false,
            commentCount = 0,
            savedCollectionIds = emptySet(),
            createdAt = createdAt,
        )

    private fun destination(id: String): Destination = Destination(
        id = id,
        name = id,
        province = "Panamá",
        ecosystem = Ecosystem.MOUNTAIN,
        summary = "",
        description = "",
        accessNotes = "",
        parking = GeoPoint(8.0, -80.0),
        attraction = GeoPoint(8.1, -80.1),
        heroImageUrl = "",
        walkDistanceMeters = 1000,
        walkMinutes = 20,
        elevationGainMeters = 50,
        recommendedVehicle = VehicleType.SUV,
        vehicleAccess = emptyMap(),
        averageRating = 0.0,
        reviewCount = 0,
        featuredOrder = 1,
    )
}

class DirectoryFilterTest {

    @Test
    fun `vehicle filter hides destinations blocked for that vehicle`() {
        val suvOnly = entry("suv-only", vehicleAccess = mapOf(
            VehicleType.SEDAN to VehicleAccessSummary(VehicleType.SEDAN, AccessStatus.BLOCKED, 2),
            VehicleType.SUV to VehicleAccessSummary(VehicleType.SUV, AccessStatus.CLEAR, 2),
        ))
        val allAccess = entry("all", vehicleAccess = emptyMap(), recommendedVehicle = VehicleType.SEDAN)

        val filtered = DirectoryFilter.apply(
            listOf(suvOnly, allAccess),
            DirectoryFilters(vehicle = VehicleType.SEDAN),
        )

        assertEquals(listOf("all"), filtered.map { it.destination.id })
    }

    @Test
    fun `distance and ecosystem filters combine`() {
        val near = entry("near", distanceMeters = 5_000, ecosystem = Ecosystem.WATERFALL)
        val far = entry("far", distanceMeters = 80_000, ecosystem = Ecosystem.WATERFALL)
        val otherEcosystem = entry("other", distanceMeters = 5_000)

        val filtered = DirectoryFilter.apply(
            listOf(near, far, otherEcosystem),
            DirectoryFilters(ecosystem = Ecosystem.WATERFALL, maxDistanceMeters = 15_000),
        )

        assertEquals(listOf("near"), filtered.map { it.destination.id })
    }

    @Test
    fun `directory is sorted by distance`() {
        val far = entry("far", distanceMeters = 40_000)
        val near = entry("near", distanceMeters = 2_000)

        val filtered = DirectoryFilter.apply(listOf(far, near), DirectoryFilters())

        assertTrue(filtered.first().distanceMeters < filtered.last().distanceMeters)
    }

    private fun entry(
        id: String,
        distanceMeters: Int = 1_000,
        ecosystem: Ecosystem = Ecosystem.MOUNTAIN,
        vehicleAccess: Map<VehicleType, VehicleAccessSummary> = emptyMap(),
        recommendedVehicle: VehicleType = VehicleType.SUV,
    ): DestinationWithDistance = DestinationWithDistance(
        destination = Destination(
            id = id,
            name = id,
            province = "Panamá",
            ecosystem = ecosystem,
            summary = "",
            description = "",
            accessNotes = "",
            parking = GeoPoint(8.0, -80.0),
            attraction = GeoPoint(8.1, -80.1),
            heroImageUrl = "",
            walkDistanceMeters = 1000,
            walkMinutes = 20,
            elevationGainMeters = 50,
            recommendedVehicle = recommendedVehicle,
            vehicleAccess = vehicleAccess,
            averageRating = 0.0,
            reviewCount = 0,
            featuredOrder = 1,
        ),
        distanceMeters = distanceMeters,
    )
}
