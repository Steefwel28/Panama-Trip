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

/**
 * Estado local del invitado: origen, filtro preferido, ruta activa, afinidad y
 * perfil offline-first. `guestMode` es la única fuente de verdad del invitado:
 * el onboarding escribe aquí, no en una preferencia paralela.
 */
data class UserPreferences(
    val guestMode: Boolean = true,
    val preferredVehicle: VehicleType? = null,
    val origin: GeoPoint = DEFAULT_ORIGIN,
    val activeRouteDestinationId: String? = null,
    val cashAlertsEnabled: Boolean = true,
    val affinity: AffinitySnapshot = AffinitySnapshot(),
    val hasCompletedOnboarding: Boolean = false,
    val userName: String = "",
    val userHandle: String = "",
    val userBio: String = "",
    val avatarPath: String? = null,
    val bannerPath: String? = null,
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
            cashAlertsEnabled = prefs[KEY_CASH_ALERTS] ?: true,
            affinity = AffinityCodec.decodeSnapshot(
                destinationScores = prefs[KEY_AFFINITY_DESTINATIONS],
                ecosystemScores = prefs[KEY_AFFINITY_ECOSYSTEMS],
                interactions = prefs[KEY_AFFINITY_INTERACTIONS] ?: 0,
            ),
            hasCompletedOnboarding = prefs[KEY_ONBOARDING_DONE] ?: false,
            userName = prefs[KEY_USER_NAME].orEmpty(),
            userHandle = prefs[KEY_USER_HANDLE].orEmpty(),
            userBio = prefs[KEY_USER_BIO].orEmpty(),
            avatarPath = prefs[KEY_AVATAR_PATH],
            bannerPath = prefs[KEY_BANNER_PATH],
        )
    }

    /**
     * Cierra el onboarding en una sola escritura atómica. Cancelar la pantalla
     * no llama aquí, así que `hasCompletedOnboarding` sigue en false.
     */
    suspend fun completeOnboarding(
        name: String,
        handle: String,
        bio: String,
        guest: Boolean,
    ) {
        dataStore.edit { prefs ->
            if (guest) {
                prefs[KEY_GUEST_MODE] = true
            } else {
                prefs[KEY_GUEST_MODE] = false
                prefs[KEY_USER_NAME] = name.trim()
                prefs[KEY_USER_HANDLE] = normalizeHandle(handle)
                prefs[KEY_USER_BIO] = bio.trim()
            }
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    /**
     * «Iniciar sesión» local: recupera el perfil guardado en este teléfono.
     * Devuelve false si nunca hubo un perfil registrado.
     */
    suspend fun signInLocal(): Boolean {
        var found = false
        dataStore.edit { prefs ->
            found = !prefs[KEY_USER_HANDLE].isNullOrBlank()
            if (found) prefs[KEY_GUEST_MODE] = false
        }
        return found
    }

    suspend fun enterAsGuest() {
        dataStore.edit { prefs ->
            prefs[KEY_GUEST_MODE] = true
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun updateProfile(name: String, handle: String, bio: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name.trim()
            prefs[KEY_USER_HANDLE] = normalizeHandle(handle)
            prefs[KEY_USER_BIO] = bio.trim()
        }
    }

    suspend fun setAvatarPath(path: String?) {
        dataStore.edit { prefs ->
            if (path == null) prefs.remove(KEY_AVATAR_PATH) else prefs[KEY_AVATAR_PATH] = path
        }
    }

    suspend fun setBannerPath(path: String?) {
        dataStore.edit { prefs ->
            if (path == null) prefs.remove(KEY_BANNER_PATH) else prefs[KEY_BANNER_PATH] = path
        }
    }

    private fun normalizeHandle(raw: String): String {
        val trimmed = raw.trim().removePrefix("@").lowercase()
        return if (trimmed.isEmpty()) "" else "@$trimmed"
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

    suspend fun setCashAlertsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_CASH_ALERTS] = enabled }
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
        val KEY_CASH_ALERTS = booleanPreferencesKey("cash_alerts_enabled")
        val KEY_AFFINITY_DESTINATIONS = stringPreferencesKey("affinity_destinations")
        val KEY_AFFINITY_ECOSYSTEMS = stringPreferencesKey("affinity_ecosystems")
        val KEY_AFFINITY_INTERACTIONS = intPreferencesKey("affinity_interactions")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("has_completed_onboarding")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_HANDLE = stringPreferencesKey("user_handle")
        val KEY_USER_BIO = stringPreferencesKey("user_bio")
        val KEY_AVATAR_PATH = stringPreferencesKey("avatar_path")
        val KEY_BANNER_PATH = stringPreferencesKey("banner_path")
    }
}
