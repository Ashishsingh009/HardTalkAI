package ai.hardtalk.source.domain.model

/**
 * Encapsulates domain details obtained during app initialization.
 */
data class AppInitializationResult(
    val destination: SplashDestination,
    val isUserLoggedIn: Boolean,
    val isFirstLaunch: Boolean
)
