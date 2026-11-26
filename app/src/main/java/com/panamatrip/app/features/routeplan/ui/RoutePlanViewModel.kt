package com.panamatrip.app.features.routeplan.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.core.datastore.UserPreferences
import com.panamatrip.app.core.ui.UiError
import com.panamatrip.app.core.ui.UiNotice
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.logistics.FuelType
import com.panamatrip.app.domain.logistics.LogisticsEngine
import com.panamatrip.app.domain.logistics.LogisticsInput
import com.panamatrip.app.domain.logistics.SavedRoutePlan
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import com.panamatrip.app.domain.repository.RoutePlanRepository
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RoutePlanViewModel(
    savedStateHandle: SavedStateHandle,
    private val destinationRepository: DestinationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val routePlanRepository: RoutePlanRepository,
    private val locationSource: suspend () -> GeoPoint? = { null },
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val requestedDestinationId: String? =
        savedStateHandle.get<String>(ARG_DESTINATION_ID)?.takeIf { it.isNotBlank() }
    private val retryTrigger = MutableStateFlow(0)
    private val fuelOverride = MutableStateFlow<FuelType?>(null)
    private val surchargeOverride = MutableStateFlow<Boolean?>(null)
    private val departureWindow = MutableStateFlow(DepartureWindow.DAWN)
    private val originMode = MutableStateFlow(OriginMode.CUSTOM)
    private val gpsLocation = MutableStateFlow<GeoPoint?>(null)
    private val locatingOrigin = MutableStateFlow(false)
    private var locationJob: Job? = null
    private var locationRequest = 0
    private var lastDestinationId: String? = null

    private val noticeChannel = MutableSharedFlow<UiNotice>(
        extraBufferCapacity = NOTICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<UiNotice> = noticeChannel.asSharedFlow()

    val state: StateFlow<RoutePlanUiState> = retryTrigger.flatMapLatest {
        combine(
            destinationRepository.observeDestinations(),
            preferencesRepository.preferences,
        ) { destinations, preferences ->
            val destinationId = requestedDestinationId ?: preferences.activeRouteDestinationId
            destinations.firstOrNull { it.id == destinationId } to preferences
        }.flatMapLatest { (destination, preferences) ->
            if (destination == null) {
                flowOf(RoutePlanUiState.Empty)
            } else {
                planFlow(destination, preferences)
            }
        }.catch { emit(RoutePlanUiState.Error(UiError.DATABASE)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RoutePlanUiState.Loading,
    )

    init {
        requestedDestinationId?.let(::persistRoute)
        viewModelScope.launch {
            combine(
                destinationRepository.observeDestinations(),
                preferencesRepository.preferences,
            ) { destinations, preferences ->
                requestedDestinationId ?: preferences.activeRouteDestinationId
            }
                .distinctUntilChanged()
                .collect { destinationId ->
                    if (lastDestinationId != null && lastDestinationId != destinationId) {
                        resetTransientSelection()
                    }
                    lastDestinationId = destinationId
                }
        }
    }

    /**
     * «Mi ubicación» siempre consulta coordenadas frescas: no reutiliza la
     * caché de una selección anterior. Al volver a origen personalizado se
     * cancela la consulta pendiente y se descarta cualquier respuesta tardía.
     */
    fun selectOrigin(mode: OriginMode) {
        if (mode == OriginMode.CUSTOM) {
            cancelLocationRequest()
            originMode.value = OriginMode.CUSTOM
            return
        }
        originMode.value = OriginMode.CURRENT_LOCATION
        refreshLocation()
    }

    fun refreshLocation() {
        val request = ++locationRequest
        locationJob?.cancel()
        locatingOrigin.value = true
        locationJob = viewModelScope.launch {
            val point = runCatching { locationSource() }.getOrNull()
            // Respuesta tardía: cambió el destino, el usuario volvió a origen
            // personalizado o ya hay una consulta más nueva en curso.
            if (request != locationRequest || originMode.value != OriginMode.CURRENT_LOCATION) {
                return@launch
            }
            if (point == null) {
                originMode.value = OriginMode.CUSTOM
                locatingOrigin.value = false
                noticeChannel.tryEmit(UiNotice.LOCATION_DENIED)
            } else {
                gpsLocation.value = point
                locatingOrigin.value = false
            }
        }
    }

    fun onLocationDenied() {
        cancelLocationRequest()
        originMode.value = OriginMode.CUSTOM
        noticeChannel.tryEmit(UiNotice.LOCATION_DENIED)
    }

    private fun cancelLocationRequest() {
        locationRequest++
        locationJob?.cancel()
        locationJob = null
        locatingOrigin.value = false
    }

    private fun resetTransientSelection() {
        fuelOverride.value = null
        surchargeOverride.value = null
        departureWindow.value = DepartureWindow.DAWN
        cancelLocationRequest()
        originMode.value = OriginMode.CUSTOM
        gpsLocation.value = null
    }

    fun selectFuel(fuelType: FuelType) {
        fuelOverride.value = fuelType
    }

    fun setSurcharge(enabled: Boolean) {
        surchargeOverride.value = enabled
    }

    fun selectDeparture(window: DepartureWindow) {
        departureWindow.value = window
    }

    fun saveCurrentPlan() {
        val content = state.value as? RoutePlanUiState.Content ?: return
        val now = clock()
        viewModelScope.launch {
            runCatching { routePlanRepository.save(content.toSavedPlan(now)) }
                .onSuccess { noticeChannel.tryEmit(UiNotice.PLAN_SAVED) }
                .onFailure { noticeChannel.tryEmit(UiNotice.ACTION_FAILED) }
        }
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

    private fun planFlow(
        destination: Destination,
        preferences: UserPreferences,
    ): Flow<RoutePlanUiState> {
        val vehicle = preferences.preferredVehicle ?: destination.recommendedVehicle
        val suggestedSurcharge = LogisticsEngine.suggestsMountainSurcharge(destination)
        val selection = combine(
            combine(fuelOverride, surchargeOverride, departureWindow) { fuel, surcharge, window ->
                Triple(fuel, surcharge, window)
            },
            combine(originMode, gpsLocation, locatingOrigin) { mode, gps, locating ->
                Triple(mode, gps, locating)
            },
        ) { (fuel, surcharge, window), (mode, gps, locating) ->
            PlanSelection(
                fuel = fuel,
                surcharge = surcharge,
                window = window,
                originMode = mode,
                gps = gps,
                locating = locating,
            )
        }

        return combine(
            routePlanRepository.observeSavedPlans(destination.id, SAVED_PLANS_LIMIT),
            selection,
        ) { savedPlans, current ->
            val fuel = current.fuel ?: DEFAULT_FUEL
            val surcharge = current.surcharge ?: suggestedSurcharge
            val origin = if (current.originMode == OriginMode.CURRENT_LOCATION) {
                current.gps ?: preferences.origin
            } else {
                preferences.origin
            }
            val plan = LogisticsEngine.plan(
                LogisticsInput(
                    origin = origin,
                    destination = destination,
                    vehicle = vehicle,
                    fuelType = fuel,
                    mountainSurcharge = surcharge,
                    departureMinuteOfDay = current.window.minuteOfDay,
                    month = currentMonth(),
                ),
            )
            RoutePlanUiState.Content(
                destination = destination,
                origin = origin,
                originMode = current.originMode,
                isLocatingOrigin = current.locating,
                vehicle = vehicle,
                fuelType = fuel,
                surchargeEnabled = surcharge,
                departureWindow = current.window,
                cashAlertsEnabled = preferences.cashAlertsEnabled,
                plan = plan,
                savedPlans = savedPlans.map { it.toUi() },
            )
        }
    }

    private fun currentMonth(): Int =
        Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).monthValue

    private fun RoutePlanUiState.Content.toSavedPlan(now: Long): SavedRoutePlan = SavedRoutePlan(
        id = "plan-${destination.id}-$now",
        destinationId = destination.id,
        destinationName = destination.name,
        fuelType = fuelType,
        vehicle = vehicle,
        mountainSurcharge = surchargeEnabled,
        departureMinuteOfDay = departureWindow.minuteOfDay,
        distanceMeters = plan.oneWayDistanceMeters,
        driveMinutes = plan.driveMinutes,
        roundTripKilometers = plan.fuel.roundTripKilometers,
        fuelLiters = plan.fuel.effectiveLiters,
        fuelCostCents = plan.fuel.costCents,
        tollCostCents = plan.tollRoundTripCents,
        cashOnlyCents = plan.cashOnlyRoundTripCents,
        totalCents = plan.totalRoundTripCents,
        createdAtEpochMillis = now,
    )

    private fun SavedRoutePlan.toUi(): SavedPlanUi = SavedPlanUi(
        id = id,
        fuelType = fuelType,
        mountainSurcharge = mountainSurcharge,
        departureMinuteOfDay = departureMinuteOfDay,
        totalCents = totalCents,
        cashOnlyCents = cashOnlyCents,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    private data class PlanSelection(
        val fuel: FuelType?,
        val surcharge: Boolean?,
        val window: DepartureWindow,
        val originMode: OriginMode,
        val gps: GeoPoint?,
        val locating: Boolean,
    )

    companion object {
        const val ARG_DESTINATION_ID = "destinationId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val NOTICE_BUFFER = 4
        private const val SAVED_PLANS_LIMIT = 3
        private val DEFAULT_FUEL = FuelType.GASOLINE_95

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RoutePlanViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    destinationRepository = container.destinationRepository,
                    preferencesRepository = container.preferencesRepository,
                    routePlanRepository = container.routePlanRepository,
                    locationSource = { container.locationProvider.lastKnownLocation() },
                )
            }
        }
    }
}
