package ai.hardtalk.source.presentation

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.Feedback
import ai.hardtalk.source.domain.model.Persona
import ai.hardtalk.source.domain.model.PracticeLoop
import ai.hardtalk.source.domain.model.CoachingDimension
import ai.hardtalk.source.domain.model.CounterpartTone
import ai.hardtalk.source.domain.model.HealthStatus
import ai.hardtalk.source.domain.model.ReplyResult
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.model.VoiceCompleteResult
import ai.hardtalk.source.domain.model.VoiceSession
import ai.hardtalk.source.domain.repository.PracticeRepository
import ai.hardtalk.source.presentation.chat.ChatViewModel
import ai.hardtalk.source.presentation.scenarios.ScenarioListUiState
import ai.hardtalk.source.presentation.scenarios.ScenarioListViewModel
import ai.hardtalk.source.voice.VoiceCallConfig
import ai.hardtalk.source.voice.VoiceCallEvent
import ai.hardtalk.source.voice.VoiceCallSession
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
        val viewModel = ChatViewModel(sampleScenario(), FakePracticeRepository())
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
        val viewModel = ChatViewModel(sampleScenario(), FakePracticeRepository())
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

    @Test
    fun `call button stays hidden when server voice is off`() = runTest {
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(voice = false),
            voiceSupported = true,
        )
        val state = viewModel.uiState.value
        assertTrue(state.voiceSupported)
        assertFalse(state.serverVoiceAvailable)
        assertFalse(state.callAvailable)
    }

    @Test
    fun `counterpart call hangup scores transcript and shows recap`() = runTest {
        val session = FakeVoiceCallSession()
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(voice = true),
            voiceSupported = true,
            voiceCallFactory = { session },
        )
        assertTrue(viewModel.uiState.value.callAvailable)

        viewModel.startCall()
        assertTrue(viewModel.uiState.value.inCall)
        assertFalse(viewModel.uiState.value.canSend)

        session.emit(
            VoiceCallEvent.Ended(
                turns = listOf(
                    ChatMessage(ChatRole.COUNTERPART, sampleScenario().opening),
                    ChatMessage(ChatRole.USER, "I'd like a raise with three launches."),
                    ChatMessage(ChatRole.COUNTERPART, "Walk me through the number."),
                    ChatMessage(ChatRole.USER, "I'd like 12 percent this cycle."),
                    ChatMessage(ChatRole.COUNTERPART, "I can take that upstairs."),
                    ChatMessage(ChatRole.USER, "I hear the freeze. I'd like a number before Friday."),
                ),
                reason = "max_turns",
            ),
        )

        val after = viewModel.uiState.value
        assertFalse(after.inCall)
        assertTrue(after.roundComplete)
        assertEquals(PracticeLoop.MAX_USER_TURNS, after.scoredUserTurns)
        assertNotNull(after.roundSummary)
        assertEquals("defensive and cautious", after.mood)
        assertEquals(CounterpartTone.GUARDED, after.counterpartTone)
        assertTrue(after.messages.any { it.role == ChatRole.USER && it.feedback != null })
    }

    @Test
    fun `typed send still works when voice is available`() = runTest {
        val viewModel = ChatViewModel(
            sampleScenario(),
            FakePracticeRepository(voice = true),
            voiceSupported = true,
        )
        assertTrue(viewModel.uiState.value.callAvailable)
        viewModel.onInputChange("I shipped 3 launches and would like a raise.")
        viewModel.send()
        val after = viewModel.uiState.value
        assertEquals(3, after.messages.size)
        assertEquals(1, after.scoredUserTurns)
        assertFalse(after.inCall)
    }

    private class FakePracticeRepository(
        private val failLoad: Boolean = false,
        private val failSend: Boolean = false,
        private val voice: Boolean = false,
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

        override suspend fun getHealth(): HealthStatus {
            if (failLoad) error("offline")
            return HealthStatus(engine = if (voice) "openai" else "builtin", voice = voice, scenarios = 1)
        }

        override suspend fun createVoiceSession(scenarioId: String): VoiceSession {
            if (failSend) error("boom")
            assertEquals("ask-for-raise", scenarioId)
            return VoiceSession(
                clientSecret = "ek_test",
                realtimeUrl = "https://api.openai.com/v1/realtime/calls",
                model = "gpt-realtime",
                voice = "coral",
                opening = sampleScenario().opening,
                instructions = "You are Dana.",
                personaName = "Dana",
                maxUserTurns = PracticeLoop.MAX_USER_TURNS,
                maxDurationSeconds = PracticeLoop.MAX_CALL_DURATION_SECONDS,
            )
        }

        override suspend fun completeVoiceRound(
            scenarioId: String,
            turns: List<ChatMessage>,
        ): VoiceCompleteResult {
            if (failSend) error("boom")
            val scored = mutableListOf<ChatMessage>()
            var userCount = 0
            for (turn in turns) {
                if (turn.role == ChatRole.USER) {
                    userCount += 1
                    val feedback = when (userCount) {
                        1 -> Feedback(80, 70, 75, 75, listOf("Cite a metric."))
                        2 -> Feedback(72, 82, 68, 74, listOf("Lead with the ask."))
                        else -> Feedback(88, 52, 80, 73, listOf("Acknowledge them first."))
                    }
                    scored += turn.copy(feedback = feedback)
                } else {
                    scored += turn
                }
            }
            return VoiceCompleteResult(
                messages = scored,
                mood = if (userCount >= 3) "defensive and cautious" else "curious",
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

private class FakeVoiceCallSession : VoiceCallSession {
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<VoiceCallEvent>(extraBufferCapacity = 16)
    override val events = _events
    var connected: VoiceCallConfig? = null
        private set

    override suspend fun connect(config: VoiceCallConfig) {
        connected = config
        _events.emit(VoiceCallEvent.Status("Live — they can hear you"))
    }

    override fun setMuted(muted: Boolean) = Unit

    override fun hangUp() = Unit

    suspend fun emit(event: VoiceCallEvent) {
        _events.emit(event)
    }
}
