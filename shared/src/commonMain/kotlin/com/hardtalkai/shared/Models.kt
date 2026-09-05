package com.hardtalkai.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty {
    @SerialName("warm-up")
    WARM_UP,

    @SerialName("moderate")
    MODERATE,

    @SerialName("hard")
    HARD,
}

@Serializable
data class Persona(
    val name: String,
    val role: String,
    /** Short description of the counterpart's disposition at the start. */
    val mood: String,
)

@Serializable
data class Scenario(
    val id: String,
    val title: String,
    val summary: String,
    val difficulty: Difficulty,
    val persona: Persona,
    /** The first line the counterpart says to open the conversation. */
    val opening: String,
    /** Coaching goals the user is trying to achieve in this conversation. */
    val goals: List<String>,
)

@Serializable
data class ChatTurn(
    val role: String,
    val content: String,
)

@Serializable
data class Feedback(
    /** 0-100 sub-scores for the latest user message. */
    val clarity: Int,
    val empathy: Int,
    val assertiveness: Int,
    /** 0-100 blended score. */
    val overall: Int,
    /** Human-readable coaching tips. */
    val tips: List<String>,
)

@Serializable
data class ReplyResult(
    val reply: String,
    val feedback: Feedback,
    /** How the counterpart currently feels, after this exchange. */
    val mood: String,
)

@Serializable
data class ChatRequest(
    val scenarioId: String? = null,
    val message: String? = null,
    val history: List<ChatTurn> = emptyList(),
)

@Serializable
data class ScenariosResponse(
    val scenarios: List<Scenario>,
)
