package ai.hardtalk.source.domain

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.CoachingDimension
import ai.hardtalk.source.domain.model.CoachingInsights
import ai.hardtalk.source.domain.model.CounterpartTone
import ai.hardtalk.source.domain.model.DimensionDelta
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.PracticeLoop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoachingInsightsTest {

    @Test
    fun `toneOf maps FastAPI mood strings to coaching labels`() {
        assertEquals(
            CounterpartTone.WARMING,
            CoachingInsights.toneOf("opening up and more receptive"),
        )
        assertEquals(
            CounterpartTone.GUARDED,
            CoachingInsights.toneOf("defensive and cautious"),
        )
        assertEquals(
            CounterpartTone.NEUTRAL,
            CoachingInsights.toneOf("listening, weighing what you said"),
        )
        assertEquals(CounterpartTone.NEUTRAL, CoachingInsights.toneOf("curious"))
    }

    @Test
    fun `progress is null until a scored user turn exists`() {
        val opening = listOf(ChatMessage(ChatRole.COUNTERPART, "Hey — what's up?"))
        assertNull(CoachingInsights.progress(opening))
        assertTrue(CoachingInsights.scoredTurns(opening).isEmpty())
    }

    @Test
    fun `progress tracks score history and weakest dimension`() {
        val messages = scoredRound(
            Feedback(50, 40, 55, 48, listOf("Acknowledge them.")),
            Feedback(65, 50, 60, 58, listOf("Lead with the ask.")),
            Feedback(78, 48, 72, 66, listOf("Name how they feel.")),
        )
        val progress = CoachingInsights.progress(messages)
        assertNotNull(progress)
        assertEquals(listOf(48, 58, 66), progress.overallScores)
        assertEquals(3, progress.turns.size)
        assertEquals(48, progress.overallFirst)
        assertEquals(66, progress.overallLast)
        assertEquals(18, progress.overallChange)
        assertEquals(CoachingDimension.EMPATHY, progress.weakest.dimension)
        assertEquals(40, progress.weakest.first)
        assertEquals(48, progress.weakest.last)
        assertEquals(CoachingDimension.CLARITY, progress.strongest.dimension)
        assertEquals(CoachingDimension.CLARITY, progress.mostImproved.dimension)
        assertTrue(progress.roundComplete)
        assertTrue(progress.takeaway.contains("Empathy", ignoreCase = true))
        assertTrue(progress.takeaway.contains("acknowledgment", ignoreCase = true))
    }

    @Test
    fun `in-progress takeaway names the dimension to watch`() {
        val messages = scoredRound(
            Feedback(80, 42, 70, 64, listOf("Acknowledge them first.")),
        )
        val progress = CoachingInsights.progress(messages)
        assertNotNull(progress)
        assertFalse(progress.roundComplete)
        assertEquals("Watch empathy on the next reply.", progress.takeaway)
    }

    @Test
    fun `strong round takeaway points at the closing goal`() {
        val takeaway = CoachingInsights.buildTakeaway(
            weakest = DimensionDelta(CoachingDimension.EMPATHY, 70, 72),
            overallFirst = 74,
            overallLast = 80,
            roundComplete = true,
            goals = listOf(
                "State clearly that you want a raise",
                "Back it up with specific accomplishments",
                "Stay collaborative rather than combative",
            ),
        )
        assertTrue(takeaway.startsWith("Strong round"))
        assertTrue(takeaway.contains("stay collaborative rather than combative"))
    }

    @Test
    fun `slipped scores get a reset-the-ask takeaway`() {
        val takeaway = CoachingInsights.buildTakeaway(
            weakest = DimensionDelta(CoachingDimension.ASSERTIVENESS, 60, 40),
            overallFirst = 70,
            overallLast = 50,
            roundComplete = true,
            goals = emptyList(),
        )
        assertEquals(
            "Scores slipped (70 → 50). Acknowledge them first, then restate the ask in one sentence.",
            takeaway,
        )
    }

    @Test
    fun `clarity limiter takeaway asks for a concrete example`() {
        val takeaway = CoachingInsights.buildTakeaway(
            weakest = DimensionDelta(CoachingDimension.CLARITY, 40, 44),
            overallFirst = 55,
            overallLast = 58,
            roundComplete = true,
            goals = emptyList(),
        )
        assertTrue(takeaway.contains("Clarity was the limiter"))
        assertTrue(takeaway.contains("number or example"))
    }

    @Test
    fun `assertiveness lift still names it as the limiter`() {
        val takeaway = CoachingInsights.buildTakeaway(
            weakest = DimensionDelta(CoachingDimension.ASSERTIVENESS, 30, 50),
            overallFirst = 50,
            overallLast = 62,
            roundComplete = true,
            goals = emptyList(),
        )
        assertTrue(takeaway.contains("Assertiveness improved"))
        assertTrue(takeaway.contains("I'd like"))
    }

    @Test
    fun `formatChange keeps a signed coaching delta`() {
        assertEquals("+12", CoachingInsights.formatChange(12))
        assertEquals("-5", CoachingInsights.formatChange(-5))
        assertEquals("0", CoachingInsights.formatChange(0))
    }

    @Test
    fun `a full round is exactly MAX_USER_TURNS scored replies`() {
        assertEquals(3, PracticeLoop.MAX_USER_TURNS)
        val messages = scoredRound(
            Feedback(50, 50, 50, 50, emptyList()),
            Feedback(60, 60, 60, 60, emptyList()),
            Feedback(70, 70, 70, 70, emptyList()),
        )
        assertEquals(3, CoachingInsights.scoredTurns(messages).size)
        assertTrue(CoachingInsights.progress(messages)!!.roundComplete)
    }

    private fun scoredRound(vararg turns: Feedback): List<ChatMessage> {
        val messages = mutableListOf(ChatMessage(ChatRole.COUNTERPART, "Opening"))
        turns.forEach { feedback ->
            messages += ChatMessage(ChatRole.USER, "Practice line", feedback)
            messages += ChatMessage(ChatRole.COUNTERPART, "Reply")
        }
        return messages
    }
}
