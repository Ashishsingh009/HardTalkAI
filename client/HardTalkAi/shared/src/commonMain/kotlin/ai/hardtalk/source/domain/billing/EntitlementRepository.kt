package ai.hardtalk.source.domain.billing

import ai.hardtalk.source.domain.model.Scenario
import kotlinx.coroutines.flow.StateFlow

/**
 * Client-side Pro gate. FastAPI stays ungated; Android wires this to RevenueCat.
 * Other hosts can inject [ai.hardtalk.source.data.billing.InMemoryEntitlementRepository].
 */
interface EntitlementRepository {
    val state: StateFlow<EntitlementState>

    fun canPlay(scenario: Scenario): Boolean =
        scenario.isPlayable(state.value.isPro)

    suspend fun refresh()

    suspend fun purchaseMonthly(): BillingActionResult

    suspend fun restorePurchases(): BillingActionResult
}
