package com.panamatrip.app.features.routeplan.ui

import androidx.lifecycle.SavedStateHandle
import com.panamatrip.app.core.datastore.UserPreferences
import com.panamatrip.app.core.testing.MainDispatcherRule
import com.panamatrip.app.core.ui.UiNotice
import com.panamatrip.app.domain.affinity.AffinitySnapshot
import com.panamatrip.app.domain.logistics.SavedRoutePlan
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.GeoPoint
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.repository.DestinationRepository
import com.panamatrip.app.domain.repository.PreferencesRepository
import com.panamatrip.app.domain.repository.RoutePlanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutePlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `elegir mi ubicacion recalcula la distancia desde el GPS`() = runTest {
        val destination = destination(
            id = "portobelo",
            parking = GeoPoint(9.55, -79.65),
        )
        val viewModel = createViewModel(
            destinations = listOf(destination),
            location = GeoPoint(9.50, -79.60),
        )
        startCollecting(viewModel)
        advanceUntilIdle()

        val custom = viewModel.state.value as RoutePlanUiState.Content
        viewModel.selectOrigin(OriginMode.CURRENT_LOCATION)
        advanceUntilIdle()
        val fromGps = viewModel.state.value as RoutePlanUiState.Content

        assertEquals(OriginMode.CURRENT_LOCATION, fromGps.originMode)
        assertEquals(GeoPoint(9.50, -79.60), fromGps.origin)
        assertTrue(fromGps.plan.oneWayDistanceMeters < custom.plan.oneWayDistanceMeters)
    }

    @Test
    fun `cambiar de destino reinicia la ventana de salida`() = runTest {
        val first = destination(id = "a", parking = GeoPoint(8.8, -79.9))
        val second = destination(id = "b", parking = GeoPoint(9.2, -79.4))
        val preferences = FakePreferencesRepository(activeRoute = first.id)
        val viewModel = createViewModel(
            destinations = listOf(first, second),
            preferences = preferences,
        )
        startCollecting(viewModel)
        advanceUntilIdle()

        viewModel.selectDeparture(DepartureWindow.NOON)
        advanceUntilIdle()
        assertEquals(
            DepartureWindow.NOON,
            (viewModel.state.value as RoutePlanUiState.Content).departureWindow,
        )

        preferences.updateActiveRoute(second.id)
        advanceUntilIdle()

        val after = viewModel.state.value as RoutePlanUiState.Content
        assertEquals(second.id, after.destination.id)
        assertEquals(DepartureWindow.DAWN, after.departureWindow)
        assertEquals(OriginMode.CUSTOM, after.originMode)
    }

    @Test
    fun `sin GPS vuelve al punto guardado y avisa`() = runTest {
        val destination = destination(id = "a", parking = GeoPoint(8.8, -79.9))
        val viewModel = createViewModel(destinations = listOf(destination), location = null)
        startCollecting(viewModel)
        val notices = mutableListOf<UiNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notices.collect { notices += it }
        }
        advanceUntilIdle()

        viewModel.selectOrigin(OriginMode.CURRENT_LOCATION)
        advanceUntilIdle()

        val content = viewModel.state.value as RoutePlanUiState.Content
        assertEquals(OriginMode.CUSTOM, content.originMode)
        assertTrue(notices.contains(UiNotice.LOCATION_DENIED))
    }

    @Test
    fun `el plan refleja la preferencia de avisos de efectivo`() = runTest {
        val destination = destination(id = "colon", parking = GeoPoint(9.35, -79.9), province = "Colón")
        val preferences = FakePreferencesRepository(activeRoute = destination.id, cashAlerts = false)
        val viewModel = createViewModel(
            destinations = listOf(destination),
            preferences = preferences,
        )
        startCollecting(viewModel)
        advanceUntilIdle()

        val content = viewModel.state.value as RoutePlanUiState.Content
        assertEquals(false, content.cashAlertsEnabled)
        assertTrue(content.plan.cashOnlyRoundTripCents > 0)
    }

    private fun TestScope.startCollecting(viewModel: RoutePlanViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
    }

    private fun createViewModel(
        destinations: List<Destination>,
        preferences: FakePreferencesRepository = FakePreferencesRepository(activeRoute = destinations.first().id),
        location: GeoPoint? = null,
    ): RoutePlanViewModel = RoutePlanViewModel(
        savedStateHandle = SavedStateHandle(),
        destinationRepository = FakeDestinationRepository(destinations),
        preferencesRepository = preferences,
        routePlanRepository = FakeRoutePlanRepository(),
        locationSource = { location },
    )

    private fun destination(
        id: String,
        parking: GeoPoint,
        province: String = "Panamá",
    ): Destination = Destination(
        id = id,
        name = "Destino $id",
        province = province,
        ecosystem = Ecosystem.MOUNTAIN,
        summary = "Resumen",
        description = "Descripción",
        accessNotes = "Notas",
        parking = parking,
        attraction = parking,
        heroImageUrl = "",
        walkDistanceMeters = 800,
        walkMinutes = 25,
        elevationGainMeters = 200,
        recommendedVehicle = VehicleType.SUV,
        vehicleAccess = emptyMap(),
        averageRating = 4.0,
        reviewCount = 3,
        featuredOrder = 0,
    )

    private class FakeDestinationRepository(
        destinations: List<Destination>,
    ) : DestinationRepository {
        private val state = MutableStateFlow(destinations)

        override fun observeDestinations(): Flow<List<Destination>> = state

        override fun observeDestination(id: String): Flow<Destination?> =
            MutableStateFlow(state.value.firstOrNull { it.id == id })

        override fun observeReviews(destinationId: String): Flow<List<DestinationReview>> =
            flowOf(emptyList())
    }

    private class FakePreferencesRepository(
        activeRoute: String?,
        cashAlerts: Boolean = true,
    ) : PreferencesRepository {
        private val state = MutableStateFlow(
            UserPreferences(
                activeRouteDestinationId = activeRoute,
                cashAlertsEnabled = cashAlerts,
            ),
        )

        override val preferences: Flow<UserPreferences> = state

        override val affinity: Flow<AffinitySnapshot> = MutableStateFlow(AffinitySnapshot())

        fun updateActiveRoute(destinationId: String?) {
            state.update { it.copy(activeRouteDestinationId = destinationId) }
        }

        override suspend fun setPreferredVehicle(vehicle: VehicleType?) {
            state.update { it.copy(preferredVehicle = vehicle) }
        }

        override suspend fun setActiveRoute(destinationId: String?) =
            updateActiveRoute(destinationId = destinationId)

        override suspend fun setCashAlertsEnabled(enabled: Boolean) {
            state.update { it.copy(cashAlertsEnabled = enabled) }
        }

        override suspend fun recordLike(destination: Destination) = Unit

        override suspend fun recordSave(destination: Destination) = Unit

        override suspend fun recordView(destination: Destination) = Unit
    }

    private class FakeRoutePlanRepository : RoutePlanRepository {
        override fun observeSavedPlans(
            destinationId: String,
            limit: Int,
        ): Flow<List<SavedRoutePlan>> = flowOf(emptyList())

        override suspend fun save(plan: SavedRoutePlan) = Unit
    }
}
