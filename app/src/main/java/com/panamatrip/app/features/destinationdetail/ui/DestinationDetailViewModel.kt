package com.panamatrip.app.features.destinationdetail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DestinationDetailUiState {

    data object Loading : DestinationDetailUiState

    data class Content(
        val destination: Destination,
        val reviews: List<DestinationReview>,
    ) : DestinationDetailUiState

    data object Empty : DestinationDetailUiState

    data class Error(val error: UiError) : DestinationDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class DestinationDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val destinationRepository: DestinationRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val destinationId: String = savedStateHandle.get<String>(ARG_DESTINATION_ID).orEmpty()
    private val retryTrigger = MutableStateFlow(0)

    val state: StateFlow<DestinationDetailUiState> = retryTrigger.flatMapLatest {
        if (destinationId.isBlank()) {
            kotlinx.coroutines.flow.flowOf(DestinationDetailUiState.Empty)
        } else {
            combine(
                destinationRepository.observeDestination(destinationId),
                destinationRepository.observeReviews(destinationId),
            ) { destination, reviews ->
                if (destination == null) {
                    DestinationDetailUiState.Empty
                } else {
                    DestinationDetailUiState.Content(destination = destination, reviews = reviews)
                }
            }.catch { emit(DestinationDetailUiState.Error(UiError.DATABASE)) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DestinationDetailUiState.Loading,
    )

    init {
        viewModelScope.launch {
            runCatching {
                val destination = destinationRepository.observeDestination(destinationId)
                    .filterNotNull()
                    .first()
                preferencesRepository.recordView(destination)
            }
        }
    }

    fun rememberRoute(destination: Destination) {
        viewModelScope.launch {
            runCatching { preferencesRepository.setActiveRoute(destination.id) }
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
                DestinationDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    destinationRepository = container.destinationRepository,
                    preferencesRepository = container.preferencesRepository,
                )
            }
        }
    }
}
