package de.richargh.pipeline.monitor

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import de.richargh.pipeline.monitor.status.app.api.PipelineStatus
import de.richargh.pipeline.monitor.widgets.menu.MenuActionHandler
import de.richargh.pipeline.monitor.widgets.menu.MenuBuilder
import de.richargh.pipeline.monitor.widgets.menu.MenuItemType

fun main() =
    application {
        val trayState = rememberTrayState()
        var pipelineStatus by remember { mutableStateOf(PipelineStatus.UNKNOWN) }

        val trayIconManager = remember { TrayIconManager() }
        val menuBuilder = remember { MenuBuilder() }
        val app = remember { Application() }

        val menuActionHandler =
            remember {
                MenuActionHandler(
                    onQuit = {
                        app.stop()
                        exitApplication()
                    },
                    onRefresh = {
                        // TODO: Implement refresh logic in Phase 2
                        println("Refresh triggered")
                    },
                    onSettings = {
                        // TODO: Implement settings window in Phase 4
                        println("Settings triggered")
                    },
                    onOpenGitLab = {
                        // TODO: Implement open in browser in Phase 5
                        println("Open in GitLab triggered")
                    },
                )
            }

        LaunchedEffect(Unit) {
            app.start()
        }

        LaunchedEffect(pipelineStatus) {
            trayIconManager.setStatus(pipelineStatus)
            menuBuilder.setPipelineStatus(pipelineStatus)
        }

        val icon =
            remember(pipelineStatus) {
                BitmapPainter(trayIconManager.getIconForStatus(pipelineStatus).toComposeImageBitmap())
            }

        Tray(
            state = trayState,
            icon = icon,
            tooltip = "GitLab Pipeline Monitor - ${pipelineStatus.displayName}",
            menu = {
                val menuItems = menuBuilder.buildMenuItems()

                menuItems.forEach { item ->
                    when (item.type) {
                        MenuItemType.SEPARATOR -> Separator()
                        MenuItemType.STATUS -> Item(item.label, enabled = false, onClick = {})
                        MenuItemType.REFRESH -> Item(item.label, onClick = { menuActionHandler.handleRefresh() })
                        MenuItemType.SETTINGS -> Item(item.label, onClick = { menuActionHandler.handleSettings() })
                        MenuItemType.OPEN_GITLAB -> Item(item.label, onClick = { menuActionHandler.handleOpenGitLab() })
                        MenuItemType.QUIT -> Item(item.label, onClick = { menuActionHandler.handleQuit() })
                    }
                }
            },
        )
    }
