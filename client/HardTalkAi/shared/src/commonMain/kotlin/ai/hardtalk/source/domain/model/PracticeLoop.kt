package ai.hardtalk.source.domain.model

/**
 * Career-coaching practice is a short scored drill, not an open-ended chat.
 * After [MAX_USER_TURNS] scored replies the round ends and the user retries
 * the same scenario or picks another career drill from the FastAPI catalog.
 */
object PracticeLoop {
    const val MAX_USER_TURNS = 3
}
