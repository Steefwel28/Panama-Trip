package com.panamatrip.app.data.repository

import com.panamatrip.app.core.database.dao.RoutePlanDao
import com.panamatrip.app.data.mapper.toDomain
import com.panamatrip.app.data.mapper.toEntity
import com.panamatrip.app.domain.logistics.SavedRoutePlan
import com.panamatrip.app.domain.repository.RoutePlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutePlanRepositoryImpl(
    private val routePlanDao: RoutePlanDao,
) : RoutePlanRepository {

    override fun observeSavedPlans(destinationId: String, limit: Int): Flow<List<SavedRoutePlan>> =
        routePlanDao.observeRecentForDestination(destinationId, limit)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(plan: SavedRoutePlan) {
        routePlanDao.insert(plan.toEntity())
    }
}
