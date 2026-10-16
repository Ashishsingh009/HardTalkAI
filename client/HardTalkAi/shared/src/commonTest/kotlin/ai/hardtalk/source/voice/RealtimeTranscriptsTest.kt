package ai.hardtalk.source.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RealtimeTranscriptsTest {

    @Test
    fun `transcription completed is a user turn`() {
        val event = """{"type":"conversation.item.input_audio_transcription.completed","transcript":"I'd like a raise"}"""
        val parsed = parseRealtimeTranscript(event)
        assertEquals(RealtimeTranscriptKind.USER, parsed?.kind)
        assertEquals("I'd like a raise", parsed?.text)
    }

    @Test
    fun `item added without a transcript is ignored`() {
        val event =
            """{"type":"conversation.item.added","item":{"role":"user","content":[{"type":"input_audio","audio":"AAAA"}]}}"""
        assertNull(parseRealtimeTranscript(event))
    }

    @Test
    fun `item content toString is never treated as speech`() {
        val noisy =
            """{"type":"conversation.item.created","item":{"role":"user","content":[{"type":"input_audio"}]}}"""
        assertNull(parseRealtimeTranscript(noisy))
        assertFalse(isDuplicateUserTranscript(null, "I'd like a raise"))
    }

    @Test
    fun `item added with a real transcript is accepted`() {
        val event =
            """{"type":"conversation.item.added","item":{"role":"user","transcript":"I'd like a raise","content":[{"type":"input_audio"}]}}"""
        assertEquals("I'd like a raise", parseRealtimeTranscript(event)?.text)
    }

    @Test
    fun `structured content transcript is accepted`() {
        val event =
            """{"type":"conversation.item.added","item":{"role":"user","content":[{"type":"input_audio","transcript":"Walk me through it"}]}}"""
        assertEquals("Walk me through it", parseRealtimeTranscript(event)?.text)
    }

    @Test
    fun `duplicate user transcripts are detected`() {
        assertTrue(isDuplicateUserTranscript("I'd like a raise", "I'd like a raise"))
        assertFalse(isDuplicateUserTranscript("I'd like a raise", "Twelve percent"))
        assertFalse(isDuplicateUserTranscript(null, "I'd like a raise"))
    }

    @Test
    fun `counterpart audio transcript is parsed`() {
        val event = """{"type":"response.output_audio_transcript.done","transcript":"What's going on?"}"""
        val parsed = parseRealtimeTranscript(event)
        assertEquals(RealtimeTranscriptKind.COUNTERPART, parsed?.kind)
        assertEquals("What's going on?", parsed?.text)
    }
}
