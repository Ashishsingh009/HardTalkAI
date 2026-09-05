package com.hardtalkai.shared

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EngineTest {
    @Test
    fun rewardsBalancedEmpatheticAssertiveMessages() {
        val good = scoreMessage(
            "I really appreciate how slammed you've been. I'd like us to agree on a plan " +
                "so the deadlines stop slipping. Can we set a weekly check-in?",
        )
        assertTrue(good.empathy > 55, "empathy was ${good.empathy}")
        assertTrue(good.assertiveness > 45, "assertiveness was ${good.assertiveness}")
        assertTrue(good.overall > 60, "overall was ${good.overall}")
    }

    @Test
    fun penalizesAggressiveBlamingLanguage() {
        val bad = scoreMessage("You always miss deadlines and it's your fault the team looks stupid.")
        assertTrue(bad.empathy < 30, "empathy was ${bad.empathy}")
        assertTrue(bad.tips.any { it.contains("blaming", ignoreCase = true) }, "tips: ${bad.tips}")
    }

    @Test
    fun flagsExcessiveHedging() {
        val hedgy = scoreMessage("Sorry to bother, maybe I just kind of wanted to possibly ask something?")
        assertTrue(hedgy.assertiveness < 45, "assertiveness was ${hedgy.assertiveness}")
        assertTrue(hedgy.tips.any { it.contains("hedg", ignoreCase = true) }, "tips: ${hedgy.tips}")
    }

    @Test
    fun warmsUpCounterpartForStrongMessage() {
        val scenario = findScenario("ask-for-raise")
        assertNotNull(scenario)
        val res = generateReply(
            scenario,
            emptyList(),
            "I appreciate you making time. I'd like to talk about a raise: I shipped 3 launches " +
                "and cut latency by 40%. What's possible?",
        )
        assertTrue(res.reply.isNotEmpty())
        assertContains(res.mood, "receptive")
        assertTrue(res.feedback.overall > 60, "overall was ${res.feedback.overall}")
    }

    @Test
    fun makesCounterpartGuardedForWeakMessage() {
        val scenario = findScenario("give-feedback")
        assertNotNull(scenario)
        val res = generateReply(scenario, emptyList(), "you never do your work")
        assertContains(res.mood, "defensive")
    }
}
