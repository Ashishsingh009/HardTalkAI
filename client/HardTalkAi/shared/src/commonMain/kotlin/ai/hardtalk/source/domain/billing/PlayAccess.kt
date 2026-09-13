package ai.hardtalk.source.domain.billing

import ai.hardtalk.source.domain.model.Scenario

object HardTalkBilling {
    const val ENTITLEMENT_PRO = "hardtalk_pro"
}

fun canPlayScenario(
    scenario: Scenario,
    hasPro: Boolean,
    ungated: Boolean,
): Boolean {
    if (ungated) return true
    return scenario.free || hasPro
}
