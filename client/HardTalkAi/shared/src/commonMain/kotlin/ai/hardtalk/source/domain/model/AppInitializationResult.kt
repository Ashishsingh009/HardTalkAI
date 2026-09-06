package ai.hardtalk.source.domain.model

/**
 * Encapsulates domain details obtained during app initialization.
 */
data class AppInitializationResult(
    val destination: NavigationDestination,
    val isUserLoggedIn: Boolean,
    val isFirstLaunch: Boolean
)
