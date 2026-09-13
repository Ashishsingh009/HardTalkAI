package ai.hardtalk.source.data.api

import ai.hardtalk.source.data.api.dto.ChatTurnDto
import ai.hardtalk.source.data.api.dto.FeedbackDto
import ai.hardtalk.source.data.api.dto.PersonaDto
import ai.hardtalk.source.data.api.dto.ReplyResultDto
import ai.hardtalk.source.data.api.dto.ScenarioDto
import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.Persona
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.model.toApiRole

fun ScenarioDto.toDomain(): Scenario = Scenario(
    id = id,
    title = title,
    summary = summary,
    difficulty = difficulty,
    persona = persona.toDomain(),
    opening = opening,
    goals = goals,
    free = free,
)

fun PersonaDto.toDomain(): Persona = Persona(
    name = name,
    role = role,
    mood = mood,
)

fun FeedbackDto.toDomain(): Feedback = Feedback(
    clarity = clarity,
    empathy = empathy,
    assertiveness = assertiveness,
    overall = overall,
    tips = tips,
)

fun ReplyResultDto.toDomain(): ReplyResult = ReplyResult(
    reply = reply,
    feedback = feedback.toDomain(),
    mood = mood,
)

fun ChatMessage.toHistoryDto(): ChatTurnDto = ChatTurnDto(
    role = role.toApiRole(),
    content = content,
)
