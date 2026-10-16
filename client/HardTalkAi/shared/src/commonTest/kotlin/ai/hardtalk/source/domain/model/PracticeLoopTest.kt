package ai.hardtalk.source.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PracticeLoopTest {

    @Test
    fun `call duration cap stays at the 90 second drill`() {
        assertEquals(3, PracticeLoop.MAX_USER_TURNS)
        assertEquals(90, PracticeLoop.MAX_CALL_DURATION_SECONDS)
    }
}
