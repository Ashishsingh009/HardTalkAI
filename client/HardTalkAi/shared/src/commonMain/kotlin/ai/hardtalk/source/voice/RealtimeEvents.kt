package ai.hardtalk.source.voice

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RealtimeResponseCreateEvent(
    val type: String = "response.create",
    val response: RealtimeResponseCreateBody,
)

@Serializable
data class RealtimeResponseCreateBody(
    val instructions: String,
)

private val eventJson = Json {
    encodeDefaults = true
    explicitNulls = false
}

fun realtimeOpeningInstructions(opening: String): String =
    "The 1:1 has started. Speak this opening line verbatim, then wait and listen. Opening: $opening"

fun realtimeOpeningEventJson(opening: String, json: Json = eventJson): String =
    json.encodeToString(
        RealtimeResponseCreateEvent.serializer(),
        RealtimeResponseCreateEvent(
            response = RealtimeResponseCreateBody(realtimeOpeningInstructions(opening)),
        ),
    )
