package de.richargh.pipeline.monitor.config

import de.richargh.pipeline.monitor.gitlab.api.GitLabConfig

data class AppConfig(
    val gitLabConfig: GitLabConfig?,
    val projectId: Long?,
    val ref: String?,
    val refreshIntervalSeconds: Long,
) {
    val isConfigured: Boolean
        get() = gitLabConfig != null && projectId != null

    companion object {
        fun fromEnvironment(): AppConfig {
            val baseUrl = System.getenv("GITLAB_URL")
            val accessToken = System.getenv("GITLAB_TOKEN")
            val projectId = System.getenv("GITLAB_PROJECT_ID")?.toLongOrNull()
            val ref = System.getenv("GITLAB_REF")
            val refreshInterval = System.getenv("GITLAB_REFRESH_INTERVAL")?.toLongOrNull() ?: 60

            val gitLabConfig =
                if (!baseUrl.isNullOrBlank() && !accessToken.isNullOrBlank()) {
                    GitLabConfig(baseUrl, accessToken)
                } else {
                    null
                }

            return AppConfig(
                gitLabConfig = gitLabConfig,
                projectId = projectId,
                ref = ref,
                refreshIntervalSeconds = refreshInterval,
            )
        }
    }
}
