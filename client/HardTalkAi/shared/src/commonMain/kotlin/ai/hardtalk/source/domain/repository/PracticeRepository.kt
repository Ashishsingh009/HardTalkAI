package ai.hardtalk.source.domain.repository

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.HealthStatus
import ai.hardtalk.source.domain.model.InvoiceExtraction
import ai.hardtalk.source.domain.model.InvoiceSendResult
import ai.hardtalk.source.domain.model.InvoiceTerms
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.model.VoiceCompleteResult
import ai.hardtalk.source.domain.model.VoiceSession

interface PracticeRepository {
    suspend fun getScenarios(): List<Scenario>
    suspend fun sendMessage(
        scenarioId: String,
        message: String,
        history: List<ChatMessage>,
    ): ReplyResult

    suspend fun getHealth(): HealthStatus

    suspend fun createVoiceSession(scenarioId: String): VoiceSession

    suspend fun completeVoiceRound(
        scenarioId: String,
        turns: List<ChatMessage>,
    ): VoiceCompleteResult

    suspend fun extractInvoiceTerms(
        scenarioId: String,
        turns: List<ChatMessage>,
    ): InvoiceExtraction

    suspend fun sendPaypalInvoice(
        scenarioId: String,
        terms: InvoiceTerms,
    ): InvoiceSendResult

    suspend fun trackFunnelEvent(name: String, scenarioId: String? = null) = Unit
}
