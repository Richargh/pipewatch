package de.richargh.pipeline.monitor.settings.multiproject

import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
data class ProjectConfig(
    val id: String,
    val name: String,
    val gitLabUrl: String,
    val projectPath: String,
    val projectId: Long,
    val tokenId: String
) {
    val displayName: String
        get() {
            val host = try {
                val url = URL(gitLabUrl)
                if (url.port != -1 && url.port != url.defaultPort) {
                    "${url.host}:${url.port}"
                } else {
                    url.host
                }
            } catch (e: Exception) {
                gitLabUrl
            }
            return "$name ($host)"
        }
}
