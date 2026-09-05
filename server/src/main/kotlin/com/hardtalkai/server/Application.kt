package com.hardtalkai.server

import com.hardtalkai.shared.ChatRequest
import com.hardtalkai.shared.ScenariosResponse
import com.hardtalkai.shared.findScenario
import com.hardtalkai.shared.generateReply
import com.hardtalkai.shared.scenarios
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class HealthResponse(val status: String, val engine: String, val scenarios: Int)

@Serializable
data class ErrorResponse(val error: String)

private val logger = LoggerFactory.getLogger("HardTalkAI")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 3001
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal error"))
        }
    }

    routing {
        get("/api/health") {
            call.respond(HealthResponse(status = "ok", engine = "builtin", scenarios = scenarios.size))
        }

        get("/api/scenarios") {
            call.respond(ScenariosResponse(scenarios))
        }

        post("/api/chat") {
            val body = call.receive<ChatRequest>()

            val scenarioId = body.scenarioId
            if (scenarioId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("scenarioId is required"))
                return@post
            }
            val message = body.message
            if (message.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("message is required"))
                return@post
            }
            val scenario = findScenario(scenarioId)
            if (scenario == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Unknown scenario: $scenarioId"))
                return@post
            }

            val result = generateReply(scenario, body.history, message)
            call.respond(result)
        }
    }

    logger.info("HardTalkAI Ktor server routes registered")
}
