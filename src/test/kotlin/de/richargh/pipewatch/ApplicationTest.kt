package de.richargh.pipewatch

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationTest {
    @Test
    fun `Application class can be instantiated`() {
        val app = Application()
        assertNotNull(app)
    }

    @Test
    fun `Application has isRunning property initially false`() {
        val app = Application()
        assertFalse(app.isRunning)
    }

    @Test
    fun `Application start sets isRunning to true`() {
        val app = Application()
        app.start()
        assertTrue(app.isRunning)
    }

    @Test
    fun `Application stop sets isRunning to false`() {
        val app = Application()
        app.start()
        app.stop()
        assertFalse(app.isRunning)
    }

    @Test
    fun `Application stop triggers shutdown callback`() {
        var shutdownCalled = false
        val app = Application()
        app.onShutdown = { shutdownCalled = true }
        app.start()
        app.stop()
        assertTrue(shutdownCalled)
    }

    @Test
    fun `Application cleanup is called on stop`() {
        var cleanupCalled = false
        val app = Application()
        app.onCleanup = { cleanupCalled = true }
        app.start()
        app.stop()
        assertTrue(cleanupCalled)
    }
}
