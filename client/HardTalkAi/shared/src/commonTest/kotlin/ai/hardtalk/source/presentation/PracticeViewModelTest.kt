package ai.hardtalk.source.presentation

import ai.hardtalk.source.data.billing.FakeBillingRepository
import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.Persona
import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.domain.model.CoachingDimension
import ai.hardtalk.source.domain.model.CounterpartTone
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.repository.PracticeRepository
import ai.hardtalk.source.presentation.chat.ChatViewModel
import ai.hardtalk.source.presentation.scenarios.ScenarioListUiState
import ai.hardtalk.source.presentation.scenarios.ScenarioListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `scenario list loads from repository`() = runTest {
        val viewModel = ScenarioListViewModel(FakePracticeRepository(), FakeBillingRepository())
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Ready)
        assertEquals(listOf("ask-for-raise"), state.scenarios.map { it.id })
    }

    @Test
    fun `scenario list surfaces load errors`() = runTest {
        val viewModel = ScenarioListViewModel(
            FakePracticeRepository(failLoad = true),
            FakeBillingRepository(),
        )
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Error)
        assertEquals("offline", state.message)
    }

    @Test
    fun `free drill opens without paywall`() = runTest {
        val viewModel = ScenarioListViewModel(
            FakePracticeRepository(scenarios = listOf(sampleScenario(), paidScenario())),
            FakeBillingRepository(),
        )
        viewModel.onScenarioTapped(sampleScenario())
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Ready)
        assertEquals("ask-for-raise", state.openScenario?.id)
        assertNull(state.paywallScenario)
    }

    @Test
    fun `paid drill shows paywall until purchase`() = runTest {
        val billing = FakeBillingRepository()
        val viewModel = ScenarioListViewModel(
            FakePracticeRepository(scenarios = listOf(sampleScenario(), paidScenario())),
            billing,
        )
        viewModel.onScenarioTapped(paidScenario())
        val locked = viewModel.uiState.value
        assertTrue(locked is ScenarioListUiState.Ready)
        assertNull(locked.openScenario)
        assertEquals("give-feedback", locked.paywallScenario?.id)
        assertTrue(locked.isLocked(paidScenario()))

        viewModel.purchaseSelected()
        val unlocked = viewModel.uiState.value
        assertTrue(unlocked is ScenarioListUiState.Ready)
        assertEquals("give-feedback", unlocked.openScenario?.id)
        assertNull(unlocked.paywallScenario)
        assertTrue(unlocked.hasPro)
        assertEquals(1, billing.purchaseCalls)
    }

    @Test
    fun `chat starts with counterpart opening and scored send`() = runTest {
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(),
            FakeBillingRepository(),
        )
        val opened = viewModel.uiState.value
        assertEquals(1, opened.messages.size)
        assertEquals(ChatRole.COUNTERPART, opened.messages[0].role)
        assertEquals(sampleScenario().opening, opened.messages[0].content)

        viewModel.onInputChange("I shipped 3 launches and would like a raise.")
        viewModel.send()

        val after = viewModel.uiState.value
        assertFalse(after.sending)
        assertEquals(3, after.messages.size)
        val userTurn = after.messages[1]
        assertEquals(ChatRole.USER, userTurn.role)
        assertEquals(80, userTurn.feedback?.clarity)
        assertEquals(70, userTurn.feedback?.empathy)
        assertEquals(75, userTurn.feedback?.assertiveness)
        assertEquals("Walk me through the number.", after.messages[2].content)
        assertEquals("curious", after.mood)
        assertEquals(CounterpartTone.NEUTRAL, after.counterpartTone)
        assertEquals("", after.input)
        assertEquals(1, after.scoredUserTurns)
        assertTrue(after.canRetry)
        assertFalse(after.roundComplete)
        assertEquals(1, after.scoreHistory.size)
        assertEquals(80, after.scoreHistory[0].feedback.clarity)
        assertNotNull(after.roundProgress)
        assertFalse(after.roundProgress!!.roundComplete)
        assertEquals("Watch empathy on the next reply.", after.roundProgress!!.takeaway)
        assertNull(after.roundSummary)
    }

    @Test
    fun `retryScenario restores opening and clears scores`() = runTest {
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(),
            FakeBillingRepository(),
        )
        viewModel.onInputChange("I shipped 3 launches and would like a raise.")
        viewModel.send()
        viewModel.retryScenario()

        val reset = viewModel.uiState.value
        assertEquals(1, reset.messages.size)
        assertEquals(ChatRole.COUNTERPART, reset.messages[0].role)
        assertEquals(sampleScenario().opening, reset.messages[0].content)
        assertEquals(sampleScenario().persona.mood, reset.mood)
        assertEquals(0, reset.scoredUserTurns)
        assertFalse(reset.canRetry)
        assertFalse(reset.roundComplete)
        assertEquals("", reset.input)
        assertEquals(null, reset.error)
        assertNull(reset.roundProgress)
        assertNull(reset.roundSummary)
        assertTrue(reset.scoreHistory.isEmpty())
    }

    @Test
    fun `practice round ends after max scored turns and ignores further sends`() = runTest {
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(),
            FakeBillingRepository(),
        )
        repeat(PracticeLoop.MAX_USER_TURNS) { index ->
            viewModel.onInputChange("Attempt ${index + 1} with a clear ask and evidence.")
            viewModel.send()
        }

        val complete = viewModel.uiState.value
        assertTrue(complete.roundComplete)
        assertEquals(PracticeLoop.MAX_USER_TURNS, complete.scoredUserTurns)
        assertFalse(complete.canSend)
        val summary = complete.roundSummary
        assertNotNull(summary)
        assertEquals(listOf(75, 74, 73), summary.overallScores)
        assertEquals(CoachingDimension.EMPATHY, summary.weakest.dimension)
        assertEquals(52, summary.weakest.last)
        assertTrue(summary.takeaway.contains("Empathy", ignoreCase = true))
        assertTrue(summary.takeaway.contains("acknowledgment", ignoreCase = true))
        assertEquals(CounterpartTone.GUARDED, complete.counterpartTone)
        val sizeAfterRound = complete.messages.size

        viewModel.onInputChange("This should not send.")
        viewModel.send()
        assertEquals(sizeAfterRound, viewModel.uiState.value.messages.size)
        assertTrue(viewModel.uiState.value.roundComplete)

        viewModel.retryScenario()
        assertNull(viewModel.uiState.value.roundSummary)
        assertFalse(viewModel.uiState.value.roundComplete)
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun `chat ignores empty send`() = runTest {
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(),
            FakeBillingRepository(),
        )
        viewModel.send()
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `chat surfaces send errors`() = runTest {
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(failSend = true),
            FakeBillingRepository(),
        )
        viewModel.onInputChange("hello there")
        viewModel.send()
        val state = viewModel.uiState.value
        assertEquals("boom", state.error)
        assertFalse(state.sending)
        assertEquals(2, state.messages.size)
    }

    @Test
    fun `chat blocks send when the drill is locked`() = runTest {
        val viewModel = ChatViewModel(
            paidScenario(),
            FakePracticeRepository(),
            FakeBillingRepository(),
        )
        viewModel.onInputChange("I need to name the misses without piling on.")
        viewModel.send()
        val state = viewModel.uiState.value
        assertEquals("Unlock HardTalk Pro to practice this drill.", state.error)
        assertEquals(1, state.messages.size)
        assertFalse(state.sending)
    }

    private class FakePracticeRepository(
        private val failLoad: Boolean = false,
        private val failSend: Boolean = false,
        private val scenarios: List<Scenario> = listOf(sampleScenario()),
    ) : PracticeRepository {
        override suspend fun getScenarios(): List<Scenario> {
            if (failLoad) error("offline")
            return scenarios
        }

        override suspend fun sendMessage(
            scenarioId: String,
            message: String,
            history: List<ChatMessage>,
        ): ReplyResult {
            if (failSend) error("boom")
            assertEquals("ask-for-raise", scenarioId)
            assertTrue(history.isNotEmpty())
            val turnNumber = history.count { it.role == ChatRole.USER } + 1
            val feedback = when (turnNumber) {
                1 -> Feedback(80, 70, 75, 75, listOf("Cite a metric."))
                2 -> Feedback(72, 82, 68, 74, listOf("Lead with the ask."))
                else -> Feedback(88, 52, 80, 73, listOf("Acknowledge them first."))
            }
            return ReplyResult(
                reply = "Walk me through the number.",
                feedback = feedback,
                mood = if (turnNumber >= 3) "defensive and cautious" else "curious",
            )
        }
    }
}

private fun sampleScenario() = Scenario(
    id = "ask-for-raise",
    title = "Ask your manager for a raise",
    summary = "Make the case for a pay increase.",
    difficulty = "moderate",
    persona = Persona("Dana", "Your engineering manager", "busy"),
    opening = "What did you want to talk about?",
    goals = listOf("State clearly that you want a raise"),
    free = true,
)

private fun paidScenario() = Scenario(
    id = "give-feedback",
    title = "Give a teammate critical feedback",
    summary = "Name the pattern without torching the relationship.",
    difficulty = "hard",
    persona = Persona("Sam", "A peer on your squad", "defensive"),
    opening = "You wanted to chat?",
    goals = listOf("Name the specific misses"),
    free = false,
)
