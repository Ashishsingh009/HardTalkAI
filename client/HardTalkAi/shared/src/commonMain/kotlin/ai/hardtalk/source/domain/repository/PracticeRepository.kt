package ai.hardtalk.source.domain.repository

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario

interface PracticeRepository {
    suspend fun getScenarios(): List<Scenario>
    suspend fun sendMessage(
        scenarioId: String,
        message: String,
        history: List<ChatMessage>,
    ): ReplyResult
}
