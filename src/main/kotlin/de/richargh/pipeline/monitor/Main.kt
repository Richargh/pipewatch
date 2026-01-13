package de.richargh.pipeline.monitor

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import de.richargh.pipeline.monitor.config.AppConfig
import de.richargh.pipeline.monitor.gitlab.api.GitLabClient
import de.richargh.pipeline.monitor.polling.PollingService
import de.richargh.pipeline.monitor.status.app.api.PipelineStatus
import de.richargh.pipeline.monitor.viewmodel.PipelineViewModel
import de.richargh.pipeline.monitor.widget.tray.TrayIconManager
import de.richargh.pipeline.monitor.widgets.menu.MenuActionHandler
import de.richargh.pipeline.monitor.widgets.menu.MenuBuilder
import de.richargh.pipeline.monitor.widgets.menu.MenuItemType
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

fun main() =
    application {
        val trayState = rememberTrayState()
        val scope = rememberCoroutineScope()

        val appConfig = remember { AppConfig.fromEnvironment() }
        var configError by remember { mutableStateOf<String?>(null) }

        val gitLabClient =
            remember {
                if (appConfig.isConfigured) {
                    GitLabClient(appConfig.gitLabConfig!!)
                } else {
                    configError = "Not configured. Set GITLAB_URL, GITLAB_TOKEN, and GITLAB_PROJECT_ID environment variables."
                    null
                }
            }

        val viewModel =
            remember {
                if (gitLabClient != null && appConfig.projectId != null) {
                    PipelineViewModel(gitLabClient, appConfig.projectId, appConfig.ref)
                } else {
                    null
                }
            }

        val pollingService =
            remember {
                PollingService(interval = appConfig.refreshIntervalSeconds.seconds)
            }

        val pipelineStatus by viewModel?.pipelineStatus?.collectAsState()
            ?: remember { mutableStateOf(PipelineStatus.UNKNOWN) }
        val currentPipeline by viewModel?.currentPipeline?.collectAsState()
            ?: remember { mutableStateOf(null) }

        val trayIconManager = remember { TrayIconManager() }
        val menuBuilder = remember { MenuBuilder() }
        val app = remember { Application() }

        val menuActionHandler =
            remember {
                MenuActionHandler(
                    onQuit = {
                        pollingService.stop()
                        gitLabClient?.close()
                        app.stop()
                        exitApplication()
                    },
                    onRefresh = {
                        scope.launch {
                            viewModel?.refresh()
                        }
                    },
                    onSettings = {
                        // TODO: Implement settings window in Phase 4
                        println("Settings triggered - configure via environment variables for now")
                    },
                    onOpenGitLab = {
                        currentPipeline?.webUrl?.let { url ->
                            try {
                                java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                            } catch (e: Exception) {
                                println("Failed to open browser: ${e.message}")
                            }
                        }
                    },
                )
            }

        LaunchedEffect(Unit) {
            app.start()

            if (viewModel != null) {
                pollingService.start {
                    viewModel.refresh()
                }
            }
        }

        LaunchedEffect(pipelineStatus) {
            trayIconManager.setStatus(pipelineStatus)
        }

        val icon =
            remember(pipelineStatus) {
                BitmapPainter(trayIconManager.getIconForStatus(pipelineStatus).toComposeImageBitmap())
            }

        val tooltipText =
            buildString {
                append("GitLab Pipeline Monitor")
                if (configError != null) {
                    append(" - $configError")
                } else {
                    append(" - ${pipelineStatus.displayName}")
                    currentPipeline?.let { append(" (${it.ref})") }
                }
            }

        Tray(
            state = trayState,
            icon = icon,
            tooltip = tooltipText,
            menu = {
                if (configError != null) {
                    Item("⚠️ $configError", enabled = false, onClick = {})
                    Separator()
                }

                // Update menu builder state here to ensure recomposition
                menuBuilder.setPipelineStatus(pipelineStatus)
                currentPipeline?.let { menuBuilder.setLastUpdated("${it.ref} #${it.iid}") }

                val menuItems = menuBuilder.buildMenuItems()

                menuItems.forEach { item ->
                    when (item.type) {
                        MenuItemType.SEPARATOR -> Separator()
                        MenuItemType.STATUS -> Item(item.label, enabled = false, onClick = {})
                        MenuItemType.REFRESH ->
                            Item(
                                item.label,
                                enabled = viewModel != null,
                                onClick = { menuActionHandler.handleRefresh() },
                            )
                        MenuItemType.SETTINGS -> Item(item.label, onClick = { menuActionHandler.handleSettings() })
                        MenuItemType.OPEN_GITLAB ->
                            Item(
                                item.label,
                                enabled = currentPipeline?.webUrl != null,
                                onClick = { menuActionHandler.handleOpenGitLab() },
                            )
                        MenuItemType.QUIT -> Item(item.label, onClick = { menuActionHandler.handleQuit() })
                    }
                }
            },
        )
    }
