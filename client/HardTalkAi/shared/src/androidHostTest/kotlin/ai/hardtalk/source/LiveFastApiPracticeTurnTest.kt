package ai.hardtalk.source

import ai.hardtalk.source.data.api.HardTalkApi
import ai.hardtalk.source.data.api.createHardTalkHttpClient
import ai.hardtalk.source.data.api.provideHttpClientEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Hits a local FastAPI process when one is running (typically :3001).
 * Skips cleanly when the API is down so unit-test CI stays green.
 */
class LiveFastApiPracticeTurnTest {

    @Test
    fun completeOneScoredPracticeTurnAgainstLocalApi() = runBlocking {
        val baseUrl = System.getenv("HARDTALK_API_BASE_URL") ?: "http://127.0.0.1:3001"
        val api = HardTalkApi(baseUrl, createHardTalkHttpClient(provideHttpClientEngine()))
        val scenarios = runCatching { api.getScenarios() }.getOrElse { error ->
            println("Skipping live FastAPI turn; $baseUrl is unreachable (${error.message})")
            return@runBlocking
        }
        assertTrue(scenarios.isNotEmpty(), "API returned no scenarios")
        assertTrue(
            scenarios.size in 10..12,
            "expected 10–12 career scenarios, got ${scenarios.size}",
        )
        assertTrue(scenarios.any { it.id == "ask-for-raise" && it.free })
        assertTrue(scenarios.any { it.id == "chase-overdue-invoice" && it.free })
        assertTrue(scenarios.any { it.id == "give-feedback" })
        assertTrue(scenarios.any { it.id == "decline-request" })

        val scenario = scenarios.first { it.id == "ask-for-raise" }
        val result = api.sendChat(
            scenarioId = scenario.id,
            message = "I appreciate your time. I'd like a raise: I shipped 3 launches and cut latency 40%. What's possible?",
            history = listOf(
                ai.hardtalk.source.data.api.dto.ChatTurnDto(
                    role = "counterpart",
                    content = scenario.opening,
                ),
            ),
        )
        assertTrue(result.reply.isNotBlank())
        assertTrue(result.feedback.clarity in 0..100)
        assertTrue(result.feedback.empathy in 0..100)
        assertTrue(result.feedback.assertiveness in 0..100)
        assertTrue(result.mood.isNotBlank())
        println("Live turn OK: mood=${result.mood} overall=${result.feedback.overall} reply=${result.reply.take(80)}")
    }

    @Test
    fun extractThenSendInvoiceAgainstLocalApi() = runBlocking {
        val baseUrl = System.getenv("HARDTALK_API_BASE_URL") ?: "http://127.0.0.1:3001"
        val api = HardTalkApi(baseUrl, createHardTalkHttpClient(provideHttpClientEngine()))
        val health = runCatching { api.getHealth() }.getOrElse { error ->
            println("Skipping live invoice send; $baseUrl is unreachable (${error.message})")
            return@runBlocking
        }
        val extracted = api.extractInvoiceTerms(
            "chase-overdue-invoice",
            listOf(
                ai.hardtalk.source.data.api.dto.ChatTurnDto(
                    role = "counterpart",
                    content = "Can we push the $2,400 to next quarter? Don't send PayPal yet.",
                ),
                ai.hardtalk.source.data.api.dto.ChatTurnDto(
                    role = "user",
                    content = "I want to keep the relationship. $2,400 USD is overdue. " +
                        "Can we lock 2026-11-01 if I invoice casey@northwind.example " +
                        "for the brand site redesign?",
                ),
            ),
        )
        assertTrue(extracted.terms.amount.orEmpty().contains("2400"))
        assertEquals("casey@northwind.example", extracted.terms.recipientEmail)

        val sent = api.sendPaypalInvoice(
            "chase-overdue-invoice",
            ai.hardtalk.source.data.api.dto.InvoiceSendRequestDto(
                scenarioId = "chase-overdue-invoice",
                amount = extracted.terms.amount,
                currency = extracted.terms.currency,
                dueDate = extracted.terms.dueDate,
                recipientEmail = extracted.terms.recipientEmail,
                recipientName = extracted.terms.recipientName,
                description = extracted.terms.description,
            ),
        )
        assertTrue(sent.invoiceId.isNotBlank())
        assertEquals("SENT", sent.status)
        assertTrue(sent.payerLink.contains("sandbox.paypal.com"))
        println(
            "Live invoice OK: healthPaypal=${health.paypal} mode=${sent.mode} " +
                "id=${sent.invoiceId} link=${sent.payerLink}",
        )
    }
}
