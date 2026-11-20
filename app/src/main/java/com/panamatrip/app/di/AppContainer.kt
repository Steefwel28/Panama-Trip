package com.panamatrip.app.di

import android.content.Context
import com.panamatrip.app.core.database.DatabaseSeeder
import com.panamatrip.app.core.database.PanamaTripDatabase
import com.panamatrip.app.core.datastore.PreferencesStore
import com.panamatrip.app.core.datastore.panamaTripPreferences
import com.panamatrip.app.data.repository.CollectionRepositoryImpl
import com.panamatrip.app.data.repository.CommunityRepositoryImpl
import com.panamatrip.app.data.repository.DestinationRepositoryImpl
import com.panamatrip.app.data.repository.GuideRepositoryImpl
import com.panamatrip.app.data.repository.PreferencesRepositoryImpl
import com.panamatrip.app.data.repository.RoutePlanRepositoryImpl
import com.panamatrip.app.domain.repository.CollectionRepository
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.GuideRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import com.panamatrip.app.domain.repository.RoutePlanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyección manual: una sola instancia de Room, DataStore y repositorios por
 * proceso. Sin frameworks extra para mantener la v0.1 auditable.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: PanamaTripDatabase by lazy { PanamaTripDatabase.build(appContext) }

    private val preferencesStore: PreferencesStore by lazy {
        PreferencesStore(appContext.panamaTripPreferences)
    }

    val destinationRepository: DestinationRepository by lazy {
        DestinationRepositoryImpl(
            destinationDao = database.destinationDao(),
            vehicleReportDao = database.vehicleReportDao(),
            reviewDao = database.reviewDao(),
        )
    }

    val communityRepository: CommunityRepository by lazy {
        CommunityRepositoryImpl(
            postDao = database.postDao(),
            collectionDao = database.collectionDao(),
            destinationRepository = destinationRepository,
        )
    }

    val collectionRepository: CollectionRepository by lazy {
        CollectionRepositoryImpl(
            collectionDao = database.collectionDao(),
            postDao = database.postDao(),
        )
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepositoryImpl(preferencesStore)
    }

    val routePlanRepository: RoutePlanRepository by lazy {
        RoutePlanRepositoryImpl(routePlanDao = database.routePlanDao())
    }

    val guideRepository: GuideRepository by lazy {
        GuideRepositoryImpl(guideDao = database.guideDao())
    }

    fun warmUp() {
        applicationScope.launch {
            runCatching { DatabaseSeeder(database).seedIfEmpty() }
        }
    }
}
