package ai.hardtalk.source.domain

import ai.hardtalk.source.domain.model.AppInitializationResult
import ai.hardtalk.source.domain.model.SplashDestination
import ai.hardtalk.source.domain.repository.SplashRepository
import ai.hardtalk.source.domain.usecase.GetSplashDestinationUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSplashDestinationUseCaseTest {

    private class FakeSplashRepository(
        private val shouldFail: Boolean = false,
        private val destination: SplashDestination = SplashDestination.HOME
    ) : SplashRepository {
        override suspend fun initializeApp(): AppInitializationResult {
            if (shouldFail) {
                throw IllegalStateException("Initialization error")
            }
            return AppInitializationResult(
                destination = destination,
                isUserLoggedIn = true,
                isFirstLaunch = false
            )
        }
    }

    @Test
    fun `invoke returns success destination when repository succeeds`() = runTest {
        val fakeRepository = FakeSplashRepository(destination = SplashDestination.HOME)
        val useCase = GetSplashDestinationUseCase(fakeRepository)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(SplashDestination.HOME, result.getOrNull())
    }

    @Test
    fun `invoke returns failure when repository throws exception`() = runTest {
        val fakeRepository = FakeSplashRepository(shouldFail = true)
        val useCase = GetSplashDestinationUseCase(fakeRepository)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("Initialization error", result.exceptionOrNull()?.message)
    }
}
