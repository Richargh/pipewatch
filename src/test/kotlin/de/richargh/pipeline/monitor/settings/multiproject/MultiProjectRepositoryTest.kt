package de.richargh.pipeline.monitor.settings.multiproject

import com.russhwolf.settings.MapSettings
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MultiProjectRepositoryTest {

    private lateinit var settings: MapSettings
    private lateinit var repository: MultiProjectRepository

    @BeforeEach
    fun setup() {
        settings = MapSettings()
        repository = MultiProjectRepository(settings)
    }

    @Nested
    inner class TokenStorageTests {
        @Test
        fun `getAllTokens returns empty list when no tokens stored`() {
            val tokens = repository.getAllTokens()
            assertTrue(tokens.isEmpty())
        }

        @Test
        fun `saveToken stores token and can retrieve it`() {
            val token = TokenConfig(
                id = "token-1",
                gitLabUrl = "https://gitlab.example.com",
                accessToken = "glpat-xxxx"
            )

            repository.saveToken(token)
            val tokens = repository.getAllTokens()

            assertEquals(1, tokens.size)
            assertEquals("token-1", tokens[0].id)
            assertEquals("https://gitlab.example.com", tokens[0].gitLabUrl)
        }

        @Test
        fun `saveToken updates existing token with same id`() {
            val token1 = TokenConfig("token-1", "https://gitlab.example.com", "old-token")
            val token2 = TokenConfig("token-1", "https://gitlab.example.com", "new-token")

            repository.saveToken(token1)
            repository.saveToken(token2)

            val tokens = repository.getAllTokens()
            assertEquals(1, tokens.size)
            assertEquals("new-token", tokens[0].accessToken)
        }

        @Test
        fun `getToken returns token by id`() {
            val token = TokenConfig("token-1", "https://gitlab.example.com", "glpat-xxxx")
            repository.saveToken(token)

            val retrieved = repository.getToken("token-1")

            assertNotNull(retrieved)
            assertEquals("token-1", retrieved.id)
        }

        @Test
        fun `getToken returns null for unknown id`() {
            assertNull(repository.getToken("unknown"))
        }

        @Test
        fun `deleteToken removes token`() {
            val token = TokenConfig("token-1", "https://gitlab.example.com", "glpat-xxxx")
            repository.saveToken(token)

            repository.deleteToken("token-1")

            assertTrue(repository.getAllTokens().isEmpty())
        }
    }

    @Nested
    inner class TokenReuseDetectionTests {
        @Test
        fun `findTokenForGitLabUrl returns matching token`() {
            val token = TokenConfig("token-1", "https://gitlab.example.com", "glpat-xxxx")
            repository.saveToken(token)

            val found = repository.findTokenForGitLabUrl("https://gitlab.example.com")

            assertNotNull(found)
            assertEquals("token-1", found.id)
        }

        @Test
        fun `findTokenForGitLabUrl returns null when no match`() {
            val token = TokenConfig("token-1", "https://gitlab.example.com", "glpat-xxxx")
            repository.saveToken(token)

            val found = repository.findTokenForGitLabUrl("https://gitlab.other.com")

            assertNull(found)
        }

        @Test
        fun `findTokenForGitLabUrl handles trailing slash difference`() {
            val token = TokenConfig("token-1", "https://gitlab.example.com", "glpat-xxxx")
            repository.saveToken(token)

            val found = repository.findTokenForGitLabUrl("https://gitlab.example.com/")

            assertNotNull(found)
        }
    }

    @Nested
    inner class ProjectStorageTests {
        @Test
        fun `getAllProjects returns empty list when no projects stored`() {
            val projects = repository.getAllProjects()
            assertTrue(projects.isEmpty())
        }

        @Test
        fun `saveProject stores project and can retrieve it`() {
            val project = ProjectConfig(
                id = "proj-1",
                name = "My Project",
                gitLabUrl = "https://gitlab.example.com",
                projectPath = "group/project",
                projectId = 12345L,
                tokenId = "token-1"
            )

            repository.saveProject(project)
            val projects = repository.getAllProjects()

            assertEquals(1, projects.size)
            assertEquals("proj-1", projects[0].id)
            assertEquals("My Project", projects[0].name)
        }

        @Test
        fun `saveProject updates existing project with same id`() {
            val project1 = ProjectConfig("proj-1", "Old Name", "https://gitlab.example.com", "group/project", 12345L, "token-1")
            val project2 = ProjectConfig("proj-1", "New Name", "https://gitlab.example.com", "group/project", 12345L, "token-1")

            repository.saveProject(project1)
            repository.saveProject(project2)

            val projects = repository.getAllProjects()
            assertEquals(1, projects.size)
            assertEquals("New Name", projects[0].name)
        }

        @Test
        fun `getProject returns project by id`() {
            val project = ProjectConfig("proj-1", "My Project", "https://gitlab.example.com", "group/project", 12345L, "token-1")
            repository.saveProject(project)

            val retrieved = repository.getProject("proj-1")

            assertNotNull(retrieved)
            assertEquals("proj-1", retrieved.id)
        }

        @Test
        fun `getProject returns null for unknown id`() {
            assertNull(repository.getProject("unknown"))
        }

        @Test
        fun `deleteProject removes project`() {
            val project = ProjectConfig("proj-1", "My Project", "https://gitlab.example.com", "group/project", 12345L, "token-1")
            repository.saveProject(project)

            repository.deleteProject("proj-1")

            assertTrue(repository.getAllProjects().isEmpty())
        }
    }

    @Nested
    inner class ActiveProjectTests {
        @Test
        fun `activeProjectId is null when not set`() {
            assertNull(repository.activeProjectId)
        }

        @Test
        fun `activeProjectId can be set and retrieved`() {
            repository.activeProjectId = "proj-1"
            assertEquals("proj-1", repository.activeProjectId)
        }

        @Test
        fun `activeProjectId can be cleared`() {
            repository.activeProjectId = "proj-1"
            repository.activeProjectId = null
            assertNull(repository.activeProjectId)
        }

        @Test
        fun `getActiveProject returns project when set`() {
            val project = ProjectConfig("proj-1", "My Project", "https://gitlab.example.com", "group/project", 12345L, "token-1")
            repository.saveProject(project)
            repository.activeProjectId = "proj-1"

            val active = repository.getActiveProject()

            assertNotNull(active)
            assertEquals("proj-1", active.id)
        }

        @Test
        fun `getActiveProject returns null when id not set`() {
            assertNull(repository.getActiveProject())
        }

        @Test
        fun `getActiveProject returns null when project deleted`() {
            val project = ProjectConfig("proj-1", "My Project", "https://gitlab.example.com", "group/project", 12345L, "token-1")
            repository.saveProject(project)
            repository.activeProjectId = "proj-1"

            repository.deleteProject("proj-1")

            assertNull(repository.getActiveProject())
        }
    }

    @Nested
    inner class OrphanedTokenCleanupTests {
        @Test
        fun `deleteOrphanedTokens removes tokens not referenced by any project`() {
            val token1 = TokenConfig("token-1", "https://gitlab.example.com", "glpat-1")
            val token2 = TokenConfig("token-2", "https://gitlab.other.com", "glpat-2")
            repository.saveToken(token1)
            repository.saveToken(token2)

            val project = ProjectConfig("proj-1", "My Project", "https://gitlab.example.com", "group/project", 12345L, "token-1")
            repository.saveProject(project)

            repository.deleteOrphanedTokens()

            val tokens = repository.getAllTokens()
            assertEquals(1, tokens.size)
            assertEquals("token-1", tokens[0].id)
        }

        @Test
        fun `deleteOrphanedTokens keeps all tokens when all are referenced`() {
            val token1 = TokenConfig("token-1", "https://gitlab.example.com", "glpat-1")
            val token2 = TokenConfig("token-2", "https://gitlab.other.com", "glpat-2")
            repository.saveToken(token1)
            repository.saveToken(token2)

            val project1 = ProjectConfig("proj-1", "Project 1", "https://gitlab.example.com", "group/project1", 12345L, "token-1")
            val project2 = ProjectConfig("proj-2", "Project 2", "https://gitlab.other.com", "group/project2", 67890L, "token-2")
            repository.saveProject(project1)
            repository.saveProject(project2)

            repository.deleteOrphanedTokens()

            assertEquals(2, repository.getAllTokens().size)
        }
    }
}
