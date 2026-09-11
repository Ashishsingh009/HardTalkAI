package ai.hardtalk.source

import ai.hardtalk.source.billing.RevenueCatEntitlementRepository
import ai.hardtalk.source.domain.billing.EntitlementRepository
import android.app.Application

class HardTalkApplication : Application() {
    lateinit var entitlements: EntitlementRepository
        private set

    override fun onCreate() {
        super.onCreate()
        entitlements = RevenueCatEntitlementRepository.create(
            application = this,
            apiKey = BuildConfig.REVENUECAT_API_KEY,
            debugLogs = BuildConfig.DEBUG,
        )
    }
}
