package com.panamatrip.app.features.guides.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.repository.GuideRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class GuidesViewModel(
    private val guideRepository: GuideRepository,
) : ViewModel() {

    private val selectedDestinationId = MutableStateFlow<String?>(null)
    private val retryTrigger = MutableStateFlow(0)

    val state: StateFlow<GuidesUiState> = retryTrigger.flatMapLatest {
        combine(
            guideRepository.observeGuides(),
            selectedDestinationId,
        ) { guides, selectedId ->
            if (guides.isEmpty()) {
                GuidesUiState.Empty
            } else {
                GuidesUiState.Content(
                    guides = guides.filter { guide ->
                        selectedId == null || guide.destinationId == selectedId
                    },
                    filters = guides
                        .map { guide ->
                            GuideDestinationFilterUi(
                                id = guide.destinationId,
                                name = guide.destinationName,
                            )
                        }
                        .distinctBy { it.id }
                        .sortedBy { it.name },
                    selectedDestinationId = selectedId,
                    totalCount = guides.size,
                )
            }
        }.catch { emit(GuidesUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = GuidesUiState.Loading,
    )

    fun selectDestination(destinationId: String?) {
        selectedDestinationId.value = destinationId
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GuidesViewModel(guideRepository = container.guideRepository)
            }
        }
    }
}
