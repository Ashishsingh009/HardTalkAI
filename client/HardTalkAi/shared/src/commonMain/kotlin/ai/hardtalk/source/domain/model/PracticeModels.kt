package ai.hardtalk.source.domain.model

data class Persona(
    val name: String,
    val role: String,
    val mood: String,
)

data class Scenario(
    val id: String,
    val title: String,
    val summary: String,
    val difficulty: String,
    val persona: Persona,
    val opening: String,
    val goals: List<String>,
    val free: Boolean = false,
)

data class Feedback(
    val clarity: Int,
    val empathy: Int,
    val assertiveness: Int,
    val overall: Int,
    val tips: List<String>,
)

data class ReplyResult(
    val reply: String,
    val feedback: Feedback,
    val mood: String,
)

data class HealthStatus(
    val engine: String,
    val voice: Boolean,
    val scenarios: Int,
    val paypal: String = "mock",
)

data class InvoiceTerms(
    val amount: String = "",
    val currency: String = "USD",
    val dueDate: String = "",
    val recipientEmail: String = "",
    val recipientName: String = "",
    val description: String = "",
) {
    val missingRequired: List<String>
        get() = buildList {
            if (amount.isBlank()) add("amount")
            if (currency.isBlank()) add("currency")
            if (recipientEmail.isBlank()) add("client email")
            if (description.isBlank()) add("description")
        }

    val readyToSend: Boolean get() = missingRequired.isEmpty()
}

data class InvoiceExtraction(
    val terms: InvoiceTerms,
    val missing: List<String> = emptyList(),
    val source: String = "heuristic",
)

data class InvoiceSendResult(
    val invoiceId: String,
    val status: String,
    val payerLink: String,
    val mode: String,
) {
    val isMock: Boolean get() = mode.equals("mock", ignoreCase = true)
}

data class VoiceSession(
    val clientSecret: String,
    val realtimeUrl: String,
    val model: String,
    val voice: String,
    val opening: String,
    val personaName: String,
    val maxUserTurns: Int,
    val maxDurationSeconds: Int,
)

data class VoiceCompleteResult(
    val messages: List<ChatMessage>,
    val mood: String,
)

enum class ChatRole {
    USER,
    COUNTERPART,
}

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val feedback: Feedback? = null,
)

fun ChatRole.toApiRole(): String = when (this) {
    ChatRole.USER -> "user"
    ChatRole.COUNTERPART -> "counterpart"
}

fun String.toChatRole(): ChatRole = when (this) {
    "user" -> ChatRole.USER
    else -> ChatRole.COUNTERPART
}
