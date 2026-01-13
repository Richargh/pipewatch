package de.richargh.pipewatch.settings

import com.russhwolf.settings.Settings
import java.net.MalformedURLException
import java.net.URL

class SettingsRepository(
    private val settings: Settings = Settings(),
) {
    var gitLabUrl: String?
        get() = settings.getStringOrNull(KEY_GITLAB_URL)
        set(value) {
            if (value == null) {
                settings.remove(KEY_GITLAB_URL)
            } else {
                settings.putString(KEY_GITLAB_URL, value.trimEnd('/'))
            }
        }

    var accessToken: String?
        get() = settings.getStringOrNull(KEY_ACCESS_TOKEN)
        set(value) {
            if (value == null) {
                settings.remove(KEY_ACCESS_TOKEN)
            } else {
                settings.putString(KEY_ACCESS_TOKEN, value.trim())
            }
        }

    var selectedProjectId: Long?
        get() {
            val value = settings.getLongOrNull(KEY_PROJECT_ID)
            return if (value == -1L) null else value
        }
        set(value) {
            if (value == null) {
                settings.remove(KEY_PROJECT_ID)
            } else {
                settings.putLong(KEY_PROJECT_ID, value)
            }
        }

    var selectedProjectName: String?
        get() = settings.getStringOrNull(KEY_PROJECT_NAME)
        set(value) {
            if (value == null) {
                settings.remove(KEY_PROJECT_NAME)
            } else {
                settings.putString(KEY_PROJECT_NAME, value)
            }
        }

    val selectedProject: SelectedProject?
        get() {
            val id = selectedProjectId ?: return null
            val name = selectedProjectName ?: return null
            return SelectedProject(id, name)
        }

    fun setSelectedProject(project: SelectedProject) {
        selectedProjectId = project.id
        selectedProjectName = project.name
    }

    fun clearSelectedProject() {
        selectedProjectId = null
        selectedProjectName = null
    }

    var refreshInterval: RefreshInterval
        get() {
            val seconds = settings.getInt(KEY_REFRESH_INTERVAL, RefreshInterval.DEFAULT.duration.inWholeSeconds.toInt())
            return RefreshInterval.fromSeconds(seconds)
        }
        set(value) {
            settings.putInt(KEY_REFRESH_INTERVAL, value.duration.inWholeSeconds.toInt())
        }

    var notificationsEnabled: Boolean
        get() = settings.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = settings.putBoolean(KEY_NOTIFICATIONS_ENABLED, value)

    var branchFilter: String?
        get() = settings.getStringOrNull(KEY_BRANCH_FILTER)
        set(value) {
            if (value == null) {
                settings.remove(KEY_BRANCH_FILTER)
            } else {
                settings.putString(KEY_BRANCH_FILTER, value)
            }
        }

    val isConfigured: Boolean
        get() =
            isValidGitLabUrl(gitLabUrl) &&
                !accessToken.isNullOrBlank() &&
                selectedProjectId != null

    fun clearAll() {
        settings.clear()
    }

    companion object {
        private const val KEY_GITLAB_URL = "gitlab_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_PROJECT_NAME = "project_name"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_seconds"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_BRANCH_FILTER = "branch_filter"

        fun isValidGitLabUrl(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            return try {
                val parsed = URL(url)
                parsed.protocol in listOf("http", "https")
            } catch (e: MalformedURLException) {
                false
            }
        }
    }
}
