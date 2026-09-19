package ai.hardtalk.source.domain.repository

import ai.hardtalk.source.domain.billing.BillingState
import ai.hardtalk.source.domain.billing.canPlayScenario
import ai.hardtalk.source.domain.model.Scenario
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val state: StateFlow<BillingState>

    fun canPlay(scenario: Scenario): Boolean {
        val snapshot = state.value
        return canPlayScenario(scenario, snapshot.hasPro, snapshot.ungated)
    }

    suspend fun refresh()

    suspend fun purchase(): Result<Unit>

    suspend fun restore(): Result<Unit>
}

expect fun createBillingRepository(): BillingRepository
