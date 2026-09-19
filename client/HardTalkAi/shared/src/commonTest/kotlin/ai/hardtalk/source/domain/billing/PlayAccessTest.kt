package ai.hardtalk.source.domain.billing

import ai.hardtalk.source.domain.model.Persona
import ai.hardtalk.source.domain.model.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayAccessTest {

    @Test
    fun raiseIsAlwaysPlayable() {
        assertTrue(canPlayScenario(raise(), hasPro = false, ungated = false))
        assertTrue(canPlayScenario(raise(), hasPro = true, ungated = false))
    }

    @Test
    fun paidDrillIsLockedWithoutEntitlement() {
        assertFalse(canPlayScenario(paid(), hasPro = false, ungated = false))
    }

    @Test
    fun paidDrillUnlocksWithPro() {
        assertTrue(canPlayScenario(paid(), hasPro = true, ungated = false))
    }

    @Test
    fun debugUngatedOpensPaidDrills() {
        assertTrue(canPlayScenario(paid(), hasPro = false, ungated = true))
    }

    @Test
    fun entitlementIdIsStable() {
        assertEquals("hardtalk_pro", HardTalkBilling.ENTITLEMENT_PRO)
    }
}

private fun raise() = Scenario(
    id = "ask-for-raise",
    title = "Ask your manager for a raise",
    summary = "Make the case for a pay increase.",
    difficulty = "moderate",
    persona = Persona("Dana", "Your engineering manager", "busy"),
    opening = "What did you want to talk about?",
    goals = listOf("State clearly that you want a raise"),
    free = true,
)

private fun paid() = raise().copy(
    id = "give-feedback",
    title = "Give a teammate critical feedback",
    free = false,
)
