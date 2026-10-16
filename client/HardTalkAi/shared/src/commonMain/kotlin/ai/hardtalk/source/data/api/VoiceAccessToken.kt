package ai.hardtalk.source.data.api

const val HARDTALK_VOICE_TOKEN_HEADER = "X-HardTalk-Voice-Token"
const val VOICE_TOKEN_TTL_SECONDS = 120L
const val VOICE_TOKEN_SKEW_SECONDS = 30L
const val MIN_VOICE_SECRET_LENGTH = 16
const val VOICE_TOKEN_VERSION = "v1"

fun voiceSigningPayload(installId: String, expiresAtEpochSeconds: Long): String =
    "$VOICE_TOKEN_VERSION\n$installId\n$expiresAtEpochSeconds"

fun issueVoiceAccessToken(
    secret: String,
    installId: String,
    nowEpochSeconds: Long,
    ttlSeconds: Long = VOICE_TOKEN_TTL_SECONDS,
): String {
    require(secret.length >= MIN_VOICE_SECRET_LENGTH) { "Voice secret is too short" }
    require(installId.matches(Regex("^[0-9a-f]{32}$"))) { "installId must be 32 hex chars" }
    val expiresAt = nowEpochSeconds + ttlSeconds
    val signature = hmacSha256Hex(secret, voiceSigningPayload(installId, expiresAt))
    return "$VOICE_TOKEN_VERSION.$installId.$expiresAt.$signature"
}

fun parseVoiceAccessToken(raw: String): VoiceAccessToken? {
    val parts = raw.trim().split('.')
    if (parts.size != 4) return null
    val (version, installId, expiresRaw, signature) = parts
    if (version != VOICE_TOKEN_VERSION) return null
    if (!installId.matches(Regex("^[0-9a-f]{32}$"))) return null
    val expiresAt = expiresRaw.toLongOrNull() ?: return null
    if (!signature.matches(Regex("^[0-9a-f]{64}$"))) return null
    return VoiceAccessToken(installId = installId, expiresAtEpochSeconds = expiresAt, signatureHex = signature)
}

fun verifyVoiceAccessToken(
    secret: String,
    raw: String,
    nowEpochSeconds: Long,
    maxSkewSeconds: Long = VOICE_TOKEN_SKEW_SECONDS,
): VoiceAccessToken? {
    if (secret.length < MIN_VOICE_SECRET_LENGTH) return null
    val parsed = parseVoiceAccessToken(raw) ?: return null
    if (nowEpochSeconds > parsed.expiresAtEpochSeconds + maxSkewSeconds) return null
    if (parsed.expiresAtEpochSeconds - nowEpochSeconds > VOICE_TOKEN_TTL_SECONDS + maxSkewSeconds) return null
    val expected = hmacSha256Hex(secret, voiceSigningPayload(parsed.installId, parsed.expiresAtEpochSeconds))
    if (!constantTimeEquals(expected, parsed.signatureHex)) return null
    return parsed
}

data class VoiceAccessToken(
    val installId: String,
    val expiresAtEpochSeconds: Long,
    val signatureHex: String,
)

fun randomInstallId(): String {
    val alphabet = "0123456789abcdef"
    return buildString(32) {
        repeat(32) { append(alphabet.random()) }
    }
}

private fun constantTimeEquals(left: String, right: String): Boolean {
    if (left.length != right.length) return false
    var diff = 0
    for (index in left.indices) {
        diff = diff or (left[index].code xor right[index].code)
    }
    return diff == 0
}
