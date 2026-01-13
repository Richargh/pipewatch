package de.richargh.pipeline.monitor.widgets.menu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MenuActionsTest {
    @Test
    fun `MenuActionHandler can be instantiated`() {
        val handler = MenuActionHandler()
        Assertions.assertNotNull(handler)
    }

    @Test
    fun `quit action triggers onQuit callback`() {
        var quitCalled = false
        val handler =
            MenuActionHandler(
                onQuit = { quitCalled = true },
            )
        handler.handleQuit()
        Assertions.assertTrue(quitCalled)
    }

    @Test
    fun `refresh action triggers onRefresh callback`() {
        var refreshCalled = false
        val handler =
            MenuActionHandler(
                onRefresh = { refreshCalled = true },
            )
        handler.handleRefresh()
        Assertions.assertTrue(refreshCalled)
    }

    @Test
    fun `settings action triggers onSettings callback`() {
        var settingsCalled = false
        val handler =
            MenuActionHandler(
                onSettings = { settingsCalled = true },
            )
        handler.handleSettings()
        Assertions.assertTrue(settingsCalled)
    }

    @Test
    fun `openGitLab action triggers onOpenGitLab callback`() {
        var openGitLabCalled = false
        val handler =
            MenuActionHandler(
                onOpenGitLab = { openGitLabCalled = true },
            )
        handler.handleOpenGitLab()
        Assertions.assertTrue(openGitLabCalled)
    }
}
