package ai.hardtalk.source.legal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegalLinksTest {

    @Test
    fun privacyContactsMatchPlayStub() {
        assertEquals("hello@hardtalk.ai", LegalLinks.CONTACT_EMAIL)
        assertEquals("aashish2k2@gmail.com", LegalLinks.CONTACT_FALLBACK_EMAIL)
        assertTrue(LegalLinks.PRIVACY_POLICY_URL.startsWith("https://"))
        assertTrue(LegalLinks.PRIVACY_POLICY_URL.contains("privacy-policy"))
        assertTrue(LegalLinks.SUMMARY.contains("OpenAI"))
        assertTrue(LegalLinks.SUMMARY.contains("do not sell", ignoreCase = true))
        assertTrue(LegalLinks.SUMMARY.contains(LegalLinks.CONTACT_EMAIL))
        assertTrue(LegalLinks.SUMMARY.contains("prototype", ignoreCase = true))
        assertTrue(LegalLinks.SUMMARY.contains("RevenueCat"))
    }
}
