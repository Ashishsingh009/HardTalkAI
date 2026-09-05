package com.hardtalkai.shared

import kotlin.math.floor

private val EMPATHY_MARKERS = listOf(
    "understand", "i hear", "i know", "appreciate", "thank", "must be",
    "i can imagine", "that makes sense", "i'm sorry", "sorry to hear",
    "how are you", "how do you feel",
)

private val ASSERTIVE_MARKERS = listOf(
    "i want", "i need", "i'd like", "i would like", "i am asking", "i'm asking",
    "i expect", "i believe", "i think we should", "let's", "i've decided",
    "i cannot", "i can't take", "no,",
)

private val HEDGES = listOf(
    "maybe", "just", "kind of", "sort of", "i guess", "if that's okay",
    "if it's not too much", "sorry to bother", "i'm not sure", "possibly",
)

private val AGGRESSIVE_MARKERS = listOf(
    "you always", "you never", "your fault", "ridiculous", "stupid",
    "shut up", "obviously you", "you people",
)

private fun countMatches(text: String, markers: List<String>): Int =
    markers.count { text.contains(it) }

/** JS `Math.round`-compatible clamp into the 0-100 range. */
private fun clamp(n: Double): Int = floor(n + 0.5).toInt().coerceIn(0, 100)

private data class Signals(
    val aggressiveHits: Int,
    val hedgeHits: Int,
    val empathyHits: Int,
    val assertiveHits: Int,
    val wordCount: Int,
)

/** Analyze the latest user message and produce coaching feedback. */
fun scoreMessage(message: String): Feedback {
    val text = message.lowercase().trim()
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    val wordCount = words.size

    val empathyHits = countMatches(text, EMPATHY_MARKERS)
    val assertiveHits = countMatches(text, ASSERTIVE_MARKERS)
    val hedgeHits = countMatches(text, HEDGES)
    val aggressiveHits = countMatches(text, AGGRESSIVE_MARKERS)
    val hasQuestion = text.contains("?")
    val hasNumbers = Regex("\\d").containsMatchIn(text)

    // Empathy: acknowledging the other person and asking about them.
    var empathy = 35 + empathyHits * 22 + (if (hasQuestion) 12 else 0)
    empathy -= aggressiveHits * 30

    // Assertiveness: clear "I" statements and direct asks, minus hedging.
    var assertiveness = 30 + assertiveHits * 22 - hedgeHits * 12
    if (aggressiveHits > 0) assertiveness -= 10 // aggression is not assertiveness

    // Clarity: reasonable length, concreteness, not rambling.
    var clarity = 45
    when {
        wordCount in 8..60 -> clarity += 25
        wordCount < 4 -> clarity -= 25
        wordCount > 90 -> clarity -= 15
    }
    if (hasNumbers) clarity += 10
    clarity -= hedgeHits * 6

    val clarityScore = clamp(clarity.toDouble())
    val empathyScore = clamp(empathy.toDouble())
    val assertivenessScore = clamp(assertiveness.toDouble())
    val overall = clamp(clarityScore * 0.34 + empathyScore * 0.33 + assertivenessScore * 0.33)

    val signals = Signals(aggressiveHits, hedgeHits, empathyHits, assertiveHits, wordCount)
    return Feedback(
        clarity = clarityScore,
        empathy = empathyScore,
        assertiveness = assertivenessScore,
        overall = overall,
        tips = buildTips(clarityScore, empathyScore, assertivenessScore, signals),
    )
}

private fun buildTips(clarity: Int, empathy: Int, assertiveness: Int, s: Signals): List<String> {
    val tips = mutableListOf<String>()
    if (s.aggressiveHits > 0) {
        tips.add(
            "Watch out for blaming language like \"you always\" - it puts people on the defensive. " +
                "Describe the behavior, not the character.",
        )
    }
    if (empathy < 45) {
        tips.add(
            "Acknowledge the other person first. A line like \"I know you've been slammed\" " +
                "lowers defenses before you make your point.",
        )
    }
    if (assertiveness < 45) {
        tips.add(
            "Be more direct. Lead with a clear \"I\" statement such as \"I'd like...\" " +
                "so your ask can't be missed.",
        )
    }
    if (s.hedgeHits >= 2) {
        tips.add(
            "You're hedging a lot (\"just\", \"maybe\", \"sorry\"). " +
                "Trimming qualifiers makes you sound more confident.",
        )
    }
    if (clarity < 45 && s.wordCount < 8) {
        tips.add("Add a bit more detail - one concrete example or number makes your point land.")
    }
    if (clarity < 45 && s.wordCount > 90) {
        tips.add("Tighten it up. A shorter, focused message is easier to respond to.")
    }
    if (tips.isEmpty()) {
        tips.add(
            "Nicely balanced - clear, direct, and considerate. " +
                "Keep steering toward a concrete next step.",
        )
    }
    return tips
}

/**
 * Produce the counterpart's reply. The counterpart warms up when the user is
 * both empathetic and assertive, and gets guarded when the user is aggressive
 * or wishy-washy. Replies are scenario-specific so the practice feels real.
 */
fun generateReply(scenario: Scenario, history: List<ChatTurn>, userMessage: String): ReplyResult {
    val feedback = scoreMessage(userMessage)
    val turnNumber = history.count { it.role == "user" } + 1
    val tone = replyTone(feedback)
    val reply = replyText(scenario, tone, turnNumber)
    return ReplyResult(reply = reply, feedback = feedback, mood = moodLabel(tone))
}

private enum class Tone { WARMING, NEUTRAL, GUARDED }

private fun replyTone(f: Feedback): Tone = when {
    f.overall >= 60 && f.empathy >= 50 -> Tone.WARMING
    f.overall <= 42 || f.empathy <= 30 -> Tone.GUARDED
    else -> Tone.NEUTRAL
}

private fun moodLabel(tone: Tone): String = when (tone) {
    Tone.WARMING -> "opening up and more receptive"
    Tone.GUARDED -> "defensive and cautious"
    Tone.NEUTRAL -> "listening, weighing what you said"
}

private val REPLIES: Map<String, Map<Tone, List<String>>> = mapOf(
    "ask-for-raise" to mapOf(
        Tone.WARMING to listOf(
            "Okay, I appreciate you laying that out so clearly. Those results are real. Let me look at what's possible in the budget - walk me through the numbers you have in mind.",
            "That's a fair case, honestly. I can't promise the full amount today, but I want to advocate for you. Can you send me a short summary I can take to my director?",
        ),
        Tone.NEUTRAL to listOf(
            "I hear you. Money's tight this cycle, though. What specifically are you basing the number on?",
            "I get that you want more. Help me understand the impact - what did you ship that moved the needle?",
        ),
        Tone.GUARDED to listOf(
            "Look, everyone thinks they deserve a raise. I need more than 'I work hard' to take this upstairs.",
            "That's a lot to drop on me. Budgets are frozen. What exactly are you expecting me to do here?",
        ),
    ),
    "give-feedback" to mapOf(
        Tone.WARMING to listOf(
            "Thanks for saying it that way - that's fair. You're right that the deadlines slipped. I've been drowning, but that's on me to flag earlier. What would help?",
            "I appreciate you being honest and not just piling on. Yeah, the launch date thing hurt the team. Let's figure out a way to catch this sooner.",
        ),
        Tone.NEUTRAL to listOf(
            "Okay... I didn't realize it was landing that hard on everyone. What deadlines are you talking about specifically?",
            "That's tough to hear. I've had a lot going on. Can you give me an example so I understand?",
        ),
        Tone.GUARDED to listOf(
            "Wow. I've been putting in a ton of hours, and this is what I get? It's not all on me, you know.",
            "That feels really unfair. Everyone's behind, not just me. Why am I the one getting this talk?",
        ),
    ),
    "decline-request" to mapOf(
        Tone.WARMING to listOf(
            "Ah, okay - I hear you that you're at capacity. I don't want to burn you out. When could you realistically pick it up?",
            "That's fair, and thanks for being straight with me. Who else do you think could take the dashboard, or should we push it a sprint?",
        ),
        Tone.NEUTRAL to listOf(
            "Hmm, but this is pretty important to leadership. Are you sure you can't squeeze it in?",
            "I understand you're busy, but everyone's busy. Isn't there any way to make it work?",
        ),
        Tone.GUARDED to listOf(
            "I really need this done, though. Can't you just find the time? It won't take long.",
            "That's disappointing. I was counting on you. So you're just saying no?",
        ),
    ),
)

private fun replyText(scenario: Scenario, tone: Tone, turn: Int): String {
    val bank = REPLIES[scenario.id]?.get(tone)
    if (bank.isNullOrEmpty()) return "Go on - I'm listening."
    return bank[(turn - 1) % bank.size]
}
