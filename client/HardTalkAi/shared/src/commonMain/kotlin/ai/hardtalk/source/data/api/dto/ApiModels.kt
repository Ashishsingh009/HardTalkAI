package ai.hardtalk.source.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PersonaDto(
    val name: String,
    val role: String,
    val mood: String,
)

@Serializable
data class ScenarioDto(
    val id: String,
    val title: String,
    val summary: String,
    val difficulty: String,
    val persona: PersonaDto,
    val opening: String,
    val goals: List<String> = emptyList(),
    val free: Boolean = false,
)

@Serializable
data class ScenariosResponseDto(
    val scenarios: List<ScenarioDto>,
)

@Serializable
data class ChatTurnDto(
    val role: String,
    val content: String,
)

@Serializable
data class ChatRequestDto(
    val scenarioId: String,
    val message: String,
    val history: List<ChatTurnDto> = emptyList(),
)

@Serializable
data class FeedbackDto(
    val clarity: Int,
    val empathy: Int,
    val assertiveness: Int,
    val overall: Int,
    val tips: List<String> = emptyList(),
)

@Serializable
data class ReplyResultDto(
    val reply: String,
    val feedback: FeedbackDto,
    val mood: String,
)

@Serializable
data class ApiErrorDto(
    val error: String,
)

@Serializable
data class HealthResponseDto(
    val status: String,
    val engine: String,
    val scenarios: Int,
    val voice: Boolean = false,
    val paypal: String = "mock",
)

@Serializable
data class InvoiceTermsDto(
    val amount: String? = null,
    val currency: String? = null,
    val dueDate: String? = null,
    val recipientEmail: String? = null,
    val recipientName: String? = null,
    val description: String? = null,
)

@Serializable
data class InvoiceExtractRequestDto(
    val scenarioId: String,
    val turns: List<ChatTurnDto> = emptyList(),
)

@Serializable
data class InvoiceExtractResponseDto(
    val terms: InvoiceTermsDto = InvoiceTermsDto(),
    val missing: List<String> = emptyList(),
    val source: String = "heuristic",
)

@Serializable
data class InvoiceSendRequestDto(
    val scenarioId: String,
    val amount: String? = null,
    val currency: String? = null,
    val dueDate: String? = null,
    val recipientEmail: String? = null,
    val recipientName: String? = null,
    val description: String? = null,
)

@Serializable
data class InvoiceSendResponseDto(
    val invoiceId: String,
    val status: String,
    val payerLink: String,
    val mode: String,
)

@Serializable
data class VoiceSessionRequestDto(
    val scenarioId: String,
)

@Serializable
data class VoiceSessionResponseDto(
    val clientSecret: String,
    val realtimeUrl: String,
    val model: String,
    val voice: String,
    val opening: String,
    val personaName: String,
    val maxUserTurns: Int = 3,
    val maxDurationSeconds: Int = 90,
)

@Serializable
data class VoiceCompleteRequestDto(
    val scenarioId: String,
    val turns: List<ChatTurnDto> = emptyList(),
)

@Serializable
data class ScoredVoiceTurnDto(
    val role: String,
    val content: String,
    val feedback: FeedbackDto? = null,
)

@Serializable
data class VoiceCompleteResponseDto(
    val messages: List<ScoredVoiceTurnDto> = emptyList(),
    val mood: String,
)

@Serializable
data class AnalyticsEventRequestDto(
    val event: String,
    val scenarioId: String? = null,
    val channel: String? = null,
    val mode: String? = null,
)
