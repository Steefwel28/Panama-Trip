package com.panamatrip.app.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import com.panamatrip.app.domain.repository.TrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val communityRepository: CommunityRepository,
    private val trackRepository: TrackRepository,
    private val preferencesRepository: PreferencesRepository,
    private val guestAuthorName: String,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(ProfileTab.POSTS)
    private val retryTrigger = MutableStateFlow(0)

    val state: StateFlow<ProfileUiState> = retryTrigger.flatMapLatest {
        val flow: kotlinx.coroutines.flow.Flow<ProfileUiState> = combine(
            communityRepository.observePosts(),
            trackRepository.observeTracks(),
            preferencesRepository.preferences,
            selectedTab,
        ) { posts, tracks, preferences, tab ->
            val completed = tracks.filter { it.status == TrackStatus.COMPLETED }
            ProfileUiState.Content(
                stats = ExpeditionStats(
                    totalMeters = completed.sumOf { it.distanceMeters },
                    elevationGainMeters = completed.sumOf { it.elevationGainMeters },
                    completedRoutes = completed.size,
                ),
                tab = tab,
                posts = posts
                    .filter { it.authorName == guestAuthorName }
                    .sortedByDescending { it.createdAt },
                routes = completed.map { track ->
                    ProfileRouteUi(
                        id = track.id,
                        name = track.destinationName.orEmpty(),
                        distanceMeters = track.distanceMeters,
                        elevationGainMeters = track.elevationGainMeters,
                        averageSpeedKmh = track.averageSpeedKmh,
                        finishedAtEpochMillis = track.finishedAtEpochMillis,
                    )
                },
                likes = posts.filter { it.isLiked }.sortedByDescending { it.createdAt },
                preferredVehicle = preferences.preferredVehicle,
                cashAlertsEnabled = preferences.cashAlertsEnabled,
                guestMode = preferences.guestMode,
            )
        }
        flow.catch { emit(ProfileUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ProfileUiState.Loading,
    )

    fun selectTab(tab: ProfileTab) {
        selectedTab.value = tab
    }

    fun setPreferredVehicle(vehicle: VehicleType) {
        viewModelScope.launch {
            runCatching { preferencesRepository.setPreferredVehicle(vehicle) }
        }
    }

    fun setCashAlerts(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { preferencesRepository.setCashAlertsEnabled(enabled) }
        }
    }

    fun toggleLike(post: CommunityPost) {
        viewModelScope.launch {
            runCatching {
                communityRepository.toggleLike(post.id)
                if (!post.isLiked) preferencesRepository.recordLike(post.destination)
            }
        }
    }

    fun retry() {
        retryTrigger.value += 1
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    communityRepository = container.communityRepository,
                    trackRepository = container.trackRepository,
                    preferencesRepository = container.preferencesRepository,
                    guestAuthorName = container.guestAuthorName,
                )
            }
        }
    }
}
