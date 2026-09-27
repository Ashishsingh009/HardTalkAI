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
    val instructions: String,
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
