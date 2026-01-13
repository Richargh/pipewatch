package de.richargh.pipewatch

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
import de.richargh.pipewatch.gitlab.api.GitLabClient
import de.richargh.pipewatch.gitlab.api.Job
import de.richargh.pipewatch.notification.ComposeNotificationService
import de.richargh.pipewatch.notification.NotificationBuilder
import de.richargh.pipewatch.notification.PipelineStateTracker
import de.richargh.pipewatch.polling.PollingService
import de.richargh.pipewatch.settings.SettingsRepository
import de.richargh.pipewatch.settings.multiproject.MultiProjectRepository
import de.richargh.pipewatch.settings.multiproject.MultiProjectSettingsWindow
import de.richargh.pipewatch.settings.multiproject.ProjectConfig
import de.richargh.pipewatch.settings.multiproject.TokenConfig
import java.util.UUID
import de.richargh.pipewatch.status.app.api.PipelineStatus
import de.richargh.pipewatch.viewmodel.PipelineViewModel
import de.richargh.pipewatch.widget.tray.TrayIconManager
import de.richargh.pipewatch.widgets.menu.MenuActionHandler
import de.richargh.pipewatch.widgets.menu.MenuBuilder
import de.richargh.pipewatch.widgets.menu.MenuItemType
import kotlinx.coroutines.launch

fun main() =
    application {
        val trayState = rememberTrayState()
        val scope = rememberCoroutineScope()

        val settingsRepository = remember { SettingsRepository() }
        val multiProjectRepository = remember { MultiProjectRepository() }

        // Migrate legacy settings if needed
        LaunchedEffect(Unit) {
            migrateFromLegacySettings(settingsRepository, multiProjectRepository)
        }

        var gitLabClient by remember {
            mutableStateOf(createGitLabClient(settingsRepository))
        }

        var viewModel by remember {
            mutableStateOf(createViewModel(gitLabClient, settingsRepository))
        }

        val pollingService =
            remember {
                PollingService(interval = settingsRepository.refreshInterval.duration)
            }

        val pipelineStatus by viewModel?.pipelineStatus?.collectAsState()
            ?: remember { mutableStateOf(PipelineStatus.UNKNOWN) }
        val currentPipeline by viewModel?.currentPipeline?.collectAsState()
            ?: remember { mutableStateOf(null) }
        val failedJobsByStage by viewModel?.failedJobsByStage?.collectAsState()
            ?: remember { mutableStateOf<Map<String, List<Job>>>(emptyMap()) }

        val trayIconManager = remember { TrayIconManager() }
        val menuBuilder = remember { MenuBuilder() }
        val app = remember { Application() }
        val stateTracker = remember { PipelineStateTracker() }
        val notificationService =
            remember {
                ComposeNotificationService(
                    onNotificationClick = { url -> url?.let { openInBrowser(it) } },
                )
            }

        var showSettingsWindow by remember { mutableStateOf(false) }

        val isConfigured = settingsRepository.isConfigured

        // Update notification service settings
        notificationService.notificationsEnabled = settingsRepository.notificationsEnabled

        val menuActionHandler =
            remember(viewModel, currentPipeline) {
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
                        showSettingsWindow = true
                    },
                    onOpenGitLab = {
                        currentPipeline?.webUrl?.let { url ->
                            openInBrowser(url)
                        }
                    },
                )
            }

        LaunchedEffect(Unit) {
            app.start()

            if (viewModel != null) {
                pollingService.start {
                    viewModel?.refresh()
                }
            } else if (!isConfigured) {
                // Show settings on first run if not configured
                showSettingsWindow = true
            }
        }

        LaunchedEffect(pipelineStatus) {
            trayIconManager.setStatus(pipelineStatus)

            // Check for failure and send notification
            val failureInfo = stateTracker.updateAndCheckForFailure(pipelineStatus, currentPipeline)
            if (failureInfo != null && failureInfo.isNewFailure) {
                val projectName = settingsRepository.selectedProjectName ?: "Unknown"
                val notification =
                    NotificationBuilder.buildFailureNotification(
                        failureInfo.pipeline,
                        projectName,
                        failedJobsByStage,
                    )
                notificationService.sendNotification(notification)
            }
        }

        val icon =
            remember(pipelineStatus) {
                BitmapPainter(trayIconManager.getIconForStatus(pipelineStatus).toComposeImageBitmap())
            }

        val tooltipText =
            buildString {
                append("GitLab Pipeline Monitor")
                if (!isConfigured) {
                    append(" - Not configured")
                } else {
                    append(" - ${pipelineStatus.displayName}")
                    currentPipeline?.let { append(" (${it.ref})") }
                }
            }

        if (showSettingsWindow) {
            MultiProjectSettingsWindow(
                settingsRepository = settingsRepository,
                multiProjectRepository = multiProjectRepository,
                onClose = { showSettingsWindow = false },
                onSave = {
                    // Recreate client and view model with new settings
                    gitLabClient?.close()
                    gitLabClient = createGitLabClient(settingsRepository)
                    viewModel = createViewModel(gitLabClient, settingsRepository)

                    // Update polling interval
                    pollingService.setInterval(settingsRepository.refreshInterval.duration)

                    // Start polling if we now have a valid configuration
                    if (viewModel != null) {
                        scope.launch {
                            pollingService.stop()
                            pollingService.start {
                                viewModel?.refresh()
                            }
                        }
                    }
                },
            )
        }

        Tray(
            state = trayState,
            icon = icon,
            tooltip = tooltipText,
            menu = {
                if (!isConfigured) {
                    Item("Not configured - click Settings to configure", enabled = false, onClick = {})
                    Separator()
                }

                // Update menu builder state
                menuBuilder.setPipelineStatus(pipelineStatus)
                menuBuilder.setPipeline(currentPipeline)
                menuBuilder.setProjectName(settingsRepository.selectedProjectName)
                menuBuilder.setFailedJobsByStage(failedJobsByStage)

                val menuItems = menuBuilder.buildMenuItems()

                menuItems.forEach { item ->
                    when (item.type) {
                        MenuItemType.SEPARATOR -> Separator()
                        MenuItemType.STATUS -> Item(item.label, enabled = false, onClick = {})
                        MenuItemType.FAILED_STAGE_HEADER -> Item(item.label, enabled = false, onClick = {})
                        MenuItemType.FAILED_JOB ->
                            Item(
                                item.label,
                                enabled = item.url != null,
                                onClick = { item.url?.let { openInBrowser(it) } },
                            )
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
                                enabled = item.url != null,
                                onClick = { item.url?.let { openInBrowser(it) } },
                            )
                        MenuItemType.QUIT -> Item(item.label, onClick = { menuActionHandler.handleQuit() })
                    }
                }
            },
        )
    }

private fun createGitLabClient(settingsRepository: SettingsRepository): GitLabClient? {
    return if (settingsRepository.isConfigured) {
        GitLabClient(
            baseUrl = settingsRepository.gitLabUrl!!,
            accessToken = settingsRepository.accessToken!!,
        )
    } else {
        null
    }
}

private fun createViewModel(
    client: GitLabClient?,
    settingsRepository: SettingsRepository,
): PipelineViewModel? {
    if (client == null) return null

    val projectId = settingsRepository.selectedProjectId ?: return null
    val ref = settingsRepository.branchFilter?.ifBlank { null }

    return PipelineViewModel(client, projectId, ref)
}

private fun openInBrowser(url: String) {
    try {
        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
    } catch (e: Exception) {
        println("Failed to open browser: ${e.message}")
    }
}

private fun migrateFromLegacySettings(
    settingsRepository: SettingsRepository,
    multiProjectRepository: MultiProjectRepository,
) {
    // Check if already migrated (projects exist in multi-project repo)
    if (multiProjectRepository.getAllProjects().isNotEmpty()) {
        return
    }

    // Check if legacy settings exist
    val gitLabUrl = settingsRepository.gitLabUrl ?: return
    val accessToken = settingsRepository.accessToken ?: return
    val projectId = settingsRepository.selectedProjectId ?: return
    val projectName = settingsRepository.selectedProjectName ?: return

    // Create token
    val tokenId = UUID.randomUUID().toString()
    multiProjectRepository.saveToken(
        TokenConfig(
            id = tokenId,
            gitLabUrl = gitLabUrl,
            accessToken = accessToken,
        )
    )

    // Create project config
    val projectConfigId = UUID.randomUUID().toString()
    multiProjectRepository.saveProject(
        ProjectConfig(
            id = projectConfigId,
            name = projectName,
            gitLabUrl = gitLabUrl,
            projectPath = projectName,  // Use name as path for legacy migration
            projectId = projectId,
            tokenId = tokenId,
        )
    )

    // Set as active project
    multiProjectRepository.activeProjectId = projectConfigId
}
