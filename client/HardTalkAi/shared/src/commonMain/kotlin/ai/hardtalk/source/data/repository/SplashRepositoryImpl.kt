package ai.hardtalk.source.data.repository

import ai.hardtalk.source.data.datasource.AppInitializerDataSource
import ai.hardtalk.source.data.datasource.LocalAppInitializerDataSource
import ai.hardtalk.source.domain.model.AppInitializationResult
import ai.hardtalk.source.domain.repository.SplashRepository

/**
 * Implementation of SplashRepository coordinating data sources to initialize the app.
 */
class SplashRepositoryImpl(
    private val dataSource: AppInitializerDataSource = LocalAppInitializerDataSource()
) : SplashRepository {

    override suspend fun initializeApp(): AppInitializationResult {
        return dataSource.loadInitialData()
    }
}
