package de.richargh.pipewatch.settings.multiproject

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProjectConfigTest {
    @Nested
    inner class SerializationTests {
        @Test
        fun `ProjectConfig can be serialized to JSON`() {
            val config =
                ProjectConfig(
                    id = "proj-123",
                    name = "My Project",
                    gitLabUrl = "https://gitlab.example.com",
                    projectPath = "group/project",
                    projectId = 12345L,
                    tokenId = "token-abc",
                )

            val json = Json.encodeToString(config)

            assertNotNull(json)
            assert(json.contains("proj-123"))
            assert(json.contains("My Project"))
            assert(json.contains("https://gitlab.example.com"))
            assert(json.contains("group/project"))
            assert(json.contains("12345"))
            assert(json.contains("token-abc"))
        }

        @Test
        fun `ProjectConfig can be deserialized from JSON`() {
            val json =
                """
                {
                    "id": "proj-123",
                    "name": "My Project",
                    "gitLabUrl": "https://gitlab.example.com",
                    "projectPath": "group/project",
                    "projectId": 12345,
                    "tokenId": "token-abc"
                }
                """.trimIndent()

            val config = Json.decodeFromString<ProjectConfig>(json)

            assertEquals("proj-123", config.id)
            assertEquals("My Project", config.name)
            assertEquals("https://gitlab.example.com", config.gitLabUrl)
            assertEquals("group/project", config.projectPath)
            assertEquals(12345L, config.projectId)
            assertEquals("token-abc", config.tokenId)
        }
    }

    @Nested
    inner class DisplayTests {
        @Test
        fun `displayName returns project name with GitLab host`() {
            val config =
                ProjectConfig(
                    id = "proj-123",
                    name = "My Project",
                    gitLabUrl = "https://gitlab.example.com",
                    projectPath = "group/project",
                    projectId = 12345L,
                    tokenId = "token-abc",
                )

            assertEquals("My Project (gitlab.example.com)", config.displayName)
        }

        @Test
        fun `displayName handles URL with port`() {
            val config =
                ProjectConfig(
                    id = "proj-123",
                    name = "My Project",
                    gitLabUrl = "https://gitlab.example.com:8443",
                    projectPath = "group/project",
                    projectId = 12345L,
                    tokenId = "token-abc",
                )

            assertEquals("My Project (gitlab.example.com:8443)", config.displayName)
        }
    }
}
