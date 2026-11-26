package com.panamatrip.app.data.repository

import com.panamatrip.app.core.datastore.PreferencesStore
import com.panamatrip.app.core.datastore.UserPreferences
import com.panamatrip.app.domain.affinity.AffinitySnapshot
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesRepositoryImpl(
    private val store: PreferencesStore,
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = store.preferences

    override val affinity: Flow<AffinitySnapshot> = store.preferences.map { it.affinity }

    override val cashAlerts: Flow<Boolean> = store.preferences.map { it.cashAlertsEnabled }

    override suspend fun setPreferredVehicle(vehicle: VehicleType?) = store.setPreferredVehicle(vehicle)

    override suspend fun setActiveRoute(destinationId: String?) = store.setActiveRoute(destinationId)

    override suspend fun setCashAlertsEnabled(enabled: Boolean) = store.setCashAlertsEnabled(enabled)

    override suspend fun recordLike(destination: Destination) =
        store.recordLike(destination.id, destination.ecosystem)

    override suspend fun recordSave(destination: Destination) =
        store.recordSave(destination.id, destination.ecosystem)

    override suspend fun recordView(destination: Destination) =
        store.recordView(destination.id, destination.ecosystem)
}
