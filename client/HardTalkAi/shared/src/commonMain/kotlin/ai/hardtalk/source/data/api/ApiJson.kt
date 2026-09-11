package ai.hardtalk.source.data.api

import kotlinx.serialization.json.Json

val apiJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    prettyPrint = false
}
