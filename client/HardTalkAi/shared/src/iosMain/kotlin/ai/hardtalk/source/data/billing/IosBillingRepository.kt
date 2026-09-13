package ai.hardtalk.source.data.billing

import ai.hardtalk.source.domain.billing.BillingState
import ai.hardtalk.source.domain.repository.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS StoreKit / Purchases is out of scope. Preview and simulator stay ungated.
 */
class IosBillingRepository : BillingRepository {
    private val _state = MutableStateFlow(BillingState(ungated = true))
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun purchase(): Result<Unit> =
        Result.failure(IllegalStateException("HardTalk Pro is not available on iOS yet."))

    override suspend fun restore(): Result<Unit> =
        Result.failure(IllegalStateException("HardTalk Pro is not available on iOS yet."))
}

actual fun createBillingRepository(): BillingRepository = IosBillingRepository()
