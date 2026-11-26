package com.panamatrip.app.features.profile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.data.photo.PhotoInspector
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.TrackStatus
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.passport.HikeEvidence
import com.panamatrip.app.domain.passport.PassportEvaluator
import com.panamatrip.app.domain.profile.OdometerComparator
import com.panamatrip.app.domain.repository.CommunityRepository
import com.panamatrip.app.domain.repository.DestinationRepository
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
    private val destinationRepository: DestinationRepository,
    private val photoInspector: PhotoInspector,
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
            destinationRepository.observeDestinations(),
        ) { posts, tracks, preferences, tab, destinations ->
            val completed = tracks.filter { it.status == TrackStatus.COMPLETED }
            // Los tracks históricos sin evidencia de velocidad cuentan su
            // distancia completa: no se infla ni se borra el historial.
            val odometerMeters = completed.sumOf { track ->
                if (track.evidenceAvailable) {
                    track.validWalkingDistanceMeters
                } else {
                    track.distanceMeters
                }
            }
            val provinceById = destinations.associate { it.id to it.province }
            val hikes = completed.map { track ->
                HikeEvidence(
                    province = track.destinationId?.let(provinceById::get),
                    startedAtEpochMillis = track.startedAtEpochMillis,
                    elevationGainMeters = track.elevationGainMeters,
                )
            }
            val passport = PassportEvaluator.evaluate(hikes, odometerMeters)
            val odometerKm = odometerMeters / METERS_PER_KILOMETER
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
                userName = preferences.userName,
                userHandle = preferences.userHandle,
                userBio = preferences.userBio,
                avatarPath = preferences.avatarPath,
                bannerPath = preferences.bannerPath,
                odometerMeters = odometerMeters,
                odometerTier = OdometerComparator.tierFor(odometerKm),
                metersToNextTier = OdometerComparator.metersToNextTier(odometerKm),
                ecosystems = ecosystemShares(preferences.affinity.ecosystemScores),
                passport = PassportUi(
                    unlockedRegionCodes = passport.unlockedRegionCodes,
                    badges = passport.badges,
                    unlockedRegions = passport.unlockedRegions,
                    totalRegions = passport.totalRegions,
                ),
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

    /** Copia la imagen al almacenamiento interno y la fija como avatar. */
    fun setAvatar(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val prepared = photoInspector.preparePhoto(uri)
                preferencesRepository.setAvatarPath(prepared.fileUri)
            }
        }
    }

    /** Copia la imagen al almacenamiento interno y la fija como portada. */
    fun setBanner(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val prepared = photoInspector.preparePhoto(uri)
                preferencesRepository.setBannerPath(prepared.fileUri)
            }
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

    /** Reparte la afinidad por ecosistema en porciones que suman 1.0. */
    private fun ecosystemShares(scores: Map<String, Double>): List<EcosystemShareUi> {
        val ranked = scores.entries
            .mapNotNull { (key, score) ->
                val ecosystem = Ecosystem.entries.firstOrNull { it.name == key }
                if (ecosystem == null || score <= 0.0) null else ecosystem to score
            }
            .sortedByDescending { (_, score) -> score }
            .take(MAX_ECOSYSTEM_SLICES)
        val total = ranked.sumOf { (_, score) -> score }
        if (total <= 0.0) return emptyList()
        return ranked.map { (ecosystem, score) ->
            EcosystemShareUi(ecosystem = ecosystem, share = (score / total).toFloat())
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val METERS_PER_KILOMETER = 1_000.0
        private const val MAX_ECOSYSTEM_SLICES = 4

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    communityRepository = container.communityRepository,
                    trackRepository = container.trackRepository,
                    preferencesRepository = container.preferencesRepository,
                    destinationRepository = container.destinationRepository,
                    photoInspector = container.photoInspector,
                    guestAuthorName = container.guestAuthorName,
                )
            }
        }
    }
}
