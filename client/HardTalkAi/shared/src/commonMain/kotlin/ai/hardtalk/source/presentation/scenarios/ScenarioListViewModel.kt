package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.domain.billing.canPlayScenario
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.repository.BillingRepository
import ai.hardtalk.source.domain.repository.PracticeRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScenarioListViewModel(
    private val practiceRepository: PracticeRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScenarioListUiState>(ScenarioListUiState.Loading)
    val uiState: StateFlow<ScenarioListUiState> = _uiState.asStateFlow()

    init {
        loadScenarios()
        viewModelScope.launch {
            billingRepository.refresh()
            billingRepository.state.collect { billing ->
                _uiState.update { current ->
                    if (current is ScenarioListUiState.Ready) {
                        current.copy(
                            hasPro = billing.hasPro,
                            ungated = billing.ungated,
                            priceLabel = billing.priceLabel,
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun loadScenarios() {
        viewModelScope.launch {
            _uiState.value = ScenarioListUiState.Loading
            runCatching { practiceRepository.getScenarios() }
                .onSuccess { scenarios ->
                    val billing = billingRepository.state.value
                    _uiState.value = ScenarioListUiState.Ready(
                        scenarios = scenarios,
                        hasPro = billing.hasPro,
                        ungated = billing.ungated,
                        priceLabel = billing.priceLabel,
                    )
                }
                .onFailure { error ->
                    _uiState.value = ScenarioListUiState.Error(
                        error.message ?: "Failed to load scenarios",
                    )
                }
        }
    }

    fun onScenarioTapped(scenario: Scenario) {
        val ready = _uiState.value as? ScenarioListUiState.Ready ?: return
        if (canPlayScenario(scenario, ready.hasPro, ready.ungated)) {
            _uiState.value = ready.copy(openScenario = scenario, paywallScenario = null)
        } else {
            _uiState.value = ready.copy(
                paywallScenario = scenario,
                paywallError = null,
                paywallPurchasing = false,
            )
        }
    }

    fun consumeOpenScenario() {
        val ready = _uiState.value as? ScenarioListUiState.Ready ?: return
        _uiState.value = ready.copy(openScenario = null)
    }

    fun dismissPaywall() {
        val ready = _uiState.value as? ScenarioListUiState.Ready ?: return
        _uiState.value = ready.copy(
            paywallScenario = null,
            paywallPurchasing = false,
            paywallError = null,
        )
    }

    fun purchaseSelected() {
        val ready = _uiState.value as? ScenarioListUiState.Ready ?: return
        val scenario = ready.paywallScenario ?: return
        viewModelScope.launch {
            _uiState.update { current ->
                if (current is ScenarioListUiState.Ready) {
                    current.copy(paywallPurchasing = true, paywallError = null)
                } else {
                    current
                }
            }
            val result = billingRepository.purchase()
            val billing = billingRepository.state.value
            result
                .onSuccess {
                    val canOpen = canPlayScenario(scenario, billing.hasPro, billing.ungated)
                    _uiState.update { current ->
                        if (current !is ScenarioListUiState.Ready) {
                            current
                        } else {
                            current.copy(
                                hasPro = billing.hasPro,
                                ungated = billing.ungated,
                                priceLabel = billing.priceLabel,
                                paywallPurchasing = false,
                                paywallError = if (canOpen) null else "Purchase finished, but HardTalk Pro is not active yet.",
                                paywallScenario = if (canOpen) null else current.paywallScenario,
                                openScenario = if (canOpen) scenario else current.openScenario,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    val cancelled = error.message?.contains("cancelled", ignoreCase = true) == true
                    _uiState.update { current ->
                        if (current !is ScenarioListUiState.Ready) {
                            current
                        } else {
                            current.copy(
                                paywallPurchasing = false,
                                paywallError = if (cancelled) null else (error.message ?: "Purchase failed"),
                            )
                        }
                    }
                }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { current ->
                if (current is ScenarioListUiState.Ready) {
                    current.copy(paywallPurchasing = true, paywallError = null)
                } else {
                    current
                }
            }
            val result = billingRepository.restore()
            val billing = billingRepository.state.value
            val ready = _uiState.value as? ScenarioListUiState.Ready
            val scenario = ready?.paywallScenario
            val canOpen = scenario != null &&
                canPlayScenario(scenario, billing.hasPro, billing.ungated)
            result
                .onSuccess {
                    _uiState.update { current ->
                        if (current !is ScenarioListUiState.Ready) {
                            current
                        } else {
                            current.copy(
                                hasPro = billing.hasPro,
                                ungated = billing.ungated,
                                priceLabel = billing.priceLabel,
                                paywallPurchasing = false,
                                paywallError = if (canOpen || billing.hasPro) null else "No HardTalk Pro purchase to restore.",
                                paywallScenario = if (canOpen) null else current.paywallScenario,
                                openScenario = if (canOpen) scenario else current.openScenario,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        if (current is ScenarioListUiState.Ready) {
                            current.copy(
                                paywallPurchasing = false,
                                paywallError = error.message ?: "Restore failed",
                            )
                        } else {
                            current
                        }
                    }
                }
        }
    }
}
