package ai.hardtalk.source.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoiceAccessTokenTest {

    private val secret = "test-voice-secret-16"
    private val installId = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private val now = 1_700_000_000L

    @Test
    fun `issued token verifies and matches python hmac format`() {
        val token = issueVoiceAccessToken(secret, installId, now)
        val parts = token.split('.')
        assertEquals(4, parts.size)
        assertEquals("v1", parts[0])
        assertEquals(installId, parts[1])
        assertEquals((now + VOICE_TOKEN_TTL_SECONDS).toString(), parts[2])
        assertEquals(64, parts[3].length)
        val verified = verifyVoiceAccessToken(secret, token, now)
        assertNotNull(verified)
        assertEquals(installId, verified.installId)
    }

    @Test
    fun `hmac matches the python hashlib vector`() {
        val payload = voiceSigningPayload(installId, now + VOICE_TOKEN_TTL_SECONDS)
        assertEquals(
            "4445f880dd53f46eff38108f96e275e3e779d66b4d75a7a5ffdea279c3c9364b",
            hmacSha256Hex(secret, payload),
        )
    }

    @Test
    fun `expired token is rejected`() {
        val token = issueVoiceAccessToken(secret, installId, now, ttlSeconds = 60)
        assertNull(verifyVoiceAccessToken(secret, token, now + 60 + VOICE_TOKEN_SKEW_SECONDS + 1))
    }

    @Test
    fun `wrong secret or tampered payload is rejected`() {
        val token = issueVoiceAccessToken(secret, installId, now)
        assertNull(verifyVoiceAccessToken("other-voice-secret-16", token, now))
        val parts = token.split('.').toMutableList()
        parts[1] = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        assertNull(verifyVoiceAccessToken(secret, parts.joinToString("."), now))
    }

    @Test
    fun `issued token binds install id as identity not a rate limit key`() {
        val otherInstall = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        val token = issueVoiceAccessToken(secret, installId, now)
        val other = issueVoiceAccessToken(secret, otherInstall, now)
        assertEquals(installId, token.split('.')[1])
        assertEquals(otherInstall, other.split('.')[1])
        assertTrue(token.split('.')[3] != other.split('.')[3])
        assertNotNull(verifyVoiceAccessToken(secret, token, now))
        assertNotNull(verifyVoiceAccessToken(secret, other, now))
    }

    @Test
    fun `short secret cannot issue or verify`() {
        assertTrue(
            runCatching { issueVoiceAccessToken("short-secret", installId, now) }.isFailure,
        )
        assertNull(verifyVoiceAccessToken("short-secret", "v1.$installId.$now.${"ab".repeat(32)}", now))
    }
}
