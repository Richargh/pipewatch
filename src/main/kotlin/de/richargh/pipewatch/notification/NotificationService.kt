package de.richargh.pipewatch.notification

import java.awt.SystemTray
import java.awt.TrayIcon

interface NotificationService {
    fun sendNotification(content: NotificationContent)

    var notificationsEnabled: Boolean
}

class DesktopNotificationService(
    private val trayIcon: TrayIcon?,
    private val onNotificationClick: ((String?) -> Unit)? = null,
) : NotificationService {
    override var notificationsEnabled: Boolean = true

    private var lastNotificationUrl: String? = null

    init {
        trayIcon?.addActionListener {
            lastNotificationUrl?.let { url ->
                onNotificationClick?.invoke(url)
            }
        }
    }

    override fun sendNotification(content: NotificationContent) {
        if (!notificationsEnabled) return
        if (!SystemTray.isSupported()) return

        trayIcon?.let { icon ->
            lastNotificationUrl = content.url
            icon.displayMessage(
                content.title,
                content.body,
                TrayIcon.MessageType.WARNING,
            )
        }
    }
}

class ComposeNotificationService(
    private val onNotificationClick: ((String?) -> Unit)? = null,
) : NotificationService {
    override var notificationsEnabled: Boolean = true

    override fun sendNotification(content: NotificationContent) {
        if (!notificationsEnabled) return

        // On macOS, we can use the Compose tray notification
        // The tray state's sendNotification can be called from a remembered state
        // For now, we'll use a callback pattern
        try {
            sendDesktopNotification(content)
        } catch (e: Exception) {
            println("Failed to send notification: ${e.message}")
        }
    }

    private fun sendDesktopNotification(content: NotificationContent) {
        // Use ProcessBuilder to send notification on macOS
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            val script =
                """
                display notification "${content.body}" with title "${content.title}"
                """.trimIndent()
            ProcessBuilder("osascript", "-e", script).start()
        }
    }
}
