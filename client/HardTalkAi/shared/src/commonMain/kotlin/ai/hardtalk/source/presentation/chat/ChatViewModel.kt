package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.repository.PracticeRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    scenario: Scenario,
    private val practiceRepository: PracticeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            scenario = scenario,
            messages = listOf(
                ChatMessage(role = ChatRole.COUNTERPART, content = scenario.opening),
            ),
            mood = scenario.persona.mood,
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun send() {
        val current = _uiState.value
        val message = current.input.trim()
        if (message.isEmpty() || current.sending) return

        val history = current.messages
        val userTurn = ChatMessage(role = ChatRole.USER, content = message)
        _uiState.update {
            it.copy(
                messages = it.messages + userTurn,
                input = "",
                sending = true,
                error = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                practiceRepository.sendMessage(
                    scenarioId = current.scenario.id,
                    message = message,
                    history = history,
                )
            }.onSuccess { result ->
                _uiState.update { state ->
                    val withFeedback = state.messages.mapIndexed { index, turn ->
                        if (index == state.messages.lastIndex && turn.role == ChatRole.USER) {
                            turn.copy(feedback = result.feedback)
                        } else {
                            turn
                        }
                    }
                    state.copy(
                        messages = withFeedback + ChatMessage(
                            role = ChatRole.COUNTERPART,
                            content = result.reply,
                        ),
                        mood = result.mood,
                        sending = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        sending = false,
                        error = error.message ?: "Failed to send message",
                    )
                }
            }
        }
    }
}
