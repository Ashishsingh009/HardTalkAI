package ai.hardtalk.source.data.repository

import ai.hardtalk.source.data.api.HardTalkApi
import ai.hardtalk.source.data.api.toDomain
import ai.hardtalk.source.data.api.toHistoryDto
import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.repository.PracticeRepository

class PracticeRepositoryImpl(
    private val api: HardTalkApi,
) : PracticeRepository {
    override suspend fun getScenarios(): List<Scenario> =
        api.getScenarios().map { it.toDomain() }

    override suspend fun sendMessage(
        scenarioId: String,
        message: String,
        history: List<ChatMessage>,
    ): ReplyResult = api.sendChat(
        scenarioId = scenarioId,
        message = message,
        history = history.map { it.toHistoryDto() },
    ).toDomain()
}
