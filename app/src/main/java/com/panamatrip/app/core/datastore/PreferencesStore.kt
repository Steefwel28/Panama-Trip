package com.panamatrip.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.panamatrip.app.domain.affinity.AffinityCodec
import com.panamatrip.app.domain.affinity.AffinitySnapshot
import com.panamatrip.app.domain.affinity.AffinityWeights
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.VehicleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Estado local del invitado: origen, filtro preferido, ruta activa y afinidad. */
data class UserPreferences(
    val guestMode: Boolean = true,
    val preferredVehicle: VehicleType? = null,
    val origin: GeoPoint = DEFAULT_ORIGIN,
    val activeRouteDestinationId: String? = null,
    val affinity: AffinitySnapshot = AffinitySnapshot(),
) {
    companion object {
        val DEFAULT_ORIGIN = GeoPoint(latitude = 8.9824, longitude = -79.5199)
    }
}

/**
 * Motor silencioso de afinidad persistido en DataStore. Cada interacción se
 * escribe de forma atómica para sobrevivir al cierre de la app sin cuenta.
 */
class PreferencesStore(private val dataStore: DataStore<Preferences>) {

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            guestMode = prefs[KEY_GUEST_MODE] ?: true,
            preferredVehicle = prefs[KEY_PREFERRED_VEHICLE]
                ?.let { runCatching { VehicleType.valueOf(it) }.getOrNull() },
            origin = readOrigin(prefs),
            activeRouteDestinationId = prefs[KEY_ACTIVE_ROUTE],
            affinity = AffinityCodec.decodeSnapshot(
                destinationScores = prefs[KEY_AFFINITY_DESTINATIONS],
                ecosystemScores = prefs[KEY_AFFINITY_ECOSYSTEMS],
                interactions = prefs[KEY_AFFINITY_INTERACTIONS] ?: 0,
            ),
        )
    }

    suspend fun setPreferredVehicle(vehicle: VehicleType?) {
        dataStore.edit { prefs ->
            if (vehicle == null) prefs.remove(KEY_PREFERRED_VEHICLE)
            else prefs[KEY_PREFERRED_VEHICLE] = vehicle.name
        }
    }

    suspend fun setActiveRoute(destinationId: String?) {
        dataStore.edit { prefs ->
            if (destinationId == null) prefs.remove(KEY_ACTIVE_ROUTE)
            else prefs[KEY_ACTIVE_ROUTE] = destinationId
        }
    }

    suspend fun recordInteraction(destinationId: String, ecosystem: Ecosystem, weight: Double) {
        dataStore.edit { prefs ->
            val current = AffinityCodec.decodeSnapshot(
                destinationScores = prefs[KEY_AFFINITY_DESTINATIONS],
                ecosystemScores = prefs[KEY_AFFINITY_ECOSYSTEMS],
                interactions = prefs[KEY_AFFINITY_INTERACTIONS] ?: 0,
            )
            val updated = current.withInteraction(destinationId, ecosystem, weight)
            prefs[KEY_AFFINITY_DESTINATIONS] = AffinityCodec.encode(updated.destinationScores)
            prefs[KEY_AFFINITY_ECOSYSTEMS] = AffinityCodec.encode(updated.ecosystemScores)
            prefs[KEY_AFFINITY_INTERACTIONS] = updated.totalInteractions
        }
    }

    suspend fun recordLike(destinationId: String, ecosystem: Ecosystem) =
        recordInteraction(destinationId, ecosystem, AffinityWeights.LIKE)

    suspend fun recordSave(destinationId: String, ecosystem: Ecosystem) =
        recordInteraction(destinationId, ecosystem, AffinityWeights.SAVE)

    suspend fun recordView(destinationId: String, ecosystem: Ecosystem) =
        recordInteraction(destinationId, ecosystem, AffinityWeights.VIEW)

    private fun readOrigin(prefs: Preferences): GeoPoint {
        val latitude = prefs[KEY_ORIGIN_LATITUDE]?.toDoubleOrNull()
            ?: return UserPreferences.DEFAULT_ORIGIN
        val longitude = prefs[KEY_ORIGIN_LONGITUDE]?.toDoubleOrNull()
            ?: return UserPreferences.DEFAULT_ORIGIN
        return GeoPoint(latitude, longitude)
    }

    private companion object {
        val KEY_GUEST_MODE = booleanPreferencesKey("guest_mode")
        val KEY_PREFERRED_VEHICLE = stringPreferencesKey("preferred_vehicle")
        val KEY_ORIGIN_LATITUDE = stringPreferencesKey("origin_latitude")
        val KEY_ORIGIN_LONGITUDE = stringPreferencesKey("origin_longitude")
        val KEY_ACTIVE_ROUTE = stringPreferencesKey("active_route_destination_id")
        val KEY_AFFINITY_DESTINATIONS = stringPreferencesKey("affinity_destinations")
        val KEY_AFFINITY_ECOSYSTEMS = stringPreferencesKey("affinity_ecosystems")
        val KEY_AFFINITY_INTERACTIONS = intPreferencesKey("affinity_interactions")
    }
}
