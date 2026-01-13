package de.richargh.pipeline.monitor.notification

import de.richargh.pipeline.monitor.gitlab.api.Job
import de.richargh.pipeline.monitor.gitlab.api.Pipeline

object NotificationBuilder {
    fun buildFailureNotification(
        pipeline: Pipeline,
        projectName: String,
        failedJobsByStage: Map<String, List<Job>>,
    ): NotificationContent {
        val title = "Pipeline Failed"

        val bodyBuilder = StringBuilder()
        bodyBuilder.append("$projectName on ${pipeline.ref}")

        if (failedJobsByStage.isNotEmpty()) {
            bodyBuilder.append("\n")
            failedJobsByStage.forEach { (stage, jobs) ->
                bodyBuilder.append("Failed in $stage: ${jobs.joinToString(", ") { it.name }}")
            }
        }

        return NotificationContent(
            title = title,
            body = bodyBuilder.toString(),
            url = pipeline.webUrl,
        )
    }
}
