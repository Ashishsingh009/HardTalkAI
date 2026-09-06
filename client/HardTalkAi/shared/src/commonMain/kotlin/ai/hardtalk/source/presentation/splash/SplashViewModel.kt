package ai.hardtalk.source.presentation.splash

import ai.hardtalk.source.data.repository.SplashRepositoryImpl
import ai.hardtalk.source.domain.usecase.GetSplashDestinationUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Splash screen lifecycle and initialization state.
 */
class SplashViewModel(
    private val getSplashDestinationUseCase: GetSplashDestinationUseCase = GetSplashDestinationUseCase(SplashRepositoryImpl()),
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        initializeApp()
    }

    fun initializeApp() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            getSplashDestinationUseCase()
                .onSuccess { destination ->
                    _uiState.value = SplashUiState.Success(destination)
                }
                .onFailure { error ->
                    _uiState.value = SplashUiState.Error(error.message ?: "Failed to initialize app")
                }
        }
    }
}
