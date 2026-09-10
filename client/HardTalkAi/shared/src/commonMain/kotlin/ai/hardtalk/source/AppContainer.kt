package ai.hardtalk.source

import ai.hardtalk.source.data.api.HardTalkApi
import ai.hardtalk.source.data.api.createHardTalkHttpClient
import ai.hardtalk.source.data.api.defaultApiBaseUrl
import ai.hardtalk.source.data.api.provideHttpClientEngine
import ai.hardtalk.source.data.repository.PracticeRepositoryImpl
import ai.hardtalk.source.domain.repository.PracticeRepository

/**
 * Tiny composition root so Android/iOS hosts stay a one-liner (`App()` / `App(apiBaseUrl)`).
 */
class AppContainer(
    apiBaseUrl: String = defaultApiBaseUrl(),
) {
    val apiBaseUrl: String = apiBaseUrl
    val practiceRepository: PracticeRepository = PracticeRepositoryImpl(
        HardTalkApi(
            baseUrl = apiBaseUrl,
            client = createHardTalkHttpClient(provideHttpClientEngine()),
        ),
    )
}
