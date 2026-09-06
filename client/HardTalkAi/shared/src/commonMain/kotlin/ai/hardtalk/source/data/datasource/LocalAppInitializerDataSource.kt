package ai.hardtalk.source.data.datasource

import ai.hardtalk.source.domain.model.AppInitializationResult
import ai.hardtalk.source.domain.model.SplashDestination
import kotlinx.coroutines.delay

/**
 * Local implementation of initialization data source.
 * Simulates checking session tokens, loading local preferences, or fetching remote config.
 */
class LocalAppInitializerDataSource(
    private val splashDelayMs: Long = 2000L
) : AppInitializerDataSource {

    override suspend fun loadInitialData(): AppInitializationResult {
        // Enforce minimum splash delay for branding/smooth entrance animation
        if (splashDelayMs > 0) {
            delay(splashDelayMs)
        }

        // Simulate determining destination based on local/remote data
        return AppInitializationResult(
            destination = SplashDestination.HOME,
            isUserLoggedIn = true,
            isFirstLaunch = false
        )
    }
}
