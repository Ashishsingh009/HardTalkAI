package ai.hardtalk.source.domain.billing

import ai.hardtalk.source.domain.model.Persona
import ai.hardtalk.source.domain.model.Scenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScenarioAccessTest {

    @Test
    fun freeScenarioIsAlwaysPlayable() {
        val raise = scenario(id = "ask-for-raise", free = true)
        assertTrue(raise.isPlayable(hasPro = false))
        assertTrue(raise.isPlayable(hasPro = true))
    }

    @Test
    fun paidScenarioRequiresPro() {
        val feedback = scenario(id = "give-feedback", free = false)
        assertFalse(feedback.isPlayable(hasPro = false))
        assertTrue(feedback.isPlayable(hasPro = true))
    }

    @Test
    fun dashboardIdentifiersMatchShipatonContract() {
        assertEquals("pro", ProEntitlement.ID)
        assertEquals("hardtalkai_pro_monthly", ProEntitlement.MONTHLY_PRODUCT_ID)
        assertEquals("\$rc_monthly", ProEntitlement.MONTHLY_PACKAGE_ID)
        assertEquals("default", ProEntitlement.OFFERING_ID)
    }

    private fun scenario(id: String, free: Boolean) = Scenario(
        id = id,
        title = id,
        summary = "summary",
        difficulty = "moderate",
        persona = Persona("Dana", "Manager", "busy"),
        opening = "Hey",
        goals = listOf("Ask"),
        free = free,
    )
}
