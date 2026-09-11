package ai.hardtalk.source.domain.billing

data class EntitlementState(
    val isPro: Boolean = false,
    /** True after Purchases.configure succeeded (Android). False if the API key is missing. */
    val configured: Boolean = false,
    /** Localized monthly price from the current offering, e.g. "$4.99 / month". */
    val monthlyPriceLabel: String? = null,
    val productId: String? = null,
    val busy: Boolean = false,
    /** User-visible status or error. Null when idle. */
    val message: String? = null,
)

sealed interface BillingActionResult {
    data object Success : BillingActionResult
    data object Cancelled : BillingActionResult
    data class Error(val message: String) : BillingActionResult
}
