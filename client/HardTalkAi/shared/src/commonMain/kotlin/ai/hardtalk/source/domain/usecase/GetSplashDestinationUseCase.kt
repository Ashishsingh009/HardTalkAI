package ai.hardtalk.source.domain.usecase

import ai.hardtalk.source.domain.model.NavigationDestination
import ai.hardtalk.source.domain.repository.SplashRepository

/**
 * Use case encapsulating the business logic for resolving the appropriate screen
 * destination upon app startup.
 */
class GetSplashDestinationUseCase(
    private val repository: SplashRepository
) {
    suspend operator fun invoke(): Result<NavigationDestination> {
        return runCatching {
            val result = repository.initializeApp()
            result.destination
        }
    }
}
