package ai.hardtalk.source.domain.model

/**
 * Career-coaching practice is a short scored drill, not an open-ended chat.
 * After [MAX_USER_TURNS] scored replies the round ends and the user retries
 * the same scenario or picks another of the three existing drills.
 */
object PracticeLoop {
    const val MAX_USER_TURNS = 3
}
