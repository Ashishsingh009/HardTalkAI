package ai.hardtalk.source.domain.model

/**
 * Client-side presentation of the existing FastAPI feedback payload.
 * No extra API fields — Heather's coaching screens are derived from
 * clarity / empathy / assertiveness, tips, and counterpart mood.
 */
enum class CoachingDimension(val label: String) {
    CLARITY("Clarity"),
    EMPATHY("Empathy"),
    ASSERTIVENESS("Assertiveness"),
}

enum class CounterpartTone(val shortLabel: String) {
    WARMING("Opening up"),
    NEUTRAL("Weighing it"),
    GUARDED("Guarded"),
}

data class TurnScore(
    val turnNumber: Int,
    val feedback: Feedback,
)

data class DimensionDelta(
    val dimension: CoachingDimension,
    val first: Int,
    val last: Int,
) {
    val change: Int get() = last - first
}

data class RoundProgress(
    val turns: List<TurnScore>,
    val overallFirst: Int,
    val overallLast: Int,
    val overallChange: Int,
    val overallScores: List<Int>,
    val deltas: List<DimensionDelta>,
    val weakest: DimensionDelta,
    val strongest: DimensionDelta,
    val mostImproved: DimensionDelta,
    val takeaway: String,
    val roundComplete: Boolean,
)

object CoachingInsights {

    fun toneOf(mood: String): CounterpartTone {
        val text = mood.lowercase()
        return when {
            listOf("opening", "receptive", "warming", "warm").any { it in text } ->
                CounterpartTone.WARMING
            listOf("defensive", "cautious", "guarded", "closed").any { it in text } ->
                CounterpartTone.GUARDED
            else -> CounterpartTone.NEUTRAL
        }
    }

    fun scoredTurns(messages: List<ChatMessage>): List<TurnScore> =
        messages
            .filter { it.role == ChatRole.USER && it.feedback != null }
            .mapIndexed { index, message ->
                TurnScore(turnNumber = index + 1, feedback = message.feedback!!)
            }

    fun progress(
        messages: List<ChatMessage>,
        goals: List<String> = emptyList(),
    ): RoundProgress? {
        val turns = scoredTurns(messages)
        if (turns.isEmpty()) return null

        val first = turns.first().feedback
        val last = turns.last().feedback
        val deltas = listOf(
            DimensionDelta(CoachingDimension.CLARITY, first.clarity, last.clarity),
            DimensionDelta(CoachingDimension.EMPATHY, first.empathy, last.empathy),
            DimensionDelta(CoachingDimension.ASSERTIVENESS, first.assertiveness, last.assertiveness),
        )
        val weakest = deltas.minWith(
            compareBy<DimensionDelta> { it.last }
                .thenBy { it.change }
                .thenBy { it.dimension.ordinal },
        )
        val strongest = deltas.maxWith(
            compareBy<DimensionDelta> { it.last }
                .thenBy { it.change }
                .thenByDescending { it.dimension.ordinal },
        )
        val mostImproved = deltas.maxWith(
            compareBy<DimensionDelta> { it.change }
                .thenBy { it.last }
                .thenBy { it.dimension.ordinal },
        )
        val complete = turns.size >= PracticeLoop.MAX_USER_TURNS
        return RoundProgress(
            turns = turns,
            overallFirst = first.overall,
            overallLast = last.overall,
            overallChange = last.overall - first.overall,
            overallScores = turns.map { it.feedback.overall },
            deltas = deltas,
            weakest = weakest,
            strongest = strongest,
            mostImproved = mostImproved,
            takeaway = buildTakeaway(
                weakest = weakest,
                overallFirst = first.overall,
                overallLast = last.overall,
                roundComplete = complete,
                goals = goals,
            ),
            roundComplete = complete,
        )
    }

    fun formatChange(change: Int): String = when {
        change > 0 -> "+$change"
        change < 0 -> "$change"
        else -> "0"
    }

    internal fun buildTakeaway(
        weakest: DimensionDelta,
        overallFirst: Int,
        overallLast: Int,
        roundComplete: Boolean,
        goals: List<String>,
    ): String {
        if (!roundComplete) {
            return "Watch ${weakest.dimension.label.lowercase()} on the next reply."
        }

        val overallChange = overallLast - overallFirst
        if (overallLast >= 75 && weakest.last >= 60) {
            val closer = goals.lastOrNull()
                ?.replaceFirstChar { char -> char.lowercase() }
                ?.let { " Next: $it." }
                ?: " Next: lock a concrete next step so the conversation does not stall."
            return "Strong round — clarity, empathy, and assertiveness all landed.$closer"
        }
        if (overallChange <= -8) {
            return "Scores slipped ($overallFirst → $overallLast). Acknowledge them first, then restate the ask in one sentence."
        }

        val range = "${weakest.first} → ${weakest.last}"
        val lifted = weakest.change >= 8
        return when (weakest.dimension) {
            CoachingDimension.EMPATHY -> if (lifted) {
                "Empathy improved ($range) but is still the one to practice. Lead with acknowledgment before the ask."
            } else {
                "Empathy was the soft spot ($range). Lead with acknowledgment before the ask."
            }
            CoachingDimension.ASSERTIVENESS -> if (lifted) {
                "Assertiveness improved ($range) but is still the limiter. Lead with a clear \"I'd like…\" so the ask cannot be missed."
            } else {
                "Assertiveness lagged ($range). Lead with a clear \"I'd like…\" so the ask cannot be missed."
            }
            CoachingDimension.CLARITY -> if (lifted) {
                "Clarity improved ($range) but is still the limiter. Add one number or example so the point lands."
            } else {
                "Clarity was the limiter ($range). Add one number or example so the point lands."
            }
        }
    }
}
