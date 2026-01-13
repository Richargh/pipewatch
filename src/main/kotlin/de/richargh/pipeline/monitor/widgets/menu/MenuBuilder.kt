package de.richargh.pipeline.monitor.widgets.menu

import de.richargh.pipeline.monitor.status.app.api.PipelineStatus

enum class MenuItemType {
    STATUS,
    SEPARATOR,
    REFRESH,
    SETTINGS,
    OPEN_GITLAB,
    QUIT,
}

data class MenuItem(
    val type: MenuItemType,
    val label: String = "",
    val enabled: Boolean = true,
    val action: (() -> Unit)? = null,
)

class MenuBuilder {
    private var pipelineStatus: PipelineStatus = PipelineStatus.UNKNOWN
    private var lastUpdated: String? = null

    fun setPipelineStatus(status: PipelineStatus) {
        pipelineStatus = status
    }

    fun setLastUpdated(timestamp: String?) {
        lastUpdated = timestamp
    }

    fun buildMenuItems(): List<MenuItem> {
        val items = mutableListOf<MenuItem>()

        // Status item
        val statusLabel = buildStatusLabel()
        items.add(
            MenuItem(
                type = MenuItemType.STATUS,
                label = statusLabel,
                enabled = false,
            ),
        )

        // Last updated timestamp
        lastUpdated?.let {
            items.add(
                MenuItem(
                    type = MenuItemType.STATUS,
                    label = "Updated: $it",
                    enabled = false,
                ),
            )
        }

        items.add(MenuItem(type = MenuItemType.SEPARATOR))

        // Action items
        items.add(
            MenuItem(
                type = MenuItemType.OPEN_GITLAB,
                label = "Open in GitLab",
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
        return "Pipeline: ${pipelineStatus.displayName}"
    }
}
