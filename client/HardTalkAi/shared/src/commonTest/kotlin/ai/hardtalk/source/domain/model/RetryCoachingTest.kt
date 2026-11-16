package ai.hardtalk.source.domain.model

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetryCoachingTest {

    @Test
    fun `selects the best card and quotes the matching scored user turn`() = runTest {
        val scenarioId = "decline-request"
        val cards = RetryCoachingCards.forScenario(scenarioId)
        val targetIndex = 1
        val question = "Could you own the dashboard by Friday?"
        val attempt = "I can probably fit in the dashboard, although I'm already stretched."
        val irrelevant = "I saw the demo yesterday."
        val unfinished = "I am still editing this message."
        val model = FakeModel { inputs ->
            assertEquals(cards.size + 2, inputs.size)
            assertTrue(inputs.take(2).any { attempt in it })
            assertTrue(inputs.take(2).any { irrelevant in it })
            assertFalse(inputs.any { question in it || unfinished in it })
            val queryVectors = inputs.take(2).map { text ->
                if (attempt in text) vector(1f, 0f, 0f) else vector(0f, 0f, 1f)
            }
            queryVectors + cards.mapIndexed { index, _ ->
                if (index == targetIndex) vector(1f, 0f, 0f) else vector(0f, 1f, 0f)
            }
        }

        val suggestion = assertNotNull(
            RetryCoaching(model).suggest(
                scenarioId,
                listOf(
                    ChatMessage(ChatRole.COUNTERPART, question),
                    scoredUser(attempt),
                    scoredUser(irrelevant),
                    ChatMessage(ChatRole.USER, unfinished),
                ),
            ),
        )

        assertEquals(attempt, suggestion.quote)
        assertEquals(cards[targetIndex].guidance, suggestion.guidance)
        assertEquals(cards[targetIndex].example, suggestion.example)
        assertEquals(1, model.batchCount)
    }

    @Test
    fun `skips inference for unsupported drills or missing scored user turns`() = runTest {
        val model = FakeModel { error("Unexpected inference") }
        val coaching = RetryCoaching(model)

        assertTrue(coaching.supports("ask-for-raise"))
        assertTrue(coaching.supports("give-feedback"))
        assertTrue(coaching.supports("decline-request"))
        assertFalse(coaching.supports("chase-overdue-invoice"))
        assertNull(coaching.suggest("chase-overdue-invoice", listOf(scoredUser("Please pay Friday."))))
        assertNull(
            coaching.suggest(
                "ask-for-raise",
                listOf(
                    ChatMessage(ChatRole.COUNTERPART, "What are you asking for?", feedback),
                    ChatMessage(ChatRole.USER, "Unscored draft."),
                    scoredUser("  "),
                ),
            ),
        )
        assertEquals(0, model.batchCount)
    }

    @Test
    fun `keeps the default takeaway when retrieval is weak or ambiguous`() = runTest {
        val messages = listOf(scoredUser("I'd like to talk about my next steps."))
        val weak = FakeModel { inputs ->
            listOf(vector(1f, 0f)) + List(inputs.size - 1) { vector(0f, 1f) }
        }
        val ambiguous = FakeModel { inputs -> List(inputs.size) { vector(1f, 0f) } }

        assertNull(RetryCoaching(weak).suggest("ask-for-raise", messages))
        assertNull(RetryCoaching(ambiguous).suggest("ask-for-raise", messages))
    }

    @Test
    fun `keeps the default takeaway after unusable embedding results`() = runTest {
        val messages = listOf(scoredUser("I want to make sure the next release goes smoothly."))
        val wrongCount = FakeModel { emptyList() }
        val wrongDimension = FakeModel { inputs ->
            listOf(vector(1f, 0f)) + List(inputs.size - 1) { vector(1f, 0f, 0f) }
        }
        val nonFinite = FakeModel { inputs -> List(inputs.size) { vector(Float.NaN, 1f) } }
        val zero = FakeModel { inputs -> List(inputs.size) { vector(0f, 0f) } }

        for (model in listOf(wrongCount, wrongDimension, nonFinite, zero)) {
            assertNull(RetryCoaching(model).suggest("give-feedback", messages))
        }
    }

    private val feedback = Feedback(50, 50, 50, 50, emptyList())

    private fun scoredUser(content: String) = ChatMessage(ChatRole.USER, content, feedback)

    private fun vector(vararg components: Float) = components

    private class FakeModel(
        private val vectors: (List<String>) -> List<FloatArray>,
    ) : RetryCoachingModel {
        var batchCount = 0
            private set

        override suspend fun isAvailable(): Boolean = true

        override suspend fun download(onProgress: (Int) -> Unit) = error("Unexpected download")

        override suspend fun embed(inputs: List<String>): List<FloatArray> {
            batchCount++
            return vectors(inputs)
        }
    }
}
