package ai.hardtalk.source.presentation

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.Persona
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
        val viewModel = ScenarioListViewModel(FakePracticeRepository())
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Ready)
        assertEquals(listOf("ask-for-raise"), state.scenarios.map { it.id })
    }

    @Test
    fun `scenario list surfaces load errors`() = runTest {
        val viewModel = ScenarioListViewModel(FakePracticeRepository(failLoad = true))
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Error)
        assertEquals("offline", state.message)
    }

    @Test
    fun `chat starts with counterpart opening and scored send`() = runTest {
        val viewModel = ChatViewModel(sampleScenario(), FakePracticeRepository())
        val opened = viewModel.uiState.value
        assertEquals(1, opened.messages.size)
        assertEquals(ChatRole.COUNTERPART, opened.messages[0].role)
        assertEquals(sampleScenario().opening, opened.messages[0].content)

        viewModel.onInputChange("I shipped 3 launches and would like a raise.")
        viewModel.send()

        val after = viewModel.uiState.value
        assertFalse(after.sending)
        assertEquals(2, after.messages.count { it.role == ChatRole.USER || it.role == ChatRole.COUNTERPART })
        assertEquals(3, after.messages.size)
        val userTurn = after.messages[1]
        assertEquals(ChatRole.USER, userTurn.role)
        assertEquals(80, userTurn.feedback?.clarity)
        assertEquals(70, userTurn.feedback?.empathy)
        assertEquals(75, userTurn.feedback?.assertiveness)
        assertEquals("Walk me through the number.", after.messages[2].content)
        assertEquals("curious", after.mood)
        assertEquals("", after.input)
    }

    @Test
    fun `chat ignores empty send`() = runTest {
        val viewModel = ChatViewModel(sampleScenario(), FakePracticeRepository())
        viewModel.send()
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `chat surfaces send errors`() = runTest {
        val viewModel = ChatViewModel(sampleScenario(), FakePracticeRepository(failSend = true))
        viewModel.onInputChange("hello there")
        viewModel.send()
        val state = viewModel.uiState.value
        assertEquals("boom", state.error)
        assertFalse(state.sending)
        assertEquals(2, state.messages.size)
    }

    private class FakePracticeRepository(
        private val failLoad: Boolean = false,
        private val failSend: Boolean = false,
    ) : PracticeRepository {
        override suspend fun getScenarios(): List<Scenario> {
            if (failLoad) error("offline")
            return listOf(sampleScenario())
        }

        override suspend fun sendMessage(
            scenarioId: String,
            message: String,
            history: List<ChatMessage>,
        ): ReplyResult {
            if (failSend) error("boom")
            assertEquals("ask-for-raise", scenarioId)
            assertTrue(history.isNotEmpty())
            return ReplyResult(
                reply = "Walk me through the number.",
                feedback = Feedback(
                    clarity = 80,
                    empathy = 70,
                    assertiveness = 75,
                    overall = 75,
                    tips = listOf("Cite a metric."),
                ),
                mood = "curious",
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
)
