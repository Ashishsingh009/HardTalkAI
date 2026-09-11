package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.domain.model.Scenario

data class ChatUiState(
    val scenario: Scenario,
    val messages: List<ChatMessage>,
    val mood: String,
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
) {
    val scoredUserTurns: Int
        get() = messages.count { it.role == ChatRole.USER && it.feedback != null }

    val roundComplete: Boolean
        get() = scoredUserTurns >= PracticeLoop.MAX_USER_TURNS

    val lastFeedback: Feedback?
        get() = messages.lastOrNull { it.feedback != null }?.feedback

    val canRetry: Boolean
        get() = messages.any { it.role == ChatRole.USER }

    val canSend: Boolean
        get() = !sending && !roundComplete
}
