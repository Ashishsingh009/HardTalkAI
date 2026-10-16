package ai.hardtalk.source.data.api

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual fun hmacSha256Hex(secret: String, message: String): String {
    val key = secret.encodeToByteArray()
    val data = message.encodeToByteArray()
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    key.usePinned { keyPinned ->
        data.usePinned { dataPinned ->
            digest.usePinned { digestPinned ->
                CCHmac(
                    kCCHmacAlgSHA256,
                    keyPinned.addressOf(0),
                    key.size.toULong(),
                    dataPinned.addressOf(0),
                    data.size.toULong(),
                    digestPinned.addressOf(0),
                )
            }
        }
    }
    return digest.joinToString("") { byte ->
        val value = byte.toInt() and 0xff
        value.toString(16).padStart(2, '0')
    }
}

actual fun currentEpochSeconds(): Long = NSDate().timeIntervalSince1970.toLong()

actual fun persistentInstallId(): String = randomInstallId()
