package com.panamatrip.app.data.repository

import com.panamatrip.app.core.database.dao.DestinationDao
import com.panamatrip.app.core.database.dao.ReviewDao
import com.panamatrip.app.core.database.dao.VehicleReportDao
import com.panamatrip.app.data.mapper.toDomain
import com.panamatrip.app.data.mapper.toTrafficLightReport
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.VehicleAccessSummary
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.ranking.VehicleReportFreshness
import com.panamatrip.app.domain.ranking.VehicleTrafficLight
import com.panamatrip.app.domain.repository.DestinationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class DestinationRepositoryImpl(
    private val destinationDao: DestinationDao,
    private val vehicleReportDao: VehicleReportDao,
    private val reviewDao: ReviewDao,
) : DestinationRepository {

    override fun observeDestinations(): Flow<List<Destination>> = combine(
        destinationDao.observeAll(),
        vehicleReportDao.observeAll(),
        reviewDao.observeAll(),
        clockTicker,
    ) { destinations, reports, reviews, now ->
        val freshReports = reports.filter { report ->
            VehicleReportFreshness.isFresh(report.createdAt, now)
        }
        val accessByDestination: Map<String, Map<VehicleType, VehicleAccessSummary>> =
            freshReports.groupBy { it.destinationId }
                .mapValues { (_, destinationReports) ->
                    VehicleTrafficLight.aggregate(destinationReports.map { it.toTrafficLightReport() })
                }
        val reviewsByDestination = reviews
            .groupBy { it.destinationId }
            .mapValues { (_, destinationReviews) -> destinationReviews.map { it.toDomain() } }

        destinations.map { entity ->
            entity.toDomain(
                access = accessByDestination[entity.id].orEmpty(),
                reviews = reviewsByDestination[entity.id].orEmpty(),
            )
        }
    }

    override fun observeDestination(id: String): Flow<Destination?> =
        observeDestinations().map { destinations -> destinations.firstOrNull { it.id == id } }

    override fun observeReviews(destinationId: String): Flow<List<DestinationReview>> =
        reviewDao.observeForDestination(destinationId).map { reviews -> reviews.map { it.toDomain() } }

    /**
     * Emite cada minuto para que la caducidad de 48 h sea en vivo: un reporte
     * deja de pesar en el semáforo sin reiniciar la app.
     */
    private val clockTicker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(EXPIRY_TICK_MILLIS)
        }
    }

    private companion object {
        const val EXPIRY_TICK_MILLIS = 60_000L
    }
}
