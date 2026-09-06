package ai.hardtalk.source.domain.repository

import ai.hardtalk.source.domain.model.AppInitializationResult

/**
 * Domain repository contract for initializing application state and configurations.
 */
interface SplashRepository {
    suspend fun initializeApp(): AppInitializationResult
}
