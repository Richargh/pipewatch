package de.richargh.pipewatch.widgets.menu

import de.richargh.pipewatch.gitlab.api.Job
import de.richargh.pipewatch.gitlab.api.Pipeline
import de.richargh.pipewatch.status.app.api.PipelineStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MenuBuilderTest {
    @Test
    fun `MenuBuilder can be instantiated`() {
        val builder = MenuBuilder()
        Assertions.assertNotNull(builder)
    }

    @Test
    fun `MenuBuilder generates list of menu items`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        Assertions.assertNotNull(items)
        Assertions.assertTrue(items.isNotEmpty())
    }

    @Test
    fun `MenuBuilder includes status item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val statusItem = items.find { it.type == MenuItemType.STATUS }
        Assertions.assertNotNull(statusItem)
    }

    @Test
    fun `MenuBuilder includes separator`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val separatorItem = items.find { it.type == MenuItemType.SEPARATOR }
        Assertions.assertNotNull(separatorItem)
    }

    @Test
    fun `MenuBuilder includes refresh item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val refreshItem = items.find { it.type == MenuItemType.REFRESH }
        Assertions.assertNotNull(refreshItem)
        Assertions.assertEquals("Refresh", refreshItem?.label)
    }

    @Test
    fun `MenuBuilder includes quit item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val quitItem = items.find { it.type == MenuItemType.QUIT }
        Assertions.assertNotNull(quitItem)
        Assertions.assertEquals("Quit", quitItem?.label)
    }

    @Test
    fun `MenuBuilder includes settings item`() {
        val builder = MenuBuilder()
        val items = builder.buildMenuItems()
        val settingsItem = items.find { it.type == MenuItemType.SETTINGS }
        Assertions.assertNotNull(settingsItem)
        Assertions.assertEquals("Settings...", settingsItem?.label)
    }

    @Test
    fun `status item displays current pipeline status`() {
        val builder = MenuBuilder()
        builder.setPipelineStatus(PipelineStatus.SUCCESS)
        val items = builder.buildMenuItems()
        val statusItem = items.find { it.type == MenuItemType.STATUS }
        Assertions.assertTrue(statusItem?.label?.contains("Success") == true)
    }

    @Test
    fun `status label includes pipeline iid and branch when pipeline is set`() {
        val builder = MenuBuilder()
        builder.setPipelineStatus(PipelineStatus.SUCCESS)
        builder.setPipeline(
            Pipeline(
                id = 12345,
                iid = 100,
                status = "success",
                ref = "main",
                sha = "abc123",
            ),
        )
        val items = builder.buildMenuItems()
        val statusItem = items.find { it.type == MenuItemType.STATUS && it.label.contains("Success") }
        Assertions.assertTrue(statusItem?.label?.contains("#100") == true)
        Assertions.assertTrue(statusItem?.label?.contains("main") == true)
    }

    @Test
    fun `project name is displayed when set`() {
        val builder = MenuBuilder()
        builder.setProjectName("my-group/my-project")
        val items = builder.buildMenuItems()
        val projectItem = items.find { it.label == "my-group/my-project" }
        Assertions.assertNotNull(projectItem)
    }

    @Test
    fun `failed jobs are displayed when pipeline failed`() {
        val builder = MenuBuilder()
        builder.setPipelineStatus(PipelineStatus.FAILED)
        builder.setFailedJobsByStage(
            mapOf(
                "test" to
                    listOf(
                        Job(id = 1, name = "unit-test", stage = "test", status = "failed"),
                        Job(id = 2, name = "integration-test", stage = "test", status = "failed"),
                    ),
            ),
        )
        val items = builder.buildMenuItems()
        val stageHeader = items.find { it.type == MenuItemType.FAILED_STAGE_HEADER }
        val failedJobs = items.filter { it.type == MenuItemType.FAILED_JOB }

        Assertions.assertNotNull(stageHeader)
        Assertions.assertEquals("Failed in test:", stageHeader?.label)
        Assertions.assertEquals(2, failedJobs.size)
    }

    @Test
    fun `failed jobs have URLs for direct linking`() {
        val builder = MenuBuilder()
        builder.setPipelineStatus(PipelineStatus.FAILED)
        builder.setFailedJobsByStage(
            mapOf(
                "test" to
                    listOf(
                        Job(
                            id = 1,
                            name = "unit-test",
                            stage = "test",
                            status = "failed",
                            webUrl = "https://gitlab.example.com/job/1",
                        ),
                    ),
            ),
        )
        val items = builder.buildMenuItems()
        val failedJob = items.find { it.type == MenuItemType.FAILED_JOB }

        Assertions.assertEquals("https://gitlab.example.com/job/1", failedJob?.url)
    }

    @Test
    fun `failed jobs are not displayed when pipeline succeeded`() {
        val builder = MenuBuilder()
        builder.setPipelineStatus(PipelineStatus.SUCCESS)
        builder.setFailedJobsByStage(
            mapOf(
                "test" to listOf(Job(id = 1, name = "unit-test", stage = "test", status = "failed")),
            ),
        )
        val items = builder.buildMenuItems()
        val failedJobs = items.filter { it.type == MenuItemType.FAILED_JOB }

        Assertions.assertTrue(failedJobs.isEmpty())
    }

    @Test
    fun `Open in GitLab item has pipeline URL`() {
        val builder = MenuBuilder()
        builder.setPipeline(
            Pipeline(
                id = 12345,
                iid = 100,
                status = "success",
                ref = "main",
                sha = "abc123",
                webUrl = "https://gitlab.example.com/pipeline/12345",
            ),
        )
        val items = builder.buildMenuItems()
        val openItem = items.find { it.type == MenuItemType.OPEN_GITLAB }

        Assertions.assertEquals("https://gitlab.example.com/pipeline/12345", openItem?.url)
    }

    @Test
    fun `menu includes version item before quit`() {
        val menuBuilder = MenuBuilder(appVersion = "1.2.3")
        val items = menuBuilder.buildMenuItems()

        val quitIndex = items.indexOfFirst { it.type == MenuItemType.QUIT }
        val versionIndex = items.indexOfFirst { it.type == MenuItemType.VERSION }

        Assertions.assertTrue(versionIndex >= 0, "Version item should exist")
        Assertions.assertTrue(versionIndex < quitIndex, "Version should come before Quit")
        Assertions.assertEquals("Version 1.2.3", items[versionIndex].label)
    }
}
