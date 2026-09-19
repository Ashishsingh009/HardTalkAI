package ai.hardtalk.source.voice

import ai.hardtalk.source.domain.model.ChatMessage
import ai.hardtalk.source.domain.model.ChatRole
import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OpenAI Realtime over WebRTC. The remote party is the scenario counterpart.
 * Transcripts are collected for POST /api/voice/complete — Heather is not on this peer.
 */
class OpenAiRealtimeWebRtcSession : VoiceCallSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }
    private val _events = MutableSharedFlow<VoiceCallEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<VoiceCallEvent> = _events.asSharedFlow()

    private val finished = AtomicBoolean(false)
    private val turns = mutableListOf<ChatMessage>()
    private var userTurns = 0
    private var maxUserTurns = 3
    private var durationJob: Job? = null
    private var audioManager: AudioManager? = null
    private var previousAudioMode: Int? = null

    private var factory: PeerConnectionFactory? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: org.webrtc.AudioSource? = null
    private var audioTrack: org.webrtc.AudioTrack? = null
    private var dataChannel: DataChannel? = null

    override suspend fun connect(config: VoiceCallConfig) {
        val context = AndroidVoiceHost.applicationContext
            ?: throw IllegalStateException("Voice host is not attached")
        if (!MicrophonePermissionBridge.ensure()) {
            throw IllegalStateException("Microphone permission is required to call the counterpart")
        }
        maxUserTurns = config.maxUserTurns.coerceAtLeast(1)
        ensureFactory(context)
        configureAudio(context)
        emitStatus("Connecting to the counterpart…")

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            ),
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val connected = CompletableDeferred<Unit>()
        val pc = factory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED ||
                    state == PeerConnection.IceConnectionState.COMPLETED
                ) {
                    connected.complete(Unit)
                    emitStatus("Live — they can hear you")
                }
                if (state == PeerConnection.IceConnectionState.FAILED) {
                    fail("The counterpart call dropped")
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidate(candidate: IceCandidate?) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onAddStream(stream: MediaStream?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(channel: DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) = Unit
        }) ?: throw IllegalStateException("Could not create a WebRTC peer connection")
        peerConnection = pc

        audioSource = factory?.createAudioSource(MediaConstraints())
        audioTrack = factory?.createAudioTrack("hardtalk-mic", audioSource)
        audioTrack?.setEnabled(true)
        pc.addTrack(audioTrack, listOf("hardtalk-audio"))

        dataChannel = pc.createDataChannel("oai-events", DataChannel.Init()).also { channel ->
            channel.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(amount: Long) = Unit
                override fun onStateChange() {
                    if (channel.state() == DataChannel.State.OPEN) {
                        sendOpening(config.opening)
                    }
                }
                override fun onMessage(buffer: DataChannel.Buffer) {
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    handleRealtimeEvent(String(bytes, StandardCharsets.UTF_8))
                }
            })
        }

        val offer = createOffer(pc, constraints)
        setLocalDescription(pc, offer)
        waitForIceGathering(pc)
        val localSdp = pc.localDescription?.description
            ?: throw IllegalStateException("Missing local SDP")
        val answerSdp = postSdpOffer(config.realtimeUrl, config.clientSecret, localSdp)
        setRemoteDescription(
            pc,
            SessionDescription(SessionDescription.Type.ANSWER, answerSdp),
        )
        durationJob = scope.launch {
            delay(config.maxDurationSeconds.coerceIn(15, 180) * 1000L)
            hangUpInternal("time")
        }
        withTimeout(20_000) { connected.await() }
    }

    override fun setMuted(muted: Boolean) {
        audioTrack?.setEnabled(!muted)
    }

    override fun hangUp() {
        hangUpInternal("hangup")
    }

    private fun hangUpInternal(reason: String) {
        if (!finished.compareAndSet(false, true)) return
        durationJob?.cancel()
        release()
        scope.launch {
            _events.emit(VoiceCallEvent.Ended(turns.toList(), reason))
        }
    }

    private fun fail(message: String) {
        if (!finished.compareAndSet(false, true)) return
        durationJob?.cancel()
        release()
        scope.launch {
            _events.emit(VoiceCallEvent.Failed(message))
        }
    }

    private fun sendOpening(opening: String) {
        val payload =
            """{"type":"response.create","response":{"instructions":"The 1:1 has started. Speak this opening line verbatim, then wait and listen. Opening: ${opening.jsonEscape()}"}}"""
        sendEvent(payload)
    }

    private fun sendEvent(payload: String) {
        val channel = dataChannel ?: return
        val buffer = ByteBuffer.wrap(payload.toByteArray(StandardCharsets.UTF_8))
        channel.send(DataChannel.Buffer(buffer, false))
    }

    private fun handleRealtimeEvent(raw: String) {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return
        val type = root.string("type") ?: return
        when (type) {
            "conversation.item.input_audio_transcription.completed",
            "conversation.item.input_audio_transcription.done",
            -> {
                val text = root.string("transcript") ?: return
                onUserTranscript(text)
            }
            "response.output_audio_transcript.done",
            "response.audio_transcript.done",
            -> {
                val text = root.string("transcript") ?: return
                onCounterpartTranscript(text)
            }
            "conversation.item.added",
            "conversation.item.created",
            -> {
                val item = root["item"] as? JsonObject ?: return
                val role = item.string("role")
                val transcript = item.string("transcript")
                    ?: item["content"]?.toString()
                if (role == "user" && !transcript.isNullOrBlank()) {
                    onUserTranscript(transcript.trim('"'))
                }
            }
            "error" -> {
                val message = (root["error"] as? JsonObject)?.string("message")
                    ?: root.string("message")
                    ?: "Realtime error"
                fail(message)
            }
        }
    }

    private fun onUserTranscript(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        synchronized(turns) {
            if (userTurns >= maxUserTurns) return
            userTurns += 1
            turns += ChatMessage(role = ChatRole.USER, content = cleaned)
            val count = userTurns
            scope.launch {
                _events.emit(VoiceCallEvent.Transcript(ChatRole.USER, cleaned, count))
            }
            if (count >= maxUserTurns) {
                hangUpInternal("max_turns")
            }
        }
    }

    private fun onCounterpartTranscript(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        synchronized(turns) {
            turns += ChatMessage(role = ChatRole.COUNTERPART, content = cleaned)
        }
        scope.launch {
            _events.emit(VoiceCallEvent.Transcript(ChatRole.COUNTERPART, cleaned, userTurns))
        }
    }

    private fun emitStatus(text: String) {
        scope.launch { _events.emit(VoiceCallEvent.Status(text)) }
    }

    private fun configureAudio(context: Context) {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = manager
        previousAudioMode = manager.mode
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
        manager.isSpeakerphoneOn = true
    }

    private fun restoreAudio() {
        val manager = audioManager ?: return
        previousAudioMode?.let { manager.mode = it }
        manager.isSpeakerphoneOn = false
    }

    private fun release() {
        try {
            dataChannel?.close()
        } catch (_: Exception) {
        }
        dataChannel = null
        try {
            audioTrack?.setEnabled(false)
            audioTrack?.dispose()
        } catch (_: Exception) {
        }
        audioTrack = null
        try {
            audioSource?.dispose()
        } catch (_: Exception) {
        }
        audioSource = null
        try {
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (_: Exception) {
        }
        peerConnection = null
        try {
            factory?.dispose()
        } catch (_: Exception) {
        }
        factory = null
        try {
            audioDeviceModule?.release()
        } catch (_: Exception) {
        }
        audioDeviceModule = null
        restoreAudio()
    }

    private suspend fun createOffer(
        pc: PeerConnection,
        constraints: MediaConstraints,
    ): SessionDescription = suspendCancellableCoroutine { cont ->
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp != null) cont.resume(sdp) else {
                    cont.resumeWithException(IllegalStateException("Empty offer"))
                }
            }
            override fun onCreateFailure(error: String?) {
                cont.resumeWithException(IllegalStateException(error ?: "createOffer failed"))
            }
            override fun onSetSuccess() = Unit
            override fun onSetFailure(error: String?) = Unit
        }, constraints)
    }

    private suspend fun setLocalDescription(pc: PeerConnection, sdp: SessionDescription) {
        suspendCancellableCoroutine { cont ->
            pc.setLocalDescription(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) = Unit
                override fun onCreateFailure(error: String?) = Unit
                override fun onSetSuccess() { cont.resume(Unit) }
                override fun onSetFailure(error: String?) {
                    cont.resumeWithException(IllegalStateException(error ?: "setLocalDescription failed"))
                }
            }, sdp)
        }
    }

    private suspend fun setRemoteDescription(pc: PeerConnection, sdp: SessionDescription) {
        suspendCancellableCoroutine { cont ->
            pc.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) = Unit
                override fun onCreateFailure(error: String?) = Unit
                override fun onSetSuccess() { cont.resume(Unit) }
                override fun onSetFailure(error: String?) {
                    cont.resumeWithException(IllegalStateException(error ?: "setRemoteDescription failed"))
                }
            }, sdp)
        }
    }

    private suspend fun waitForIceGathering(pc: PeerConnection) {
        if (pc.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) return
        val timeout = scope.launch {
            delay(2_500)
        }
        while (pc.iceGatheringState() != PeerConnection.IceGatheringState.COMPLETE && timeout.isActive) {
            delay(50)
        }
        timeout.cancel()
    }

    private fun postSdpOffer(realtimeUrl: String, clientSecret: String, offer: String): String {
        val connection = URL(realtimeUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Authorization", "Bearer $clientSecret")
        connection.setRequestProperty("Content-Type", "application/sdp")
        connection.outputStream.use { it.write(offer.toByteArray(StandardCharsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val body = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Realtime SDP exchange failed (${connection.responseCode}): ${body.take(240)}")
        }
        if (body.trimStart().startsWith("{")) {
            throw IllegalStateException("Realtime SDP exchange returned JSON: ${body.take(240)}")
        }
        return body
    }

    private fun ensureFactory(context: Context) {
        initializeOnce(context)
        val adm = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
        audioDeviceModule = adm
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)
            .createPeerConnectionFactory()
    }

    companion object {
        @Volatile
        private var nativeReady = false

        private fun initializeOnce(context: Context) {
            if (nativeReady) return
            synchronized(this) {
                if (nativeReady) return
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                        .createInitializationOptions(),
                )
                nativeReady = true
            }
        }
    }
}

private fun JsonObject.string(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

private fun String.jsonEscape(): String = buildString {
    for (ch in this@jsonEscape) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(ch)
        }
    }
}
