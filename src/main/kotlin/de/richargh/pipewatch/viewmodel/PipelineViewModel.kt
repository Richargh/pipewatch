package de.richargh.pipewatch.viewmodel

import de.richargh.pipewatch.gitlab.api.GitLabClient
import de.richargh.pipewatch.gitlab.api.Job
import de.richargh.pipewatch.gitlab.api.Pipeline
import de.richargh.pipewatch.gitlab.api.getFailedJobsByStage
import de.richargh.pipewatch.status.app.api.PipelineStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PipelineViewModel(
    private val client: GitLabClient,
    private val projectId: Long,
    private val ref: String? = null,
) {
    private val _pipelineStatus = MutableStateFlow(PipelineStatus.UNKNOWN)
    val pipelineStatus: StateFlow<PipelineStatus> = _pipelineStatus.asStateFlow()

    private val _currentPipeline = MutableStateFlow<Pipeline?>(null)
    val currentPipeline: StateFlow<Pipeline?> = _currentPipeline.asStateFlow()

    private val _failedJobsByStage = MutableStateFlow<Map<String, List<Job>>>(emptyMap())
    val failedJobsByStage: StateFlow<Map<String, List<Job>>> = _failedJobsByStage.asStateFlow()

    suspend fun refresh() {
        val pipeline = client.getLatestPipeline(projectId, ref)
        _currentPipeline.value = pipeline

        val status =
            if (pipeline != null) {
                PipelineStatus.fromString(pipeline.status)
            } else {
                PipelineStatus.UNKNOWN
            }
        _pipelineStatus.value = status

        // Fetch failed jobs if pipeline failed
        if (status == PipelineStatus.FAILED && pipeline != null) {
            val jobs = client.getPipelineJobs(projectId, pipeline.id)
            _failedJobsByStage.value = jobs.getFailedJobsByStage()
        } else {
            _failedJobsByStage.value = emptyMap()
        }
    }
}
