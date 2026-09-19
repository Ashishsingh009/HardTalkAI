package ai.hardtalk.source.voice

actual fun isVoiceCallSupported(): Boolean = true

actual fun createVoiceCallSession(): VoiceCallSession? = OpenAiRealtimeWebRtcSession()
