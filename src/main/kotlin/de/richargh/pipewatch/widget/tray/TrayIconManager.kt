package de.richargh.pipewatch.widget.tray

import de.richargh.pipewatch.status.app.api.PipelineStatus
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

class TrayIconManager {
    var currentStatus: PipelineStatus = PipelineStatus.UNKNOWN
        private set

    fun setStatus(status: PipelineStatus) {
        currentStatus = status
    }

    fun getIconForStatus(status: PipelineStatus): BufferedImage {
        return createCircleIcon(status.color)
    }

    fun getCurrentIcon(): BufferedImage {
        return getIconForStatus(currentStatus)
    }

    private fun createCircleIcon(
        color: Color,
        size: Int = 22,
    ): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2d: Graphics2D = image.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Draw filled circle
        g2d.color = color
        val padding = 2
        g2d.fillOval(padding, padding, size - padding * 2, size - padding * 2)

        g2d.dispose()
        return image
    }
}
