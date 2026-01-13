package de.richargh.pipewatch.settings.multiproject

import java.net.MalformedURLException
import java.net.URL

data class ParsedGitLabUrl(
    val gitLabUrl: String,
    val projectPath: String,
)

class GitLabUrlParser {
    companion object {
        // GitLab-specific path segments that indicate the start of non-project paths
        private val GITLAB_PATH_MARKERS = setOf("-", "api", "users", "groups", "explore", "admin")
    }

    fun parse(url: String): ParsedGitLabUrl? {
        if (url.isBlank()) return null

        val parsedUrl =
            try {
                URL(url.trim())
            } catch (e: MalformedURLException) {
                return null
            }

        if (parsedUrl.protocol !in listOf("http", "https")) return null

        val path = parsedUrl.path?.trimEnd('/') ?: return null
        if (path.isEmpty()) return null

        val segments = path.split('/').filter { it.isNotEmpty() }

        // Need at least group/project (2 segments)
        if (segments.size < 2) return null

        // Extract project path by finding where GitLab markers start
        val projectSegments = mutableListOf<String>()
        for (segment in segments) {
            if (segment in GITLAB_PATH_MARKERS) break
            projectSegments.add(segment)
        }

        // Need at least group/project
        if (projectSegments.size < 2) return null

        val baseUrl =
            buildString {
                append(parsedUrl.protocol)
                append("://")
                append(parsedUrl.host)
                if (parsedUrl.port != -1 && parsedUrl.port != parsedUrl.defaultPort) {
                    append(":")
                    append(parsedUrl.port)
                }
            }

        val projectPath = projectSegments.joinToString("/")

        return ParsedGitLabUrl(
            gitLabUrl = baseUrl,
            projectPath = projectPath,
        )
    }
}
