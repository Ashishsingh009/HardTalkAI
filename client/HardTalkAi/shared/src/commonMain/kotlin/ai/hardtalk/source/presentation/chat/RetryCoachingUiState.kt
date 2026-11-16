package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.RetryCoachingSuggestion

sealed interface RetryCoachingUiState {
    data object Hidden : RetryCoachingUiState
    data object DownloadAvailable : RetryCoachingUiState
    data class Downloading(val percent: Int) : RetryCoachingUiState
    data object DownloadFailed : RetryCoachingUiState
    data object Finding : RetryCoachingUiState
    data class Ready(val suggestion: RetryCoachingSuggestion) : RetryCoachingUiState
}
