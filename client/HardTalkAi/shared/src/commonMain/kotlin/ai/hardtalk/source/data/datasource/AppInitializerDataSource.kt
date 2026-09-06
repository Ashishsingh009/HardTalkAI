package ai.hardtalk.source.data.datasource

import ai.hardtalk.source.domain.model.AppInitializationResult

/**
 * Data source interface for fetching/computing initialization configurations.
 */
interface AppInitializerDataSource {
    suspend fun loadInitialData(): AppInitializationResult
}
