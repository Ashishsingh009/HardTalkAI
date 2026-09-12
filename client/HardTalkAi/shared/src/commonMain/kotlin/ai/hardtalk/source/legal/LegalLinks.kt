package ai.hardtalk.source.legal

/**
 * Play / in-app privacy pointers. Full policy lives in docs/privacy-policy.md.
 * Swap [PRIVACY_POLICY_URL] to GitHub Pages (or FastAPI `/privacy`) once hosted.
 */
object LegalLinks {
    const val PRIVACY_POLICY_URL =
        "https://github.com/Ashishsingh009/HardTalkAI/blob/main/docs/privacy-policy.md"
    const val CONTACT_EMAIL = "hello@hardtalk.ai"
    const val CONTACT_FALLBACK_EMAIL = "aashish2k2@gmail.com"
    const val LAST_UPDATED = "10 September 2026"

    val SUMMARY =
        "HardTalkAI is a prototype career-coaching practice app (Coach Heather). " +
            "There are no accounts in this build. When you practice, your replies and " +
            "the round history go to the coaching API so we can score the turn and " +
            "generate the counterpart’s next line. If the operator configured OpenAI, " +
            "that conversation is sent to OpenAI for the counterpart reply; scores stay " +
            "on our server. We do not sell personal information, run ads, or take " +
            "payments (no RevenueCat yet). Chats are not written to a database in this " +
            "prototype. Contact $CONTACT_EMAIL, or $CONTACT_FALLBACK_EMAIL if that inbox " +
            "is not monitored yet."
}
