package com.panamatrip.app.features.profile.ui

import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.VehicleType

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
    ) : ProfileUiState

    data class Error(val error: UiError) : ProfileUiState
}
