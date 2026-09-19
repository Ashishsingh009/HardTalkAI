package ai.hardtalk.source.domain.billing

data class BillingState(
    val ungated: Boolean = false,
    val hasPro: Boolean = false,
    val configured: Boolean = false,
    val priceLabel: String? = null,
    val error: String? = null,
)
