package de.richargh.pipewatch.widgets.menu

class MenuActionHandler(
    private val onQuit: (() -> Unit)? = null,
    private val onRefresh: (() -> Unit)? = null,
    private val onSettings: (() -> Unit)? = null,
    private val onOpenGitLab: (() -> Unit)? = null,
) {
    fun handleQuit() {
        onQuit?.invoke()
    }

    fun handleRefresh() {
        onRefresh?.invoke()
    }

    fun handleSettings() {
        onSettings?.invoke()
    }

    fun handleOpenGitLab() {
        onOpenGitLab?.invoke()
    }

    fun handleAction(menuItem: MenuItem) {
        when (menuItem.type) {
            MenuItemType.QUIT -> handleQuit()
            MenuItemType.REFRESH -> handleRefresh()
            MenuItemType.SETTINGS -> handleSettings()
            MenuItemType.OPEN_GITLAB -> handleOpenGitLab()
            else -> { /* No action for other menu item types */ }
        }
    }
}
