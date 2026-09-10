package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.Scenario

data class ChatUiState(
    val scenario: Scenario,
    val messages: List<ChatMessage>,
    val mood: String,
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)
