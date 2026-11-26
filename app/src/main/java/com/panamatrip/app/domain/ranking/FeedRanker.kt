package com.panamatrip.app.domain.ranking

import com.panamatrip.app.domain.model.AccessStatus
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.VehicleAccessSummary
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.affinity.AffinitySnapshot
import com.panamatrip.app.domain.affinity.AffinityWeights
import kotlin.math.max

/** Orden del feed: afinidad del invitado, luego frescura del contenido. */
object FeedRanker {

    private const val RECENCY_WINDOW_DAYS = 30.0
    private const val MILLIS_PER_DAY = 86_400_000.0

    fun rank(posts: List<CommunityPost>, affinity: AffinitySnapshot, now: Long): List<CommunityPost> =
        posts.sortedByDescending { score(it, affinity, now) }

    private fun score(post: CommunityPost, affinity: AffinitySnapshot, now: Long): Double {
        val affinityScore = affinity.scoreFor(post.destination)
        val ageDays = ((now - post.createdAt).coerceAtLeast(0) / MILLIS_PER_DAY)
            .coerceAtMost(RECENCY_WINDOW_DAYS)
        val recency = AffinityWeights.RECENCY_FACTOR * (1.0 - ageDays / RECENCY_WINDOW_DAYS)
        return affinityScore + recency
    }
}

data class DestinationWithDistance(
    val destination: Destination,
    val distanceMeters: Int,
)

/** Filtros del directorio: vehículo, ecosistema y radio de distancia. */
data class DirectoryFilters(
    val vehicle: VehicleType? = null,
    val ecosystem: Ecosystem? = null,
    val maxDistanceMeters: Int? = null,
) {
    val isActive: Boolean get() = vehicle != null || ecosystem != null || maxDistanceMeters != null

    fun toggleVehicle(value: VehicleType): DirectoryFilters =
        copy(vehicle = if (vehicle == value) null else value)

    fun toggleEcosystem(value: Ecosystem): DirectoryFilters =
        copy(ecosystem = if (ecosystem == value) null else value)

    fun toggleDistance(limitMeters: Int): DirectoryFilters =
        copy(maxDistanceMeters = if (maxDistanceMeters == limitMeters) null else limitMeters)
}

object DirectoryFilter {

    fun apply(
        items: List<DestinationWithDistance>,
        filters: DirectoryFilters,
    ): List<DestinationWithDistance> = items
        .filter { item ->
            val vehicleOk = filters.vehicle?.let { item.destination.isReachableBy(it) } ?: true
            val ecosystemOk = filters.ecosystem?.let { it == item.destination.ecosystem } ?: true
            val distanceOk = filters.maxDistanceMeters?.let { item.distanceMeters <= it } ?: true
            vehicleOk && ecosystemOk && distanceOk
        }
        .sortedBy { it.distanceMeters }
}

/**
 * Un destino es alcanzable con un vehículo cuando la comunidad lo confirma o,
 * sin reportes, cuando el vehículo iguala la capacidad recomendada.
 */
fun Destination.isReachableBy(vehicle: VehicleType): Boolean {
    val summary = vehicleAccess[vehicle]
    if (summary != null && summary.hasCommunitySignal) {
        return summary.status != AccessStatus.BLOCKED
    }
    return vehicle.capabilityRank >= recommendedVehicle.capabilityRank
}

private val VehicleType.capabilityRank: Int
    get() = when (this) {
        VehicleType.SEDAN -> 0
        VehicleType.SUV -> 1
        VehicleType.FOUR_BY_FOUR -> 2
    }

/** Semáforo comunitario agregado a partir de reportes individuales. */
object VehicleTrafficLight {

    private const val CLEAR_THRESHOLD = 0.25
    private const val BLOCKED_THRESHOLD = -0.25

    data class Report(val vehicle: VehicleType, val verdict: AccessStatus)

    fun aggregate(reports: List<Report>): Map<VehicleType, VehicleAccessSummary> =
        reports.groupBy { it.vehicle }
            .mapValues { (vehicle, group) -> summaryFor(vehicle, group.map { it.verdict }) }

    fun summaryFor(vehicle: VehicleType, verdicts: List<AccessStatus>): VehicleAccessSummary {
        if (verdicts.isEmpty()) {
            return VehicleAccessSummary(vehicle, AccessStatus.CAUTION, reports = 0)
        }
        // Un cierre reportado manda sobre el promedio: el destino no está apto.
        if (verdicts.any { it == AccessStatus.CLOSED }) {
            return VehicleAccessSummary(vehicle, AccessStatus.CLOSED, reports = verdicts.size)
        }
        val average = verdicts.sumOf { verdictScore(it) }.toDouble() / verdicts.size
        val status = when {
            average >= CLEAR_THRESHOLD -> AccessStatus.CLEAR
            average <= BLOCKED_THRESHOLD -> AccessStatus.BLOCKED
            else -> AccessStatus.CAUTION
        }
        return VehicleAccessSummary(vehicle, status, reports = verdicts.size)
    }

    private fun verdictScore(status: AccessStatus): Int = when (status) {
        AccessStatus.CLEAR -> 1
        AccessStatus.CAUTION -> 0
        AccessStatus.BLOCKED -> -1
        AccessStatus.CLOSED -> 0
    }
}

/** Promedio de reseñas con redondeo estable a un decimal para la UI. */
object RatingCalculator {

    fun average(reviews: List<DestinationReview>): Double {
        if (reviews.isEmpty()) return 0.0
        val sum = reviews.sumOf { it.rating.toDouble() }
        return max(0.0, sum / reviews.size).let { kotlin.math.round(it * 10) / 10.0 }
    }
}
