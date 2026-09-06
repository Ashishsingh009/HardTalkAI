package ai.hardtalk.source.presentation.splash

import ai.hardtalk.source.domain.model.SplashDestination

/**
 * UI State for the Splash Screen.
 */
sealed interface SplashUiState {
    data object Loading : SplashUiState
    data class Success(val destination: SplashDestination) : SplashUiState
    data class Error(val message: String) : SplashUiState
}
