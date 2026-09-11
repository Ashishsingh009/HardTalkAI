package ai.hardtalk.source

import ai.hardtalk.source.data.api.HardTalkApi
import ai.hardtalk.source.data.api.createHardTalkHttpClient
import ai.hardtalk.source.data.api.defaultApiBaseUrl
import ai.hardtalk.source.data.api.provideHttpClientEngine
import ai.hardtalk.source.data.billing.InMemoryEntitlementRepository
import ai.hardtalk.source.data.repository.PracticeRepositoryImpl
import ai.hardtalk.source.domain.billing.EntitlementRepository
import ai.hardtalk.source.domain.repository.PracticeRepository

/**
 * Tiny composition root so Android/iOS hosts stay a one-liner (`App()` / `App(apiBaseUrl)`).
 * Android injects a RevenueCat [EntitlementRepository]; other hosts keep the in-memory default.
 */
class AppContainer(
    apiBaseUrl: String = defaultApiBaseUrl(),
    val entitlements: EntitlementRepository = InMemoryEntitlementRepository(
        statusMessage = "HardTalk Pro is billed on Android in this build. The raise drill stays free.",
    ),
) {
    val apiBaseUrl: String = apiBaseUrl
    val practiceRepository: PracticeRepository = PracticeRepositoryImpl(
        HardTalkApi(
            baseUrl = apiBaseUrl,
            client = createHardTalkHttpClient(provideHttpClientEngine()),
        ),
    )
}
