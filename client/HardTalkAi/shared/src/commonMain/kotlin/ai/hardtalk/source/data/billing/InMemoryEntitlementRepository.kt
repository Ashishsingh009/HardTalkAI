package ai.hardtalk.source.data.billing

import ai.hardtalk.source.domain.billing.BillingActionResult
import ai.hardtalk.source.domain.billing.EntitlementRepository
import ai.hardtalk.source.domain.billing.EntitlementState
import ai.hardtalk.source.domain.billing.ProEntitlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Default / test / iOS stand-in. Does not talk to a store.
 * Unpaid users keep the free raise drill; [purchaseMonthly] can simulate a sandbox buy in tests.
 */
class InMemoryEntitlementRepository(
    initiallyPro: Boolean = false,
    configured: Boolean = false,
    monthlyPriceLabel: String? = null,
    statusMessage: String? = null,
    private val purchaseResult: BillingActionResult = BillingActionResult.Error(
        "HardTalk Pro is billed on Android in this build.",
    ),
) : EntitlementRepository {

    private val _state = MutableStateFlow(
        EntitlementState(
            isPro = initiallyPro,
            configured = configured,
            monthlyPriceLabel = monthlyPriceLabel,
            productId = if (monthlyPriceLabel != null) ProEntitlement.MONTHLY_PRODUCT_ID else null,
            message = statusMessage,
        ),
    )
    override val state: StateFlow<EntitlementState> = _state.asStateFlow()

    override suspend fun refresh() {
        // No network. Leave cached isPro / price as-is so the free drill still works offline.
    }

    override suspend fun purchaseMonthly(): BillingActionResult {
        _state.update { it.copy(busy = true, message = null) }
        val result = purchaseResult
        _state.update { current ->
            when (result) {
                BillingActionResult.Success -> current.copy(
                    busy = false,
                    isPro = true,
                    message = null,
                )
                BillingActionResult.Cancelled -> current.copy(
                    busy = false,
                    message = "Purchase cancelled.",
                )
                is BillingActionResult.Error -> current.copy(
                    busy = false,
                    message = result.message,
                )
            }
        }
        return result
    }

    override suspend fun restorePurchases(): BillingActionResult {
        _state.update { it.copy(busy = true, message = null) }
        val result = purchaseResult
        _state.update { current ->
            when (result) {
                BillingActionResult.Success -> current.copy(
                    busy = false,
                    isPro = true,
                    message = null,
                )
                BillingActionResult.Cancelled -> current.copy(busy = false, message = null)
                is BillingActionResult.Error -> current.copy(
                    busy = false,
                    message = result.message,
                )
            }
        }
        return result
    }

    fun setPro(isPro: Boolean) {
        _state.update { it.copy(isPro = isPro, message = null) }
    }
}
