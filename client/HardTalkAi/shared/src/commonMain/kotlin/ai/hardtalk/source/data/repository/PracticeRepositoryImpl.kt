package ai.hardtalk.source.data.repository

import ai.hardtalk.source.data.api.HardTalkApi
import ai.hardtalk.source.data.api.toDomain
import ai.hardtalk.source.data.api.toHistoryDto
import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.HealthStatus
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.model.VoiceCompleteResult
import ai.hardtalk.source.domain.model.VoiceSession
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

    override suspend fun getHealth(): HealthStatus = api.getHealth().toDomain()

    override suspend fun createVoiceSession(scenarioId: String): VoiceSession =
        api.createVoiceSession(scenarioId).toDomain()

    override suspend fun completeVoiceRound(
        scenarioId: String,
        turns: List<ChatMessage>,
    ): VoiceCompleteResult = api.completeVoiceRound(
        scenarioId = scenarioId,
        turns = turns.map { it.toHistoryDto() },
    ).toDomain()
}
