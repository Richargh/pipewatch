package de.richargh.pipewatch.settings.multiproject

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitLabUrlParserTest {

    private val parser = GitLabUrlParser()

    @Nested
    inner class ParsePipelineUrlTests {
        @Test
        fun `parses standard pipeline URL`() {
            val url = "https://gitlab.maibornwolff.de/gitpushrun/skyward-meadow-poc/-/pipelines"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.maibornwolff.de", result.gitLabUrl)
            assertEquals("gitpushrun/skyward-meadow-poc", result.projectPath)
        }

        @Test
        fun `parses pipeline URL with specific pipeline ID`() {
            val url = "https://gitlab.maibornwolff.de/gitpushrun/skyward-meadow-poc/-/pipelines/12345"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.maibornwolff.de", result.gitLabUrl)
            assertEquals("gitpushrun/skyward-meadow-poc", result.projectPath)
        }

        @Test
        fun `parses pipeline URL with nested groups`() {
            val url = "https://gitlab.com/my-org/team/subteam/project/-/pipelines"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.com", result.gitLabUrl)
            assertEquals("my-org/team/subteam/project", result.projectPath)
        }

        @Test
        fun `parses project URL without pipelines path`() {
            val url = "https://gitlab.example.com/group/project"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.example.com", result.gitLabUrl)
            assertEquals("group/project", result.projectPath)
        }

        @Test
        fun `parses URL with trailing slash`() {
            val url = "https://gitlab.maibornwolff.de/gitpushrun/skyward-meadow-poc/-/pipelines/"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.maibornwolff.de", result.gitLabUrl)
            assertEquals("gitpushrun/skyward-meadow-poc", result.projectPath)
        }

        @Test
        fun `parses HTTP URL`() {
            val url = "http://gitlab.local/group/project/-/pipelines"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("http://gitlab.local", result.gitLabUrl)
            assertEquals("group/project", result.projectPath)
        }

        @Test
        fun `parses URL with port number`() {
            val url = "https://gitlab.example.com:8443/group/project/-/pipelines"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.example.com:8443", result.gitLabUrl)
            assertEquals("group/project", result.projectPath)
        }
    }

    @Nested
    inner class ParseJobsUrlTests {
        @Test
        fun `parses jobs URL`() {
            val url = "https://gitlab.com/group/project/-/jobs/12345"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.com", result.gitLabUrl)
            assertEquals("group/project", result.projectPath)
        }
    }

    @Nested
    inner class ParseMergeRequestUrlTests {
        @Test
        fun `parses merge request URL`() {
            val url = "https://gitlab.com/group/project/-/merge_requests/42"
            val result = parser.parse(url)

            assertNotNull(result)
            assertEquals("https://gitlab.com", result.gitLabUrl)
            assertEquals("group/project", result.projectPath)
        }
    }

    @Nested
    inner class InvalidUrlTests {
        @Test
        fun `returns null for empty string`() {
            assertNull(parser.parse(""))
        }

        @Test
        fun `returns null for blank string`() {
            assertNull(parser.parse("   "))
        }

        @Test
        fun `returns null for invalid URL`() {
            assertNull(parser.parse("not-a-url"))
        }

        @Test
        fun `returns null for URL without project path`() {
            assertNull(parser.parse("https://gitlab.com"))
        }

        @Test
        fun `returns null for URL with only one path segment`() {
            assertNull(parser.parse("https://gitlab.com/group"))
        }
    }
}
