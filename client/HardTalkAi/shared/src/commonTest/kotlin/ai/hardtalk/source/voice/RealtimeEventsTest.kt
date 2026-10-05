package ai.hardtalk.source.voice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealtimeEventsTest {

    @Test
    fun `opening event is valid json and keeps the spoken line`() {
        val opening = "What did you want to talk about?"
        val encoded = realtimeOpeningEventJson(opening)
        val root = Json.parseToJsonElement(encoded).jsonObject
        assertEquals("response.create", root["type"]?.jsonPrimitive?.content)
        val instructions = root["response"]?.jsonObject
            ?.get("instructions")
            ?.jsonPrimitive
            ?.content
        assertEquals(realtimeOpeningInstructions(opening), instructions)
        assertTrue(opening in instructions.orEmpty())
    }

    @Test
    fun `control characters are escaped by the serializer`() {
        val opening = "line\tone\nback\u0008feed\u000cnull\u0001"
        val encoded = realtimeOpeningEventJson(opening)
        assertFalse("\t" in encoded)
        assertFalse("\n" in encoded.substringAfter("{"))
        assertFalse("\u0008" in encoded)
        assertFalse("\u000c" in encoded)
        assertFalse("\u0001" in encoded)
        val instructions = Json.parseToJsonElement(encoded)
            .jsonObject["response"]
            ?.jsonObject
            ?.get("instructions")
            ?.jsonPrimitive
            ?.content
        assertEquals(realtimeOpeningInstructions(opening), instructions)
        assertTrue("\\t" in encoded || "\\u0009" in encoded.lowercase())
    }

    @Test
    fun `quotes and slashes do not break the event`() {
        val opening = """She said "ship it" \ already"""
        val encoded = realtimeOpeningEventJson(opening)
        val instructions = Json.parseToJsonElement(encoded)
            .jsonObject["response"]
            ?.jsonObject
            ?.get("instructions")
            ?.jsonPrimitive
            ?.content
        assertEquals(realtimeOpeningInstructions(opening), instructions)
    }
}
