package ai.hardtalk.source

import ai.hardtalk.source.data.api.HardTalkApi
import ai.hardtalk.source.data.api.createHardTalkHttpClient
import ai.hardtalk.source.data.api.provideHttpClientEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Hits a local FastAPI process when one is running (typically :3001).
 * Skips cleanly when the API is down so unit-test CI stays green.
 */
class LiveFastApiPracticeTurnTest {

    @Test
    fun completeOneScoredPracticeTurnAgainstLocalApi() = runBlocking {
        val baseUrl = System.getenv("HARDTALK_API_BASE_URL") ?: "http://127.0.0.1:3001"
        val api = HardTalkApi(baseUrl, createHardTalkHttpClient(provideHttpClientEngine()))
        val scenarios = runCatching { api.getScenarios() }.getOrElse { error ->
            println("Skipping live FastAPI turn; $baseUrl is unreachable (${error.message})")
            return@runBlocking
        }
        assertTrue(scenarios.isNotEmpty(), "API returned no scenarios")

        val scenario = scenarios.first { it.id == "ask-for-raise" }
        val result = api.sendChat(
            scenarioId = scenario.id,
            message = "I appreciate your time. I'd like a raise: I shipped 3 launches and cut latency 40%. What's possible?",
            history = listOf(
                ai.hardtalk.source.data.api.dto.ChatTurnDto(
                    role = "counterpart",
                    content = scenario.opening,
                ),
            ),
        )
        assertTrue(result.reply.isNotBlank())
        assertTrue(result.feedback.clarity in 0..100)
        assertTrue(result.feedback.empathy in 0..100)
        assertTrue(result.feedback.assertiveness in 0..100)
        assertTrue(result.mood.isNotBlank())
        println("Live turn OK: mood=${result.mood} overall=${result.feedback.overall} reply=${result.reply.take(80)}")
    }
}
