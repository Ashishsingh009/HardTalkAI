package ai.hardtalk.source.domain.repository

import ai.hardtalk.source.data.billing.JvmBillingRepository

actual fun createBillingRepository(): BillingRepository = JvmBillingRepository()
