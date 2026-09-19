package ai.hardtalk.source.domain.repository

import ai.hardtalk.source.data.billing.AndroidBillingRepository

actual fun createBillingRepository(): BillingRepository =
    AndroidBillingRepository(ungated = true, activityProvider = { null })
