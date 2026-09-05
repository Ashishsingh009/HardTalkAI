package com.hardtalkai.server

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun healthReportsOk() = testApplication {
        application { module() }
        val res = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("\"status\":\"ok\""))
    }

    @Test
    fun scenariosReturnsAll() = testApplication {
        application { module() }
        val body = client.get("/api/scenarios").bodyAsText()
        assertTrue(body.contains("ask-for-raise"))
        assertTrue(body.contains("give-feedback"))
        assertTrue(body.contains("decline-request"))
    }

    @Test
    fun chatReturnsReplyAndFeedback() = testApplication {
        application { module() }
        val res = client.post("/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"scenarioId":"ask-for-raise","message":"I appreciate your time. I'd like a raise: I shipped 3 launches and cut latency 40%. What's possible?","history":[]}""",
            )
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("\"reply\""), body)
        assertTrue(body.contains("\"feedback\""), body)
        assertTrue(body.contains("\"mood\""), body)
    }

    @Test
    fun chatValidatesInput() = testApplication {
        application { module() }
        val res = client.post("/api/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"scenarioId":"ask-for-raise"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }
}
