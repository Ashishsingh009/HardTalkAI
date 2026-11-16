package ai.hardtalk.source.data.coaching

import ai.hardtalk.source.domain.model.RetryCoachingModel
import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.EmbeddingOptions
import com.google.ai.edge.litertlm.InputData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AndroidRetryCoachingModel(context: Context) : RetryCoachingModel {
    private val appContext = context.applicationContext
    private val modelDirectory = File(appContext.noBackupFilesDir, "retry-coaching")
    private val modelFile = File(modelDirectory, "embeddinggemma-2-text-270m-v1.litertlm")

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        hasModelFile()
    }

    override suspend fun download(onProgress: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            modelLock.withLock {
                if (hasModelFile()) {
                    onProgress(100)
                    return@withLock
                }

                // Probe LiteRT-LM's Android library before downloading a large model.
                try {
                    System.loadLibrary("litertlm_jni")
                } catch (error: LinkageError) {
                    throw IOException("On-device coaching is unavailable on this device.", error)
                }
                if (!modelDirectory.isDirectory && !modelDirectory.mkdirs()) {
                    throw IOException("Could not create the model directory.")
                }

                val partialFile = File(modelDirectory, "${modelFile.name}.part")
                try {
                    onProgress(0)
                    downloadVerifiedFile(partialFile, onProgress)
                    ensureActive()
                    if (!partialFile.renameTo(modelFile)) {
                        throw IOException("Could not install the downloaded model.")
                    }
                    onProgress(100)
                } finally {
                    partialFile.delete()
                }
            }
        }
    }

    override suspend fun embed(inputs: List<String>): List<FloatArray> =
        withContext(Dispatchers.Default) {
            if (inputs.isEmpty()) return@withContext emptyList()

            modelLock.withLock {
                check(hasModelFile()) { "Download the coaching model first." }
                val engine = EmbeddingEngine(
                    EmbeddingEngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = Backend.CPU(),
                        cacheDir = modelDirectory.absolutePath,
                    ),
                )
                try {
                    engine.initialize()
                    val options = EmbeddingOptions(normalize = true, outputSize = 256)
                    inputs.map { input ->
                        ensureActive()
                        engine.computeEmbedding(listOf(InputData.Text(input)), options).embedding
                    }
                } catch (error: LinkageError) {
                    throw IllegalStateException("On-device coaching is unavailable on this device.", error)
                } finally {
                    if (engine.isInitialized()) engine.close()
                }
            }
        }

    private fun hasModelFile(): Boolean = modelFile.isFile && modelFile.length() == MODEL_SIZE_BYTES

    private suspend fun downloadVerifiedFile(file: File, onProgress: (Int) -> Unit) {
        val coroutineContext = currentCoroutineContext()
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Model download failed (HTTP ${connection.responseCode}).")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            var reportedPercent = 0
            connection.inputStream.use { input ->
                file.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count == -1) break
                        downloaded += count
                        if (downloaded > MODEL_SIZE_BYTES) {
                            throw IOException("The downloaded model has an unexpected size.")
                        }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)

                        val percent = (downloaded * 100 / MODEL_SIZE_BYTES).toInt().coerceAtMost(99)
                        if (percent != reportedPercent) {
                            reportedPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }

            val checksum = digest.digest().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
            if (downloaded != MODEL_SIZE_BYTES || checksum != MODEL_SHA_256) {
                throw IOException("The downloaded model could not be verified.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val modelLock = Mutex()

        // Pin both the official LiteRT file revision and its Git LFS content hash.
        const val MODEL_URL = "https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/" +
            "resolve/8c9f652b5233a80313d5c848f3656e492cf0397f/embeddinggemma-2-text-270m.litertlm"
        const val MODEL_SIZE_BYTES = 164_626_432L
        const val MODEL_SHA_256 = "2d079ee2f6f066b1f368e8d7c819f55214eaef1d0513b312321901f30ab286fb"
    }
}
