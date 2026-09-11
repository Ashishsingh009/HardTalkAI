package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.data.billing.InMemoryEntitlementRepository
import ai.hardtalk.source.domain.billing.EntitlementRepository
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.repository.PracticeRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScenarioListViewModel(
    private val practiceRepository: PracticeRepository,
    private val entitlements: EntitlementRepository = InMemoryEntitlementRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScenarioListUiState>(ScenarioListUiState.Loading)
    val uiState: StateFlow<ScenarioListUiState> = _uiState.asStateFlow()

    private var loadedScenarios: List<Scenario>? = null

    init {
        loadScenarios()
        viewModelScope.launch {
            entitlements.state.collect { billing ->
                val scenarios = loadedScenarios ?: return@collect
                _uiState.value = ScenarioListUiState.Ready(
                    scenarios = scenarios,
                    isPro = billing.isPro,
                    monthlyPriceLabel = billing.monthlyPriceLabel,
                    billingConfigured = billing.configured,
                )
            }
        }
    }

    fun loadScenarios() {
        viewModelScope.launch {
            _uiState.value = ScenarioListUiState.Loading
            loadedScenarios = null
            runCatching { practiceRepository.getScenarios() }
                .onSuccess { scenarios ->
                    loadedScenarios = scenarios
                    val billing = entitlements.state.value
                    _uiState.value = ScenarioListUiState.Ready(
                        scenarios = scenarios,
                        isPro = billing.isPro,
                        monthlyPriceLabel = billing.monthlyPriceLabel,
                        billingConfigured = billing.configured,
                    )
                }
                .onFailure { error ->
                    _uiState.value = ScenarioListUiState.Error(
                        error.message ?: "Failed to load scenarios",
                    )
                }
            runCatching { entitlements.refresh() }
        }
    }
}
