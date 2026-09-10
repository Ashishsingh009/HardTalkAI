package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.domain.model.Scenario

sealed interface ScenarioListUiState {
    data object Loading : ScenarioListUiState
    data class Ready(val scenarios: List<Scenario>) : ScenarioListUiState
    data class Error(val message: String) : ScenarioListUiState
}
