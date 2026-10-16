package ai.hardtalk.source.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealtimeUrlTest {

    @Test
    fun `official openai realtime url is allowlisted`() {
        val url = "https://api.openai.com/v1/realtime/calls"
        assertTrue(isAllowlistedRealtimeUrl(url))
        assertEquals(url, requireAllowlistedRealtimeUrl(url))
    }

    @Test
    fun `https openai host with default port is allowlisted`() {
        assertTrue(isAllowlistedRealtimeUrl("https://api.openai.com:443/v1/realtime/calls"))
    }

    @Test
    fun `plaintext and foreign hosts are rejected`() {
        val rejected = listOf(
            "http://api.openai.com/v1/realtime/calls",
            "https://evil.example/v1/realtime/calls",
            "https://api.openai.com.evil.example/v1/realtime/calls",
            "https://attacker.test/?next=https://api.openai.com/v1/realtime/calls",
            "https://user@api.openai.com/v1/realtime/calls",
            "https://api.openai.com:8443/v1/realtime/calls",
            "https://api.openai.com/v1/other",
            "http://10.0.2.2:3001/v1/realtime/calls",
        )
        for (url in rejected) {
            assertFalse(isAllowlistedRealtimeUrl(url), url)
            assertFailsWith<IllegalArgumentException> { requireAllowlistedRealtimeUrl(url) }
        }
    }
}
