package ai.hardtalk.source.domain.billing

import ai.hardtalk.source.domain.model.Scenario

/**
 * RevenueCat dashboard identifiers the Android client expects.
 *
 * These cannot be created from app code — Ashish must enter them in
 * RevenueCat (and Play Console for a real store product). See
 * `docs/revenuecat-android.md`.
 */
object ProEntitlement {
    /** Entitlement identifier. Unlock every non-free scenario when this is active. */
    const val ID = "pro"

    /**
     * Subscription product id (Play Console + RevenueCat Test Store / Play product).
     * Monthly auto-renewing subscription.
     */
    const val MONTHLY_PRODUCT_ID = "hardtalkai_pro_monthly"

    /** RevenueCat package type attached to [MONTHLY_PRODUCT_ID] on the current offering. */
    const val MONTHLY_PACKAGE_ID = "\$rc_monthly"

    const val OFFERING_ID = "default"
}

fun Scenario.isPlayable(hasPro: Boolean): Boolean = free || hasPro
