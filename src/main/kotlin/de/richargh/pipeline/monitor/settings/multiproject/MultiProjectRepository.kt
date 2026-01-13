package de.richargh.pipeline.monitor.settings.multiproject

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MultiProjectRepository(
    private val settings: Settings = Settings()
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_TOKENS = "multi_project_tokens"
        private const val KEY_PROJECTS = "multi_project_projects"
        private const val KEY_ACTIVE_PROJECT_ID = "multi_project_active_id"
    }

    // Token operations
    fun getAllTokens(): List<TokenConfig> {
        val jsonString = settings.getStringOrNull(KEY_TOKENS) ?: return emptyList()
        return try {
            json.decodeFromString<List<TokenConfig>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveToken(token: TokenConfig) {
        val tokens = getAllTokens().toMutableList()
        val existingIndex = tokens.indexOfFirst { it.id == token.id }
        if (existingIndex >= 0) {
            tokens[existingIndex] = token
        } else {
            tokens.add(token)
        }
        settings.putString(KEY_TOKENS, json.encodeToString(tokens))
    }

    fun getToken(id: String): TokenConfig? {
        return getAllTokens().find { it.id == id }
    }

    fun deleteToken(id: String) {
        val tokens = getAllTokens().filter { it.id != id }
        settings.putString(KEY_TOKENS, json.encodeToString(tokens))
    }

    fun findTokenForGitLabUrl(url: String): TokenConfig? {
        return getAllTokens().find { it.matchesGitLabUrl(url) }
    }

    // Project operations
    fun getAllProjects(): List<ProjectConfig> {
        val jsonString = settings.getStringOrNull(KEY_PROJECTS) ?: return emptyList()
        return try {
            json.decodeFromString<List<ProjectConfig>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProject(project: ProjectConfig) {
        val projects = getAllProjects().toMutableList()
        val existingIndex = projects.indexOfFirst { it.id == project.id }
        if (existingIndex >= 0) {
            projects[existingIndex] = project
        } else {
            projects.add(project)
        }
        settings.putString(KEY_PROJECTS, json.encodeToString(projects))
    }

    fun getProject(id: String): ProjectConfig? {
        return getAllProjects().find { it.id == id }
    }

    fun deleteProject(id: String) {
        val projects = getAllProjects().filter { it.id != id }
        settings.putString(KEY_PROJECTS, json.encodeToString(projects))

        // Clear active project if it was deleted
        if (activeProjectId == id) {
            activeProjectId = null
        }
    }

    // Active project
    var activeProjectId: String?
        get() = settings.getStringOrNull(KEY_ACTIVE_PROJECT_ID)
        set(value) {
            if (value == null) {
                settings.remove(KEY_ACTIVE_PROJECT_ID)
            } else {
                settings.putString(KEY_ACTIVE_PROJECT_ID, value)
            }
        }

    fun getActiveProject(): ProjectConfig? {
        val id = activeProjectId ?: return null
        return getProject(id)
    }

    // Cleanup orphaned tokens
    fun deleteOrphanedTokens() {
        val projects = getAllProjects()
        val usedTokenIds = projects.map { it.tokenId }.toSet()
        val tokens = getAllTokens().filter { it.id in usedTokenIds }
        settings.putString(KEY_TOKENS, json.encodeToString(tokens))
    }
}
