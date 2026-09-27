package ai.hardtalk.source.data.api

import ai.hardtalk.source.data.api.dto.ApiErrorDto
import ai.hardtalk.source.data.api.dto.ChatRequestDto
import ai.hardtalk.source.data.api.dto.ChatTurnDto
import ai.hardtalk.source.data.api.dto.HealthResponseDto
import ai.hardtalk.source.data.api.dto.ReplyResultDto
import ai.hardtalk.source.data.api.dto.ScenarioDto
import ai.hardtalk.source.data.api.dto.ScenariosResponseDto
import ai.hardtalk.source.data.api.dto.VoiceCompleteRequestDto
import ai.hardtalk.source.data.api.dto.VoiceCompleteResponseDto
import ai.hardtalk.source.data.api.dto.VoiceSessionRequestDto
import ai.hardtalk.source.data.api.dto.VoiceSessionResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ApiException(message: String) : Exception(message)

class HardTalkApi(
    baseUrl: String,
    private val client: HttpClient,
) {
    private val root = normalizeApiBaseUrl(baseUrl)

    val baseUrl: String get() = root

    suspend fun getScenarios(): List<ScenarioDto> {
        val response = client.get("$root/api/scenarios")
        ensureSuccess(response)
        return response.body<ScenariosResponseDto>().scenarios
    }

    suspend fun sendChat(
        scenarioId: String,
        message: String,
        history: List<ChatTurnDto>,
    ): ReplyResultDto {
        val response = client.post("$root/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequestDto(
                    scenarioId = scenarioId,
                    message = message,
                    history = history,
                ),
            )
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun getHealth(): HealthResponseDto {
        val response = client.get("$root/api/health")
        ensureSuccess(response)
        return response.body()
    }

    suspend fun createVoiceSession(scenarioId: String): VoiceSessionResponseDto {
        val response = client.post("$root/api/voice/session") {
            contentType(ContentType.Application.Json)
            setBody(VoiceSessionRequestDto(scenarioId = scenarioId))
        }
        ensureSuccess(response)
        return response.body()
    }

    suspend fun completeVoiceRound(
        scenarioId: String,
        turns: List<ChatTurnDto>,
    ): VoiceCompleteResponseDto {
        val response = client.post("$root/api/voice/complete") {
            contentType(ContentType.Application.Json)
            setBody(
                VoiceCompleteRequestDto(
                    scenarioId = scenarioId,
                    turns = turns,
                ),
            )
        }
        ensureSuccess(response)
        return response.body()
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val message = runCatching {
            apiJson.decodeFromString(ApiErrorDto.serializer(), response.bodyAsText()).error
        }.getOrNull()
        throw ApiException(message ?: "Request failed (${response.status.value})")
    }
}
