package de.richargh.pipeline.monitor.widgets.menu

import de.richargh.pipeline.monitor.status.app.api.PipelineStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MenuBuilderTest {
    @Test
    fun `MenuBuilder can be instantiated`() {
        val builder = MenuBuilder()
        Assertions.assertNotNull(builder)
    }

    @Test
    fun `MenuBuilder generates list of menu items`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        Assertions.assertNotNull(items)
        Assertions.assertTrue(items.isNotEmpty())
    }

    @Test
    fun `MenuBuilder includes status item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val statusItem = items.find { it.type == MenuItemType.STATUS }
        Assertions.assertNotNull(statusItem)
    }

    @Test
    fun `MenuBuilder includes separator`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val separatorItem = items.find { it.type == MenuItemType.SEPARATOR }
        Assertions.assertNotNull(separatorItem)
    }

    @Test
    fun `MenuBuilder includes refresh item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val refreshItem = items.find { it.type == MenuItemType.REFRESH }
        Assertions.assertNotNull(refreshItem)
        Assertions.assertEquals("Refresh", refreshItem?.label)
    }

    @Test
    fun `MenuBuilder includes quit item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val quitItem = items.find { it.type == MenuItemType.QUIT }
        Assertions.assertNotNull(quitItem)
        Assertions.assertEquals("Quit", quitItem?.label)
    }

    @Test
    fun `MenuBuilder includes settings item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val settingsItem = items.find { it.type == MenuItemType.SETTINGS }
        Assertions.assertNotNull(settingsItem)
        Assertions.assertEquals("Settings...", settingsItem?.label)
    }

    @Test
    fun `status item displays current pipeline status`() {
        val builder = MenuBuilder()
        builder.setPipelineStatus(PipelineStatus.SUCCESS)
        val items = builder.buildMenuItems()
        val statusItem = items.find { it.type == MenuItemType.STATUS }
        Assertions.assertTrue(statusItem?.label?.contains("Success") == true)
    }
}
