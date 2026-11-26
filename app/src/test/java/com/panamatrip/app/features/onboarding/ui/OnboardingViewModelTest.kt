package com.panamatrip.app.features.onboarding.ui

import com.panamatrip.app.core.datastore.UserPreferences
import com.panamatrip.app.core.testing.MainDispatcherRule
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.VehicleType
import com.panamatrip.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `invitado escribe guestMode sin tocar nombre ni handle`() = runTest {
        val repository = FakePreferencesRepository()
        val viewModel = OnboardingViewModel(repository)

        viewModel.continueAsGuest()
        advanceUntilIdle()

        assertEquals(1, repository.guestCalls)
        assertEquals(0, repository.completeCalls)
        assertTrue(repository.state.value.guestMode)
    }

    @Test
    fun `registro invalido no escribe nada`() = runTest {
        val repository = FakePreferencesRepository()
        val viewModel = OnboardingViewModel(repository)

        viewModel.showRegister()
        viewModel.updateName("A")
        viewModel.updateHandle("ab")
        viewModel.register()
        advanceUntilIdle()

        assertEquals(OnboardingError.NAME_REQUIRED, viewModel.state.value.error)
        assertEquals(0, repository.completeCalls)
    }

    @Test
    fun `registro valido guarda nombre handle y bio en una sola llamada`() = runTest {
        val repository = FakePreferencesRepository()
        val viewModel = OnboardingViewModel(repository)

        viewModel.showRegister()
        viewModel.updateName("  Lázaro  ")
        viewModel.updateHandle("@Lazaro.PT")
        viewModel.updateBio("Senderos del Istmo")
        viewModel.register()
        advanceUntilIdle()

        assertEquals(1, repository.completeCalls)
        assertEquals("Lázaro", repository.lastName)
        assertEquals("@lazaro.pt", repository.lastHandle)
        assertEquals("Senderos del Istmo", repository.lastBio)
        assertEquals(false, repository.lastGuest)
    }

    @Test
    fun `doble toque en invitado solo escribe una vez`() = runTest {
        val repository = FakePreferencesRepository()
        val viewModel = OnboardingViewModel(repository)

        viewModel.continueAsGuest()
        viewModel.continueAsGuest()
        advanceUntilIdle()

        assertEquals(1, repository.guestCalls)
    }

    private class FakePreferencesRepository : PreferencesRepository {
        val state = MutableStateFlow(UserPreferences())
        override val preferences: Flow<UserPreferences> = state
        override val affinity = MutableStateFlow(com.panamatrip.app.domain.affinity.AffinitySnapshot())

        var completeCalls = 0
        var guestCalls = 0
        var lastName = ""
        var lastHandle = ""
        var lastBio = ""
        var lastGuest = true

        override suspend fun setPreferredVehicle(vehicle: VehicleType?) = Unit

        override suspend fun setActiveRoute(destinationId: String?) = Unit

        override suspend fun completeOnboarding(
            name: String,
            handle: String,
            bio: String,
            guest: Boolean,
        ) {
            completeCalls += 1
            lastName = name
            lastHandle = handle
            lastBio = bio
            lastGuest = guest
            state.value = state.value.copy(
                hasCompletedOnboarding = true,
                userName = name,
                userHandle = handle,
                userBio = bio,
                guestMode = guest,
            )
        }

        override suspend fun signInLocal(): Boolean = state.value.userName.isNotBlank()

        override suspend fun enterAsGuest() {
            guestCalls += 1
            state.value = state.value.copy(hasCompletedOnboarding = true, guestMode = true)
        }

        override suspend fun recordLike(destination: Destination) = Unit

        override suspend fun recordSave(destination: Destination) = Unit

        override suspend fun recordView(destination: Destination) = Unit
    }
}
