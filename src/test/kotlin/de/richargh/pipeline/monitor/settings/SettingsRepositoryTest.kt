package de.richargh.pipeline.monitor.settings

import com.russhwolf.settings.MapSettings
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SettingsRepositoryTest {
    private lateinit var settings: MapSettings
    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setup() {
        settings = MapSettings()
        repository = SettingsRepository(settings)
    }

    @Nested
    inner class GitLabUrlTests {
        @Test
        fun `gitLabUrl returns null when not set`() {
            assertNull(repository.gitLabUrl)
        }

        @Test
        fun `gitLabUrl can be saved and retrieved`() {
            repository.gitLabUrl = "https://gitlab.example.com"
            assertEquals("https://gitlab.example.com", repository.gitLabUrl)
        }

        @Test
        fun `gitLabUrl trims trailing slash`() {
            repository.gitLabUrl = "https://gitlab.example.com/"
            assertEquals("https://gitlab.example.com", repository.gitLabUrl)
        }

        @Test
        fun `gitLabUrl can be cleared by setting null`() {
            repository.gitLabUrl = "https://gitlab.example.com"
            repository.gitLabUrl = null
            assertNull(repository.gitLabUrl)
        }

        @Test
        fun `isValidGitLabUrl returns false for null`() {
            assertFalse(SettingsRepository.isValidGitLabUrl(null))
        }

        @Test
        fun `isValidGitLabUrl returns false for empty string`() {
            assertFalse(SettingsRepository.isValidGitLabUrl(""))
        }

        @Test
        fun `isValidGitLabUrl returns false for invalid URL`() {
            assertFalse(SettingsRepository.isValidGitLabUrl("not-a-url"))
        }

        @Test
        fun `isValidGitLabUrl returns true for valid HTTPS URL`() {
            assertTrue(SettingsRepository.isValidGitLabUrl("https://gitlab.example.com"))
        }

        @Test
        fun `isValidGitLabUrl returns true for valid HTTP URL`() {
            assertTrue(SettingsRepository.isValidGitLabUrl("http://gitlab.local"))
        }
    }

    @Nested
    inner class AccessTokenTests {
        @Test
        fun `accessToken returns null when not set`() {
            assertNull(repository.accessToken)
        }

        @Test
        fun `accessToken can be saved and retrieved`() {
            repository.accessToken = "glpat-xxxxxxxxxxxx"
            assertEquals("glpat-xxxxxxxxxxxx", repository.accessToken)
        }

        @Test
        fun `accessToken can be cleared by setting null`() {
            repository.accessToken = "glpat-xxxxxxxxxxxx"
            repository.accessToken = null
            assertNull(repository.accessToken)
        }

        @Test
        fun `accessToken trims whitespace`() {
            repository.accessToken = "  glpat-xxxxxxxxxxxx  "
            assertEquals("glpat-xxxxxxxxxxxx", repository.accessToken)
        }
    }

    @Nested
    inner class SelectedProjectTests {
        @Test
        fun `selectedProjectId returns null when not set`() {
            assertNull(repository.selectedProjectId)
        }

        @Test
        fun `selectedProjectId can be saved and retrieved`() {
            repository.selectedProjectId = 12345L
            assertEquals(12345L, repository.selectedProjectId)
        }

        @Test
        fun `selectedProjectName returns null when not set`() {
            assertNull(repository.selectedProjectName)
        }

        @Test
        fun `selectedProjectName can be saved and retrieved`() {
            repository.selectedProjectName = "My Project"
            assertEquals("My Project", repository.selectedProjectName)
        }

        @Test
        fun `selectedProject returns bundled data`() {
            repository.selectedProjectId = 12345L
            repository.selectedProjectName = "My Project"

            val selected = repository.selectedProject
            assertEquals(12345L, selected?.id)
            assertEquals("My Project", selected?.name)
        }

        @Test
        fun `selectedProject returns null when id not set`() {
            repository.selectedProjectName = "My Project"
            assertNull(repository.selectedProject)
        }

        @Test
        fun `setSelectedProject sets both id and name`() {
            repository.setSelectedProject(SelectedProject(12345L, "My Project"))

            assertEquals(12345L, repository.selectedProjectId)
            assertEquals("My Project", repository.selectedProjectName)
        }

        @Test
        fun `clearSelectedProject clears both id and name`() {
            repository.selectedProjectId = 12345L
            repository.selectedProjectName = "My Project"

            repository.clearSelectedProject()

            assertNull(repository.selectedProjectId)
            assertNull(repository.selectedProjectName)
        }
    }

    @Nested
    inner class RefreshIntervalTests {
        @Test
        fun `refreshInterval returns default when not set`() {
            assertEquals(RefreshInterval.ONE_MINUTE, repository.refreshInterval)
        }

        @Test
        fun `refreshInterval can be saved and retrieved`() {
            repository.refreshInterval = RefreshInterval.FIVE_MINUTES
            assertEquals(RefreshInterval.FIVE_MINUTES, repository.refreshInterval)
        }

        @Test
        fun `RefreshInterval has correct duration values`() {
            assertEquals(30.seconds, RefreshInterval.THIRTY_SECONDS.duration)
            assertEquals(1.minutes, RefreshInterval.ONE_MINUTE.duration)
            assertEquals(2.minutes, RefreshInterval.TWO_MINUTES.duration)
            assertEquals(5.minutes, RefreshInterval.FIVE_MINUTES.duration)
            assertEquals(10.minutes, RefreshInterval.TEN_MINUTES.duration)
        }

        @Test
        fun `RefreshInterval has correct display names`() {
            assertEquals("30 seconds", RefreshInterval.THIRTY_SECONDS.displayName)
            assertEquals("1 minute", RefreshInterval.ONE_MINUTE.displayName)
            assertEquals("2 minutes", RefreshInterval.TWO_MINUTES.displayName)
            assertEquals("5 minutes", RefreshInterval.FIVE_MINUTES.displayName)
            assertEquals("10 minutes", RefreshInterval.TEN_MINUTES.displayName)
        }
    }

    @Nested
    inner class NotificationsSettingTests {
        @Test
        fun `notificationsEnabled returns true by default`() {
            assertTrue(repository.notificationsEnabled)
        }

        @Test
        fun `notificationsEnabled can be saved and retrieved`() {
            repository.notificationsEnabled = false
            assertFalse(repository.notificationsEnabled)
        }
    }

    @Nested
    inner class BranchFilterTests {
        @Test
        fun `branchFilter returns null when not set`() {
            assertNull(repository.branchFilter)
        }

        @Test
        fun `branchFilter can be saved and retrieved`() {
            repository.branchFilter = "main"
            assertEquals("main", repository.branchFilter)
        }

        @Test
        fun `branchFilter can be cleared by setting null`() {
            repository.branchFilter = "main"
            repository.branchFilter = null
            assertNull(repository.branchFilter)
        }
    }

    @Nested
    inner class ConfigurationValidationTests {
        @Test
        fun `isConfigured returns false when nothing is set`() {
            assertFalse(repository.isConfigured)
        }

        @Test
        fun `isConfigured returns false when only URL is set`() {
            repository.gitLabUrl = "https://gitlab.example.com"
            assertFalse(repository.isConfigured)
        }

        @Test
        fun `isConfigured returns false when only URL and token are set`() {
            repository.gitLabUrl = "https://gitlab.example.com"
            repository.accessToken = "glpat-xxxxxxxxxxxx"
            assertFalse(repository.isConfigured)
        }

        @Test
        fun `isConfigured returns true when URL, token, and project are set`() {
            repository.gitLabUrl = "https://gitlab.example.com"
            repository.accessToken = "glpat-xxxxxxxxxxxx"
            repository.selectedProjectId = 12345L

            assertTrue(repository.isConfigured)
        }
    }

    @Nested
    inner class ClearAllTests {
        @Test
        fun `clearAll removes all settings`() {
            repository.gitLabUrl = "https://gitlab.example.com"
            repository.accessToken = "glpat-xxxxxxxxxxxx"
            repository.selectedProjectId = 12345L
            repository.selectedProjectName = "My Project"
            repository.refreshInterval = RefreshInterval.FIVE_MINUTES
            repository.notificationsEnabled = false
            repository.branchFilter = "main"

            repository.clearAll()

            assertNull(repository.gitLabUrl)
            assertNull(repository.accessToken)
            assertNull(repository.selectedProjectId)
            assertNull(repository.selectedProjectName)
            assertEquals(RefreshInterval.ONE_MINUTE, repository.refreshInterval)
            assertTrue(repository.notificationsEnabled)
            assertNull(repository.branchFilter)
        }
    }
}
