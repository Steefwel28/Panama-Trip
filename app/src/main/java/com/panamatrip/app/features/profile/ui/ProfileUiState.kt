package com.panamatrip.app.features.profile.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.passport.PassportBadge
import com.panamatrip.app.domain.profile.OdometerTier

enum class ProfileTab {
    POSTS,
    ROUTES,
    SAVED,
    LIKES,
}

data class ExpeditionStats(
    val totalMeters: Int,
    val elevationGainMeters: Int,
    val completedRoutes: Int,
) {
    val totalKilometers: Double get() = totalMeters / METERS_PER_KILOMETER

    private companion object {
        const val METERS_PER_KILOMETER = 1_000.0
    }
}

/** Pasaporte resumido para la UI: regiones desbloqueadas e insignias. */
data class PassportUi(
    val unlockedRegionCodes: Set<String>,
    val badges: Set<PassportBadge>,
    val unlockedRegions: Int,
    val totalRegions: Int,
)

/** Porción de afinidad del usuario por ecosistema (suma 1.0). */
data class EcosystemShareUi(
    val ecosystem: com.panamatrip.app.domain.model.Ecosystem,
    val share: Float,
)

data class ProfileRouteUi(
    val id: String,
    val name: String,
    val distanceMeters: Int,
    val elevationGainMeters: Int,
    val averageSpeedKmh: Double,
    val finishedAtEpochMillis: Long,
)

sealed interface ProfileUiState {

    data object Loading : ProfileUiState

    data class Content(
        val stats: ExpeditionStats,
        val tab: ProfileTab,
        val posts: List<CommunityPost>,
        val routes: List<ProfileRouteUi>,
        val likes: List<CommunityPost>,
        val preferredVehicle: VehicleType?,
        val cashAlertsEnabled: Boolean,
        val guestMode: Boolean,
        val userName: String = "",
        val userHandle: String = "",
        val userBio: String = "",
        val avatarPath: String? = null,
        val bannerPath: String? = null,
        val odometerMeters: Int = 0,
        val odometerTier: OdometerTier = OdometerTier.PRIMER_PASO,
        val metersToNextTier: Int = 0,
        val ecosystems: List<EcosystemShareUi> = emptyList(),
        val passport: PassportUi = PassportUi(
            unlockedRegionCodes = emptySet(),
            badges = emptySet(),
            unlockedRegions = 0,
            totalRegions = 0,
        ),
    ) : ProfileUiState

    data class Error(val error: UiError) : ProfileUiState
}
