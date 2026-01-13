package de.richargh.pipeline.monitor.widgets.menu

import de.richargh.pipeline.monitor.gitlab.api.Job
import de.richargh.pipeline.monitor.gitlab.api.Pipeline
import de.richargh.pipeline.monitor.status.app.api.PipelineStatus
import de.richargh.pipeline.monitor.util.TimeFormatter

enum class MenuItemType {
    STATUS,
    SEPARATOR,
    REFRESH,
    SETTINGS,
    OPEN_GITLAB,
    QUIT,
    FAILED_STAGE_HEADER,
    FAILED_JOB,
}

data class MenuItem(
    val type: MenuItemType,
    val label: String = "",
    val enabled: Boolean = true,
    val action: (() -> Unit)? = null,
    val url: String? = null,
)

data class PipelineMenuState(
    val status: PipelineStatus = PipelineStatus.UNKNOWN,
    val pipeline: Pipeline? = null,
    val projectName: String? = null,
    val failedJobsByStage: Map<String, List<Job>> = emptyMap(),
)

class MenuBuilder {
    private var menuState: PipelineMenuState = PipelineMenuState()

    fun setPipelineStatus(status: PipelineStatus) {
        menuState = menuState.copy(status = status)
    }

    fun setPipeline(pipeline: Pipeline?) {
        menuState = menuState.copy(pipeline = pipeline)
    }

    fun setProjectName(name: String?) {
        menuState = menuState.copy(projectName = name)
    }

    fun setFailedJobsByStage(failedJobs: Map<String, List<Job>>) {
        menuState = menuState.copy(failedJobsByStage = failedJobs)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setLastUpdated(timestamp: String?) {
        // Kept for backwards compatibility - now we use pipeline.updatedAt
    }

    fun buildMenuItems(): List<MenuItem> {
        val items = mutableListOf<MenuItem>()

        // Project name (if available)
        menuState.projectName?.let {
            items.add(
                MenuItem(
                    type = MenuItemType.STATUS,
                    label = it,
                    enabled = false,
                ),
            )
        }

        // Pipeline status with branch
        val statusLabel = buildStatusLabel()
        items.add(
            MenuItem(
                type = MenuItemType.STATUS,
                label = statusLabel,
                enabled = false,
            ),
        )

        // Last updated timestamp
        val relativeTime = TimeFormatter.formatRelativeTime(menuState.pipeline?.updatedAt)
        items.add(
            MenuItem(
                type = MenuItemType.STATUS,
                label = "Updated $relativeTime",
                enabled = false,
            ),
        )

        // Failed stages/jobs (if pipeline failed)
        if (menuState.status == PipelineStatus.FAILED && menuState.failedJobsByStage.isNotEmpty()) {
            items.add(MenuItem(type = MenuItemType.SEPARATOR))

            menuState.failedJobsByStage.forEach { (stage, jobs) ->
                items.add(
                    MenuItem(
                        type = MenuItemType.FAILED_STAGE_HEADER,
                        label = "Failed in $stage:",
                        enabled = false,
                    ),
                )

                jobs.forEach { job ->
                    items.add(
                        MenuItem(
                            type = MenuItemType.FAILED_JOB,
                            label = "  ${job.name}",
                            enabled = true,
                            url = job.webUrl,
                        ),
                    )
                }
            }
        }

        items.add(MenuItem(type = MenuItemType.SEPARATOR))

        // Action items
        items.add(
            MenuItem(
                type = MenuItemType.OPEN_GITLAB,
                label = "Open in GitLab",
                url = menuState.pipeline?.webUrl,
            ),
        )

        items.add(
            MenuItem(
                type = MenuItemType.REFRESH,
                label = "Refresh",
            ),
        )

        items.add(MenuItem(type = MenuItemType.SEPARATOR))

        items.add(
            MenuItem(
                type = MenuItemType.SETTINGS,
                label = "Settings...",
            ),
        )

        items.add(MenuItem(type = MenuItemType.SEPARATOR))

        items.add(
            MenuItem(
                type = MenuItemType.QUIT,
                label = "Quit",
            ),
        )

        return items
    }

    private fun buildStatusLabel(): String {
        val pipeline = menuState.pipeline
        return if (pipeline != null) {
            "${menuState.status.displayName} #${pipeline.iid} (${pipeline.ref})"
        } else {
            "Pipeline: ${menuState.status.displayName}"
        }
    }
}
