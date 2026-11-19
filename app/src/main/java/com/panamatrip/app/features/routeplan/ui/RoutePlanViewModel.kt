package com.panamatrip.app.features.routeplan.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.geo.GeoCalculator
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RoutePlanUiState {

    data object Loading : RoutePlanUiState

    data class Content(
        val destination: Destination,
        val originDistanceMeters: Int,
        val originDriveMinutes: Int,
    ) : RoutePlanUiState

    data object Empty : RoutePlanUiState

    data class Error(val error: UiError) : RoutePlanUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoutePlanViewModel(
    savedStateHandle: SavedStateHandle,
    private val destinationRepository: DestinationRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val requestedDestinationId: String? =
        savedStateHandle.get<String>(ARG_DESTINATION_ID)?.takeIf { it.isNotBlank() }
    private val retryTrigger = MutableStateFlow(0)

    val state: StateFlow<RoutePlanUiState> = retryTrigger.flatMapLatest {
        combine(
            destinationRepository.observeDestinations(),
            preferencesRepository.preferences,
        ) { destinations, preferences ->
            val destinationId = requestedDestinationId ?: preferences.activeRouteDestinationId
            val destination = destinations.firstOrNull { it.id == destinationId }
            if (destination == null) {
                RoutePlanUiState.Empty
            } else {
                val distance = GeoCalculator.distanceMeters(preferences.origin, destination.parking)
                RoutePlanUiState.Content(
                    destination = destination,
                    originDistanceMeters = distance,
                    originDriveMinutes = GeoCalculator.estimatedDriveMinutes(distance),
                )
            }
        }.catch { emit(RoutePlanUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RoutePlanUiState.Loading,
    )

    init {
        requestedDestinationId?.let(::persistRoute)
    }

    fun persistRoute(destinationId: String) {
        viewModelScope.launch {
            runCatching { preferencesRepository.setActiveRoute(destinationId) }
        }
    }

    fun clearRoute() {
        viewModelScope.launch {
            runCatching { preferencesRepository.setActiveRoute(null) }
        }
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    companion object {
        const val ARG_DESTINATION_ID = "destinationId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RoutePlanViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    destinationRepository = container.destinationRepository,
                    preferencesRepository = container.preferencesRepository,
                )
            }
        }
    }
}
