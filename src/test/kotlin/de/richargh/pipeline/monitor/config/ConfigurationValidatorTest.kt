package de.richargh.pipeline.monitor.config

import com.russhwolf.settings.MapSettings
import de.richargh.pipeline.monitor.settings.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfigurationValidatorTest {
    private lateinit var settings: MapSettings
    private lateinit var settingsRepository: SettingsRepository

    @BeforeEach
    fun setup() {
        settings = MapSettings()
        settingsRepository = SettingsRepository(settings)
    }

    @Test
    fun `requiresSetup returns true when nothing is configured`() {
        val validator = ConfigurationValidator(settingsRepository)

        assertTrue(validator.requiresSetup())
    }

    @Test
    fun `requiresSetup returns false when settings are configured`() {
        settingsRepository.gitLabUrl = "https://gitlab.example.com"
        settingsRepository.accessToken = "test-token"
        settingsRepository.selectedProjectId = 123L

        val validator = ConfigurationValidator(settingsRepository)

        assertFalse(validator.requiresSetup())
    }

    @Test
    fun `validate returns Valid when settings are configured`() {
        settingsRepository.gitLabUrl = "https://gitlab.example.com"
        settingsRepository.accessToken = "test-token"
        settingsRepository.selectedProjectId = 123L

        val validator = ConfigurationValidator(settingsRepository)

        assertIs<ConfigurationStatus.Valid>(validator.validate())
    }

    @Test
    fun `validate returns Invalid with errors when not configured`() {
        val validator = ConfigurationValidator(settingsRepository)

        val result = validator.validate()
        assertIs<ConfigurationStatus.Invalid>(result)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `validate returns Invalid when URL is invalid`() {
        settingsRepository.gitLabUrl = "not-a-valid-url"
        settingsRepository.accessToken = "test-token"
        settingsRepository.selectedProjectId = 123L

        val validator = ConfigurationValidator(settingsRepository)

        val result = validator.validate()
        assertIs<ConfigurationStatus.Invalid>(result)
        assertTrue(result.errors.any { it.contains("URL") })
    }

    @Test
    fun `validate returns Invalid when token is missing`() {
        settingsRepository.gitLabUrl = "https://gitlab.example.com"
        settingsRepository.selectedProjectId = 123L

        val validator = ConfigurationValidator(settingsRepository)

        val result = validator.validate()
        assertIs<ConfigurationStatus.Invalid>(result)
        assertTrue(result.errors.any { it.contains("token") })
    }
}
