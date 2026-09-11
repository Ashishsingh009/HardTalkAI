package ai.hardtalk.source.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

expect fun provideHttpClientEngine(): HttpClientEngine

fun createHardTalkHttpClient(engine: HttpClientEngine = provideHttpClientEngine()): HttpClient =
    HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(apiJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }
