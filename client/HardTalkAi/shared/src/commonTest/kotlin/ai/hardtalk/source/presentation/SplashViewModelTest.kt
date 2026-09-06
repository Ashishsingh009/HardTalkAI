package ai.hardtalk.source.presentation

import ai.hardtalk.source.domain.model.AppInitializationResult
import ai.hardtalk.source.domain.model.NavigationDestination
import ai.hardtalk.source.domain.repository.SplashRepository
import ai.hardtalk.source.domain.usecase.GetSplashDestinationUseCase
import ai.hardtalk.source.presentation.splash.SplashUiState
import ai.hardtalk.source.presentation.splash.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSplashRepository(
        private val shouldFail: Boolean = false,
        private val destination: NavigationDestination = NavigationDestination.HOME
    ) : SplashRepository {
        override suspend fun initializeApp(): AppInitializationResult {
            if (shouldFail) {
                throw IllegalStateException("Network timeout")
            }
            return AppInitializationResult(
                destination = destination,
                isUserLoggedIn = true,
                isFirstLaunch = false
            )
        }
    }

    @Test
    fun `initializeApp transitions to Success when use case succeeds`() = runTest {
        val repository = FakeSplashRepository(destination = NavigationDestination.HOME)
        val useCase = GetSplashDestinationUseCase(repository)
        val viewModel = SplashViewModel(useCase)

        val currentState = viewModel.uiState.value
        assertTrue(currentState is SplashUiState.Success)
        assertEquals(NavigationDestination.HOME, currentState.destination)
    }

    @Test
    fun `initializeApp transitions to Error when use case fails`() = runTest {
        val repository = FakeSplashRepository(shouldFail = true)
        val useCase = GetSplashDestinationUseCase(repository)
        val viewModel = SplashViewModel(useCase)

        val currentState = viewModel.uiState.value
        assertTrue(currentState is SplashUiState.Error)
        assertEquals("Network timeout", currentState.message)
    }
}
