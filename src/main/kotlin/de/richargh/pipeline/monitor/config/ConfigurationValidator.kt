package de.richargh.pipeline.monitor.config

import de.richargh.pipeline.monitor.settings.SettingsRepository

sealed class ConfigurationStatus {
    data object Valid : ConfigurationStatus()

    data class Invalid(val errors: List<String>) : ConfigurationStatus()
}

class ConfigurationValidator(
    private val settingsRepository: SettingsRepository,
) {
    fun validate(): ConfigurationStatus {
        val errors = mutableListOf<String>()

        if (!settingsRepository.isConfigured) {
            errors.add("No configuration found")
        }

        // Validate URL if present
        if (settingsRepository.gitLabUrl != null && !SettingsRepository.isValidGitLabUrl(settingsRepository.gitLabUrl)) {
            errors.add("Invalid GitLab URL")
        }

        // Validate token presence
        if (settingsRepository.gitLabUrl != null && settingsRepository.accessToken.isNullOrBlank()) {
            errors.add("Access token is required")
        }

        // Validate project selection
        if (settingsRepository.gitLabUrl != null && settingsRepository.selectedProjectId == null) {
            errors.add("No project selected")
        }

        return if (errors.isEmpty()) {
            ConfigurationStatus.Valid
        } else {
            ConfigurationStatus.Invalid(errors)
        }
    }

    fun isConfigured(): Boolean = settingsRepository.isConfigured

    fun requiresSetup(): Boolean = !isConfigured()
}
