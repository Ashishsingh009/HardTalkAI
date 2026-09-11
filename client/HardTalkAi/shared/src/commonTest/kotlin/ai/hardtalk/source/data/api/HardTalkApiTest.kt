package ai.hardtalk.source.data.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HardTalkApiTest {

    @Test
    fun `getScenarios returns list from FastAPI envelope`() = runTest {
        val api = apiWith(
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertTrue(request.url.toString().endsWith("/api/scenarios"))
                respond(
                    content = """{"scenarios":[{"id":"decline-request","title":"Say no","summary":"Full plate","difficulty":"warm-up","persona":{"name":"Priya","role":"Stakeholder","mood":"persistent"},"opening":"Can you start today?","goals":["Decline clearly"]}]}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
            },
        )

        val scenarios = api.getScenarios()
        assertEquals(1, scenarios.size)
        assertEquals("decline-request", scenarios[0].id)
        assertEquals("Priya", scenarios[0].persona.name)
        assertFalse(scenarios[0].free)
    }

    @Test
    fun `sendChat posts scenarioId message and history then parses reply`() = runTest {
        val api = apiWith(
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertTrue(request.url.toString().endsWith("/api/chat"))
                val body = (request.body as TextContent).text
                assertTrue("ask-for-raise" in body)
                assertTrue("I'd like a raise" in body)
                assertTrue("counterpart" in body)
                respond(
                    content = """{"reply":"Tell me more.","feedback":{"clarity":82,"empathy":64,"assertiveness":71,"overall":72,"tips":["Cite a metric."]},"mood":"engaged"}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
            },
        )

        val result = api.sendChat(
            scenarioId = "ask-for-raise",
            message = "I'd like a raise",
            history = listOf(
                ai.hardtalk.source.data.api.dto.ChatTurnDto("counterpart", "Hey, what did you want to talk about?"),
            ),
        )
        assertEquals("Tell me more.", result.reply)
        assertEquals(82, result.feedback.clarity)
        assertEquals("engaged", result.mood)
    }

    @Test
    fun `sendChat surfaces FastAPI error body`() = runTest {
        val api = apiWith(
            MockEngine {
                respond(
                    content = """{"error":"Unknown scenario: nope"}""",
                    status = HttpStatusCode.NotFound,
                    headers = jsonHeaders,
                )
            },
        )

        val error = assertFailsWith<ApiException> {
            api.sendChat("nope", "hi there", emptyList())
        }
        assertEquals("Unknown scenario: nope", error.message)
    }

    private fun apiWith(engine: MockEngine): HardTalkApi = HardTalkApi(
        baseUrl = "http://10.0.2.2:3001/",
        client = createHardTalkHttpClient(engine),
    )

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
}
