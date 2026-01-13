package de.richargh.pipeline.monitor.settings.multiproject

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TokenConfigTest {

    @Nested
    inner class SerializationTests {
        @Test
        fun `TokenConfig can be serialized to JSON`() {
            val token = TokenConfig(
                id = "token-abc",
                gitLabUrl = "https://gitlab.example.com",
                accessToken = "glpat-xxxxxxxxxxxx"
            )

            val json = Json.encodeToString(token)

            assertNotNull(json)
            assert(json.contains("token-abc"))
            assert(json.contains("https://gitlab.example.com"))
            assert(json.contains("glpat-xxxxxxxxxxxx"))
        }

        @Test
        fun `TokenConfig can be deserialized from JSON`() {
            val json = """
                {
                    "id": "token-abc",
                    "gitLabUrl": "https://gitlab.example.com",
                    "accessToken": "glpat-xxxxxxxxxxxx"
                }
            """.trimIndent()

            val token = Json.decodeFromString<TokenConfig>(json)

            assertEquals("token-abc", token.id)
            assertEquals("https://gitlab.example.com", token.gitLabUrl)
            assertEquals("glpat-xxxxxxxxxxxx", token.accessToken)
        }
    }

    @Nested
    inner class MatchingTests {
        @Test
        fun `matchesGitLabUrl returns true for exact match`() {
            val token = TokenConfig(
                id = "token-abc",
                gitLabUrl = "https://gitlab.example.com",
                accessToken = "glpat-xxxxxxxxxxxx"
            )

            assert(token.matchesGitLabUrl("https://gitlab.example.com"))
        }

        @Test
        fun `matchesGitLabUrl returns true when trailing slash differs`() {
            val token = TokenConfig(
                id = "token-abc",
                gitLabUrl = "https://gitlab.example.com",
                accessToken = "glpat-xxxxxxxxxxxx"
            )

            assert(token.matchesGitLabUrl("https://gitlab.example.com/"))
        }

        @Test
        fun `matchesGitLabUrl returns false for different hosts`() {
            val token = TokenConfig(
                id = "token-abc",
                gitLabUrl = "https://gitlab.example.com",
                accessToken = "glpat-xxxxxxxxxxxx"
            )

            assert(!token.matchesGitLabUrl("https://gitlab.other.com"))
        }

        @Test
        fun `matchesGitLabUrl is case insensitive for host`() {
            val token = TokenConfig(
                id = "token-abc",
                gitLabUrl = "https://gitlab.example.com",
                accessToken = "glpat-xxxxxxxxxxxx"
            )

            assert(token.matchesGitLabUrl("https://GITLAB.EXAMPLE.COM"))
        }
    }
}
