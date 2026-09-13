package ai.hardtalk.source.domain.repository

import ai.hardtalk.source.data.billing.IosBillingRepository

actual fun createBillingRepository(): BillingRepository = IosBillingRepository()
