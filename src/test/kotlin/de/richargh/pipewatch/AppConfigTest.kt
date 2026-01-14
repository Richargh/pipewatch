package de.richargh.pipewatch

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppConfigTest {
    @Test
    fun `version returns value from manifest or fallback`() {
        val config = AppConfig()
        assertNotNull(config.version)
        assertTrue(config.version.isNotBlank())
    }
}
