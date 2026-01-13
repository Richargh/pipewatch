package de.richargh.pipewatch.gitlab.api

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GitLabConfigTest {
    @Test
    fun `can create GitLabConfig with base URL and token`() {
        val config =
            GitLabConfig(
                baseUrl = "https://gitlab.example.com",
                accessToken = "test-token",
            )

        assertEquals("https://gitlab.example.com", config.baseUrl)
        assertEquals("test-token", config.accessToken)
    }

    @Test
    fun `baseUrl should strip trailing slash`() {
        val config =
            GitLabConfig(
                baseUrl = "https://gitlab.example.com/",
                accessToken = "test-token",
            )

        assertEquals("https://gitlab.example.com", config.baseUrl)
    }
}

class GitLabClientTest {
    @Test
    fun `can instantiate GitLabClient with config`() {
        val config =
            GitLabConfig(
                baseUrl = "https://gitlab.example.com",
                accessToken = "test-token",
            )

        val client = GitLabClient(config)

        assertNotNull(client)
    }

    @Test
    fun `can instantiate GitLabClient with base URL and token directly`() {
        val client =
            GitLabClient(
                baseUrl = "https://gitlab.example.com",
                accessToken = "test-token",
            )

        assertNotNull(client)
    }

    @Test
    fun `client exposes its config`() {
        val config =
            GitLabConfig(
                baseUrl = "https://gitlab.example.com",
                accessToken = "test-token",
            )

        val client = GitLabClient(config)

        assertEquals(config, client.config)
    }
}
