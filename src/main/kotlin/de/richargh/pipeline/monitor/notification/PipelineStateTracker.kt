package de.richargh.pipeline.monitor.notification

import de.richargh.pipeline.monitor.gitlab.api.Pipeline
import de.richargh.pipeline.monitor.status.app.api.PipelineStatus

data class FailureInfo(
    val pipeline: Pipeline,
    val isNewFailure: Boolean,
)

class PipelineStateTracker {
    private var previousStatus: PipelineStatus? = null
    private var previousPipelineId: Long? = null

    fun updateAndCheckForFailure(
        status: PipelineStatus,
        pipeline: Pipeline?,
    ): FailureInfo? {
        val wasFirstUpdate = previousStatus == null
        val statusChangedToFailed = previousStatus != PipelineStatus.FAILED && status == PipelineStatus.FAILED
        val isNewFailedPipeline =
            status == PipelineStatus.FAILED &&
                pipeline != null &&
                previousPipelineId != null &&
                previousPipelineId != pipeline.id

        previousStatus = status
        previousPipelineId = pipeline?.id

        if (wasFirstUpdate) {
            return null
        }

        if (status != PipelineStatus.FAILED) {
            return null
        }

        if (statusChangedToFailed || isNewFailedPipeline) {
            return pipeline?.let { FailureInfo(it, isNewFailure = true) }
        }

        return null
    }

    fun reset() {
        previousStatus = null
        previousPipelineId = null
    }
}
