package ai.hardtalk.source.data.api

import ai.hardtalk.source.data.api.dto.ChatTurnDto
import ai.hardtalk.source.data.api.dto.FeedbackDto
import ai.hardtalk.source.data.api.dto.HealthResponseDto
import ai.hardtalk.source.data.api.dto.InvoiceExtractResponseDto
import ai.hardtalk.source.data.api.dto.InvoiceSendRequestDto
import ai.hardtalk.source.data.api.dto.InvoiceSendResponseDto
import ai.hardtalk.source.data.api.dto.InvoiceTermsDto
import ai.hardtalk.source.data.api.dto.PersonaDto
import ai.hardtalk.source.data.api.dto.ReplyResultDto
import ai.hardtalk.source.data.api.dto.ScenarioDto
import ai.hardtalk.source.data.api.dto.ScoredVoiceTurnDto
import ai.hardtalk.source.data.api.dto.VoiceCompleteResponseDto
import ai.hardtalk.source.data.api.dto.VoiceSessionResponseDto
import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.HealthStatus
import ai.hardtalk.source.domain.model.InvoiceExtraction
import ai.hardtalk.source.domain.model.InvoiceSendResult
import ai.hardtalk.source.domain.model.InvoiceTerms
import ai.hardtalk.source.domain.model.Persona
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.model.VoiceCompleteResult
import ai.hardtalk.source.domain.model.VoiceSession
import ai.hardtalk.source.domain.model.toApiRole
import ai.hardtalk.source.domain.model.toChatRole

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

fun HealthResponseDto.toDomain(): HealthStatus = HealthStatus(
    engine = engine,
    voice = voice,
    scenarios = scenarios,
    paypal = paypal,
)

fun InvoiceTermsDto.toDomain(): InvoiceTerms = InvoiceTerms(
    amount = amount.orEmpty(),
    currency = currency?.ifBlank { null } ?: "USD",
    dueDate = dueDate.orEmpty(),
    recipientEmail = recipientEmail.orEmpty(),
    recipientName = recipientName.orEmpty(),
    description = description.orEmpty(),
)

fun InvoiceTerms.toSendDto(scenarioId: String): InvoiceSendRequestDto = InvoiceSendRequestDto(
    scenarioId = scenarioId,
    amount = amount.ifBlank { null },
    currency = currency.ifBlank { null },
    dueDate = dueDate.ifBlank { null },
    recipientEmail = recipientEmail.ifBlank { null },
    recipientName = recipientName.ifBlank { null },
    description = description.ifBlank { null },
)

fun InvoiceExtractResponseDto.toDomain(): InvoiceExtraction = InvoiceExtraction(
    terms = terms.toDomain(),
    missing = missing,
    source = source,
)

fun InvoiceSendResponseDto.toDomain(): InvoiceSendResult = InvoiceSendResult(
    invoiceId = invoiceId,
    status = status,
    payerLink = payerLink,
    mode = mode,
)

fun VoiceSessionResponseDto.toDomain(): VoiceSession = VoiceSession(
    clientSecret = clientSecret,
    realtimeUrl = realtimeUrl,
    model = model,
    voice = voice,
    opening = opening,
    personaName = personaName,
    maxUserTurns = maxUserTurns,
    maxDurationSeconds = maxDurationSeconds,
)

fun ScoredVoiceTurnDto.toDomain(): ChatMessage = ChatMessage(
    role = role.toChatRole(),
    content = content,
    feedback = feedback?.toDomain(),
)

fun VoiceCompleteResponseDto.toDomain(): VoiceCompleteResult = VoiceCompleteResult(
    messages = messages.map { it.toDomain() },
    mood = mood,
)
