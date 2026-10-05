package ai.hardtalk.source.data.api

import ai.hardtalk.source.voice.AndroidVoiceHost
import android.content.Context
import java.util.UUID
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

actual fun persistentInstallId(): String {
    val context = AndroidVoiceHost.applicationContext ?: return randomInstallId()
    val prefs = context.getSharedPreferences("hardtalk", Context.MODE_PRIVATE)
    val existing = prefs.getString("install_id", null)?.trim().orEmpty()
    if (existing.matches(Regex("^[0-9a-f]{32}$"))) return existing
    val created = UUID.randomUUID().toString().replace("-", "")
    prefs.edit().putString("install_id", created).apply()
    return created
}
