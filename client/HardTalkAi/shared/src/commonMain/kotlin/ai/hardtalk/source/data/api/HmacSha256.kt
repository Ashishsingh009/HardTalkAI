package ai.hardtalk.source.data.api

/**
 * HMAC-SHA256 used to mint short-lived voice access tokens.
 * Platform actuals use the OS crypto provider.
 */
expect fun hmacSha256Hex(secret: String, message: String): String

expect fun currentEpochSeconds(): Long

expect fun persistentInstallId(): String
