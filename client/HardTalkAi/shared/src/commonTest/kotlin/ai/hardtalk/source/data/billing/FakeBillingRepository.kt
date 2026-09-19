package ai.hardtalk.source.data.billing

import ai.hardtalk.source.domain.billing.BillingState
import ai.hardtalk.source.domain.repository.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeBillingRepository(
    ungated: Boolean = false,
    hasPro: Boolean = false,
    priceLabel: String? = "$4.99",
    private var purchaseResult: Result<Unit> = Result.success(Unit),
    private var restoreResult: Result<Unit> = Result.success(Unit),
) : BillingRepository {

    private val _state = MutableStateFlow(
        BillingState(
            ungated = ungated,
            hasPro = hasPro,
            configured = !ungated,
            priceLabel = priceLabel,
        ),
    )
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    var purchaseCalls: Int = 0
        private set

    fun setHasPro(value: Boolean) {
        _state.update { it.copy(hasPro = value) }
    }

    fun setPurchaseResult(result: Result<Unit>) {
        purchaseResult = result
    }

    override suspend fun refresh() = Unit

    override suspend fun purchase(): Result<Unit> {
        purchaseCalls += 1
        val result = purchaseResult
        if (result.isSuccess) {
            _state.update { it.copy(hasPro = true, error = null) }
        } else {
            _state.update { it.copy(error = result.exceptionOrNull()?.message) }
        }
        return result
    }

    override suspend fun restore(): Result<Unit> {
        val result = restoreResult
        if (result.isSuccess) {
            _state.update { it.copy(hasPro = true, error = null) }
        }
        return result
    }
}
