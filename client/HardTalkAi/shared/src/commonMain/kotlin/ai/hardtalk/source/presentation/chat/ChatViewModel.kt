package ai.hardtalk.source.presentation.chat

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.repository.PracticeRepository
import ai.hardtalk.source.voice.VoiceCallEvent
import ai.hardtalk.source.voice.VoiceCallFactory
import ai.hardtalk.source.voice.VoiceCallSession
import ai.hardtalk.source.voice.toCallConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val scenario: Scenario,
    private val practiceRepository: PracticeRepository,
    private val voiceSupported: Boolean = false,
    private val voiceCallFactory: VoiceCallFactory? = null,
) : ViewModel() {

    private var attemptId = 0
    private var activeCall: VoiceCallSession? = null
    private var callEventsJob: Job? = null

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        if (voiceSupported) {
            viewModelScope.launch {
                runCatching { practiceRepository.getHealth() }
                    .onSuccess { health ->
                        _uiState.update { it.copy(serverVoiceAvailable = health.voice) }
                    }
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun retryScenario() {
        stopCallQuietly()
        attemptId += 1
        val voice = _uiState.value.serverVoiceAvailable
        _uiState.value = initialState().copy(serverVoiceAvailable = voice)
    }

    fun setCallMuted(muted: Boolean) {
        activeCall?.setMuted(muted)
        _uiState.update { it.copy(callMuted = muted) }
    }

    fun hangUpCall() {
        activeCall?.hangUp()
    }

    fun startCall() {
        val current = _uiState.value
        if (!current.callAvailable) return
        val factory = voiceCallFactory ?: return
        val capturedAttempt = attemptId
        _uiState.update {
            it.copy(
                inCall = true,
                sending = true,
                callMuted = false,
                liveUserTurns = 0,
                callStatus = "Connecting to ${it.scenario.persona.name}…",
                error = null,
            )
        }
        viewModelScope.launch {
            val session = runCatching {
                practiceRepository.createVoiceSession(current.scenario.id)
            }.getOrElse { error ->
                if (capturedAttempt != attemptId) return@launch
                _uiState.update {
                    it.copy(
                        inCall = false,
                        sending = false,
                        callStatus = "",
                        error = error.message ?: "Could not start the counterpart call",
                    )
                }
                return@launch
            }
            if (capturedAttempt != attemptId) return@launch
            val call = factory.create()
            if (call == null) {
                _uiState.update {
                    it.copy(
                        inCall = false,
                        sending = false,
                        callStatus = "",
                        error = "Voice calls are not available on this device.",
                    )
                }
                return@launch
            }
            activeCall = call
            callEventsJob = viewModelScope.launch {
                call.events.collect { event ->
                    if (capturedAttempt != attemptId) return@collect
                    handleCallEvent(event, capturedAttempt)
                }
            }
            runCatching { call.connect(session.toCallConfig()) }
                .onFailure { error ->
                    if (capturedAttempt != attemptId) return@launch
                    stopCallQuietly()
                    _uiState.update {
                        it.copy(
                            inCall = false,
                            sending = false,
                            callStatus = "",
                            error = error.message ?: "Could not reach the counterpart",
                        )
                    }
                }
        }
    }

    fun send() {
        val current = _uiState.value
        val message = current.input.trim()
        if (message.isEmpty() || !current.canSend) return

        val history = current.messages
        val userTurn = ChatMessage(role = ChatRole.USER, content = message)
        val capturedAttempt = attemptId
        _uiState.update {
            it.copy(
                messages = it.messages + userTurn,
                input = "",
                sending = true,
                error = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                practiceRepository.sendMessage(
                    scenarioId = current.scenario.id,
                    message = message,
                    history = history,
                )
            }.onSuccess { result ->
                if (capturedAttempt != attemptId) return@launch
                _uiState.update { state ->
                    val withFeedback = state.messages.mapIndexed { index, turn ->
                        if (index == state.messages.lastIndex && turn.role == ChatRole.USER) {
                            turn.copy(feedback = result.feedback)
                        } else {
                            turn
                        }
                    }
                    state.copy(
                        messages = withFeedback + ChatMessage(
                            role = ChatRole.COUNTERPART,
                            content = result.reply,
                        ),
                        mood = result.mood,
                        sending = false,
                    )
                }
            }.onFailure { error ->
                if (capturedAttempt != attemptId) return@launch
                _uiState.update {
                    it.copy(
                        sending = false,
                        error = error.message ?: "Failed to send message",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        stopCallQuietly()
        super.onCleared()
    }

    private suspend fun handleCallEvent(event: VoiceCallEvent, capturedAttempt: Int) {
        if (capturedAttempt != attemptId) return
        when (event) {
            is VoiceCallEvent.Status -> _uiState.update { it.copy(callStatus = event.text) }
            is VoiceCallEvent.Transcript -> {
                _uiState.update {
                    it.copy(
                        liveUserTurns = event.userTurnCount,
                        callStatus = if (event.role == ChatRole.USER) {
                            "Your turn ${event.userTurnCount}"
                        } else {
                            "${it.scenario.persona.name} is speaking…"
                        },
                    )
                }
            }
            is VoiceCallEvent.Failed -> {
                activeCall = null
                _uiState.update {
                    it.copy(
                        inCall = false,
                        sending = false,
                        callStatus = "",
                        error = event.message,
                    )
                }
            }
            is VoiceCallEvent.Ended -> {
                activeCall = null
                if (event.turns.none { it.role == ChatRole.USER }) {
                    _uiState.update {
                        it.copy(
                            inCall = false,
                            sending = false,
                            callStatus = "",
                            liveUserTurns = 0,
                        )
                    }
                    return
                }
                _uiState.update {
                    it.copy(
                        callStatus = "Scoring the call…",
                        sending = true,
                        inCall = true,
                    )
                }
                runCatching {
                    practiceRepository.completeVoiceRound(scenario.id, event.turns)
                }.onSuccess { result ->
                    if (capturedAttempt != attemptId) return
                    _uiState.update {
                        it.copy(
                            messages = result.messages.ifEmpty { it.messages },
                            mood = result.mood,
                            inCall = false,
                            sending = false,
                            callStatus = "",
                            liveUserTurns = 0,
                            error = null,
                        )
                    }
                }.onFailure { error ->
                    if (capturedAttempt != attemptId) return
                    _uiState.update {
                        it.copy(
                            messages = if (event.turns.isNotEmpty()) event.turns else it.messages,
                            inCall = false,
                            sending = false,
                            callStatus = "",
                            error = error.message ?: "Could not score the call",
                        )
                    }
                }
            }
        }
    }

    private fun stopCallQuietly() {
        callEventsJob?.cancel()
        callEventsJob = null
        val call = activeCall
        activeCall = null
        runCatching { call?.hangUp() }
    }

    private fun initialState(): ChatUiState = ChatUiState(
        scenario = scenario,
        messages = listOf(
            ChatMessage(role = ChatRole.COUNTERPART, content = scenario.opening),
        ),
        mood = scenario.persona.mood,
        voiceSupported = voiceSupported,
    )
}
