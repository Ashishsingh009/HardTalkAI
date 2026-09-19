package ai.hardtalk.source.voice

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.VoiceSession
import kotlinx.coroutines.flow.SharedFlow

/**
 * Live audio is the **counterpart** (the manager in the drill).
 * Coach Heather is not on the call — she scores the transcript after hang-up.
 */
data class VoiceCallConfig(
    val clientSecret: String,
    val realtimeUrl: String,
    val opening: String,
    val maxUserTurns: Int,
    val maxDurationSeconds: Int,
)

fun VoiceSession.toCallConfig(): VoiceCallConfig = VoiceCallConfig(
    clientSecret = clientSecret,
    realtimeUrl = realtimeUrl,
    opening = opening,
    maxUserTurns = maxUserTurns,
    maxDurationSeconds = maxDurationSeconds,
)

sealed interface VoiceCallEvent {
    data class Status(val text: String) : VoiceCallEvent
    data class Transcript(val role: ChatRole, val text: String, val userTurnCount: Int) : VoiceCallEvent
    data class Failed(val message: String) : VoiceCallEvent
    data class Ended(val turns: List<ChatMessage>, val reason: String) : VoiceCallEvent
}

interface VoiceCallSession {
    val events: SharedFlow<VoiceCallEvent>
    suspend fun connect(config: VoiceCallConfig)
    fun setMuted(muted: Boolean)
    fun hangUp()
}

fun interface VoiceCallFactory {
    fun create(): VoiceCallSession?
}

expect fun isVoiceCallSupported(): Boolean

expect fun createVoiceCallSession(): VoiceCallSession?
