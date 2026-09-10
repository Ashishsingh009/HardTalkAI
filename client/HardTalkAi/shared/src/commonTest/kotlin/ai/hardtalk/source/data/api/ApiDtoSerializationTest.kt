package ai.hardtalk.source.data.api

import ai.hardtalk.source.data.api.dto.ApiErrorDto
import ai.hardtalk.source.data.api.dto.ChatRequestDto
import ai.hardtalk.source.data.api.dto.ChatTurnDto
import ai.hardtalk.source.data.api.dto.ReplyResultDto
import ai.hardtalk.source.data.api.dto.ScenariosResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiDtoSerializationTest {

    @Test
    fun `scenarios response matches FastAPI payload`() {
        val json = """
            {
              "scenarios": [
                {
                  "id": "ask-for-raise",
                  "title": "Ask your manager for a raise",
                  "summary": "You have delivered strong results.",
                  "difficulty": "moderate",
                  "persona": {
                    "name": "Dana",
                    "role": "Your engineering manager",
                    "mood": "busy and slightly guarded about budget"
                  },
                  "opening": "Hey, thanks for grabbing time.",
                  "goals": ["State clearly that you want a raise"]
                }
              ]
            }
        """.trimIndent()

        val parsed = apiJson.decodeFromString(ScenariosResponseDto.serializer(), json)
        assertEquals(1, parsed.scenarios.size)
        assertEquals("ask-for-raise", parsed.scenarios[0].id)
        assertEquals("Dana", parsed.scenarios[0].persona.name)
        assertEquals("moderate", parsed.scenarios[0].difficulty)
    }

    @Test
    fun `chat request encodes camelCase scenarioId`() {
        val encoded = apiJson.encodeToString(
            ChatRequestDto.serializer(),
            ChatRequestDto(
                scenarioId = "ask-for-raise",
                message = "I'd like a raise.",
                history = listOf(ChatTurnDto(role = "counterpart", content = "Hi")),
            ),
        )
        assertTrue("scenarioId" in encoded)
        assertTrue("ask-for-raise" in encoded)
        assertTrue("counterpart" in encoded)
    }

    @Test
    fun `reply result matches FastAPI payload`() {
        val json = """
            {
              "reply": "Walk me through the impact.",
              "feedback": {
                "clarity": 80,
                "empathy": 70,
                "assertiveness": 75,
                "overall": 75,
                "tips": ["Name a specific number."]
              },
              "mood": "curious but still guarded"
            }
        """.trimIndent()

        val parsed = apiJson.decodeFromString(ReplyResultDto.serializer(), json)
        assertEquals("Walk me through the impact.", parsed.reply)
        assertEquals(80, parsed.feedback.clarity)
        assertEquals(70, parsed.feedback.empathy)
        assertEquals(75, parsed.feedback.assertiveness)
        assertEquals(listOf("Name a specific number."), parsed.feedback.tips)
        assertEquals("curious but still guarded", parsed.mood)
    }

    @Test
    fun `error payload decodes`() {
        val parsed = apiJson.decodeFromString(ApiErrorDto.serializer(), """{"error":"message is required"}""")
        assertEquals("message is required", parsed.error)
    }

    @Test
    fun `normalizeApiBaseUrl strips trailing slash`() {
        assertEquals("http://10.0.2.2:3001", normalizeApiBaseUrl("http://10.0.2.2:3001/"))
    }
}
