package ai.hardtalk.source.billing

import android.app.Application
import ai.hardtalk.source.data.billing.InMemoryEntitlementRepository
import ai.hardtalk.source.domain.billing.BillingActionResult
import ai.hardtalk.source.domain.billing.EntitlementRepository
import ai.hardtalk.source.domain.billing.EntitlementState
import ai.hardtalk.source.domain.billing.ProEntitlement
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RevenueCatEntitlementRepository private constructor() : EntitlementRepository {

    private val _state = MutableStateFlow(
        EntitlementState(configured = true),
    )
    override val state: StateFlow<EntitlementState> = _state.asStateFlow()

    private var monthlyPackage: com.revenuecat.purchases.Package? = null

    override suspend fun refresh() {
        if (!Purchases.isConfigured) {
            _state.update {
                it.copy(
                    configured = false,
                    message = "RevenueCat isn’t configured. The free raise drill still works.",
                )
            }
            return
        }
        try {
            val info = Purchases.sharedInstance.awaitCustomerInfo()
            applyCustomerInfo(info)
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    configured = true,
                    message = offlineMessage(error, offeringFailed = false),
                )
            }
        }
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            applyOfferings(offerings)
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    configured = true,
                    message = offlineMessage(error, offeringFailed = true),
                )
            }
        }
    }

    override suspend fun purchaseMonthly(): BillingActionResult {
        val activity = PurchaseActivityHolder.current()
            ?: return BillingActionResult.Error("Open the app and try the purchase again.")
        val pkg = monthlyPackage
            ?: return BillingActionResult.Error(
                "Monthly plan isn’t available. Confirm product `${ProEntitlement.MONTHLY_PRODUCT_ID}` " +
                    "on offering `${ProEntitlement.OFFERING_ID}` in RevenueCat. The free raise drill still works.",
            )
        _state.update { it.copy(busy = true, message = null) }
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(
                PurchaseParams.Builder(activity, pkg).build(),
            )
            applyCustomerInfo(result.customerInfo)
            _state.update { it.copy(busy = false, message = null) }
            BillingActionResult.Success
        } catch (error: PurchasesException) {
            val cancelled = error.error.code == PurchasesErrorCode.PurchaseCancelledError
            val result = if (cancelled) {
                BillingActionResult.Cancelled
            } else {
                BillingActionResult.Error(error.message)
            }
            _state.update {
                it.copy(
                    busy = false,
                    message = if (cancelled) null else error.message,
                )
            }
            result
        } catch (error: Exception) {
            val message = error.message ?: "Purchase failed."
            _state.update { it.copy(busy = false, message = message) }
            BillingActionResult.Error(message)
        }
    }

    override suspend fun restorePurchases(): BillingActionResult {
        if (!Purchases.isConfigured) {
            return BillingActionResult.Error("RevenueCat isn’t configured.")
        }
        _state.update { it.copy(busy = true, message = null) }
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            applyCustomerInfo(info)
            val unlocked = _state.value.isPro
            _state.update {
                it.copy(
                    busy = false,
                    message = if (unlocked) null else "No Pro subscription found to restore.",
                )
            }
            if (unlocked) BillingActionResult.Success
            else BillingActionResult.Error("No Pro subscription found to restore.")
        } catch (error: Exception) {
            val message = error.message ?: "Restore failed."
            _state.update { it.copy(busy = false, message = message) }
            BillingActionResult.Error(message)
        }
    }

    internal fun applyCustomerInfo(info: CustomerInfo) {
        val entitled = info.entitlements[ProEntitlement.ID]?.isActive == true ||
            info.entitlements.active.containsKey(ProEntitlement.ID)
        _state.update {
            it.copy(
                isPro = entitled,
                configured = true,
                message = if (entitled) null else it.message,
            )
        }
    }

    private fun applyOfferings(offerings: Offerings) {
        val offering = offerings.current
            ?: offerings[ProEntitlement.OFFERING_ID]
            ?: offerings.all.values.firstOrNull()
        val pkg = offering?.monthly
            ?: offering?.availablePackages?.firstOrNull { it.packageType == PackageType.MONTHLY }
            ?: offering?.availablePackages?.firstOrNull {
                it.product.id == ProEntitlement.MONTHLY_PRODUCT_ID
            }
            ?: offering?.availablePackages?.firstOrNull()
        monthlyPackage = pkg
        val priceLabel = pkg?.let { formatMonthlyPrice(it) }
        _state.update {
            it.copy(
                monthlyPriceLabel = priceLabel,
                productId = pkg?.product?.id,
                configured = true,
                message = if (priceLabel == null) {
                    "No monthly package on the current offering. Add `${ProEntitlement.MONTHLY_PRODUCT_ID}` " +
                        "as `${ProEntitlement.MONTHLY_PACKAGE_ID}` in RevenueCat. The free raise drill still works."
                } else {
                    it.message
                },
            )
        }
    }

    private fun formatMonthlyPrice(pkg: com.revenuecat.purchases.Package): String {
        val formatted = pkg.product.price.formatted
        return if (formatted.contains("month", ignoreCase = true)) formatted else "$formatted / month"
    }

    private fun offlineMessage(error: Exception, offeringFailed: Boolean): String {
        val detail = error.message?.takeIf { it.isNotBlank() }
        return buildString {
            append("Couldn't reach RevenueCat")
            if (offeringFailed) append(" for offerings")
            append(". The free raise drill still works.")
            if (detail != null) {
                append(' ')
                append(detail)
            }
        }
    }

    companion object {
        fun create(
            application: Application,
            apiKey: String,
            debugLogs: Boolean,
        ): EntitlementRepository {
            val trimmed = apiKey.trim()
            if (trimmed.isEmpty()) {
                return InMemoryEntitlementRepository(
                    configured = false,
                    statusMessage = "Add revenuecat.androidApiKey to local.properties. " +
                        "The free raise drill still works without RevenueCat.",
                )
            }
            if (!debugLogs && trimmed.startsWith("test_")) {
                return InMemoryEntitlementRepository(
                    configured = false,
                    statusMessage = "Refusing to configure a Test Store (`test_`) key in release. " +
                        "Set revenuecat.playApiKey to a goog_ key. The free raise drill still works.",
                )
            }
            return try {
                Purchases.logLevel = if (debugLogs) LogLevel.DEBUG else LogLevel.INFO
                if (!Purchases.isConfigured) {
                    Purchases.configure(
                        PurchasesConfiguration.Builder(application, trimmed).build(),
                    )
                }
                RevenueCatEntitlementRepository().also { repo ->
                    Purchases.sharedInstance.updatedCustomerInfoListener =
                        UpdatedCustomerInfoListener { info -> repo.applyCustomerInfo(info) }
                }
            } catch (error: Exception) {
                InMemoryEntitlementRepository(
                    configured = false,
                    statusMessage = "RevenueCat failed to start (${error.message}). " +
                        "The free raise drill still works.",
                )
            }
        }
    }
}
