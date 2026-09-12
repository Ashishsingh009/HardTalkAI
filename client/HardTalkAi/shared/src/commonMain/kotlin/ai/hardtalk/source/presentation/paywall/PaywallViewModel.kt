package ai.hardtalk.source.presentation.paywall

import ai.hardtalk.source.domain.billing.EntitlementRepository
import ai.hardtalk.source.domain.billing.EntitlementState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val entitlements: EntitlementRepository,
) : ViewModel() {

    val uiState: StateFlow<EntitlementState> = entitlements.state

    init {
        viewModelScope.launch {
            runCatching { entitlements.refresh() }
        }
    }

    fun purchase() {
        viewModelScope.launch {
            entitlements.purchaseMonthly()
        }
    }

    fun restore() {
        viewModelScope.launch {
            entitlements.restorePurchases()
        }
    }
}
