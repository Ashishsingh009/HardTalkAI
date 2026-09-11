package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.domain.model.Scenario

sealed interface ScenarioListUiState {
    data object Loading : ScenarioListUiState
    data class Ready(
        val scenarios: List<Scenario>,
        val isPro: Boolean = false,
        val monthlyPriceLabel: String? = null,
        val billingConfigured: Boolean = false,
    ) : ScenarioListUiState
    data class Error(val message: String) : ScenarioListUiState
}
