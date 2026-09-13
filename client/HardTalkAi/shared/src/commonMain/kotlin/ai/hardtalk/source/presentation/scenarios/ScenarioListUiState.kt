package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.domain.billing.canPlayScenario
import ai.hardtalk.source.domain.model.Scenario

sealed interface ScenarioListUiState {
    data object Loading : ScenarioListUiState
    data class Ready(
        val scenarios: List<Scenario>,
        val hasPro: Boolean = false,
        val ungated: Boolean = false,
        val priceLabel: String? = null,
        val paywallScenario: Scenario? = null,
        val paywallPurchasing: Boolean = false,
        val paywallError: String? = null,
        val openScenario: Scenario? = null,
    ) : ScenarioListUiState {
        fun isLocked(scenario: Scenario): Boolean =
            !canPlayScenario(scenario, hasPro, ungated)
    }
    data class Error(val message: String) : ScenarioListUiState
}
