package ai.hardtalk.source.voice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class RealtimeTranscriptKind {
    USER,
    COUNTERPART,
}

data class RealtimeTranscript(
    val kind: RealtimeTranscriptKind,
    val text: String,
)

private val realtimeJson = Json { ignoreUnknownKeys = true }

/**
 * Pull a spoken transcript out of an OpenAI Realtime data-channel event.
 *
 * User speech is accepted from `input_audio_transcription` events, or from
 * `conversation.item.*` only when a real `transcript` string is present.
 * `item.content.toString()` is never treated as speech — that JSON dump is
 * what previously double-counted turns.
 */
fun parseRealtimeTranscript(raw: String, json: Json = realtimeJson): RealtimeTranscript? {
    val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
    return when (root.string("type")) {
        "conversation.item.input_audio_transcription.completed",
        "conversation.item.input_audio_transcription.done",
        -> userTranscript(root.string("transcript"))
        "response.output_audio_transcript.done",
        "response.audio_transcript.done",
        -> counterpartTranscript(root.string("transcript"))
        "conversation.item.added",
        "conversation.item.created",
        -> transcriptFromConversationItem(root["item"] as? JsonObject)
        else -> null
    }
}

fun isDuplicateUserTranscript(previous: String?, next: String): Boolean =
    !previous.isNullOrEmpty() && previous == next

private fun transcriptFromConversationItem(item: JsonObject?): RealtimeTranscript? {
    if (item == null) return null
    val transcript = item.string("transcript") ?: structuredContentTranscript(item["content"])
    val cleaned = transcript?.trim()?.trim('"').orEmpty()
    if (cleaned.isEmpty()) return null
    return when (item.string("role")) {
        "user" -> RealtimeTranscript(RealtimeTranscriptKind.USER, cleaned)
        "assistant" -> RealtimeTranscript(RealtimeTranscriptKind.COUNTERPART, cleaned)
        else -> null
    }
}

private fun structuredContentTranscript(content: kotlinx.serialization.json.JsonElement?): String? {
    val parts = content as? JsonArray ?: return null
    for (part in parts) {
        val obj = part as? JsonObject ?: continue
        val transcript = obj.string("transcript")
        if (!transcript.isNullOrBlank()) return transcript
    }
    return null
}

private fun userTranscript(text: String?): RealtimeTranscript? =
    text?.trim()?.takeIf { it.isNotEmpty() }?.let {
        RealtimeTranscript(RealtimeTranscriptKind.USER, it)
    }

private fun counterpartTranscript(text: String?): RealtimeTranscript? =
    text?.trim()?.takeIf { it.isNotEmpty() }?.let {
        RealtimeTranscript(RealtimeTranscriptKind.COUNTERPART, it)
    }

private fun JsonObject.string(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()
