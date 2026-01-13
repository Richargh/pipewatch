package de.richargh.pipeline.monitor.viewmodel

import de.richargh.pipeline.monitor.gitlab.api.GitLabClient
import de.richargh.pipeline.monitor.gitlab.api.Pipeline
import de.richargh.pipeline.monitor.status.app.api.PipelineStatus
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

    suspend fun refresh() {
        val pipeline = client.getLatestPipeline(projectId, ref)
        _currentPipeline.value = pipeline

        _pipelineStatus.value =
            if (pipeline != null) {
                PipelineStatus.fromString(pipeline.status)
            } else {
                PipelineStatus.UNKNOWN
            }
    }
}
