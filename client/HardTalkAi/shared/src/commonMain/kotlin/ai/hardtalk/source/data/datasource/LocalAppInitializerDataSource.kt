package ai.hardtalk.source.data.datasource

import ai.hardtalk.source.domain.model.AppInitializationResult
import ai.hardtalk.source.domain.model.NavigationDestination
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
            delay(splashDelayMs.milliseconds)
        }

        // Simulate determining destination based on local/remote data
        return AppInitializationResult(
            destination = NavigationDestination.HOME,
            isUserLoggedIn = true,
            isFirstLaunch = false
        )
    }
}
