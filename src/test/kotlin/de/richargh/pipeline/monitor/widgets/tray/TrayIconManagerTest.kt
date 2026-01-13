package de.richargh.pipeline.monitor.widgets.tray

import de.richargh.pipeline.monitor.status.app.api.PipelineStatus
import de.richargh.pipeline.monitor.widget.tray.TrayIconManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class TrayIconManagerTest {
    @Test
    fun `TrayIconManager can be instantiated`() {
        val manager = TrayIconManager()
        assertNotNull(manager)
    }

    @Test
    fun `TrayIconManager has default gray status`() {
        val manager = TrayIconManager()
        assertEquals(PipelineStatus.UNKNOWN, manager.currentStatus)
    }

    @Test
    fun `TrayIconManager can generate icon for status`() {
        val manager = TrayIconManager()
        val icon = manager.getIconForStatus(PipelineStatus.UNKNOWN)
        assertNotNull(icon)
        assertTrue(icon is BufferedImage)
    }

    @Test
    fun `setStatus updates currentStatus`() {
        val manager = TrayIconManager()
        manager.setStatus(PipelineStatus.SUCCESS)
        assertEquals(PipelineStatus.SUCCESS, manager.currentStatus)
    }
}
