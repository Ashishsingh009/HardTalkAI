package ai.hardtalk.source.presentation.scenarios

import ai.hardtalk.source.domain.repository.PracticeRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScenarioListViewModel(
    private val practiceRepository: PracticeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScenarioListUiState>(ScenarioListUiState.Loading)
    val uiState: StateFlow<ScenarioListUiState> = _uiState.asStateFlow()

    init {
        loadScenarios()
    }

    fun loadScenarios() {
        viewModelScope.launch {
            _uiState.value = ScenarioListUiState.Loading
            runCatching { practiceRepository.getScenarios() }
                .onSuccess { scenarios ->
                    _uiState.value = ScenarioListUiState.Ready(scenarios)
                }
                .onFailure { error ->
                    _uiState.value = ScenarioListUiState.Error(
                        error.message ?: "Failed to load scenarios",
                    )
                }
        }
    }
}
