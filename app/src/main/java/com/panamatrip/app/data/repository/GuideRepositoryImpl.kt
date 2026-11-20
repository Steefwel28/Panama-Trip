package com.panamatrip.app.data.repository

import com.panamatrip.app.core.database.dao.GuideDao
import com.panamatrip.app.data.mapper.toDomain
import com.panamatrip.app.domain.model.LocalGuide
import com.panamatrip.app.domain.repository.GuideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GuideRepositoryImpl(
    private val guideDao: GuideDao,
) : GuideRepository {

    override fun observeGuides(): Flow<List<LocalGuide>> =
        guideDao.observeAll().map { rows -> rows.map { it.toDomain() } }
}
