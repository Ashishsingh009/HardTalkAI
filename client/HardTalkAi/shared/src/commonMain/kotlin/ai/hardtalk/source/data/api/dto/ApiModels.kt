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
