package ai.hardtalk.source

import ai.hardtalk.source.data.api.ANDROID_EMULATOR_API_BASE_URL
import ai.hardtalk.source.data.api.defaultApiBaseUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicAndroidHostTest {

    @Test
    fun androidDefaultApiBaseUrlTargetsEmulatorLoopback() {
        assertEquals(ANDROID_EMULATOR_API_BASE_URL, defaultApiBaseUrl())
        assertEquals("http://10.0.2.2:3001", defaultApiBaseUrl())
    }
}
