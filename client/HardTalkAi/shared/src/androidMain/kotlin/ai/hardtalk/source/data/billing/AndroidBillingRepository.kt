package ai.hardtalk.source.data.billing

import android.app.Activity
import android.content.Context
import ai.hardtalk.source.domain.billing.BillingState
import ai.hardtalk.source.domain.billing.HardTalkBilling
import ai.hardtalk.source.domain.repository.BillingRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package as RcPackage
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidBillingRepository(
    private val ungated: Boolean,
    private val activityProvider: () -> Activity?,
) : BillingRepository {

    private val _state = MutableStateFlow(
        BillingState(
            ungated = ungated,
            configured = !ungated && Purchases.isConfigured,
        ),
    )
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    override suspend fun refresh() {
        if (ungated) {
            _state.update { it.copy(ungated = true, configured = false, error = null) }
            return
        }
        if (!Purchases.isConfigured) {
            _state.update {
                it.copy(
                    configured = false,
                    hasPro = false,
                    error = null,
                )
            }
            return
        }
        val info = fetchCustomerInfo()
        val offerings = fetchOfferings()
        val price = offerings?.let { formatPrice(it) }
        _state.update {
            it.copy(
                ungated = false,
                configured = true,
                hasPro = info?.entitlements?.get(HardTalkBilling.ENTITLEMENT_PRO)?.isActive == true,
                priceLabel = price ?: it.priceLabel,
                error = null,
            )
        }
    }

    override suspend fun purchase(): Result<Unit> {
        if (ungated) {
            _state.update { it.copy(hasPro = true) }
            return Result.success(Unit)
        }
        if (!Purchases.isConfigured) {
            return Result.failure(
                IllegalStateException(
                    "Play Billing is not configured. Add the RevenueCat Google SDK key to local.properties.",
                ),
            )
        }
        val activity = activityProvider()
            ?: return Result.failure(IllegalStateException("Open the paywall from the Android app."))
        val offerings = fetchOfferings()
            ?: return Result.failure(IllegalStateException("No HardTalk Pro offering is available yet."))
        val pkg = offerings.current?.availablePackages?.firstOrNull()
            ?: return Result.failure(IllegalStateException("No HardTalk Pro package is available yet."))
        return purchasePackage(activity, pkg).onSuccess { refresh() }
    }

    override suspend fun restore(): Result<Unit> {
        if (ungated) {
            _state.update { it.copy(hasPro = true) }
            return Result.success(Unit)
        }
        if (!Purchases.isConfigured) {
            return Result.failure(
                IllegalStateException(
                    "Play Billing is not configured. Add the RevenueCat Google SDK key to local.properties.",
                ),
            )
        }
        return restorePurchases().onSuccess { refresh() }
    }

    companion object {
        fun configure(context: Context, apiKey: String) {
            if (apiKey.isBlank() || Purchases.isConfigured) return
            Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        }
    }
}

private suspend fun fetchCustomerInfo(): CustomerInfo? =
    suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { cont.resume(null) },
            onSuccess = { cont.resume(it) },
        )
    }

private suspend fun fetchOfferings(): Offerings? =
    suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.getOfferingsWith(
            onError = { cont.resume(null) },
            onSuccess = { cont.resume(it) },
        )
    }

private suspend fun purchasePackage(activity: Activity, pkg: RcPackage): Result<Unit> =
    suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, pkg).build(),
            onError = { error, userCancelled ->
                if (!cont.isActive) return@purchaseWith
                if (userCancelled) {
                    cont.resume(Result.failure(IllegalStateException("Purchase cancelled")))
                } else {
                    cont.resume(Result.failure(IllegalStateException(error.message)))
                }
            },
            onSuccess = { _, _ ->
                if (cont.isActive) cont.resume(Result.success(Unit))
            },
        )
    }

private suspend fun restorePurchases(): Result<Unit> =
    suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.restorePurchases(
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    if (cont.isActive) cont.resume(Result.success(Unit))
                }

                override fun onError(error: PurchasesError) {
                    if (cont.isActive) {
                        cont.resume(Result.failure(IllegalStateException(error.message)))
                    }
                }
            },
        )
    }

private fun formatPrice(offerings: Offerings): String? {
    val pkg = offerings.current?.availablePackages?.firstOrNull() ?: return null
    return pkg.product.price.formatted
}

actual fun createBillingRepository(): BillingRepository =
    AndroidBillingRepository(ungated = true, activityProvider = { null })
