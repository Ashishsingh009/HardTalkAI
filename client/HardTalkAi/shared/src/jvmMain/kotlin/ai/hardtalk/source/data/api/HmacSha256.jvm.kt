package ai.hardtalk.source.data.api

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun hmacSha256Hex(secret: String, message: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
    return mac.doFinal(message.toByteArray(Charsets.UTF_8)).joinToString("") { byte ->
        "%02x".format(byte)
    }
}

actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L

actual fun persistentInstallId(): String = randomInstallId()
