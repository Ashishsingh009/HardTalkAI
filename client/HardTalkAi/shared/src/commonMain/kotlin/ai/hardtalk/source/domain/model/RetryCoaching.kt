package ai.hardtalk.source.domain.model

import kotlin.math.sqrt

interface RetryCoachingModel {
    suspend fun isAvailable(): Boolean
    suspend fun download(onProgress: (Int) -> Unit)
    suspend fun embed(inputs: List<String>): List<FloatArray>
}

data class RetryCoachingSuggestion(
    val quote: String,
    val guidance: String,
    val example: String,
)

internal data class RetryCoachingCard(
    val id: String,
    val trigger: String,
    val guidance: String,
    val example: String,
)

internal class RetryCoaching(private val model: RetryCoachingModel) {
    fun supports(scenarioId: String): Boolean = RetryCoachingCards.forScenario(scenarioId).isNotEmpty()

    suspend fun suggest(scenarioId: String, messages: List<ChatMessage>): RetryCoachingSuggestion? {
        val cards = RetryCoachingCards.forScenario(scenarioId)
        val turns = messages
            .filter { it.role == ChatRole.USER && it.feedback != null }
            .map { it.content.trim() }
            .filter { it.isNotEmpty() }
            .takeLast(PracticeLoop.MAX_USER_TURNS)
            .asReversed()
        if (cards.isEmpty() || turns.isEmpty()) return null

        val queries = turns.map { "task: search result | query: ${it.take(MAX_QUERY_CHARACTERS)}" }
        val documents = cards.map { "title: ${it.guidance} | text: ${it.trigger}" }
        val vectors = model.embed(queries + documents)
        if (vectors.size != queries.size + documents.size) return null

        val size = vectors.first().size
        val unitVectors = vectors.map { vector ->
            if (vector.size != size) return null
            normalize(vector) ?: return null
        }
        val queriesByTurn = unitVectors.take(queries.size)
        val matches = cards.mapIndexed { index, card ->
            val document = unitVectors[queries.size + index]
            queriesByTurn.mapIndexed { turnIndex, query ->
                Match(card, turns[turnIndex], cosine(query, document))
            }.maxBy { it.similarity }
        }.sortedByDescending { it.similarity }

        val best = matches.first()
        val runnerUp = matches.getOrNull(1)
        if (best.similarity < MIN_SIMILARITY ||
            (runnerUp != null && best.similarity - runnerUp.similarity < MIN_MARGIN)
        ) return null

        return RetryCoachingSuggestion(best.quote, best.card.guidance, best.card.example)
    }

    private data class Match(val card: RetryCoachingCard, val quote: String, val similarity: Double)

    private fun normalize(vector: FloatArray): DoubleArray? {
        val squaredLength = vector.sumOf { coordinate ->
            val value = coordinate.toDouble()
            value * value
        }
        if (!squaredLength.isFinite() || squaredLength == 0.0) return null
        val length = sqrt(squaredLength)
        return DoubleArray(vector.size) { vector[it] / length }
    }

    private fun cosine(a: DoubleArray, b: DoubleArray): Double =
        a.indices.sumOf { index -> a[index] * b[index] }

    private companion object {
        const val MAX_QUERY_CHARACTERS = 2_000
        // Initial retrieval thresholds. Tune against labeled recap examples from the text model.
        const val MIN_SIMILARITY = 0.50
        const val MIN_MARGIN = 0.04
    }
}
